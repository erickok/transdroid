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

import java.util.Comparator;

/**
 * Implements a comparator for Torrent objects, which can for example be used to sort torrent on several 
 * different properties, which can be found in the TorrentsSortBy enum
 * 
 * @author erickok
 *
 */
public class TorrentsComparator implements Comparator<Torrent> {

	TorrentsSortBy sortBy;
	boolean reversed;
	
	/**
	 * Instantiate a torrents comparator. The daemon object is used to check support for comparing 
	 * on the set properties. If the daemon does not support the property, ascending Alphanumeric  
	 * sorting will be used even if sorting is requested on the unsupported property.
	 * @param daemon The loaded server daemon, which exposes what features and properties it supports
	 * @param sortBy The requested sorting property (Alphanumeric is used for unsupported properties that are requested)
	 * @param reversed If the sorting should be in reverse order
	 */
	public TorrentsComparator(IDaemonAdapter daemon, TorrentsSortBy sortBy, boolean reversed) {
		this.sortBy = sortBy;
		this.reversed = reversed;
		switch (sortBy) {
		case DateAdded:
			if (daemon != null && !Daemon.supportsDateAdded(daemon.getType())) {
				// Reset the sorting to simple Alphanumeric
				this.sortBy = TorrentsSortBy.Alphanumeric;
				this.reversed = false;
			}
			break;
		}
	}
	
	@Override
	public int compare(Torrent tor1, Torrent tor2) {
		if (!reversed) {
			switch (sortBy) {
			case Status:
				return tor1.getStatusCode().compareStatusCodeTo(tor2.getStatusCode());
			case DateAdded:
				return tor1.getDateAdded().compareTo(tor2.getDateAdded());
			case DateDone:
				return tor1.getDateDone().compareTo(tor2.getDateDone());
			case UploadSpeed:
				return new Integer(tor1.getRateUpload()).compareTo(new Integer(tor2.getRateUpload()));
			case Ratio:
				return new Double(tor1.getRatio()).compareTo(new Double(tor2.getRatio()));
			default:
				return tor1.getName().toLowerCase().compareTo(tor2.getName().toLowerCase());
			}
		} else {
			switch (sortBy) {
			case Status:
				return 0 - tor1.getStatusCode().compareStatusCodeTo(tor2.getStatusCode());
			case DateAdded:
				return 0 - tor1.getDateAdded().compareTo(tor2.getDateAdded());
			case DateDone:
				return 0 - tor1.getDateDone().compareTo(tor2.getDateDone());
			case UploadSpeed:
				return 0 - (new Integer(tor1.getRateUpload()).compareTo(new Integer(tor2.getRateUpload())));
			case Ratio:
				return 0 - new Double(tor1.getRatio()).compareTo(new Double(tor2.getRatio()));
			default:
				return 0 - tor1.getName().toLowerCase().compareTo(tor2.getName().toLowerCase());
			}
		}
	}

}
