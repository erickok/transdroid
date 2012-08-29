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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Priority;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.TorrentStatus;
import org.transdroid.daemon.DaemonException.ExceptionType;
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
import org.transdroid.daemon.util.DLog;
import org.transdroid.daemon.util.HttpHelper;
import com.android.internalcopy.http.multipart.FilePart;
import com.android.internalcopy.http.multipart.MultipartEntity;
import com.android.internalcopy.http.multipart.Part;

/**
 * The daemon adapter for the qBittorrent torrent client.
 * 
 * @author erickok
 *
 */
public class QbittorrentAdapter implements IDaemonAdapter {

	private static final String LOG_NAME = "qBittorrent daemon";

	private DaemonSettings settings;
	private DefaultHttpClient httpclient;
	private int version = -1;
	
	public QbittorrentAdapter(DaemonSettings settings) {
		this.settings = settings;
	}
	
	private void ensureVersion() throws DaemonException {
		if (version > 0)
			return;
		// We still need to retrieve the version number from the server
		// Do this by getting the web interface about page and trying to parse the version number
		// Format is something like 'qBittorrent v2.9.7 (Web UI)'
		String about = makeRequest("/about.html");
		String aboutStartText = "qBittorrent v";
		String aboutEndText = " (Web UI)";
		int aboutStart = about.indexOf(aboutStartText);
		int aboutEnd = about.indexOf(aboutEndText);
		if (aboutStart >= 0 && aboutEnd > aboutStart) {
			// String found: now parse a version like 2.9.7 as a number like 20907 (allowing 10 places for each .)
			String[] parts = about.substring(aboutStart + aboutStartText.length(), aboutEnd).split("\\.");
			if (parts.length > 0) {
				version = Integer.parseInt(parts[0]) * 100 * 100;
				if (parts.length > 1) {
					version += Integer.parseInt(parts[1]) * 100;
					if (parts.length > 2) {
						version += Integer.parseInt(parts[2]);
						return;
					}
				}
			}
		}
		// Unable to establish version number; assume an old version by setting it to version 1
		version = 10000;
	}
	
	@Override
	public DaemonTaskResult executeTask(DaemonTask task) {
		
		try {
			ensureVersion();
			switch (task.getMethod()) {
			case Retrieve:

				// Request all torrents from server
				JSONArray result = new JSONArray(makeRequest(version > 30000? "/json/torrents": "/json/events"));
				return new RetrieveTaskSuccessResult((RetrieveTask) task, parseJsonTorrents(result),null);

			case GetTorrentDetails:

				// Request tracker and error details for a specific teacher 
				String mhash = ((GetTorrentDetailsTask)task).getTargetTorrent().getUniqueID();
				JSONArray messages = new JSONArray(makeRequest("/json/propertiesTrackers/" + mhash));
				return new GetTorrentDetailsTaskSuccessResult((GetTorrentDetailsTask) task, parseJsonTorrentDetails(messages));

			case GetFileList:

				// Request files listing for a specific torrent
				String fhash = ((GetFileListTask)task).getTargetTorrent().getUniqueID();
				JSONArray files = new JSONArray(makeRequest("/json/propertiesFiles/" + fhash));
				return new GetFileListTaskSuccessResult((GetFileListTask) task, parseJsonFiles(files));
				
			case AddByFile:

				// Upload a local .torrent file
				String ufile = ((AddByFileTask)task).getFile();
				makeUploadRequest("/command/upload", ufile);
				return new DaemonTaskSuccessResult(task);

			case AddByUrl:

				// Request to add a torrent by URL
				String url = ((AddByUrlTask)task).getUrl();
				makeRequest("/command/download", new BasicNameValuePair("urls", url));
				return new DaemonTaskSuccessResult(task);

			case AddByMagnetUrl:

				// Request to add a magnet link by URL
				String magnet = ((AddByMagnetUrlTask)task).getUrl();
				makeRequest("/command/download", new BasicNameValuePair("urls", magnet));
				return new DaemonTaskSuccessResult(task);

			case Remove:

				// Remove a torrent
				RemoveTask removeTask = (RemoveTask) task;
				makeRequest((removeTask.includingData()? "/command/deletePerm": "/command/delete"), new BasicNameValuePair("hashes", removeTask.getTargetTorrent().getUniqueID()));
				return new DaemonTaskSuccessResult(task);
				
			case Pause:

				// Pause a torrent
				makeRequest("/command/pause", new BasicNameValuePair("hash", task.getTargetTorrent().getUniqueID()));
				return new DaemonTaskSuccessResult(task);
				
			case PauseAll:

				// Resume all torrents
				makeRequest("/command/pauseall");
				return new DaemonTaskSuccessResult(task);

			case Resume:

				// Resume a torrent
				makeRequest("/command/resume", new BasicNameValuePair("hash", task.getTargetTorrent().getUniqueID()));
				return new DaemonTaskSuccessResult(task);
				
			case ResumeAll:

				// Resume all torrents
				makeRequest("/command/resumeall");
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
					makeRequest("/command/setFilePrio", new BasicNameValuePair("hash", task.getTargetTorrent().getUniqueID()), new BasicNameValuePair("id", file.getKey()), new BasicNameValuePair("priority", newPrio));
				}
				return new DaemonTaskSuccessResult(task);
				
			case SetTransferRates:

				// TODO: This doesn't seem to work yet
				// Request to set the maximum transfer rates
				SetTransferRatesTask ratesTask = (SetTransferRatesTask) task;
				int dl = (ratesTask.getDownloadRate() == null? -1: ratesTask.getDownloadRate().intValue());
				int ul = (ratesTask.getUploadRate() == null? -1: ratesTask.getUploadRate().intValue());
				
				// First get the preferences
				JSONObject prefs = new JSONObject(makeRequest("/json/preferences"));
				prefs.put("dl_limit", dl);
				prefs.put("up_limit", ul);
				makeRequest("/command/setPreferences", new BasicNameValuePair("json", URLEncoder.encode(prefs.toString(), HTTP.UTF_8)));
				return new DaemonTaskSuccessResult(task);
				
			default:
				return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.MethodUnsupported, task.getMethod() + " is not supported by " + getType()));
			}
		} catch (JSONException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.ParsingFailed, e.toString()));
		} catch (DaemonException e) {
			return new DaemonTaskFailureResult(task, e);
		} catch (UnsupportedEncodingException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.ParsingFailed, e.toString()));
		}
	}

	private String makeRequest(String path, NameValuePair... params) throws DaemonException {

		try {

			// Setup request using POST
			HttpPost httppost = new HttpPost(buildWebUIUrl(path));
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			for (NameValuePair param : params) {
				nvps.add(param);
			}
			httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			return makeWebRequest(path, httppost);
	
		} catch (UnsupportedEncodingException e) {
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}
		
	}

	private String makeUploadRequest(String path, String file) throws DaemonException {

		try {

			// Setup request using POST
			HttpPost httppost = new HttpPost(buildWebUIUrl(path));
			File upload = new File(URI.create(file));
			Part[] parts = { new FilePart("torrentfile", upload) };
			httppost.setEntity(new MultipartEntity(parts, httppost.getParams()));
			return makeWebRequest(path, httppost);
	
		} catch (FileNotFoundException e) {
			throw new DaemonException(ExceptionType.FileAccessError, e.toString());
		}
		
	}
	
	private String makeWebRequest(String path, HttpPost httppost) throws DaemonException {

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
				String result = HttpHelper.ConvertStreamToString(instream);
				instream.close();
				
				//TLog.d(LOG_NAME, "Success: " + (result.length() > 300? result.substring(0, 300) + "... (" + result.length() + " chars)": result));
				
				// Return raw result
				return result;
			}

			DLog.d(LOG_NAME, "Error: No entity in HTTP response");
			throw new DaemonException(ExceptionType.UnexpectedResponse, "No HTTP entity object in response.");

		} catch (Exception e) {
			DLog.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}
		
	}

	/**
	 * Instantiates an HTTP client with proper credentials that can be used for all qBittorrent requests.
	 * @param connectionTimeout The connection timeout in milliseconds
	 * @throws DaemonException On conflicting or missing settings
	 */
	private void initialise() throws DaemonException {

		httpclient = HttpHelper.createStandardHttpClient(settings, true);
		
		/*httpclient.addRequestInterceptor(new HttpRequestInterceptor() {			
			@Override
			public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
				for (Header header : request.getAllHeaders()) {
					TLog.d(LOG_NAME, "Request: " + header.getName() + ": " + header.getValue());
				}
			}
		});*/
		
	}
	
	/**
	 * Build the URL of the web UI request from the user settings
	 * @return The URL to request
	 */
	private String buildWebUIUrl(String path) {
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() + path;
	}
	
	private TorrentDetails parseJsonTorrentDetails(JSONArray messages) throws JSONException {
		
		ArrayList<String> trackers = new ArrayList<String>();
		ArrayList<String> errors = new ArrayList<String>();

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
		ArrayList<Torrent> torrents = new ArrayList<Torrent>();
		for (int i = 0; i < response.length(); i++) {
			JSONObject tor = response.getJSONObject(i);
			int leechers = parseLeech(tor.getString("num_leechs"));
			int seeders = parseSeeds(tor.getString("num_seeds"));
			int known = parseKnown(tor.getString("num_leechs"), tor.getString("num_seeds"));
			long size = parseSize(tor.getString("size"));
			double ratio = parseRatio(tor.getString("ratio"));
			double progress = tor.getDouble("progress");
			int dlspeed = parseSpeed(tor.getString("dlspeed"));
			// Add the parsed torrent to the list
			torrents.add(new Torrent(
					(long)i,
					tor.getString("hash"),
					tor.getString("name"),
					parseStatus(tor.getString("state")),
					null,
					dlspeed,
					parseSpeed(tor.getString("upspeed")),
					leechers,
					leechers + seeders,
					known,
					known,
					(int) ((size - (size * progress)) / dlspeed),
					(long)(size * progress),
					(long)(size * ratio),
					size,
					(float)progress,
					0f,
					null,
					null,
					null));
		}
		
		// Return the list
		return torrents;
		
	}

	private double parseRatio(String string) {
		// Ratio is given in "1.5" string format
		try {
			return Double.parseDouble(string);
		} catch (Exception e) {
			return 0D;
		}
	}

	private long parseSize(String string) {
		// Sizes are given in "703.3 MiB" string format
		// Returns size in B-based long
		String[] parts = string.split(" ");
		if (parts[1].equals("GiB")) {
			return (long) (Double.parseDouble(parts[0]) * 1024L * 1024L * 1024L);
		} else if (parts[1].equals("MiB")) {
			return (long) (Double.parseDouble(parts[0]) * 1024L * 1024L);
		} else if (parts[1].equals("KiB")) {
			return (long) (Double.parseDouble(parts[0]) * 1024L);
		}
		return (long) (Double.parseDouble(parts[0]));
	}

	private int parseKnown(String leechs, String seeds) {
		// Peers are given in the "num_leechs":"91 (449)","num_seeds":"6 (27)" strings
		// Or sometimes just "num_leechs":"91","num_seeds":"6" strings
		// Peers known are in the last () bit of the leechers and seeders
		int leechers = 0;
		if (leechs.indexOf("(") < 0) {
			leechers = Integer.parseInt(leechs);
		} else {
			leechers = Integer.parseInt(leechs.substring(leechs.indexOf("(") + 1, leechs.indexOf(")")));
		}
		int seeders = 0;
		if (seeds.indexOf("(") < 0) {
			seeders = Integer.parseInt(seeds);
		} else {
			seeders = Integer.parseInt(seeds.substring(seeds.indexOf("(") + 1, seeds.indexOf(")")));
		}
		return leechers + seeders;
	}

	private int parseSeeds(String seeds) {
		// Seeds are in the first part of the "num_seeds":"6 (27)" string
		// In some situations it it just a "6" string
		if (seeds.indexOf(" ") < 0) {
			return Integer.parseInt(seeds);
		}
		return Integer.parseInt(seeds.substring(0, seeds.indexOf(" ")));
	}

	private int parseLeech(String leechs) {
		// Leechers are in the first part of the "num_leechs":"91 (449)" string
		// In some situations it it just a "0" string
		if (leechs.indexOf(" ") < 0) {
			return Integer.parseInt(leechs);
		}
		return Integer.parseInt(leechs.substring(0, leechs.indexOf(" ")));
	}

	private int parseSpeed(String speed) {
		// Speeds are in "21.9 KiB/s" string format
		// Returns speed in B/s-based integer
		String[] parts = speed.split(" ");
		if (parts[1].equals("GiB/s")) {
			return (int) (Double.parseDouble(parts[0]) * 1024 * 1024 * 1024);
		} else if (parts[1].equals("MiB/s")) {
			return (int) (Double.parseDouble(parts[0]) * 1024 * 1024);
		} else if (parts[1].equals("KiB/s")) {
			return (int) (Double.parseDouble(parts[0]) * 1024);
		}
		return (int) (Double.parseDouble(parts[0]));
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
			long size = parseSize(file.getString("size"));
			torrentfiles.add(new TorrentFile(
					"" + i,
					file.getString("name"),
					null,
					null,
					size,
					(long) (size * file.getDouble("progress")),
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
