package org.transdroid.daemon.Tfb4rt;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentStatus;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * A Torrentflux-b4rt-specific parser for it's stats.xml output.
 * 
 * @author erickok
 *
 */
public class StatsParser {

	public static List<Torrent> parse(Reader in) throws DaemonException {

		try {
			
			// Use a PullParser to handle XML tags one by one
			XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
			//in = new FileReader("/sdcard/tfdebug.xml");
			xpp.setInput(in);

			// Temp variables to load into torrent objects
			int id = 0;
			String tname = "";
			int time = 0;		// Seconds remaining
			int up = 0;			// Upload rate in seconds
			int down = 0;		// Download rate in seconds
			float progress = 0;	// Part [0,1] completed
			TorrentStatus status = TorrentStatus.Unknown;
			long size = 0;		// Total size
			long upSize = -1;	// Total uploaded
			
			// Start pulling
			List<Torrent> torrents = new ArrayList<Torrent>();
			int next = xpp.nextTag();
			String name = xpp.getName();
			if (name.equals("html")) {
				// We are given an html page instead of xml data; probably an authentication error
				throw new DaemonException(DaemonException.ExceptionType.AuthenticationFailure, "HTML tag found instead of XML data; authentication error?");
			}
			if (name.equals("rss")) {
				// We are given an html page instead of xml data; probably an authentication error
				throw new DaemonException(DaemonException.ExceptionType.UnexpectedResponse, "RSS feed found instead of XML data; configuration error?");
			}
			
			while (next != XmlPullParser.END_DOCUMENT) {
				
				if (next == XmlPullParser.END_TAG && name.equals("transfer")) {
					
					// End of a 'transfer' item, add gathered torrent data
					torrents.add(new Torrent(
							id++, 
							tname, 
							tname, 
							status, 
							null, 
							down, 
							up, 
							0, 
							0, 
							0, 
							0, 
							time, 
							(progress > 1L? size: (long)(progress * size)), // Cap the download size to the torrent size
							(upSize == -1? (progress > 1L? (long)(progress * size): 0L): upSize), // If T. Up doesn't exist, we can use the progress size instead
							size, 
							(status == TorrentStatus.Seeding? 1F: progress),
							0f,
							null, // Not supported in the XML stats
							null,
							null,
							null,
							Daemon.Tfb4rt));
					
				} else if (next == XmlPullParser.START_TAG && name.equals("transfer")){
					
					// Start of a new 'transfer' item, for which the name is in the first attribute
					// i.e. '<transfer name="_isoHunt_ubuntu-9.10-desktop-amd64.iso.torrent">'
					tname = xpp.getAttributeValue(0);
					
					// Reset gathered torrent data
					size = 0;
					status = TorrentStatus.Unknown;
					progress = 0;
					down = 0;
					up = 0;
					time = 0;

				} else if (next == XmlPullParser.START_TAG && name.equals("transferStat")){
					
					// Encountered an actual stat, which will always have an attribute name indicating it's type
					// i.e. '<transferStat name="Size">691 MB</transferStat>'
					String type = xpp.getAttributeValue(0);
					next = xpp.next();
					if (next == XmlPullParser.TEXT) {
						try {
							if (type.equals("Size")) {
								size = convertSize(xpp.getText());
							} else if (type.equals("Status")) {
								status = convertStatus(xpp.getText());
							} else if (type.equals("Progress")) {
								progress = convertProgress(xpp.getText());
							} else if (type.equals("Down")) {
								down = convertRate(xpp.getText());
							} else if (type.equals("Up")) {
								up = convertRate(xpp.getText());
							} else if (type.equals("Estimated Time")) {
								time = convertEta(xpp.getText());
							} else if (type.equals("T. Up")) {
								upSize = convertSize(xpp.getText());
							}
						} catch (Exception e) {
							throw new DaemonException(ExceptionType.ConnectionError, e.toString());
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
	 * Returns the part done (or progress) of a torrent, as parsed from some string
	 * @param progress The part done in a string format, i.e. '15%'
	 * @return The part done as [0..1] fraction
	 */
	private static float convertProgress(String progress) {
		if (progress.endsWith("%")) {
			return Float.parseFloat(progress.substring(0, progress.length() - 1).replace(",", "")) / 100;
		}
		return 0;
	}

	/**
	 * Returns the size of the torrent, as parsed form some string
	 * @param size The size in a string format, i.e. '691 MB'
	 * @return The size in number of kB
	 */
	private static long convertSize(String size) {
		if (size.endsWith("GB")) {
			return (long)(Float.parseFloat(size.substring(0, size.length() - 3)) * 1024 * 1024 * 1024);
		} else if (size.endsWith("MB")) {
			return (long)(Float.parseFloat(size.substring(0, size.length() - 3)) * 1024 * 1024);
		} else if (size.endsWith("kB")) {
			return (long)(Float.parseFloat(size.substring(0, size.length() - 3)) * 1024);
		} else if (size.endsWith("B")) {
			return (long)(Float.parseFloat(size.substring(0, size.length() - 2)));
		}
		return 0;
	}

	/**
	 * Returns the eta (estimated time of arrival), as parsed from some string
	 * @param time The time in a string format, i.e. '1d 06:20:48' or '21:36:49'
	 * @return The eta in number of seconds
	 */
	private static int convertEta(String time) {
		
		if (!time.contains(":")) {
			// Not running (something like 'Torrent Stopped' is shown) 
			return -1;
		}
		
		int seconds = 0;
		// Days
		if (time.contains("d ")) {
			seconds += Integer.parseInt(time.substring(0, time.indexOf("d "))) * 60 * 60 * 24;
			time = time.substring(time.indexOf("d ") + 2);
		}
		// Hours, minutes and seconds
		String[] parts = time.split(":");
		if (parts.length > 2) {
			seconds += Integer.parseInt(parts[0]) * 60 * 60;
			seconds += Integer.parseInt(parts[1]) * 60;
			seconds += Integer.parseInt(parts[2]);
		} else if (parts.length > 1) {
			seconds += Integer.parseInt(parts[0]) * 60;
			seconds += Integer.parseInt(parts[1]);
		} else {
			seconds += Integer.parseInt(time);
		}
		return seconds;
	}

	/**
	 * Returns the rate (speed), as parsed from some string
	 * @param rate The rate in a string format, i.e. '9 kB/s'
	 * @return The rate (or speed) in kB/s
	 */
	private static int convertRate(String rate) {
		if (rate.endsWith("MB/s")) {
			return (int) (Float.parseFloat(rate.substring(0, rate.length() - 5)) * 1024 * 1024);
		} else if (rate.endsWith("kB/s")) {
			return (int) (Float.parseFloat(rate.substring(0, rate.length() - 5)) * 1024);
		} else if (rate.endsWith("B/s")) {
			return (int) Float.parseFloat(rate.substring(0, rate.length() - 4));
		}
		return 0;
	}

	/**
	 * Returns the status, as parsed from some string
	 * @param status THe torrent status in a string format, i.e. 'Leeching'
	 * @return The status as TorrentStatus or Unknown if it could not been parsed
	 */
	private static TorrentStatus convertStatus(String status) {
		if (status.equals("Leeching")) {
			return TorrentStatus.Downloading;
		} else if (status.equals("Seeding")) {
			return TorrentStatus.Seeding;
		} else if (status.equals("Stopped")) {
			return TorrentStatus.Paused;
		} else if (status.equals("New")) {
			return TorrentStatus.Paused;
		} else if (status.equals("Done")) {
			return TorrentStatus.Paused;
		}
		return TorrentStatus.Unknown;
	}
	
}
