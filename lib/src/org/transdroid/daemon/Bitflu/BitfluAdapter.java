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
 package org.transdroid.daemon.Bitflu;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.TorrentStatus;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.task.AddByMagnetUrlTask;
import org.transdroid.daemon.task.AddByUrlTask;
import org.transdroid.daemon.task.DaemonTask;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;
import org.transdroid.daemon.task.GetFileListTask;
import org.transdroid.daemon.task.GetFileListTaskSuccessResult;
import org.transdroid.daemon.task.GetStatsTask;
import org.transdroid.daemon.task.GetStatsTaskSuccessResult;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.util.HttpHelper;
import org.transdroid.daemon.util.DLog;


/**
 * An adapter that allows for easy access to uTorrent torrent data. Communication
 * is handled via authenticated JSON-RPC HTTP GET requests and responses.
 * 
 * @author adrianulrich
 *
 */
 
// TODO: TransferRates support

public class BitfluAdapter implements IDaemonAdapter {

	private static final String LOG_NAME           = "Bitflu daemon";
	private static final String JSON_ROOT          = "Bitflu";
	private static final String RPC_TORRENT_LIST   = "torrentList";
	private static final String RPC_PAUSE_TORRENT  = "pause/";
	private static final String RPC_RESUME_TORRENT = "resume/";
	private static final String RPC_CANCEL_TORRENT = "cancel/";
	private static final String RPC_REMOVE_TORRENT = "wipe/";
	private static final String RPC_TORRENT_FILES  = "showfiles-ext/";
	private static final String RPC_START_DOWNLOAD = "startdownload/";
	
	private String            webuiroot = "";
	private DaemonSettings    settings;
	private DefaultHttpClient httpclient;

	/**
	 * Initialises an adapter that provides operations to the Bitflu web interface
	 */
	public BitfluAdapter(DaemonSettings settings) {
		this.settings = settings;
	}

	@Override
	public DaemonTaskResult executeTask(DaemonTask task) {
		
		try {
			switch (task.getMethod()) {
			case Retrieve:
				// Request all torrents from server
				JSONObject result = makeBitfluRequest(RPC_TORRENT_LIST);
				return new RetrieveTaskSuccessResult((RetrieveTask) task, parseJsonRetrieveTorrents(result.getJSONArray(JSON_ROOT)),null);
			case GetStats:
				return new GetStatsTaskSuccessResult((GetStatsTask) task, false, -1);
			case Pause:
				makeBitfluRequest(RPC_PAUSE_TORRENT + task.getTargetTorrent().getUniqueID());
				return new DaemonTaskSuccessResult(task);
			case Resume:
				makeBitfluRequest(RPC_RESUME_TORRENT + task.getTargetTorrent().getUniqueID());
				return new DaemonTaskSuccessResult(task);
			case Remove:
				// Remove a torrent
				RemoveTask removeTask = (RemoveTask) task;
				String removeUriBase = RPC_CANCEL_TORRENT;
				
				if(removeTask.includingData()) {
					removeUriBase = RPC_REMOVE_TORRENT;
				}
				DLog.d(LOG_NAME, "*** CALLING "+removeUriBase);
				makeBitfluRequest(removeUriBase + task.getTargetTorrent().getUniqueID());
				return new DaemonTaskSuccessResult(task);
			case GetFileList:
				JSONObject jfiles = makeBitfluRequest(RPC_TORRENT_FILES + task.getTargetTorrent().getUniqueID());
				return new GetFileListTaskSuccessResult((GetFileListTask) task, parseJsonShowFilesTorrent(jfiles.getJSONArray(JSON_ROOT)));
			case AddByUrl:
				String url = URLEncoder.encode(((AddByUrlTask)task).getUrl(), "UTF-8");
				makeBitfluRequest(RPC_START_DOWNLOAD + url);
				return new DaemonTaskSuccessResult(task);
			case AddByMagnetUrl:
				String magnet = URLEncoder.encode(((AddByMagnetUrlTask)task).getUrl(), "UTF-8");
				makeBitfluRequest(RPC_START_DOWNLOAD + magnet);
				return new DaemonTaskSuccessResult(task);
			default:
				return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.MethodUnsupported, task.getMethod() + " is not supported by " + getType()));
			}
		} catch (JSONException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.ParsingFailed, e.toString()));
		} catch (DaemonException e) {
			return new DaemonTaskFailureResult(task, e);
		} catch (UnsupportedEncodingException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.MethodUnsupported, e.toString()));
		}
	}
	
	private JSONObject makeBitfluRequest(String addToUrl) throws DaemonException {

		try {
				
			// Initialise the HTTP client
			if (httpclient == null) {
				initialise();
			}
			
			//TLog.d(LOG_NAME, "Request to: "+ buildWebUIUrl() + addToUrl);
			
			// Make request
			HttpGet httpget = new HttpGet(buildWebUIUrl() + addToUrl);
			HttpResponse response = httpclient.execute(httpget);

			// Read JSON response
			InputStream instream = response.getEntity().getContent();
			String result        = HttpHelper.ConvertStreamToString(instream);
			int httpstatus       = response.getStatusLine().getStatusCode();
			
			if(httpstatus != 200) {
				throw new DaemonException(ExceptionType.UnexpectedResponse, "Invalid reply from server, http status code: " + httpstatus);
			}
			
			if(result.equals("")) { // Empty responses are ok: add fake json content
				result = "empty_response";
			}
			
			JSONObject json = new JSONObject("{ \""+JSON_ROOT+"\" : "+ result +"}");
			
			instream.close();
			return json;			
			
		} catch (DaemonException e) {
			throw e;
		} catch (JSONException e) {
			DLog.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ParsingFailed, e.toString());
		} catch (Exception e) {
			DLog.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}
		
	}
	
	private ArrayList<Torrent> parseJsonRetrieveTorrents(JSONArray results) throws JSONException {
		ArrayList<Torrent> torrents = new ArrayList<Torrent>();
		
		if (results != null) {
			for (int i = 0; i < results.length(); i++) {
				
				JSONObject tor   = results.getJSONObject(i);
				long done_bytes  = tor.getLong("done_bytes");
				long total_bytes = tor.getLong("total_bytes");
				float percent    = ((float)done_bytes/((float)total_bytes+1));
				
				torrents.add(new Torrent(i, 
						tor.getString("key"),
						tor.getString("name"),
						convertBitfluStatus(tor),
						"/" + settings.getOS().getPathSeperator(),
						tor.getInt("speed_download"), 
						tor.getInt("speed_upload"), 
						0,                               // 'uploading to'
						tor.getInt("active_clients"), 
						tor.getInt("clients"), 
						tor.getInt("clients"), 
						tor.getInt("eta"),
						done_bytes,
						tor.getLong("uploaded_bytes"), 
						total_bytes,
						percent,  // Percentage to [0..1]
						0f,       // Not available
						null,     // label
						null,     // Not available
						null,     // Not available
						null,   // Not available
						settings.getType()));
			}
		}
		// Return the list
		return torrents;
	}
	
	
	private ArrayList<TorrentFile> parseJsonShowFilesTorrent(JSONArray response) throws JSONException {
		ArrayList<TorrentFile> files = new ArrayList<TorrentFile>();
		
		if(response != null) {
			for (int i = 0; i < response.length(); i++) {
				JSONObject finfo = response.getJSONObject(i);
				
				long done_bytes = finfo.getLong("done") * finfo.getLong("chunksize");
				long file_size  = finfo.getLong("size");
				
				if( done_bytes > file_size) { /* Shared chunk */
					done_bytes = file_size;
				}
				
				files.add(new TorrentFile(
					"" + i,
					finfo.getString("name"),
					finfo.getString("path"),
					null, // hmm.. can we have something without file:// ?!
					file_size,
					done_bytes,
					Priority.Normal
				));
			}
		}
		
		return files;
	}
	

	private TorrentStatus convertBitfluStatus(JSONObject obj) throws JSONException {
		
		if( obj.getInt("paused") != 0 ) {
			return TorrentStatus.Paused;
		}
		else if (obj.getLong("done_bytes") == obj.getLong("total_bytes")) {
			return TorrentStatus.Seeding;
		}
		return TorrentStatus.Downloading;
	}

	/**
	 * Instantiates an HTTP client with proper credentials that can be used for all HTTP requests.
	 * @param connectionTimeout The connection timeout in milliseconds
	 * @throws DaemonException On conflicting or missing settings
	 */
	private void initialise() throws DaemonException {

		httpclient = HttpHelper.createStandardHttpClient(settings, true);
		if(settings != null) {
			webuiroot = settings.getFolder();
		}
	}
	
	/**
	 * Build the URL of the Transmission web UI from the user settings.
	 * @return The URL of the RPC API
	 */
	private String buildWebUIUrl() {
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() + webuiroot + "/";
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
