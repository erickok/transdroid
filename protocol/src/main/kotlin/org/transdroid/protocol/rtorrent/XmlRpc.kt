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
import java.io.PushbackInputStream
import java.util.Base64
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import nl.adaptivity.xmlutil.core.KtXmlReader
import org.transdroid.protocol.DaemonException

/**
 * Minimal XML-RPC codec covering what rTorrent needs. Requests are built as a string (they're
 * tiny); responses are decoded straight off the stream into kotlinx serializable types by
 * [XmlRpcValueDecoder] on xmlutil's pure-Kotlin [KtXmlReader] - the same parser on the JVM and
 * on Android, never building a document tree however large the reply. The reader never
 * expands a declared entity and the decoder refuses any DOCTYPE, ruling out XXE.
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

    /**
     * Decodes a methodResponse directly off the response body stream into [result]; XML-RPC
     * faults become exceptions. Peeks a few bytes first (without consuming them) so a genuine
     * parse failure can report what the server actually sent instead of a content-free "not
     * XML" message - seedboxes (e.g. Xirvik/ruTorrent behind nginx) commonly answer a wrong
     * mount path or an expired session with an HTML page instead of a fault response.
     */
    fun <T> decodeResponse(stream: InputStream, result: DeserializationStrategy<T>): T {
        val pushback = PushbackInputStream(stream, PEEK_BYTES)
        val peek = ByteArray(PEEK_BYTES)
        val peeked = pushback.read(peek).coerceAtLeast(0)
        if (peeked > 0) pushback.unread(peek, 0, peeked)
        try {
            val cursor = XmlRpcCursor(KtXmlReader(pushback, expandEntities = true))
            cursor.requireStart("methodResponse")
            cursor.nextTag()
            when (cursor.localName) {
                "fault" -> {
                    cursor.requireStart("value")
                    val fault = XmlRpcValueDecoder(cursor).decodeSerializableValue(Fault.serializer())
                    throw DaemonException.UnexpectedResponse("rTorrent error: ${fault.faultString ?: "unknown fault"}")
                }
                "params" -> {
                    cursor.requireStart("param")
                    cursor.requireStart("value")
                    return XmlRpcValueDecoder(cursor).decodeSerializableValue(result)
                }
                else -> throw SerializationException("Not an XML-RPC methodResponse")
            }
        } catch (e: DaemonException) {
            throw e
        } catch (e: Exception) {
            throw DaemonException.UnexpectedResponse(parseFailureHint(String(peek, 0, peeked, Charsets.UTF_8), e), e)
        }
    }

    /** For calls whose reply value isn't needed: skips it, whatever it holds. */
    object Ignored : DeserializationStrategy<Unit> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("XmlRpcIgnored", PrimitiveKind.STRING)
        override fun deserialize(decoder: Decoder) = (decoder as XmlRpcValueDecoder).skipValue()
    }

    @Serializable
    private data class Fault(val faultCode: Long = 0, val faultString: String? = null)

    /** Turns whatever a genuinely unparseable response started with into an actionable message. */
    private fun parseFailureHint(peeked: String, parseError: Exception): String {
        val snippet = peeked.trim()
        return when {
            snippet.isEmpty() -> "Empty response from rTorrent"
            Regex("^<!doctype html|^<html", RegexOption.IGNORE_CASE).containsMatchIn(snippet) ->
                "The server answered with a web page instead of XML-RPC — a login portal may be " +
                    "in front of it, or the SCGI/RPC mount path is wrong"
            // Looks like XML-RPC but genuinely failed to parse - the underlying parser error
            // (often with a line/column) plus a snippet is far more actionable than a flat
            // "not XML" message for tracking down a malformed byte deep in a large response.
            else -> "Not an XML-RPC response (${parseError.message}): " +
                "${snippet.take(200).replace(Regex("\\s+"), " ")}"
        }
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    /** Bytes peeked (without consuming) to diagnose a non-XML response before parsing fails. */
    private const val PEEK_BYTES = 512
}
