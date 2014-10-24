package org.transdroid.daemon.Ktorrent;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.Priority;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * A Ktorrent-specific parser for it's /data/torrent/files.xml output.
 * 
 * @author erickok
 *
 */
public class FileListParser {

	public static List<TorrentFile> parse(Reader in, String torrentDownloadDir) throws DaemonException, LoggedOutException {

		try {
			
			// Use a PullParser to handle XML tags one by one
			XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
			xpp.setInput(in);

			// Temp variables to load into torrent objects
			int i = 0;
			String path = "";
			long size = 0;
			float partDone = 0;
			Priority priority = Priority.Normal;
			
			// Start pulling
			List<TorrentFile> torrents = new ArrayList<TorrentFile>();
			int next = xpp.nextTag();
			String name = xpp.getName();
			
			// Check if we had a proper XML result
			if (name.equals("html")) {
				// Apparently we were returned an HTML page instead of the expected XML
				// This happens in particular when we were logged out (because somebody else logged into KTorrent's web interface)
				throw new LoggedOutException();
			}
			
			while (next != XmlPullParser.END_DOCUMENT) {
				
				if (next == XmlPullParser.END_TAG && name.equals("file")) {
					
					// End of a 'transfer' item, add gathered torrent data
					torrents.add(new TorrentFile(
							"" + i,
							path, 
							path,
							(torrentDownloadDir == null? null: torrentDownloadDir + path),
							size,
							(long) (size * partDone), 
							priority));
					
				} else if (next == XmlPullParser.START_TAG && name.equals("file")){
					
					// Start of a new 'transfer' item; reset gathered torrent data
					i++; // Increase the file index identifier
					path = "";
					size = 0;
					partDone = 0;
					priority = Priority.Normal;

				} else if (next == XmlPullParser.START_TAG){
					
					// Probably encountered a file property, i.e. '<percentage>73.09</percentage>'
					next = xpp.next();
					if (next == XmlPullParser.TEXT) {
						if (name.equals("path")) {
							path = xpp.getText().trim();
						} else if (name.equals("size")) {
							size = StatsParser.convertSize(xpp.getText());
						} else if (name.equals("priority")) {
							priority = convertPriority(xpp.getText());
						} else if (name.equals("percentage")) {
							partDone = StatsParser.convertProgress(xpp.getText());
						}
					}
				}

				next = xpp.next();
				if (next == XmlPullParser.START_TAG || next == XmlPullParser.END_TAG) {
					name = xpp.getName();
				}
				
			}
			
			return torrents;
			
		} catch (XmlPullParserException e) {
			throw new DaemonException(ExceptionType.ParsingFailed, e.toString());
		} catch (IOException e) {
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}
		
	}

	/**
	 * Returns the priority of a file, as parsed from some string
	 * @param priority The priority in a numeric string, i.e. '20'
	 * @return The priority as enum type, i.e. Priority.Off
	 */
	private static Priority convertPriority(String priority) {
		if (priority.equals("20")) {
			return Priority.Off;
		} else if (priority.equals("30")) {
			return Priority.Low;
		} else if (priority.equals("50")) {
			return Priority.High;
		} else {
			return Priority.Normal;
		}
	}

}
