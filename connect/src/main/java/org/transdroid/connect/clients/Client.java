package org.transdroid.connect.clients;

import org.transdroid.connect.Configuration;
import org.transdroid.connect.clients.rtorrent.Rtorrent;
import org.transdroid.connect.clients.transmission.Transmission;

/**
 * Support clients enum, allowing you to create instances (given a configuration) and query for feature support.
 */
@SuppressWarnings("unchecked")
public enum Client {

	RTORRENT(Rtorrent.class) {
		@Override
		public Rtorrent create(Configuration configuration) {
			return new Rtorrent(configuration);
		}
	},
	TRANSMISSION(Transmission.class) {
		@Override
		public Transmission create(Configuration configuration) {
			return new Transmission();
		}
	};

	final Class<?> type;

	Client(Class<?> type) {
		this.type = type;
	}

	public final Class<?> type() {
		return type;
	}

	abstract Object create(Configuration configuration);

	public final ClientSpec createClient(Configuration configuration) {
		return new ClientDelegate(configuration.client(), create(configuration));
	}

	public final boolean supports(Feature feature) {
		return feature.type().isAssignableFrom(type);
	}

}
