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
package org.transdroid.debug

import kotlin.random.Random
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.transdroid.protocol.DaemonAdapter
import org.transdroid.protocol.DaemonConfig
import org.transdroid.protocol.DaemonException
import org.transdroid.protocol.DaemonType
import org.transdroid.protocol.FilePriority
import org.transdroid.protocol.Torrent
import org.transdroid.protocol.TorrentFile
import org.transdroid.protocol.TorrentStatus
import org.transdroid.protocol.Tracker
import org.transdroid.protocol.TrackerStatus

/**
 * A [DaemonAdapter] that never touches the network: it seeds itself with a random set of
 * generically-titled torrents ("Movie 3", "Book 12", ...) and mutates them locally in response to
 * every action, so the app is fully and instantly usable - for screenshots, UI work, and manual
 * testing - with no real torrent client anywhere. Only reachable via [DebugTools.adapterFor];
 * this whole file lives under app/src/debug, a source set Gradle excludes from every release
 * variant by construction, so it is never compiled into, let alone reachable from, a release APK.
 *
 * Titles are always a generic category plus a number - never a real or fictional work - and the
 * couple of fake tracker URLs point at IANA's reserved example.org/example.com domains.
 *
 * One instance holds its own in-memory state for as long as [org.transdroid.AppContainer] keeps
 * it cached (i.e. for as long as this remains the selected server), same as a real adapter;
 * state resets whenever a fresh instance is created (app restart, or switching servers and back).
 */
class DummyDaemonAdapter : DaemonAdapter {

    override val config = DaemonConfig(type = DaemonType.TRANSMISSION, host = "dummy", port = 0)

    private val categories = listOf("Movie", "Documentary", "TV Show", "Book", "Album", "App", "ISO Image")
    private val labelPool = listOf("video", "docs", "books", "music", "software")
    private val statusPool = listOf(
        TorrentStatus.SEEDING, TorrentStatus.SEEDING, TorrentStatus.SEEDING,
        TorrentStatus.DOWNLOADING, TorrentStatus.DOWNLOADING, TorrentStatus.DOWNLOADING,
        TorrentStatus.PAUSED, TorrentStatus.QUEUED, TorrentStatus.ERROR,
    )

    // LinkedHashMap: listTorrents() returns these in a stable, deterministic-per-session order.
    private val torrents = LinkedHashMap<String, Torrent>()
    private val filesByTorrent = mutableMapOf<String, MutableList<TorrentFile>>()
    private val trackersByTorrent = mutableMapOf<String, MutableList<Tracker>>()

    /** Torrent id -> status to restore on the next [listTorrents] poll, simulating [checkData]. */
    private val statusBeforeRecheck = mutableMapOf<String, TorrentStatus>()

    private var nextId = 1
    private var altSpeedEnabled = false

    // All calls are expected to come from one server's sequential poll/action loop, but a real
    // adapter is safe to call from any dispatcher - this just makes that guarantee actually hold.
    private val mutex = Mutex()

    init {
        repeat(Random.nextInt(24, 41)) { seedTorrent() }
    }

    override suspend fun testConnection(): String = "Dummy 1.0 (offline test data)"

    override suspend fun listTorrents(): List<Torrent> = mutex.withLock {
        val ids = torrents.keys.toList()
        for (id in ids) {
            torrents[id]?.let { torrents[id] = tick(it) }
        }
        torrents.values.toList()
    }

    override suspend fun addByUrl(url: String, startPaused: Boolean) = mutex.withLock {
        addGeneric(startPaused)
    }

    override suspend fun addByFile(fileName: String, contents: ByteArray, startPaused: Boolean) = mutex.withLock {
        addGeneric(startPaused)
    }

    override suspend fun start(torrentId: String) = mutex.withLock {
        mutate(torrentId) { it.copy(status = if (it.progress >= 1f) TorrentStatus.SEEDING else TorrentStatus.DOWNLOADING) }
    }

    override suspend fun pause(torrentId: String) = mutex.withLock {
        mutate(torrentId) { it.copy(status = TorrentStatus.PAUSED, downloadRate = 0, uploadRate = 0) }
    }

    override suspend fun remove(torrentId: String, deleteData: Boolean) = mutex.withLock {
        torrents.remove(torrentId)
        filesByTorrent.remove(torrentId)
        trackersByTorrent.remove(torrentId)
        Unit
    }

    override suspend fun listFiles(torrentId: String): List<TorrentFile> = mutex.withLock {
        val torrent = requireTorrent(torrentId)
        // Re-derive downloadedBytes from the torrent's current progress on every call (rather
        // than freezing it at whatever the torrent's progress was on the first listFiles() call)
        // so the files tab keeps up as tick() advances a downloading torrent between polls.
        filesByTorrent.getOrPut(torrentId) { generateFiles(torrent) }
            .map { it.copy(downloadedBytes = (it.sizeBytes * torrent.progress).toLong()) }
    }

    override suspend fun setFilePriority(torrentId: String, fileIndex: Int, priority: FilePriority) = mutex.withLock {
        val files = filesByTorrent[torrentId] ?: requireTorrent(torrentId).let { generateFiles(it) }.also {
            filesByTorrent[torrentId] = it
        }
        val index = files.indexOfFirst { it.index == fileIndex }
        if (index < 0) throw DaemonException.UnexpectedResponse("Dummy torrent $torrentId has no file $fileIndex")
        files[index] = files[index].copy(priority = priority)
    }

    override suspend fun listTrackers(torrentId: String): List<Tracker> = mutex.withLock {
        requireTorrent(torrentId)
        trackersByTorrent.getOrPut(torrentId) {
            mutableListOf(
                Tracker(
                    url = "udp://tracker.example.org:80/announce",
                    status = TrackerStatus.WORKING,
                    seeders = Random.nextInt(0, 50),
                    leechers = Random.nextInt(0, 20),
                ),
                Tracker(
                    url = "https://tracker.example.com/announce",
                    status = TrackerStatus.WORKING,
                    seeders = Random.nextInt(0, 50),
                    leechers = Random.nextInt(0, 20),
                ),
            )
        }.toList()
    }

    override suspend fun setLabel(torrentId: String, label: String) = mutex.withLock {
        mutate(torrentId) { it.copy(labels = if (label.isBlank()) emptyList() else listOf(label)) }
    }

    override suspend fun checkData(torrentId: String) = mutex.withLock {
        val torrent = requireTorrent(torrentId)
        statusBeforeRecheck[torrentId] = torrent.status
        torrents[torrentId] = torrent.copy(status = TorrentStatus.CHECKING)
    }

    override val supportsAltSpeedLimits: Boolean = true

    override suspend fun isAltSpeedLimitsEnabled(): Boolean = mutex.withLock { altSpeedEnabled }

    override suspend fun setAltSpeedLimitsEnabled(enabled: Boolean) = mutex.withLock {
        altSpeedEnabled = enabled
    }

    private fun requireTorrent(torrentId: String): Torrent =
        torrents[torrentId] ?: throw DaemonException.UnexpectedResponse("Unknown dummy torrent $torrentId")

    private fun mutate(torrentId: String, block: (Torrent) -> Torrent) {
        torrents[torrentId] = block(requireTorrent(torrentId))
    }

    /** Advances one poll's worth of simulated progress, or reverts a just-shown "Checking" flash. */
    private fun tick(torrent: Torrent): Torrent {
        statusBeforeRecheck.remove(torrent.id)?.let { previousStatus ->
            return torrent.copy(status = previousStatus)
        }
        if (torrent.status != TorrentStatus.DOWNLOADING) return torrent
        val newProgress = (torrent.progress + Random.nextFloat() * 0.05f).coerceAtMost(1f)
        val newDownloaded = (torrent.sizeBytes * newProgress).toLong()
        val finished = newProgress >= 1f
        val newDownloadRate = if (finished) {
            0L
        } else {
            (torrent.downloadRate + Random.nextLong(-50_000, 50_000)).coerceAtLeast(10_000)
        }
        return torrent.copy(
            status = if (finished) TorrentStatus.SEEDING else TorrentStatus.DOWNLOADING,
            progress = newProgress,
            downloadedBytes = newDownloaded,
            downloadRate = newDownloadRate,
            uploadRate = if (finished) Random.nextLong(5_000, 500_000) else torrent.uploadRate,
            etaSeconds = if (finished || newDownloadRate <= 0) {
                null
            } else {
                (torrent.sizeBytes - newDownloaded) / newDownloadRate
            },
        )
    }

    private fun seedTorrent() {
        val number = nextId++
        val name = "${categories.random()} $number"
        val status = statusPool.random()
        val sizeBytes = (Random.nextDouble(0.05, 8.0) * 1024.0 * 1024.0 * 1024.0).toLong()
        val progress = when (status) {
            TorrentStatus.SEEDING -> 1f
            TorrentStatus.DOWNLOADING -> Random.nextFloat().coerceIn(0.01f, 0.95f)
            TorrentStatus.QUEUED, TorrentStatus.PAUSED -> Random.nextFloat() * 0.6f
            TorrentStatus.ERROR -> Random.nextFloat() * 0.3f
            else -> 0f
        }
        val downloadedBytes = (sizeBytes * progress).toLong()
        val downloadRate = if (status == TorrentStatus.DOWNLOADING) Random.nextLong(20_000, 6_000_000) else 0L
        val uploadRate = if (status == TorrentStatus.DOWNLOADING || status == TorrentStatus.SEEDING) {
            Random.nextLong(5_000, 2_000_000)
        } else {
            0L
        }
        val uploadedBytes = (downloadedBytes * Random.nextDouble(0.0, 3.0)).toLong()
        val peersConnected = if (status.isActive) Random.nextInt(0, 40) else 0
        val seeders = if (peersConnected > 0) Random.nextInt(0, peersConnected + 1) else 0

        torrents[number.toString()] = Torrent(
            id = number.toString(),
            name = name,
            status = status,
            progress = progress,
            downloadRate = downloadRate,
            uploadRate = uploadRate,
            etaSeconds = if (status == TorrentStatus.DOWNLOADING && downloadRate > 0) {
                (sizeBytes - downloadedBytes) / downloadRate
            } else {
                null
            },
            sizeBytes = sizeBytes,
            downloadedBytes = downloadedBytes,
            uploadedBytes = uploadedBytes,
            ratio = if (downloadedBytes > 0) uploadedBytes.toFloat() / downloadedBytes else 0f,
            peersConnected = peersConnected,
            seedersConnected = seeders,
            leechersConnected = peersConnected - seeders,
            addedTimestamp = System.currentTimeMillis() / 1000 - Random.nextLong(0, 30L * 24 * 3600),
            downloadDir = "/downloads/" + name.replace(" ", "_").lowercase(),
            error = if (status == TorrentStatus.ERROR) "Dummy tracker error" else null,
            labels = if (Random.nextBoolean()) listOf(labelPool.random()) else emptyList(),
        )
    }

    private fun addGeneric(startPaused: Boolean) {
        val number = nextId++
        val sizeBytes = (Random.nextDouble(0.1, 4.0) * 1024.0 * 1024.0 * 1024.0).toLong()
        torrents[number.toString()] = Torrent(
            id = number.toString(),
            name = "${categories.random()} $number",
            status = if (startPaused) TorrentStatus.PAUSED else TorrentStatus.DOWNLOADING,
            progress = 0f,
            downloadRate = 0L,
            uploadRate = 0L,
            sizeBytes = sizeBytes,
            downloadedBytes = 0L,
            uploadedBytes = 0L,
            ratio = 0f,
            addedTimestamp = System.currentTimeMillis() / 1000,
            downloadDir = "/downloads",
        )
    }

    private fun generateFiles(torrent: Torrent): MutableList<TorrentFile> {
        val count = Random.nextInt(1, 6)
        val sizePerFile = torrent.sizeBytes / count
        return (0 until count).map { index ->
            TorrentFile(
                index = index,
                path = "${torrent.name}/file_${index + 1}.dat",
                sizeBytes = sizePerFile,
                downloadedBytes = (sizePerFile * torrent.progress).toLong(),
                priority = FilePriority.NORMAL,
            )
        }.toMutableList()
    }
}
