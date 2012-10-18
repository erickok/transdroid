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
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
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
import org.transdroid.daemon.task.PauseTask;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.ResumeTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetDownloadLocationTask;
import org.transdroid.daemon.task.SetFilePriorityTask;
import org.transdroid.daemon.task.SetTrackersTask;
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.util.DLog;
import org.transdroid.daemon.util.HttpHelper;

import com.android.internalcopy.http.multipart.FilePart;
import com.android.internalcopy.http.multipart.MultipartEntity;
import com.android.internalcopy.http.multipart.Part;

/**
 * The daemon adapter from the Deluge torrent client.
 * 
 * @author erickok
 * 
 */
public class DelugeAdapter implements IDaemonAdapter {

	private static final String LOG_NAME = "Deluge daemon";

	private static final String PATH_TO_RPC = "/json";
	private static final String PATH_TO_UPLOAD = "/upload";
	
	private static final String RPC_ID = "id";
	private static final String RPC_METHOD = "method";
	private static final String RPC_PARAMS = "params";
	private static final String RPC_RESULT = "result";
	private static final String RPC_TORRENTS = "torrents";
	private static final String RPC_FILE = "file";
	private static final String RPC_FILES = "files";
	private static final String RPC_SESSION_ID = "_session_id";

	private static final String RPC_METHOD_LOGIN = "auth.login";
	private static final String RPC_METHOD_GET = "web.update_ui";
	private static final String RPC_METHOD_STATUS = "core.get_torrent_status";
	private static final String RPC_METHOD_ADD = "core.add_torrent_url";
	private static final String RPC_METHOD_ADD_MAGNET = "core.add_torrent_magnet";
	private static final String RPC_METHOD_ADD_FILE = "web.add_torrents";
	private static final String RPC_METHOD_REMOVE = "core.remove_torrent";
	private static final String RPC_METHOD_PAUSE = "core.pause_torrent";
	private static final String RPC_METHOD_PAUSE_ALL = "core.pause_all_torrents";
	private static final String RPC_METHOD_RESUME = "core.resume_torrent";
	private static final String RPC_METHOD_RESUME_ALL = "core.resume_all_torrents";
	private static final String RPC_METHOD_SETCONFIG = "core.set_config";
	private static final String RPC_METHOD_SETFILE = "core.set_torrent_file_priorities";
	//private static final String RPC_METHOD_SETOPTIONS = "core.set_torrent_options";
	private static final String RPC_METHOD_MOVESTORAGE = "core.move_storage";
	private static final String RPC_METHOD_SETTRACKERS = "core.set_torrent_trackers";

	private static final String RPC_NAME = "name";
	private static final String RPC_STATUS = "state";
	private static final String RPC_MESSAGE = "message";
	private static final String RPC_TRACKERSTATUS = "tracker_status";
	private static final String RPC_SAVEPATH = "save_path";
	private static final String RPC_MAXDOWNLOAD = "max_download_speed";
	private static final String RPC_MAXUPLOAD = "max_upload_speed";

	private static final String RPC_RATEDOWNLOAD = "download_payload_rate";
	private static final String RPC_RATEUPLOAD = "upload_payload_rate";
	private static final String RPC_PEERSGETTING = "num_seeds";
	private static final String RPC_PEERSSENDING = "num_seeds";
	private static final String RPC_PEERSCONNECTED = "num_peers";
	private static final String RPC_PEERSKNOWN = "total_peers";
	private static final String RPC_ETA = "eta";
	private static final String RPC_TIMEADDED = "time_added";

	private static final String RPC_DOWNLOADEDEVER = "total_done";
	private static final String RPC_UPLOADEDEVER = "total_uploaded";
	private static final String RPC_TOTALSIZE = "total_size";
	private static final String RPC_PARTDONE = "progress";
	private static final String RPC_LABEL = "label";
	private static final String RPC_TRACKERS = "trackers";
	private static final String RPC_TRACKER_STATUS = "tracker_status";

	private static final String RPC_DETAILS = "files";
	private static final String RPC_INDEX = "index";
	private static final String RPC_PATH = "path";
	private static final String RPC_SIZE = "size";
	private static final String RPC_FILEPROGRESS = "file_progress";
	private static final String RPC_FILEPRIORITIES = "file_priorities";

	private static final String[] RPC_FIELDS_ARRAY = new String[] { 
			RPC_NAME, RPC_STATUS, RPC_SAVEPATH, RPC_RATEDOWNLOAD, RPC_RATEUPLOAD,
			RPC_PEERSGETTING, RPC_PEERSSENDING, RPC_PEERSCONNECTED,
			RPC_PEERSKNOWN, RPC_ETA, RPC_DOWNLOADEDEVER, RPC_UPLOADEDEVER,
			RPC_TOTALSIZE, RPC_PARTDONE, RPC_LABEL, RPC_MESSAGE, RPC_TIMEADDED, RPC_TRACKER_STATUS };

	private DaemonSettings settings;
	private DefaultHttpClient httpclient;
	private Cookie sessionCookie;
	
	public DelugeAdapter(DaemonSettings settings) {
		this.settings = settings;
	}

	public JSONArray AddTorrentByFile(String file) throws JSONException, ClientProtocolException, IOException, DaemonException {

		String url = buildWebUIUrl() + PATH_TO_UPLOAD;
		
		DLog.d(LOG_NAME, "Uploading a file to the Deluge daemon: " + url);

		// Initialise the HTTP client
		if (httpclient == null) {
			initialise();
		}

		// Setup client using POST
		HttpPost httppost = new HttpPost(url);
		File upload = new File(URI.create(file));
		Part[] parts = { new FilePart(RPC_FILE, upload) };
		httppost.setEntity(new MultipartEntity(parts, httppost.getParams()));

		// Make request
		HttpResponse response = httpclient.execute(httppost);

		// Read JSON response
		InputStream instream = response.getEntity().getContent();
		String result = HttpHelper.ConvertStreamToString(instream);

		// If the upload succeeded, add the torrent file on the server
		// For this we need the file name, which is now send as a JSON object like:
		// {"files": ["/tmp/delugeweb/tmp00000.torrent"], "success": true}
		String remoteFile = (new JSONObject(result)).getJSONArray(RPC_FILES).getString(0);
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
	public DaemonTaskResult executeTask(DaemonTask task) {
		
		try {
			JSONArray params = new JSONArray();

			// Array of the fields needed for files listing calls
			JSONArray ffields = new JSONArray();
			ffields.put(RPC_DETAILS);
			ffields.put(RPC_FILEPROGRESS);
			ffields.put(RPC_FILEPRIORITIES);
			
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
				
				JSONObject result = makeRequest(buildRequest(RPC_METHOD_GET, params));
				return new RetrieveTaskSuccessResult((RetrieveTask) task, parseJsonRetrieveTorrents(result.getJSONObject(RPC_RESULT)),null);

			case GetTorrentDetails:

				// Array of the fields needed for files listing calls
				JSONArray dfields = new JSONArray();
				dfields.put(RPC_TRACKERS);
				dfields.put(RPC_TRACKER_STATUS);
				
				// Request file listing of a torrent
				params.put(task.getTargetTorrent().getUniqueID()); // torrent_id
				params.put(dfields); // keys
				
				JSONObject dinfo = makeRequest(buildRequest(RPC_METHOD_STATUS, params));
				return new GetTorrentDetailsTaskSuccessResult((GetTorrentDetailsTask) task, parseJsonTorrentDetails(dinfo.getJSONObject(RPC_RESULT)));
				
			case GetFileList:

				// Request file listing of a torrent
				params.put(task.getTargetTorrent().getUniqueID()); // torrent_id
				params.put(ffields); // keys
				
				JSONObject finfo = makeRequest(buildRequest(RPC_METHOD_STATUS, params));
				return new GetFileListTaskSuccessResult((GetFileListTask) task, parseJsonFileListing(finfo.getJSONObject(RPC_RESULT), task.getTargetTorrent()));
				
			case AddByFile:

				// Request to add a torrent by local .torrent file
				String file = ((AddByFileTask)task).getFile();
				makeRequest(buildRequest(RPC_METHOD_ADD_FILE, AddTorrentByFile(file)));
				return new DaemonTaskSuccessResult(task);

			case AddByUrl:

				// Request to add a torrent by URL
				String url = ((AddByUrlTask)task).getUrl();
				params.put(url);
				params.put(new JSONArray());
				
				makeRequest(buildRequest(RPC_METHOD_ADD, params));
				return new DaemonTaskSuccessResult(task);

			case AddByMagnetUrl:

				// Request to add a magnet link by URL
				String magnet = ((AddByMagnetUrlTask)task).getUrl();
				params.put(magnet);
				params.put(new JSONArray());
				
				makeRequest(buildRequest(RPC_METHOD_ADD_MAGNET, params));
				return new DaemonTaskSuccessResult(task);

			case Remove:

				// Remove a torrent
				RemoveTask removeTask = (RemoveTask) task;
				params.put(removeTask.getTargetTorrent().getUniqueID());
				params.put(removeTask.includingData());
				makeRequest(buildRequest(RPC_METHOD_REMOVE, params));
				return new DaemonTaskSuccessResult(task);
				
			case Pause:

				// Pause a torrent
				PauseTask pauseTask = (PauseTask) task;
				makeRequest(buildRequest(RPC_METHOD_PAUSE,
						((new JSONArray()).put((new JSONArray()).put(pauseTask.getTargetTorrent().getUniqueID())))));
				return new DaemonTaskSuccessResult(task);
				
			case PauseAll:

				// Resume all torrents
				makeRequest(buildRequest(RPC_METHOD_PAUSE_ALL, null));
				return new DaemonTaskSuccessResult(task);

			case Resume:

				// Resume a torrent
				ResumeTask resumeTask = (ResumeTask) task;
				makeRequest(buildRequest(RPC_METHOD_RESUME,
						((new JSONArray()).put((new JSONArray()).put(resumeTask.getTargetTorrent().getUniqueID())))));
				return new DaemonTaskSuccessResult(task);

			case ResumeAll:

				// Resume all torrents
				makeRequest(buildRequest(RPC_METHOD_RESUME_ALL, null));
				return new DaemonTaskSuccessResult(task);

			case SetFilePriorities:

				// Set the priorities of files in a specific torrent
				SetFilePriorityTask prioTask = (SetFilePriorityTask) task;
				
				// We first need a listing of all the files (because we can only set the priorities all at once)
				params.put(task.getTargetTorrent().getUniqueID()); // torrent_id
				params.put(ffields); // keys				
				JSONObject pinfo = makeRequest(buildRequest(RPC_METHOD_STATUS, params));
				ArrayList<TorrentFile> pfiles = parseJsonFileListing(pinfo.getJSONObject(RPC_RESULT), prioTask.getTargetTorrent());
				
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
					pfields.put(convertPriority(newPriority));
				}
				params.put(pfields); // keys
				
				// Make a single call to set the priorities on all files at once
				makeRequest(buildRequest(RPC_METHOD_SETFILE, params));
				return new DaemonTaskSuccessResult(task);
				
			case SetDownloadLocation:
				
				// Set the download location of some torrent
				SetDownloadLocationTask sdlTask = (SetDownloadLocationTask) task;
				// This works, but does not move the torrent
				//makeRequest(buildRequest(RPC_METHOD_SETOPTIONS, buildSetTorrentOptions(
				//		sdlTask.getTargetTorrent().getUniqueID(), RPC_DOWNLOADLOCATION, sdlTask.getNewLocation())));
				params.put(new JSONArray().put(task.getTargetTorrent().getUniqueID()));
				params.put(sdlTask.getNewLocation());
				makeRequest(buildRequest(RPC_METHOD_MOVESTORAGE, params));
				return new DaemonTaskSuccessResult(task);
				
			case SetTransferRates:

				// Request to set the maximum transfer rates
				SetTransferRatesTask ratesTask = (SetTransferRatesTask) task;
				JSONObject map = new JSONObject();
				map.put(RPC_MAXUPLOAD, (ratesTask.getUploadRate() == null? -1: ratesTask.getUploadRate().intValue()));
				map.put(RPC_MAXDOWNLOAD, (ratesTask.getDownloadRate() == null? -1: ratesTask.getDownloadRate().intValue()));

				makeRequest(buildRequest(RPC_METHOD_SETCONFIG, (new JSONArray()).put(map)));
				return new DaemonTaskSuccessResult(task);

			case SetTrackers:

				// Set the trackers of some torrent
				SetTrackersTask trackersTask = (SetTrackersTask) task;
				JSONArray trackers = new JSONArray();
				// Build an JSON arrays of objcts that each have a tier (order) number and an url
				for (int i = 0; i < trackersTask.getNewTrackers().size(); i++) {
					JSONObject trackerObj = new JSONObject();
					trackerObj.put("tier", i);
					trackerObj.put("url", trackersTask.getNewTrackers().get(i));
					trackers.put(trackerObj);
				}
				params.put(new JSONArray().put(task.getTargetTorrent().getUniqueID()));
				params.put(trackers);
				makeRequest(buildRequest(RPC_METHOD_SETTRACKERS, params));
				return new DaemonTaskSuccessResult(task);
				
			default:
				return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.MethodUnsupported, task.getMethod() + " is not supported by " + getType()));
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
		
	private JSONObject buildRequest(String sendMethod, JSONArray params) throws JSONException {

		// Build request for method
		JSONObject request = new JSONObject();
		request.put(RPC_METHOD, sendMethod);
		request.put(RPC_PARAMS, (params == null) ? new JSONArray() : params);
		request.put(RPC_ID, 2);
		return request;
		
	}
	
	private JSONObject makeRequest(JSONObject data) throws DaemonException {

		try {
			
			// Initialise the HTTP client
			if (httpclient == null) {
				initialise();
			}
	
			// Login first?
			if (sessionCookie == null) {
				
				// Build login object
				String extraPass = settings.getExtraPassword();
				if (extraPass == null || extraPass == "") {
					extraPass = settings.getPassword();
				}
				JSONObject loginRequest = new JSONObject();
				loginRequest.put(RPC_METHOD, RPC_METHOD_LOGIN);
				loginRequest.put(RPC_PARAMS, (new JSONArray()).put(extraPass));
				loginRequest.put(RPC_ID, 1);

				// Set POST URL and data
				HttpPost httppost = new HttpPost(buildWebUIUrl() + PATH_TO_RPC);
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
					throw new DaemonException(ExceptionType.AuthenticationFailure, "Password error? Server time difference? No (valid) cookie in response and JSON was: " + HttpHelper.ConvertStreamToString(instream));
				}

			}

			// Regular action

			// Set POST URL and data
			HttpPost httppost = new HttpPost(buildWebUIUrl() + PATH_TO_RPC);
			StringEntity se = new StringEntity(data.toString());
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
				String result = HttpHelper.ConvertStreamToString(instream);
				JSONObject json = new JSONObject(result);
				instream.close();

				DLog.d(LOG_NAME, "Success: " + (result.length() > 300? result.substring(0, 300) + "... (" + result.length() + " chars)": result));
				
				// Return JSON object
				return json;

			}

			// No result?
			throw new DaemonException(ExceptionType.UnexpectedResponse, "No HTTP entity in response object.");

		} catch (JSONException e) {
			DLog.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.UnexpectedResponse, e.toString());
		} catch (Exception e) {
			DLog.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}
		
	}

	/**
	 * Instantiates an HTTP client with proper credentials that can be used for all Transmission requests.
	 * @param connectionTimeout The connection timeout in milliseconds
	 * @throws DaemonException On missing settings
	 */
	private void initialise() throws DaemonException {

		httpclient = HttpHelper.createStandardHttpClient(settings, settings.getUsername() != null && settings.getUsername() != "");
        httpclient.addRequestInterceptor(HttpHelper.gzipRequestInterceptor);
        httpclient.addResponseInterceptor(HttpHelper.gzipResponseInterceptor);
        
	}

    /**
	 * Build the URL of the Transmission web UI from the user settings.
	 * @return The URL of the RPC API
	 */
	private String buildWebUIUrl() {
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() + (settings.getFolder() == null? "": settings.getFolder());
	}

	private ArrayList<Torrent> parseJsonRetrieveTorrents(JSONObject response) throws JSONException, DaemonException {

		// Parse response
		ArrayList<Torrent> torrents = new ArrayList<Torrent>();
		if (response.isNull(RPC_TORRENTS)) {
			throw new DaemonException(ExceptionType.NotConnected, "Web interface probably not connected to a daemon yet, because 'torrents' is null: " + response.toString());
		}
		JSONObject objects = response.getJSONObject(RPC_TORRENTS);
		JSONArray names = objects.names();
		if (names != null) {
			for (int j = 0; j < names.length(); j++) {
				
				JSONObject tor = objects.getJSONObject(names.getString(j));
				// Add the parsed torrent to the list
				TorrentStatus status = convertDelugeState(tor.getString(RPC_STATUS));
				String error = tor.getString(RPC_MESSAGE);
				if (tor.getString(RPC_TRACKER_STATUS).indexOf("Error") > 0) {
					error += (error.length() > 0? "\n": "") + tor.getString(RPC_TRACKER_STATUS);
					status = TorrentStatus.Error;
				}
				torrents.add(new Torrent(j, 
						names.getString(j), 
						tor.getString(RPC_NAME), 
						status,
						tor.getString(RPC_SAVEPATH) + settings.getOS().getPathSeperator(),
						tor.getInt(RPC_RATEDOWNLOAD), 
						tor.getInt(RPC_RATEUPLOAD), 
						tor.getInt(RPC_PEERSGETTING), 
						tor.getInt(RPC_PEERSSENDING), 
						tor.getInt(RPC_PEERSCONNECTED), 
						tor.getInt(RPC_PEERSKNOWN), 
						tor.getInt(RPC_ETA),
						tor.getLong(RPC_DOWNLOADEDEVER), 
						tor.getLong(RPC_UPLOADEDEVER), 
						tor.getLong(RPC_TOTALSIZE), 
						((float) tor.getDouble(RPC_PARTDONE)) / 100f, // Percentage to [0..1]
						0f, // Not available
						tor.has(RPC_LABEL)? tor.getString(RPC_LABEL): null,
						tor.has(RPC_TIMEADDED)? new Date(tor.getInt(RPC_TIMEADDED) * 1000L): null,
						null,
						tor.getString(RPC_MESSAGE))); // Not available
			}
		}

		// Return the list
		return torrents;

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
				files.add(new TorrentFile(
						"" + file.getInt(RPC_INDEX),
						file.getString(RPC_PATH),
						file.getString(RPC_PATH),
						torrent.getLocationDir() + file.getString(RPC_PATH),
						file.getLong(RPC_SIZE),
						(long) (progress.getDouble(j) * file.getLong(RPC_SIZE)),
						convertDelugePriority(priorities.getInt(j))));
			}
		}

		// Return the list
		return files;

	}

	private Priority convertDelugePriority(int priority) {
		switch (priority) {
		case 0:
			return Priority.Off;
		case 2:
			return Priority.Normal;
		case 5:
			return Priority.High;
		default:
			return Priority.Low;
		}
	}

	private int convertPriority(Priority priority) {
		switch (priority) {
		case Off:
			return 0;
		case Normal:
			return 2;
		case High:
			return 5;
		default:
			return 1;
		}
	}

	private TorrentStatus convertDelugeState(String state) {
		// Deluge sends a string with status code
		if (state.compareTo("Paused") == 0) {
			return TorrentStatus.Paused;
		} else if (state.compareTo("Seeding") == 0) {
			return TorrentStatus.Seeding;
		} else if (state.compareTo("Downloading") == 0 || state.compareTo("Active") == 0) {
			return TorrentStatus.Downloading;
		} else if (state.compareTo("Checking") == 0) {
			return TorrentStatus.Checking;
		} else if (state.compareTo("Queued") == 0) {
			return TorrentStatus.Queued;
		}
		return TorrentStatus.Unknown;
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
