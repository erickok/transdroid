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
package org.transdroid.daemon.Vuze;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.lib.util.Base16Encoder;
import org.transdroid.core.gui.log.Log;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.DaemonMethod;
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
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetFilePriorityTask;
import org.transdroid.daemon.task.SetTransferRatesTask;

/**
 * An adapter that allows for easy access to Vuze torrent data. Communication
 * is handled via the XML-RPC protocol.
 *
 * @author erickok
 *
 */
public class VuzeAdapter implements IDaemonAdapter {

	private static final String LOG_NAME = "Vuze daemon";

	private static final String RPC_URL = "/process.cgi";

	private VuzeXmlOverHttpClient rpcclient;
	private DaemonSettings settings;

	private Long savedConnectionID;
	private Long savedPluginID;
	private Long savedDownloadManagerID;
	private Long savedTorrentManagerID;
	private Long savedPluginConfigID;
	
	public VuzeAdapter(DaemonSettings settings) {
		this.settings = settings;
	}

	@Override
	public DaemonTaskResult executeTask(Log log, DaemonTask task) {
		
		try {
			switch (task.getMethod()) {
			case Retrieve:

				Object result = makeVuzeCall(DaemonMethod.Retrieve, "getDownloads");
				return new RetrieveTaskSuccessResult((RetrieveTask) task, onTorrentsRetrieved(log, result),null);
				
			case GetFileList:
				
				// Retrieve a listing of the files in some torrent
				Object fresult = makeVuzeCall(DaemonMethod.GetFileList, "getDiskManagerFileInfo", task.getTargetTorrent(), new Object[] {} );
				return new GetFileListTaskSuccessResult((GetFileListTask) task, onTorrentFilesRetrieved(fresult, task.getTargetTorrent()));

			case AddByFile:

				byte[] bytes;
				FileInputStream in = null;
				try {
					// Request to add a torrent by local .torrent file
					String file = ((AddByFileTask)task).getFile();
					in = new FileInputStream(new File(URI.create(file)));
					bytes = new byte[in.available()];
					in.read(bytes, 0, in.available());
					in.close();
				} catch (FileNotFoundException e) {
					return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.FileAccessError, e.toString()));
				} catch (IllegalArgumentException e) {
					return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.FileAccessError, "Invalid local URI"));
				} catch (Exception e) {
					return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.FileAccessError, e.toString()));
				} finally {
					try {
						if (in != null)
							in.close();
					} catch (IOException e) {
						// Ignore; it was already closed or never opened
					}
				}
				makeVuzeCall(DaemonMethod.AddByFile, "createFromBEncodedData[byte[]]", new String[] { Base16Encoder.encode(bytes) });
				return new DaemonTaskSuccessResult(task);
				
			case AddByUrl:

				// Request to add a torrent by URL
				String url = ((AddByUrlTask)task).getUrl();
				makeVuzeCall(DaemonMethod.AddByUrl, "addDownload[URL]", new URL[] { new URL(url) });
				return new DaemonTaskSuccessResult(task);
				
			case Remove:

				// Remove a torrent
				RemoveTask removeTask = (RemoveTask) task;
				if (removeTask.includingData()) {
					makeVuzeCall(DaemonMethod.Remove, "remove[boolean,boolean]", task.getTargetTorrent(), new String[] { "true", "true"} );
				} else {
					makeVuzeCall(DaemonMethod.Remove, "remove", task.getTargetTorrent(), new Object[] {} );
				}
				return new DaemonTaskSuccessResult(task);
				
			case Pause:

				// Pause a torrent
				makeVuzeCall(DaemonMethod.Pause, "stop", task.getTargetTorrent(), new Object[] {} );
				return new DaemonTaskSuccessResult(task);
				
			case PauseAll:

				// Resume all torrents
				makeVuzeCall(DaemonMethod.ResumeAll, "stopAllDownloads");
				return new DaemonTaskSuccessResult(task);

			case Resume:

				// Resume a torrent
				makeVuzeCall(DaemonMethod.Start, "restart", task.getTargetTorrent(), new Object[] {} );
				return new DaemonTaskSuccessResult(task);
				
			case ResumeAll:

				// Resume all torrents
				makeVuzeCall(DaemonMethod.ResumeAll, "startAllDownloads" );
				return new DaemonTaskSuccessResult(task);

			case SetFilePriorities:

				// For each of the chosen files belonging to some torrent, set the priority
				SetFilePriorityTask prioTask = (SetFilePriorityTask) task;
				// One at a time; Vuze doesn't seem to support setting the isPriority or isSkipped on (a subset of) all files at once
				for (TorrentFile forFile : prioTask.getForFiles()) {
					if (prioTask.getNewPriority() == Priority.Off) {
						makeVuzeCall(DaemonMethod.SetFilePriorities, "setSkipped[boolean]", Long.parseLong(forFile.getKey()), new String[] { "true" } );
					} else if (prioTask.getNewPriority() == Priority.High) {
						makeVuzeCall(DaemonMethod.SetFilePriorities, "setSkipped[boolean]", Long.parseLong(forFile.getKey()), new String[] { "false" } );
						makeVuzeCall(DaemonMethod.SetFilePriorities, "setPriority[boolean]", Long.parseLong(forFile.getKey()), new String[] { "true" } );
					} else {
						makeVuzeCall(DaemonMethod.SetFilePriorities, "setSkipped[boolean]", Long.parseLong(forFile.getKey()), new String[] { "false" } );
						makeVuzeCall(DaemonMethod.SetFilePriorities, "setPriority[boolean]", Long.parseLong(forFile.getKey()), new String[] { "false" } );
					}
				}
				return new DaemonTaskSuccessResult(task);
				
			case SetTransferRates:

				// Request to set the maximum transfer rates
				SetTransferRatesTask ratesTask = (SetTransferRatesTask) task;
				makeVuzeCall(DaemonMethod.SetTransferRates, "setBooleanParameter[String,boolean]", new Object[] { "Auto Upload Speed Enabled", false } );
				makeVuzeCall(DaemonMethod.SetTransferRates, "setCoreIntParameter[String,int]", new Object[] { "Max Upload Speed KBs", (ratesTask.getUploadRate() == null? 0:
						ratesTask.getUploadRate())} );
				makeVuzeCall(DaemonMethod.SetTransferRates, "setCoreIntParameter[String,int]", new Object[] { "Max Download Speed KBs", (ratesTask.getDownloadRate() == null? 0:
						ratesTask.getDownloadRate())} );
				return new DaemonTaskSuccessResult(task);
				
			default:
				return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.MethodUnsupported, task.getMethod() + " is not supported by " + getType()));
			}
		} catch (DaemonException e) {
			return new DaemonTaskFailureResult(task, e);
		} catch (IOException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.ConnectionError, e.toString()));
		}
	}

	private Map<String, Object> makeVuzeCall(DaemonMethod method, String serverMethod, Torrent actOnTorrent, Object[] params) throws DaemonException {
		return makeVuzeCall(method, serverMethod, Long.parseLong(actOnTorrent.getUniqueID()), params, actOnTorrent.getStatusCode());
	}
	
	private Map<String, Object> makeVuzeCall(DaemonMethod method, String serverMethod, Long actOnObject, Object[] params) throws DaemonException {
		return makeVuzeCall(method, serverMethod, actOnObject, params, null);
	}

	private Map<String, Object> makeVuzeCall(DaemonMethod method, String serverMethod, Object[] params) throws DaemonException {
		return makeVuzeCall(method, serverMethod, null, params, null);
	}

	private Map<String, Object> makeVuzeCall(DaemonMethod method, String serverMethod) throws DaemonException {
		return makeVuzeCall(method, serverMethod, null, new Object[] {}, null);
	}

	private synchronized Map<String, Object> makeVuzeCall(DaemonMethod method, String serverMethod, Long actOnObject, Object[] params, TorrentStatus torrentStatus) throws DaemonException {

		// TODO: It would be nicer to now split each of these steps into separate makeVuzeCalls when there are multiple logical steps such as stopping a torrent before removing it
	
		// Initialise the HTTP client
		if (rpcclient == null) {
			initialise();
		}
		if (settings.getAddress() == null || settings.getAddress().equals("")) {
			throw new DaemonException(DaemonException.ExceptionType.AuthenticationFailure, "No host name specified.");
		}

		if (savedConnectionID == null || savedPluginID == null) {
			// Get plug-in interface (for connection and plug-in object IDs)
			Map<String, Object> plugin = rpcclient.callXMLRPC(null, "getSingleton", null, null, false);
			if (!plugin.containsKey("_connection_id")) {
				throw new DaemonException(ExceptionType.UnexpectedResponse, "No connection ID returned on getSingleton request.");
			}				
			savedConnectionID = (Long) plugin.get("_connection_id");
			savedPluginID = (Long) plugin.get("_object_id");
		}

		// If no specific torrent was provided, get the download manager or plugin config to execute the method against
		long vuzeObjectID;
		if (actOnObject == null) {
			if (method == DaemonMethod.SetTransferRates) {
				
				// Execute this method against the plugin config (setParameter)
				if (savedPluginConfigID == null) {
					// Plugin config needed, but we don't know it's ID yet
					Map<String, Object> config = rpcclient.callXMLRPC(savedPluginID, "getPluginconfig", null, savedConnectionID, false);
					if (!config.containsKey("_object_id")) {
						throw new DaemonException(ExceptionType.UnexpectedResponse, "No plugin config ID returned on getPluginconfig");
					}				
					savedPluginConfigID = (Long) config.get("_object_id");
					vuzeObjectID = savedPluginConfigID;
				} else {
					// We stored the plugin config ID, so no need to ask for it again
					vuzeObjectID = savedPluginConfigID;
				}
				
			} else if (serverMethod.equals("createFromBEncodedData[byte[]]")) {
					
					// Execute this method against the torrent manager (createFromBEncodedData)
					if (savedTorrentManagerID == null) {
						// Download manager needed, but we don't know it's ID yet
						Map<String, Object> manager = rpcclient.callXMLRPC(savedPluginID, "getTorrentManager", null, savedConnectionID, false);
						if (!manager.containsKey("_object_id")) {
							throw new DaemonException(ExceptionType.UnexpectedResponse, "No torrent manager ID returned on getTorrentManager");
						}				
						savedTorrentManagerID = (Long) manager.get("_object_id");
						vuzeObjectID = savedTorrentManagerID;
					} else {
						// We stored the torrent manager ID, so no need to ask for it again
						vuzeObjectID = savedTorrentManagerID;
					}
					// And we will need the download manager as well later on (for addDownload after createFromBEncodedData)
					if (savedDownloadManagerID == null) {
						// Download manager needed, but we don't know it's ID yet
						Map<String, Object> manager = rpcclient.callXMLRPC(savedPluginID, "getDownloadManager", null, savedConnectionID, false);
						if (!manager.containsKey("_object_id")) {
							throw new DaemonException(ExceptionType.UnexpectedResponse, "No download manager ID returned on getDownloadManager");
						}
						savedDownloadManagerID = (Long) manager.get("_object_id");
					}
					
			} else {
				
				// Execute this method against download manager (addDownload, startAllDownloads, etc.)
				if (savedDownloadManagerID == null) {
					// Download manager needed, but we don't know it's ID yet
					Map<String, Object> manager = rpcclient.callXMLRPC(savedPluginID, "getDownloadManager", null, savedConnectionID, false);
					if (!manager.containsKey("_object_id")) {
						throw new DaemonException(ExceptionType.UnexpectedResponse, "No download manager ID returned on getDownloadManager");
					}
					savedDownloadManagerID = (Long) manager.get("_object_id");
					vuzeObjectID = savedDownloadManagerID;
				} else {
					// We stored the download manager ID, so no need to ask for it again
					vuzeObjectID = savedDownloadManagerID;
				}
				
			}
		} else {
			vuzeObjectID = actOnObject;
		}
		
		if (method == DaemonMethod.Remove && torrentStatus != null && torrentStatus != TorrentStatus.Paused) {
			// Vuze, for some strange reason, wants us to stop the torrent first before removing it
			rpcclient.callXMLRPC(vuzeObjectID, "stop", new Object[] {}, savedConnectionID, false);
		}
		
		boolean paramsAreVuzeObjects = false;
		if (serverMethod.equals("createFromBEncodedData[byte[]]")) {
			// Vuze does not directly add the torrent that we are uploading the file contents of
			// We first do the createFromBEncodedData call and next actually add it
			Map<String, Object> torrentData = rpcclient.callXMLRPC(vuzeObjectID, serverMethod, params, savedConnectionID, false);
			serverMethod = "addDownload[Torrent]";
			vuzeObjectID = savedDownloadManagerID;
			params = new String[] { torrentData.get("_object_id").toString() };
			paramsAreVuzeObjects = true;
		}
		
		// Call the actual method we wanted
		return rpcclient.callXMLRPC(vuzeObjectID, serverMethod, params, savedConnectionID, paramsAreVuzeObjects);
		
	}

	/**
	 * Instantiates a Vuze XML over HTTP client with proper credentials.
	 * @throws DaemonException On conflicting settings (i.e. user authentication but no password or username provided)
	 */
	private void initialise() throws DaemonException {

		this.rpcclient = new VuzeXmlOverHttpClient(settings, buildWebUIUrl());
		
	}
	
	/**
	 * Build the URL of the Vuze XML over HTTP plugin listener from the user settings.
	 * @return The URL of the RPC API
	 */
	private String buildWebUIUrl() {
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() + RPC_URL;
	}

	@SuppressWarnings("unchecked")
	private List<Torrent> onTorrentsRetrieved(Log log, Object result) throws DaemonException {
		
		Map<String, Object> response = (Map<String, Object>) result;
		
		// We might have an empty list if no torrents are on the server
		if (response == null) {
			return new ArrayList<Torrent>();
		}
		
		log.d(LOG_NAME, response.toString().length() > 300? response.toString().substring(0, 300) + "... (" + response.toString().length() + " chars)": response.toString());
		
		List<Torrent> torrents = new ArrayList<Torrent>();
		
		// Parse torrent list from Vuze response, which is a map list of ENTRYs
		for (String key : response.keySet()) {
		
			/**
			 *  Every Vuze ENTRY is a map of key-value pairs with information, or a key-map pair with that map being a mapping of key-value pairs with information
			 *  VuzeXmlTorrentListResponse.txt in the Transdroid wiki shows a full example response, but it looks something like:
			 *  ENTRY0={
					position=1, 
					torrent_file=/home/erickok/.azureus/torrents/ubuntu.torrent, 
					name=ubuntu-9.04-desktop-i386.iso, 
					torrent={
						size=732909568,  
						creation_date=1240473087
					}
				}
			 */
			Map<String, Object> info = (Map<String, Object>) response.get(key);
			if (info == null || !info.containsKey("_object_id") || info.get("_object_id") == null) {
				// No valid XML data object returned
				throw new DaemonException(DaemonException.ExceptionType.UnexpectedResponse, "Map of objects returned by Vuze, but these object do not have some <info> attached or no <_object_id> is available");
			}
			Map<String, Object> torrentinfo = (Map<String, Object>) info.get("torrent");
			Map<String, Object> statsinfo = (Map<String, Object>) info.get("stats");
			Map<String, Object> scrapeinfo = (Map<String, Object>) info.get("scrape_result");
			Map<String, Object> announceinfo = (Map<String, Object>) info.get("announce_result");
			int scrapeSeedCount = ((Long) scrapeinfo.get("seed_count")).intValue();
			int scrapeNonSeedCount = ((Long) scrapeinfo.get("non_seed_count")).intValue();
			String error = (String) info.get("error_state_details");
			error = error != null && error.equals("")? null: error;
			int announceSeedCount = ((Long) announceinfo.get("seed_count")).intValue();
			int announceNonSeedCount = ((Long) announceinfo.get("non_seed_count")).intValue();
			int rateDownload = ((Long) statsinfo.get("download_average")).intValue();
			Double availability = (Double)statsinfo.get("availability");
			Long size = torrentinfo != null? (Long)torrentinfo.get("size"): 0;
			
			torrents.add(new Torrent(
				(Long) info.get("_object_id"), // id
				info.get("_object_id").toString(), // hash	//(String) torrentinfo.get("hash"), // hash
				info.get("name").toString().trim(), // name
				convertTorrentStatus((Long) info.get("state")), // status
				statsinfo.get("target_file_or_dir") + "/", // locationDir
				rateDownload, // rateDownload
				((Long)statsinfo.get("upload_average")).intValue(), // rateUpload
				announceSeedCount, // seedersConnected
				scrapeSeedCount, // seedersKnown
				announceNonSeedCount, // leechersConnected
				scrapeNonSeedCount, // leechersKnown
				(rateDownload > 0? (int)((Long)statsinfo.get("remaining") / rateDownload): -1), // eta (bytes left / rate download, if rate > 0)
				(Long)statsinfo.get("downloaded"), // downloadedEver
				(Long)statsinfo.get("uploaded"), // uploadedEver
				size, // totalSize
				(float)((Long)statsinfo.get("downloaded")) / (float)(size), // partDone (downloadedEver / totalSize)
				Math.min(availability.floatValue(), 1f),
				null, // TODO: Implement Vuze label support
				new Date((Long) statsinfo.get("time_started")), // dateAdded
				null, // Unsupported?
				error,
				settings.getType()));
			
		}
		
		return torrents;
		
	}

	@SuppressWarnings("unchecked")
	private List<TorrentFile> onTorrentFilesRetrieved(Object result, Torrent torrent) {
		
		Map<String, Object> response = (Map<String, Object>) result;
		
		// We might have an empty list
		if (response == null) {
			return new ArrayList<TorrentFile>();
		}
		
		//DLog.d(LOG_NAME, response.toString().length() > 300? response.toString().substring(0, 300) + "... (" + response.toString().length() + " chars)": response.toString());

		List<TorrentFile> files = new ArrayList<TorrentFile>();
		
		// Parse torrent file list from Vuze response, which is a map list of ENTRYs
		for (String key : response.keySet()) {
		
			/**
			 *  Every Vuze ENTRY is a map of key-value pairs with information
			 *  For file lists, it looks something like:
			 *  ENTRY2={
				 	is_deleted=false, 
				 	length=298, 
				 	downloaded=298, 
				 	is_priority=false, 
				 	first_piece_number=726, 
				 	is_skipped=false, 
				 	file=/var/data/Downloads/Some.Torrent/OneFile.txt, 
				 	_object_id=443243294889782236, 
				 	num_pieces=1, 
				 	access_mode=1
				}
			 */
			Map<String, Object> info = (Map<String, Object>) response.get(key);
			String file = (String)info.get("file");
			
			files.add(new TorrentFile(
				String.valueOf(info.get("_object_id")),
				new File(file).getName(), // name
				(file.length() > torrent.getLocationDir().length()? file.substring(torrent.getLocationDir().length()): file), // name
				file, // fullPath
				(Long)info.get("length"), // size
				(Long)info.get("downloaded"), // downloaded
				convertVuzePriority((String)info.get("is_skipped"), (String)info.get("is_priority")))); // priority
			
		}
		
		return files;
		
	}
	
	private Priority convertVuzePriority(String isSkipped, String isPriority) {
		return isSkipped.equals("true")? Priority.Off: (isPriority.equals("true")? Priority.High: Priority.Normal);
	}

	private TorrentStatus convertTorrentStatus(Long state) {
		
		switch (state.intValue()) {
		case 2:
			return TorrentStatus.Checking;
		case 4:
			return TorrentStatus.Downloading;
		case 5:
			return TorrentStatus.Seeding;
		case 7:
			return TorrentStatus.Paused;
		case 8:
			return TorrentStatus.Error;
		}
		return TorrentStatus.Unknown;
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
