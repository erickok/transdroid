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

/**
 * The connection status of a tracker as reported by the server daemon. Used to render a coloured
 * status marker next to each tracker in the GUI.
 *
 * @author erickok
 */
public enum TrackerStatus {
    /** The tracker was contacted successfully. */
    WORKING(0),
    /** The tracker returned an error or could not be contacted. */
    ERROR(1),
    /** The tracker is disabled for this torrent. */
    DISABLED(2),
    /** The tracker status is not known (e.g. not yet announced, or the client does not report it). */
    UNKNOWN(3);

    private final int code;

    TrackerStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TrackerStatus fromCode(int code) {
        for (TrackerStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
