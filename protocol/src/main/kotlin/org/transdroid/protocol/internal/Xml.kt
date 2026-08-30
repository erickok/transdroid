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
package org.transdroid.protocol.internal

import java.io.InputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

/** Parses XML from an untrusted server with DTDs disabled, ruling out XXE. */
internal fun parseXmlSafely(xml: String): Document = safeDocumentBuilder().parse(InputSource(StringReader(xml)))

/**
 * Parses XML directly off a response body stream, without first buffering the whole
 * document into a String — the response-size-scaling case (e.g. a multicall reply
 * listing thousands of torrents).
 */
internal fun parseXmlSafely(stream: InputStream): Document = safeDocumentBuilder().parse(stream)

private fun safeDocumentBuilder(): DocumentBuilder =
    DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        isExpandEntityReferences = false
    }.newDocumentBuilder()

internal fun Element.childElements(): List<Element> {
    val result = mutableListOf<Element>()
    var child: Node? = firstChild
    while (child != null) {
        if (child is Element) result.add(child)
        child = child.nextSibling
    }
    return result
}

/** The text of the first direct child element with this (possibly prefixed) tag name. */
internal fun Element.childText(tagName: String): String? =
    childElements().firstOrNull { it.tagName == tagName || it.tagName.substringAfter(':') == tagName }
        ?.textContent?.trim()?.takeIf { it.isNotEmpty() }
