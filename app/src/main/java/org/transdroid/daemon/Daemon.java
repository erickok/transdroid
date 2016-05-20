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

import org.transdroid.daemon.Aria2c.Aria2Adapter;
import org.transdroid.daemon.BitComet.BitCometAdapter;
import org.transdroid.daemon.Bitflu.BitfluAdapter;
import org.transdroid.daemon.BuffaloNas.BuffaloNasAdapter;
import org.transdroid.daemon.DLinkRouterBT.DLinkRouterBTAdapter;
import org.transdroid.daemon.Deluge.DelugeAdapter;
import org.transdroid.daemon.Ktorrent.KtorrentAdapter;
import org.transdroid.daemon.Qbittorrent.QbittorrentAdapter;
import org.transdroid.daemon.Rtorrent.RtorrentAdapter;
import org.transdroid.daemon.Synology.SynologyAdapter;
import org.transdroid.daemon.Tfb4rt.Tfb4rtAdapter;
import org.transdroid.daemon.Transmission.TransmissionAdapter;
import org.transdroid.daemon.Ttorrent.TtorrentAdapter;
import org.transdroid.daemon.Utorrent.UtorrentAdapter;
import org.transdroid.daemon.Vuze.VuzeAdapter;

/**
 * Factory for new instances of server daemons, based on user settings.
 * 
 * @author erickok
 *
 */
public enum Daemon {

	Aria2 {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new Aria2Adapter(settings);
		}
	},
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
	Dummy {
		public IDaemonAdapter createAdapter(DaemonSettings settings) { 
			return new DummyAdapter(settings);
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
	tTorrent {
		public IDaemonAdapter createAdapter(DaemonSettings settings) {
			return new TtorrentAdapter(settings);
		}
	},
	Synology {
		public IDaemonAdapter createAdapter(DaemonSettings settings) {
			return new SynologyAdapter(settings);
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
	 * Returns the code as used in preferences matching the given daemon type
	 * @return A string of the form 'daemon_<type>' that represents the daemon's enum value
	 */
	public static String toCode(Daemon type) {
		if (type == null)
			return null;
		switch (type) {
		case Aria2:
			return "daemon_aria2";
		case BitComet:
			return "daemon_bitcomet";
		case Bitflu:
			return "daemon_bitflue";
		case BitTorrent:
			return "daemon_bittorrent";
		case BuffaloNas:
			return "daemon_buffalonas";
		case Deluge:
			return "daemon_deluge";
		case DLinkRouterBT:
			return "daemon_dlinkrouterbt";
		case Dummy:
			return "daemon_dummy";
		case KTorrent:
			return "daemon_ktorrent";
		case qBittorrent:
			return "daemon_qbittorrent";
		case rTorrent:
			return "daemon_rtorrent";
		case Synology:
			return "daemon_synology";
		case Tfb4rt:
			return "daemon_tfb4rt";
		case tTorrent:
			return "daemon_ttorrent";
		case Transmission:
			return "daemon_transmission";
		case uTorrent:
			return "daemon_utorrent";
		case Vuze:
			return "daemon_vuze";
		default:
			return null;
		}
	}

	/**
	 * Returns the daemon enum type based on the code used in the user preferences.
	 * @param daemonCode The 'daemon_&lt;name&gt;' type code
	 * @return The corresponding daemon enum value, or null if it was an empty or invalid code
	 */
	public static Daemon fromCode(String daemonCode) {
		if (daemonCode == null) {
			return null;
		}
		if (daemonCode.equals("daemon_aria2")) {
			return Aria2;
		}
		if (daemonCode.equals("daemon_bitcomet")) {
			return BitComet;
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
		if (daemonCode.equals("daemon_dummy")) {
			return Dummy;
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
		if (daemonCode.equals("daemon_synology")) {
			return Synology;
		}
		if (daemonCode.equals("daemon_tfb4rt")) {
			return Tfb4rt;
		}
		if (daemonCode.equals("daemon_ttorrent")) {
			return tTorrent;
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
        case Dummy:
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
        case Synology:
        	if (ssl) {
        		return 5001;
        	} else {
        		return 5000;
        	}
        case Transmission:
        	return 9091;
        case Bitflu:
        	return 4081;
        case Vuze:
        	return 6884;
        case Aria2:
        	return 6800;
		case tTorrent:
			return 1080;
        }
		return 8080;
	}

	public static boolean supportsStats(Daemon type) {
		return type == Transmission || type == Bitflu || type == Dummy;
	}

	public static boolean supportsAvailability(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == DLinkRouterBT || type == Transmission || type == Vuze || type == BuffaloNas || type == Dummy;
	}

	public static boolean supportsFileListing(Daemon type) {
		return type == Synology || type == Transmission || type == uTorrent || type == BitTorrent || type == KTorrent || type == Deluge || type == rTorrent || type == Vuze || type == DLinkRouterBT || type == Bitflu || type == qBittorrent || type == BuffaloNas || type == BitComet || type == Aria2 || type == tTorrent || type == Dummy;
	}

	public static boolean supportsFineDetails(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == Daemon.Transmission || type == Deluge || type == rTorrent || type == qBittorrent || type == Aria2 || type == Dummy;
	}

	public static boolean needsManualPathSpecified(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == KTorrent || type == BuffaloNas;
	}

	public static boolean supportsFilePaths(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == Vuze || type == Deluge || type == Transmission || type == rTorrent || type == KTorrent || type == BuffaloNas || type == Aria2 || type == Dummy;
	}

	public static boolean supportsStoppingStarting(Daemon type) {
		return type == uTorrent || type == rTorrent || type == BitTorrent || type == BitComet || type == Dummy;
	}

	public static boolean supportsForcedStarting(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == Dummy;
	}

	public static boolean supportsCustomFolder(Daemon type) {
		return type == rTorrent || type == Tfb4rt || type == Bitflu || type == Deluge || type == Transmission || type == BitTorrent || type == uTorrent || type == Dummy;
	}

	public static boolean supportsSetTransferRates(Daemon type) {
		return type == Deluge || type == Transmission || type == uTorrent || type == BitTorrent || type == rTorrent || type == Vuze || type == BuffaloNas || type == BitComet || type == Aria2 || type == Dummy;
	}

	public static boolean supportsAddByFile(Daemon type) {
		// Supported by every client except Bitflu 
		return type != Bitflu;
	}

	public static boolean supportsAddByMagnetUrl(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == Transmission || type == Synology || type == Deluge || type == Bitflu || type == KTorrent || type == rTorrent || type == qBittorrent || type == BitComet || type == Aria2 || type == tTorrent || type == Dummy;
	}
	
	public static boolean supportsRemoveWithData(Daemon type) {
		return type == uTorrent || type == Vuze || type == Transmission || type == Deluge || type == BitTorrent || type == Tfb4rt || type == DLinkRouterBT || type == Bitflu || type == qBittorrent || type == BuffaloNas || type == BitComet || type == rTorrent || type == Aria2 || type == tTorrent || type == Dummy;
	}

	public static boolean supportsFilePrioritySetting(Daemon type) {
		return type == BitTorrent || type == uTorrent || type == Transmission || type == KTorrent || type == rTorrent || type == Vuze || type == Deluge || type == qBittorrent || type == tTorrent || type == Dummy;
	}
	
	public static boolean supportsDateAdded(Daemon type) {
		return type == Vuze || type == Transmission || type == rTorrent || type == Bitflu || type == BitComet || type == uTorrent || type == BitTorrent || type == Deluge || type == Dummy;
	}

	public static boolean supportsLabels(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == Deluge || type == BitComet || type == rTorrent || type == Dummy;
	}

	public static boolean supportsSetLabel(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == rTorrent || type == Deluge || type == Dummy;
	}

	public static boolean supportsSetDownloadLocation(Daemon type) {
		return type == Transmission || type == Deluge || type == Dummy;
	}

	public static boolean supportsSetAlternativeMode(Daemon type) {
		return type == Transmission || type == Dummy;
	}

	public static boolean supportsSetTrackers(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == Deluge || type == Dummy;
	}

	public static boolean supportsForceRecheck(Daemon type) {
		return type == uTorrent || type == BitTorrent || type == Deluge || type == rTorrent || type == Transmission || type == Dummy || type == qBittorrent;
	}

	public static boolean supportsExtraPassword(Daemon type) {
		return type == Deluge || type == Aria2;
	}

	public static boolean supportsUsernameForHttp(Daemon type) {
		return type == Deluge || type == Aria2;
	}

}
