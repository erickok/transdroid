package org.transdroid.connect.clients.rtorrent

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.transdroid.connect.Configuration
import org.transdroid.connect.clients.Client
import org.transdroid.connect.clients.ClientSpec
import org.transdroid.connect.clients.Feature
import org.transdroid.connect.clients.UnsupportedFeatureException
import org.transdroid.connect.mock.MockTorrent
import org.transdroid.connect.model.Torrent

class RtorrentLiveTest {

    private lateinit var rtorrent: ClientSpec

    @Before
    fun setUp() {
        rtorrent = Configuration(Client.RTORRENT,
                "http://localhost:8008/", "RPC2",
                loggingEnabled = true)
                .createClient()
    }

    @Test
    fun features() {
        assertThat(Client.RTORRENT.supports(Feature.VERSION)).isTrue()
        assertThat(Client.RTORRENT.supports(Feature.LISTING)).isTrue()
        assertThat(Client.RTORRENT.supports(Feature.STARTING_STOPPING)).isTrue()
        assertThat(Client.RTORRENT.supports(Feature.RESUMING_PAUSING)).isTrue()
        assertThat(Client.RTORRENT.supports(Feature.FORCE_STARTING)).isFalse()
        assertThat(Client.RTORRENT.supports(Feature.ADD_BY_FILE)).isTrue()
        assertThat(Client.RTORRENT.supports(Feature.ADD_BY_URL)).isTrue()
        assertThat(Client.RTORRENT.supports(Feature.ADD_BY_MAGNET)).isTrue()
    }

    @Test
    fun clientVersion() {
        rtorrent.clientVersion()
                .test()
                .assertValue("0.9.6")
    }

    @Test
    fun torrents() {
        rtorrent.torrents()
                .toList()
                .test()
                .assertValue { torrents -> torrents.size > 0 }
    }

    @Test
    fun addByUrl() {
        rtorrent.addByUrl(MockTorrent.torrentUrl)
                .test()
                .assertNoErrors()
    }

    @Test
    fun addByMagnet() {
        rtorrent.addByMagnet(MockTorrent.magnetUrl)
                .test()
                .assertNoErrors()
    }

    @Test
    fun addByFile() {
        rtorrent.addByFile(MockTorrent.torrentFile)
                .test()
                .assertNoErrors()
    }

    @Test
    fun start() {
        rtorrent.start(firstLiveTorrent())
                .test()
                .assertValue({ it.canStop })
    }

    @Test
    fun stop() {
        rtorrent.stop(firstLiveTorrent())
                .test()
                .assertValue({ it.canStart })
    }

    @Test(expected = UnsupportedFeatureException::class)
    fun forceStart() {
        rtorrent.forceStart(firstLiveTorrent())
                .test()
                .assertValue({ it.canStop })
    }

    private fun firstLiveTorrent(): Torrent = rtorrent.torrents().blockingFirst()

}
