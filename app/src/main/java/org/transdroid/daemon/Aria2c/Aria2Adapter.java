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
package org.transdroid.daemon.Aria2c;

import android.net.Uri;
import android.text.TextUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.base64.android.Base64;
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
import org.transdroid.daemon.task.PauseTask;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.ResumeTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.util.HttpHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * The daemon adapter from the Aria2 torrent client. Documentation available at http://aria2.sourceforge.net/manual/en/html/aria2c.html
 * @author erickok
 */
public class Aria2Adapter implements IDaemonAdapter {

	private static final String LOG_NAME = "Aria2 daemon";

	private DaemonSettings settings;
	private DefaultHttpClient httpclient;

	public Aria2Adapter(DaemonSettings settings) {
		this.settings = settings;
	}

	@Override
	public DaemonTaskResult executeTask(Log log, DaemonTask task) {

		try {
			JSONArray params = new JSONArray();

			switch (task.getMethod()) {
				case Retrieve:

					// Request all torrents from server
					// NOTE Since there is no aria2.tellAll (or something) we have to use batch requests
					JSONArray fields =
							new JSONArray().put("gid").put("status").put("totalLength").put("completedLength")
									.put("uploadLength").put("downloadSpeed").put("uploadSpeed").put("numSeeders")
									.put("dir").put("connections").put("errorCode").put("bittorrent").put("files");
					JSONObject active = buildRequest("aria2.tellActive", new JSONArray().put(fields));
					JSONObject waiting =
							buildRequest("aria2.tellWaiting", new JSONArray().put(0).put(9999).put(fields));
					JSONObject stopped =
							buildRequest("aria2.tellStopped", new JSONArray().put(0).put(9999).put(fields));
					params.put(active).put(waiting).put(stopped);

					List<Torrent> torrents = new ArrayList<Torrent>();
					JSONArray lists = makeRequestForArray(log, params.toString());
					for (int i = 0; i < lists.length(); i++) {
						torrents.addAll(parseJsonRetrieveTorrents(lists.getJSONObject(i).getJSONArray("result")));
					}
					return new RetrieveTaskSuccessResult((RetrieveTask) task, torrents, null);

				case GetTorrentDetails:

					// Request file listing of a torrent
					params.put(task.getTargetTorrent().getUniqueID()); // gid
					params.put(new JSONArray().put("bittorrent").put("errorCode"));

					JSONObject dinfo = makeRequest(log, buildRequest("aria2.tellStatus", params).toString());
					return new GetTorrentDetailsTaskSuccessResult((GetTorrentDetailsTask) task,
							parseJsonTorrentDetails(dinfo.getJSONObject("result")));

				case GetFileList:

					// Request file listing of a torrent
					params.put(task.getTargetTorrent().getUniqueID()); // torrent_id

					JSONObject finfo = makeRequest(log, buildRequest("aria2.getFiles", params).toString());
					return new GetFileListTaskSuccessResult((GetFileListTask) task,
							parseJsonFileListing(finfo.getJSONArray("result"), task.getTargetTorrent()));

				case AddByFile:

					// Encode the .torrent file's data
					String file = ((AddByFileTask) task).getFile();
					InputStream in =
							new Base64.InputStream(new FileInputStream(new File(URI.create(file))), Base64.ENCODE);
					StringWriter writer = new StringWriter();
					int c;
					while ((c = in.read()) != -1) {
						writer.write(c);
					}
					in.close();

					// Request to add a torrent by local .torrent file
					params.put(writer.toString());
					makeRequest(log, buildRequest("aria2.addTorrent", params).toString());
					return new DaemonTaskSuccessResult(task);

				case AddByUrl:

					// Request to add a torrent by URL
					String url = ((AddByUrlTask) task).getUrl();
					params.put(new JSONArray().put(url));

					makeRequest(log, buildRequest("aria2.addUri", params).toString());
					return new DaemonTaskSuccessResult(task);

				case AddByMagnetUrl:

					// Request to add a magnet link by URL
					String magnet = ((AddByMagnetUrlTask) task).getUrl();
					params.put(new JSONArray().put(magnet));

					makeRequest(log, buildRequest("aria2.addUri", params).toString());
					return new DaemonTaskSuccessResult(task);

				case Remove:

					// Remove a torrent
					RemoveTask removeTask = (RemoveTask) task;
					makeRequest(log,
							buildRequest(removeTask.includingData() ? "aria2.removeDownloadResult" : "aria2.remove",
									params.put(removeTask.getTargetTorrent().getUniqueID())).toString());
					return new DaemonTaskSuccessResult(task);

				case Pause:

					// Pause a torrent
					PauseTask pauseTask = (PauseTask) task;
					makeRequest(log, buildRequest("aria2.pause", params.put(pauseTask.getTargetTorrent().getUniqueID()))
							.toString());
					return new DaemonTaskSuccessResult(task);

				case PauseAll:

					// Resume all torrents
					makeRequest(log, buildRequest("aria2.pauseAll", null).toString());
					return new DaemonTaskSuccessResult(task);

				case Resume:

					// Resume a torrent
					ResumeTask resumeTask = (ResumeTask) task;
					makeRequest(log,
							buildRequest("aria2.unpause", params.put(resumeTask.getTargetTorrent().getUniqueID()))
									.toString());
					return new DaemonTaskSuccessResult(task);

				case ResumeAll:

					// Resume all torrents
					makeRequest(log, buildRequest("aria2.unpauseAll", null).toString());
					return new DaemonTaskSuccessResult(task);

				case SetTransferRates:

					// Request to set the maximum transfer rates
					SetTransferRatesTask ratesTask = (SetTransferRatesTask) task;
					JSONObject options = new JSONObject();
					options.put("max-overall-download-limit",
							(ratesTask.getDownloadRate() == null ? -1 : ratesTask.getDownloadRate()));
					options.put("max-overall-upload-limit",
							(ratesTask.getUploadRate() == null ? -1 : ratesTask.getUploadRate()));

					makeRequest(log, buildRequest("aria2.changeGlobalOption", params.put(options)).toString());
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

	private JSONObject buildRequest(String sendMethod, JSONArray params) throws JSONException {

		// Build request for method
		if (!TextUtils.isEmpty(settings.getExtraPassword())) {
			JSONArray signed = new JSONArray();
			// Start with the secret token as parameter and then add the normal parameters
			signed.put("token:" + settings.getExtraPassword());
			if (params != null) {
				for (int i = 0; i < params.length(); i++) {
					signed.put(params.get(i));
				}
			}
			params = signed;
		}
		JSONObject request = new JSONObject();
		request.put("id", "transdroid");
		request.put("jsonrpc", "2.0");
		request.put("method", sendMethod);
		request.put("params", params);
		return request;

	}

	private synchronized JSONObject makeRequest(Log log, String data) throws DaemonException {
		String raw = makeRawRequest(log, data);
		try {
			return new JSONObject(raw);
		} catch (JSONException e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.UnexpectedResponse, e.toString());
		}
	}

	private synchronized JSONArray makeRequestForArray(Log log, String data) throws DaemonException {
		String raw = makeRawRequest(log, data);
		try {
			return new JSONArray(raw);
		} catch (JSONException e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.UnexpectedResponse, e.toString());
		}
	}

	private synchronized String makeRawRequest(Log log, String data) throws DaemonException {

		try {

			// Initialise the HTTP client
			if (httpclient == null) {
				httpclient = HttpHelper.createStandardHttpClient(settings, !TextUtils.isEmpty(settings.getUsername()));
				httpclient.addRequestInterceptor(HttpHelper.gzipRequestInterceptor);
				httpclient.addResponseInterceptor(HttpHelper.gzipResponseInterceptor);
			}

			// Set POST URL and data
			String url =
					(settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() +
							(settings.getFolder() == null ? "" : settings.getFolder()) + "/jsonrpc";
			HttpPost httppost = new HttpPost(url);
			httppost.setEntity(new StringEntity(data));
			httppost.setHeader("Content-Type", "application/json");
			httppost.setHeader("Accept", "application/json");

			// Execute
			HttpResponse response = httpclient.execute(httppost);

			HttpEntity entity = response.getEntity();
			if (entity == null) {
				throw new DaemonException(ExceptionType.UnexpectedResponse, "No HTTP entity in response object.");
			}

			// Read JSON response
			InputStream instream = entity.getContent();
			String result = HttpHelper.convertStreamToString(instream);
			instream.close();

			log.d(LOG_NAME, "Success: " +
					(result.length() > 300 ? result.substring(0, 300) + "... (" + result.length() + " chars)" :
							result));
			return result;

		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	private ArrayList<Torrent> parseJsonRetrieveTorrents(JSONArray response) throws JSONException, DaemonException {

		// Parse response
		ArrayList<Torrent> torrents = new ArrayList<Torrent>();
		for (int j = 0; j < response.length(); j++) {

			// Add the parsed torrent to the list
			JSONObject tor = response.getJSONObject(j);
			int downloadSpeed = tor.getInt("downloadSpeed");
			long totalLength = tor.getLong("totalLength");
			long completedLength = tor.getLong("completedLength");
			int numSeeders = tor.has("numSeeders") ? tor.getInt("numSeeders") : 0;
			TorrentStatus status = convertAriaState(tor.getString("status"), completedLength == totalLength);
			int errorCode = tor.optInt("errorCode", 0);
			String error = errorCode > 0 ? convertAriaError(errorCode) : null;
			String name = null;
			JSONObject bittorrent;
			if (tor.has("bittorrent")) {
				// Get name form the bittorrent info object
				bittorrent = tor.getJSONObject("bittorrent");
				if (bittorrent.has("info")) {
					name = bittorrent.getJSONObject("info").getString("name");
				}
			} else if (tor.has("files")) {
				// Get name from the first included file we can find
				JSONArray files = tor.getJSONArray("files");
				if (files.length() > 0) {
					name = Uri.parse(files.getJSONObject(0).getString("path")).getLastPathSegment();
					if (name == null) {
						name = files.getJSONObject(0).getString("path");
					}
				}
			}
			if (name == null) {
				name = tor.getString("gid"); // Fallback name
			}
			// @formatter:off
			torrents.add(new Torrent(
					j, 
					tor.getString("gid"), 
					name, 
					status, 
					tor.getString("dir"), 
					downloadSpeed,
					tor.getInt("uploadSpeed"), 
					tor.getInt("connections"), 
					numSeeders , 
					tor.getInt("connections"), 
					numSeeders, 
					(downloadSpeed > 0? (int) (totalLength / downloadSpeed): -1), 
					completedLength, 
					tor.getLong("uploadLength"),
					totalLength, 
					completedLength / (float) totalLength, // Percentage to [0..1]
					0f, // Not available
					null, // Not available
					null, // Not available
					null, // Not available
					error, 
					settings.getType()));
			// @formatter:on

		}
		return torrents;

	}

	private ArrayList<TorrentFile> parseJsonFileListing(JSONArray response, Torrent torrent) throws JSONException {

		// Parse response
		ArrayList<TorrentFile> files = new ArrayList<TorrentFile>();
		for (int j = 0; j < response.length(); j++) {

			JSONObject file = response.getJSONObject(j);
			// Add the parsed torrent to the list
			String rel = file.getString("path");
			if (rel.startsWith(torrent.getLocationDir())) {
				rel = rel.substring(torrent.getLocationDir().length());
			}
			// @formatter:off
			files.add(new TorrentFile(
					Integer.toString(file.getInt("index")), 
					rel, 
					rel, 
					file.getString("path"), 
					file.getLong("length"),
					file.getLong("completedLength"), 
					file.getBoolean("selected") ? Priority.Normal : Priority.Off));
			// @formatter:on

		}
		return files;

	}

	private TorrentDetails parseJsonTorrentDetails(JSONObject response) throws JSONException {

		// Parse response
		List<String> trackers = new ArrayList<String>();
		List<String> errors = new ArrayList<String>();

		int error = response.optInt("errorCode", 0);
		if (error > 0) {
			errors.add(convertAriaError(error));
		}

		if (response.has("bittorrent")) {
			JSONObject bittorrent = response.getJSONObject("bittorrent");
			JSONArray announceList = bittorrent.getJSONArray("announceList");
			for (int i = 0; i < announceList.length(); i++) {
				JSONArray announceUrlList = announceList.getJSONArray(i);
				for (int j = 0; j < announceUrlList.length(); j++) {
					trackers.add(announceUrlList.getString(j));
				}
			}
		}

		return new TorrentDetails(trackers, errors);

	}

	private TorrentStatus convertAriaState(String state, boolean isFinished) {
		// Aria2 sends a string as status code
		// (http://aria2.sourceforge.net/manual/en/html/aria2c.html#aria2.tellStatus)
		if (state.equals("active")) {
			return isFinished ? TorrentStatus.Seeding : TorrentStatus.Downloading;
		} else if (state.equals("waiting")) {
			return TorrentStatus.Queued;
		} else if (state.equals("paused") || state.equals("complete")) {
			return TorrentStatus.Paused;
		} else if (state.equals("error")) {
			return TorrentStatus.Error;
		} else if (state.equals("removed")) {
			return TorrentStatus.Checking;
		}
		return TorrentStatus.Unknown;
	}

	private String convertAriaError(int errorCode) {
		// Aria2 sends an exit code as error (http://aria2.sourceforge.net/manual/en/html/aria2c.html#id1)
		String error = "Aria error #" + Integer.toString(errorCode);
		switch (errorCode) {
			case 3:
			case 4:
				return error + ": Resource was not found";
			case 5:
				return error + ": Aborted because download speed was too slow";
			case 6:
				return error + ": Network problem occurred";
			case 8:
				return error + ": Remote server did not support resume when resume was required to complete download";
			case 9:
				return error + ": There was not enough disk space available";
			case 11:
			case 12:
				return error + ": Duplicate file or info hash download";
			case 15:
			case 16:
				return error + ": Aria2 could not create new or open or truncate existing file";
			case 17:
			case 18:
			case 19:
				return error + ": File I/O error occurred";
			case 20:
			case 27:
				return error + ": Aria2 could not parse Magnet URI or Metalink document";
			case 21:
				return error + ": FTP command failed";
			case 22:
				return error + ": HTTP response header was bad or unexpected";
			case 23:
				return error + ": Too many redirects occurred";
			case 24:
				return error + ": HTTP authorization failed";
			case 26:
				return error + ": \".torrent\" file is corrupted or missing information that aria2 needs";
			default:
				return error;
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
