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

import android.support.annotation.NonNull;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.base64.android.Base64;
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
import org.transdroid.daemon.task.ForceRecheckTask;
import org.transdroid.daemon.task.GetFileListTask;
import org.transdroid.daemon.task.GetFileListTaskSuccessResult;
import org.transdroid.daemon.task.GetTorrentDetailsTask;
import org.transdroid.daemon.task.GetTorrentDetailsTaskSuccessResult;
import org.transdroid.daemon.task.RemoveTask;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetDownloadLocationTask;
import org.transdroid.daemon.task.SetFilePriorityTask;
import org.transdroid.daemon.task.SetLabelTask;
import org.transdroid.daemon.task.SetTrackersTask;
import org.transdroid.daemon.task.SetTransferRatesTask;

/**
 * The daemon adapter from the Deluge torrent client using deluged API directly.
 *
 * @author alon.albert
 */
public class DelugeRpcAdapter implements IDaemonAdapter {

  public static final int DEFAULT_PORT = 58846;

  // TODO: Extract constants to a common file used by both Adapters.
  private static final String RPC_METHOD_INFO = "daemon.info";
  private static final String RPC_METHOD_GET_TORRENTS_STATUS = "core.get_torrents_status";
  private static final String RPC_METHOD_STATUS = "core.get_torrent_status";
  private static final String RPC_METHOD_GET_LABELS = "label.get_labels";
  private static final String RPC_METHOD_ADD = "core.add_torrent_url";
  private static final String RPC_METHOD_ADD_MAGNET = "core.add_torrent_magnet";
  private static final String RPC_METHOD_ADD_FILE = "core.add_torrent_file";
  private static final String RPC_METHOD_REMOVE = "core.remove_torrent";
  private static final String RPC_METHOD_PAUSE = "core.pause_torrent";
  private static final String RPC_METHOD_PAUSE_ALL = "core.pause_all_torrents";
  private static final String RPC_METHOD_RESUME = "core.resume_torrent";
  private static final String RPC_METHOD_RESUME_ALL = "core.resume_all_torrents";
  private static final String RPC_METHOD_SETCONFIG = "core.set_config";
  private static final String RPC_METHOD_SET_TORRENT_OPTIONS = "core.set_torrent_options";
  private static final String RPC_METHOD_MOVESTORAGE = "core.move_storage";
  private static final String RPC_METHOD_SETTRACKERS = "core.set_torrent_trackers";
  private static final String RPC_METHOD_FORCERECHECK = "core.force_recheck";
  private static final String RPC_METHOD_SETLABEL = "label.set_torrent";

  private static final String RPC_HASH = "hash";
  private static final String RPC_NAME = "name";
  private static final String RPC_STATUS = "state";
  private static final String RPC_MESSAGE = "message";
  private static final String RPC_SAVEPATH = "save_path";
  private static final String RPC_RATEDOWNLOAD = "download_payload_rate";
  private static final String RPC_RATEUPLOAD = "upload_payload_rate";
  private static final String RPC_NUMSEEDS = "num_seeds";
  private static final String RPC_TOTALSEEDS = "total_seeds";
  private static final String RPC_NUMPEERS = "num_peers";
  private static final String RPC_TOTALPEERS = "total_peers";
  private static final String RPC_ETA = "eta";
  private static final String RPC_TIMEADDED = "time_added";
  private static final String RPC_DOWNLOADEDEVER = "total_done";
  private static final String RPC_UPLOADEDEVER = "total_uploaded";
  private static final String RPC_TOTALSIZE = "total_size";
  private static final String RPC_PARTDONE = "progress";
  private static final String RPC_LABEL = "label";
  private static final String RPC_TRACKERS = "trackers";
  private static final String RPC_TRACKER_STATUS = "tracker_status";

  private static final String RPC_FILES = "files";
  private static final String RPC_FILE_PROGRESS = "file_progress";
  private static final String RPC_FILE_PRIORITIES = "file_priorities";

  private static final String RPC_INDEX = "index";
  private static final String RPC_PATH = "path";
  private static final String RPC_SIZE = "size";

  private static final String RPC_TRACKER_TIER = "tier";
  private static final String RPC_TRACKER_URL = "url";

  private static final String RPC_MAX_DOWNLOAD = "max_download_speed";
  private static final String RPC_MAX_UPLOAD = "max_upload_speed";

  private static final String[] TORRENT_FIELDS = {
      RPC_HASH,
      RPC_NAME,
      RPC_STATUS,
      RPC_SAVEPATH,
      RPC_RATEDOWNLOAD,
      RPC_RATEUPLOAD,
      RPC_NUMPEERS,
      RPC_NUMSEEDS,
      RPC_TOTALPEERS,
      RPC_TOTALSEEDS,
      RPC_ETA,
      RPC_DOWNLOADEDEVER,
      RPC_UPLOADEDEVER,
      RPC_TOTALSIZE,
      RPC_PARTDONE,
      RPC_LABEL,
      RPC_MESSAGE,
      RPC_TIMEADDED,
      RPC_TRACKER_STATUS,
  };

  private static final String[] TORRENT_FILE_FIELDS = {
      RPC_FILES,
      RPC_FILE_PROGRESS,
      RPC_FILE_PRIORITIES,
  };

  private static final String[] TORRENT_TRACKER_FIELDS = {
      RPC_TRACKERS,
      RPC_TRACKER_STATUS,
  };

  private final DaemonSettings settings;
  private final DelugeRpcClient client;

  private int version = -1;

  public DelugeRpcAdapter(DaemonSettings settings) {
    this.settings = settings;
    client = new DelugeRpcClient(settings);
  }

  @Override
  public DaemonTaskResult executeTask(Log log, DaemonTask task) {
    try {
      switch (task.getMethod()) {
        case Retrieve:
          return doRetrieve((RetrieveTask) task);
        case AddByUrl:
          return doAddByUrl((AddByUrlTask) task);
        case AddByMagnetUrl:
          return doAddByMagnetUrl((AddByMagnetUrlTask) task);
        case AddByFile:
          return doAddByFile((AddByFileTask) task);
        case Remove:
          return doRemove((RemoveTask) task);
        case Pause:
          return doControl(task, RPC_METHOD_PAUSE);
        case PauseAll:
          return doControlAll(task, RPC_METHOD_PAUSE_ALL);
        case Resume:
          return doControl(task, RPC_METHOD_RESUME);
        case ResumeAll:
          return doControlAll(task, RPC_METHOD_RESUME_ALL);
        case GetFileList:
          return doGetFileList((GetFileListTask) task);
        case SetFilePriorities:
          return doSetFilePriorities((SetFilePriorityTask) task);
        case SetTransferRates:
          return doSetTransferRates((SetTransferRatesTask) task);
        case SetLabel:
          return doSetLabel((SetLabelTask) task);
        case SetDownloadLocation:
          return doSetDownloadLocation((SetDownloadLocationTask) task);
        case GetTorrentDetails:
          return doGetTorrentDetails((GetTorrentDetailsTask) task);
        case SetTrackers:
          return doSetTrackers((SetTrackersTask) task);
        case ForceRecheck:
          return doForceRecheck((ForceRecheckTask) task);
        default:
          return new DaemonTaskFailureResult(task,
              new DaemonException(ExceptionType.MethodUnsupported,
                  task.getMethod() + " is not supported by " + getType()));
      }
    } catch (DaemonException e) {
      return new DaemonTaskFailureResult(task, e);
    }
  }

  @Override
  public Daemon getType() {
    return Daemon.DelugeRpc;
  }

  @Override
  public DaemonSettings getSettings() {
    return settings;
  }

  @NonNull
  private RetrieveTaskSuccessResult doRetrieve(RetrieveTask task) throws DaemonException {

    final List<Object> results = client.sendRequests(
        new Request(RPC_METHOD_INFO),
        new Request(RPC_METHOD_GET_TORRENTS_STATUS, new HashMap<>(), TORRENT_FIELDS),
        new Request(RPC_METHOD_GET_LABELS));
    setVersion((String) results.get(0));
    //noinspection unchecked
    final Map<String, Map<String, Object>> torrentsStatus = (Map<String, Map<String, Object>>) results
        .get(1);
    final List<Torrent> torrents = getTorrents(torrentsStatus.values());
    //noinspection unchecked
    final List<Label> labels = getLabels((List<String>) results.get(2), torrents);

    return new RetrieveTaskSuccessResult(task, torrents, labels);
  }

  private GetTorrentDetailsTaskSuccessResult doGetTorrentDetails(GetTorrentDetailsTask task)
      throws DaemonException {
    //noinspection unchecked
    final Map<String, Object> response = (Map<String, Object>) client.sendRequest(
        RPC_METHOD_STATUS,
        task.getTargetTorrent().getUniqueID(),
        TORRENT_TRACKER_FIELDS);

    //noinspection unchecked
    final List<Map<String, Object>> trackerResponses = (List<Map<String, Object>>) response
        .get(RPC_TRACKERS);
    final List<String> trackers = new ArrayList<>();
    for (Map<String, Object> trackerResponse : trackerResponses) {
      trackers.add((String) trackerResponse.get(RPC_TRACKER_URL));
    }

    return new GetTorrentDetailsTaskSuccessResult(task, new TorrentDetails(
        trackers,
        Collections.singletonList((String) response.get(RPC_TRACKER_STATUS))));
  }

  private GetFileListTaskSuccessResult doGetFileList(GetFileListTask task) throws DaemonException {
    final ArrayList<TorrentFile> files = getTorrentFiles(task.getTargetTorrent());
    return new GetFileListTaskSuccessResult(task, files);
  }

  private DaemonTaskResult doControl(DaemonTask task, String method) throws DaemonException {
    client.sendRequest(method, (Object) getTorrentIdsArg(task));
    return new DaemonTaskSuccessResult(task);
  }

  private DaemonTaskResult doRemove(RemoveTask task) throws DaemonException {
    client.sendRequest(RPC_METHOD_REMOVE, task.getTargetTorrent().getUniqueID(),
        task.includingData());
    return new DaemonTaskSuccessResult(task);
  }

  @NonNull
  private DaemonTaskResult doControlAll(DaemonTask task, String method) throws DaemonException {
    client.sendRequest(method);
    return new DaemonTaskSuccessResult(task);
  }

  @NonNull
  private DaemonTaskResult doAddByFile(AddByFileTask task) throws DaemonException {
    final String file = task.getFile();
    final String fileContent = Base64.encodeBytes(loadFile(file));
    client.sendRequest(RPC_METHOD_ADD_FILE, file, fileContent, new HashMap<>());
    return new DaemonTaskSuccessResult(task);
  }

  @NonNull
  private DaemonTaskResult doAddByUrl(AddByUrlTask task) throws DaemonException {
    client.sendRequest(RPC_METHOD_ADD, task.getUrl(), new HashMap<>());
    return new DaemonTaskSuccessResult(task);
  }

  @NonNull
  private DaemonTaskResult doAddByMagnetUrl(AddByMagnetUrlTask task) throws DaemonException {
    client.sendRequest(RPC_METHOD_ADD_MAGNET, task.getUrl(), new HashMap<>());
    return new DaemonTaskSuccessResult(task);
  }

  @NonNull
  private DaemonTaskResult doSetLabel(SetLabelTask task) throws DaemonException {
    final String torrentId = task.getTargetTorrent().getUniqueID();
    final String label = task.getNewLabel() == null ? "" : task.getNewLabel();
    client.sendRequest(RPC_METHOD_SETLABEL, torrentId, label);
    return new DaemonTaskSuccessResult(task);
  }

  @NonNull
  private DaemonTaskResult doSetFilePriorities(SetFilePriorityTask task) throws DaemonException {
    // We first need a listing of all the files (because we can only set the priorities all at once)
    final ArrayList<TorrentFile> files = getTorrentFiles(task.getTargetTorrent());

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
      priorities.add(
          convertPriority(changedFiles.contains(file.getKey()) ? newPriority : file.getPriority()));
    }

    optionsArgs.put(RPC_FILE_PRIORITIES, priorities);
    client.sendRequest(RPC_METHOD_SET_TORRENT_OPTIONS, getTorrentIdsArg(task), optionsArgs);
    return new DaemonTaskSuccessResult(task);
  }

  @NonNull
  private DaemonTaskResult doSetTransferRates(SetTransferRatesTask task) throws DaemonException {
    final Map<String, Object> config = new HashMap<>();
    config.put(RPC_MAX_DOWNLOAD, task.getDownloadRate() == null ? -1 : task.getDownloadRate());
    config.put(RPC_MAX_UPLOAD, task.getUploadRate() == null ? -1 : task.getUploadRate());
    client.sendRequest(RPC_METHOD_SETCONFIG, config);
    return new DaemonTaskSuccessResult(task);
  }

  @NonNull
  private DaemonTaskResult doSetTrackers(SetTrackersTask task) throws DaemonException {
    final List<Map<String, Object>> trackers = new ArrayList<>();
    final ArrayList<String> newTrackers = task.getNewTrackers();
    for (int i = 0, n = newTrackers.size(); i < n; i++) {
      final Map<String, Object> tracker = new HashMap<>();
      tracker.put(RPC_TRACKER_TIER, i);
      tracker.put(RPC_TRACKER_URL, newTrackers.get(i));
      trackers.add(tracker);
    }
    client.sendRequest(RPC_METHOD_SETTRACKERS, task.getTargetTorrent().getUniqueID(), trackers);
    return new DaemonTaskSuccessResult(task);
  }

  @NonNull
  private DaemonTaskResult doForceRecheck(ForceRecheckTask task) throws DaemonException {
    client.sendRequest(RPC_METHOD_FORCERECHECK, getTorrentIdsArg(task));
    return new DaemonTaskSuccessResult(task);
  }

  @NonNull
  private DaemonTaskResult doSetDownloadLocation(SetDownloadLocationTask task) throws DaemonException {
    client.sendRequest(RPC_METHOD_MOVESTORAGE, getTorrentIdsArg(task), task.getNewLocation());
    return new DaemonTaskSuccessResult(task);
  }

  @NonNull
  private List<Torrent> getTorrents(Collection<Map<String, Object>> torrentMaps) throws DaemonException {
    final List<Torrent> torrents = new ArrayList<>();
    int id = 0;
    for (Map<String, Object> torrentMap : torrentMaps) {
      final Object timeAdded = torrentMap.get(RPC_TIMEADDED);
      final Date timeAddedDate;
      if (timeAdded != null) {
        final long seconds = (long) (float) timeAdded;
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

      torrents.add(new Torrent(
          id++,
          (String) torrentMap.get(RPC_HASH),
          (String) torrentMap.get(RPC_NAME),
          convertDelugeState((String) torrentMap.get(RPC_STATUS)),
          torrentMap.get(RPC_SAVEPATH) + settings.getOS().getPathSeperator(),
          ((Number) torrentMap.get(RPC_RATEDOWNLOAD)).intValue(),
          ((Number) torrentMap.get(RPC_RATEUPLOAD)).intValue(),
          ((Number) torrentMap.get(RPC_NUMSEEDS)).intValue(),
          ((Number) torrentMap.get(RPC_TOTALSEEDS)).intValue(),
          ((Number) torrentMap.get(RPC_NUMPEERS)).intValue(),
          ((Number) torrentMap.get(RPC_TOTALPEERS)).intValue(),
          ((Number) torrentMap.get(RPC_ETA)).intValue(),
          ((Number) torrentMap.get(RPC_DOWNLOADEDEVER)).longValue(),
          ((Number) torrentMap.get(RPC_UPLOADEDEVER)).longValue(),
          ((Number) torrentMap.get(RPC_TOTALSIZE)).longValue(),
          ((Number) torrentMap.get(RPC_PARTDONE)).floatValue() / 100f,
          0f, // Not available
          (String) torrentMap.get(RPC_LABEL),
          timeAddedDate,
          null, // Not available
          error,
          getType()));
    }
    return torrents;
  }

  @NonNull
  private List<Label> getLabels(List<String> labelsResponse, List<Torrent> torrents) throws DaemonException {
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
  private ArrayList<TorrentFile> getTorrentFiles(Torrent torrent) throws DaemonException {
    final ArrayList<TorrentFile> files = new ArrayList<>();
    //noinspection unchecked
    final Map<String, Object> response = (Map<String, Object>) client.sendRequest(
        RPC_METHOD_STATUS,
        torrent.getUniqueID(),
        TORRENT_FILE_FIELDS);

    //noinspection unchecked
    final List<Map<String, Object>> fileMaps = (List<Map<String, Object>>) response
        .get(RPC_FILES);
    //noinspection unchecked
    final List<Integer> priorities = (List<Integer>) response.get(RPC_FILE_PRIORITIES);
    //noinspection unchecked
    final List<Float> progresses = (List<Float>) response.get(RPC_FILE_PROGRESS);

    for (int i = 0, n = fileMaps.size(); i < n; i++) {
      final Map<String, Object> fileMap = fileMaps.get(i);
      final int priority = priorities.get(i);
      final float progress = progresses.get(i);

      final String path = (String) fileMap.get(RPC_PATH);
      final long size = ((Number) fileMap.get(RPC_SIZE)).longValue();
      files.add(new TorrentFile(
          fileMap.get(RPC_INDEX).toString(),
          path,
          path,
          torrent.getLocationDir() + path,
          size,
          (long) (size * progress),
          convertDelugePriority(priority)));
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
      throw new DaemonException(ExceptionType.FileAccessError,
          "File not found: " + file.getAbsolutePath());
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
      throw new DaemonException(ExceptionType.FileAccessError,
          "Error reading file: " + file.getAbsolutePath());
    } finally {
      try {
        in.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  // TODO: Move method to a common file used by both Adapters.
  private static TorrentStatus convertDelugeState(String state) {
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

  // TODO: Move method to a common file used by both Adapters.
  @NonNull
  private Priority convertDelugePriority(int priority)
      throws DaemonException {
    ensureVersion();
    if (version >= 10303) {
      // Priority codes changes from Deluge 1.3.3 onwards
      switch (priority) {
        case 0:
          return Priority.Off;
        case 1:
          return Priority.Low;
        case 7:
          return Priority.High;
        default:
          return Priority.Normal;
      }
    } else {
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
  }

  // TODO: Move method to a common file used by both Adapters.
  private int convertPriority(Priority priority) throws DaemonException {
    ensureVersion();
    if (version >= 10303) {
      // Priority codes changes from Deluge 1.3.3 onwards
      switch (priority) {
        case Off:
          return 0;
        case Low:
          return 1;
        case High:
          return 7;
        default:
          return 5;
      }
    } else {
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
  }

  private void ensureVersion() throws DaemonException {
    if (version > 0) {
      return;
    }
    setVersion((String) client.sendRequest(RPC_METHOD_INFO));
  }

  // TODO: Move to a common class
  private void setVersion(String versionString) {
    final String[] parts = versionString.split("\\.");

    if (parts.length > 0) {
      version = Integer.parseInt(parts[0]) * 100 * 100;
      if (parts.length > 1) {
        version += Integer.parseInt(parts[1]) * 100;
        if (parts.length > 2) {
          // For the last part only read until a non-numeric character is read
          // For example version 3.0.0-alpha5 is read as version code 30000
          String numbers = "";
          for (char c : parts[2].toCharArray()) {
            if (Character.isDigit(c))
            // Still a number; add it to the numbers string
            {
              numbers += Character.toString(c);
            } else {
              // No longer reading numbers; stop reading
              break;
            }
          }
          version += Integer.parseInt(numbers);
        }
      }
    }
  }

  // Return an Object so it doesn't confuse our varargs sendRequest methods.
  @NonNull
  private Object getTorrentIdsArg(DaemonTask task) {
    return new String[]{task.getTargetTorrent().getUniqueID()};
  }
}
