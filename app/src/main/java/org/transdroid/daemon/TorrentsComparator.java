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
 */
public class TorrentsComparator implements Comparator<Torrent> {

    private TorrentsSortBy sortBy;
    private boolean reversed;
    private Comparator<String> alphanumComparator = new AlphanumComparator();

    /**
     * Instantiate a torrents comparator. The daemon type is used to check support for comparing on the set property. If
     * the daemon does not support the property, Alphanumeric sorting will be used even if sorting is requested on the
     * unsupported property.
     *
     * @param daemonType The currently loaded server daemon's type, which exposes what features and properties it supports
     * @param sortBy     The requested sorting property (Alphanumeric is used for unsupported properties that are requested)
     * @param reversed   If the sorting should be in reverse order
     */
    public TorrentsComparator(Daemon daemonType, TorrentsSortBy sortBy, boolean reversed) {
        this.sortBy = sortBy;
        this.reversed = reversed;
        if (sortBy == TorrentsSortBy.DateAdded) {
            if (daemonType != null && !Daemon.supportsDateAdded(daemonType)) {
                // Reset the sorting to simple Alphanumeric
                this.sortBy = TorrentsSortBy.Alphanumeric;
                this.reversed = false;
            }
        }
    }

    @Override
    public int compare(Torrent tor1, Torrent tor2) {
        if (!reversed) {
            switch (sortBy) {
                case Status:
                    return tor1.getStatusCode().compareStatusCodeTo(tor2.getStatusCode());
                case DateAdded:
                    if (tor1.getDateAdded() == null)
                        return -1;
                    if (tor2.getDateAdded() == null)
                        return 1;
                    return tor1.getDateAdded().compareTo(tor2.getDateAdded());
                case DateDone:
                    return tor1.getDateDone().compareTo(tor2.getDateDone());
                case Percent:
                    return Float.compare(tor1.getDownloadedPercentage(), tor2.getDownloadedPercentage());
                case DownloadSpeed:
                    return Integer.compare(tor1.getRateDownload(), tor2.getRateDownload());
                case UploadSpeed:
                    return Integer.compare(tor1.getRateUpload(), tor2.getRateUpload());
                case Ratio:
                    return Double.compare(tor1.getRatio(), tor2.getRatio());
                case Size:
                    return Double.compare(tor1.getTotalSize(), (double) tor2.getTotalSize());
                case NumberOfTrackers:
                    return Integer.compare(tor1.getNumberOfTrackers(), tor2.getNumberOfTrackers());
                default:
                    return alphanumComparator.compare(tor1.getName().toLowerCase(), tor2.getName().toLowerCase());
            }
        } else {
            switch (sortBy) {
                case Status:
                    return -tor1.getStatusCode().compareStatusCodeTo(tor2.getStatusCode());
                case DateAdded:
                    if (tor1.getDateAdded() == null)
                        return 1;
                    if (tor2.getDateAdded() == null)
                        return -1;
                    return -tor1.getDateAdded().compareTo(tor2.getDateAdded());
                case DateDone:
                    return -tor1.getDateDone().compareTo(tor2.getDateDone());
                case Percent:
                    return -(Float.compare(tor1.getDownloadedPercentage(), tor2.getDownloadedPercentage()));
                case DownloadSpeed:
                    return -(Integer.compare(tor1.getRateDownload(), tor2.getRateDownload()));
                case UploadSpeed:
                    return -(Integer.compare(tor1.getRateUpload(), tor2.getRateUpload()));
                case Ratio:
                    return -Double.compare(tor1.getRatio(), tor2.getRatio());
                case Size:
                    return -Double.compare(tor1.getTotalSize(), (double) tor2.getTotalSize());
                case NumberOfTrackers:
                    return -Integer.compare(tor1.getNumberOfTrackers(), tor2.getNumberOfTrackers());
                default:
                    return -alphanumComparator.compare(tor1.getName().toLowerCase(), tor2.getName().toLowerCase());
            }
        }
    }

}
