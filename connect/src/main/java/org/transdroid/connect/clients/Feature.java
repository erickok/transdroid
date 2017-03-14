package org.transdroid.connect.clients;

import org.transdroid.connect.model.Torrent;

import java.io.InputStream;

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
	FORCE_STARTING(ForceStarting.class),
	ADD_BY_FILE(AddByFile.class),
	ADD_BY_URL(AddByUrl.class),
	ADD_BY_MAGNET(AddByMagnet.class);

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

		Flowable<Torrent> start(Torrent torrent);

		Flowable<Torrent> stop(Torrent torrent);

	}

	public interface ResumingPausing {

	}

	public interface ForceStarting {

		Flowable<Torrent> forceStart(Torrent torrent);

	}

	public interface AddByFile {

		Flowable<Void> addByFile(InputStream file);

	}

	public interface AddByUrl {

		Flowable<Void> addByUrl(String url);

	}

	public interface AddByMagnet {

		Flowable<Void> addByMagnet(String magnet);

	}

}
