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
package org.transdroid.update

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** The newest released version, as announced in [AppUpdates.LATEST_URL]. */
data class LatestRelease(val versionCode: Int, val versionName: String)

/**
 * Finds out whether a newer Transdroid has been released, for builds distributed outside an app
 * store (see the `updatecheck_available` flavor bool). The announcement is a one-line
 * `versionCode|versionName` file on the master branch; Transdroid 2 installs read the very same
 * file, so bumping it at release time notifies both.
 */
object AppUpdates {

    const val LATEST_URL = "https://raw.githubusercontent.com/erickok/transdroid/master/latest-app.html"
    const val DOWNLOAD_URL = "https://www.transdroid.org/latest"

    /** Parses `versionCode|versionName`; null for anything else, like an error page. */
    fun parse(text: String): LatestRelease? {
        val parts = text.trim().split('|')
        if (parts.size != 2) return null
        val code = parts[0].trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
        val name = parts[1].trim().takeIf { it.isNotEmpty() && it.length <= 40 && '<' !in it } ?: return null
        return LatestRelease(code, name)
    }

    /**
     * Whether to notify about [release]: it's newer than the installed build, and the user
     * wasn't already notified about this version ([notifiedVersionCode]), so a dismissed
     * notification doesn't come back every day.
     */
    fun shouldNotify(release: LatestRelease, installedVersionCode: Int, notifiedVersionCode: Int?): Boolean =
        release.versionCode > installedVersionCode && release.versionCode != notifiedVersionCode

    suspend fun fetch(httpClient: OkHttpClient): LatestRelease = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(LATEST_URL).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Update check failed with HTTP ${response.code}")
            // The real file is a few bytes; never read an unexpected large reply
            val text = response.body?.source()?.let { source ->
                source.request(MAX_BYTES)
                source.buffer.readUtf8(minOf(source.buffer.size, MAX_BYTES))
            }.orEmpty()
            parse(text) ?: throw IOException("Unexpected update check reply")
        }
    }

    private const val MAX_BYTES = 1024L
}
