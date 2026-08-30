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
package org.transdroid.protocol.rtorrent

import java.io.InputStream
import java.util.Base64
import org.transdroid.protocol.DaemonException
import org.transdroid.protocol.internal.childElements
import org.transdroid.protocol.internal.parseXmlSafely
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Minimal XML-RPC codec covering what rTorrent needs: string, int/i4/i8, boolean, double,
 * base64, array and struct values. Values map to String, Long, Boolean, Double, ByteArray,
 * List and Map. Parsing forbids DTDs to rule out XXE.
 */
internal object XmlRpc {

    fun buildRequest(method: String, params: List<Any?>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><methodCall><methodName>")
        append(escape(method))
        append("</methodName><params>")
        params.forEach { param ->
            append("<param><value>")
            writeValue(param)
            append("</value></param>")
        }
        append("</params></methodCall>")
    }

    private fun StringBuilder.writeValue(value: Any?) {
        when (value) {
            null -> append("<string></string>")
            is String -> append("<string>").append(escape(value)).append("</string>")
            is Int, is Long -> append("<i8>").append(value).append("</i8>")
            is Boolean -> append("<boolean>").append(if (value) "1" else "0").append("</boolean>")
            is Double -> append("<double>").append(value).append("</double>")
            is ByteArray -> append("<base64>").append(Base64.getEncoder().encodeToString(value)).append("</base64>")
            is List<*> -> {
                append("<array><data>")
                value.forEach {
                    append("<value>")
                    writeValue(it)
                    append("</value>")
                }
                append("</data></array>")
            }
            else -> throw IllegalArgumentException("Unsupported XML-RPC type: ${value::class}")
        }
    }

    /** Parses a methodResponse, returning its single value; XML-RPC faults become exceptions. */
    fun parseResponse(xml: String): Any? = parseResponse { parseXmlSafely(xml) }

    /**
     * Same as [parseResponse], parsing directly off the response body stream instead of a
     * pre-buffered String — matters for a large multicall reply (e.g. thousands of torrents).
     */
    fun parseResponse(stream: InputStream): Any? = parseResponse { parseXmlSafely(stream) }

    private inline fun parseResponse(parseDocument: () -> Document): Any? {
        val document = try {
            parseDocument()
        } catch (e: Exception) {
            throw DaemonException.UnexpectedResponse("Not an XML-RPC response", e)
        }
        val root = document.documentElement
        if (root.tagName != "methodResponse") {
            throw DaemonException.UnexpectedResponse("Not an XML-RPC methodResponse")
        }
        root.childElements().firstOrNull { it.tagName == "fault" }?.let { fault ->
            val value = fault.childElements().firstOrNull { it.tagName == "value" }?.let(::parseValue)
            val message = (value as? Map<*, *>)?.get("faultString")?.toString() ?: "unknown fault"
            throw DaemonException.UnexpectedResponse("rTorrent error: $message")
        }
        val value = root.childElements().firstOrNull { it.tagName == "params" }
            ?.childElements()?.firstOrNull { it.tagName == "param" }
            ?.childElements()?.firstOrNull { it.tagName == "value" }
            ?: throw DaemonException.UnexpectedResponse("XML-RPC response without value")
        return parseValue(value)
    }

    private fun parseValue(value: Element): Any? {
        val typed = value.childElements().firstOrNull()
            ?: return value.textContent // untyped <value> is a string
        return when (typed.tagName) {
            "string" -> typed.textContent
            "i4", "i8", "int" -> typed.textContent.trim().toLong()
            "boolean" -> typed.textContent.trim() == "1"
            "double" -> typed.textContent.trim().toDouble()
            "base64" -> Base64.getDecoder().decode(typed.textContent.trim())
            "array" -> typed.childElements().firstOrNull { it.tagName == "data" }
                ?.childElements()?.filter { it.tagName == "value" }?.map(::parseValue)
                ?: emptyList<Any?>()
            "struct" -> typed.childElements().filter { it.tagName == "member" }.associate { member ->
                val name = member.childElements().firstOrNull { it.tagName == "name" }?.textContent ?: ""
                val memberValue = member.childElements().firstOrNull { it.tagName == "value" }?.let(::parseValue)
                name to memberValue
            }
            else -> typed.textContent
        }
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
