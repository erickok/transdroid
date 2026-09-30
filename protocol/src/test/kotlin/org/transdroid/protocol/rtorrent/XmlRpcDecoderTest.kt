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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.transdroid.protocol.DaemonException

class XmlRpcDecoderTest {

    private fun <T> decode(value: String, result: DeserializationStrategy<T>): T =
        XmlRpc.decodeResponse(
            """<?xml version="1.0" encoding="UTF-8"?>
               <methodResponse><params><param><value>$value</value></param></params></methodResponse>"""
                .byteInputStream(),
            result,
        )

    private fun decodeFails(document: String): DaemonException.UnexpectedResponse {
        try {
            XmlRpc.decodeResponse(document.byteInputStream(), String.serializer())
        } catch (expected: DaemonException.UnexpectedResponse) {
            return expected
        }
        fail("Expected DaemonException.UnexpectedResponse")
        throw AssertionError()
    }

    @Serializable
    private data class Row(
        @SerialName("x.name=") val name: String = "",
        @SerialName("x.size=") val size: Long = 0,
        @SerialName("x.flag=") val flag: Boolean = false,
        @SerialName("x.missing=") val missing: Long = -1,
    )

    @Serializable
    private data class Pair(val first: String = "", val second: Long = 0)

    @Test
    fun `scalars decode from every xml-rpc type`() {
        assertEquals("text", decode("<string>text</string>", String.serializer()))
        assertEquals("untyped text is a string", "plain", decode("plain", String.serializer()))
        assertEquals("", decode("<string/>", String.serializer()))
        assertEquals("", decode("", String.serializer()))
        assertEquals(42L, decode("<i4>42</i4>", Long.serializer()))
        assertEquals(42L, decode("<int> 42 </int>", Long.serializer()))
        assertEquals(6114656256L, decode("<i8>6114656256</i8>", Long.serializer()))
        assertEquals(true, decode("<boolean>1</boolean>", Boolean.serializer()))
        assertEquals(false, decode("<boolean>0</boolean>", Boolean.serializer()))
        assertEquals(2.5, decode("<double>2.5</double>", Double.serializer()), 0.0)
        assertEquals("a number read as a string keeps its text", "123", decode("<i8>123</i8>", String.serializer()))
        assertEquals("a non-numeric string read as a number is 0", 0L, decode("<string>n/a</string>", Long.serializer()))
    }

    @Test
    fun `entities, character references and cdata are expanded`() {
        assertEquals("a & b < c > d \"e\" 'f'", decode("<string>a &amp; b &lt; c &gt; d &quot;e&quot; &apos;f&apos;</string>", String.serializer()))
        assertEquals("Café ✓", decode("<string>Caf&#233; &#x2713;</string>", String.serializer()))
        assertEquals("<raw>", decode("<string><![CDATA[<raw>]]></string>", String.serializer()))
    }

    @Test
    fun `arrays decode into lists, including nested and empty ones`() {
        assertEquals(
            listOf(listOf("a", "b"), emptyList(), listOf("c")),
            decode(
                """<array><data>
                     <value><array><data><value><string>a</string></value><value>b</value></data></array></value>
                     <value><array><data/></array></value>
                     <!-- a comment between values -->
                     <value><array><data><value><string>c</string></value></data></array></value>
                   </data></array>""",
                ListSerializer(ListSerializer(String.serializer())),
            ),
        )
    }

    @Test
    fun `a multicall row decodes by position, skipping extra values and defaulting missing ones`() {
        val rows = decode(
            """<array><data>
                 <value><array><data>
                   <value><string>ubuntu.iso</string></value><value><i8>100</i8></value><value><i8>1</i8></value>
                 </data></array></value>
                 <value><array><data>
                   <value><string>debian.iso</string></value><value><i8>200</i8></value><value><i8>0</i8></value>
                   <value><i8>7</i8></value><value><string>an extra trailing value</string></value>
                 </data></array></value>
               </data></array>""",
            ListSerializer(Row.serializer()),
        )

        assertEquals(Row("ubuntu.iso", 100, true, -1), rows[0])
        assertEquals(Row("debian.iso", 200, false, 7), rows[1])
    }

    @Test
    fun `multicall commands follow the row class declaration order`() {
        assertEquals(listOf("x.name=", "x.size=", "x.flag=", "x.missing="), multicallCommands(Row.serializer().descriptor))
    }

    @Test
    fun `a struct decodes by member name, skipping unknown members`() {
        assertEquals(
            Pair("one", 2),
            decode(
                """<struct>
                     <member><name>second</name><value><i4>2</i4></value></member>
                     <member><name>unknown</name><value><array><data><value>x</value></data></array></value></member>
                     <member><name>first</name><value><string>one</string></value></member>
                   </struct>""",
                Pair.serializer(),
            ),
        )
    }

    @Test
    fun `an ignored reply is skipped whatever it holds`() {
        decode("<i8>0</i8>", XmlRpc.Ignored)
        decode("<struct><member><name>a</name><value><array><data/></array></value></member></struct>", XmlRpc.Ignored)
    }

    @Test
    fun `a fault becomes an unexpected response carrying its message`() {
        val error = decodeFails(
            """<?xml version="1.0"?><methodResponse><fault><value><struct>
               <member><name>faultCode</name><value><i4>-506</i4></value></member>
               <member><name>faultString</name><value><string>Method 'nope' not defined</string></value></member>
               </struct></value></fault></methodResponse>"""
        )
        assertTrue(error.message!!.contains("Method 'nope' not defined"))
    }

    @Test
    fun `an external entity is never resolved`() {
        decodeFails(
            """<?xml version="1.0"?><!DOCTYPE methodResponse [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
               <methodResponse><params><param><value><string>&xxe;</string></value></param></params></methodResponse>"""
        )
    }

    @Test
    fun `an entity expansion bomb is refused`() {
        decodeFails(
            """<?xml version="1.0"?><!DOCTYPE lolz [
                 <!ENTITY lol "lol">
                 <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
                 <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
               ]>
               <methodResponse><params><param><value><string>&lol3;</string></value></param></params></methodResponse>"""
        )
    }

    @Test
    fun `an undeclared entity is an error rather than silently dropped text`() {
        decodeFails(
            """<?xml version="1.0"?><methodResponse><params><param><value><string>&nope;</string></value></param></params></methodResponse>"""
        )
    }

    @Test
    fun `a document other than a methodResponse is rejected`() {
        val error = decodeFails("""<?xml version="1.0"?><methodCall><methodName>x</methodName></methodCall>""")
        assertTrue(error.message!!.contains("methodResponse"))
    }
}
