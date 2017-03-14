package org.transdroid.connect.clients;

import org.transdroid.connect.model.Torrent;

import java.io.InputStream;

import io.reactivex.Flowable;

/**
 * Wraps an actual client implementation by calling through the appropriate method only if it is supported. This allows the final
 * {@link ClientSpec} API to expose all methods without forcing the individual implementations to implement unsupported featured with a no-op.
 */
final class ClientDelegate implements ClientSpec {

	private final Client client;
	private final Object actual;

	ClientDelegate(Client client, Object actual) {
		this.client = client;
		this.actual = actual;
	}

	@Override
	public Flowable<Torrent> torrents() {
		if (client.supports(Feature.LISTING))
			return ((Feature.Listing) actual).torrents();
		throw new UnsupportedFeatureException(client, Feature.LISTING);
	}

	@Override
	public Flowable<String> clientVersion() {
		if (client.supports(Feature.VERSION))
			return ((Feature.Version) actual).clientVersion();
		throw new UnsupportedFeatureException(client, Feature.VERSION);
	}

	@Override
	public Flowable<Torrent> start(Torrent torrent) {
		if (client.supports(Feature.STARTING_STOPPING))
			return ((Feature.StartingStopping) actual).start(torrent);
		throw new UnsupportedFeatureException(client, Feature.STARTING_STOPPING);
	}

	@Override
	public Flowable<Torrent> stop(Torrent torrent) {
		if (client.supports(Feature.STARTING_STOPPING))
			return ((Feature.StartingStopping) actual).stop(torrent);
		throw new UnsupportedFeatureException(client, Feature.STARTING_STOPPING);
	}

	@Override
	public Flowable<Torrent> forceStart(Torrent torrent) {
		if (client.supports(Feature.FORCE_STARTING))
			return ((Feature.ForceStarting) actual).forceStart(torrent);
		throw new UnsupportedFeatureException(client, Feature.FORCE_STARTING);
	}

	@Override
	public Flowable<Void> addByFile(InputStream file) {
		if (client.supports(Feature.ADD_BY_FILE))
			return ((Feature.AddByFile) actual).addByFile(file);
		throw new UnsupportedFeatureException(client, Feature.ADD_BY_FILE);
	}

	@Override
	public Flowable<Void> addByUrl(String url) {
		if (client.supports(Feature.ADD_BY_URL))
			return ((Feature.AddByUrl) actual).addByUrl(url);
		throw new UnsupportedFeatureException(client, Feature.ADD_BY_URL);
	}

	@Override
	public Flowable<Void> addByMagnet(String magnet) {
		if (client.supports(Feature.ADD_BY_MAGNET))
			return ((Feature.AddByMagnet) actual).addByMagnet(magnet);
		throw new UnsupportedFeatureException(client, Feature.ADD_BY_MAGNET);
	}

}
