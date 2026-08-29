/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.transdroid.daemon;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The criteria by which the GUI can sort a torrent's connected peers. The stored value matches the
 * entryValues of the peer-sort preference.
 *
 * @author Eric Kok
 */
public enum PeersSortBy {

    TotalSpeed("totalspeed"),
    DownloadSpeed("downloadspeed"),
    UploadSpeed("upspeed"),
    Address("address"),
    Client("client");

    private final String value;

    PeersSortBy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PeersSortBy fromValue(String value) {
        for (PeersSortBy sortBy : values()) {
            if (sortBy.value.equals(value)) {
                return sortBy;
            }
        }
        return TotalSpeed;
    }

    /**
     * A comparator that orders peers according to this sort criterion. Speed-based sorts are
     * descending (most active first); text-based sorts are ascending. Address is used as a stable
     * tie-breaker so the list does not jump around between refreshes.
     */
    public Comparator<Peer> comparator() {
        final Comparator<Peer> byAddress = (a, b) -> compareAddresses(a.getAddress(), b.getAddress());
        switch (this) {
            case DownloadSpeed:
                return chain((a, b) -> Integer.compare(b.getDownSpeed(), a.getDownSpeed()), byAddress);
            case UploadSpeed:
                return chain((a, b) -> Integer.compare(b.getUpSpeed(), a.getUpSpeed()), byAddress);
            case Address:
                return byAddress;
            case Client:
                return chain((a, b) -> safe(a.getClient()).compareToIgnoreCase(safe(b.getClient())), byAddress);
            case TotalSpeed:
            default:
                return chain((a, b) -> Long.compare((long) b.getDownSpeed() + b.getUpSpeed(),
                        (long) a.getDownSpeed() + a.getUpSpeed()), byAddress);
        }
    }

    private static Comparator<Peer> chain(final Comparator<Peer> primary, final Comparator<Peer> secondary) {
        return (a, b) -> {
            int result = primary.compare(a, b);
            return result != 0 ? result : secondary.compare(a, b);
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final Pattern IPV4 = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");

    /**
     * Compares two peer addresses. IPv4 addresses are ordered numerically octet-by-octet (so
     * 2.2.2.2 sorts before 111.1.1.1) and come before non-IPv4 addresses; anything else falls back
     * to a case-insensitive string comparison.
     */
    static int compareAddresses(String a, String b) {
        String hostA = host(a);
        String hostB = host(b);
        Long ipA = ipv4ToLong(hostA);
        Long ipB = ipv4ToLong(hostB);
        if (ipA != null && ipB != null) {
            return Long.compare(ipA, ipB);
        }
        if (ipA != null) {
            return -1; // IPv4 before IPv6/hostnames
        }
        if (ipB != null) {
            return 1;
        }
        return hostA.compareToIgnoreCase(hostB);
    }

    /**
     * Extracts the host part from a "host", "host:port" or "[ipv6]:port" address string.
     */
    private static String host(String address) {
        if (address == null) {
            return "";
        }
        address = address.trim();
        if (address.startsWith("[")) {
            int end = address.indexOf(']');
            return end > 0 ? address.substring(1, end) : address;
        }
        int colon = address.indexOf(':');
        if (colon >= 0 && colon == address.lastIndexOf(':')) {
            // Exactly one colon: host:port (IPv4 or hostname)
            return address.substring(0, colon);
        }
        // No colon, or multiple colons (bare IPv6): use as-is
        return address;
    }

    /**
     * Packs a dotted-quad IPv4 string into a long for numeric comparison, or null if not IPv4.
     */
    private static Long ipv4ToLong(String host) {
        Matcher matcher = IPV4.matcher(host);
        if (!matcher.matches()) {
            return null;
        }
        long value = 0;
        for (int i = 1; i <= 4; i++) {
            int octet = Integer.parseInt(matcher.group(i));
            if (octet > 255) {
                return null;
            }
            value = (value << 8) | octet;
        }
        return value;
    }

}
