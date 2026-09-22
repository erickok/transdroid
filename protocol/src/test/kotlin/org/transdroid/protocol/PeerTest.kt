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
package org.transdroid.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class PeerTest {

    private fun peer(ip: String, port: Int?) =
        Peer(ip = ip, progress = 0f, downloadRate = 0, uploadRate = 0, port = port)

    @Test
    fun `endpoint joins an ipv4 address and port with a colon`() {
        assertEquals("203.0.113.5:51413", peer("203.0.113.5", 51413).endpoint)
    }

    @Test
    fun `endpoint brackets an ipv6 address so the port is unambiguous`() {
        assertEquals("[2001:db8::1]:6881", peer("2001:db8::1", 6881).endpoint)
    }

    @Test
    fun `endpoint is the bare address when the port is unknown`() {
        assertEquals("203.0.113.5", peer("203.0.113.5", null).endpoint)
        assertEquals("no brackets without a port either", "2001:db8::1", peer("2001:db8::1", null).endpoint)
    }
}
