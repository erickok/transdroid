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
package org.transdroid.data.legacy

import okhttp3.Credentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.transdroid.data.ProfilesData
import org.transdroid.data.RssFeed
import org.transdroid.data.ServerProfile
import org.transdroid.protocol.DaemonType

class LegacySettingsConverterTest {

    /** Sequential ids so tests can refer to "id0", "id1", ... */
    private fun ids(): () -> String {
        var next = 0
        return { "id${next++}" }
    }

    private fun convert(vararg prefs: Pair<String, Any?>) = LegacySettingsConverter.convert(mapOf(*prefs), ids())

    @Test
    fun `transmission server with defaults`() {
        val result = convert(
            "server_name_0" to "Home",
            "server_type_0" to "daemon_transmission",
            "server_address_0" to " 192.168.1.2 ",
            "server_user_0" to "admin",
            "server_pass_0" to "secret",
        )
        val profile = result.profiles.single()
        assertEquals("Home", profile.name)
        assertEquals(DaemonType.TRANSMISSION, profile.type)
        assertEquals("192.168.1.2", profile.host)
        assertEquals(9091, profile.port)
        assertFalse(profile.useSsl)
        assertEquals("", profile.path)
        assertEquals("admin", profile.username)
        assertEquals("secret", profile.password)
        assertFalse(profile.localNetworkEnabled)
        assertEquals("id0", result.activeProfileId)
    }

    @Test
    fun `transmission folder gets the rpc suffix v2 always appended`() {
        assertEquals("/proxy/transmission/rpc", LegacySettingsConverter.v3Path(DaemonType.TRANSMISSION, "/proxy/transmission/"))
        assertEquals("/custom/rpc", LegacySettingsConverter.v3Path(DaemonType.TRANSMISSION, "custom"))
        assertEquals("", LegacySettingsConverter.v3Path(DaemonType.TRANSMISSION, " "))
    }

    @Test
    fun `other clients keep their folder as path`() {
        assertEquals("/qbt", LegacySettingsConverter.v3Path(DaemonType.QBITTORRENT, "/qbt/"))
        assertEquals("/deluge", LegacySettingsConverter.v3Path(DaemonType.DELUGE, "deluge"))
        assertEquals("/user/RPC2", LegacySettingsConverter.v3Path(DaemonType.RTORRENT, "/user/RPC2"))
        assertEquals("", LegacySettingsConverter.v3Path(DaemonType.RTORRENT, null))
    }

    @Test
    fun `ports default like v2, which only used 443 for rTorrent over SSL`() {
        val result = convert(
            "server_type_0" to "daemon_qbittorrent", "server_address_0" to "a", "server_sslenabled_0" to true,
            "server_type_1" to "daemon_deluge", "server_address_1" to "b", "server_sslenabled_1" to true,
            "server_type_2" to "daemon_rtorrent", "server_address_2" to "c", "server_sslenabled_2" to true,
            "server_type_3" to "daemon_rtorrent", "server_address_3" to "d",
            "server_type_4" to "daemon_transmission", "server_address_4" to "e", "server_port_4" to "not a port",
            "server_type_5" to "daemon_transmission", "server_address_5" to "f", "server_port_5" to "8443",
        )
        assertEquals(listOf(8080, 8112, 443, 80, 9091, 8443), result.profiles.map { it.port })
    }

    @Test
    fun `disabled authentication drops the stored credentials`() {
        val profile = convert(
            "server_type_0" to "daemon_qbittorrent",
            "server_address_0" to "host",
            "server_disableauth_0" to true,
            "server_user_0" to "stale",
            "server_pass_0" to "stale",
        ).profiles.single()
        assertEquals("", profile.username)
        assertEquals("", profile.password)
    }

    @Test
    fun `deluge logs in with the extra password and sends any username as HTTP auth`() {
        val profile = convert(
            "server_type_0" to "daemon_deluge",
            "server_address_0" to "host",
            "server_user_0" to "proxyuser",
            "server_pass_0" to "proxypass",
            "server_extrapass_0" to "webpass",
        ).profiles.single()
        assertEquals("webpass", profile.password)
        assertEquals("", profile.username)
        assertEquals("Authorization: ${Credentials.basic("proxyuser", "proxypass")}", profile.customHeaders)
    }

    @Test
    fun `deluge without a username has no extra header`() {
        val profile = convert(
            "server_type_0" to "daemon_deluge",
            "server_address_0" to "host",
            "server_extrapass_0" to "webpass",
        ).profiles.single()
        assertEquals("", profile.customHeaders)
    }

    @Test
    fun `local network override with several SSIDs`() {
        val profile = convert(
            "server_type_0" to "daemon_transmission",
            "server_address_0" to "example.org",
            "server_port_0" to "443",
            "server_sslenabled_0" to true,
            "server_folder_0" to "/transmission",
            "server_localnetwork_0" to "Home | Home 5G|",
            "server_localaddress_0" to "192.168.1.2",
            "server_localport_0" to "9091",
            "server_localsslenabled_0" to false,
        ).profiles.single()
        assertTrue(profile.localNetworkEnabled)
        assertEquals(listOf("Home", "Home 5G"), profile.localNetworkSsids)
        assertEquals("192.168.1.2", profile.localHost)
        assertEquals(9091, profile.localPort)
        assertFalse(profile.localUseSsl)
        assertEquals("/transmission/rpc", profile.localPath)
        assertEquals("", profile.localUsername)
    }

    @Test
    fun `local override falls back to the main port and SSL setting`() {
        val profile = convert(
            "server_type_0" to "daemon_qbittorrent",
            "server_address_0" to "example.org",
            "server_port_0" to "8443",
            "server_sslenabled_0" to true,
            "server_localnetwork_0" to "Home",
            "server_localaddress_0" to "nas.local",
        ).profiles.single()
        assertEquals(8443, profile.localPort)
        assertTrue(profile.localUseSsl)
    }

    @Test
    fun `local override needs both a network and an address`() {
        val profile = convert(
            "server_type_0" to "daemon_qbittorrent",
            "server_address_0" to "example.org",
            "server_localnetwork_0" to "Home",
        ).profiles.single()
        assertFalse(profile.localNetworkEnabled)
        assertTrue(profile.localNetworkSsids.isEmpty())
    }

    @Test
    fun `trusted self-signed certificates are flagged for re-trusting`() {
        val result = convert(
            "server_name_0" to "Pinned",
            "server_type_0" to "daemon_transmission", "server_address_0" to "a", "server_sslenabled_0" to true,
            "server_ssltrustkey_0" to "ab:cd",
            "server_name_1" to "Trust all",
            "server_type_1" to "daemon_transmission", "server_address_1" to "b", "server_sslenabled_1" to true,
            "server_ssltrustall_1" to true,
            "server_name_2" to "Plain HTTP",
            "server_type_2" to "daemon_transmission", "server_address_2" to "c", "server_ssltrustall_2" to true,
        )
        assertEquals(listOf("Pinned", "Trust all"), result.needsCertificate)
        assertTrue(result.profiles.all { it.pinnedCertSha256.isEmpty() })
    }

    @Test
    fun `unsupported clients and deluge daemon connections are skipped and reported`() {
        val result = convert(
            "server_name_0" to "Old box",
            "server_type_0" to "daemon_utorrent", "server_address_0" to "a",
            "server_type_1" to "daemon_deluge_rpc", "server_address_1" to "deluge.local",
            "server_type_2" to "daemon_deluge2_rpc", "server_address_2" to "c",
            "server_type_3" to "daemon_rtorrent", "server_address_3" to "d",
        )
        assertEquals(listOf("id0"), result.profiles.map { it.id })
        assertEquals(
            listOf(
                LegacySkippedServer("Old box", LegacySkipReason.UNSUPPORTED_CLIENT),
                LegacySkippedServer("deluge.local", LegacySkipReason.DELUGE_DAEMON),
                LegacySkippedServer("c", LegacySkipReason.DELUGE_DAEMON),
            ),
            result.skipped,
        )
    }

    @Test
    fun `reading stops at the first server without type or address, like v2`() {
        val result = convert(
            "server_type_0" to "daemon_transmission", "server_address_0" to "a",
            "server_type_1" to "daemon_transmission",
            "server_type_2" to "daemon_transmission", "server_address_2" to "c",
        )
        assertEquals(listOf("a"), result.profiles.map { it.host })
    }

    @Test
    fun `xirvik seedboxes use their fixed v2 settings, dediseedbox is skipped`() {
        val result = convert(
            "seedbox_dediseedbox_server_0" to "dedi.example",
            "seedbox_dediseedbox_name_0" to "Dedi",
            "seedbox_xirvikshared_server_0" to "shared.xirvik.com",
            "seedbox_xirvikshared_user_0" to "me",
            "seedbox_xirvikshared_pass_0" to "pw",
            "seedbox_xirvikshared_rpc_0" to "/me/RPC2",
            "seedbox_xirviksemi_server_0" to "semi.xirvik.com",
            "seedbox_xirvikdedi_server_0" to "dedi.xirvik.com",
            "seedbox_xirvikdedi_client_0" to "daemon_deluge",
            "seedbox_xirvikdedi_user_0" to "me",
            "seedbox_xirvikdedi_pass_0" to "pw",
            "seedbox_xirvikdedi_server_1" to "ut.xirvik.com",
            "seedbox_xirvikdedi_client_1" to "daemon_utorrent",
        )
        assertEquals(
            listOf(
                LegacySkippedServer("Dedi", LegacySkipReason.UNSUPPORTED_SEEDBOX),
                LegacySkippedServer("ut.xirvik.com", LegacySkipReason.UNSUPPORTED_CLIENT),
            ),
            result.skipped,
        )
        val (shared, semi, dedi) = result.profiles
        assertEquals(DaemonType.RTORRENT, shared.type)
        assertEquals("shared.xirvik.com", shared.host)
        assertEquals(443, shared.port)
        assertTrue(shared.useSsl)
        assertEquals("/me/RPC2", shared.path)
        assertEquals("me", shared.username)
        assertEquals("pw", shared.password)
        assertEquals("/RPC2", semi.path)
        assertEquals(DaemonType.DELUGE, dedi.type)
        assertEquals("/deluge", dedi.path)
        assertEquals("deluge", dedi.password)
        assertEquals("Authorization: ${Credentials.basic("me", "pw")}", dedi.customHeaders)
    }

    @Test
    fun `xirvik dedicated defaults to rTorrent`() {
        val profile = convert("seedbox_xirvikdedi_server_0" to "dedi.xirvik.com").profiles.single()
        assertEquals(DaemonType.RTORRENT, profile.type)
        assertEquals("", profile.path)
    }

    @Test
    fun `explicit default server wins over the last used one`() {
        val result = convert(
            "server_type_0" to "daemon_transmission", "server_address_0" to "a",
            "server_type_1" to "daemon_transmission", "server_address_1" to "b",
            "header_defaultserver" to "1",
            "system_lastusedserver" to 0,
        )
        assertEquals("id1", result.activeProfileId)
    }

    @Test
    fun `last used server counts skipped servers and seedboxes in v2's numbering`() {
        val result = convert(
            "server_type_0" to "daemon_utorrent", "server_address_0" to "skipped",
            "server_type_1" to "daemon_transmission", "server_address_1" to "b",
            "seedbox_dediseedbox_server_0" to "dedi.example",
            "seedbox_xirviksemi_server_0" to "semi.xirvik.com",
            "header_defaultserver" to "-2",
            "system_lastusedserver" to 3,
        )
        val semi = result.profiles.single { it.host == "semi.xirvik.com" }
        assertEquals(semi.id, result.activeProfileId)
    }

    @Test
    fun `a skipped or unknown last used server falls back to the first imported`() {
        val result = convert(
            "server_type_0" to "daemon_utorrent", "server_address_0" to "skipped",
            "server_type_1" to "daemon_transmission", "server_address_1" to "b",
            "system_lastusedserver" to 0,
        )
        assertEquals(result.profiles.single().id, result.activeProfileId)
        assertNull(convert().activeProfileId)
    }

    @Test
    fun `rss feeds convert last viewed from milliseconds to seconds`() {
        val feeds = convert(
            "rssfeed_name_0" to "Linux ISOs",
            "rssfeed_url_0" to "https://example.org/rss?passkey=x",
            "rssfeed_lastviewed_0" to 1_700_000_000_123L,
            "rssfeed_url_1" to "https://example.org/other",
            "rssfeed_lastviewed_1" to -1L,
        ).feeds
        assertEquals(listOf("Linux ISOs", ""), feeds.map { it.name })
        assertEquals(listOf(1_700_000_000L, null), feeds.map { it.lastViewedTimestamp })
    }

    @Test
    fun `merge skips servers and feeds that already exist`() {
        val existing = ProfilesData(
            profiles = listOf(ServerProfile(id = "v3", name = "Mine", type = DaemonType.TRANSMISSION, host = "HOST", port = 9091)),
            feeds = listOf(RssFeed(id = "f", name = "", url = "https://example.org/rss")),
        )
        val import = convert(
            "server_name_0" to "Dupe", "server_type_0" to "daemon_transmission", "server_address_0" to "host",
            "server_ssltrustall_0" to true, "server_sslenabled_0" to true,
            "server_type_1" to "daemon_transmission", "server_address_1" to "other",
            "server_type_2" to "daemon_utorrent", "server_address_2" to "ut",
            "rssfeed_url_0" to "https://example.org/rss",
            "rssfeed_url_1" to "https://example.org/new",
        )
        val result = LegacySettingsConverter.merge(existing, import)
        assertEquals(listOf("v3", "id1"), result.data.profiles.map { it.id })
        assertEquals(2, result.data.feeds.size)
        assertEquals(LegacyImportSummary(1, 1, import.skipped, emptyList()), result.summary)
        // v2's active server was the duplicate, which wasn't added
        assertNull(result.activeProfileId)
    }

    @Test
    fun `importing twice adds nothing the second time`() {
        val prefs = mapOf<String, Any?>("server_type_0" to "daemon_qbittorrent", "server_address_0" to "nas")
        val first = LegacySettingsConverter.merge(ProfilesData(), LegacySettingsConverter.convert(prefs))
        val second = LegacySettingsConverter.merge(first.data, LegacySettingsConverter.convert(prefs))
        assertEquals(1, second.data.profiles.size)
        assertTrue(second.summary.isEmpty)
    }

    @Test
    fun `settings json export maps onto the same settings`() {
        val json = """
            {
              "servers": [
                {"name": "Seedbox", "type": "daemon_rtorrent", "host": "box.example", "port": "443", "ssl": true,
                 "local_ssl": false, "ssl_accept_all": false, "folder": "/RPC2", "use_auth": true,
                 "username": "me", "password": "pw", "download_alarm": true},
                {"name": "Old", "type": "daemon_deluge_rpc", "host": "deluge", "ssl": false, "use_auth": false,
                 "username": "x"}
              ],
              "websites": [],
              "rssfeeds": [{"name": "Feed", "url": "https://example.org/rss", "needs_auth": false, "last_seen_time": 1700000000000}],
              "alarm_enabled_torrents": true
            }
        """.trimIndent()
        val result = LegacySettingsConverter.convert(LegacySettingsConverter.fromExportJson(json), ids())
        val profile = result.profiles.single()
        assertEquals("Seedbox", profile.name)
        assertEquals(DaemonType.RTORRENT, profile.type)
        assertEquals("box.example", profile.host)
        assertEquals(443, profile.port)
        assertTrue(profile.useSsl)
        assertEquals("/RPC2", profile.path)
        assertEquals("me", profile.username)
        assertEquals("pw", profile.password)
        assertEquals(listOf(LegacySkippedServer("Old", LegacySkipReason.DELUGE_DAEMON)), result.skipped)
        assertEquals(1_700_000_000L, result.feeds.single().lastViewedTimestamp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `other json is rejected`() {
        LegacySettingsConverter.fromExportJson("""{"profiles": []}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-json is rejected`() {
        LegacySettingsConverter.fromExportJson("TDBK\u0000binary")
    }
}
