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
package org.transdroid.daemon.Rtorrent;

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
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.util.HttpHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCClient.UnauthorizdException;
import de.timroes.axmlrpc.XMLRPCException;

/**
 * An adapter that allows for easy access to rTorrent torrent data. Communication is handled via the XML-RPC protocol as
 * implemented by the aXMLRPC library.
 * @author erickok
 */
public class RtorrentAdapter implements IDaemonAdapter {

	private static final String LOG_NAME = "rTorrent daemon";

	private static final String DEFAULT_RPC_URL = "/RPC2";

	private DaemonSettings settings;
	private XMLRPCClient rpcclient;
	private List<Label> lastKnownLabels = null;

	public RtorrentAdapter(DaemonSettings settings) {
		this.settings = settings;
	}

	@Override
	public DaemonTaskResult executeTask(Log log, DaemonTask task) {

		try {
			switch (task.getMethod()) {
				case Retrieve:

					// @formatter:off
				Object result = makeRtorrentCall(log,"d.multicall2",
						new String[] { "", "main",
						"d.hash=",
						"d.name=",
						"d.state=",
						"d.down.rate=",
						"d.up.rate=",
						"d.peers_connected=",
						"d.peers_not_connected=",
						"d.peers_accounted=",
						"d.bytes_done=",
						"d.up.total=",
						"d.size_bytes=",
						"d.creation_date=",
						"d.left_bytes=",
						"d.complete=",
						"d.is_active=", 
						"d.is_hash_checking=", 
						"d.base_path=",
						"d.base_filename=",
						"d.message=",
						"d.custom=addtime",
						"d.custom=seedingtime",
						"d.custom1=",
						"d.peers_complete=",
						"d.peers_accounted=" });
				// @formatter:on
					return new RetrieveTaskSuccessResult((RetrieveTask) task, onTorrentsRetrieved(result),
							lastKnownLabels);

				case GetTorrentDetails:

					// @formatter:off
				Object dresult = makeRtorrentCall(log,"t.multicall", new String[] {
						task.getTargetTorrent().getUniqueID(),
						"", 
						"t.url=" });
				// @formatter:on
					return new GetTorrentDetailsTaskSuccessResult((GetTorrentDetailsTask) task,
							onTorrentDetailsRetrieved(log, dresult));

				case GetFileList:

					// @formatter:off
				Object fresult = makeRtorrentCall(log,"f.multicall", new String[] {
						task.getTargetTorrent().getUniqueID(),
						"", 
						"f.path=",
						"f.size_bytes=",
						"f.priority=",
						"f.completed_chunks=",
						"f.size_chunks=",
						"f.priority=",
						"f.frozen_path=" });
				// @formatter:on
					return new GetFileListTaskSuccessResult((GetFileListTask) task,
							onTorrentFilesRetrieved(fresult, task.getTargetTorrent()));

				case AddByFile:

					// Request to add a torrent by local .torrent file
					File file = new File(URI.create(((AddByFileTask) task).getFile()));
					FileInputStream in = new FileInputStream(file);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					byte[] buffer = new byte[(int) file.length()];
					int read;
					while ((read = in.read(buffer, 0, buffer.length)) > 0) {
						baos.write(buffer, 0, read);
					}
					byte[] bytes = baos.toByteArray();
					int size = (int) file.length() * 2;
					final int XMLRPC_EXTRA_PADDING = 1280;
					makeRtorrentCall(log, "network.xmlrpc.size_limit.set", new Object[]{size + XMLRPC_EXTRA_PADDING});
					makeRtorrentCall(log, "load.raw_start", new Object[]{bytes});
					return new DaemonTaskSuccessResult(task);

				case AddByUrl:

					// Request to add a torrent by URL
					String url = ((AddByUrlTask) task).getUrl();
					makeRtorrentCall(log, "load.start", new String[]{"",url});
					return new DaemonTaskSuccessResult(task);

				case AddByMagnetUrl:

					// Request to add a magnet link by URL
					String magnet = ((AddByMagnetUrlTask) task).getUrl();
					makeRtorrentCall(log, "load.start", new String[]{"",magnet});
					return new DaemonTaskSuccessResult(task);

				case Remove:

					// Remove a torrent
					RemoveTask removeTask = (RemoveTask) task;
					if (removeTask.includingData()) {
						makeRtorrentCall(log, "d.custom5.set",
								new String[]{task.getTargetTorrent().getUniqueID(), "1"});
					}
					makeRtorrentCall(log, "d.erase", new String[]{task.getTargetTorrent().getUniqueID()});
					return new DaemonTaskSuccessResult(task);

				case Pause:

					// Pause a torrent
					makeRtorrentCall(log, "d.pause", new String[]{task.getTargetTorrent().getUniqueID()});
					return new DaemonTaskSuccessResult(task);

				case PauseAll:

					// Resume all torrents
					makeRtorrentCall(log, "d.multicall2", new String[]{"","main", "d.pause="});
					return new DaemonTaskSuccessResult(task);

				case Resume:

					// Resume a torrent
					makeRtorrentCall(log, "d.resume", new String[]{task.getTargetTorrent().getUniqueID()});
					return new DaemonTaskSuccessResult(task);

				case ResumeAll:

					// Resume all torrents
					makeRtorrentCall(log, "d.multicall2", new String[]{"", "main", "d.resume="});
					return new DaemonTaskSuccessResult(task);

				case Stop:

					// Stop a torrent
					makeRtorrentCall(log, "d.stop", new String[]{task.getTargetTorrent().getUniqueID()});
					return new DaemonTaskSuccessResult(task);

				case StopAll:

					// Stop all torrents
					makeRtorrentCall(log, "d.multicall2", new String[]{"", "main", "d.stop="});
					return new DaemonTaskSuccessResult(task);

				case Start:

					// Start a torrent
					makeRtorrentCall(log, "d.start", new String[]{task.getTargetTorrent().getUniqueID()});
					return new DaemonTaskSuccessResult(task);

				case StartAll:

					// Start all torrents
					makeRtorrentCall(log, "d.multicall2", new String[]{"", "main", "d.start="});
					return new DaemonTaskSuccessResult(task);

				case SetFilePriorities:

					// For each of the chosen files belonging to some torrent, set the priority
					SetFilePriorityTask prioTask = (SetFilePriorityTask) task;
					String newPriority = "" + convertPriority(prioTask.getNewPriority());
					// One at a time; rTorrent doesn't seem to support a multicall on a selective number of files
					for (TorrentFile forFile : prioTask.getForFiles()) {
						makeRtorrentCall(log, "f.priority.set",
								new String[]{task.getTargetTorrent().getUniqueID() + ":f" + forFile.getKey(),
										newPriority});
					}
					return new DaemonTaskSuccessResult(task);

				case SetTransferRates:

					// Request to set the maximum transfer rates
					SetTransferRatesTask ratesTask = (SetTransferRatesTask) task;
					makeRtorrentCall(log, "throttle.global_down.max_rate.set", new String[]{"",(ratesTask.getDownloadRate() == null ? "0" :
							ratesTask.getDownloadRate().toString() + "k")});
					makeRtorrentCall(log, "throttle.global_up.max_rate.set", new String[]{"",
							(ratesTask.getUploadRate() == null ? "0" : ratesTask.getUploadRate().toString() + "k")});
					return new DaemonTaskSuccessResult(task);

				case SetLabel:

					SetLabelTask labelTask = (SetLabelTask) task;
					makeRtorrentCall(log, "d.custom1.set",
							new String[]{task.getTargetTorrent().getUniqueID(), labelTask.getNewLabel()});
					return new DaemonTaskSuccessResult(task);

				case ForceRecheck:

					// Force re-check of data of a torrent
					makeRtorrentCall(log, "d.check_hash", new String[]{task.getTargetTorrent().getUniqueID()});
					return new DaemonTaskSuccessResult(task);

				default:
					return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.MethodUnsupported,
							task.getMethod() + " is not supported by " + getType()));
			}
		} catch (DaemonException e) {
			return new DaemonTaskFailureResult(task, e);
		} catch (FileNotFoundException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.FileAccessError, e.toString()));
		} catch (IOException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.ConnectionError, e.toString()));
		}
	}

	private Object makeRtorrentCall(Log log, String serverMethod, Object[] arguments)
			throws DaemonException, MalformedURLException {

		// Initialise the HTTP client
		if (rpcclient == null) {
			initialise();
		}

		String params = "";
		for (Object arg : arguments) {
			params += " " + arg.toString();
		}
		try {
			log.d(LOG_NAME, "Calling " + serverMethod + " with params [" +
							(params.length() > 100 ? params.substring(0, 100) + "..." : params) + " ]");
			return rpcclient.call(serverMethod, arguments);
		} catch (XMLRPCException e) {
			log.d(LOG_NAME, e.toString());
			if (e.getCause() instanceof UnauthorizdException) {
				throw new DaemonException(ExceptionType.AuthenticationFailure, e.toString());
			}
			if (e.getCause() instanceof DaemonException) {
				throw (DaemonException) e.getCause();
			}
			throw new DaemonException(ExceptionType.ConnectionError,
					"Error making call to " + serverMethod + " with params [" +
							(params.length() > 100 ? params.substring(0, 100) + "..." : params) + " ]: " +
							e.toString());
		}

	}

	/**
	 * Instantiates a XML-RPC client with proper credentials.
	 * @throws DaemonException On conflicting settings (i.e. user authentication but no password or username provided)
	 * @throws MalformedURLException Thrown when the URL could not be properly constructed
	 */
	private void initialise() throws DaemonException, MalformedURLException {

		int flags = XMLRPCClient.FLAGS_8BYTE_INT;
		this.rpcclient = new XMLRPCClient(HttpHelper.createStandardHttpClient(settings, true), buildWebUIUrl(), flags);

	}

	/**
	 * Build the URL of rTorrent's XML-RPC location from the user settings.
	 * @return The URL of the RPC API
	 */
	private String buildWebUIUrl() {
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() +
				(settings.getFolder() == null || settings.getFolder().equals("") ? DEFAULT_RPC_URL :
						settings.getFolder());
	}

	private List<Torrent> onTorrentsRetrieved(Object response) throws DaemonException {

		if (response == null || !(response instanceof Object[])) {

			throw new DaemonException(ExceptionType.ParsingFailed,
					"Response on retrieveing torrents did not return a list of objects");

		} else {

			// Parse torrent list from response
			// Formatted as Object[][], see http://libtorrent.rakshasa.no/wiki/RTorrentCommands#Download
			List<Torrent> torrents = new ArrayList<Torrent>();
			Map<String, Integer> labels = new HashMap<String, Integer>();
			Object[] responseList = (Object[]) response;
			for (int i = 0; i < responseList.length; i++) {

				Object[] info = (Object[]) responseList[i];
				String error = (String) info[18];
				error = error.equals("") ? null : error;

				// Determine the time added
				Date added;
				Long addtime = null;
				try {
					addtime = Long.valueOf(((String) info[19]).trim());
				} catch (NumberFormatException e) {
					// Not a number (timestamp); ignore and fall back to using creationtime
				}
				if (addtime != null)
				// Successfully received the addtime from rTorrent (which is a String like '1337089336\n')
				{
					added = new Date(addtime * 1000L);
				} else {
					// rTorrent didn't have the addtime (missing plugin?): base it on creationtime instead
					if (info[11] instanceof Long) {
						added = new Date((Long) info[11] * 1000L);
					} else {
						added = new Date((Integer) info[11] * 1000L);
					}
				}

				// Determine the seeding time
				Date finished = null;
				Long seedingtime = null;
				try {
					seedingtime = Long.valueOf(((String) info[20]).trim());
				} catch (NumberFormatException e) {
					// Not a number (timestamp); ignore and fall back to using creationtime
				}
				if (seedingtime != null)
				// Successfully received the seedingtime from rTorrent (which is a String like '1337089336\n')
				{
					finished = new Date(seedingtime * 1000L);
				}

				// Determine the label
				String label = null;
				try {
					label = URLDecoder.decode((String) info[21], "UTF-8");
					if (labels.containsKey(label)) {
						labels.put(label, labels.get(label) + 1);
					} else {
						labels.put(label, 0);
					}
				} catch (UnsupportedEncodingException e) {
					// Can't decode label name; ignore it
				}

				if (info[3] instanceof Long) {

					// rTorrent uses the i8 dialect which returns 64-bit integers
					long rateDownload = (Long) info[3];
					String basePath = (String) info[16];

					// @formatter:off
					torrents.add(new Torrent(
						i,
						(String)info[0], // hash
						(String)info[1], // name
						convertTorrentStatus((Long)info[2], (Long)info[13], (Long)info[14], (Long)info[15]), // status
						basePath.substring(0, basePath.indexOf((String)info[17])), // locationDir
						((Long)info[3]).intValue(), // rateDownload
						((Long)info[4]).intValue(), // rateUpload
						((Long)info[22]).intValue(), // seedersConnected
						((Long)info[5]).intValue() + ((Long)info[6]).intValue(), // seedersKnown
						((Long)info[23]).intValue(), // leechersConnected
						((Long)info[5]).intValue() + ((Long)info[6]).intValue(), // leechersKnown
						(rateDownload > 0? (int) (((Long)info[12]) / rateDownload): -1), // eta (bytes left / rate download, if rate > 0)
						(Long)info[8], // downloadedEver
						(Long)info[9], // uploadedEver
						(Long)info[10], // totalSize
						((Long)info[8]).floatValue() / ((Long)info[10]).floatValue(), // partDone
						0f, // TODO: Add availability data
						label,
						added,
						finished,
						error,
						settings.getType()));
					// @formatter:on

				} else {

					// rTorrent uses the default dialect with 32-bit integers
					int rateDownload = (Integer) info[3];
					String basePath = (String) info[16];

					// @formatter:off
					torrents.add(new Torrent(
						i,
						(String)info[0], // hash
						(String)info[1], // name
						convertTorrentStatus(((Integer)info[2]).longValue(), ((Integer)info[13]).longValue(), ((Integer)info[14]).longValue(), ((Integer)info[15]).longValue()), // status
						basePath.substring(0, basePath.indexOf((String)info[17])), // locationDir
						rateDownload, // rateDownload
						(Integer)info[4], // rateUpload
						(Integer)info[22], // seedersConnected
						(Integer)info[5] + (Integer)info[6], // seedersKnown
						(Integer)info[23], // leechersConnected
						(Integer)info[5] + (Integer)info[6], // leechersKnown
						(rateDownload > 0? (Integer)info[12] / rateDownload: -1), // eta (bytes left / rate download, if rate > 0)
						(Integer)info[8], // downloadedEver
						(Integer)info[9], // uploadedEver
						(Integer)info[10], // totalSize
						((Integer)info[8]).floatValue() / ((Integer)info[10]).floatValue(), // partDone
						0f, // TODO: Add availability data
						label,
						added,
						finished,
						error,
						settings.getType()));
					// @formatter:on

				}
			}
			lastKnownLabels = new ArrayList<Label>();
			for (Entry<String, Integer> pair : labels.entrySet()) {
				if (pair.getKey() != null) {
					lastKnownLabels.add(new Label(pair.getKey(), pair.getValue()));
				}
			}
			return torrents;

		}

	}

	private List<TorrentFile> onTorrentFilesRetrieved(Object response, Torrent torrent) throws DaemonException {

		if (response == null || !(response instanceof Object[])) {

			throw new DaemonException(ExceptionType.ParsingFailed,
					"Response on retrieveing torrent files did not return a list of objects");

		} else {

			// Parse torrent files from response
			// Formatted as Object[][], see http://libtorrent.rakshasa.no/wiki/RTorrentCommands#Download
			List<TorrentFile> files = new ArrayList<TorrentFile>();
			Object[] responseList = (Object[]) response;
			for (int i = 0; i < responseList.length; i++) {

				Object[] info = (Object[]) responseList[i];
				if (info[1] instanceof Long) {

					// rTorrent uses the i8 dialect which returns 64-bit integers
					Long size = (Long) info[1];
					Long chunksDone = (Long) info[3];
					Long chunksTotal = (Long) info[4];
					Long priority = (Long) info[5];

					// @formatter:off
					files.add(new TorrentFile(
						"" + i,
						(String)info[0], // name
						((String)info[6]).substring(torrent.getLocationDir().length()), // relativePath (= fullPath - torrent locationDir)
						(String)info[6], // fullPath
						size, // size
						(long) (size * ((float)chunksDone / (float)chunksTotal)), // done
						convertRtorrentPriority(priority.intValue()))); // priority
					// @formatter:on

				} else {

					// rTorrent uses the default dialect with 32-bit integers
					Integer size = (Integer) info[1];
					Integer chunksDone = (Integer) info[3];
					Integer chunksTotal = (Integer) info[4];
					Integer priority = (Integer) info[5];

					// @formatter:off
					files.add(new TorrentFile(
						"" + i,
						(String)info[0], // name
						((String)info[6]).substring(torrent.getLocationDir().length()), // relativePath (= fullPath - torrent locationDir)
						(String)info[0], // fullPath
						size, // size
						(int) (size * ((float)chunksDone / (float)chunksTotal)), // done
					convertRtorrentPriority(priority))); // priority
					// @formatter:on

				}
			}
			return files;

		}

	}

	private Priority convertRtorrentPriority(int code) {
		// Note that Rtorrent has no low priority value
		switch (code) {
			case 0:
				return Priority.Off;
			case 2:
				return Priority.High;
			default:
				return Priority.Normal;
		}
	}

	private int convertPriority(Priority priority) {
		// Note that Rtorrent has no low priority value
		switch (priority) {
			case Off:
				return 0;
			case High:
				return 2;
			default:
				return 1;
		}
	}

	private TorrentStatus convertTorrentStatus(Long state, Long complete, Long active, Long checking) {
		if (state == 0) {
			return TorrentStatus.Queued;
		} else if (active == 1) {
			if (complete == 1) {
				return TorrentStatus.Seeding;
			} else {
				return TorrentStatus.Downloading;
			}
		} else if (checking == 1) {
			return TorrentStatus.Checking;
		} else {
			return TorrentStatus.Paused;
		}
	}

	private TorrentDetails onTorrentDetailsRetrieved(Log log, Object response) throws DaemonException {

		if (response == null || !(response instanceof Object[])) {

			throw new DaemonException(ExceptionType.ParsingFailed,
					"Response on retrieveing trackers did not return a list of objects");

		} else {

			// Parse a torrent's trackers from response
			// Formatted as Object[][], see http://libtorrent.rakshasa.no/wiki/RTorrentCommands#Download
			List<String> trackers = new ArrayList<String>();
			Object[] responseList = (Object[]) response;
			try {
				for (Object aResponseList : responseList) {
					Object[] info = (Object[]) aResponseList;
					trackers.add((String) info[0]);
				}
			} catch (Exception e) {
				log.e(LOG_NAME, e.toString());
			}
			return new TorrentDetails(trackers, null);

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
