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
package org.transdroid.daemon.BitComet;

import com.android.internalcopy.http.multipart.BitCometFilePart;
import com.android.internalcopy.http.multipart.MultipartEntity;
import com.android.internalcopy.http.multipart.Part;
import com.android.internalcopy.http.multipart.Utf8StringPart;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
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
import org.transdroid.daemon.task.AddByMagnetUrlTask;
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
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.util.HttpHelper;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * The daemon adapter for the BitComet torrent client.
 * @author SeNS (sensboston)
 *         <p/>
 *         09/26/2012: added AJAX support for BitComet v.1.34 and up : added additional tasks support
 */
public class BitCometAdapter implements IDaemonAdapter {

	private static final String LOG_NAME = "BitComet daemon";

	private DaemonSettings settings;
	private DefaultHttpClient httpclient;

	public BitCometAdapter(DaemonSettings settings) {
		this.settings = settings;
	}

	/**
	 * Returns the size of the torrent, as parsed form some string
	 * @param size The size in a string format, i.e. '691 MB'
	 * @return The size in bytes
	 */
	private static long convertSize(String size) {
		try {
			if (size.endsWith("GB")) {
				return (long) (Float.parseFloat(size.substring(0, size.indexOf("GB"))) * 1024 * 1024 * 1024);
			} else if (size.endsWith("MB")) {
				return (long) (Float.parseFloat(size.substring(0, size.indexOf("MB"))) * 1024 * 1024);
			} else if (size.endsWith("kB")) {
				return (long) (Float.parseFloat(size.substring(0, size.indexOf("kB"))) * 1024);
			} else if (size.endsWith("B")) {
				return (long) (Float.parseFloat(size.substring(0, size.indexOf("B"))));
			}
		} catch (Exception e) {
		}
		return 0;
	}

	/**
	 * Returns the part done (or progress) of a torrent, as parsed from some string
	 * @param progress The part done in a string format, i.e. '15.96'
	 * @return The part done as [0..1] fraction, i.e. 0.1596
	 */
	public static float convertProgress(String progress) {
		return Float.parseFloat(progress) / 1000.0f;
	}

	@Override
	public DaemonTaskResult executeTask(Log log, DaemonTask task) {

		try {
			switch (task.getMethod()) {
				case Retrieve:

					// Request all torrents from server
					// first, check client for the new AJAX interface (BitComet v.1.34 and up)
					try {
						String xmlResult = makeRequest(log, "/panel/task_list_xml");
						if (xmlResult.startsWith("<?xml", 0)) {
							return new RetrieveTaskSuccessResult((RetrieveTask) task, parseXmlTorrents(xmlResult),
									null);
						}
					} catch (Exception e) {
						// it's probably an old client, parse HTML instead
						String htmlResult = makeRequest(log, "/panel/task_list");
						return new RetrieveTaskSuccessResult((RetrieveTask) task, parseHttpTorrents(log, htmlResult),
								null);
					}

				case GetFileList:

					// Request files listing for a specific torrent
					String fhash = task.getTargetTorrent().getUniqueID();
					String fileListResult = makeRequest(log, "/panel/task_detail", new BasicNameValuePair("id", fhash),
							new BasicNameValuePair("show", "files"));
					return new GetFileListTaskSuccessResult((GetFileListTask) task,
							parseHttpTorrentFiles(fileListResult, fhash));

				case AddByFile:

					// Upload a local .torrent file
					String ufile = ((AddByFileTask) task).getFile();
					makeFileUploadRequest(log, "/panel/task_add_bt_result", ufile);
					return new DaemonTaskSuccessResult(task);

				case AddByUrl:

					// Request to add a torrent by URL
					String url = ((AddByUrlTask) task).getUrl();
					makeUploadUrlRequest(log, "/panel/task_add_httpftp_result", url);
					return new DaemonTaskSuccessResult(task);

				case AddByMagnetUrl:

					// Request to add a torrent by URL
					String magnetUrl = ((AddByMagnetUrlTask) task).getUrl();
					makeUploadUrlRequest(log, "/panel/task_add_httpftp_result", magnetUrl);
					return new DaemonTaskSuccessResult(task);

				case Remove:

					// Remove a torrent
					RemoveTask removeTask = (RemoveTask) task;
					makeRequest(log, "/panel/task_delete",
							new BasicNameValuePair("id", removeTask.getTargetTorrent().getUniqueID()),
							new BasicNameValuePair("action",
									(removeTask.includingData() ? "delete_all" : "delete_task")));
					return new DaemonTaskSuccessResult(task);

				case Pause:

					// Pause a torrent
					makeRequest(log, "/panel/task_action",
							new BasicNameValuePair("id", task.getTargetTorrent().getUniqueID()),
							new BasicNameValuePair("action", "stop"));
					return new DaemonTaskSuccessResult(task);

				case Resume:

					// Resume a torrent
					makeRequest(log, "/panel/task_action",
							new BasicNameValuePair("id", task.getTargetTorrent().getUniqueID()),
							new BasicNameValuePair("action", "start"));
					return new DaemonTaskSuccessResult(task);

				case PauseAll:

					// Suspend (pause) all active torrents
					makeRequest(log, "/panel/tasklist_action", new BasicNameValuePair("id", "suspend_all"));
					return new DaemonTaskSuccessResult(task);

				case ResumeAll:

					// Resume suspended torrents
					makeRequest(log, "/panel/tasklist_action", new BasicNameValuePair("id", "resume_all"));
					return new DaemonTaskSuccessResult(task);

				case StopAll:

					// Stop all torrents
					makeRequest(log, "/panel/tasklist_action", new BasicNameValuePair("id", "stop_all"));
					return new DaemonTaskSuccessResult(task);

				case StartAll:

					// Start all torrents for download and seeding
					makeRequest(log, "/panel/tasklist_action", new BasicNameValuePair("id", "start_all_download"));
					makeRequest(log, "/panel/tasklist_action", new BasicNameValuePair("id", "start_all_seeding"));

					return new DaemonTaskSuccessResult(task);

				case SetTransferRates:

					// Request to set the maximum transfer rates
					SetTransferRatesTask ratesTask = (SetTransferRatesTask) task;
					String dl =
							Integer.toString((ratesTask.getDownloadRate() == null ? -1 : ratesTask.getDownloadRate()));
					String ul = Integer.toString((ratesTask.getUploadRate() == null ? -1 : ratesTask.getUploadRate()));
					makeRequest(log, "/panel/option_set", new BasicNameValuePair("key", "down_rate_max"),
							new BasicNameValuePair("value", dl));
					makeRequest(log, "/panel/option_set", new BasicNameValuePair("key", "up_rate_max"),
							new BasicNameValuePair("value", ul));
					return new DaemonTaskSuccessResult(task);

				default:
					return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.MethodUnsupported,
							task.getMethod() + " is not supported by " + getType()));
			}
		} catch (DaemonException e) {
			return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.ParsingFailed, e.toString()));
		}
	}

	/**
	 * Instantiates an HTTP client with proper credentials that can be used for all Buffalo NAS requests.
	 * @throws DaemonException On conflicting or missing settings
	 */
	private void initialise() throws DaemonException {
		httpclient = HttpHelper.createStandardHttpClient(settings, true);
	}

	/**
	 * Build the URL of the HTTP request from the user settings
	 * @return The URL to request
	 */
	private String buildWebUIUrl(String path) {
		return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() + path;
	}

	private String makeRequest(Log log, String url, NameValuePair... params) throws DaemonException {

		try {

			// Initialize the HTTP client
			if (httpclient == null) {
				initialise();
			}

			// Add the parameters to the query string
			boolean first = true;
			for (NameValuePair param : params) {
				if (first) {
					url += "?";
					first = false;
				} else {
					url += "&";
				}
				url += param.getName() + "=" + param.getValue();
			}

			// Make the request
			HttpResponse response = httpclient.execute(new HttpGet(buildWebUIUrl(url)));
			HttpEntity entity = response.getEntity();
			if (entity != null) {

				// Read HTTP response
				java.io.InputStream instream = entity.getContent();
				String result = HttpHelper.convertStreamToString(instream);
				instream.close();

				// Return raw result
				return result;
			}

			log.d(LOG_NAME, "Error: No entity in HTTP response");
			throw new DaemonException(ExceptionType.UnexpectedResponse, "No HTTP entity object in response.");

		} catch (UnsupportedEncodingException e) {
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}

	}

	private boolean makeFileUploadRequest(Log log, String path, String file) throws DaemonException {

		try {

			// Initialize the HTTP client
			if (httpclient == null) {
				initialise();
			}

			// Get default download file location first
			HttpResponse response = httpclient.execute(new HttpGet(buildWebUIUrl("/panel/task_add_bt")));
			HttpEntity entity = response.getEntity();
			if (entity != null) {

				// Read BitComet response
				java.io.InputStream instream = entity.getContent();
				String result = HttpHelper.convertStreamToString(instream);
				instream.close();

				int idx = result.indexOf("save_path' value='") + 18;
				String defaultPath = result.substring(idx, result.indexOf("'>", idx));

				// Setup request using POST
				HttpPost httppost = new HttpPost(buildWebUIUrl(path));
				File upload = new File(URI.create(file));
				Part[] parts =
						{new BitCometFilePart("torrent_file", upload), new Utf8StringPart("save_path", defaultPath)};
				httppost.setEntity(new MultipartEntity(parts, httppost.getParams()));

				// Make the request
				response = httpclient.execute(httppost);

				entity = response.getEntity();
				if (entity != null) {
					// Check BitComet response
					instream = entity.getContent();
					result = HttpHelper.convertStreamToString(instream);
					instream.close();
					if (result.indexOf("failed!") > 0) {
						throw new Exception("Adding torrent file failed");
					}
				}

				return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
			}
			return false;

		} catch (FileNotFoundException e) {
			throw new DaemonException(ExceptionType.FileAccessError, e.toString());
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}
	}

	private boolean makeUploadUrlRequest(Log log, String path, String url) throws DaemonException {

		try {

			// Initialize the HTTP client
			if (httpclient == null) {
				initialise();
			}

			// Get default download file location first
			HttpResponse response = httpclient.execute(new HttpGet(buildWebUIUrl("/panel/task_add_httpftp")));
			HttpEntity entity = response.getEntity();
			if (entity != null) {

				// Read BitComet response
				java.io.InputStream instream = entity.getContent();
				String result = HttpHelper.convertStreamToString(instream);
				instream.close();

				int idx = result.indexOf("save_path' value='") + 18;
				String defaultPath = result.substring(idx, result.indexOf("'>", idx));

				// Setup form fields and post request
				HttpPost httppost = new HttpPost(buildWebUIUrl(path));

				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("url", url));
				params.add(new BasicNameValuePair("save_path", defaultPath));
				params.add(new BasicNameValuePair("connection", "5"));
				params.add(new BasicNameValuePair("ReferPage", ""));
				params.add(new BasicNameValuePair("textSpeedLimit", "0"));
				httppost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));

				// Make the request
				response = httpclient.execute(httppost);

				entity = response.getEntity();
				if (entity != null) {
					// Check BitComet response
					instream = entity.getContent();
					result = HttpHelper.convertStreamToString(instream);
					instream.close();
					if (result.indexOf("failed!") > 0) {
						throw new Exception("Adding URL failed");
					}
				}

				return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
			}
			return false;
		} catch (Exception e) {
			log.d(LOG_NAME, "Error: " + e.toString());
			throw new DaemonException(ExceptionType.ConnectionError, e.toString());
		}
	}

	/**
	 * Parse BitComet HTML page (http response)
	 * @param response The raw HTML response from the server
	 * @return The parsed list of torrents from the raw HTML content
	 * @throws DaemonException
	 */
	private ArrayList<Torrent> parseHttpTorrents(Log log, String response) throws DaemonException {

		ArrayList<Torrent> torrents = new ArrayList<Torrent>();

		try {

			// Find, prepare and split substring with HTML tag TABLE
			String[] parts =
					response.substring(response.indexOf("<TABLE"), response.indexOf("</TABLE>")).replaceAll("</td>", "")
							.replaceAll("</tr>", "").replaceAll("\n", "").split("<tr>");

			for (int i = 2; i < parts.length; i++) {

				String[] subParts = parts[i].replaceAll("<td>", "<td").split("<td");

				if (subParts.length == 10 && subParts[1].contains("BT")) {

					String name = subParts[2].substring(subParts[2].indexOf("/panel/task_detail"));
					name = name.substring(name.indexOf(">") + 1, name.indexOf("<"));

					TorrentStatus status = convertStatus(subParts[3]);
					String percenDoneStr = subParts[6];
					String downloadRateStr = subParts[7];
					String uploadRateStr = subParts[8];

					long size = convertSize(subParts[5]);
					float percentDone = Float.parseFloat(percenDoneStr.substring(0, percenDoneStr.indexOf("%")));
					long sizeDone = (long) (size * percentDone / 100);

					int rateUp = 1000 * Integer.parseInt(uploadRateStr.substring(0, uploadRateStr.indexOf("kB/s")));
					int rateDown =
							1000 * Integer.parseInt(downloadRateStr.substring(0, downloadRateStr.indexOf("kB/s")));

					// Unfortunately, there is no info for above values providing by BitComet now,
					// so we may only send additional request for that
					int leechers = 0;
					int seeders = 0;
					int knownLeechers = 0;
					int knownSeeders = 0;
					int distributed_copies = 0;
					long sizeUp;
					String comment;
					Date dateAdded;

					// Comment code below to speedup torrent listing
					// P.S. feature request to extend torrents info is already sent to the BitComet developers
					//*
					// Lets make summary request and parse details
					String summary = makeRequest(log, "/panel/task_detail", new BasicNameValuePair("id", "" + (i - 2)),
							new BasicNameValuePair("show", "summary"));

					String[] sumParts = summary.substring(summary.indexOf("<div align=\"left\">Value</div></th>"))
							.split("<tr><td>");
					comment = sumParts[7].substring(sumParts[7].indexOf("<td>") + 4, sumParts[7].indexOf("</td></tr>"));

					// Indexes for date and uploaded size
					int idx = 9;
					int sizeIdx = 12;

					if (status == TorrentStatus.Downloading) {
						seeders = Integer.parseInt(sumParts[9]
								.substring(sumParts[9].indexOf("Seeds:") + 6, sumParts[9].indexOf("(Max possible")));
						leechers = Integer.parseInt(sumParts[9].substring(sumParts[9].indexOf("Peers:") + 6,
								sumParts[9].lastIndexOf("(Max possible")));
						knownSeeders = Integer.parseInt(sumParts[9]
								.substring(sumParts[9].indexOf("(Max possible:") + 14, sumParts[9].indexOf(")")));
						knownLeechers = Integer.parseInt(sumParts[9]
								.substring(sumParts[9].lastIndexOf("(Max possible:") + 14,
										sumParts[9].lastIndexOf(")")));
						idx = 13;
						sizeIdx = 16;
					}

					DateFormat df = new SimpleDateFormat("yyyy-mm-dd kk:mm:ss");
					dateAdded = df.parse(sumParts[idx]
							.substring(sumParts[idx].indexOf("<td>") + 4, sumParts[idx].indexOf("</td></tr>")));
					//sizeDone =  convertSize(sumParts[sizeIdx].substring(sumParts[sizeIdx].indexOf("<td>")+4, sumParts[sizeIdx].indexOf(" (")));
					sizeUp = convertSize(sumParts[sizeIdx + 1]
							.substring(sumParts[sizeIdx + 1].indexOf("<td>") + 4, sumParts[sizeIdx + 1].indexOf(" (")));
					//*

					// Add the parsed torrent to the list
					// @formatter:off
					torrents.add(new Torrent(
							(long)i-2,
							null,
							name,
							status,
							null,
							rateDown,
							rateUp,
							seeders,
							knownSeeders,
							leechers,
							knownLeechers,
							(rateDown == 0? -1: (int) ((size - sizeDone) / rateDown)),
							sizeDone,
							sizeUp,
							size,
							percentDone / 100,
							distributed_copies,
							comment,
							dateAdded,
							null,
							null,
							settings.getType()));
					// @formatter:on
				}
			}
		} catch (Exception e) {
			throw new DaemonException(ExceptionType.UnexpectedResponse, "Invalid BitComet HTTP response.");
		}

		return torrents;
	}

	/**
	 * Parse BitComet AJAX response that code was copy-pasted and slightly modified from \Ktorrent\StatsParser.java
	 * @param response The raw XML data as string that was returned by the server
	 * @return The parsed list of torrents from the XML
	 * @throws DaemonException
	 */
	private ArrayList<Torrent> parseXmlTorrents(String response) throws DaemonException {

		ArrayList<Torrent> torrents = new ArrayList<Torrent>();

		try {
			// Use a PullParser to handle XML tags one by one
			XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
			xpp.setInput(new StringReader(response));

			// Temp variables to load into torrent objects
			int id = 0;
			String name = "";
			@SuppressWarnings("unused") String hash = "";
			TorrentStatus status = TorrentStatus.Unknown;
			long sizeDone = 0;
			long sizeUp = 0;
			long totalSize = 0;
			int rateDown = 0;
			int rateUp = 0;
			int seeders = 0;
			int seedersTotal = 0;
			int leechers = 0;
			int leechersTotal = 0;
			float progress = 0;
			String label = "";
			Date dateAdded = new Date();

			// Start pulling
			int next = xpp.nextTag();
			String tagName = xpp.getName();

			while (next != XmlPullParser.END_DOCUMENT) {

				if (next == XmlPullParser.END_TAG && tagName.equals("task")) {

					// End of a 'transfer' item, add gathered torrent data
					sizeDone = (long) (totalSize * progress);
					// @formatter:off
					torrents.add(new Torrent(
							id,
							null, // hash,  // we suppose to use simple integer IDs
							name,
							status,
							null,
							rateDown,
							rateUp,
							seeders,
							seedersTotal,
							leechers,
							leechersTotal,
							(int) ((status == TorrentStatus.Downloading && rateDown != 0)? (totalSize - sizeDone) / rateDown: -1), // eta (in seconds) = (total_size_in_btes - bytes_already_downloaded) / bytes_per_second
							sizeDone,
							sizeUp,
							totalSize,
							progress,
							0f,
							label,
							dateAdded,
							null,
							null, // Not supported in the web interface
							settings.getType()));
					// @formatter:on

					id++; // Stop/start/etc. requests are made by ID, which is the order number in the returned XML list :-S

				} else if (next == XmlPullParser.START_TAG && tagName.equals("task")) {

					// Start of a new 'transfer' item; reset gathered torrent data
					name = "";
					//hash = "";
					status = TorrentStatus.Unknown;
					sizeDone = 0;
					sizeUp = 0;
					totalSize = 0;
					rateDown = 0;
					rateUp = 0;
					seeders = 0;
					seedersTotal = 0;
					leechers = 0;
					leechersTotal = 0;
					progress = 0;
					label = "";
					dateAdded = new Date();

				} else if (next == XmlPullParser.START_TAG) {

					// Probably encountered a torrent property, i.e. '<type>BT</type>'
					next = xpp.next();
					if (next == XmlPullParser.TEXT) {
						if (tagName.equals("name")) {
							name = xpp.getText().trim();
						} else if (tagName.equals("infohash")) {
							hash = xpp.getText().trim();
						} else if (tagName.equals("state")) {
							status = convertStatus(xpp.getText());
						} else if (tagName.equals("bytes_downloaded")) {
							sizeDone = Integer.parseInt(xpp.getText());
						} else if (tagName.equals("bytes_uploaded")) {
							sizeUp = Integer.parseInt(xpp.getText());
						} else if (tagName.equals("size")) {
							totalSize = Long.parseLong(xpp.getText());
						} else if (tagName.equals("down_speed")) {
							rateDown = Integer.parseInt(xpp.getText());
						} else if (tagName.equals("up_speed")) {
							rateUp = Integer.parseInt(xpp.getText());
						} else if (tagName.equals("seeders")) {
							seeders = Integer.parseInt(xpp.getText());
						} else if (tagName.equals("total_seeders")) {
							seedersTotal = Integer.parseInt(xpp.getText());
						} else if (tagName.equals("peers")) {
							leechers = Integer.parseInt(xpp.getText());
						} else if (tagName.equals("total_peers")) {
							leechersTotal = Integer.parseInt(xpp.getText());
						} else if (tagName.equals("progress_permillage")) {
							progress = convertProgress(xpp.getText());
						} else if (tagName.equals("created_time")) {
							dateAdded = new Date(Long.parseLong(xpp.getText()));
						} else if (tagName.equals("comment")) {
							label = xpp.getText().trim();
						}
					}
				}

				next = xpp.next();
				if (next == XmlPullParser.START_TAG || next == XmlPullParser.END_TAG) {
					tagName = xpp.getName();
				}
			}

		} catch (XmlPullParserException e) {
			throw new DaemonException(ExceptionType.ParsingFailed, e.toString());
		} catch (Exception e) {
			throw new DaemonException(ExceptionType.UnexpectedResponse, "Invalid BitComet HTTP response.");
		}

		return torrents;
	}

	/**
	 * Parse BitComet HTML page (HTTP response)
	 * @param response The raw HTML response from the server
	 * @return The parsed list of files in the torrent from the raw HTML
	 * @throws DaemonException
	 */
	private ArrayList<TorrentFile> parseHttpTorrentFiles(String response, String hash) throws DaemonException {

		// Parse response
		ArrayList<TorrentFile> torrentfiles = new ArrayList<TorrentFile>();

		try {

			String[] files = response.substring(response.indexOf("Operation Method</div></th>") + 27,
					response.lastIndexOf("</TABLE>")).replaceAll("</td>", "").replaceAll("</tr>", "").split("<tr>");

			for (int i = 1; i < files.length; i++) {

				String[] fileDetails = files[i].replace(">", "").split("<td");

				long size = convertSize(fileDetails[4].substring(fileDetails[4].indexOf("&nbsp&nbsp ") + 11));
				long sizeDone = 0;
				if (!fileDetails[2].contains("--")) {
					double percentDone = Double.parseDouble(fileDetails[2].substring(0, fileDetails[2].indexOf("%")));
					sizeDone = (long) (size / 100.0 * percentDone);
				}

				// @formatter:off
				torrentfiles.add(new TorrentFile(
						hash,
						fileDetails[3],
						fileDetails[3],
						settings.getDownloadDir() + fileDetails[3],
						size,
						sizeDone,
						convertPriority(fileDetails[1])));
				// @formatter:on
			}
		} catch (Exception e) {
			throw new DaemonException(ExceptionType.UnexpectedResponse, "Invalid BitComet HTTP response.");
		}

		// Return the list
		return torrentfiles;
	}

	/**
	 * Parse BitComet torrent files priority
	 */
	private Priority convertPriority(String priority) {
		if (priority.equals("Very High") || priority.equals("High")) {
			return Priority.High;
		} else if (priority.equals("Normal")) {
			return Priority.Normal;
		}
		return Priority.Off;
	}

	/**
	 * Parse BitComet torrent status
	 */
	private TorrentStatus convertStatus(String state) {
		// Status is given as a descriptive string and an indication if the torrent was stopped/paused
		if (state.equals("stopped")) {
			return TorrentStatus.Paused;
		} else if (state.equals("running")) {
			return TorrentStatus.Downloading;
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
