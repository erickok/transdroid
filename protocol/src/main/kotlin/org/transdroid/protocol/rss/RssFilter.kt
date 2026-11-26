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

/**
 * A feed's title filters, as Transdroid 2 had them: each is a list of |-separated words, matched
 * case-insensitively anywhere in an item's title. With an [include] filter, only items matching
 * one of its words are kept; items matching any [exclude] word are dropped.
 *
 * Words are not trimmed, like in v2, so a space can be part of one (" CAM ").
 */
class RssFilter(include: String, exclude: String) {

    private val includeWords = words(include)
    private val excludeWords = words(exclude)

    val isEmpty: Boolean
        get() = includeWords.isEmpty() && excludeWords.isEmpty()

    fun matches(title: String): Boolean =
        (includeWords.isEmpty() || includeWords.any { title.contains(it, ignoreCase = true) }) &&
            excludeWords.none { title.contains(it, ignoreCase = true) }

    private companion object {
        // Empty words (from "a||b" or a trailing "|") match nothing, rather than everything
        fun words(filter: String): List<String> = filter.split('|').filter { it.isNotEmpty() }
    }
}

/** The items of this channel that pass [filter]. */
fun RssChannel.filtered(filter: RssFilter): RssChannel =
    if (filter.isEmpty) this else copy(items = items.filter { filter.matches(it.title) })
