package org.transdroid.connect.clients.rtorrent;

import org.junit.Before;
import org.junit.Test;
import org.transdroid.connect.Configuration;
import org.transdroid.connect.clients.Client;
import org.transdroid.connect.clients.ClientSpec;
import org.transdroid.connect.clients.Feature;
import org.transdroid.connect.model.Torrent;

import java.io.IOException;
import java.util.List;

import io.reactivex.functions.Predicate;

import static com.google.common.truth.Truth.assertThat;

public final class RtorrentTest {

	private ClientSpec rtorrent;

	@Before
	public void setUp() {
		Configuration configuration = new Configuration(Client.RTORRENT, "http://localhost:8008/", "RPC2", null, null, true);
		rtorrent = configuration.create();
	}

	@Test
	public void features() {
		assertThat(Client.RTORRENT.supports(Feature.VERSION)).isTrue();
		assertThat(Client.RTORRENT.supports(Feature.STARTING)).isTrue();
		assertThat(Client.RTORRENT.supports(Feature.STOPPING)).isTrue();
		assertThat(Client.RTORRENT.supports(Feature.RESUMING)).isTrue();
		assertThat(Client.RTORRENT.supports(Feature.PAUSING)).isTrue();
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

}
