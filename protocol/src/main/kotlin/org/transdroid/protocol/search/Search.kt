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
package org.transdroid.protocol.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.transdroid.protocol.DaemonException
import org.transdroid.protocol.internal.childElements
import org.transdroid.protocol.internal.childText
import org.transdroid.protocol.internal.executeOnIo
import org.transdroid.protocol.internal.parseXmlSafely

data class SearchResult(
    val title: String,
    val torrentUrl: String,
    val detailsUrl: String?,
    val sizeBytes: Long?,
    val seeders: Int?,
    val leechers: Int?,
    val timestamp: Long?,
    /** The underlying tracker this result came from, e.g. Jackett's per-item <jackettindexer> tag
     *  on an aggregated "all indexers" feed. Null for a single-tracker feed or a non-Jackett server. */
    val indexerName: String? = null,
)

/**
 * The in-app search extension point. Implementations turn a query into a list of results
 * whose [SearchResult.torrentUrl] can be fed straight to a [org.transdroid.protocol.DaemonAdapter].
 */
interface SearchProvider {
    suspend fun search(query: String): List<SearchResult>
}

/**
 * A [SearchProvider] speaking the Torznab API — the de-facto standard served by Jackett,
 * Prowlarr and NZBHydra, which proxy hundreds of indexers behind one stable XML interface.
 * Point it at an endpoint like http://host:9117/api/v2.0/indexers/all/results/torznab.
 *
 * [username]/[password], when both given, are sent as HTTP Basic Auth - needed when the
 * Torznab endpoint sits behind its own reverse-proxy login (e.g. a seedbox's member area)
 * in front of the indexer's own apikey check.
 */
class TorznabProvider(
    private val endpointUrl: String,
    private val apiKey: String?,
    private val httpClient: OkHttpClient,
    private val username: String? = null,
    private val password: String? = null,
) : SearchProvider {

    override suspend fun search(query: String): List<SearchResult> {
        val url = endpointUrl.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("t", "search")
            ?.addQueryParameter("q", query)
            ?.apply { if (!apiKey.isNullOrBlank()) addQueryParameter("apikey", apiKey) }
            ?.build()
            ?: throw DaemonException.UnexpectedResponse("Invalid Torznab endpoint URL")
        val requestBuilder = Request.Builder().url(url).get()
        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            requestBuilder.header("Authorization", Credentials.basic(username, password))
        }
        val body = httpClient.executeOnIo(requestBuilder.build()).use { response ->
            when {
                response.code == 401 || response.code == 403 ->
                    throw DaemonException.Authentication(
                        if (!username.isNullOrBlank()) {
                            "The indexer rejected the API key or login"
                        } else {
                            "The indexer rejected the API key"
                        }
                    )
                !response.isSuccessful ->
                    throw DaemonException.UnexpectedResponse("The indexer returned HTTP ${response.code}")
            }
            // executeOnIo only wraps the round-trip up to receiving headers; the body still
            // streams off the same connection, so reading it can still block on the socket
            // and must stay on IO too, or it risks a NetworkOnMainThreadException on whatever
            // dispatcher called this.
            withContext(Dispatchers.IO) { response.body?.string().orEmpty() }
        }
        return parse(body)
    }

    internal fun parse(xml: String): List<SearchResult> {
        val root = try {
            parseXmlSafely(xml).documentElement
        } catch (e: Exception) {
            throw DaemonException.UnexpectedResponse("Not a valid Torznab response", e)
        }
        if (root.tagName == "error") {
            throw DaemonException.UnexpectedResponse(
                "Indexer error: " + (root.getAttribute("description").ifBlank { "code ${root.getAttribute("code")}" })
            )
        }
        val channel = root.childElements().firstOrNull { it.tagName == "channel" }
            ?: throw DaemonException.UnexpectedResponse("Not a Torznab search response")
        return channel.childElements().filter { it.tagName == "item" }.mapNotNull { item ->
            val attrs = item.childElements()
                .filter { it.tagName.substringAfter(':') == "attr" }
                .associate { it.getAttribute("name") to it.getAttribute("value") }
            val enclosure = item.childElements()
                .firstOrNull { it.tagName == "enclosure" }
                ?.getAttribute("url")?.takeIf { it.isNotBlank() }
            val link = item.childText("link")
            val magnet = attrs["magneturl"]?.takeIf { it.startsWith("magnet:") }
            val torrentUrl = magnet ?: enclosure ?: link ?: return@mapNotNull null
            SearchResult(
                title = item.childText("title") ?: return@mapNotNull null,
                torrentUrl = torrentUrl,
                detailsUrl = item.childText("comments") ?: item.childText("guid"),
                sizeBytes = item.childText("size")?.toLongOrNull() ?: attrs["size"]?.toLongOrNull(),
                seeders = attrs["seeders"]?.toIntOrNull(),
                leechers = attrs["peers"]?.toIntOrNull()?.let { peers ->
                    val seeders = attrs["seeders"]?.toIntOrNull() ?: 0
                    (peers - seeders).coerceAtLeast(0)
                },
                timestamp = item.childText("pubDate")?.let { pubDate ->
                    try {
                        java.time.ZonedDateTime
                            .parse(pubDate, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                            .toEpochSecond()
                    } catch (e: Exception) {
                        null
                    }
                },
                indexerName = item.childText("jackettindexer"),
            )
        }
    }
}
