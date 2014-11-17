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
 * Implements a comparator for TorrentFile objects, which can be used to sort 
 * a list listing in the ways specified by the TorrentFilesSortBy enum
 * 
 * @author erickok
 *
 */
public class TorrentFilesComparator implements Comparator<TorrentFile> {

	private TorrentFilesSortBy sortBy;
	private boolean reversed;
	private Comparator<String> alphanumComparator = new AlphanumComparator();
	
	/**
	 * Instantiate a torrent files comparator.
	 * @param sortBy The requested sorting property (Alphanumeric is used for unsupported properties that are requested)
	 * @param reversed If the sorting should be in reverse order
	 */
	public TorrentFilesComparator(TorrentFilesSortBy sortBy, boolean reversed) {
		this.sortBy = sortBy;
		this.reversed = reversed;
	}
	
	@Override
	public int compare(TorrentFile file1, TorrentFile file2) {
		if (!reversed) {
			switch (sortBy) {
			case PartDone:
				return Float.compare(file1.getPartDone(), file2.getPartDone());
			case TotalSize:
				return Long.valueOf(file1.getTotalSize()).compareTo(file2.getTotalSize());
			default:
				return alphanumComparator.compare(file1.getName().toLowerCase(), file2.getName().toLowerCase());
			}
		} else {
			switch (sortBy) {
			case PartDone:
				return 0 - Float.compare(file1.getPartDone(), file2.getPartDone());
			case TotalSize:
				return 0 - Long.valueOf(file1.getTotalSize()).compareTo(file2.getTotalSize());
			default:
				return 0 - alphanumComparator.compare(file1.getName().toLowerCase(), file2.getName().toLowerCase());
			}
		}
	}

}
