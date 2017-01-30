package org.transdroid.connect.clients;

import org.transdroid.connect.Configuration;
import org.transdroid.connect.clients.rtorrent.Rtorrent;

import java.util.Set;

public enum Client {

	RTORRENT {
		@Override
		public ClientSpec create(Configuration configuration) {
			return new Rtorrent(configuration);
		}

		@Override
		Set<Feature> features() {
			return Rtorrent.FEATURES;
		}
	};

	public abstract ClientSpec create(Configuration configuration);

	abstract Set<Feature> features();

	public boolean supports(Feature feature) {
		return features().contains(feature);
	}

}
