package org.transdroid.connect.clients;

import org.transdroid.connect.model.Torrent;

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
	public Flowable<Torrent> forceStartTorrent() {
		if (client.supports(Feature.FORCE_STARTING))
			return ((Feature.ForceStarting) actual).forceStartTorrent();
		throw new UnsupportedFeatureException(client, Feature.FORCE_STARTING);
	}

}
