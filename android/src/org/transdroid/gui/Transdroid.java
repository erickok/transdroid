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
package org.transdroid.gui;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;

/**
 * The application's constants.
 * 
 * @author erickok
 *
 */
public class Transdroid {

	public final static String SCAN_INTENT = "com.google.zxing.client.android.SCAN";
	public static final Uri SCANNER_MARKET_URI = Uri.parse("market://search?q=pname:com.google.zxing.client.android");
	public static final String SCAN_FORMAT_QRCODE = "QR_CODE";
    
	public static final String BITTORRENT_MIME = "application/x-bittorrent";
	public static final String INTENT_OPENDAEMON = "org.transdroid.OPEN_DAEMON";
	public static final String INTENT_ADD_MULTIPLE = "org.transdroid.ADD_MULTIPLE";
	public static final String INTENT_TORRENT_URLS = "TORRENT_URLS";
	public static final String INTENT_TORRENT_TITLES = "TORRENT_TITLES";
	public static final String INTENT_TORRENT_TITLE = "TORRENT_TITLE";

	public static final String REMOTEINTENT = "org.openintents.remote.intent.action.VIEW";
	public static final String REMOTEINTENT_HOST = "org.openintents.remote.intent.extra.HOST";
	public final static Uri VLCREMOTE_MARKET_URI = Uri.parse("market://search?q=pname:org.peterbaldwin.client.android.vlcremote");
	public final static Uri ANDFTP_MARKET_URI = Uri.parse("market://search?q=pname:lysesoft.andftp");
	public final static String ANDFTP_INTENT_TYPE = "vnd.android.cursor.dir/lysesoft.andftp.uri";
	public final static String ANDFTP_INTENT_USER = "ftp_username";
	public final static String ANDFTP_INTENT_PASS = "ftp_password";
	public final static String ANDFTP_INTENT_PASV = "ftp_pasv";
	public final static String ANDFTP_INTENT_CMD = "command_type";
	public final static String ANDFTP_INTENT_FILE = "remote_file1";
	public final static String ANDFTP_INTENT_LOCAL = "local_folder";

	/**
	 * Returns whether the device is a tablet (or better: whether it can fit a tablet layout)
	 * @param r The application resources
	 * @return True if the device is a tablet, false otherwise
	 */
	public static boolean isTablet(Resources r) {
		//boolean hasLargeScreen = ((r.getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
		boolean hasXLargeScreen = ((r.getConfiguration().screenLayout & 
				Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE) &&
				android.os.Build.VERSION.SDK_INT >= 11;
		return hasXLargeScreen;
	}

}
