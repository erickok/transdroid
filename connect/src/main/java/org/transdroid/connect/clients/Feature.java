package org.transdroid.connect.clients;

import org.transdroid.connect.model.Torrent;

import io.reactivex.Flowable;

/**
 * Available feature enum which can be implemented by clients. Use {@link Client#supports(Feature)} to see if a certain {@link Client} support a
 * {@link Feature}.
 */
public enum Feature {

	VERSION(Version.class),
	LISTING(Listing.class),
	STARTING_STOPPING(StartingStopping.class),
	RESUMING_PAUSING(ResumingPausing.class),
	FORCE_STARTING(ForceStarting.class);

	private final Class<?> type;

	Feature(Class<?> type) {
		this.type = type;
	}

	public Class<?> type() {
		return type;
	}

	public interface Version {

		Flowable<String> clientVersion();

	}

	public interface Listing {

		Flowable<Torrent> torrents();

	}

	public interface StartingStopping {

	}

	public interface ResumingPausing {

	}

	public interface ForceStarting {

		Flowable<Torrent> forceStartTorrent();

	}

}
