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
package org.transdroid.daemon.Utorrent;

import com.android.internalcopy.http.multipart.FilePart;
import com.android.internalcopy.http.multipart.MultipartEntity;
import com.android.internalcopy.http.multipart.Part;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.transdroid.core.gui.log.Log;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Label;
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
import org.transdroid.daemon.task.SetLabelTask;
import org.transdroid.daemon.task.SetTrackersTask;
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.task.StartTask;
import org.transdroid.daemon.util.HttpHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An adapter that allows for easy access to uTorrent torrent data. Communication is handled via authenticated JSON-RPC
 * HTTP GET requests and responses.
 * @author erickok
 */
public class UtorrentAdapter implements IDaemonAdapter {

	private static final String LOG_NAME = "uTorrent daemon";
	private static final String RPC_URL_HASH = "&hash=";
	private static final int NAME_IDX = 0;
	private static final int COUNT_IDX = 1;
	// These are the positions inside the JSON response array of a torrent
	// See http://forum.utorrent.com/viewtopic.php?id=25661
	private static final int RPC_HASH_IDX = 0;
	private static final int RPC_STATUS_IDX = 1;
	private static final int RPC_NAME_IDX = 2;
	private static final int RPC_SIZE_IDX = 3;
	private static final int RPC_PARTDONE = 4;
	private static final int RPC_DOWNLOADED_IDX = 5;
	private static final int RPC_UPLOADED_IDX = 6;
	private static final int RPC_DOWNLOADSPEED_IDX = 9;
	private static final int RPC_UPLOADSPEED_IDX = 8;
	private static final int RPC_ETA_IDX = 10;
	private static final int RPC_LABEL_IDX = 11;
	private static final int RPC_PEERSCONNECTED_IDX = 12;
	private static final int RPC_PEERSINSWARM_IDX = 13;
	private static final int RPC_SEEDSCONNECTED_IDX = 14;
	private static final int RPC_SEEDSINSWARM_IDX = 15;
	private static final int RPC_AVAILABILITY_IDX = 16;
	private static final int RPC_ADDEDON_IDX = 23;
	private static final int RPC_COMPLETEDON_IDX = 24;
	// These are the positions inside the JSON response array of a torrent
	// See http://forum.utorrent.com/viewtopic.php?id=25661
	private static final int RPC_FILENAME_IDX = 0;
	private static final int RPC_FILESIZE_IDX = 1;
	private static final int RPC_FILEDOWNLOADED_IDX = 2;
	private static final int RPC_FILEPRIORITY_IDX = 3;
	private static String authtoken;
	private DaemonSettings settings;
	private DefaultHttpClient httpclient;

	/**
	 * Initialises an adapter that provides operations to the uTorrent web daemon
	 */
	public UtorrentAdapter(DaemonSettings settings) {
		this.settings = settings;
	}

	@Override
	public DaemonTaskResult executeTask(Log log, DaemonTask task) {

		try {
			switch (task.getMethod()) {
				case Retrieve:

					// Request all torrents from server
					JSONObject result = makeUtorrentRequest(log, "&list=1");
					return new RetrieveTaskSuccessResult((RetrieveTask) task,
							parseJsonRetrieveTorrents(result.getJSONArray("torrents")),
							parseJsonRetrieveGetLabels(result.getJSONArray("label")));

				case GetTorrentDetails:

					// Request fine details of a specific torrent
					JSONObject dresult = makeUtorrentRequest(log,
							"&action=getprops" + RPC_URL_HASH + task.getTargetTorrent().getUniqueID());
					return new GetTorrentDetailsTaskSuccessResult((GetTorrentDetailsTask) task,
							parseJsonTorrentDetails(dresult.getJSONArray("props")));

				case GetFileList:

					// Get the file listing of a torrent
					JSONObject files = makeUtorrentRequest(log,
							"&action=getfiles" + RPC_URL_HASH + task.getTargetTorrent().getUniqueID());
					return new GetFileListTaskSuccessResult((GetFileListTask) task,
							parseJsonFileListing(files.getJSONArray("files").getJSONArray(1), task.getTargetTorrent()));

				case AddByFile:

					// Add a torrent to the server by sending the contents of a local .torrent file
					String file = ((AddByFileTask) task).getFile();
					uploadTorrentFile(file);
					return new DaemonTaskSuccessResult(task);

				case AddByUrl:

					// Request to add a torrent by URL
					String url = ((AddByUrlTask) task).getUrl();
					if (url == null || url.equals("")) {
						throw new DaemonException(DaemonException.ExceptionType.ParsingFailed, "No url specified");
					}
					makeUtorrentRequest(log, "&action=add-url&s=" + URLEncoder.encode(url, "UTF-8"));
					return new DaemonTaskSuccessResult(task);

				case AddByMagnetUrl:

					// Request to add a magnet link by URL
					String magnet = ((AddByMagnetUrlTask) task).getUrl();
					makeUtorrentRequest(log, "&action=add-url&s=" + URLEncoder.encode(magnet, "UTF-8"));
					return new DaemonTaskSuccessResult(task);

				case Remove:

					// Remove a torrent
					RemoveTask removeTask = (RemoveTask) task;
					if (removeTask.includingData()) {
						makeUtorrentRequest(log,
								"&action=removedata" + RPC_URL_HASH + task.getTargetTorrent().getUniqueID());
					} else {
						makeUtorrentRequest(log,
								"&action=remove" + RPC_URL_HASH + task.getTargetTorrent().getUniqueID());
					}
					return new DaemonTaskSuccessResult(task);

				case Pause:

					// Pause a torrent
					makeUtorrentRequest(log, "&action=pause" + RPC_URL_HASH + task.getTargetTorrent().getUniqueID());
					return new DaemonTaskSuccessResult(task);

				case PauseAll:

					// Pause all torrents
					makeUtorrentRequest(log, "&action=pause" + getAllHashes(log));
					return new DaemonTaskSuccessResult(task);

				case Resume:

					// Resume a torrent
					makeUtorrentRequest(log, "&action=unpause" + RPC_URL_HASH + task.getTargetTorrent().getUniqueID());
					return new DaemonTaskSuccessResult(task);

				case ResumeAll:

					// Resume all torrents
					makeUtorrentRequest(log, "&action=unpause" + getAllHashes(log));
					return new DaemonTaskSuccessResult(task);

				case Stop:

					// Stop a torrent
					makeUtorrentRequest(log, "&action=stop" + RPC_URL_HASH + task.getTargetTorrent().getUniqueID());
					return new DaemonTaskSuccessResult(task);

				case StopAll:

					// Stop all torrents
					makeUtorrentRequest(log, "&action=stop" + getAllHashes(log));
					return new DaemonTaskSuccessResult(task);

				case Start:

					// Start a torrent (maybe forced)
					StartTask startTask = (StartTask) task;
					if (startTask.isForced()) {
						makeUtorrentRequest(log,
								"&action=forcestart" + RPC_URL_HASH + startTask.getTargetTorrent().getUniqueID());
					} else {
						makeUtorrentRequest(log,
								"&action=start" + RPC_URL_HASH + startTask.getTargetTorrent().getUniqueID());
					}
					return new DaemonTaskSuccessResult(task);

				case StartAll:

					// Start all torrents
					makeUtorrentRequest(log, "&action=start" + getAllHashes(log));
					return new DaemonTaskSuccessResult(task);

				case SetFilePriorities:

					// Set priorities of the files of some torrent
					SetFilePriorityTask prioTask = (SetFilePriorityTask) task;
					String prioUrl = "&p=" + convertPriority(prioTask.getNewPriority());
					for (TorrentFile forFile : prioTask.getForFiles()) {
						prioUrl += "&f=" + forFile.getKey();
					}
					makeUtorrentRequest(log,
							"&action=setprio" + RPC_URL_HASH + task.getTargetTorrent().getUniqueID() + prioUrl);
					return new DaemonTaskSuccessResult(task);

				case SetTransferRates:

					// Request to set the maximum transfer rates
					SetTransferRatesTask ratesTask = (SetTransferRatesTask) task;
					makeUtorrentRequest(log, "&action=setsetting&s=ul_auto_throttle&v=0&s=max_ul_rate&v=" +
									(ratesTask.getUploadRate() == null ? 0 : ratesTask.getUploadRate()) +
									"&s=max_dl_rate&v=" +
									(ratesTask.getDownloadRate() == null ? 0 : ratesTask.getDownloadRate()));
					return new DaemonTaskSuccessResult(task);

				case SetLabel:

					// Set the label of some torrent
					SetLabelTask labelTask = (SetLabelTask) task;
					makeUtorrentRequest(log,
							"&action=setprops" + RPC_URL_HASH + labelTask.getTargetTorrent().getUniqueID() +
									"&s=label&v=" + URLEncoder.encode(labelTask.getNewLabel(), "UTF-8"));
					return new DaemonTaskSuccessResult(task);

				case SetTrackers:

					// Set the trackers of some torrent
					SetTrackersTask trackersTask = (SetTrackersTask) task;
					// Build list of tracker lines, separated by a \r\n
					String newTrackersText = "";
					for (String tracker : trackersTask.getNewTrackers()) {
						newTrackersText += (newTrackersText.length() == 0 ? "" : "\r\n") + tracker;
					}
					makeUtorrentRequest(log,
							"&action=setprops" + RPC_URL_HASH + trackersTask.getTargetTorrent().getUniqueID() +
									"&s=trackers&v=" + URLEncoder.encode(newTrackersText, "UTF-8"));
					return new DaemonTaskSuccessResult(task);

				case ForceRecheck:

					// Force re-check of data on a torrent
					makeUtorrentRequest(log, "&action=recheck" + RPC_URL_HASH + task.getTargetTorrent().getUniqueID());
					return new DaemonTaskSuccessResult(task);

				default:
					return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.MethodUnsupported,
							task.getMethod() + " is not supported by " + getType()));
			}
		} catch (JSONException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.ParsingFailed, e.toString()));
		} catch (DaemonException e) {
			return new DaemonTaskFailureResult(task, e);
		} catch (FileNotFoundException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.FileAccessError, e.toString()));
		} catch (UnsupportedEncodingException e) {
			return new DaemonTaskFailureResult(task,
					new DaemonException(ExceptionType.MethodUnsupported, e.toString()));
		} catch (IOException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.ConnectionError, e.toString()));
		}
	}

	private ArrayList<Label> parseJsonRetrieveGetLabels(JSONArray lresults) throws JSONException {

		// Parse response
		ArrayList<Label> labels = new ArrayList<Label>();
		for (int i = 0; i < lresults.length(); i++) {
			JSONArray lab = lresults.getJSONArray(i);
			String name = lab.getString(NAME_IDX);
			int count = lab.getInt(COUNT_IDX);
			labels.add(new Label(name, count));
		}
		return labels;

	}

	private JSONObject makeUtorrentRequest(Log log, String addToUrl) throws DaemonException {
		return makeUtorrentRequest(log, addToUrl, 0);
	}

	private JSONObject makeUtorrentRequest(Log log, String addToUrl, int retried) throws DaemonException {

		try {

			// Initialise the HTTP client
			if (httpclient == null) {
				initialise();
			}

			ensureToken();

			// Make request
			HttpGet httpget = new HttpGet(buildWebUIUrl() + "?token=" + authtoken + addToUrl);
			HttpResponse response = httpclient.execute(httpget);

			// Read JSON response
			InputStream instream = response.getEntity().getContent();
			String result = HttpHelper.convertStreamToString(instream);
			if ((result.equals("") || result.trim().equals("invalid request"))) {
				// Auth token was invalidated; retry at max 3 times
				authtoken = null; // So that ensureToken() will request a new token on the next try
				if (retried < 2) {
					return makeUtorrentRequest(log, addToUrl, ++retried);
				}
				throw new DaemonException(ExceptionType.AuthenticationFailure,
						"Response was '" + result.replace("\n", "") +
								"' instead of a proper JSON object (and we used auth token '" + authtoken + "')");
			}
			JSONObject json = new JSONObject(result);
			instream.close();
			return json;

		} catch (DaemonException e) {
			throw e;
		} catch (JSONException e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ParsingFailed, e.toString());
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	private synchronized void ensureToken() throws IOException, DaemonException {

		// Make sure we have a valid token
		if (authtoken == null) {

			// Make a request to /gui/token.html
			// See https://github.com/bittorrent/webui/wiki/TokenSystem
			HttpGet httpget = new HttpGet(buildWebUIUrl() + "token.html");

			// Parse the response HTML
			HttpResponse response = httpclient.execute(httpget);
			if (response.getStatusLine().getStatusCode() == 401) {
				throw new DaemonException(ExceptionType.AuthenticationFailure,
						"Auth denied (401) on token.html retrieval");
			}
			if (response.getStatusLine().getStatusCode() == 404) {
				throw new DaemonException(ExceptionType.ConnectionError,
						"Not found (404); server doesn't exist or is inaccessible");
			}
			InputStream instream = response.getEntity().getContent();
			String result = HttpHelper.convertStreamToString(instream);
			authtoken = result.replaceAll("<.*?>", "").trim();

		}

	}

	public JSONObject uploadTorrentFile(String file) throws DaemonException, IOException, JSONException {

		// Initialise the HTTP client
		if (httpclient == null) {
			initialise();
		}

		ensureToken();

		// Build and make request
		HttpPost httppost = new HttpPost(buildWebUIUrl() + "?token=" + authtoken + "&action=add-file");
		File upload = new File(URI.create(file));
		Part[] parts = {new FilePart("torrent_file", upload, FilePart.DEFAULT_CONTENT_TYPE, null)};
		httppost.setEntity(new MultipartEntity(parts, httppost.getParams()));
		HttpResponse response = httpclient.execute(httppost);

		// Read JSON response
		InputStream instream = response.getEntity().getContent();
		String result = HttpHelper.convertStreamToString(instream);
		JSONObject json = new JSONObject(result);
		instream.close();
		return json;

	}

	/**
	 * Instantiates an HTTP client with proper credentials that can be used for all Transmission requests.
	 * @throws DaemonException On conflicting or missing settings
	 */
	private void initialise() throws DaemonException {
		this.httpclient = HttpHelper.createStandardHttpClient(settings, true);
	}

	/**
	 * Build the URL of the Transmission web UI from the user settings.
	 * @return The URL of the RPC API
	 */
	private String buildWebUIUrl() {
		String folder = settings.getFolder() == null ? "" : settings.getFolder().trim();
		if (!folder.startsWith("/")) {
			// Add leading slash
			folder = "/" + folder;
		}
		if (folder.endsWith("/")) {
			// Strip trailing slash
			folder = folder.substring(0, folder.length() - 1);
		}
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress().trim() + ":" + settings.getPort() + folder + "/gui/";
	}

	private TorrentStatus convertUtorrentStatus(int uStatus, boolean finished) {
		// Convert bitwise int to uTorrent status codes
		// Now based on http://forum.utorrent.com/viewtopic.php?id=50779
		if ((uStatus & 1) == 1) {
			// Started
			if ((uStatus & 32) == 32) {
				// Paused
				return TorrentStatus.Paused;
			} else if (finished) {
				return TorrentStatus.Seeding;
			} else {
				return TorrentStatus.Downloading;
			}
		} else if ((uStatus & 2) == 2) {
			// Checking
			return TorrentStatus.Checking;
		} else if ((uStatus & 16) == 16) {
			// Error
			return TorrentStatus.Error;
		} else if ((uStatus & 128) == 128) {
			// Queued
			return TorrentStatus.Queued;
		} else {
			return TorrentStatus.Waiting;
		}
	}

	private Priority convertUtorrentPriority(int code) {
		switch (code) {
			case 0:
				return Priority.Off;
			case 1:
				return Priority.Low;
			case 3:
				return Priority.High;
			default:
				return Priority.Normal;
		}
	}

	private int convertPriority(Priority newPriority) {
		if (newPriority == null) {
			return 2;
		}
		switch (newPriority) {
			case Off:
				return 0;
			case Low:
				return 1;
			case High:
				return 3;
			default:
				return 2;
		}
	}

	private ArrayList<Torrent> parseJsonRetrieveTorrents(JSONArray results) throws JSONException {

		// Parse response
		ArrayList<Torrent> torrents = new ArrayList<Torrent>();
		boolean createPaths = !(settings.getDownloadDir() == null || settings.getDownloadDir().equals(""));
		for (int i = 0; i < results.length(); i++) {
			JSONArray tor = results.getJSONArray(i);
			String name = tor.getString(RPC_NAME_IDX);
			boolean downloaded = (tor.getLong(RPC_PARTDONE) == 1000l);
			float available = ((float) tor.getInt(RPC_AVAILABILITY_IDX)) / 65536f; // Integer in 1/65536ths
			// The full torrent path is not available in uTorrent web UI API
			// Guess the torrent's directory based on the user-specific default download dir and the torrent name
			String dir = null;
			if (createPaths) {
				dir = settings.getDownloadDir();
				if (name.length() < 4 || name.charAt(name.length() - 4) != '.') {
					// Assume this is a directory rather than a single-file torrent
					dir += name + settings.getOS().getPathSeperator();
				}
			}
			// Add the parsed torrent to the list
			TorrentStatus status = convertUtorrentStatus(tor.getInt(RPC_STATUS_IDX), downloaded);
			long addedOn = tor.optInt(RPC_ADDEDON_IDX, -1);
			long completedOn = tor.optInt(RPC_COMPLETEDON_IDX, -1);
			Date addedOnDate = addedOn == -1 ? null : new Date(addedOn * 1000L);
			Date completedOnDate = completedOn == -1 ? null : new Date(completedOn * 1000L);
			torrents.add(new Torrent(i, // No ID but a hash is used
					tor.getString(RPC_HASH_IDX), name, status, dir, tor.getInt(RPC_DOWNLOADSPEED_IDX),
					tor.getInt(RPC_UPLOADSPEED_IDX), tor.getInt(RPC_SEEDSCONNECTED_IDX),
					tor.getInt(RPC_SEEDSINSWARM_IDX), tor.getInt(RPC_PEERSCONNECTED_IDX),
					tor.getInt(RPC_PEERSINSWARM_IDX), tor.getInt(RPC_ETA_IDX), tor.getLong(RPC_DOWNLOADED_IDX),
					tor.getLong(RPC_UPLOADED_IDX), tor.getLong(RPC_SIZE_IDX),
					((float) tor.getLong(RPC_PARTDONE)) / 1000f, // Integer in promille
					Math.min(available, 1f), // Can be > 100% if multiple peers have 100%
					tor.getString(RPC_LABEL_IDX).trim(), addedOnDate, completedOnDate,
					// uTorrent doesn't give the error message, so just remind that there is some error
					status == TorrentStatus.Error ? "See GUI for error message" : null, settings.getType()));
		}
		return torrents;

	}

	private TorrentDetails parseJsonTorrentDetails(JSONArray results) throws JSONException {

		// Parse response
		// NOTE: Assumes only details for one torrent are requested at a time
		if (results.length() > 0) {

			JSONObject tor = results.getJSONObject(0);
			List<String> trackers = new ArrayList<String>();
			for (String tracker : tor.getString("trackers").split("\\r\\n")) {
				// Ignore any blank lines
				if (!tracker.trim().equals("")) {
					trackers.add(tracker.trim());
				}
			}
			// uTorrent doesn't support tracker error messages in the web UI
			// See http://forum.utorrent.com/viewtopic.php?pid=553340#p553340
			return new TorrentDetails(trackers, null);
		}

		return null;

	}

	private ArrayList<TorrentFile> parseJsonFileListing(JSONArray results, Torrent torrent) throws JSONException {

		// Parse response
		ArrayList<TorrentFile> files = new ArrayList<TorrentFile>();
		boolean createPaths =
				torrent != null && torrent.getLocationDir() != null && !torrent.getLocationDir().equals("");
		final String pathSep = settings.getOS().getPathSeperator();
		for (int i = 0; i < results.length(); i++) {
			JSONArray file = results.getJSONArray(i);
			// Add the parsed torrent to the list
			files.add(new TorrentFile("" + i, file.getString(RPC_FILENAME_IDX),        // Name
					(createPaths ?
							file.getString(RPC_FILENAME_IDX).replace((pathSep.equals("/") ? "\\" : "/"), pathSep) :
							null),    // Relative path; 'wrong' path slashes will be replaced
					(createPaths ? torrent.getLocationDir() +
							file.getString(RPC_FILENAME_IDX).replace((pathSep.equals("/") ? "\\" : "/"), pathSep) :
							null),    // Full path; 'wrong' path slashes will be replaced
					file.getLong(RPC_FILESIZE_IDX),            // Total size
					file.getLong(RPC_FILEDOWNLOADED_IDX),    // Part done
					convertUtorrentPriority(file.getInt(RPC_FILEPRIORITY_IDX))));    // Priority
		}
		return files;

	}

	private String getAllHashes(Log log) throws DaemonException, JSONException {

		// Make a retrieve torrents call first to gather all hashes
		JSONObject result = makeUtorrentRequest(log, "&list=1");
		ArrayList<Torrent> torrents = parseJsonRetrieveTorrents(result.getJSONArray("torrents"));

		// Build a string of hashes of all the torrents
		String hashes = "";
		for (Torrent torrent : torrents) {
			hashes += RPC_URL_HASH + torrent.getUniqueID();
		}
		return hashes;

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
