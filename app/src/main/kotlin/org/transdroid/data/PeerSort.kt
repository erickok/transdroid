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

import org.transdroid.protocol.Peer

/**
 * The order a torrent's connected peers are listed in, carried over from Transdroid 2's "Sort
 * peers by" preference. Speed-based orders are descending (most active first), text-based ones
 * ascending. Every order falls back to the address as a tie-breaker, so the many peers sitting at
 * an identical 0 B/s keep a stable position instead of reshuffling on every poll.
 */
enum class PeerSort {
    TOTAL_SPEED, DOWNLOAD_SPEED, UPLOAD_SPEED, ADDRESS, CLIENT;

    val comparator: Comparator<Peer>
        get() = when (this) {
            TOTAL_SPEED -> compareByDescending<Peer> { it.downloadRate + it.uploadRate }.then(BY_ADDRESS)
            DOWNLOAD_SPEED -> compareByDescending<Peer> { it.downloadRate }.then(BY_ADDRESS)
            UPLOAD_SPEED -> compareByDescending<Peer> { it.uploadRate }.then(BY_ADDRESS)
            ADDRESS -> BY_ADDRESS
            // Peers that never identified themselves go last rather than first
            CLIENT -> compareBy<Peer> { it.clientName == null }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.clientName.orEmpty() }
                .then(BY_ADDRESS)
        }

    companion object {
        val DEFAULT = TOTAL_SPEED

        /** Tolerates a stored name that no longer exists, e.g. after a constant is renamed. */
        fun fromName(name: String?): PeerSort = entries.firstOrNull { it.name == name } ?: DEFAULT

        /**
         * IPv4 addresses order numerically octet by octet (so 2.2.2.2 sorts before 111.1.1.1,
         * which a plain string comparison gets backwards) and ahead of everything else; IPv6
         * addresses fall back to a case-insensitive string comparison, which is stable if not
         * numeric. The port breaks ties between several peers behind one address.
         */
        private val BY_ADDRESS: Comparator<Peer> = Comparator<Peer> { a, b ->
            val ipA = ipv4ToLong(a.ip)
            val ipB = ipv4ToLong(b.ip)
            when {
                ipA != null && ipB != null -> ipA.compareTo(ipB)
                ipA != null -> -1
                ipB != null -> 1
                else -> a.ip.compareTo(b.ip, ignoreCase = true)
            }
        }.thenBy { it.port ?: 0 }

        /** Packs a dotted-quad string into a number for comparison, or null when it isn't IPv4. */
        internal fun ipv4ToLong(ip: String): Long? {
            val octets = ip.split('.')
            if (octets.size != 4) return null
            return octets.fold(0L) { packed, octet ->
                if (octet.isEmpty() || octet.length > 3 || !octet.all { it in '0'..'9' }) return null
                val value = octet.toInt()
                if (value > 255) return null
                (packed shl 8) or value.toLong()
            }
        }
    }
}
