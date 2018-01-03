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
package org.transdroid.daemon.Deluge;

import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_DETAILS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_DOWNLOADEDEVER;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_ETA;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_FIELDS_ARRAY;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_FILE;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_FILEPRIORITIES;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_FILEPROGRESS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_FILE_FIELDS_ARRAY;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_INDEX;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_LABEL;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_MAXDOWNLOAD;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_MAXUPLOAD;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_MESSAGE;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_ADD;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_ADD_FILE;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_ADD_MAGNET;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_AUTH_LOGIN;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_FORCERECHECK;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_GET;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_MOVESTORAGE;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_PAUSE;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_PAUSE_ALL;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_REMOVE;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_RESUME;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_RESUME_ALL;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_SETCONFIG;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_SETFILE;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_SETLABEL;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_SETTRACKERS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_METHOD_STATUS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_NAME;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_NUMPEERS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_NUMSEEDS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_PARAMS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_PARTDONE;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_PATH;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_RATEDOWNLOAD;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_RATEUPLOAD;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_RESULT;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_SAVEPATH;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_SESSION_ID;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_SIZE;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_STATUS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_DETAILS_FIELDS_ARRAY;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_TIMEADDED;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_TORRENTS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_TOTALPEERS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_TOTALSEEDS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_TOTALSIZE;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_TRACKERS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_TRACKER_STATUS;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_TRACKER_TIER;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_TRACKER_URL;
import static org.transdroid.daemon.Deluge.DelugeCommon.RPC_UPLOADEDEVER;

import com.android.internalcopy.http.multipart.FilePart;
import com.android.internalcopy.http.multipart.MultipartEntity;
import com.android.internalcopy.http.multipart.Part;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
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
import org.transdroid.daemon.task.PauseTask;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.ResumeTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetDownloadLocationTask;
import org.transdroid.daemon.task.SetFilePriorityTask;
import org.transdroid.daemon.task.SetLabelTask;
import org.transdroid.daemon.task.SetTrackersTask;
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.util.HttpHelper;


/**
 * The daemon adapter from the Deluge torrent client.
 * @author erickok
 */
public class DelugeAdapter implements IDaemonAdapter {

	private static final String LOG_NAME = "Deluge daemon";

	private static final String PATH_TO_RPC = "/json";
	private static final String PATH_TO_UPLOAD = "/upload";

	private static final String RPC_ID = "id";

	private DaemonSettings settings;
	private DefaultHttpClient httpclient;
	private Cookie sessionCookie;
	private int version = -1;

	public DelugeAdapter(DaemonSettings settings) {
		this.settings = settings;
	}

	public JSONArray addTorrentByFile(String file, Log log) throws JSONException, IOException, DaemonException {

		String url = buildWebUIUrl() + PATH_TO_UPLOAD;

		log.d(LOG_NAME, "Uploading a file to the Deluge daemon: " + url);

		// Initialise the HTTP client
		if (httpclient == null) {
			initialise();
		}

		// Setup client using POST
		HttpPost httppost = new HttpPost(url);
		File upload = new File(URI.create(file));
		Part[] parts = {new FilePart(RPC_FILE, upload)};
		httppost.setEntity(new MultipartEntity(parts, httppost.getParams()));

		// Make request
		HttpResponse response = httpclient.execute(httppost);

		// Read JSON response
		InputStream instream = response.getEntity().getContent();
		String result = HttpHelper.convertStreamToString(instream);

		// If the upload succeeded, add the torrent file on the server
		// For this we need the file name, which is now send as a JSON object like:
		// {"files": ["/tmp/delugeweb/tmp00000.torrent"], "success": true}
		String remoteFile = (new JSONObject(result)).getJSONArray(RPC_DETAILS).getString(0);
		JSONArray params = new JSONArray();
		JSONArray files = new JSONArray();
		JSONObject fileu = new JSONObject();
		fileu.put("path", remoteFile);
		fileu.put("options", new JSONArray());
		files.put(fileu);
		params.put(files);

		return params;

	}

	@Override
	public DaemonTaskResult executeTask(Log log, DaemonTask task) {

		try {
			ensureVersion(log);

			JSONArray params = new JSONArray();

			// Array of the fields needed for files listing calls
			JSONArray ffields = new JSONArray();
			for (String field : RPC_FILE_FIELDS_ARRAY) {
				ffields.put(field);
			}

			switch (task.getMethod()) {
				case Retrieve:

					// Request all torrents from server
					JSONArray fields = new JSONArray();
					for (String field : RPC_FIELDS_ARRAY) {
						fields.put(field);
					}
					params.put(fields); // keys
					params.put(new JSONArray()); // filter_dict
					// params.put(-1); // cache_id

					JSONObject result = makeRequest(buildRequest(RPC_METHOD_GET, params), log);
					return new RetrieveTaskSuccessResult((RetrieveTask) task,
							parseJsonRetrieveTorrents(result.getJSONObject(RPC_RESULT)),
							parseJsonRetrieveLabels(result.getJSONObject(RPC_RESULT)));

				case GetTorrentDetails:

					// Array of the fields needed for files listing calls
					JSONArray dfields = new JSONArray();
					for (String field : RPC_DETAILS_FIELDS_ARRAY) {
						dfields.put(field);
					}

					// Request file listing of a torrent
					params.put(task.getTargetTorrent().getUniqueID()); // torrent_id
					params.put(dfields); // keys

					JSONObject dinfo = makeRequest(buildRequest(RPC_METHOD_STATUS, params), log);
					return new GetTorrentDetailsTaskSuccessResult((GetTorrentDetailsTask) task,
							parseJsonTorrentDetails(dinfo.getJSONObject(RPC_RESULT)));

				case GetFileList:

					// Request file listing of a torrent
					params.put(task.getTargetTorrent().getUniqueID()); // torrent_id
					params.put(ffields); // keys

					JSONObject finfo = makeRequest(buildRequest(RPC_METHOD_STATUS, params), log);
					return new GetFileListTaskSuccessResult((GetFileListTask) task,
							parseJsonFileListing(finfo.getJSONObject(RPC_RESULT), task.getTargetTorrent()));

				case AddByFile:

					// Request to add a torrent by local .torrent file
					String file = ((AddByFileTask) task).getFile();
					makeRequest(buildRequest(RPC_METHOD_ADD_FILE, addTorrentByFile(file, log)), log);
					return new DaemonTaskSuccessResult(task);

				case AddByUrl:

					// Request to add a torrent by URL
					String url = ((AddByUrlTask) task).getUrl();
					params.put(url);
					params.put(new JSONArray());

					makeRequest(buildRequest(RPC_METHOD_ADD, params), log);
					return new DaemonTaskSuccessResult(task);

				case AddByMagnetUrl:

					// Request to add a magnet link by URL
					String magnet = ((AddByMagnetUrlTask) task).getUrl();
					params.put(magnet);
					params.put(new JSONArray());

					makeRequest(buildRequest(RPC_METHOD_ADD_MAGNET, params), log);
					return new DaemonTaskSuccessResult(task);

				case Remove:

					// Remove a torrent
					RemoveTask removeTask = (RemoveTask) task;
					params.put(removeTask.getTargetTorrent().getUniqueID());
					params.put(removeTask.includingData());
					makeRequest(buildRequest(RPC_METHOD_REMOVE, params), log);
					return new DaemonTaskSuccessResult(task);

				case Pause:

					// Pause a torrent
					PauseTask pauseTask = (PauseTask) task;
					makeRequest(buildRequest(RPC_METHOD_PAUSE, ((new JSONArray())
									.put((new JSONArray()).put(pauseTask.getTargetTorrent().getUniqueID())))), log);
					return new DaemonTaskSuccessResult(task);

				case PauseAll:

					// Resume all torrents
					makeRequest(buildRequest(RPC_METHOD_PAUSE_ALL, null), log);
					return new DaemonTaskSuccessResult(task);

				case Resume:

					// Resume a torrent
					ResumeTask resumeTask = (ResumeTask) task;
					makeRequest(buildRequest(RPC_METHOD_RESUME, ((new JSONArray())
									.put((new JSONArray()).put(resumeTask.getTargetTorrent().getUniqueID())))), log);
					return new DaemonTaskSuccessResult(task);

				case ResumeAll:

					// Resume all torrents
					makeRequest(buildRequest(RPC_METHOD_RESUME_ALL, null), log);
					return new DaemonTaskSuccessResult(task);

				case SetFilePriorities:

					// Set the priorities of files in a specific torrent
					SetFilePriorityTask prioTask = (SetFilePriorityTask) task;

					// We first need a listing of all the files (because we can only set the priorities all at once)
					params.put(task.getTargetTorrent().getUniqueID()); // torrent_id
					params.put(ffields); // keys
					JSONObject pinfo = makeRequest(buildRequest(RPC_METHOD_STATUS, params), log);
					ArrayList<TorrentFile> pfiles =
							parseJsonFileListing(pinfo.getJSONObject(RPC_RESULT), prioTask.getTargetTorrent());

					// Now prepare the new list of priorities
					params = new JSONArray();
					params.put(task.getTargetTorrent().getUniqueID()); // torrent_id
					JSONArray pfields = new JSONArray();
					// Override the priorities in the just retrieved list of all files
					for (TorrentFile pfile : pfiles) {
						Priority newPriority = pfile.getPriority();
						for (TorrentFile forFile : prioTask.getForFiles()) {
							if (forFile.getKey().equals(pfile.getKey())) {
								// This is a file that we want to assign a new priority to
								newPriority = prioTask.getNewPriority();
								break;
							}
						}
						pfields.put(DelugeCommon.convertPriority(newPriority, version));
					}
					params.put(pfields); // keys

					// Make a single call to set the priorities on all files at once
					makeRequest(buildRequest(RPC_METHOD_SETFILE, params), log);
					return new DaemonTaskSuccessResult(task);

				case SetDownloadLocation:

					// Set the download location of some torrent
					SetDownloadLocationTask sdlTask = (SetDownloadLocationTask) task;
					// This works, but does not move the torrent
					//makeRequest(buildRequest(RPC_METHOD_SETOPTIONS, buildSetTorrentOptions(
					//		sdlTask.getTargetTorrent().getUniqueID(), RPC_DOWNLOADLOCATION, sdlTask.getNewLocation())));
					params.put(new JSONArray().put(task.getTargetTorrent().getUniqueID()));
					params.put(sdlTask.getNewLocation());
					makeRequest(buildRequest(RPC_METHOD_MOVESTORAGE, params), log);
					return new DaemonTaskSuccessResult(task);

				case SetTransferRates:

					// Request to set the maximum transfer rates
					SetTransferRatesTask ratesTask = (SetTransferRatesTask) task;
					JSONObject map = new JSONObject();
					map.put(RPC_MAXUPLOAD, (ratesTask.getUploadRate() == null ? -1 : ratesTask.getUploadRate()));
					map.put(RPC_MAXDOWNLOAD, (ratesTask.getDownloadRate() == null ? -1 : ratesTask.getDownloadRate()));

					makeRequest(buildRequest(RPC_METHOD_SETCONFIG, (new JSONArray()).put(map)), log);
					return new DaemonTaskSuccessResult(task);

				case SetLabel:

					// Request to set the label
					SetLabelTask labelTask = (SetLabelTask) task;
					params.put(task.getTargetTorrent().getUniqueID());
					params.put(labelTask.getNewLabel() == null ? "" : labelTask.getNewLabel());
					makeRequest(buildRequest(RPC_METHOD_SETLABEL, params), log);
					return new DaemonTaskSuccessResult(task);

				case SetTrackers:

					// Set the trackers of some torrent
					SetTrackersTask trackersTask = (SetTrackersTask) task;
					JSONArray trackers = new JSONArray();
					// Build an JSON arrays of objcts that each have a tier (order) number and an url
					for (int i = 0; i < trackersTask.getNewTrackers().size(); i++) {
						JSONObject trackerObj = new JSONObject();
						trackerObj.put(RPC_TRACKER_TIER, i);
						trackerObj.put(RPC_TRACKER_URL, trackersTask.getNewTrackers().get(i));
						trackers.put(trackerObj);
					}
					params.put(new JSONArray().put(task.getTargetTorrent().getUniqueID()));
					params.put(trackers);
					makeRequest(buildRequest(RPC_METHOD_SETTRACKERS, params), log);
					return new DaemonTaskSuccessResult(task);

				case ForceRecheck:

					// Pause a torrent
					makeRequest(buildRequest(RPC_METHOD_FORCERECHECK,
							((new JSONArray()).put((new JSONArray()).put(task.getTargetTorrent().getUniqueID())))),
							log);
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
		} catch (IOException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.FileAccessError, e.toString()));
		}
	}
	
	/*private JSONArray buildSetTorrentOptions(String torrent, String key, String value) throws JSONException {
		JSONArray params = new JSONArray();
		params.put(new JSONArray().put(torrent)); // torrent_id
		JSONObject sdlmap = new JSONObject();
		// Set the option setting to the torrent
		sdlmap.put(key, value);
		params.put(sdlmap); // options
		return params;
	}*/

	private void ensureVersion(Log log) throws DaemonException {
		if (version > 0) {
			return;
		}
		// We still need to retrieve the version number from the server
		// Do this by getting the web interface main html page and trying to parse the version number
		// Format is something like '<title>Deluge: Web UI 1.3.6</title>'
		if (httpclient == null) {
			initialise();
		}
		try {
			HttpResponse response = httpclient.execute(new HttpGet(buildWebUIUrl() + "/"));
			String main = HttpHelper.convertStreamToString(response.getEntity().getContent());
			String titleStartText = "<title>Deluge: Web UI ";
			String titleEndText = "</title>";
			int titleStart = main.indexOf(titleStartText);
			int titleEnd = main.indexOf(titleEndText, titleStart);
			if (titleStart >= 0 && titleEnd > titleStart) {
				// String found: now parse a version like 2.9.7 as a number like 20907 (allowing 10 places for each .)
				version = DelugeCommon.getVersionString(main.substring(titleStart + titleStartText.length(), titleEnd));
			}
		} catch (NumberFormatException e) {
			log.d(LOG_NAME, "Error parsing the Deluge version code as number: " + e.toString());
			// Continue though, ignoring the version number
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}
		// Unable to establish version number; assume an old version by setting it to version 1
		version = 10000;
	}

	private JSONObject buildRequest(String sendMethod, JSONArray params) throws JSONException {

		// Build request for method
		JSONObject request = new JSONObject();
		request.put(RPC_METHOD, sendMethod);
		request.put(RPC_PARAMS, (params == null) ? new JSONArray() : params);
		request.put(RPC_ID, 2);
		return request;

	}

	private synchronized JSONObject makeRequest(JSONObject data, Log log) throws DaemonException {

		try {

			// Initialise the HTTP client
			if (httpclient == null) {
				initialise();
			}

			// Login first?
			if (sessionCookie == null) {

				// Build login object
				String extraPass = settings.getExtraPassword();
				if (extraPass == null) {
					extraPass = "";
				}
				JSONObject loginRequest = new JSONObject();
				loginRequest.put(RPC_METHOD, RPC_METHOD_AUTH_LOGIN);
				loginRequest.put(RPC_PARAMS, (new JSONArray()).put(extraPass));
				loginRequest.put(RPC_ID, 1);

				// Set POST URL and data
				HttpPost httppost = new HttpPost(buildWebUIUrl() + PATH_TO_RPC);
				httppost.setHeader("content-type", "application/json");
				StringEntity se = new StringEntity(loginRequest.toString());
				httppost.setEntity(se);

				// Execute
				HttpResponse response = httpclient.execute(httppost);
				InputStream instream = response.getEntity().getContent();

				// Retrieve session ID
				if (!httpclient.getCookieStore().getCookies().isEmpty()) {
					for (Cookie cookie : httpclient.getCookieStore().getCookies()) {
						if (cookie.getName().equals(RPC_SESSION_ID)) {
							sessionCookie = cookie;
							break;
						}
					}
				}

				// Still no session cookie?
				if (sessionCookie == null) {
					// Set error message and cancel the action that was requested
					throw new DaemonException(ExceptionType.AuthenticationFailure,
							"Password error? Server time difference? No (valid) cookie in response and JSON was: " +
									HttpHelper.convertStreamToString(instream));
				}

			}

			// Regular action

			// Set POST URL and data
			HttpPost httppost = new HttpPost(buildWebUIUrl() + PATH_TO_RPC);
			httppost.setHeader("content-type", "application/json");
			StringEntity se = new StringEntity(data.toString(), HTTP.UTF_8);
			httppost.setEntity(se);

			// Set session cookie, if it was not in the httpclient object yet
			boolean cookiePresent = false;
			for (Cookie cookie : httpclient.getCookieStore().getCookies()) {
				if (cookie.getName().equals(RPC_SESSION_ID)) {
					cookiePresent = true;
					break;
				}
			}
			if (!cookiePresent) {
				httpclient.getCookieStore().addCookie(sessionCookie);
			}

			// Execute
			HttpResponse response = httpclient.execute(httppost);

			HttpEntity entity = response.getEntity();
			if (entity != null) {

				// Read JSON response
				InputStream instream = entity.getContent();
				String result = HttpHelper.convertStreamToString(instream);
				JSONObject json = new JSONObject(result);
				instream.close();

				log.d(LOG_NAME, "Success: " +
						(result.length() > 300 ? result.substring(0, 300) + "... (" + result.length() + " chars)" :
								result));

				// Return JSON object
				return json;

			}

			// No result?
			throw new DaemonException(ExceptionType.UnexpectedResponse, "No HTTP entity in response object.");

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
	 * @throws DaemonException On missing settings
	 */
	private void initialise() throws DaemonException {

		httpclient = HttpHelper.createStandardHttpClient(settings,
				settings.getUsername() != null && !settings.getUsername().equals(""));
		httpclient.addRequestInterceptor(HttpHelper.gzipRequestInterceptor);
		httpclient.addResponseInterceptor(HttpHelper.gzipResponseInterceptor);

	}

	/**
	 * Build the URL of the Transmission web UI from the user settings.
	 * @return The URL of the RPC API
	 */
	private String buildWebUIUrl() {
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() +
				(settings.getFolder() == null ? "" : settings.getFolder());
	}

	private ArrayList<Torrent> parseJsonRetrieveTorrents(JSONObject response) throws JSONException, DaemonException {

		// Parse response
		ArrayList<Torrent> torrents = new ArrayList<Torrent>();
		if (response.isNull(RPC_TORRENTS)) {
			throw new DaemonException(ExceptionType.NotConnected,
					"Web interface probably not connected to a daemon yet, because 'torrents' is null: " +
							response.toString());
		}
		JSONObject objects = response.getJSONObject(RPC_TORRENTS);
		JSONArray names = objects.names();
		if (names != null) {
			for (int j = 0; j < names.length(); j++) {

				JSONObject tor = objects.getJSONObject(names.getString(j));
				// Add the parsed torrent to the list
				TorrentStatus status = DelugeCommon.convertDelugeState(tor.getString(RPC_STATUS));
				String error = tor.getString(RPC_MESSAGE);
				if (tor.getString(RPC_TRACKER_STATUS).indexOf("Error") > 0) {
					error += (error.length() > 0 ? "\n" : "") + tor.getString(RPC_TRACKER_STATUS);
					//status = TorrentStatus.Error; // Don't report this as blocking error
				}
				// @formatter:off
				torrents.add(new Torrent(j,
						names.getString(j),
						tor.getString(RPC_NAME),
						status,
						tor.getString(RPC_SAVEPATH) + settings.getOS().getPathSeperator(),
						tor.getInt(RPC_RATEDOWNLOAD),
						tor.getInt(RPC_RATEUPLOAD),
						tor.getInt(RPC_NUMSEEDS),
						tor.getInt(RPC_TOTALSEEDS),
						tor.getInt(RPC_NUMPEERS),
						tor.getInt(RPC_TOTALPEERS),
						tor.getInt(RPC_ETA),
						tor.getLong(RPC_DOWNLOADEDEVER),
						tor.getLong(RPC_UPLOADEDEVER),
						tor.getLong(RPC_TOTALSIZE),
						((float) tor.getDouble(RPC_PARTDONE)) / 100f, // Percentage to [0..1]
						0f, // Not available
						tor.has(RPC_LABEL)? tor.getString(RPC_LABEL): null,
						tor.has(RPC_TIMEADDED)? new Date((long) (tor.getDouble(RPC_TIMEADDED) * 1000L)): null,
						null, // Not available
						error,
						settings.getType()));
				// @formatter:on
			}
		}

		// Return the list
		return torrents;

	}

	private ArrayList<Label> parseJsonRetrieveLabels(JSONObject response) throws JSONException {

		// Get the labels, of they exist (which is dependent on the plugin)
		if (!response.has("filters")) {
			return null;
		}
		JSONObject filters = response.getJSONObject("filters");
		if (!filters.has("label")) {
			return null;
		}
		JSONArray labels = filters.getJSONArray("label");

		// Parse response
		ArrayList<Label> allLabels = new ArrayList<Label>();
		for (int i = 0; i < labels.length(); i++) {
			JSONArray labelAndCount = labels.getJSONArray(i);
			if (labelAndCount.getString(0).equals("All")) {
				continue; // Ignore the 'All' filter, which is not an actual label
			}
			allLabels.add(new Label(labelAndCount.getString(0), labelAndCount.getInt(1)));
		}
		return allLabels;

	}

	private ArrayList<TorrentFile> parseJsonFileListing(JSONObject response, Torrent torrent) throws JSONException {

		// Parse response
		ArrayList<TorrentFile> files = new ArrayList<TorrentFile>();
		JSONArray objects = response.getJSONArray(RPC_DETAILS);
		JSONArray progress = response.getJSONArray(RPC_FILEPROGRESS);
		JSONArray priorities = response.getJSONArray(RPC_FILEPRIORITIES);
		if (objects != null) {
			for (int j = 0; j < objects.length(); j++) {

				JSONObject file = objects.getJSONObject(j);
				// Add the parsed torrent to the list
				// @formatter:off
				files.add(new TorrentFile(
						"" + file.getInt(RPC_INDEX),
						file.getString(RPC_PATH),
						file.getString(RPC_PATH),
						torrent.getLocationDir() + file.getString(RPC_PATH),
						file.getLong(RPC_SIZE),
						(long) (progress.getDouble(j) * file.getLong(RPC_SIZE)),
						DelugeCommon.convertDelugePriority(priorities.getInt(j), version)));
				// @formatter:on
			}
		}

		// Return the list
		return files;

	}

	private TorrentDetails parseJsonTorrentDetails(JSONObject response) throws JSONException {

		// Parse response
		List<String> trackers = new ArrayList<String>();
		JSONArray trackerObjects = response.getJSONArray(RPC_TRACKERS);
		if (trackerObjects != null && trackerObjects.length() > 0) {
			for (int i = 0; i < trackerObjects.length(); i++) {
				trackers.add(trackerObjects.getJSONObject(i).getString("url"));
			}
		}
		List<String> errors = new ArrayList<String>();
		String trackerStatus = response.getString(RPC_TRACKER_STATUS);
		errors.add(trackerStatus);

		return new TorrentDetails(trackers, errors);

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
