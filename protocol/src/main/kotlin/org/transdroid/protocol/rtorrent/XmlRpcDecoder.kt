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
@file:OptIn(ExperimentalSerializationApi::class)

package org.transdroid.protocol.rtorrent

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader

/**
 * Pull-style cursor over an XML-RPC document: yields only the element and text events the
 * decoders care about, and refuses a DOCTYPE outright - XML-RPC never has one, so any DTD is
 * hostile or broken input (the reader itself also never expands a declared entity).
 */
internal class XmlRpcCursor(private val reader: XmlReader) {

    /** The next start/end element, skipping whitespace, comments and processing instructions. */
    fun nextTag(): EventType {
        while (reader.hasNext()) {
            when (val event = reader.next()) {
                EventType.START_ELEMENT, EventType.END_ELEMENT -> return event
                EventType.TEXT, EventType.CDSECT, EventType.ENTITY_REF ->
                    if (reader.text.isNotBlank()) throw SerializationException("Unexpected text '${reader.text.take(40)}'")
                EventType.DOCDECL -> throw SerializationException("DOCTYPE is not allowed in XML-RPC")
                else -> Unit
            }
        }
        throw SerializationException("Unexpected end of XML-RPC document")
    }

    val localName: String get() = reader.localName

    fun requireStart(name: String) {
        if (nextTag() != EventType.START_ELEMENT || localName != name) {
            throw SerializationException("Expected <$name> in XML-RPC document")
        }
    }

    fun requireEnd(name: String) {
        if (nextTag() != EventType.END_ELEMENT || localName != name) {
            throw SerializationException("Expected </$name> in XML-RPC document")
        }
    }

    /**
     * The text up to the next start or end element; the caller learns which from [lastEvent].
     * Entity and character references arrive already expanded.
     */
    fun readText(): String {
        // Nearly always a single text event (or none), so only build a combined string when a
        // value's text really arrives in pieces - this runs once per value of a large reply
        var single: String? = null
        var combined: StringBuilder? = null
        var whitespace: StringBuilder? = null
        while (reader.hasNext()) {
            when (val event = reader.next()) {
                EventType.TEXT, EventType.CDSECT, EventType.ENTITY_REF -> {
                    val piece = reader.text
                    when {
                        single == null && combined == null && whitespace == null -> single = piece
                        else -> {
                            combined = (combined ?: StringBuilder().apply {
                                whitespace?.let { append(it) }
                                single?.let { append(it) }
                            }).append(piece)
                            single = null
                            whitespace = null
                        }
                    }
                }
                // Whitespace only matters as part of a text value; it's buffered lazily so the
                // common case of whitespace between elements never materializes it
                EventType.IGNORABLE_WHITESPACE -> {
                    val piece = reader.text
                    if (single == null && combined == null) {
                        whitespace = (whitespace ?: StringBuilder()).append(piece)
                    } else {
                        combined = (combined ?: StringBuilder().append(single)).append(piece)
                        single = null
                    }
                }
                EventType.START_ELEMENT, EventType.END_ELEMENT -> {
                    lastEvent = event
                    return combined?.toString() ?: single ?: whitespace?.toString() ?: ""
                }
                EventType.DOCDECL -> throw SerializationException("DOCTYPE is not allowed in XML-RPC")
                else -> Unit
            }
        }
        throw SerializationException("Unexpected end of XML-RPC document")
    }

    var lastEvent: EventType? = null
        private set

    /** Skips past the end of the element whose start tag was just read, including all its content. */
    fun skipElement() {
        var depth = 1
        while (depth > 0) {
            when (nextTagOrText()) {
                EventType.START_ELEMENT -> depth++
                EventType.END_ELEMENT -> depth--
                else -> Unit
            }
        }
    }

    private fun nextTagOrText(): EventType {
        if (!reader.hasNext()) throw SerializationException("Unexpected end of XML-RPC document")
        return reader.next().also {
            if (it == EventType.DOCDECL) throw SerializationException("DOCTYPE is not allowed in XML-RPC")
        }
    }
}

/**
 * Decodes one XML-RPC `<value>` (the cursor sits just after its start tag) into a kotlinx
 * serializable type, straight off the stream:
 * - scalars (`string`, `i4`/`int`/`i8`, `boolean`, `double`, untyped text) into primitives,
 *   as tolerantly as rTorrent's loosely typed replies need: a number read from a string value
 *   that isn't one is 0, like a missing field;
 * - `<array>` into a list, or into a class **by position** - a multicall row, whose values
 *   come back in the order the commands were requested, so a row class lists its properties
 *   in request order (see [multicallCommands]) and extra values are skipped;
 * - `<struct>` into a class by member name, skipping unknown members (e.g. a fault).
 */
internal class XmlRpcValueDecoder(private val cursor: XmlRpcCursor) : AbstractDecoder() {

    override val serializersModule: SerializersModule = EmptySerializersModule()

    /** The value's type element (e.g. "i8"), or null for an untyped text value. */
    private var type: String? = null
    private var untypedText: String? = null
    private var opened = false

    private fun open() {
        if (opened) return
        opened = true
        val text = cursor.readText()
        if (cursor.lastEvent == EventType.START_ELEMENT) {
            if (text.isNotBlank()) throw SerializationException("Mixed content in XML-RPC value")
            type = cursor.localName
        } else {
            untypedText = text // </value> already consumed
        }
    }

    /** Reads a scalar's text and consumes the rest of the value; null for an array or struct. */
    private fun scalarText(): String? {
        open()
        val tag = type ?: return untypedText.orEmpty()
        if (tag == "array" || tag == "struct") {
            cursor.skipElement()
            cursor.requireEnd("value")
            return null
        }
        val text = cursor.readText()
        if (cursor.lastEvent == EventType.START_ELEMENT) {
            throw SerializationException("Unexpected element inside XML-RPC <$tag>")
        }
        cursor.requireEnd("value")
        return text
    }

    /** Skips this value entirely, whatever it holds. */
    fun skipValue() {
        open()
        if (type != null) {
            cursor.skipElement()
            cursor.requireEnd("value")
        }
    }

    override fun decodeString(): String = scalarText().orEmpty()

    override fun decodeLong(): Long {
        open()
        val tag = type
        val text = scalarText()?.trim() ?: return 0L
        return when (tag) {
            "i4", "i8", "int" -> text.toLongOrNull()
                ?: throw SerializationException("Not an XML-RPC integer: '${text.take(40)}'")
            "boolean" -> if (text == "1") 1L else 0L
            "double" -> text.toDoubleOrNull()?.toLong() ?: 0L
            else -> text.toLongOrNull() ?: 0L
        }
    }

    override fun decodeInt(): Int = decodeLong().toInt()

    override fun decodeShort(): Short = decodeLong().toInt().toShort()

    override fun decodeByte(): Byte = decodeLong().toInt().toByte()

    override fun decodeBoolean(): Boolean {
        val text = scalarText()?.trim() ?: return false
        return text == "1" || text.equals("true", ignoreCase = true) || (text.toLongOrNull() ?: 0L) != 0L
    }

    override fun decodeDouble(): Double = scalarText()?.trim()?.toDoubleOrNull() ?: 0.0

    override fun decodeFloat(): Float = decodeDouble().toFloat()

    override fun decodeChar(): Char = decodeString().firstOrNull() ?: '\u0000'

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int =
        enumDescriptor.getElementIndex(decodeString())

    /** XML-RPC has no null, except the common `<nil/>` extension. */
    override fun decodeNotNullMark(): Boolean {
        open()
        return type != "nil"
    }

    override fun decodeNull(): Nothing? {
        skipValue()
        return null
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
        throw SerializationException("A single XML-RPC value has no elements")

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        open()
        return when (type) {
            "array" -> {
                cursor.requireStart("data")
                ArrayDecoder(cursor, positional = descriptor.kind != StructureKind.LIST)
            }
            "struct" -> StructDecoder(cursor)
            else -> throw SerializationException("Expected an XML-RPC array or struct, got <${type ?: "text"}>")
        }
    }
}

/** The shared element-by-element delegation of the array and struct decoders. */
private abstract class ContainerDecoder(protected val cursor: XmlRpcCursor) : AbstractDecoder() {

    override val serializersModule: SerializersModule = EmptySerializersModule()

    /** The element value the last [decodeElementIndex] positioned on. */
    protected var current: XmlRpcValueDecoder? = null

    private val value: XmlRpcValueDecoder
        get() = current ?: throw SerializationException("No current XML-RPC element")

    override fun decodeString() = value.decodeString()
    override fun decodeLong() = value.decodeLong()
    override fun decodeInt() = value.decodeInt()
    override fun decodeShort() = value.decodeShort()
    override fun decodeByte() = value.decodeByte()
    override fun decodeBoolean() = value.decodeBoolean()
    override fun decodeDouble() = value.decodeDouble()
    override fun decodeFloat() = value.decodeFloat()
    override fun decodeChar() = value.decodeChar()
    override fun decodeEnum(enumDescriptor: SerialDescriptor) = value.decodeEnum(enumDescriptor)
    override fun decodeNotNullMark() = value.decodeNotNullMark()
    override fun decodeNull() = value.decodeNull()
    override fun beginStructure(descriptor: SerialDescriptor) = value.beginStructure(descriptor)
}

/** An `<array><data>` of values: list elements, or a row class's properties by position. */
private class ArrayDecoder(cursor: XmlRpcCursor, private val positional: Boolean) : ContainerDecoder(cursor) {

    private var index = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (true) {
            if (cursor.nextTag() == EventType.END_ELEMENT) return CompositeDecoder.DECODE_DONE // </data>
            if (cursor.localName != "value") throw SerializationException("Expected <value> in XML-RPC array")
            val element = XmlRpcValueDecoder(cursor)
            if (positional && index >= descriptor.elementsCount) {
                element.skipValue() // a trailing value the row class doesn't declare
                continue
            }
            current = element
            return index++
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        cursor.requireEnd("array")
        cursor.requireEnd("value")
    }
}

/** A `<struct>` of named members, matched to the class's property names. */
private class StructDecoder(cursor: XmlRpcCursor) : ContainerDecoder(cursor) {

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (true) {
            if (current != null) {
                cursor.requireEnd("member")
                current = null
            }
            if (cursor.nextTag() == EventType.END_ELEMENT) return CompositeDecoder.DECODE_DONE // </struct>
            if (cursor.localName != "member") throw SerializationException("Expected <member> in XML-RPC struct")
            cursor.requireStart("name")
            val name = cursor.readText().trim()
            if (cursor.lastEvent != EventType.END_ELEMENT) throw SerializationException("Unexpected element in <name>")
            cursor.requireStart("value")
            val element = XmlRpcValueDecoder(cursor)
            val index = descriptor.getElementIndex(name)
            if (index == CompositeDecoder.UNKNOWN_NAME) {
                element.skipValue()
                cursor.requireEnd("member")
                continue
            }
            current = element
            return index
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (current != null) cursor.requireEnd("member")
        cursor.requireEnd("value")
    }
}

/**
 * The multicall command list for a row class: its serial names, in declaration order, which
 * is also the order its values come back in - so requested commands and decoded properties
 * can never drift apart.
 */
internal fun multicallCommands(row: SerialDescriptor): List<String> = List(row.elementsCount) { row.getElementName(it) }
