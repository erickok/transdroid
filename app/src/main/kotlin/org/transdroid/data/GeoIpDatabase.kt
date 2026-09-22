/*
 * Copyright 2010-2026 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid. If not, see <https://www.gnu.org/licenses/>.
 */
package org.transdroid.data

import android.net.InetAddresses
import com.maxmind.db.Reader
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.transdroid.errorlog.ErrorLog
import org.transdroid.protocol.Peer

/**
 * An optional, user-downloaded GeoIP database (MaxMind .mmdb format, from ip66.dev) that resolves
 * a peer's country from its IP address entirely on-device - no address is ever sent anywhere.
 * Only qBittorrent and Deluge can report a peer's country themselves, and only when their own
 * lookup is switched on; with this downloaded, the Peers tab shows a flag for every client.
 *
 * [directory] should be the app's no-backup files dir: at ~18 MB this file alone would eat most
 * of Android auto-backup's 25 MB quota (past which the whole backup is dropped, settings
 * included), and it is trivially re-downloadable.
 *
 * Pinned to MaxMind's reader 2.1.0 on purpose: it is the last release built for Java 8, has no
 * dependencies of its own, and is what Transdroid 2 shipped. 3.x targets Java 11 and 4.x Java 17,
 * and neither has been tried against Android's partial coverage of those APIs. Its generic
 * decoder ([Reader.get] with Map::class.java) returns the record as a nested Map.
 */
class GeoIpDatabase(private val directory: File, private val httpClient: OkHttpClient) {

    private val databaseFile = File(directory, FILE_NAME)

    private val _downloaded = MutableStateFlow(databaseFile.length() > 0)

    /** Whether a database is present, for the settings row; lookups are a no-op without one. */
    val downloaded: StateFlow<Boolean> = _downloaded.asStateFlow()

    /** Guards [reader] and the file underneath it, so a lookup never races a download swapping it out. */
    private val mutex = Mutex()
    private var reader: Reader? = null

    /**
     * Downloads the database, replacing any existing copy. The download is verified to actually
     * be a readable database before it replaces anything, so a captive portal's login page (or
     * a truncated transfer) leaves a previously working copy untouched. Throws on any failure.
     */
    suspend fun download() = withContext(Dispatchers.IO) {
        val temp = File(directory, "$FILE_NAME.tmp")
        try {
            httpClient.newCall(Request.Builder().url(DATABASE_URL).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("GeoIP download returned HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty GeoIP download")
                body.byteStream().use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > MAX_DATABASE_BYTES) throw IOException("GeoIP download is implausibly large")
                            output.write(buffer, 0, read)
                        }
                    }
                }
            }
            Reader(temp, Reader.FileMode.MEMORY_MAPPED).close()
            mutex.withLock {
                closeReader()
                Files.move(
                    temp.toPath(),
                    databaseFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
                _downloaded.value = true
            }
        } finally {
            temp.delete()
        }
    }

    /** Removes the database; peers fall back to whatever country their torrent client reports. */
    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeReader()
            databaseFile.delete()
            _downloaded.value = false
        }
    }

    /**
     * Fills in [Peer.countryCode] from the database, which takes precedence over a country the
     * torrent client reported (a daemon's bundled GeoIP data is of unknown age); a peer the
     * database has no answer for keeps the client's values. Returns [peers] untouched when no
     * database is downloaded.
     */
    suspend fun withCountries(peers: List<Peer>): List<Peer> {
        if (!_downloaded.value || peers.isEmpty()) return peers
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                // Re-checked under the lock: a clear() from Settings can slip in between the fast
                // path above and here, and the reader must not be opened on a deleted file
                if (!_downloaded.value) return@withLock peers
                val reader = openReader() ?: return@withLock peers
                peers.map { peer ->
                    // The client's country name (qBittorrent only) would no longer match an
                    // overridden code, so it goes too rather than describe a different country
                    lookup(reader, peer.ip)?.let { peer.copy(countryCode = it, countryName = null) } ?: peer
                }
            }
        }
    }

    private fun lookup(reader: Reader, ip: String): String? {
        // Only ever resolve a literal address; a hostname would otherwise trigger a DNS lookup
        if (!InetAddresses.isNumericAddress(ip)) return null
        return try {
            val record = reader.get(InetAddresses.parseNumericAddress(ip), Map::class.java)
            val country = record?.get("country") as? Map<*, *>
            (country?.get("iso_code") as? String)?.uppercase()?.takeIf { it.length == 2 }
        } catch (e: Exception) {
            null
        }
    }

    /** Memory-mapped rather than read onto the heap, so an open database costs no Java heap. */
    private fun openReader(): Reader? {
        reader?.let { return it }
        return try {
            Reader(databaseFile, Reader.FileMode.MEMORY_MAPPED).also { reader = it }
        } catch (e: IOException) {
            ErrorLog.log("GeoIp", "Could not open the country database", e)
            null
        }
    }

    private fun closeReader() {
        try {
            reader?.close()
        } catch (e: IOException) {
            // Nothing useful to do; the file is about to be replaced or deleted anyway
        }
        reader = null
    }

    companion object {
        const val DATABASE_URL = "https://downloads.ip66.dev/db/ip66.mmdb"
        private const val FILE_NAME = "ip66.mmdb"

        /** The real file is ~18 MB; this only stops a misbehaving server from filling the device. */
        private const val MAX_DATABASE_BYTES = 128L * 1024 * 1024
    }
}
