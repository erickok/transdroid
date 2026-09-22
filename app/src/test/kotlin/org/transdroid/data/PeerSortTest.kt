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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.transdroid.protocol.Peer

class PeerSortTest {

    private fun peer(ip: String, down: Long = 0, up: Long = 0, client: String? = null, port: Int? = null) =
        Peer(ip = ip, port = port, clientName = client, progress = 0f, downloadRate = down, uploadRate = up)

    private fun List<Peer>.sortedIps(sort: PeerSort) = sortedWith(sort.comparator).map { it.ip }

    @Test
    fun `total speed puts the most active peer first`() {
        val peers = listOf(
            peer("192.0.2.1", down = 100),
            peer("192.0.2.2", down = 100, up = 500),
            peer("192.0.2.3", up = 300),
        )
        assertEquals(listOf("192.0.2.2", "192.0.2.3", "192.0.2.1"), peers.sortedIps(PeerSort.TOTAL_SPEED))
    }

    @Test
    fun `download and upload speed sort on their own rate only`() {
        val peers = listOf(peer("192.0.2.1", down = 10, up = 900), peer("192.0.2.2", down = 50, up = 1))
        assertEquals(listOf("192.0.2.2", "192.0.2.1"), peers.sortedIps(PeerSort.DOWNLOAD_SPEED))
        assertEquals(listOf("192.0.2.1", "192.0.2.2"), peers.sortedIps(PeerSort.UPLOAD_SPEED))
    }

    @Test
    fun `idle peers keep a stable address order instead of reshuffling`() {
        val peers = listOf(peer("203.0.113.9"), peer("192.0.2.200"), peer("192.0.2.3"))
        val expected = listOf("192.0.2.3", "192.0.2.200", "203.0.113.9")
        assertEquals(expected, peers.sortedIps(PeerSort.TOTAL_SPEED))
        assertEquals("input order must not matter", expected, peers.reversed().sortedIps(PeerSort.TOTAL_SPEED))
    }

    @Test
    fun `addresses order numerically, not as text`() {
        val peers = listOf(peer("111.1.1.1"), peer("2.2.2.2"), peer("10.0.0.1"))
        assertEquals(listOf("2.2.2.2", "10.0.0.1", "111.1.1.1"), peers.sortedIps(PeerSort.ADDRESS))
    }

    @Test
    fun `ipv4 sorts ahead of ipv6 and the port breaks ties`() {
        val peers = listOf(
            peer("2001:db8::1"),
            peer("192.0.2.1", port = 6889),
            peer("192.0.2.1", port = 6881),
        )
        val sorted = peers.sortedWith(PeerSort.ADDRESS.comparator)
        assertEquals(listOf("192.0.2.1", "192.0.2.1", "2001:db8::1"), sorted.map { it.ip })
        assertEquals(listOf(6881, 6889, null), sorted.map { it.port })
    }

    @Test
    fun `client sort ignores case and puts unidentified peers last`() {
        val peers = listOf(
            peer("192.0.2.1", client = null),
            peer("192.0.2.2", client = "qBittorrent/5.0.2"),
            peer("192.0.2.3", client = "Deluge 2.1.1"),
            peer("192.0.2.4", client = "libtorrent/2.0.10"),
        )
        assertEquals(
            listOf("192.0.2.3", "192.0.2.4", "192.0.2.2", "192.0.2.1"),
            peers.sortedIps(PeerSort.CLIENT),
        )
    }

    @Test
    fun `ipv4 packing rejects anything that is not a dotted quad`() {
        assertEquals(0xC0000201L, PeerSort.ipv4ToLong("192.0.2.1"))
        assertEquals(0L, PeerSort.ipv4ToLong("0.0.0.0"))
        assertEquals(0xFFFFFFFFL, PeerSort.ipv4ToLong("255.255.255.255"))
        assertNull(PeerSort.ipv4ToLong("256.0.0.1"))
        assertNull(PeerSort.ipv4ToLong("192.0.2"))
        assertNull(PeerSort.ipv4ToLong("192.0.2.1.5"))
        assertNull(PeerSort.ipv4ToLong("192.0.2.+1"))
        assertNull(PeerSort.ipv4ToLong("192.0..1"))
        assertNull(PeerSort.ipv4ToLong("2001:db8::1"))
        assertNull("a hostname must never be treated as an address", PeerSort.ipv4ToLong("tracker.example.org"))
    }

    @Test
    fun `an unknown stored name falls back to the default`() {
        assertEquals(PeerSort.ADDRESS, PeerSort.fromName("ADDRESS"))
        assertEquals(PeerSort.DEFAULT, PeerSort.fromName("no_longer_exists"))
        assertEquals(PeerSort.DEFAULT, PeerSort.fromName(null))
    }
}
