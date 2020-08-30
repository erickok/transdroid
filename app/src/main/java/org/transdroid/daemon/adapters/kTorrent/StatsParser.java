package org.transdroid.daemon.adapters.kTorrent;

import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentStatus;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * A Ktorrent-specific parser for it's /data/torrents.xml output.
 *
 * @author erickok
 */
public class StatsParser {

    public static List<Torrent> parse(Reader in, String baseDir, String pathSeperator) throws DaemonException, LoggedOutException {

        try {

            // Use a PullParser to handle XML tags one by one
            XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
            xpp.setInput(in);
            //xpp.setInput(new FileReader("/sdcard/torrents.xml")); // Used for debugging

            // Temp variables to load into torrent objects
            int id = 0;
            String tname = "";
            //String hash = "";
            TorrentStatus status = TorrentStatus.Unknown;
            long down = 0;
            long up = 0;
            long total = 0;
            int downRate = 0;
            int upRate = 0;
            int seeders = 0;
            int seedersTotal = 0;
            int leechers = 0;
            int leechersTotal = 0;
            float progress = 0;
            int numFiles = -1;

            // Start pulling
            List<Torrent> torrents = new ArrayList<>();
            int next = xpp.nextTag();
            String name = xpp.getName();

            // Check if we had a proper XML result
            if (name.equals("html")) {
                // Apparently we were returned an HTML page instead of the expected XML
                // This happens in particular when we were logged out (because somebody else logged into KTorrent's web interface)
                throw new LoggedOutException();
            }

            while (next != XmlPullParser.END_DOCUMENT) {

                if (next == XmlPullParser.END_TAG && name.equals("torrent")) {

                    // End of a 'transfer' item, add gathered torrent data
                    torrents.add(new Torrent(
                            id,
                            "" + id,
                            tname,
                            status,
                            (baseDir == null ? null : (numFiles > 0 ? baseDir + tname + pathSeperator : baseDir)),
                            downRate,
                            upRate,
                            seeders,
                            seedersTotal,
                            leechers,
                            leechersTotal,
                            (int) (status == TorrentStatus.Downloading ? (total - down) / downRate : -1), // eta (in seconds) = (total_size_in_btes - bytes_already_downloaded) / bytes_per_second
                            down,
                            up,
                            total,
                            progress,
                            0f,
                            null, // Not supported in the web interface
                            null, // Not supported in the web interface
                            null, // Not supported in the web interface
                            null, // Not supported in the web interface
                            Daemon.KTorrent));
                    id++; // Stop/start/etc. requests are made by ID, which is the order number in the returned XML list :-S

                } else if (next == XmlPullParser.START_TAG && name.equals("torrent")) {

                    // Start of a new 'transfer' item; reset gathered torrent data
                    tname = "";
                    //hash = "";
                    status = TorrentStatus.Unknown;
                    down = 0;
                    up = 0;
                    total = 0;
                    downRate = 0;
                    upRate = 0;
                    seeders = 0;
                    seedersTotal = 0;
                    leechers = 0;
                    leechersTotal = 0;
                    progress = 0;
                    numFiles = -1;

                } else if (next == XmlPullParser.START_TAG) {

                    // Probably encountered a torrent property, i.e. '<status>Stopped</status>'
                    next = xpp.next();
                    if (next == XmlPullParser.TEXT) {
                        switch (name) {
                            case "name":
                                tname = xpp.getText().trim();
                                //} else if (name.equals("info_hash")) {
                                //hash = xpp.getText().trim();
                                break;
                            case "status":
                                status = convertStatus(xpp.getText());
                                break;
                            case "bytes_downloaded":
                                down = convertSize(xpp.getText());
                                break;
                            case "bytes_uploaded":
                                up = convertSize(xpp.getText());
                                break;
                            case "total_bytes_to_download":
                                total = convertSize(xpp.getText());
                                break;
                            case "download_rate":
                                downRate = convertRate(xpp.getText());
                                break;
                            case "upload_rate":
                                upRate = convertRate(xpp.getText());
                                break;
                            case "seeders":
                                seeders = Integer.parseInt(xpp.getText());
                                break;
                            case "seeders_total":
                                seedersTotal = Integer.parseInt(xpp.getText());
                                break;
                            case "leechers":
                                leechers = Integer.parseInt(xpp.getText());
                                break;
                            case "leechers_total":
                                leechersTotal = Integer.parseInt(xpp.getText());
                                break;
                            case "percentage":
                                progress = convertProgress(xpp.getText());
                                break;
                            case "num_files":
                                numFiles = Integer.parseInt(xpp.getText());
                                break;
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
     *
     * @param progress The part done in a string format, i.e. '15.96'
     * @return The part done as [0..1] fraction, i.e. 0.1596
     */
    public static float convertProgress(String progress) {
        return Float.parseFloat(progress) / 100;
    }

    /**
     * Parses a KTorrent-style number string into a float value
     *
     * @param numberString A formatted number string without any letter (like GiB)
     * @return The corresponding float value
     */
    private static Float convertStringToFloat(String numberString) {
        // KTorrent has issues with formatting its numeric values as strings. It does not always
        // adhere to the localization and never indicates which formatting it will use. We therefore
        // have to assume an American style format unless we have indications to assume otherwise.
        int comma = numberString.indexOf(',');
        int dot = numberString.indexOf('.');
        if (comma > 0 && dot > 0) {
            if (comma < dot) {
                // Like 1,234.5
                return Float.parseFloat(numberString.replace(",", ""));
            } else {
                // Like 1.234,5
                return Float.parseFloat(numberString.replace(".", "").replace(",", "."));
            }
        } else {
            if (comma > 0) {
                // Like 234,5
                return Float.parseFloat(numberString.replace(",", "."));
            } else {
                // Like 234.5
                return Float.parseFloat(numberString);
            }
        }
    }

    /**
     * Returns the size of the torrent, as parsed form some string
     *
     * @param size The size in a string format, e.g. '1,011.7 MiB'
     * @return The size in number of kB
     */
    public static long convertSize(String size) {
        String s = size.substring(0, size.length() - 4);
        if (size.endsWith("GiB")) {
            return (long) (convertStringToFloat(s) * 1024 * 1024 * 1024);
        } else if (size.endsWith("MiB")) {
            return (long) (convertStringToFloat(s) * 1024 * 1024);
        } else if (size.endsWith("KiB")) {
            return (long) (convertStringToFloat(s) * 1024);
        } else if (size.endsWith("B")) {
            return convertStringToFloat(size.substring(0, size.length() - 2)).longValue();
        }
        return 0;
    }

    /**
     * Returns the rate (speed), as parsed from some string
     *
     * @param rate The rate in a string format,e.g. '9.2 KiB/s'
     * @return The rate (or speed) in KiB/s
     */
    public static int convertRate(String rate) {
        String s = rate.substring(0, rate.length() - 6);
        if (rate.endsWith("MiB/s")) {
            return (int) (convertStringToFloat(s) * 1024 * 1024);
        } else if (rate.endsWith("KiB/s")) {
            return (int) (convertStringToFloat(s) * 1024);
        } else if (rate.endsWith("B/s")) {
            return convertStringToFloat(rate.substring(0, rate.length() - 4)).intValue();
        }
        return 0;
    }

    /**
     * Returns the status, as parsed from some string
     *
     * @param status THe torrent status in a string format, i.e. 'Leeching'
     * @return The status as TorrentStatus or Unknown if it could not been parsed
     */
    private static TorrentStatus convertStatus(String status) {
        switch (status) {
            case "Downloading":
                return TorrentStatus.Downloading;
            case "Seeding":
                return TorrentStatus.Seeding;
            case "Stopped":
            case "Download completed":
            case "Not started":
                return TorrentStatus.Paused;
            case "Stalled":
                return TorrentStatus.Waiting;
            case "Checking data":
                return TorrentStatus.Checking;
        }
        return TorrentStatus.Unknown;
    }

}
