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
package org.transdroid.daemon.Ktorrent;

import com.android.internalcopy.http.multipart.FilePart;
import com.android.internalcopy.http.multipart.MultipartEntity;
import com.android.internalcopy.http.multipart.Part;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.transdroid.core.gui.log.Log;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Priority;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.task.AddByFileTask;
import org.transdroid.daemon.task.AddByMagnetUrlTask;
import org.transdroid.daemon.task.AddByUrlTask;
import org.transdroid.daemon.task.DaemonTask;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;
import org.transdroid.daemon.task.GetFileListTask;
import org.transdroid.daemon.task.GetFileListTaskSuccessResult;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetFilePriorityTask;
import org.transdroid.daemon.util.HttpHelper;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


/**
 * An adapter that allows for easy access to Ktorrent's web interface. Communication is handled via HTTP GET requests
 * and XML responses.
 * @author erickok
 */
public class KtorrentAdapter implements IDaemonAdapter {

	private static final String LOG_NAME = "Ktorrent daemon";

	private static final String RPC_URL_CHALLENGE = "/login/challenge.xml";
	private static final String RPC_URL_LOGIN = "/login?page=interface.html";
	private static final String RPC_URL_LOGIN_USER = "username";
	private static final String RPC_URL_LOGIN_PASS = "password";
	private static final String RPC_URL_LOGIN_CHAL = "challenge";
	private static final String RPC_URL_STATS = "/data/torrents.xml?L10n=no";
	private static final String RPC_URL_ACTION = "/action?";
	private static final String RPC_URL_UPLOAD = "/torrent/load?page=interface.html";
	private static final String RPC_URL_FILES = "/data/torrent/files.xml?torrent=";
	//private static final String RPC_COOKIE_NAME = "KT_SESSID";
	private static final String RPC_SUCCESS = "<result>OK</result>";
	static private int retries = 0;
	private DaemonSettings settings;
	private DefaultHttpClient httpclient;

	/**
	 * Initialises an adapter that provides operations to the Ktorrent web interface
	 */
	public KtorrentAdapter(DaemonSettings settings) {
		this.settings = settings;
	}

	/**
	 * Calculate the SHA1 hash of a password/challenge string to use with the login requests.
	 * @param passkey A concatenation of the challenge string and plain text password
	 * @return A hex-formatted SHA1-hashed string of the challenge and password strings
	 */
	public static String sha1Pass(String passkey) {
		try {
			MessageDigest m = MessageDigest.getInstance("SHA1");
			byte[] data = passkey.getBytes();
			m.update(data, 0, data.length);
			BigInteger i = new BigInteger(1, m.digest());
			return String.format("%1$040X", i).toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public DaemonTaskResult executeTask(Log log, DaemonTask task) {

		try {
			switch (task.getMethod()) {
				case Retrieve:

					// Request all torrents from server
					return new RetrieveTaskSuccessResult((RetrieveTask) task, makeStatsRequest(log), null);

				case GetFileList:

					// Request file listing for a torrent
					return new GetFileListTaskSuccessResult((GetFileListTask) task,
							makeFileListRequest(log, task.getTargetTorrent()));

				case AddByFile:

					// Add a torrent to the server by sending the contents of a local .torrent file
					String file = ((AddByFileTask) task).getFile();
					makeFileUploadRequest(log, file);
					return null;

				case AddByUrl:

					// Request to add a torrent by URL
					String url = ((AddByUrlTask) task).getUrl();
					makeActionRequest(log, "load_torrent=" + url);
					return new DaemonTaskSuccessResult(task);

				case AddByMagnetUrl:

					// Request to add a magnet link by URL
					String magnet = ((AddByMagnetUrlTask) task).getUrl();
					makeActionRequest(log, "load_torrent=" + magnet);
					return new DaemonTaskSuccessResult(task);

				case Remove:

					// Remove a torrent
					// Note that removing with data is not supported
					makeActionRequest(log, "remove=" + task.getTargetTorrent().getUniqueID());
					return new DaemonTaskSuccessResult(task);

				case Pause:

					// Pause a torrent
					makeActionRequest(log, "stop=" + task.getTargetTorrent().getUniqueID());
					return new DaemonTaskSuccessResult(task);

				case PauseAll:

					// Pause all torrents
					makeActionRequest(log, "stopall=true");
					return new DaemonTaskSuccessResult(task);

				case Resume:

					// Resume a torrent
					makeActionRequest(log, "start=" + task.getTargetTorrent().getUniqueID());
					return new DaemonTaskSuccessResult(task);

				case ResumeAll:

					// Resume all torrents
					makeActionRequest(log, "startall=true");
					return new DaemonTaskSuccessResult(task);

				case SetFilePriorities:

					// Set the priorities of the files of some torrent
					SetFilePriorityTask prioTask = (SetFilePriorityTask) task;
					String act = "file_np=" + task.getTargetTorrent().getUniqueID() + "-";
					switch (prioTask.getNewPriority()) {
						case Off:
							act = "file_stop=" + task.getTargetTorrent().getUniqueID() + "-";
							break;
						case Low:
						case Normal:
							act = "file_lp=" + task.getTargetTorrent().getUniqueID() + "-";
							break;
						case High:
							act = "file_hp=" + task.getTargetTorrent().getUniqueID() + "-";
							break;
					}
					// It seems KTorrent's web UI does not allow for setting all priorities in one request :(
					for (TorrentFile forFile : prioTask.getForFiles()) {
						makeActionRequest(log, act + forFile.getKey());
					}
					return new DaemonTaskSuccessResult(task);

				case SetTransferRates:

					// Request to set the maximum transfer rates
					// TODO: Implement this?
					return null;

				default:
					return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.MethodUnsupported,
							task.getMethod() + " is not supported by " + getType()));
			}
		} catch (LoggedOutException e) {

			// Invalidate our session
			httpclient = null;
			if (retries < 2) {
				retries++;
				// Retry
				log.d(LOG_NAME, "We were logged out without knowing: retry");
				return executeTask(log, task);
			} else {
				// Never retry more than twice; in this case just return a task failure
				return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.ConnectionError,
						"Retried " + retries + " already, so we stopped now"));
			}

		} catch (DaemonException e) {

			// Invalidate our session
			httpclient = null;
			// Return the task failure
			return new DaemonTaskFailureResult(task, e);

		}
	}

	private List<Torrent> makeStatsRequest(Log log) throws DaemonException, LoggedOutException {

		try {

			// Initialise the HTTP client
			initialise();
			makeLoginRequest(log);

			// Make request
			HttpGet httpget = new HttpGet(buildWebUIUrl() + RPC_URL_STATS);
			HttpResponse response = httpclient.execute(httpget);

			// Read XML response
			InputStream instream = response.getEntity().getContent();
			List<Torrent> torrents = StatsParser.parse(new InputStreamReader(instream), settings.getDownloadDir(),
					settings.getOS().getPathSeperator());
			instream.close();
			return torrents;

		} catch (LoggedOutException e) {
			throw e;
		} catch (DaemonException e) {
			log.d(LOG_NAME, "Parsing error: " + e.toString());
			throw e;
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	private List<TorrentFile> makeFileListRequest(Log log, Torrent torrent) throws DaemonException, LoggedOutException {

		try {

			// Initialise the HTTP client
			initialise();
			makeLoginRequest(log);

			// Make request
			HttpGet httpget = new HttpGet(buildWebUIUrl() + RPC_URL_FILES + torrent.getUniqueID());
			HttpResponse response = httpclient.execute(httpget);

			// Read XML response
			InputStream instream = response.getEntity().getContent();
			List<TorrentFile> files = FileListParser.parse(new InputStreamReader(instream), torrent.getLocationDir());
			instream.close();

			// If the files list is empty, it means that this is a single-file torrent
			// We can mimic this single file form the torrent statistics itself
			files.add(new TorrentFile("" + 0, torrent.getName(), torrent.getName(),
					torrent.getLocationDir() + torrent.getName(), torrent.getTotalSize(), torrent.getDownloadedEver(),
					Priority.Normal));

			return files;

		} catch (LoggedOutException e) {
			throw e;
		} catch (DaemonException e) {
			log.d(LOG_NAME, "Parsing error: " + e.toString());
			throw e;
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	private void makeLoginRequest(Log log) throws DaemonException {

		try {

			// Make challenge request
			HttpGet httpget = new HttpGet(buildWebUIUrl() + RPC_URL_CHALLENGE);
			HttpResponse response = httpclient.execute(httpget);
			InputStream instream = response.getEntity().getContent();
			String challengeString = HttpHelper.convertStreamToString(instream).replaceAll("<.*?>", "").trim();
			instream.close();
			// Challenge string should be something like TncpX3TB8uZ0h8eqztZ6
			if (challengeString.length() != 20) {
				throw new DaemonException(ExceptionType.UnexpectedResponse, "No (valid) challenge string received");
			}

			// Make login request
			HttpPost httppost2 = new HttpPost(buildWebUIUrl() + RPC_URL_LOGIN);
			List<NameValuePair> params = new ArrayList<NameValuePair>(3);
			params.add(new BasicNameValuePair(RPC_URL_LOGIN_USER, settings.getUsername()));
			params.add(new BasicNameValuePair(RPC_URL_LOGIN_PASS,
					"")); // Password is send (as SHA1 hex) in the challenge field
			params.add(new BasicNameValuePair(RPC_URL_LOGIN_CHAL, sha1Pass(challengeString +
					settings.getPassword()))); // Make a SHA1 encrypted hex-formated string of the challenge code and password
			httppost2.setEntity(new UrlEncodedFormEntity(params));
			// This sets the authentication cookie
			httpclient.execute(httppost2);
			/*InputStream instream2 = response2.getEntity().getContent();
			String result2 = HttpHelper.ConvertStreamToString(instream2);
			instream2.close();*/

			// Successfully logged in; we may retry later if needed
			retries = 0;

		} catch (DaemonException e) {
			log.d(LOG_NAME, "Login error: " + e.toString());
			throw e;
		} catch (Exception e) {
			log.d(LOG_NAME, "Error during login: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	private boolean makeActionRequest(Log log, String action) throws DaemonException, LoggedOutException {

		try {

			// Initialise the HTTP client
			initialise();
			makeLoginRequest(log);

			// Make request
			HttpGet httpget = new HttpGet(buildWebUIUrl() + RPC_URL_ACTION + action);
			HttpResponse response = httpclient.execute(httpget);

			// Read response (a successful action always returned '1')
			InputStream instream = response.getEntity().getContent();
			String result = HttpHelper.convertStreamToString(instream);
			instream.close();
			if (result.contains(RPC_SUCCESS)) {
				return true;
			} else if (result.contains("KTorrent  WebInterface - Login")) {
				// Apparently we were returned an HTML page instead of the expected XML
				// This happens in particular when we were logged out (because somebody else logged into KTorrent's web interface)
				throw new LoggedOutException();
			} else {
				throw new DaemonException(ExceptionType.UnexpectedResponse, "Action response was not OK but " + result);
			}

		} catch (LoggedOutException e) {
			throw e;
		} catch (DaemonException e) {
			log.d(LOG_NAME, action + " request error: " + e.toString());
			throw e;
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	private boolean makeFileUploadRequest(Log log, String target) throws DaemonException, LoggedOutException {

		try {

			// Initialise the HTTP client
			initialise();
			makeLoginRequest(log);

			// Make request
			HttpPost httppost = new HttpPost(buildWebUIUrl() + RPC_URL_UPLOAD);
			File upload = new File(URI.create(target));
			Part[] parts = {new FilePart("load_torrent", upload)};
			httppost.setEntity(new MultipartEntity(parts, httppost.getParams()));
			// Make sure we are not automatically redirected
			RedirectHandler handler = new RedirectHandler() {
				@Override
				public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
					return false;
				}

				@Override
				public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
					return null;
				}
			};
			httpclient.setRedirectHandler(handler);
			HttpResponse response = httpclient.execute(httppost);

			// Read response (a successful action always returned '1')
			InputStream instream = response.getEntity().getContent();
			String result = HttpHelper.convertStreamToString(instream);
			instream.close();
			if (result.equals("")) {
				return true;
			} else if (result.contains("KTorrent  WebInterface - Login")) {
				// Apparently we were returned an HTML page instead of the expected XML
				// This happens in particular when we were logged out (because somebody else logged into KTorrent's web interface)
				throw new LoggedOutException();
			} else {
				throw new DaemonException(ExceptionType.UnexpectedResponse, "Action response was not 1 but " + result);
			}

		} catch (LoggedOutException e) {
			throw e;
		} catch (DaemonException e) {
			log.d(LOG_NAME, "File upload error: " + e.toString());
			throw e;
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	/**
	 * Indicates if we were already successfully authenticated
	 * @return True if the proper authentication cookie was already loaded
	 */
	/*private boolean authenticated() {
		// We should have a Ktorrent cookie in the httpclient when we are authenticated
		for(Cookie cookie : httpclient.getCookieStore().getCookies()) {
			if (cookie.getName().equals(RPC_COOKIE_NAME)) {
				return true;
			}
		}
		return false;
	}*/

	/**
	 * Instantiates an HTTP client that can be used for all Ktorrent requests.
	 * @throws DaemonException Thrown on settings error
	 */
	private void initialise() throws DaemonException {

		if (httpclient != null) {
			httpclient = null;
		}
		httpclient = HttpHelper.createStandardHttpClient(settings, false);
	}

	/**
	 * Build the base URL for a Ktorrent web site request from the user settings.
	 * @return The base URL of for a request, i.e. http://localhost:8080
	 */
	private String buildWebUIUrl() {
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort();
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
