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
import android.text.TextUtils;

import deluge.impl.net.AcceptAllTrustManager;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

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
import se.dimovski.rencode.Rencode;

/**
 * The daemon adapter from the Deluge torrent client using deluged API directly.
 *
 * @author alon.albert
 */
public class DelugeDirectAdapter implements IDaemonAdapter {

  public static final int DEFAULT_PORT = 58846;

  private static final String METHOD_LOGIN = "daemon.login";
  private static final String METHOD_GET_TORRENTS_STATUS = "core.get_torrents_status";
  private static final String METHOD_GET_TORRENT_STATUS = "core.get_torrent_status";
  private static final String METHOD_GET_LABELS = "label.get_labels";
  private static final String METHOD_ADD = "core.add_torrent_url";
  private static final String METHOD_ADD_MAGNET = "core.add_torrent_magnet";
  private static final String METHOD_ADD_FILE = "core.add_torrent_file";
  private static final String METHOD_REMOVE = "core.remove_torrent";
  private static final String METHOD_PAUSE = "core.pause_torrent";
  private static final String METHOD_PAUSE_ALL = "core.pause_all_torrents";
  private static final String METHOD_RESUME = "core.resume_torrent";
  private static final String METHOD_RESUME_ALL = "core.resume_all_torrents";
  private static final String METHOD_SETCONFIG = "core.set_config";
  private static final String METHOD_SETFILE = "core.set_torrent_file_priorities";
  private static final String METHOD_MOVESTORAGE = "core.move_storage";
  private static final String METHOD_SETTRACKERS = "core.set_torrent_trackers";
  private static final String METHOD_FORCERECHECK = "core.force_recheck";
  private static final String METHOD_SETLABEL = "label.set_torrent";

  private static final int RPC_ERROR = 2;

  private static final String TORRENT_FIELD_HASH = "hash";
  private static final String TORRENT_FIELD_NAME = "name";
  private static final String TORRENT_FIELD_STATUS = "state";
  private static final String TORRENT_FIELD_MESSAGE = "message";
  private static final String TORRENT_FIELD_SAVEPATH = "save_path";
  private static final String TORRENT_FIELD_RATEDOWNLOAD = "download_payload_rate";
  private static final String TORRENT_FIELD_RATEUPLOAD = "upload_payload_rate";
  private static final String TORRENT_FIELD_NUMSEEDS = "num_seeds";
  private static final String TORRENT_FIELD_TOTALSEEDS = "total_seeds";
  private static final String TORRENT_FIELD_NUMPEERS = "num_peers";
  private static final String TORRENT_FIELD_TOTALPEERS = "total_peers";
  private static final String TORRENT_FIELD_ETA = "eta";
  private static final String TORRENT_FIELD_TIMEADDED = "time_added";
  private static final String TORRENT_FIELD_DOWNLOADEDEVER = "total_done";
  private static final String TORRENT_FIELD_UPLOADEDEVER = "total_uploaded";
  private static final String TORRENT_FIELD_TOTALSIZE = "total_size";
  private static final String TORRENT_FIELD_PARTDONE = "progress";
  private static final String TORRENT_FIELD_LABEL = "label";
  private static final String TORRENT_FIELD_TRACKERS = "trackers";
  private static final String TORRENT_FIELD_TRACKER_STATUS = "tracker_status";

  private static final String TORRENT_FIELD_FILES = "files";
  private static final String TORRENT_FIELD_FILE_PROGRESS = "file_progress";
  private static final String TORRENT_FIELD_FILE_PRIORITIES = "file_priorities";

  private static final String FILE_INDEX = "index";
  private static final String FILE_PATH = "path";
  private static final String FILE_SIZE = "size";

  private static final String TRACKER_URL = "url";

  private static final String MAX_DOWNLOAD = "max_download_speed";
  private static final String MAX_UPLOAD = "max_upload_speed";

  private static final String[] TORRENT_FIELDS = {
      TORRENT_FIELD_HASH,
      TORRENT_FIELD_NAME,
      TORRENT_FIELD_STATUS,
      TORRENT_FIELD_SAVEPATH,
      TORRENT_FIELD_RATEDOWNLOAD,
      TORRENT_FIELD_RATEUPLOAD,
      TORRENT_FIELD_NUMPEERS,
      TORRENT_FIELD_NUMSEEDS,
      TORRENT_FIELD_TOTALPEERS,
      TORRENT_FIELD_TOTALSEEDS,
      TORRENT_FIELD_ETA,
      TORRENT_FIELD_DOWNLOADEDEVER,
      TORRENT_FIELD_UPLOADEDEVER,
      TORRENT_FIELD_TOTALSIZE,
      TORRENT_FIELD_PARTDONE,
      TORRENT_FIELD_LABEL,
      TORRENT_FIELD_MESSAGE,
      TORRENT_FIELD_TIMEADDED,
      TORRENT_FIELD_TRACKER_STATUS,
  };

  private static final String[] TORRENT_FILE_FIELDS = {
      TORRENT_FIELD_FILES,
      TORRENT_FIELD_FILE_PROGRESS,
      TORRENT_FIELD_FILE_PRIORITIES,
  };

  private static final String[] TORRENT_TRACKER_FIELDS = {
      TORRENT_FIELD_TRACKERS,
      TORRENT_FIELD_TRACKER_STATUS,
  };

  private static AtomicInteger requestIdCounter = new AtomicInteger();
  private final DaemonSettings settings;

  public DelugeDirectAdapter(DaemonSettings settings) {
    this.settings = settings;
  }

  @Override
  public DaemonTaskResult executeTask(Log log, DaemonTask task) {
    try {
      switch (task.getMethod()) {
        case Retrieve:
          return doRetrieve((RetrieveTask) task);
        case AddByUrl:
          return notSupported(task);
        case AddByMagnetUrl:
          return notSupported(task);
        case AddByFile:
          return doAddByFile((AddByFileTask) task);
        case Remove:
          return doRemove((RemoveTask)task);
        case Pause:
          return doControl(task, METHOD_PAUSE);
        case PauseAll:
          sendRequest(METHOD_PAUSE_ALL);
          return new DaemonTaskSuccessResult(task);
        case Resume:
          return doControl(task, METHOD_RESUME);
        case ResumeAll:
          sendRequest(METHOD_RESUME_ALL);
          return new DaemonTaskSuccessResult(task);
        case GetFileList:
          return doGetFileList((GetFileListTask) task);
        case SetFilePriorities:
          return notSupported(task);
        case SetTransferRates:
          return notSupported(task);
        case SetLabel:
          return notSupported(task);
        case SetDownloadLocation:
          return notSupported(task);
        case GetTorrentDetails:
          return doGetTorrentDetails((GetTorrentDetailsTask) task);
        case SetTrackers:
          return notSupported(task);
        case SetAlternativeMode:
          return notSupported(task);
        case GetStats:
          return notSupported(task);
        case ForceRecheck:
          return notSupported(task);
        default:
          return notSupported(task);
      }
    } catch (DaemonException e) {
      return new DaemonTaskFailureResult(task, e);
    }
  }

  private DaemonTaskResult doAddByFile(AddByFileTask task) throws DaemonException {
    final String file = task.getFile();
    final byte[] bytes = loadFile(file);
    final String fileContent = Base64.encodeBytes(bytes);

    sendRequest(METHOD_ADD_FILE, new Object[]{ file, fileContent, new HashMap<>() });
    return new DaemonTaskSuccessResult(task);
  }

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

  @Override
  public Daemon getType() {
    return settings.getType();
  }

  @Override
  public DaemonSettings getSettings() {
    return settings;
  }

  @NonNull
  private RetrieveTaskSuccessResult doRetrieve(RetrieveTask task) throws DaemonException {

    final List<Torrent> torrents = getTorrents();
    return new RetrieveTaskSuccessResult(task, torrents, getLabels(torrents));
  }


  private GetTorrentDetailsTaskSuccessResult doGetTorrentDetails(GetTorrentDetailsTask task)
      throws DaemonException {
    //noinspection unchecked
    final Map<String, Object> response = (Map<String, Object>) sendRequest(
        METHOD_GET_TORRENT_STATUS,
        new Object[]{task.getTargetTorrent().getUniqueID(), TORRENT_TRACKER_FIELDS});

    //noinspection unchecked
    final List<Map<String, Object>> trackerResponses = (List<Map<String, Object>>) response
        .get(TORRENT_FIELD_TRACKERS);
    final List<String> trackers = new ArrayList<>();
    for (Map<String, Object> trackerResponse : trackerResponses) {
      trackers.add((String) trackerResponse.get(TRACKER_URL));
    }

    return new GetTorrentDetailsTaskSuccessResult(task, new TorrentDetails(
        trackers,
        Collections.singletonList((String) response.get(TORRENT_FIELD_TRACKER_STATUS))));
  }

  private GetFileListTaskSuccessResult doGetFileList(GetFileListTask task) throws DaemonException {
    final ArrayList<TorrentFile> files = new ArrayList<>();
    //noinspection unchecked
    final Torrent torrent = task.getTargetTorrent();
    //noinspection unchecked
    final Map<String, Object> response = (Map<String, Object>) sendRequest(
        METHOD_GET_TORRENT_STATUS,
        new Object[]{torrent.getUniqueID(), TORRENT_FILE_FIELDS});

    //noinspection unchecked
    final List<Map<String, Object>> fileMaps = (List<Map<String, Object>>) response
        .get(TORRENT_FIELD_FILES);
    //noinspection unchecked
    final List<Integer> priorities = (List<Integer>) response.get(TORRENT_FIELD_FILE_PRIORITIES);
    //noinspection unchecked
    final List<Float> progresses = (List<Float>) response.get(TORRENT_FIELD_FILE_PROGRESS);

    for (int i = 0, n = fileMaps.size(); i < n; i++) {
      final Map<String, Object> fileMap = fileMaps.get(i);
      final int priority = priorities.get(i);
      final float progress = progresses.get(i);

      final String path = (String) fileMap.get(FILE_PATH);
      final long size = getLong(fileMap.get(FILE_SIZE));
      files.add(new TorrentFile(
          fileMap.get(FILE_INDEX).toString(),
          path,
          path,
          torrent.getLocationDir() + path,
          size,
          (long) (size * progress),
          convertDelugePriority(priority)));
    }
    return new GetFileListTaskSuccessResult(task, files);
  }

  private DaemonTaskResult doControl(DaemonTask task, String method) throws DaemonException {
    sendRequest(method, new Object[]{ new String[] { task.getTargetTorrent().getUniqueID()}});
    return new DaemonTaskSuccessResult(task);
  }

  private DaemonTaskResult doRemove(RemoveTask task) throws DaemonException {
    sendRequest(METHOD_REMOVE, new Object[]{ task.getTargetTorrent().getUniqueID(), task.includingData()});
    return new DaemonTaskSuccessResult(task);
  }

  @NonNull
  private List<Torrent> getTorrents() throws DaemonException {
    final Map response = (Map) sendRequest(
        METHOD_GET_TORRENTS_STATUS,
        new Object[]{new HashMap<>(), TORRENT_FIELDS});

    final List<Torrent> torrents = new ArrayList<>();
    int id = 0;
    for (Object o : response.values()) {
      //noinspection unchecked
      final Map<String, Object> values = (Map<String, Object>) o;

      final Object timeAdded = values.get(TORRENT_FIELD_TIMEADDED);
      final Date timeAddedDate;
      if (timeAdded != null) {
        final long seconds = (long) (float) timeAdded;
        timeAddedDate = new Date(seconds * 1000L);
      } else {
        timeAddedDate = null;
      }

      final String message = (String) values.get(TORRENT_FIELD_MESSAGE);
      final String trackerStatus = (String) values.get(TORRENT_FIELD_TRACKER_STATUS);
      final String error;
      if (trackerStatus.indexOf("Error") > 0) {
        error = message + (message.length() > 0 ? "\n" : "") + trackerStatus;
      } else {
        error = message;
      }

      torrents.add(new Torrent(
          id++,
          (String) values.get(TORRENT_FIELD_HASH),
          (String) values.get(TORRENT_FIELD_NAME),
          convertDelugeState((String) values.get(TORRENT_FIELD_STATUS)),
          values.get(TORRENT_FIELD_SAVEPATH) + settings.getOS().getPathSeperator(),
          (int) values.get(TORRENT_FIELD_RATEDOWNLOAD),
          (int) values.get(TORRENT_FIELD_RATEUPLOAD),
          (int) values.get(TORRENT_FIELD_NUMSEEDS),
          (int) values.get(TORRENT_FIELD_TOTALSEEDS),
          (int) values.get(TORRENT_FIELD_NUMPEERS),
          (int) values.get(TORRENT_FIELD_TOTALPEERS),
          getInt(values.get(TORRENT_FIELD_ETA)),
          getLong(values.get(TORRENT_FIELD_DOWNLOADEDEVER)),
          getLong(values.get(TORRENT_FIELD_UPLOADEDEVER)),
          getLong(values.get(TORRENT_FIELD_TOTALSIZE)),
          ((float) values.get(TORRENT_FIELD_PARTDONE)) / 100f,
          0f, // Not available
          (String) values.get(TORRENT_FIELD_LABEL),
          timeAddedDate,
          null, // Not available
          error,
          getType()));
    }
    return torrents;
  }

  @NonNull
  private List<Label> getLabels(List<Torrent> torrents) throws DaemonException {
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
    //noinspection unchecked
    final List<String> response = (List<String>) sendRequest(METHOD_GET_LABELS);
    for (String label : response) {
      if (!labelCounters.containsKey(label)) {
        labels.add(new Label(label, 0));
      }
    }
    return labels;
  }

  private Object sendRequest(String method) throws DaemonException {
    return sendRequest(method, new Object[]{}, new HashMap<String, Object>());
  }

  private Object sendRequest(String method, Object[] args) throws DaemonException {
    return sendRequest(method, args, new HashMap<String, Object>());
  }

  private Object sendRequest(String method, Object[] args, Map<String, Object> kwargs)
      throws DaemonException {
    final List<Object> requests = new ArrayList<>();
    final String username = settings.getUsername();
    if (!TextUtils.isEmpty(username)) {
      final String password = settings.getPassword();

      requests.add(new Object[]{
          requestIdCounter.getAndIncrement(),
          METHOD_LOGIN,
          new Object[]{username, password},
          new HashMap<>()});
    }
    requests.add(new Object[]{requestIdCounter.getAndIncrement(), method, args, kwargs});

    final byte[] request;
    try {
      request = compress(Rencode.encode(requests.toArray()));
    } catch (IOException e) {
      throw new DaemonException(ExceptionType.ConnectionError,
          "Failed to encode request: " + e.getMessage());
    }

    final Socket socket = openSocket();
    try {
      socket.getOutputStream().write(request);
      if (username != null) {
        // consume response to login request
        try {
          readResponse(socket.getInputStream());
        } catch (DaemonException e) {
          throw new DaemonException(ExceptionType.AuthenticationFailure, e.getMessage());
        }
      }
      return readResponse(socket.getInputStream());
    } catch (IOException e) {
      throw new DaemonException(ExceptionType.ConnectionError, e.getMessage());
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  private Object readResponse(InputStream in) throws DaemonException, IOException {
    final InflaterInputStream inflater = new InflaterInputStream(in);
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final byte[] buffer = new byte[1024];
    while (inflater.available() > 0) {
      final int n = inflater.read(buffer);
      if (n > 0) {
        out.write(buffer, 0, n);
      }
    }
    final byte[] bytes = out.toByteArray();
    final List response = (List) Rencode.decode(bytes);
    final int responseType = (int) response.get(0);
    if (responseType == RPC_ERROR) {
      final List errorResponse = (List) response.get(2);
      throw new DaemonException(ExceptionType.UnexpectedResponse,
          String.format("%s: %s", errorResponse.get(0), errorResponse.get(1)));
    }

    return response.get(2);
  }

  private byte[] compress(byte[] bytes) throws IOException {
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    try {
      DeflaterOutputStream deltaterOut = new DeflaterOutputStream(byteOut);
      try {
        deltaterOut.write(bytes);
        deltaterOut.finish();
        return byteOut.toByteArray();
      } finally {
        deltaterOut.close();
      }
    } finally {
      byteOut.close();
    }
  }

  private Socket openSocket() throws DaemonException {
    try {
      final TrustManager[] trustAllCerts = new TrustManager[]{new AcceptAllTrustManager()};
      final SSLContext sslContext = SSLContext.getInstance("TLSv1");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

      return sslContext.getSocketFactory().createSocket(settings.getAddress(), settings.getPort());
    } catch (NoSuchAlgorithmException e) {
      throw new DaemonException(ExceptionType.ConnectionError,
          "Failed to open socket: " + e.getMessage());
    } catch (UnknownHostException e) {
      throw new DaemonException(ExceptionType.ConnectionError,
          "Failed to open socket: " + e.getMessage());
    } catch (IOException e) {
      throw new DaemonException(ExceptionType.ConnectionError,
          "Failed to open socket: " + e.getMessage());
    } catch (KeyManagementException e) {
      throw new DaemonException(ExceptionType.ConnectionError,
          "Failed to open socket: " + e.getMessage());
    }
  }

  @NonNull
  private DaemonTaskFailureResult notSupported(DaemonTask task) {
    return new DaemonTaskFailureResult(task,
        new DaemonException(ExceptionType.MethodUnsupported,
            task.getMethod() + " is not supported by " + getType()));
  }

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

  private Priority convertDelugePriority(int priority) {
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
  }

  private static long getLong(Object o) {
    if (o instanceof Integer) {
      return (long) (int) o;
    }
    if (o instanceof Float) {
      return (long) (float) o;
    }
    if (o instanceof Double) {
      return (long) (float) o;
    }
    return (long) o;
  }

  private static int getInt(Object o) {
    if (o instanceof Long) {
      return (int) (long) o;
    }
    if (o instanceof Float) {
      return (int) (float) o;
    }
    if (o instanceof Double) {
      return (int) (float) o;
    }
    return (int) o;
  }


  private static class MutableInt {

    int value = 1;

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
