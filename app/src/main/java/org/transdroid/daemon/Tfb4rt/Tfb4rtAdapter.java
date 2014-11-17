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
package org.transdroid.daemon.Tfb4rt;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.transdroid.core.gui.log.Log;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.task.AddByFileTask;
import org.transdroid.daemon.task.AddByUrlTask;
import org.transdroid.daemon.task.DaemonTask;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.util.HttpHelper;
import com.android.internalcopy.http.multipart.FilePart;
import com.android.internalcopy.http.multipart.MultipartEntity;
import com.android.internalcopy.http.multipart.Part;

/**
 * An adapter that allows for easy access to Torrentflux-b4rt installs. Communication is handled via HTTP GET requests
 * and XML responses.
 * @author erickok
 */
public class Tfb4rtAdapter implements IDaemonAdapter {

	private static final String LOG_NAME = "Torrentflux-b4rt daemon";

	private static final String RPC_URL_STATS = "/stats.php?t=all&f=xml";
	private static final String RPC_URL_DISPATCH = "/dispatcher.php?action=";
	private static final String RPC_URL_DISPATCH2 = "&riid=_exit_";
	private static final String RPC_URL_TRANSFER = "&transfer=";
	private static final String RPC_URL_AID = "&aid=2";
	private static final String RPC_URL_URL = "&url=";
	private static final String RPC_URL_USER = "&username=";
	private static final String RPC_URL_PASS = "&md5pass=";

	private DaemonSettings settings;
	private DefaultHttpClient httpclient;

	/**
	 * Initialises an adapter that provides operations to the Torrentflux-b4rt pages
	 */
	public Tfb4rtAdapter(DaemonSettings settings) {
		this.settings = settings;
	}

	@Override
	public DaemonTaskResult executeTask(Log log, DaemonTask task) {

		try {
			switch (task.getMethod()) {
			case Retrieve:

				// Request all torrents from server
				return new RetrieveTaskSuccessResult((RetrieveTask) task, makeStatsRequest(log), null);

			case AddByFile:

				// Add a torrent to the server by sending the contents of a local .torrent file
				String file = ((AddByFileTask) task).getFile();
				makeFileUploadRequest(log, "fileUpload", file);
				return null;

			case AddByUrl:

				// Request to add a torrent by URL
				String url = ((AddByUrlTask) task).getUrl();
				makeActionRequest(log, "urlUpload", url);
				return new DaemonTaskSuccessResult(task);

			case Remove:

				// Remove a torrent
				RemoveTask removeTask = (RemoveTask) task;
				makeActionRequest(log, (removeTask.includingData() ? "deleteWithData" : "delete"), task.getTargetTorrent()
						.getUniqueID());
				return new DaemonTaskSuccessResult(task);

			case Pause:

				// Pause a torrent
				makeActionRequest(log, "stop", task.getTargetTorrent().getUniqueID());
				return new DaemonTaskSuccessResult(task);

			case PauseAll:

				// Pause all torrents
				makeActionRequest(log, "bulkStop", null);
				return new DaemonTaskSuccessResult(task);

			case Resume:

				// Resume a torrent
				makeActionRequest(log, "start", task.getTargetTorrent().getUniqueID());
				return new DaemonTaskSuccessResult(task);

			case ResumeAll:

				// Resume all torrents
				makeActionRequest(log, "bulkStart", null);
				return new DaemonTaskSuccessResult(task);

			case SetTransferRates:

				// Request to set the maximum transfer rates
				// TODO: Implement this?
				return null;

			default:
				return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.MethodUnsupported,
						task.getMethod() + " is not supported by " + getType()));
			}
		} catch (DaemonException e) {
			return new DaemonTaskFailureResult(task, e);
		}
	}

	private List<Torrent> makeStatsRequest(Log log) throws DaemonException {

		try {

			// Initialise the HTTP client
			if (httpclient == null) {
				initialise();
			}

			// Make request
			HttpGet httpget = new HttpGet(buildWebUIUrl(RPC_URL_STATS));
			HttpResponse response = httpclient.execute(httpget);

			// Read XML response
			InputStream instream = response.getEntity().getContent();
			List<Torrent> torrents = StatsParser.parse(new InputStreamReader(instream));
			instream.close();
			return torrents;

		} catch (DaemonException e) {
			log.d(LOG_NAME, "Parsing error: " + e.toString());
			throw e;
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	private boolean makeActionRequest(Log log, String action, String target) throws DaemonException {

		try {

			// Initialise the HTTP client
			if (httpclient == null) {
				initialise();
			}

			// Make request
			HttpGet httpget = new HttpGet(buildWebUIUrl(RPC_URL_DISPATCH + action + RPC_URL_DISPATCH2 + RPC_URL_AID
					+ (action.equals("urlUpload") ? RPC_URL_URL : RPC_URL_TRANSFER) + target));
			HttpResponse response = httpclient.execute(httpget);

			// Read response (a successful action always returned '1')
			InputStream instream = response.getEntity().getContent();
			String result = HttpHelper.convertStreamToString(instream);
			instream.close();
			if (result.trim().equals("1")) {
				return true;
			} else {
				throw new DaemonException(ExceptionType.UnexpectedResponse, "Action response was not 1 but " + result);
			}

		} catch (DaemonException e) {
			log.d(LOG_NAME, action + " request error: " + e.toString());
			throw e;
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	private boolean makeFileUploadRequest(Log log, String action, String target) throws DaemonException {

		try {

			// Initialise the HTTP client
			if (httpclient == null) {
				initialise();
			}

			// Make request
			HttpPost httppost = new HttpPost(buildWebUIUrl(RPC_URL_DISPATCH + action + RPC_URL_DISPATCH2 + RPC_URL_AID));

			File upload = new File(URI.create(target));
			Part[] parts = { new FilePart("upload_files[]", upload) };
			httppost.setEntity(new MultipartEntity(parts, httppost.getParams()));
			HttpResponse response = httpclient.execute(httppost);

			// Read response (a successful action always returned '1')
			InputStream instream = response.getEntity().getContent();
			String result = HttpHelper.convertStreamToString(instream);
			instream.close();
			if (result.equals("1")) {
				return true;
			} else {
				throw new DaemonException(ExceptionType.UnexpectedResponse, "Action response was not 1 but " + result);
			}

		} catch (DaemonException e) {
			log.d(LOG_NAME, action + " request error: " + e.toString());
			throw e;
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	/**
	 * Instantiates an HTTP client that can be used for all Torrentflux-b4rt requests.
	 * @throws DaemonException On conflicting or missing settings
	 */
	private void initialise() throws DaemonException {
		httpclient = HttpHelper.createStandardHttpClient(settings, true);
	}

	/**
	 * Build the URL of specific Torrentflux site request from the user settings and some requested action.
	 * @param act The action to perform, which is an already build query string without usernmae/password, i.e.
	 *            dispatcher.php?action=stop&transfer=ubuntu.torrent
	 * @return The URL of a specific request, i.e.
	 *         http://localhost:80/turrentflux/dispatcher.php?action=stop&transfer=ubuntu
	 *         .torrent&username=admin&md5pass=asd98as7d
	 */
	private String buildWebUIUrl(String act) {
		String folder = "";
		if (settings.getFolder() != null) {
			folder = settings.getFolder();
			if (folder.endsWith("/"))
				folder = folder.substring(0, folder.length() - 1);
		}
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() + folder
				+ act + RPC_URL_USER + settings.getUsername() + RPC_URL_PASS
				+ md5Pass((settings.getPassword() == null ? "" : settings.getPassword()));
	}

	/**
	 * Calculate the MD5 hash of a password to use with the Torrentflux-b4rt dispatcher requests.
	 * @param pass The plain text password
	 * @return A hex-formatted MD5-hashed string of the password
	 */
	public static String md5Pass(String pass) {
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			byte[] data = pass.getBytes();
			m.update(data, 0, data.length);
			BigInteger i = new BigInteger(1, m.digest());
			return String.format("%1$032X", i);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
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
