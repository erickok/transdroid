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

import androidx.annotation.NonNull;
import org.base64.android.Base64;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.remoterss.data.RemoteRssChannel;
import org.transdroid.core.gui.remoterss.data.RemoteRssItem;
import org.transdroid.core.gui.remoterss.data.RemoteRssSupplier;
import org.transdroid.core.rssparser.Channel;
import org.transdroid.core.rssparser.Item;
import org.transdroid.core.rssparser.RssParser;
import org.transdroid.daemon.*;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.task.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import static org.transdroid.daemon.Deluge.DelugeCommon.*;

/**
 * The daemon adapter from the Deluge torrent client using deluged API directly.
 *
 * @author alon.albert
 */
public class DelugeRpcAdapter implements IDaemonAdapter, RemoteRssSupplier {

	public static final int DEFAULT_PORT = 58846;

	private final DaemonSettings settings;
	private final boolean isVersion2;

	private int version = -1;

	public DelugeRpcAdapter(DaemonSettings settings, boolean isVersion2) {
		this.settings = settings;
		this.isVersion2 = isVersion2;
	}

	@Override
	public DaemonTaskResult executeTask(Log log, DaemonTask task) {
		final DelugeRpcClient client = new DelugeRpcClient(isVersion2);
		try {
			client.connect(settings);
			switch (task.getMethod()) {
				case Retrieve:
					return doRetrieve(client, (RetrieveTask) task);
				case AddByUrl:
					return doAddByUrl(client, (AddByUrlTask) task);
				case AddByMagnetUrl:
					return doAddByMagnetUrl(client, (AddByMagnetUrlTask) task);
				case AddByFile:
					return doAddByFile(client, (AddByFileTask) task);
				case Remove:
					return doRemove(client, (RemoveTask) task);
				case Pause:
					return doControl(client, task, RPC_METHOD_PAUSE);
				case PauseAll:
					return doControlAll(client, task, RPC_METHOD_PAUSE_ALL);
				case Resume:
					return doControl(client, task, RPC_METHOD_RESUME);
				case ResumeAll:
					return doControlAll(client, task, RPC_METHOD_RESUME_ALL);
				case GetFileList:
					return doGetFileList(client, (GetFileListTask) task);
				case SetFilePriorities:
					return doSetFilePriorities(client, (SetFilePriorityTask) task);
				case SetTransferRates:
					return doSetTransferRates(client, (SetTransferRatesTask) task);
				case SetLabel:
					return doSetLabel(client, (SetLabelTask) task);
				case SetDownloadLocation:
					return doSetDownloadLocation(client, (SetDownloadLocationTask) task);
				case GetTorrentDetails:
					return doGetTorrentDetails(client, (GetTorrentDetailsTask) task);
				case SetTrackers:
					return doSetTrackers(client, (SetTrackersTask) task);
				case ForceRecheck:
					return doForceRecheck(client, (ForceRecheckTask) task);
				default:
					return new DaemonTaskFailureResult(task, new DaemonException(ExceptionType.MethodUnsupported, task.getMethod() + " is not " +
							"supported by " + getType()));
			}
		} catch (DaemonException e) {
			return new DaemonTaskFailureResult(task, e);
		} finally {
			client.close();
		}
	}

	@Override
	public Daemon getType() {
		return isVersion2 ? Daemon.Deluge2Rpc : Daemon.DelugeRpc;
	}

	@Override
	public DaemonSettings getSettings() {
		return settings;
	}

	@Override
	public ArrayList<RemoteRssChannel> getRemoteRssChannels(Log log) throws DaemonException {
		final long now = System.currentTimeMillis();
		final DelugeRpcClient client = new DelugeRpcClient(isVersion2);
		try {
			client.connect(settings);

			if (!hasMethod(client, RPC_METHOD_GET_RSS_CONFIG)) {
				throw new DaemonException(ExceptionType.MethodUnsupported, "YaRRS2 plugin not installed");
			}
			//noinspection unchecked
			final Map<String, Object> rssConfig = (Map<String, Object>) client.sendRequest(RPC_METHOD_GET_RSS_CONFIG);

			//noinspection unchecked
			final Map<String, Map<String, Object>> rssFeeds = (Map<String, Map<String, Object>>) rssConfig.get(RPC_RSSFEEDS);

			final Map<Object, String> feedUrlMap = new HashMap<>();
			final Map<Object, List<Item>> feedItemMap = new HashMap<>();
			if (rssFeeds != null) {
				for (Map<String, Object> feed : rssFeeds.values()) {
					final String feedUrl = (String) feed.get(RPC_URL);
					final Object key = feed.get(RPC_KEY);
					feedUrlMap.put(key, feedUrl);
					final List<Item> items = getRssFeedItems(feedUrl, log);
					feedItemMap.put(key, items);
				}
			}

			//noinspection unchecked
			final Map<String, Map<String, Object>> subscriptions = (Map<String, Map<String, Object>>) rssConfig.get(RPC_SUBSCRIPTIONS);
			final ArrayList<RemoteRssChannel> channels = new ArrayList<>();
			if (subscriptions != null) {
				for (Map<String, Object> subscription : subscriptions.values()) {
					final Integer key = Integer.valueOf(subscription.get(RPC_KEY).toString());
					final String name = (String) subscription.get(RPC_NAME);
					final String label = (String) subscription.get(RPC_LABEL);
					final String downloadLocation = (String) subscription.get(RPC_DOWNLOAD_LOCATION);
					final String moveCompleted = (String) subscription.get(RPC_MOVE_COMPLETED);
					final Object feedKey = subscription.get(RPC_RSSFEED_KEY);
					final String feedUrl = feedUrlMap.get(feedKey);

					final List<RemoteRssItem> items = new ArrayList<>();
					final List<Item> feedItems = feedItemMap.get(feedKey);
					if (feedItems != null) {
						for (Item item : feedItems) {
							items.add(new DelugeRemoteRssItem(item.getTitle(), item.getLink(), name, item.getPubdate()));
						}
					}

					channels.add(new DelugeRemoteRssChannel(key, name, feedUrl, now, label, downloadLocation, moveCompleted, items));
				}
			}
			return channels;
		} finally {
			client.close();
			android.util.Log.i("Alon", String.format("getRemoteRssChannels: %dms", System.currentTimeMillis() - now));
		}
	}

	@Override
	public void downloadRemoteRssItem(Log log, RemoteRssItem rssItem, RemoteRssChannel rssChannel) throws DaemonException {
		final DelugeRemoteRssItem item = (DelugeRemoteRssItem) rssItem;
		final DelugeRemoteRssChannel channel = (DelugeRemoteRssChannel) rssChannel;

		final Map<String, Object> options = new HashMap<>();
		final String label;
		if (channel != null) {
			final String downloadLocation = channel.getDownloadLocation();
			if (downloadLocation != null) {
				options.put(RPC_DOWNLOAD_LOCATION, downloadLocation);
			}
			final String moveCompleted = channel.getMoveCompleted();
			if (moveCompleted != null) {
				options.put(RPC_MOVE_COMPLETED, true);
				options.put(RPC_MOVE_COMPLETED_PATH, moveCompleted);
			}
			label = channel.getLabel();
		} else {
			label = null;
		}
		final DelugeRpcClient client = new DelugeRpcClient(isVersion2);

		try {
			client.connect(settings);
			final String torrentId = (String) client
					.sendRequest(item.isMagnetLink() ? RPC_METHOD_ADD_MAGNET : RPC_METHOD_ADD, item.getLink(), options);
			if (label != null && hasMethod(client, RPC_METHOD_SETLABEL)) {
				client.sendRequest(RPC_METHOD_SETLABEL, torrentId, label);
			}
		} finally {
			client.close();
		}
	}

	@NonNull
	private RetrieveTaskSuccessResult doRetrieve(DelugeRpcClient client, RetrieveTask task) throws DaemonException {
		// Get torrents
		//noinspection unchecked
		final Map<String, Map<String, Object>> torrentsStatus = (Map<String, Map<String, Object>>) client.sendRequest
				(RPC_METHOD_GET_TORRENTS_STATUS, new HashMap<>(), RPC_FIELDS_ARRAY);
		final List<Torrent> torrents = getTorrents(torrentsStatus.values());

		// Check if Label plugin is enabled
		final boolean hasLabelPlugin = hasMethod(client, RPC_METHOD_GET_LABELS);

		// Get label list from server
		//noinspection unchecked
		final List<String> labelNames = hasLabelPlugin ? (List<String>) client.sendRequest(RPC_METHOD_GET_LABELS) : new ArrayList<String>();

		// Extract labels & counts from torrents.
		final List<Label> labels = getLabels(labelNames, torrents);

		return new RetrieveTaskSuccessResult(task, torrents, labels);
	}

	private GetTorrentDetailsTaskSuccessResult doGetTorrentDetails(DelugeRpcClient client, GetTorrentDetailsTask task) throws DaemonException {
		//noinspection unchecked
		final Map<String, Object> response = (Map<String, Object>) client.sendRequest(RPC_METHOD_STATUS, task.getTargetTorrent().getUniqueID(),
				RPC_DETAILS_FIELDS_ARRAY);

		//noinspection unchecked
		final List<Map<String, Object>> trackerResponses = (List<Map<String, Object>>) response.get(RPC_TRACKERS);
		final List<String> trackers = new ArrayList<>();
		if (trackerResponses != null) {
			for (Map<String, Object> trackerResponse : trackerResponses) {
				trackers.add((String) trackerResponse.get(RPC_URL));
			}
		}

		return new GetTorrentDetailsTaskSuccessResult(task, new TorrentDetails(trackers, Collections.singletonList((String) response.get
				(RPC_TRACKER_STATUS))));
	}

	private GetFileListTaskSuccessResult doGetFileList(DelugeRpcClient client, GetFileListTask task) throws DaemonException {
		final ArrayList<TorrentFile> files = getTorrentFiles(client, task.getTargetTorrent());
		return new GetFileListTaskSuccessResult(task, files);
	}

	private DaemonTaskResult doControl(DelugeRpcClient client, DaemonTask task, String method) throws DaemonException {
		client.sendRequest(method, (Object) getTorrentIdsArg(task));
		return new DaemonTaskSuccessResult(task);
	}

	private DaemonTaskResult doRemove(DelugeRpcClient client, RemoveTask task) throws DaemonException {
		client.sendRequest(RPC_METHOD_REMOVE, task.getTargetTorrent().getUniqueID(), task.includingData());
		return new DaemonTaskSuccessResult(task);
	}

	@NonNull
	private DaemonTaskResult doControlAll(DelugeRpcClient client, DaemonTask task, String method) throws DaemonException {
		client.sendRequest(method);
		return new DaemonTaskSuccessResult(task);
	}

	@NonNull
	private DaemonTaskResult doAddByFile(DelugeRpcClient client, AddByFileTask task) throws DaemonException {
		final String file = task.getFile();
		final String fileContent = Base64.encodeBytes(loadFile(file));
		client.sendRequest(RPC_METHOD_ADD_FILE, file, fileContent, new HashMap<>());
		return new DaemonTaskSuccessResult(task);
	}

	@NonNull
	private DaemonTaskResult doAddByUrl(DelugeRpcClient client, AddByUrlTask task) throws DaemonException {
		client.sendRequest(RPC_METHOD_ADD, task.getUrl(), new HashMap<>());
		return new DaemonTaskSuccessResult(task);
	}

	@NonNull
	private DaemonTaskResult doAddByMagnetUrl(DelugeRpcClient client, AddByMagnetUrlTask task) throws DaemonException {
		client.sendRequest(RPC_METHOD_ADD_MAGNET, task.getUrl(), new HashMap<>());
		return new DaemonTaskSuccessResult(task);
	}

	@NonNull
	private DaemonTaskResult doSetLabel(DelugeRpcClient client, SetLabelTask task) throws DaemonException {
		if (!hasMethod(client, RPC_METHOD_SETLABEL)) {
			throw new DaemonException(ExceptionType.MethodUnsupported, "Label plugin not installed");
		}
		final String torrentId = task.getTargetTorrent().getUniqueID();
		final String label = task.getNewLabel() == null ? "" : task.getNewLabel();
		client.sendRequest(RPC_METHOD_SETLABEL, torrentId, label);
		return new DaemonTaskSuccessResult(task);
	}

	@NonNull
	private DaemonTaskResult doSetFilePriorities(DelugeRpcClient client, SetFilePriorityTask task) throws DaemonException {
		// We first need a listing of all the files (because we can only set the priorities all at once)
		final ArrayList<TorrentFile> files = getTorrentFiles(client, task.getTargetTorrent());

		// prepare options arg
		final Map<String, Object> optionsArgs = new HashMap<>();

		// Build a fast access set of file to change
		final Set<String> changedFiles = new HashSet<>();
		for (TorrentFile file : task.getForFiles()) {
			changedFiles.add(file.getKey());
		}

		// Build array of converted priorities
		final ArrayList<Integer> priorities = new ArrayList<>();
		final Priority newPriority = task.getNewPriority();
		for (TorrentFile file : files) {
			final Priority priority = changedFiles.contains(file.getKey()) ? newPriority : file.getPriority();
			priorities.add(convertPriority(client, priority));
		}

		optionsArgs.put(RPC_FILEPRIORITIES, priorities);
		client.sendRequest(RPC_METHOD_SET_TORRENT_OPTIONS, getTorrentIdsArg(task), optionsArgs);
		return new DaemonTaskSuccessResult(task);
	}

	@NonNull
	private DaemonTaskResult doSetTransferRates(DelugeRpcClient client, SetTransferRatesTask task) throws DaemonException {
		final Map<String, Object> config = new HashMap<>();
		config.put(RPC_MAXDOWNLOAD, task.getDownloadRate() == null ? -1 : task.getDownloadRate());
		config.put(RPC_MAXUPLOAD, task.getUploadRate() == null ? -1 : task.getUploadRate());
		client.sendRequest(RPC_METHOD_SETCONFIG, config);
		return new DaemonTaskSuccessResult(task);
	}

	@NonNull
	private DaemonTaskResult doSetTrackers(DelugeRpcClient client, SetTrackersTask task) throws DaemonException {
		final List<Map<String, Object>> trackers = new ArrayList<>();
		final ArrayList<String> newTrackers = task.getNewTrackers();
		for (int i = 0, n = newTrackers.size(); i < n; i++) {
			final Map<String, Object> tracker = new HashMap<>();
			tracker.put(RPC_TIER, i);
			tracker.put(RPC_URL, newTrackers.get(i));
			trackers.add(tracker);
		}
		client.sendRequest(RPC_METHOD_SETTRACKERS, task.getTargetTorrent().getUniqueID(), trackers);
		return new DaemonTaskSuccessResult(task);
	}

	@NonNull
	private DaemonTaskResult doForceRecheck(DelugeRpcClient client, ForceRecheckTask task) throws DaemonException {
		client.sendRequest(RPC_METHOD_FORCERECHECK, getTorrentIdsArg(task));
		return new DaemonTaskSuccessResult(task);
	}

	@NonNull
	private DaemonTaskResult doSetDownloadLocation(DelugeRpcClient client, SetDownloadLocationTask task) throws DaemonException {
		client.sendRequest(RPC_METHOD_MOVESTORAGE, getTorrentIdsArg(task), task.getNewLocation());
		return new DaemonTaskSuccessResult(task);
	}

	@NonNull
	private List<Torrent> getTorrents(Collection<Map<String, Object>> torrentMaps) {
		final List<Torrent> torrents = new ArrayList<>();
		int id = 0;
		for (Map<String, Object> torrentMap : torrentMaps) {
			final Number timeAdded = (Number) torrentMap.get(RPC_TIMEADDED);
			final Date timeAddedDate;
			if (timeAdded != null) {
				final long seconds = timeAdded.longValue();
				timeAddedDate = new Date(seconds * 1000L);
			} else {
				timeAddedDate = null;
			}

			final String message = (String) torrentMap.get(RPC_MESSAGE);
			final String trackerStatus = (String) torrentMap.get(RPC_TRACKER_STATUS);
			final String error;
			if (trackerStatus.indexOf("Error") > 0) {
				error = message + (message.length() > 0 ? "\n" : "") + trackerStatus;
			} else {
				error = message;
			}

			torrents.add(new Torrent(id++, (String) torrentMap.get(RPC_HASH), (String) torrentMap.get(RPC_NAME), DelugeCommon.convertDelugeState(
					(String) torrentMap.get(RPC_STATUS)), torrentMap.get(RPC_SAVEPATH) + settings.getOS().getPathSeperator(), ((Number) torrentMap
					.get(RPC_RATEDOWNLOAD)).intValue(), ((Number) torrentMap.get(RPC_RATEUPLOAD)).intValue(), ((Number) torrentMap.get
					(RPC_NUMSEEDS)).intValue(), ((Number) torrentMap.get(RPC_TOTALSEEDS)).intValue(), ((Number) torrentMap.get(RPC_NUMPEERS))
					.intValue(), ((Number) torrentMap.get(RPC_TOTALPEERS)).intValue(), ((Number) torrentMap.get(RPC_ETA)).intValue(), ((Number)
					torrentMap.get(RPC_DOWNLOADEDEVER)).longValue(), ((Number) torrentMap.get(RPC_UPLOADEDEVER)).longValue(), ((Number) torrentMap
					.get(RPC_TOTALSIZE)).longValue(), ((Number) torrentMap.get(RPC_PARTDONE)).floatValue() / 100f, 0f, // Not available
					(String) torrentMap.get(RPC_LABEL), timeAddedDate, null, // Not available
					error, getType()));
		}
		return torrents;
	}

	@NonNull
	private List<Label> getLabels(List<String> labelsResponse, List<Torrent> torrents) {
		// First get all labels that torrents and count them
		final Map<String, MutableInt> labelCounters = new HashMap<>();
		for (Torrent torrent : torrents) {
			final String label = torrent.getLabelName();
			if (label != null) {
				final MutableInt count = labelCounters.get(label);
				if (count == null) {
					labelCounters.put(label, new MutableInt(1));
				} else {
					count.increment();
				}
			}
		}
		final List<Label> labels = new ArrayList<>();
		for (Entry<String, MutableInt> entry : labelCounters.entrySet()) {
			labels.add(new Label(entry.getKey(), entry.getValue().get()));
		}

		// Now get all labels and add labels that have no torrents.
		for (String label : labelsResponse) {
			if (!labelCounters.containsKey(label)) {
				labels.add(new Label(label, 0));
			}
		}
		return labels;
	}

	@NonNull
	private ArrayList<TorrentFile> getTorrentFiles(DelugeRpcClient client, Torrent torrent) throws DaemonException {
		final ArrayList<TorrentFile> files = new ArrayList<>();
		//noinspection unchecked
		final Map<String, Object> response = (Map<String, Object>) client.sendRequest(RPC_METHOD_STATUS, torrent.getUniqueID(),
				RPC_FILE_FIELDS_ARRAY);

		//noinspection unchecked
		final List<Map<String, Object>> fileMaps = (List<Map<String, Object>>) response.get(RPC_DETAILS);
		//noinspection unchecked
		final List<Integer> priorities = (List<Integer>) response.get(RPC_FILEPRIORITIES);
		//noinspection unchecked
		final List<Float> progresses = (List<Float>) response.get(RPC_FILEPROGRESS);

		if (fileMaps != null) {
			for (int i = 0, n = fileMaps.size(); i < n; i++) {
				final Map<String, Object> fileMap = fileMaps.get(i);
				final int priority = priorities.get(i);
				final float progress = progresses.get(i);

				final String path = (String) fileMap.get(RPC_PATH);
				final long size = ((Number) fileMap.get(RPC_SIZE)).longValue();
				files.add(new TorrentFile(fileMap.get(RPC_INDEX).toString(), path, path, torrent.getLocationDir() + path, size,
						(long) (size * progress), convertDelugePriority(client, priority)));
			}
		}
		return files;
	}

	@NonNull
	private byte[] loadFile(String url) throws DaemonException {
		final File file = new File(URI.create(url));
		final BufferedInputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new DaemonException(ExceptionType.FileAccessError, "File not found: " + file.getAbsolutePath());
		}
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			final byte[] buffer = new byte[1024];
			while (true) {
				final int n = in.read(buffer);
				if (n < 0) {
					break;
				}
				out.write(buffer, 0, n);
			}
			return out.toByteArray();
		} catch (IOException e) {
			throw new DaemonException(ExceptionType.FileAccessError, "Error reading file: " + file.getAbsolutePath());
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	@NonNull
	private Priority convertDelugePriority(DelugeRpcClient client, int priority) throws DaemonException {
		ensureVersion(client);
		return DelugeCommon.convertDelugePriority(priority, version);
	}

	private int convertPriority(DelugeRpcClient client, Priority priority) throws DaemonException {
		ensureVersion(client);
		return DelugeCommon.convertPriority(priority, version);
	}

	private void ensureVersion(DelugeRpcClient client) throws DaemonException {
		if (version > 0) {
			return;
		}
		version = DelugeCommon.getVersionString((String) client.sendRequest(RPC_METHOD_INFO));
	}

	// Return an Object so it doesn't confuse our varargs sendRequest methods.
	@NonNull
	private Object getTorrentIdsArg(DaemonTask task) {
		return new String[]{task.getTargetTorrent().getUniqueID()};
	}

	@NonNull
	private List<Item> getRssFeedItems(String feedUrl, Log log) {
		final RssParser rssParser = new RssParser(feedUrl, null, null);
		try {
			rssParser.parse();
			final Channel channel = rssParser.getChannel();
			return channel.getItems();
		} catch (ParserConfigurationException e) {
			log.e(DelugeRpcAdapter.this, "Failed to parse RSS feed.");
		} catch (SAXException e) {
			log.e(DelugeRpcAdapter.this, "Failed to parse RSS feed.");
		} catch (IOException e) {
			log.e(DelugeRpcAdapter.this, "Failed to load RSS feed.");
		}
		return new ArrayList<>();
	}

	private boolean hasMethod(DelugeRpcClient client, String method) throws DaemonException {
		//noinspection unchecked
		final List<String> methods = (List<String>) client.sendRequest(RPC_METHOD_GET_METHOD_LIST);
		return methods.contains(method);
	}

	/**
	 * Used to count torrents in labels.
	 */
	private static class MutableInt {

		int value;

		MutableInt(int value) {
			this.value = value;
		}

		void increment() {
			++value;
		}

		int get() {
			return value;
		}

	}

}
