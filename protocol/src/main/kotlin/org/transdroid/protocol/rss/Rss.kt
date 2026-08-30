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
package org.transdroid.protocol.rss

import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.transdroid.protocol.DaemonException
import org.transdroid.protocol.internal.childElements
import org.transdroid.protocol.internal.childText
import org.transdroid.protocol.internal.executeOnIo
import org.transdroid.protocol.internal.parseXmlSafely
import org.w3c.dom.Element

data class RssItem(
    val title: String,
    /** Web page link of the item, if any. */
    val link: String?,
    /** The best link to feed to a torrent client: enclosure, magnet link, or the item link. */
    val torrentUrl: String?,
    /** Publication time as unix seconds, or null when the feed doesn't provide one. */
    val timestamp: Long?,
)

data class RssChannel(
    val title: String,
    val items: List<RssItem>,
)

/** Fetches and parses RSS 2.0 and Atom feeds, tolerant of the dialects torrent sites use. */
class RssFetcher(private val httpClient: OkHttpClient) {

    suspend fun fetch(url: String): RssChannel {
        val request = Request.Builder().url(url).get().build()
        val body = httpClient.executeOnIo(request).use { response ->
            if (!response.isSuccessful) {
                throw DaemonException.UnexpectedResponse("Feed returned HTTP ${response.code}")
            }
            // executeOnIo only wraps the round-trip up to receiving headers; the body still
            // streams off the same connection, so reading it can still block on the socket
            // and must stay on IO too, or it risks a NetworkOnMainThreadException on whatever
            // dispatcher called this.
            withContext(Dispatchers.IO) { response.body?.string().orEmpty() }
        }
        return parse(body)
    }

    fun parse(xml: String): RssChannel {
        val root = try {
            parseXmlSafely(xml).documentElement
        } catch (e: Exception) {
            throw DaemonException.UnexpectedResponse("Not a valid feed", e)
        }
        return when {
            root.tagName == "rss" || root.tagName == "rdf:RDF" -> parseRss(root)
            root.tagName == "feed" -> parseAtom(root)
            else -> throw DaemonException.UnexpectedResponse("Not an RSS or Atom feed")
        }
    }

    private fun parseRss(root: Element): RssChannel {
        val channel = root.childElements().firstOrNull { it.tagName == "channel" } ?: root
        // RSS 2.0 nests items in the channel; RDF (RSS 1.0) puts them next to it
        val itemParents = if (channel === root) listOf(root) else listOf(channel, root)
        val items = itemParents.flatMap { parent -> parent.childElements().filter { it.tagName == "item" } }
            .map { parseRssItem(it) }
        return RssChannel(title = channel.childText("title") ?: "", items = items)
    }

    private fun parseRssItem(item: Element): RssItem {
        val link = item.childText("link")
        val enclosure = item.childElements()
            .firstOrNull { it.tagName == "enclosure" }
            ?.getAttribute("url")?.takeIf { it.isNotBlank() }
        val magnet = item.childText("magnetURI")?.takeIf { it.startsWith("magnet:") }
        return RssItem(
            title = item.childText("title") ?: "",
            link = link,
            torrentUrl = magnet ?: enclosure ?: link,
            timestamp = parseRssDate(item.childText("pubDate") ?: item.childText("date")),
        )
    }

    private fun parseAtom(root: Element): RssChannel {
        val items = root.childElements().filter { it.tagName == "entry" }.map { entry ->
            val links = entry.childElements().filter { it.tagName == "link" }
            val enclosure = links.firstOrNull { it.getAttribute("rel") == "enclosure" }?.getAttribute("href")
            val alternate = links.firstOrNull { it.getAttribute("rel") in listOf("", "alternate") }?.getAttribute("href")
            RssItem(
                title = entry.childText("title") ?: "",
                link = alternate?.takeIf { it.isNotBlank() },
                torrentUrl = (enclosure ?: alternate)?.takeIf { it.isNotBlank() },
                timestamp = parseIsoDate(entry.childText("updated") ?: entry.childText("published")),
            )
        }
        return RssChannel(title = root.childText("title") ?: "", items = items)
    }

    private fun parseRssDate(text: String?): Long? {
        if (text == null) return null
        return try {
            ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond()
        } catch (e: Exception) {
            parseIsoDate(text)
        }
    }

    private fun parseIsoDate(text: String?): Long? {
        if (text == null) return null
        return try {
            OffsetDateTime.parse(text).toEpochSecond()
        } catch (e: Exception) {
            null
        }
    }
}
