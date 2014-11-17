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
package org.transdroid.daemon.DLinkRouterBT;

import com.android.internalcopy.http.multipart.FilePart;
import com.android.internalcopy.http.multipart.MultipartEntity;
import com.android.internalcopy.http.multipart.Part;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
import org.transdroid.daemon.Priority;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.TorrentStatus;
import org.transdroid.daemon.task.AddByFileTask;
import org.transdroid.daemon.task.AddByUrlTask;
import org.transdroid.daemon.task.DaemonTask;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;
import org.transdroid.daemon.task.GetFileListTask;
import org.transdroid.daemon.task.GetFileListTaskSuccessResult;
import org.transdroid.daemon.task.PauseTask;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.ResumeTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.util.HttpHelper;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;

/**
 * The daemon adapter for the DLink Router Bittorrent client.
 * @author AvengerMoJo <avengermojo at gmail.com>
 */
public class DLinkRouterBTAdapter implements IDaemonAdapter {

	private static final String LOG_NAME = "DLinkRouterBT adapter";

	private static final String PATH_TO_API = "/api/";
	private static final String SESSION_HEADER = "X-Session-Id";
	private static final String JSON_TORRENTS = "torrents";

	private static final String API_GET = "torrents-get";
	private static final String API_ADD = "torrent-add-url?start=yes&url=";
	private static final String API_ADD_BY_FILE = "torrent-add?start=yes";
	private static final String API_REMOVE = "torrent-remove?delete-torrent=yes&hash=";
	private static final String API_DEL_DATA = "&delete-data=";
	private static final String API_STOP = "torrent-stop?hash=";
	private static final String API_START = "torrent-start?hash=";
	private static final String BT_ADD_BY_FILE = "fileEl";

	private static final String BT_CAPTION = "caption";
	private static final String BT_COPYS = "distributed_copies";
	private static final String BT_DOWNLOAD_RATE = "dl_rate";
	private static final String BT_DONE = "done";
	private static final String BT_HASH = "hash";
	// private static final String BT_MAX_CONNECTED = "max_connections";
	// private static final String BT_MAX_DOWNLOAD_RATE = "max_dl_rate";
	// private static final String BT_MAX_UPLOAD_RATE = "max_ul_rate";
	// private static final String BT_MAX_UPLOAD_CONNECTIONS = "max_uploads";
	// private static final String BT_PAYLOAD_DOWNLOAD = "payload_download";
	private static final String BT_PAYLOAD_UPLOAD = "payload_upload";
	private static final String BT_PEERS_CONNECTED = "peers_connected";
	private static final String BT_PEERS_TOTAL = "peers_total";
	// private static final String BT_PRIVATE = "private";
	private static final String BT_SEEDS_CONNECTED = "seeds_connected";
	private static final String BT_SEEDS_TOTAL = "seeds_total";
	private static final String BT_SIZE = "size";
	private static final String BT_STATE = "state";
	private static final String BT_STOPPED = "stopped";
	private static final String BT_UPLOAD_RATE = "ul_rate";

	private static final String API_GET_FILES = "torrent-get-files?hash=";
	private static final String BT_FILE_DONE = "done";
	private static final String BT_FILE_NAME = "name";
	private static final String BT_FILE_SIZE = "size";
	private static final String BT_FILE_PRIORITY = "pri";

	private DaemonSettings settings;
	private DefaultHttpClient httpclient;
	private String sessionToken;

	public DLinkRouterBTAdapter(DaemonSettings settings) {
		this.settings = settings;
	}

	@Override
	public DaemonTaskResult executeTask(Log log, DaemonTask task) {

		try {
			switch (task.getMethod()) {
				case Retrieve:

					// Request all torrents from server
					JSONObject result = makeRequest(log, API_GET);
					return new RetrieveTaskSuccessResult((RetrieveTask) task, parseJsonRetrieveTorrents(result), null);

				case GetFileList:

					// Request all details for a specific torrent
					JSONObject result2 = makeRequest(log, API_GET_FILES + task.getTargetTorrent().getUniqueID());
					return new GetFileListTaskSuccessResult((GetFileListTask) task,
							parseJsonFileList(result2, task.getTargetTorrent().getUniqueID()));

				case AddByFile:

					// Add a torrent to the server by sending the contents of a local .torrent file
					String file = ((AddByFileTask) task).getFile();

					// put .torrent file's data into the request
					makeRequest(log, API_ADD_BY_FILE, new File(URI.create(file)));
					return new DaemonTaskSuccessResult(task);

				case AddByUrl:

					// Request to add a torrent by URL
					String url = ((AddByUrlTask) task).getUrl();
					makeRequest(log, API_ADD + url);
					return new DaemonTaskSuccessResult(task);

				case Remove:

					// Remove a torrent
					RemoveTask removeTask = (RemoveTask) task;
					makeRequest(log, API_REMOVE + removeTask.getTargetTorrent().getUniqueID() +
							(removeTask.includingData() ? API_DEL_DATA + "yes" : ""), false);
					return new DaemonTaskSuccessResult(task);

				// case Stop:
				case Pause:

					// Pause a torrent
					PauseTask pauseTask = (PauseTask) task;
					makeRequest(log, API_STOP + pauseTask.getTargetTorrent().getUniqueID(), false);
					return new DaemonTaskSuccessResult(task);

				// case PauseAll:

				// Resume all torrents
				// makeRequest(buildRequestObject(RPC_METHOD_PAUSE, buildTorrentRequestObject(FOR_ALL, null,
				// false)));
				// return new DaemonTaskSuccessResult(task);

				// case Start:
				case Resume:

					// Resume a torrent
					ResumeTask resumeTask = (ResumeTask) task;
					makeRequest(log, API_START + resumeTask.getTargetTorrent().getUniqueID(), false);
					return new DaemonTaskSuccessResult(task);

				// case ResumeAll:

				// Resume all torrents
				// makeRequest(buildRequestObject(RPC_METHOD_RESUME, buildTorrentRequestObject(FOR_ALL, null,
				// false)));
				// return new DaemonTaskSuccessResult(task);

				// case SetTransferRates:

				// Request to set the maximum transfer rates
				// SetTransferRatesTask ratesTask = (SetTransferRatesTask) task;
				// if (ratesTask.getUploadRate() == null) {
				// request.put(RPC_SESSION_LIMITUPE, false);
				// } else {
				// request.put(RPC_SESSION_LIMITUPE, true);
				// request.put(RPC_SESSION_LIMITUP, ratesTask.getUploadRate().intValue());
				// }
				// if (ratesTask.getDownloadRate() == null) {
				// request.put(RPC_SESSION_LIMITDOWNE, false);
				// } else {
				// request.put(RPC_SESSION_LIMITDOWNE, true);
				// request.put(RPC_SESSION_LIMITDOWN, ratesTask.getDownloadRate().intValue());
				// }

				// makeRequest( RPC_METHOD_SESSIONSET );
				// return new DaemonTaskSuccessResult(task);

				default:
					return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.MethodUnsupported,
							task.getMethod() + " is not supported by " + getType()));
			}
		} catch (JSONException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.ParsingFailed, e.toString()));
		} catch (DaemonException e) {
			return new DaemonTaskFailureResult(task, e);
		}
	}

	private JSONObject makeRequest(Log log, String requestUrl, File upload) throws DaemonException {
		return makeRequest(log, requestUrl, false, upload);
	}

	private JSONObject makeRequest(Log log, String requestUrl) throws DaemonException {
		return makeRequest(log, requestUrl, true, null);
	}

	private JSONObject makeRequest(Log log, String requestUrl, boolean hasRespond) throws DaemonException {
		return makeRequest(log, requestUrl, hasRespond, null);
	}

	private JSONObject makeRequest(Log log, String requestUrl, boolean hasRespond, File upload) throws DaemonException {

		try {

			// Initialise the HTTP client
			if (httpclient == null) {
				initialise();
			}

			// Setup request using POST stream with URL and data
			HttpPost httppost = new HttpPost(buildWebUIUrl() + requestUrl);
			if (upload != null) {
				Part[] parts = {new FilePart(BT_ADD_BY_FILE, upload)};
				httppost.setEntity(new MultipartEntity(parts, httppost.getParams()));
			}

			// Send the stored session token as a header
			if (sessionToken != null) {
				httppost.addHeader(SESSION_HEADER, sessionToken);
			}

			// Execute
			HttpResponse response = httpclient.execute(httppost);

			// 409 error because of a session id?
			if (response.getStatusLine().getStatusCode() == 409) {

				// Retry post, but this time with the new session token that was encapsulated in the 409
				// response
				sessionToken = response.getFirstHeader(SESSION_HEADER).getValue();
				httppost.addHeader(SESSION_HEADER, sessionToken);
				response = httpclient.execute(httppost);

			}
			if (!hasRespond) {
				return null;
			}

			HttpEntity entity = response.getEntity();
			if (entity != null) {

				// Read JSON response
				java.io.InputStream instream = entity.getContent();
				String result = HttpHelper.convertStreamToString(instream);
				JSONObject json = new JSONObject(result);
				instream.close();

				log.d(LOG_NAME, "Success: " +
						(result.length() > 300 ? result.substring(0, 300) + "... (" + result.length() + " chars)" :
								result));

				// Return the JSON object
				return json;
			}

			log.d(LOG_NAME, "Error: No entity in HTTP response");
			throw new DaemonException(ExceptionType.UnexpectedResponse, "No HTTP entity object in response.");

		} catch (DaemonException e) {
			throw e;
		} catch (JSONException e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.UnexpectedResponse, e.toString());
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	/**
	 * Instantiates an HTTP client with proper credentials that can be used for all Transmission requests.
	 * @throws DaemonException On conflicting or missing settings
	 */
	private void initialise() throws DaemonException {
		httpclient = HttpHelper.createStandardHttpClient(settings, true);
	}

	/**
	 * Build the URL of the Transmission web UI from the user settings.
	 * @return The URL of the RPC API
	 */
	private String buildWebUIUrl() {
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() +
				PATH_TO_API;
	}

	private TorrentStatus convertStatus(String state) {
		if ("allocating".equals(state)) {
			return TorrentStatus.Checking;
		}
		if ("seeding".equals(state)) {
			return TorrentStatus.Seeding;
		}
		if ("finished".equals(state)) {
			return TorrentStatus.Downloading;
		}
		if ("connecting_to_tracker".equals(state)) {
			return TorrentStatus.Checking;
		}
		if ("queued_for_checking".equals(state)) {
			return TorrentStatus.Queued;
		}
		if ("downloading".equals(state)) {
			return TorrentStatus.Downloading;
		}
		return TorrentStatus.Unknown;
	}

	private ArrayList<Torrent> parseJsonRetrieveTorrents(JSONObject response) throws JSONException {

		// Parse response
		ArrayList<Torrent> torrents = new ArrayList<Torrent>();
		JSONArray rarray = response.getJSONArray(JSON_TORRENTS);
		for (int i = 0; i < rarray.length(); i++) {
			JSONObject tor = rarray.getJSONObject(i);
			// Add the parsed torrent to the list
			TorrentStatus status;
			if (tor.getInt(BT_STOPPED) == 1) {
				status = TorrentStatus.Paused;
			} else {
				status = convertStatus(tor.getString(BT_STATE));
			}
			int eta = (int) ((tor.getLong(BT_SIZE) - tor.getLong(BT_DONE)) / (tor.getInt(BT_DOWNLOAD_RATE) + 1));
			if (0 > eta) {
				eta = -1;
			}

			// @formatter:off
			Torrent new_t = new Torrent(
				i, 
				tor.getString(BT_HASH), 
				tor.getString(BT_CAPTION), 
				status,
				null, // Not supported?
				tor.getInt(BT_DOWNLOAD_RATE), 
				tor.getInt(BT_UPLOAD_RATE), 
				tor.getInt(BT_PEERS_CONNECTED), 
				tor.getInt(BT_PEERS_TOTAL), 
				tor.getInt(BT_SEEDS_CONNECTED), 
				tor.getInt(BT_SEEDS_TOTAL),
				eta,
				tor.getLong(BT_DONE), 
				tor.getLong(BT_PAYLOAD_UPLOAD), 
				tor.getLong(BT_SIZE), 
				tor.getLong(BT_DONE) / (float) tor.getLong(BT_SIZE),
				Float.parseFloat(tor.getString(BT_COPYS)), 
				null,
				null,
				null,
				null,
				settings.getType());
			// @formatter:on

			torrents.add(new_t);
		}

		// Return the list
		return torrents;

	}

	private ArrayList<TorrentFile> parseJsonFileList(JSONObject response, String hash) throws JSONException {

		// Parse response
		ArrayList<TorrentFile> torrentfiles = new ArrayList<TorrentFile>();
		JSONObject jobj = response.getJSONObject(JSON_TORRENTS);
		if (jobj != null) {
			JSONArray files = jobj.getJSONArray(hash); // "Hash id"
			for (int i = 0; i < files.length(); i++) {
				JSONObject file = files.getJSONObject(i);
				// @formatter:off
				torrentfiles.add(new TorrentFile(
					String.valueOf(i),
					file.getString(BT_FILE_NAME), 
					file.getString(BT_FILE_NAME), 
					null, // Not supported?
					file.getLong(BT_FILE_SIZE), 
					file.getLong(BT_FILE_DONE), 
					convertTransmissionPriority(file.getInt(BT_FILE_PRIORITY))));
				// @formatter:on
			}
		}

		// Return the list
		return torrentfiles;

	}

	private Priority convertTransmissionPriority(int priority) {
		switch (priority) {
			case 1:
				return Priority.High;
			case -1:
				return Priority.Low;
			default:
				return Priority.Normal;
		}
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
