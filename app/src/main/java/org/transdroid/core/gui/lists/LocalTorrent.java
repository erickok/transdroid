/* 
 * Copyright 2010-2013 Eric Kok et al.
 * 
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.core.gui.lists;

import java.util.Locale;

import org.transdroid.R;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentStatus;
import org.transdroid.daemon.util.FileSizeConverter;
import org.transdroid.daemon.util.TimespanConverter;

import android.content.res.Resources;

/**
 * Wrapper around Torrent to provide some addition getters that give translatable or otherwise formatted Strings of
 * torrent statistics.
 * @author Eric Kok
 */
public class LocalTorrent {

	/**
	 * Creates the LocalTorrent object so that the translatable/formattable version of a Torrent can be used.
	 * @param torrent The Torrent object
	 * @return The torrent wrapped as LocalTorrent object
	 */
	public static LocalTorrent fromTorrent(Torrent torrent) {
		return new LocalTorrent(torrent);
	}

	private final Torrent t;

	private LocalTorrent(Torrent torrent) {
		this.t = torrent;
	}

	private static final String DECIMAL_FORMATTER = "%.1f";

	/**
	 * Builds a string showing the upload/download seed ratio. If not downloading, it will base the ratio on the total
	 * size; so if you created the torrent yourself you will have downloaded 0 bytes, but the ratio will pretend you
	 * have 100%.
	 * @return A nicely formatted string containing the upload/download seed ratio
	 */
	public String getRatioString() {
		long baseSize = t.getTotalSize();
		if (t.getStatusCode() == TorrentStatus.Downloading) {
			baseSize = t.getDownloadedEver();
		}
		if (baseSize <= 0) {
			return String.format(Locale.getDefault(), DECIMAL_FORMATTER, 0d);
		} else if (t.getRatio() == Double.POSITIVE_INFINITY) {
			return "\u221E";
		} else {
			return String.format(Locale.getDefault(), DECIMAL_FORMATTER, t.getRatio());
		}
	}

	/**
	 * Returns a formatted string indicating the current progress in terms of transferred bytes
	 * @param r The context resources, to access translations
	 * @param withAvailability Whether to show file availability in-line
	 * @return A nicely formatted string indicating torrent status and, if applicable, progress in bytes
	 */
	public String getProgressSizeText(Resources r, boolean withAvailability) {

		switch (t.getStatusCode()) {
		case Waiting:
		case Checking:
		case Error:
			// Not downloading yet
			return r.getString(R.string.status_waitingtodl, FileSizeConverter.getSize(t.getTotalSize()));
		case Downloading:
			// Downloading
			return r.getString(
					R.string.status_size1,
					FileSizeConverter.getSize(t.getDownloadedEver()),
					FileSizeConverter.getSize(t.getTotalSize()),
					String.format(DECIMAL_FORMATTER, t.getDownloadedPercentage() * 100)
							+ "%"
							+ (!withAvailability ? "" : "/"
									+ String.format(DECIMAL_FORMATTER, t.getAvailability() * 100) + "%"));
		case Seeding:
		case Paused:
		case Queued:
			// Seeding or paused
			return r.getString(R.string.status_size2, FileSizeConverter.getSize(t.getTotalSize()),
					FileSizeConverter.getSize(t.getUploadedEver()));
		default:
			return "";
		}

	}

	/**
	 * Returns a formatted string indicating either the expected time to download (ETA) or, when seeding, the ratio
	 * @param r The context resources, to access translations
	 * @return A string like '~ 34 seconds', or 'RATIO 8.2' or an empty string
	 */
	public String getProgressEtaRatioText(Resources r) {
		switch (t.getStatusCode()) {
		case Downloading:
			// Downloading
			return getRemainingTimeString(r, true, false);
		case Seeding:
		case Paused:
		case Queued:
			// Seeding or paused
			return r.getString(R.string.status_ratio, getRatioString());
		case Waiting:
		case Checking:
		case Error:
		default:
			return "";
		}
	}

	/**
	 * Returns a formatted string indicating the torrent status and connected peers
	 * @param r The context resources, to access translations
	 * @return A string like 'Queued' or, when seeding or leeching, '2 OF 28 PEERS'
	 */
	public String getProgressConnectionText(Resources r) {

		switch (t.getStatusCode()) {
		case Waiting:
			return r.getString(R.string.status_waiting);
		case Checking:
			return r.getString(R.string.status_checking);
		case Downloading:
			return r.getString(R.string.status_seeders, t.getSeedersConnected(), t.getSeedersKnown());
		case Seeding:
			return r.getString(R.string.status_leechers, t.getLeechersConnected(), t.getLeechersKnown());
		case Paused:
			return r.getString(R.string.status_paused);
		case Queued:
			return r.getString(R.string.status_stopped);
		case Error:
			return r.getString(R.string.status_error);
		default:
			return r.getString(R.string.status_unknown);
		}

	}

	/**
	 * Returns a formatted string indicating current transfer speeds for the torrent
	 * @param r The context resources, to access translations
	 * @return A string like '↓ 28KB/s ↑ 1.8MB/s', or an empty string when not transferrring
	 */
	public String getProgressSpeedText(Resources r) {

		switch (t.getStatusCode()) {
		case Waiting:
		case Checking:
		case Paused:
		case Queued:
			return "";
		case Downloading:
			return r.getString(R.string.status_speed_down, FileSizeConverter.getSize(t.getRateDownload()) + "/s") + " "
					+ r.getString(R.string.status_speed_up, FileSizeConverter.getSize(t.getRateUpload()) + "/s");
		case Seeding:
			return r.getString(R.string.status_speed_up, FileSizeConverter.getSize(t.getRateUpload()) + "/s");
		default:
			return "";
		}

	}

	public String getProgressStatusEta(Resources r) {
		switch (t.getStatusCode()) {
		case Waiting:
			return r.getString(R.string.status_waiting).toUpperCase(Locale.getDefault());
		case Checking:
			return r.getString(R.string.status_checking).toUpperCase(Locale.getDefault());
		case Error:
			return r.getString(R.string.status_error).toUpperCase(Locale.getDefault());
		case Downloading:
			// Downloading
			return r.getString(R.string.status_downloading).toUpperCase(Locale.getDefault()) + " ("
					+ String.format(DECIMAL_FORMATTER, t.getDownloadedPercentage() * 100) + "%), "
					+ getRemainingTimeString(r, false, true);
		case Seeding:
			return r.getString(R.string.status_seeding).toUpperCase(Locale.getDefault());
		case Paused:
			return r.getString(R.string.status_paused).toUpperCase(Locale.getDefault());
		case Queued:
			return r.getString(R.string.status_queued).toUpperCase(Locale.getDefault());
		default:
			return r.getString(R.string.status_unknown).toUpperCase(Locale.getDefault());
		}
	}

	/**
	 * Returns a formatted string indicating the remaining download time
	 * @param r The context resources, to access translations
	 * @param inDays Whether to show days or use hours for > 24 hours left instead
	 * @return A string like '4d 8h 34m 5s' or '2m 3s'
	 */
	public String getRemainingTimeString(Resources r, boolean abbreviate, boolean inDays) {
		if (t.getEta() == -1 || t.getEta() == -2) {
			return r.getString(R.string.status_unknowneta);
		}
		return r.getString(abbreviate ? R.string.status_eta : R.string.status_etalong,
				TimespanConverter.getTime(t.getEta(), inDays));
	}

	/**
	 * Convert a DaemonException to a translatable human-readable error message
	 * @param e The exception that was thrown by the server
	 * @return A string resource ID to show to the user
	 */
	public static int getResourceForDaemonException(DaemonException e) {
		switch (e.getType()) {
		case MethodUnsupported:
			return R.string.error_unsupported;
		case ConnectionError:
			return R.string.error_httperror;
		case UnexpectedResponse:
			return R.string.error_jsonresponseerror;
		case ParsingFailed:
			return R.string.error_jsonrequesterror;
		case NotConnected:
			return R.string.error_daemonnotconnected;
		case AuthenticationFailure:
			return R.string.error_401;
		case FileAccessError:
			return R.string.error_torrentfile;
		default:
			return R.string.error_httperror;
		}
	}

}
