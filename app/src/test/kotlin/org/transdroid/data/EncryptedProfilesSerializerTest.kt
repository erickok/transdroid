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
package org.transdroid.data

import androidx.datastore.core.CorruptionException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.KeyStoreException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.transdroid.protocol.DaemonType

/** A real AES-GCM cipher with an in-memory key, standing in for the Keystore-backed one. */
private class LocalAesCipher : ProfilesCipher {
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return ByteArray(1 + iv.size + ciphertext.size).also {
            it[0] = iv.size.toByte()
            iv.copyInto(it, 1)
            ciphertext.copyInto(it, 1 + iv.size)
        }
    }

    override fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > 1) { "Encrypted blob too short" }
        val ivLength = blob[0].toInt()
        require(ivLength in 12..16 && blob.size > 1 + ivLength) { "Corrupt encrypted blob" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob.copyOfRange(1, 1 + ivLength)))
        return cipher.doFinal(blob.copyOfRange(1 + ivLength, blob.size))
    }
}

class EncryptedProfilesSerializerTest {

    private val cipher = LocalAesCipher()
    private val serializer = EncryptedProfilesSerializer(cipher)

    private val sampleData = ProfilesData(
        profiles = listOf(
            ServerProfile(
                id = "id-1",
                name = "Seedbox",
                type = DaemonType.TRANSMISSION,
                host = "10.0.0.2",
                port = 9091,
                username = "user",
                password = "s3cret",
                pinnedCertSha256 = "ab".repeat(32),
            )
        ),
        feeds = listOf(RssFeed(id = "feed-1", name = "ISOs", url = "https://example.com/rss?passkey=x")),
        searchProviders = listOf(SearchProviderConfig(id = "p1", name = "Jackett", url = "http://nas:9117", apiKey = "key")),
    )

    private suspend fun writeToBytes(data: ProfilesData): ByteArray =
        ByteArrayOutputStream().also { serializer.writeTo(data, it) }.toByteArray()

    @Test
    fun `round trips profiles feeds and providers`() = runTest {
        val bytes = writeToBytes(sampleData)

        val restored = serializer.readFrom(ByteArrayInputStream(bytes))

        assertEquals(sampleData, restored)
    }

    @Test
    fun `data is not stored in plaintext`() = runTest {
        val bytes = writeToBytes(sampleData)

        val raw = bytes.decodeToString()
        assertTrue("password must not appear in the stored blob", !raw.contains("s3cret"))
        assertTrue("passkey must not appear in the stored blob", !raw.contains("passkey"))
    }

    @Test
    fun `empty file yields defaults`() = runTest {
        assertEquals(ProfilesData(), serializer.readFrom(ByteArrayInputStream(ByteArray(0))))
    }

    @Test
    fun `tampered blob is corruption, allowing the wipe-and-replace handler`() = runTest {
        val bytes = writeToBytes(sampleData)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0x01).toByte()

        try {
            serializer.readFrom(ByteArrayInputStream(bytes))
            fail("Expected CorruptionException")
        } catch (expected: CorruptionException) {
            assertTrue(expected.cause is AEADBadTagException)
        }
    }

    @Test
    fun `transient keystore failure is an IOException, never corruption`() = runTest {
        val flaky = object : ProfilesCipher {
            override fun encrypt(plaintext: ByteArray) = cipher.encrypt(plaintext)
            override fun decrypt(blob: ByteArray) = throw KeyStoreException("keystore daemon unavailable")
        }
        val bytes = writeToBytes(sampleData)

        try {
            EncryptedProfilesSerializer(flaky).readFrom(ByteArrayInputStream(bytes))
            fail("Expected IOException")
        } catch (expected: CorruptionException) {
            fail("A transient keystore error must not be treated as corruption")
        } catch (expected: IOException) {
        }
    }

    @Test
    fun `unknown fields from future versions are ignored`() = runTest {
        val futureJson = """{"profiles":[],"feeds":[],"searchProviders":[],"someFutureField":42}"""
        val bytes = cipher.encrypt(futureJson.encodeToByteArray())

        assertEquals(ProfilesData(), serializer.readFrom(ByteArrayInputStream(bytes)))
    }

    @Test
    fun `profiles saved with a single local network SSID still load and apply`() = runTest {
        val legacyJson = """{"profiles":[{"id":"id-1","name":"Seedbox","type":"TRANSMISSION",""" +
            """"host":"public.example.org","port":443,"localNetworkEnabled":true,""" +
            """"localNetworkSsid":"HomeNet","localHost":"192.168.1.10","localPort":9091}]}"""
        val bytes = cipher.encrypt(legacyJson.encodeToByteArray())

        val profile = serializer.readFrom(ByteArrayInputStream(bytes)).profiles.single()

        assertEquals(emptyList<String>(), profile.localNetworkSsids)
        assertEquals("192.168.1.10", profile.toDaemonConfig(connectedSsid = "HomeNet").host)
    }
}
