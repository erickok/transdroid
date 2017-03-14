package org.transdroid.connect.clients.rtorrent;

import org.junit.Before;
import org.junit.Test;
import org.transdroid.connect.Configuration;
import org.transdroid.connect.clients.Client;
import org.transdroid.connect.clients.ClientSpec;
import org.transdroid.connect.clients.Feature;
import org.transdroid.connect.clients.UnsupportedFeatureException;
import org.transdroid.connect.model.Torrent;

import java.io.IOException;
import java.util.List;

import io.reactivex.functions.Predicate;

import static com.google.common.truth.Truth.assertThat;

public final class RtorrentTest {

	private ClientSpec rtorrent;

	@Before
	public void setUp() {
		rtorrent = new Configuration.Builder(Client.RTORRENT)
				.baseUrl("http://localhost:8008/")
				.endpoint("RPC2")
				.loggingEnabled(true)
				.build()
				.createClient();
	}

	@Test
	public void features() {
		assertThat(Client.RTORRENT.supports(Feature.VERSION)).isTrue();
		assertThat(Client.RTORRENT.supports(Feature.LISTING)).isTrue();
		assertThat(Client.RTORRENT.supports(Feature.STARTING_STOPPING)).isTrue();
		assertThat(Client.RTORRENT.supports(Feature.RESUMING_PAUSING)).isTrue();
		assertThat(Client.RTORRENT.supports(Feature.FORCE_STARTING)).isFalse();
		assertThat(Client.RTORRENT.supports(Feature.ADD_BY_FILE)).isTrue();
		assertThat(Client.RTORRENT.supports(Feature.ADD_BY_URL)).isTrue();
		assertThat(Client.RTORRENT.supports(Feature.ADD_BY_MAGNET)).isTrue();
	}

	@Test
	public void clientVersion() throws IOException {
		rtorrent.clientVersion()
				.test()
				.assertValue("0.9.6");
	}

	@Test
	public void torrents() throws IOException {
		rtorrent.torrents()
				.toList()
				.test()
				.assertValue(new Predicate<List<Torrent>>() {
					@Override
					public boolean test(List<Torrent> torrents) throws Exception {
						return torrents.size() > 0;
					}
				});
	}

	@Test
	public void addByMagnet() throws IOException {
		rtorrent.addByMagnet("http://torrent.ubuntu.com:6969/file?info_hash=%04%03%FBG%28%BDx%8F%BC%B6%7E%87%D6%FE%B2A%EF8%C7Z")
				.test();
	}

	@Test(expected = UnsupportedFeatureException.class)
	public void forceStart() throws IOException {
		rtorrent.forceStart(null)
				.test();
	}

}
