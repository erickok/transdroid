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
package org.transdroid.daemon.Qbittorrent;

import com.android.internalcopy.http.multipart.FilePart;
import com.android.internalcopy.http.multipart.MultipartEntity;
import com.android.internalcopy.http.multipart.Part;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.transdroid.core.gui.log.Log;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Priority;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.TorrentStatus;
import org.transdroid.daemon.task.AddByFileTask;
import org.transdroid.daemon.task.AddByMagnetUrlTask;
import org.transdroid.daemon.task.AddByUrlTask;
import org.transdroid.daemon.task.DaemonTask;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;
import org.transdroid.daemon.task.GetFileListTask;
import org.transdroid.daemon.task.GetFileListTaskSuccessResult;
import org.transdroid.daemon.task.GetTorrentDetailsTask;
import org.transdroid.daemon.task.GetTorrentDetailsTaskSuccessResult;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetFilePriorityTask;
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.util.HttpHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The daemon adapter for the qBittorrent torrent client.
 * @author erickok
 */
public class QbittorrentAdapter implements IDaemonAdapter {

	private static final String LOG_NAME = "qBittorrent daemon";

	private DaemonSettings settings;
	private DefaultHttpClient httpclient;
	private int version = -1;
	private int apiVersion = -1;

	public QbittorrentAdapter(DaemonSettings settings) {
		this.settings = settings;
	}

	private synchronized void ensureVersion(Log log) throws DaemonException {
		// Still need to retrieve the API and qBittorrent version numbers from the server?
		if (version > 0 && apiVersion > 0)
			return;

		try {

			// The API version is only supported since qBittorrent 3.2, so otherwise we assume version 1
			try {
				String apiVerText = makeRequest(log, "/version/api");
				apiVersion = Integer.parseInt(apiVerText.trim());
			} catch (DaemonException | NumberFormatException e) {
				apiVersion = 1;
			}
			log.d(LOG_NAME, "qBittorrent API version is " + apiVersion);

			// The qBittorent version is only supported since 3.2; for earlier versions we parse the about dialog and parse it
			String versionText = "";
			if (apiVersion > 1) {
				// Format is something like 'v3.2.0'
				versionText = makeRequest(log, "/version/qbittorrent").substring(1);
			} else {
				// Format is something like 'qBittorrent v2.9.7 (Web UI)' or 'qBittorrent v3.0.0-alpha5 (Web UI)'
				String about = makeRequest(log, "/about.html");
				String aboutStartText = "qBittorrent v";
				String aboutEndText = " (Web UI)";
				int aboutStart = about.indexOf(aboutStartText);
				int aboutEnd = about.indexOf(aboutEndText);
				if (aboutStart >= 0 && aboutEnd > aboutStart) {
					versionText = about.substring(aboutStart + aboutStartText.length(), aboutEnd);
				}
			}

			// String found: now parse a version like 2.9.7 as a number like 20907 (allowing 10 places for each .)
			String[] parts = versionText.split("\\.");
			if (parts.length > 0) {
				version = Integer.parseInt(parts[0]) * 100 * 100;
				if (parts.length > 1) {
					version += Integer.parseInt(parts[1]) * 100;
					if (parts.length > 2) {
						// For the last part only read until a non-numeric character is read
						// For example version 3.0.0-alpha5 is read as version code 30000
						String numbers = "";
						for (char c : parts[2].toCharArray()) {
							if (Character.isDigit(c))
								// Still a number; add it to the numbers string
								numbers += Character.toString(c);
							else {
								// No longer reading numbers; stop reading
								break;
							}
						}
						version += Integer.parseInt(numbers);
						return;
					}
				}
			}

		} catch (Exception e) {
			// Unable to establish version number; assume an old version by setting it to version 1
			version = 10000;
			apiVersion = 1;
		}

	}

	private synchronized void ensureAuthenticated(Log log) throws DaemonException {
		// API changed in 3.2.0, login is now handled by its own request, which provides you a cookie.
		// If we don't have that cookie, let's try and get it.

		if (apiVersion < 2) {
			return;
		}

		// Have we already authenticated?  Check if we have the cookie that we need
		List<Cookie> cookies = httpclient.getCookieStore().getCookies();
		for (Cookie c : cookies) {
			if (c.getName().equals("SID")) {
				// And here it is!  Okay, no need authenticate again.
				return;
			}
		}

		makeRequest(log, "/login", new BasicNameValuePair("username", settings.getUsername()),
				new BasicNameValuePair("password", settings.getPassword()));
		// The HttpClient will automatically remember the cookie for us, no need to parse it out.

		// However, we would like to see if authentication was successful or not... 
		cookies = httpclient.getCookieStore().getCookies();
		for (Cookie c : cookies) {
			if (c.getName().equals("SID")) {
				// Good.  Let's get out of here.
				return;
			}
		}

		// No cookie found, we didn't authenticate.
		throw new DaemonException(ExceptionType.AuthenticationFailure, "Server rejected our login");
	}

	@Override
	public DaemonTaskResult executeTask(Log log, DaemonTask task) {

		try {
			ensureVersion(log);
			ensureAuthenticated(log);

			switch (task.getMethod()) {
				case Retrieve:
					String path;
					if (version >= 30200) {
						path = "/query/torrents";
					} else if (version >= 30000) {
						path = "/json/torrents";
					} else {
						path = "/json/events";
					}

					// Request all torrents from server
					JSONArray result = new JSONArray(makeRequest(log, path));
					return new RetrieveTaskSuccessResult((RetrieveTask) task, parseJsonTorrents(result), null);

				case GetTorrentDetails:

					// Request tracker and error details for a specific teacher
					String mhash = task.getTargetTorrent().getUniqueID();
					JSONArray messages =
							new JSONArray(makeRequest(log, (version >= 30200 ? "/query/propertiesTrackers/" : "/json/propertiesTrackers/") + mhash));
					return new GetTorrentDetailsTaskSuccessResult((GetTorrentDetailsTask) task, parseJsonTorrentDetails(messages));

				case GetFileList:

					// Request files listing for a specific torrent
					String fhash = task.getTargetTorrent().getUniqueID();
					JSONArray files =
							new JSONArray(makeRequest(log, (version >= 30200 ? "/query/propertiesFiles/" : "/json/propertiesFiles/") + fhash));
					return new GetFileListTaskSuccessResult((GetFileListTask) task, parseJsonFiles(files));

				case AddByFile:

					// Upload a local .torrent file
					String ufile = ((AddByFileTask) task).getFile();
					makeUploadRequest("/command/upload", ufile, log);
					return new DaemonTaskSuccessResult(task);

				case AddByUrl:

					// Request to add a torrent by URL
					String url = ((AddByUrlTask) task).getUrl();
					makeRequest(log, "/command/download", new BasicNameValuePair("urls", url));
					return new DaemonTaskSuccessResult(task);

				case AddByMagnetUrl:

					// Request to add a magnet link by URL
					String magnet = ((AddByMagnetUrlTask) task).getUrl();
					makeRequest(log, "/command/download", new BasicNameValuePair("urls", magnet));
					return new DaemonTaskSuccessResult(task);

				case Remove:

					// Remove a torrent
					RemoveTask removeTask = (RemoveTask) task;
					makeRequest(log, (removeTask.includingData() ? "/command/deletePerm" : "/command/delete"),
							new BasicNameValuePair("hashes", removeTask.getTargetTorrent().getUniqueID()));
					return new DaemonTaskSuccessResult(task);

				case Pause:

					// Pause a torrent
					makeRequest(log, "/command/pause", new BasicNameValuePair("hash", task.getTargetTorrent().getUniqueID()));
					return new DaemonTaskSuccessResult(task);

				case PauseAll:

					// Resume all torrents
					makeRequest(log, "/command/pauseall");
					return new DaemonTaskSuccessResult(task);

				case Resume:

					// Resume a torrent
					makeRequest(log, "/command/resume", new BasicNameValuePair("hash", task.getTargetTorrent().getUniqueID()));
					return new DaemonTaskSuccessResult(task);

				case ResumeAll:

					// Resume all torrents
					makeRequest(log, "/command/resumeall");
					return new DaemonTaskSuccessResult(task);

				case SetFilePriorities:

					// Update the priorities to a set of files
					SetFilePriorityTask setPrio = (SetFilePriorityTask) task;
					String newPrio = "0";
					if (setPrio.getNewPriority() == Priority.Low) {
						newPrio = "1";
					} else if (setPrio.getNewPriority() == Priority.Normal) {
						newPrio = "2";
					} else if (setPrio.getNewPriority() == Priority.High) {
						newPrio = "7";
					}
					// We have to make a separate request per file, it seems
					for (TorrentFile file : setPrio.getForFiles()) {
						makeRequest(log, "/command/setFilePrio", new BasicNameValuePair("hash", task.getTargetTorrent().getUniqueID()),
								new BasicNameValuePair("id", file.getKey()), new BasicNameValuePair("priority", newPrio));
					}
					return new DaemonTaskSuccessResult(task);

                case ForceRecheck:
                    // Force recheck a torrent
                    makeRequest(log, "/command/recheck", new BasicNameValuePair("hash", task.getTargetTorrent().getUniqueID()));
                    return new DaemonTaskSuccessResult(task);

				case SetTransferRates:

					// TODO: This doesn't seem to work yet
					// Request to set the maximum transfer rates
					SetTransferRatesTask ratesTask = (SetTransferRatesTask) task;
					int dl = (ratesTask.getDownloadRate() == null ? -1 : ratesTask.getDownloadRate());
					int ul = (ratesTask.getUploadRate() == null ? -1 : ratesTask.getUploadRate());

					// First get the preferences
					JSONObject prefs = new JSONObject(makeRequest(log, "/json/preferences"));
					prefs.put("dl_limit", dl);
					prefs.put("up_limit", ul);
					makeRequest(log, "/command/setPreferences", new BasicNameValuePair("json", URLEncoder.encode(prefs.toString(), HTTP.UTF_8)));
					return new DaemonTaskSuccessResult(task);

				default:
					return new DaemonTaskFailureResult(task,
							new DaemonException(ExceptionType.MethodUnsupported, task.getMethod() + " is not supported by " + getType()));
			}
		} catch (JSONException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.ParsingFailed, e.toString()));
		} catch (DaemonException e) {
			return new DaemonTaskFailureResult(task, e);
		} catch (UnsupportedEncodingException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.ParsingFailed, e.toString()));
		}
	}

	private String makeRequest(Log log, String path, NameValuePair... params) throws DaemonException {

		try {

			// Setup request using POST
			HttpPost httppost = new HttpPost(buildWebUIUrl(path));
			List<NameValuePair> nvps = new ArrayList<>();
			Collections.addAll(nvps, params);
			httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			return makeWebRequest(httppost, log);

		} catch (UnsupportedEncodingException e) {
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	private String makeUploadRequest(String path, String file, Log log) throws DaemonException {

		try {

			// Setup request using POST
			HttpPost httppost = new HttpPost(buildWebUIUrl(path));
			File upload = new File(URI.create(file));
			Part[] parts = {new FilePart("torrentfile", upload)};
			httppost.setEntity(new MultipartEntity(parts, httppost.getParams()));
			return makeWebRequest(httppost, log);

		} catch (FileNotFoundException e) {
			throw new DaemonException(ExceptionType.FileAccessError, e.toString());
		}

	}

	private String makeWebRequest(HttpPost httppost, Log log) throws DaemonException {

		try {

			// Initialise the HTTP client
			if (httpclient == null) {
				initialise();
			}

			// Execute
			HttpResponse response = httpclient.execute(httppost);

			HttpEntity entity = response.getEntity();
			if (entity != null) {

				// Read JSON response
				java.io.InputStream instream = entity.getContent();
				String result = HttpHelper.convertStreamToString(instream);
				instream.close();

				// TLog.d(LOG_NAME, "Success: " + (result.length() > 300? result.substring(0, 300) + "... (" +
				// result.length() + " chars)": result));

				// Return raw result
				return result;
			}

			log.d(LOG_NAME, "Error: No entity in HTTP response");
			throw new DaemonException(ExceptionType.UnexpectedResponse, "No HTTP entity object in response.");

		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	/**
	 * Instantiates an HTTP client with proper credentials that can be used for all qBittorrent requests.
	 * @throws DaemonException On conflicting or missing settings
	 */
	private void initialise() throws DaemonException {
		httpclient = HttpHelper.createStandardHttpClient(settings, true);
	}

	/**
	 * Build the URL of the web UI request from the user settings
	 * @return The URL to request
	 */
	private String buildWebUIUrl(String path) {
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() + path;
	}

	private TorrentDetails parseJsonTorrentDetails(JSONArray messages) throws JSONException {

		ArrayList<String> trackers = new ArrayList<>();
		ArrayList<String> errors = new ArrayList<>();

		// Parse response
		if (messages.length() > 0) {
			for (int i = 0; i < messages.length(); i++) {
				JSONObject tor = messages.getJSONObject(i);
				trackers.add(tor.getString("url"));
				String msg = tor.getString("msg");
				if (msg != null && !msg.equals(""))
					errors.add(msg);
			}
		}

		// Return the list
		return new TorrentDetails(trackers, errors);

	}

	private ArrayList<Torrent> parseJsonTorrents(JSONArray response) throws JSONException {

		// Parse response
		ArrayList<Torrent> torrents = new ArrayList<>();
		for (int i = 0; i < response.length(); i++) {
			JSONObject tor = response.getJSONObject(i);
			double progress = tor.getDouble("progress");
			int leechers[];
			int seeders[];
			double ratio;
			long size;
			int dlspeed;
			int upspeed;

			if (apiVersion >= 2) {
				leechers = new int[2];
				leechers[0] = tor.getInt("num_leechs");
				leechers[1] = tor.getInt("num_complete") + tor.getInt("num_incomplete");
				seeders = new int[2];
				seeders[0] = tor.getInt("num_seeds");
				seeders[1] = tor.getInt("num_complete");
				size = tor.getLong("size");
				ratio = tor.getDouble("ratio");
				dlspeed = tor.getInt("dlspeed");
				upspeed = tor.getInt("upspeed");
			} else {
				leechers = parsePeers(tor.getString("num_leechs"));
				seeders = parsePeers(tor.getString("num_seeds"));
				size = parseSize(tor.getString("size"));
				ratio = parseRatio(tor.getString("ratio"));
				dlspeed = parseSpeed(tor.getString("dlspeed"));
				upspeed = parseSpeed(tor.getString("upspeed"));
			}

			long eta = -1L;
			if (dlspeed > 0)
				eta = (long) (size - (size * progress)) / dlspeed;
			// Date added is only available in /json/propertiesGeneral on a per-torrent basis, unfortunately
			// Add the parsed torrent to the list
			// @formatter:off
			torrents.add(new Torrent(
					(long) i,
					tor.getString("hash"),
					tor.getString("name"),
					parseStatus(tor.getString("state")),
					null,
					dlspeed,
					upspeed,
					seeders[0],
					seeders[1],
					leechers[0],
					leechers[1],
					(int) eta,
					(long) (size * progress),
					(long) (size * ratio),
					size,
					(float) progress,
					0f,
					null,
					null,
					null,
					null,
					settings.getType()));
			// @formatter:on
		}

		// Return the list
		return torrents;

	}

	private double parseRatio(String string) {
		// Ratio is given in "1.5" string format
		try {
			return Double.parseDouble(normalizeNumber(string));
		} catch (Exception e) {
			return 0D;
		}
	}

	private long parseSize(String string) {
		// See https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-Documentation
		if (string.equals("Unknown"))
			return -1;
		// Sizes are given in "1,023.3 MiB"-like string format
		String[] parts = string.split(" ");
		double number;
		try {
			number = Double.parseDouble(normalizeNumber(parts[0]));
		} catch (Exception e) {
			return -1L;
		}
		// Returns size in B-based long
		if (parts[1].equals("TiB")) {
			return (long) (number * 1024L * 1024L * 1024L * 1024L);
		} else if (parts[1].equals("GiB")) {
			return (long) (number * 1024L * 1024L * 1024L);
		} else if (parts[1].equals("MiB")) {
			return (long) (number * 1024L * 1024L);
		} else if (parts[1].equals("KiB")) {
			return (long) (number * 1024L);
		}
		return (long) number;
	}

	private int[] parsePeers(String seeds) {
		// Peers (seeders or leechers) are defined in a string like "num_seeds":"6 (27)"
		// In some situations it it just a "6" string
		String[] parts = seeds.split(" ");
		if (parts.length > 1) {
			return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1].substring(1, parts[1].length() - 1))};
		}
		return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[0])};
	}

	private int parseSpeed(String speed) {
		// See https://github.com/qbittorrent/qBittorrent/wiki/WebUI-API-Documentation
		if (speed.equals("Unknown"))
			return -1;
		// Sizes are given in "1,023.3 KiB/s"-like string format
		String[] parts = speed.split(" ");
		double number;
		try {
			number = Double.parseDouble(normalizeNumber(parts[0]));
		} catch (Exception e) {
			return -1;
		}
		// Returns size in B-based int
		if (parts[1].equals("GiB/s")) {
			return (int) (number * 1024 * 1024 * 1024);
		} else if (parts[1].equals("MiB/s")) {
			return (int) (number * 1024 * 1024);
		} else if (parts[1].equals("KiB/s")) {
			return (int) (number * 1024);
		}
		return (int) (Double.parseDouble(normalizeNumber(parts[0])));
	}

	private String normalizeNumber(String in) {
		// FIXME Hack for issue #115: Strip the possible . and , separators in a hopefully reliable fashion, for now
		if (in.length() >= 3) {
			String part1 = in.substring(0, in.length() - 3);
			String part2 = in.substring(in.length() - 3);
			return part1.replace("Ê", "").replace(" ", "").replace(",", "").replace(".", "") + part2.replace(",", ".");
		}
		return in.replace(",", ".");
	}

	private TorrentStatus parseStatus(String state) {
		// Status is given as a descriptive string
		if (state.equals("downloading")) {
			return TorrentStatus.Downloading;
		} else if (state.equals("uploading")) {
			return TorrentStatus.Seeding;
		} else if (state.equals("pausedDL")) {
			return TorrentStatus.Paused;
		} else if (state.equals("pausedUL")) {
			return TorrentStatus.Paused;
		} else if (state.equals("stalledUP")) {
			return TorrentStatus.Seeding;
		} else if (state.equals("stalledDL")) {
			return TorrentStatus.Downloading;
		} else if (state.equals("checkingUP")) {
			return TorrentStatus.Checking;
		} else if (state.equals("checkingDL")) {
			return TorrentStatus.Checking;
		} else if (state.equals("queuedDL")) {
			return TorrentStatus.Queued;
		} else if (state.equals("queuedUL")) {
			return TorrentStatus.Queued;
		}
		return TorrentStatus.Unknown;
	}

	private ArrayList<TorrentFile> parseJsonFiles(JSONArray response) throws JSONException {

		// Parse response
		ArrayList<TorrentFile> torrentfiles = new ArrayList<TorrentFile>();
		for (int i = 0; i < response.length(); i++) {
			JSONObject file = response.getJSONObject(i);

			long size;
			if (apiVersion >= 2) {
				size = file.getLong("size");
			} else {
				size = parseSize(file.getString("size"));
			}

			torrentfiles.add(new TorrentFile("" + i, file.getString("name"), null, null, size, (long) (size * file.getDouble("progress")),
					parsePriority(file.getInt("priority"))));
		}

		// Return the list
		return torrentfiles;

	}

	private Priority parsePriority(int priority) {
		// Priority is an integer
		// Actually 1 = Normal, 2 = High, 7 = Maximum, but adjust this to Transdroid values
		if (priority == 0) {
			return Priority.Off;
		} else if (priority == 1) {
			return Priority.Low;
		} else if (priority == 2) {
			return Priority.Normal;
		}
		return Priority.High;
	}

	@Override
	public Daemon getType() {
		return settings.getType();
	}

	@Override
	public DaemonSettings getSettings() {
		return this.settings;
	}

}
