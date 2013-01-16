/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
package org.transdroid.daemon;

import org.transdroid.daemon.Deluge.DelugeAdapter;
import org.transdroid.daemon.DLinkRouterBT.DLinkRouterBTAdapter;
import org.transdroid.daemon.Ktorrent.KtorrentAdapter;
import org.transdroid.daemon.Qbittorrent.QbittorrentAdapter;
import org.transdroid.daemon.Rtorrent.RtorrentAdapter;
import org.transdroid.daemon.Tfb4rt.Tfb4rtAdapter;
import org.transdroid.daemon.Transmission.TransmissionAdapter;
import org.transdroid.daemon.Utorrent.UtorrentAdapter;
import org.transdroid.daemon.Vuze.VuzeAdapter;
import org.transdroid.daemon.Bitflu.BitfluAdapter;
import org.transdroid.daemon.BuffaloNas.BuffaloNasAdapter;
import org.transdroid.daemon.BitComet.BitCometAdapter;

/**
 * Factory for new instances of server daemons, based on user settings.
 * 
 * @author erickok
 *
 */
public enum Daemon {

	Bitflu {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new BitfluAdapter(settings);
		}
	},
	BitTorrent {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new UtorrentAdapter(settings);
		}
	},
	BuffaloNas {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new BuffaloNasAdapter(settings);
		}
	},
	Deluge {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new DelugeAdapter(settings);
		}
	},
	DLinkRouterBT {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new DLinkRouterBTAdapter(settings);
		}
	},
	KTorrent {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new KtorrentAdapter(settings);
		}
	},
	qBittorrent {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new QbittorrentAdapter(settings);
		}
	},
	rTorrent {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new RtorrentAdapter(settings);
		}
	},
	Tfb4rt {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new Tfb4rtAdapter(settings);
		}
	},
	Transmission {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new TransmissionAdapter(settings);
		}
	},
	uTorrent {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new UtorrentAdapter(settings);
		}
	},
	Vuze {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new VuzeAdapter(settings);
		}
	},
	BitComet {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new BitCometAdapter(settings);
		}
	};
	
	public abstract IDaemonAdapter createAdapter(DaemonSettings settings);

	/**
	 * Returns the daemon enum type based on the code used in the user preferences.
	 * @param daemonCode The 'daemon_&lt;name&gt;' type code
	 * @return The corresponding daemon enum value, or null if it was an empty or invalid code
	 */
	public static Daemon fromCode(String daemonCode) {
		if (daemonCode == null) {
			return null;
		}
		if (daemonCode.equals("daemon_bitflu")) {
			return Bitflu;
		}
		if (daemonCode.equals("daemon_bittorrent")) {
			return BitTorrent;
		}
		if (daemonCode.equals("daemon_buffalonas")) {
			return BuffaloNas;
		}
		if (daemonCode.equals("daemon_deluge")) {
			return Deluge;
		}
		if (daemonCode.equals("daemon_dlinkrouterbt")) {
			return DLinkRouterBT;
		}
		if (daemonCode.equals("daemon_ktorrent")) {
			return KTorrent;
		}
		if (daemonCode.equals("daemon_qbittorrent")) {
			return qBittorrent;
		}
		if (daemonCode.equals("daemon_rtorrent")) {
			return rTorrent;
		}
		if (daemonCode.equals("daemon_tfb4rt")) {
			return Tfb4rt;
		}
		if (daemonCode.equals("daemon_transmission")) {
			return Transmission;
		}
		if (daemonCode.equals("daemon_utorrent")) {
			return uTorrent;
		}
		if (daemonCode.equals("daemon_vuze")) {
			return Vuze;
		}
		if (daemonCode.equals("daemon_bitcomet")) {
			return BitComet;
		}
		return null;
	}

	public static int getDefaultPortNumber(Daemon type, boolean ssl) {
		if (type == null) {
			return 8080; // Only happens when the daemon type isn't even set yet
		}
        switch (type) {
        case BitTorrent:
        case uTorrent:
        case KTorrent:
        case qBittorrent:
        case BuffaloNas:
        	return 8080;
        case DLinkRouterBT:
        case rTorrent:
        case Tfb4rt:
        case BitComet:
        	if (ssl) {
        		return 443;
        	} else {
        		return 80;
        	}
        case Deluge:
        	return 8112;
        case Transmission:
        	return 9091;
        case Bitflu:
        	return 4081;
        case Vuze:
        	return 6884;
        }
		return 8080;
	}

	public static boolean supportsStats(Daemon type) {
		return type == Transmission || type == Bitflu;
	}

	public static boolean supportsAvailability(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == DLinkRouterBT || type == Transmission || type == Vuze || type == BuffaloNas;
	}

	public static boolean supportsFileListing(Daemon type) {
		return type == Transmission || type == uTorrent || type == BitTorrent || type == KTorrent || type == Deluge || type == rTorrent || type == Vuze || type == DLinkRouterBT || type == Bitflu || type == qBittorrent || type == BuffaloNas || type == BitComet;
	}

	public static boolean supportsFineDetails(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == Daemon.Transmission || type == Deluge || type == rTorrent || type == qBittorrent;
	}

	public static boolean needsManualPathSpecified(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == KTorrent || type == BuffaloNas;
	}

	public static boolean supportsFilePaths(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == Vuze || type == Deluge || type == Transmission || type == rTorrent || type == KTorrent || type == BuffaloNas;
	}

	public static boolean supportsStoppingStarting(Daemon type) {
		return type == uTorrent || type == rTorrent || type == BitTorrent || type == BitComet;
	}

	public static boolean supportsForcedStarting(Daemon type) {
		return type == uTorrent || type == BitTorrent;
	}

	public static boolean supportsCustomFolder(Daemon type) {
		return type == rTorrent || type == Tfb4rt || type == Bitflu || type == Deluge || type == Transmission;
	}

	public static boolean supportsSetTransferRates(Daemon type) {
		return type == Deluge || type == Transmission || type == uTorrent || type == BitTorrent || type == Deluge || type == rTorrent || type == Vuze || type == BuffaloNas || type == BitComet;
	}

	public static boolean supportsAddByFile(Daemon type) {
		// Supported by every client except Bitflu 
		return type != Bitflu;
	}

	public static boolean supportsAddByMagnetUrl(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == Transmission || type == Deluge || type == Bitflu || type == KTorrent || type == rTorrent || type == qBittorrent || type == BitComet;
	}
	
	public static boolean supportsRemoveWithData(Daemon type) {
		return type == uTorrent || type == Vuze || type == Transmission || type == Deluge || type == BitTorrent || type == Tfb4rt || type == DLinkRouterBT || type == Bitflu || type == qBittorrent || type == BuffaloNas || type == BitComet || type == rTorrent;
	}

	public static boolean supportsFilePrioritySetting(Daemon type) {
		return type == BitTorrent || type == uTorrent || type == Transmission || type == KTorrent || type == rTorrent || type == Vuze || type == Deluge || type == qBittorrent;
	}
	
	public static boolean supportsDateAdded(Daemon type) {
		return type == Vuze || type == Transmission || type == rTorrent || type == Bitflu || type == BitComet;
	}

	public static boolean supportsLabels(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == Deluge || type == BitComet; // || type == Vuze
	}

	public static boolean supportsSetLabel(Daemon type) {
		return type == uTorrent || type == BitTorrent;
	}

	public static boolean supportsSetDownloadLocation(Daemon type) {
		return type == Transmission || type == Deluge;
	}

	public static boolean supportsSetAlternativeMode(Daemon type) {
		return type == Transmission;
	}

	public static boolean supportsSetTrackers(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == Deluge;
	}

	public static boolean supportsExtraPassword(Daemon type) {
		return type == Deluge;
	}

	public static boolean supportsUsernameForHttp(Daemon type) {
		return type == Deluge;
	}

}
