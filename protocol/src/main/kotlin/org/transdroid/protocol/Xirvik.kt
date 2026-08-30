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
package org.transdroid.protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Xirvik seedboxes run rTorrent behind a shared nginx/ruTorrent front-end; each account's XML-RPC
 * endpoint lives at a per-user mount path (like /username/RPC2) that can't be derived from the
 * hostname alone. Xirvik publishes it at a fixed diagnostic URL every account can read once
 * authenticated - this fetches it, mirroring the lookup the old Transdroid 2
 * XirvikSettingsActivity/RetrieveXirvikAutoConfTask performed.
 */
object Xirvik {

    /** Returns the account's SCGI/RPC mount path (e.g. "/username/RPC2"), or null if it can't be determined. */
    suspend fun detectRpcPath(httpClient: OkHttpClient, server: String, username: String, password: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://$server/browsers_addons/transdroid_autoconf.txt")
                    .header("Authorization", Credentials.basic(username, password))
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val folder = response.body?.string()?.trim().orEmpty()
                    // A login portal or error page answers with a full HTML/XML document instead of the
                    // plain-text mount path.
                    folder.takeIf { it.isNotEmpty() && !it.startsWith("<") }
                }
            } catch (e: Exception) {
                null
            }
        }
}
