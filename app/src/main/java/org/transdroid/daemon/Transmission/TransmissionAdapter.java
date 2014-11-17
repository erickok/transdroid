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
package org.transdroid.daemon.Transmission;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.base64.android.Base64;
import org.base64.android.Base64.InputStream;
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
import org.transdroid.daemon.task.ForceRecheckTask;
import org.transdroid.daemon.task.GetFileListTask;
import org.transdroid.daemon.task.GetFileListTaskSuccessResult;
import org.transdroid.daemon.task.GetStatsTask;
import org.transdroid.daemon.task.GetStatsTaskSuccessResult;
import org.transdroid.daemon.task.GetTorrentDetailsTask;
import org.transdroid.daemon.task.GetTorrentDetailsTaskSuccessResult;
import org.transdroid.daemon.task.PauseTask;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.ResumeTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetAlternativeModeTask;
import org.transdroid.daemon.task.SetDownloadLocationTask;
import org.transdroid.daemon.task.SetFilePriorityTask;
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.util.HttpHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The daemon adapter from the Transmission torrent client.
 * @author erickok
 */
public class TransmissionAdapter implements IDaemonAdapter {

	private static final String LOG_NAME = "Transdroid daemon";

	private static final int FOR_ALL = -1;

	private static final String RPC_ID = "id";
	private static final String RPC_NAME = "name";
	private static final String RPC_STATUS = "status";
	private static final String RPC_ERROR = "error";
	private static final String RPC_ERRORSTRING = "errorString";
	private static final String RPC_DOWNLOADDIR = "downloadDir";
	private static final String RPC_RATEDOWNLOAD = "rateDownload";
	private static final String RPC_RATEUPLOAD = "rateUpload";
	private static final String RPC_PEERSGETTING = "peersGettingFromUs";
	private static final String RPC_PEERSSENDING = "peersSendingToUs";
	private static final String RPC_PEERSCONNECTED = "peersConnected";
	private static final String RPC_ETA = "eta";
	private static final String RPC_DOWNLOADSIZE1 = "haveUnchecked";
	private static final String RPC_DOWNLOADSIZE2 = "haveValid";
	private static final String RPC_UPLOADEDEVER = "uploadedEver";
	private static final String RPC_TOTALSIZE = "sizeWhenDone";
	private static final String RPC_DATEADDED = "addedDate";
	private static final String RPC_DATEDONE = "doneDate";
	private static final String RPC_AVAILABLE = "desiredAvailable";
	private static final String RPC_COMMENT = "comment";

	private static final String RPC_FILE_NAME = "name";
	private static final String RPC_FILE_LENGTH = "length";
	private static final String RPC_FILE_COMPLETED = "bytesCompleted";
	private static final String RPC_FILESTAT_WANTED = "wanted";
	private static final String RPC_FILESTAT_PRIORITY = "priority";
	private static String sessionToken;
	private DaemonSettings settings;
	private DefaultHttpClient httpclient;
	private long rpcVersion = -1;

	public TransmissionAdapter(DaemonSettings settings) {
		this.settings = settings;
	}

	@Override
	public DaemonTaskResult executeTask(Log log, DaemonTask task) {

		try {

			// Get the server version
			if (rpcVersion <= -1) {
				// Get server session statistics
				JSONObject response = makeRequest(log, buildRequestObject("session-get", new JSONObject()));
				rpcVersion = response.getJSONObject("arguments").getInt("rpc-version");
			}

			JSONObject request = new JSONObject();
			switch (task.getMethod()) {
				case Retrieve:

					// Request all torrents from server
					JSONArray fields = new JSONArray();
					final String[] fieldsArray =
							new String[]{RPC_ID, RPC_NAME, RPC_ERROR, RPC_ERRORSTRING, RPC_STATUS, RPC_DOWNLOADDIR,
									RPC_RATEDOWNLOAD, RPC_RATEUPLOAD, RPC_PEERSGETTING, RPC_PEERSSENDING,
									RPC_PEERSCONNECTED, RPC_ETA, RPC_DOWNLOADSIZE1, RPC_DOWNLOADSIZE2, RPC_UPLOADEDEVER,
									RPC_TOTALSIZE, RPC_DATEADDED, RPC_DATEDONE, RPC_AVAILABLE, RPC_COMMENT};
					for (String field : fieldsArray) {
						fields.put(field);
					}
					request.put("fields", fields);

					JSONObject result = makeRequest(log, buildRequestObject("torrent-get", request));
					return new RetrieveTaskSuccessResult((RetrieveTask) task,
							parseJsonRetrieveTorrents(result.getJSONObject("arguments")), null);

				case GetStats:

					// Request the current server statistics
					JSONObject stats = makeRequest(log, buildRequestObject("session-get", new JSONObject()))
							.getJSONObject("arguments");
					return new GetStatsTaskSuccessResult((GetStatsTask) task, stats.getBoolean("alt-speed-enabled"),
							rpcVersion >= 12 ? stats.getLong("download-dir-free-space") : -1);

				case GetTorrentDetails:

					// Request fine details of a specific torrent
					JSONArray dfields = new JSONArray();
					dfields.put("trackers");
					dfields.put("trackerStats");

					JSONObject buildDGet =
							buildTorrentRequestObject(task.getTargetTorrent().getUniqueID(), null, false);
					buildDGet.put("fields", dfields);
					JSONObject getDResult = makeRequest(log, buildRequestObject("torrent-get", buildDGet));
					return new GetTorrentDetailsTaskSuccessResult((GetTorrentDetailsTask) task,
							parseJsonTorrentDetails(getDResult.getJSONObject("arguments")));

				case GetFileList:

					// Request all details for a specific torrent
					JSONArray ffields = new JSONArray();
					ffields.put("files");
					ffields.put("fileStats");

					JSONObject buildGet = buildTorrentRequestObject(task.getTargetTorrent().getUniqueID(), null, false);
					buildGet.put("fields", ffields);
					JSONObject getResult = makeRequest(log, buildRequestObject("torrent-get", buildGet));
					return new GetFileListTaskSuccessResult((GetFileListTask) task,
							parseJsonFileList(getResult.getJSONObject("arguments"), task.getTargetTorrent()));

				case AddByFile:

					// Add a torrent to the server by sending the contents of a local .torrent file
					String file = ((AddByFileTask) task).getFile();

					// Encode the .torrent file's data
					InputStream in =
							new Base64.InputStream(new FileInputStream(new File(URI.create(file))), Base64.ENCODE);
					StringWriter writer = new StringWriter();
					int c;
					while ((c = in.read()) != -1) {
						writer.write(c);
					}
					in.close();

					// Request to add a torrent by Base64-encoded meta data
					request.put("metainfo", writer.toString());

					makeRequest(log, buildRequestObject("torrent-add", request));
					return new DaemonTaskSuccessResult(task);

				case AddByUrl:

					// Request to add a torrent by URL
					String url = ((AddByUrlTask) task).getUrl();
					request.put("filename", url);

					makeRequest(log, buildRequestObject("torrent-add", request));
					return new DaemonTaskSuccessResult(task);

				case AddByMagnetUrl:

					// Request to add a magnet link by URL
					String magnet = ((AddByMagnetUrlTask) task).getUrl();
					request.put("filename", magnet);

					makeRequest(log, buildRequestObject("torrent-add", request));
					return new DaemonTaskSuccessResult(task);

				case Remove:

					// Remove a torrent
					RemoveTask removeTask = (RemoveTask) task;
					makeRequest(log, buildRequestObject("torrent-remove",
							buildTorrentRequestObject(removeTask.getTargetTorrent().getUniqueID(), "delete-local-data",
									removeTask.includingData())));
					return new DaemonTaskSuccessResult(task);

				case Pause:

					// Pause a torrent
					PauseTask pauseTask = (PauseTask) task;
					makeRequest(log, buildRequestObject("torrent-stop",
							buildTorrentRequestObject(pauseTask.getTargetTorrent().getUniqueID(), null, false)));
					return new DaemonTaskSuccessResult(task);

				case PauseAll:

					// Resume all torrents
					makeRequest(log,
							buildRequestObject("torrent-stop", buildTorrentRequestObject(FOR_ALL, null, false)));
					return new DaemonTaskSuccessResult(task);

				case Resume:

					// Resume a torrent
					ResumeTask resumeTask = (ResumeTask) task;
					makeRequest(log, buildRequestObject("torrent-start",
							buildTorrentRequestObject(resumeTask.getTargetTorrent().getUniqueID(), null, false)));
					return new DaemonTaskSuccessResult(task);

				case ResumeAll:

					// Resume all torrents
					makeRequest(log,
							buildRequestObject("torrent-start", buildTorrentRequestObject(FOR_ALL, null, false)));
					return new DaemonTaskSuccessResult(task);

				case SetDownloadLocation:

					// Change the download location
					SetDownloadLocationTask sdlTask = (SetDownloadLocationTask) task;
					// Build request
					JSONObject sdlrequest = new JSONObject();
					JSONArray sdlids = new JSONArray();
					sdlids.put(Long.parseLong(task.getTargetTorrent().getUniqueID()));
					sdlrequest.put("ids", sdlids);
					sdlrequest.put("location", sdlTask.getNewLocation());
					sdlrequest.put("move", true);
					makeRequest(log, buildRequestObject("torrent-set-location", sdlrequest));
					return new DaemonTaskSuccessResult(task);

				case SetFilePriorities:

					// Set priorities of the files of some torrent
					SetFilePriorityTask prioTask = (SetFilePriorityTask) task;

					// Build request
					JSONObject prequest = new JSONObject();
					JSONArray ids = new JSONArray();
					ids.put(Long.parseLong(task.getTargetTorrent().getUniqueID()));
					prequest.put("ids", ids);
					JSONArray fileids = new JSONArray();
					for (TorrentFile forfile : prioTask.getForFiles()) {
						fileids.put(Integer.parseInt(
								forfile.getKey())); // The keys are the indices of the files, so always numeric
					}
					switch (prioTask.getNewPriority()) {
						case Off:
							prequest.put("files-unwanted", fileids);
							break;
						case Low:
							prequest.put("files-wanted", fileids);
							prequest.put("priority-low", fileids);
							break;
						case Normal:
							prequest.put("files-wanted", fileids);
							prequest.put("priority-normal", fileids);
							break;
						case High:
							prequest.put("files-wanted", fileids);
							prequest.put("priority-high", fileids);
							break;
					}

					makeRequest(log, buildRequestObject("torrent-set", prequest));
					return new DaemonTaskSuccessResult(task);

				case SetTransferRates:

					// Request to set the maximum transfer rates
					SetTransferRatesTask ratesTask = (SetTransferRatesTask) task;
					if (ratesTask.getUploadRate() == null) {
						request.put("speed-limit-up-enabled", false);
					} else {
						request.put("speed-limit-up-enabled", true);
						request.put("speed-limit-up", ratesTask.getUploadRate().intValue());
					}
					if (ratesTask.getDownloadRate() == null) {
						request.put("speed-limit-down-enabled", false);
					} else {
						request.put("speed-limit-down-enabled", true);
						request.put("speed-limit-down", ratesTask.getDownloadRate().intValue());
					}

					makeRequest(log, buildRequestObject("session-set", request));
					return new DaemonTaskSuccessResult(task);

				case SetAlternativeMode:

					// Request to set the alternative speed mode (Tutle Mode)
					SetAlternativeModeTask altModeTask = (SetAlternativeModeTask) task;
					request.put("alt-speed-enabled", altModeTask.isAlternativeModeEnabled());
					makeRequest(log, buildRequestObject("session-set", request));
					return new DaemonTaskSuccessResult(task);

				case ForceRecheck:

					// Verify torrent data integrity
					ForceRecheckTask verifyTask = (ForceRecheckTask) task;
					makeRequest(log, buildRequestObject("torrent-verify",
							buildTorrentRequestObject(verifyTask.getTargetTorrent().getUniqueID(), null, false)));
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

	private JSONObject buildTorrentRequestObject(String torrentID, String extraKey, boolean extraValue)
			throws JSONException {
		return buildTorrentRequestObject(Long.parseLong(torrentID), extraKey, extraValue);
	}

	private JSONObject buildTorrentRequestObject(long torrentID, String extraKey, boolean extraValue)
			throws JSONException {

		// Build request for one specific torrent
		JSONObject request = new JSONObject();
		if (torrentID != FOR_ALL) {
			JSONArray ids = new JSONArray();
			ids.put(torrentID); // The only id to add
			request.put("ids", ids);
		}
		if (extraKey != null) {
			request.put(extraKey, extraValue);
		}
		return request;

	}

	private JSONObject buildRequestObject(String sendMethod, JSONObject arguments) throws JSONException {

		// Build request for method
		JSONObject request = new JSONObject();
		request.put("method", sendMethod);
		request.put("arguments", arguments);
		request.put("tag", 0);
		return request;
	}

	private synchronized JSONObject makeRequest(Log log, JSONObject data) throws DaemonException {

		try {

			// Initialise the HTTP client
			if (httpclient == null) {
				initialise();
			}
			final String sessionHeader = "X-Transmission-Session-Id";

			// Setup request using POST stream with URL and data
			HttpPost httppost = new HttpPost(buildWebUIUrl());
			StringEntity se = new StringEntity(data.toString(), "UTF-8");
			httppost.setEntity(se);

			// Send the stored session token as a header
			if (sessionToken != null) {
				httppost.addHeader(sessionHeader, sessionToken);
			}

			// Execute
			log.d(LOG_NAME, "Execute " + data.getString("method") + " request to " + httppost.getURI().toString());
			HttpResponse response = httpclient.execute(httppost);

			// Authentication error?
			if (response.getStatusLine().getStatusCode() == 401) {
				throw new DaemonException(ExceptionType.AuthenticationFailure,
						"401 HTTP response (username or password incorrect)");
			}

			// 409 error because of a session id?
			if (response.getStatusLine().getStatusCode() == 409) {

				// Retry post, but this time with the new session token that was encapsulated in the 409 response
				log.d(LOG_NAME, "Receive HTTP 409 with new session code; now try again for the actual request");
				sessionToken = response.getFirstHeader(sessionHeader).getValue();
				httppost.addHeader(sessionHeader, sessionToken);
				log.d(LOG_NAME,
						"Retry to execute " + data.getString("method") + " request, now with " + sessionHeader + ": " +
								sessionToken);
				response = httpclient.execute(httppost);

			}

			HttpEntity entity = response.getEntity();
			if (entity != null) {

				// Read JSON response
				java.io.InputStream instream = entity.getContent();
				String result = HttpHelper.convertStreamToString(instream);
				log.d(LOG_NAME, "Received content response starting with " +
								(result.length() > 100 ? result.substring(0, 100) + "..." : result));
				JSONObject json = new JSONObject(result);
				instream.close();

				// Return the JSON object
				return json;
			}

			log.d(LOG_NAME, "Error: No entity in HTTP response");
			throw new DaemonException(ExceptionType.UnexpectedResponse, "No HTTP entity object in response.");

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
		String folder = "/transmission";
		if (settings.getFolder() != null && !settings.getFolder().trim().equals("")) {
			// Allow the user's folder setting to override /transmission (as per Transmission's rpc-url option)
			folder = settings.getFolder().trim();
			// Strip any trailing slashes
			if (folder.endsWith("/")) {
				folder = folder.substring(0, folder.length() - 1);
			}
		}
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() +
				folder + "/rpc";
	}

	private ArrayList<Torrent> parseJsonRetrieveTorrents(JSONObject response) throws JSONException {

		// Parse response
		ArrayList<Torrent> torrents = new ArrayList<Torrent>();
		JSONArray rarray = response.getJSONArray("torrents");
		for (int i = 0; i < rarray.length(); i++) {
			JSONObject tor = rarray.getJSONObject(i);
			// Add the parsed torrent to the list
			float have = (float) (tor.getLong(RPC_DOWNLOADSIZE1) + tor.getLong(RPC_DOWNLOADSIZE2));
			long total = tor.getLong(RPC_TOTALSIZE);
			// Error is a number, see https://trac.transmissionbt.com/browser/trunk/libtransmission/transmission.h#L1747
			// We only consider it a real error if it is local (blocking), which is error code 3
			boolean hasError = tor.getInt(RPC_ERROR) == 3;
			String errorString = tor.getString(RPC_ERRORSTRING).trim();
			String commentString = tor.getString(RPC_COMMENT).trim();
			if (!commentString.equals("")) {
				errorString = errorString.equals("") ? commentString : errorString + "\n" + commentString;
			}
			String locationDir = tor.getString(RPC_DOWNLOADDIR);
			if (!locationDir.endsWith(settings.getOS().getPathSeperator())) {
				locationDir += settings.getOS().getPathSeperator();
			}
			// @formatter:off
			torrents.add(new Torrent(
					tor.getInt(RPC_ID),
					null,
					tor.getString(RPC_NAME),
					hasError ? TorrentStatus.Error : getStatus(tor.getInt(RPC_STATUS)),
					locationDir,
					tor.getInt(RPC_RATEDOWNLOAD),
					tor.getInt(RPC_RATEUPLOAD),
					tor.getInt(RPC_PEERSSENDING),
					tor.getInt(RPC_PEERSCONNECTED),
					tor.getInt(RPC_PEERSGETTING),
					tor.getInt(RPC_PEERSCONNECTED),
					tor.getInt(RPC_ETA),
					tor.getLong(RPC_DOWNLOADSIZE1) + tor.getLong(RPC_DOWNLOADSIZE2),
					tor.getLong(RPC_UPLOADEDEVER),
					tor.getLong(RPC_TOTALSIZE),
					//(float) tor.getDouble(RPC_PERCENTDONE),
					(total == 0 ? 0 : have / (float) total),
					(total == 0 ? 0 : (have + (float) tor.getLong(RPC_AVAILABLE)) / (float) total),
					// No label/category/group support in the RPC API for now
					null,
					new Date(tor.getLong(RPC_DATEADDED) * 1000L),
					new Date(tor.getLong(RPC_DATEDONE) * 1000L),
					errorString, settings.getType()));
			// @formatter:on
		}

		// Return the list
		return torrents;

	}

	private TorrentStatus getStatus(int status) {
		if (rpcVersion <= -1) {
			return TorrentStatus.Unknown;
		} else if (rpcVersion >= 14) {
			switch (status) {
				case 0:
					return TorrentStatus.Paused;
				case 1:
					return TorrentStatus.Waiting;
				case 2:
					return TorrentStatus.Checking;
				case 3:
					return TorrentStatus.Queued;
				case 4:
					return TorrentStatus.Downloading;
				case 5:
					return TorrentStatus.Queued;
				case 6:
					return TorrentStatus.Seeding;
			}
			return TorrentStatus.Unknown;
		} else {
			return TorrentStatus.getStatus(status);
		}
	}

	private ArrayList<TorrentFile> parseJsonFileList(JSONObject response, Torrent torrent) throws JSONException {

		// Parse response
		ArrayList<TorrentFile> torrentfiles = new ArrayList<TorrentFile>();
		JSONArray rarray = response.getJSONArray("torrents");
		if (rarray.length() > 0) {
			JSONArray files = rarray.getJSONObject(0).getJSONArray("files");
			JSONArray fileStats = rarray.getJSONObject(0).getJSONArray("fileStats");
			for (int i = 0; i < files.length(); i++) {
				JSONObject file = files.getJSONObject(i);
				JSONObject stat = fileStats.getJSONObject(i);
				// @formatter:off
				torrentfiles.add(new TorrentFile(
						String.valueOf(i),
						file.getString(RPC_FILE_NAME),
						file.getString(RPC_FILE_NAME),
						torrent.getLocationDir() + file.getString(RPC_FILE_NAME),
						file.getLong(RPC_FILE_LENGTH),
						file.getLong(RPC_FILE_COMPLETED),
						convertTransmissionPriority(stat.getBoolean(RPC_FILESTAT_WANTED), stat.getInt(RPC_FILESTAT_PRIORITY))));
				// @formatter:on
			}
		}

		// Return the list
		return torrentfiles;

	}

	private Priority convertTransmissionPriority(boolean isWanted, int priority) {
		if (!isWanted) {
			return Priority.Off;
		} else {
			switch (priority) {
				case 1:
					return Priority.High;
				case -1:
					return Priority.Low;
				default:
					return Priority.Normal;
			}
		}
	}

	private TorrentDetails parseJsonTorrentDetails(JSONObject response) throws JSONException {

		// Parse response
		// NOTE: Assumes only details for one torrent are requested at a time
		JSONArray rarray = response.getJSONArray("torrents");
		if (rarray.length() > 0) {
			JSONArray trackersList = rarray.getJSONObject(0).getJSONArray("trackers");
			List<String> trackers = new ArrayList<String>();
			for (int i = 0; i < trackersList.length(); i++) {
				trackers.add(trackersList.getJSONObject(i).getString("announce"));
			}
			JSONArray trackerStatsList = rarray.getJSONObject(0).getJSONArray("trackerStats");
			List<String> errors = new ArrayList<String>();
			for (int i = 0; i < trackerStatsList.length(); i++) {
				// Get the tracker response and if it was an error then add it
				String lar = trackerStatsList.getJSONObject(i).getString("lastAnnounceResult");
				if (lar != null && !lar.equals("") && !lar.equals("Success")) {
					errors.add(lar);
				}
			}
			return new TorrentDetails(trackers, errors);
		}

		return null;

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
