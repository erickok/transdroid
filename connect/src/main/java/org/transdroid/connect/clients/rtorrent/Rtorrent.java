package org.transdroid.connect.clients.rtorrent;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import org.reactivestreams.Publisher;
import org.transdroid.connect.Configuration;
import org.transdroid.connect.clients.Feature;
import org.transdroid.connect.model.Torrent;
import org.transdroid.connect.model.TorrentStatus;
import org.transdroid.connect.util.OkHttpBuilder;
import org.transdroid.connect.util.RxUtil;

import java.io.InputStream;
import java.util.Date;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.functions.Function;
import nl.nl2312.xmlrpc.Nothing;
import nl.nl2312.xmlrpc.XmlRpcConverterFactory;
import retrofit2.Retrofit;

public final class Rtorrent implements
		Feature.Version,
		Feature.Listing,
		Feature.StartingStopping,
		Feature.ResumingPausing,
		Feature.AddByFile,
		Feature.AddByUrl,
		Feature.AddByMagnet {

	private final Configuration configuration;
	private final Service service;

	public Rtorrent(Configuration configuration) {
		this.configuration = configuration;
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(configuration.baseUrl())
				.client(new OkHttpBuilder(configuration).build())
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
				.addConverterFactory(XmlRpcConverterFactory.create())
				.build();
		this.service = retrofit.create(Service.class);
	}

	@Override
	public Flowable<String> clientVersion() {
		return service.clientVersion(configuration.endpoint(), Nothing.NOTHING)
				.cache(); // Cached, as it is often used but 'never' changes
	}

	@Override
	public Flowable<Torrent> torrents() {
		return service.torrents(
				configuration.endpoint(),
				"",
				"main",
				"d.hash=",
				"d.name=",
				"d.state=",
				"d.down.rate=",
				"d.up.rate=",
				"d.peers_connected=",
				"d.peers_not_connected=",
				"d.bytes_done=",
				"d.up.total=",
				"d.size_bytes=",
				"d.left_bytes=",
				"d.creation_date=",
				"d.complete=",
				"d.is_active=",
				"d.is_hash_checking=",
				"d.base_path=",
				"d.base_filename=",
				"d.message=",
				"d.custom=addtime",
				"d.custom=seedingtime",
				"d.custom1=",
				"d.peers_complete=",
				"d.peers_accounted=")
				.compose(RxUtil.<TorrentSpec>asList())
				.map(new Function<TorrentSpec, Torrent>() {
					@Override
					public Torrent apply(TorrentSpec torrentSpec) throws Exception {
						return new Torrent(
								torrentSpec.hash.hashCode(),
								torrentSpec.hash,
								torrentSpec.name,
								torrentStatus(torrentSpec.state, torrentSpec.isComplete, torrentSpec.isActive, torrentSpec.isHashChecking),
								torrentSpec.basePath.substring(0, torrentSpec.basePath.indexOf(torrentSpec.baseFilename)),
								(int) torrentSpec.downloadRate,
								(int) torrentSpec.uploadRate,
								(int) torrentSpec.seedersConnected,
								(int) (torrentSpec.peersConnected + torrentSpec.peersNotConnected),
								(int) torrentSpec.leechersConnected,
								(int) (torrentSpec.peersConnected + torrentSpec.peersNotConnected),
								torrentSpec.downloadRate > 0 ? (torrentSpec.bytesleft / torrentSpec.downloadRate) : Torrent.UNKNOWN,
								torrentSpec.bytesDone,
								torrentSpec.bytesUploaded,
								torrentSpec.bytesTotal,
								torrentSpec.bytesDone / torrentSpec.bytesTotal,
								0F,
								torrentSpec.label,
								torrentTimeAdded(torrentSpec.timeAdded, torrentSpec.timeCreated),
								torrentTimeFinished(torrentSpec.timeFinished),
								torrentSpec.errorMessage
						);
					}
				});
	}

	@Override
	public Flowable<Torrent> start(final Torrent torrent) {
		return service.start(
				configuration.endpoint(),
				torrent.hash()).map(new Function<Void, Torrent>() {
			@Override
			public Torrent apply(Void result) throws Exception {
				return torrent.mimicStart();
			}
		});
	}

	@Override
	public Flowable<Torrent> stop(final Torrent torrent) {
		return service.stop(
				configuration.endpoint(),
				torrent.hash()).map(new Function<Void, Torrent>() {
			@Override
			public Torrent apply(Void result) throws Exception {
				return torrent.mimicStart();
			}
		});
	}

	@Override
	public Flowable<Void> addByFile(InputStream file) {
		// TODO
		return null;
	}

	@Override
	public Flowable<Void> addByUrl(final String url) {
		return clientVersion().compose(clientVersionAsInt).flatMap(new Function<Integer, Publisher<Integer>>() {
			@Override
			public Publisher<Integer> apply(Integer integer) throws Exception {
				if (integer > 904) {
					return service.loadStart(
							configuration.endpoint(),
							"",
							url);
				} else {
					return service.loadStart(
							configuration.endpoint(),
							url);
				}
			}
		}).map(new Function<Integer, Void>() {
			@Override
			public Void apply(Integer integer) throws Exception {
				return null;
			}
		});
	}

	@Override
	public Flowable<Void> addByMagnet(final String magnet) {
		return clientVersion().compose(clientVersionAsInt).flatMap(new Function<Integer, Publisher<Integer>>() {
			@Override
			public Publisher<Integer> apply(Integer integer) throws Exception {
				if (integer > 904) {
					return service.loadStart(
							configuration.endpoint(),
							"",
							magnet);
				} else {
					return service.loadStart(
							configuration.endpoint(),
							magnet);
				}
			}
		}).map(new Function<Integer, Void>() {
			@Override
			public Void apply(Integer integer) throws Exception {
				return null;
			}
		});
	}

	private FlowableTransformer<String, Integer> clientVersionAsInt = new FlowableTransformer<String, Integer>() {
		@Override
		public Publisher<Integer> apply(Flowable<String> version) {
			return version.map(new Function<String, Integer>() {
				@Override
				public Integer apply(String version) throws Exception {
					if (version == null)
						return 10000;
					try {
						String[] versionParts = version.split("\\.");
						return (Integer.parseInt(versionParts[0]) * 10000) + (Integer.parseInt(versionParts[1]) * 100) + Integer.parseInt
								(versionParts[2]);
					} catch (NumberFormatException e) {
						return 10000;
					}
				}
			});
		}
	};

	private TorrentStatus torrentStatus(long state, long complete, long active, long checking) {
		if (state == 0) {
			return TorrentStatus.QUEUED;
		} else if (active == 1) {
			if (complete == 1) {
				return TorrentStatus.SEEDING;
			} else {
				return TorrentStatus.DOWNLOADING;
			}
		} else if (checking == 1) {
			return TorrentStatus.CHECKING;
		} else {
			return TorrentStatus.PAUSED;
		}
	}

	private Date torrentTimeAdded(String timeAdded, long timeCreated) {
		if (timeAdded != null || timeAdded.trim().length() != 0) {
			return new Date(Long.parseLong(timeAdded.trim()) * 1000L);
		}
		return new Date(timeCreated * 1000L);
	}

	private Date torrentTimeFinished(String timeFinished) {
		if (timeFinished == null || timeFinished.trim().length() == 0)
			return null;
		return new Date(Long.parseLong(timeFinished.trim()) * 1000L);
	}

}
