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

import org.transdroid.daemon.Priority;
import org.transdroid.daemon.TorrentStatus;

/**
 * Common constants and methods used by both adapters.
 *
 * @author alon.albert
 */
class DelugeCommon {
    static final String RPC_DETAILS = "files";
    static final String RPC_DOWNLOADEDEVER = "total_done";
    static final String RPC_ETA = "eta";
    static final String RPC_FILE = "file";
    static final String RPC_FILEPRIORITIES = "file_priorities";
    static final String RPC_FILEPROGRESS = "file_progress";
    static final String RPC_HASH = "hash";
    static final String RPC_INDEX = "index";
    static final String RPC_LABEL = "label";
    static final String RPC_MAXDOWNLOAD = "max_download_speed";
    static final String RPC_MAXUPLOAD = "max_upload_speed";
    static final String RPC_MESSAGE = "message";
    static final String RPC_METHOD = "method";
    static final String RPC_METHOD_ADD = "core.add_torrent_url";
    static final String RPC_METHOD_ADD_FILE = "core.add_torrent_file";
    static final String RPC_METHOD_ADD_MAGNET = "core.add_torrent_magnet";
    static final String RPC_METHOD_AUTH_LOGIN = "auth.login";
    static final String RPC_METHOD_DAEMON_LOGIN = "daemon.login";
    static final String RPC_METHOD_FORCERECHECK = "core.force_recheck";
    static final String RPC_METHOD_GET = "web.update_ui";
    static final String RPC_METHOD_GET_LABELS = "label.get_labels";
    static final String RPC_METHOD_GET_METHOD_LIST = "daemon.get_method_list";
    static final String RPC_METHOD_GET_TORRENTS_STATUS = "core.get_torrents_status";
    static final String RPC_METHOD_INFO = "daemon.info";
    static final String RPC_METHOD_MOVESTORAGE = "core.move_storage";
    static final String RPC_METHOD_PAUSE = "core.pause_torrent";
    static final String RPC_METHOD_PAUSE_ALL = "core.pause_all_torrents";
    static final String RPC_METHOD_REMOVE = "core.remove_torrent";
    static final String RPC_METHOD_RESUME = "core.resume_torrent";
    static final String RPC_METHOD_RESUME_ALL = "core.resume_all_torrents";
    static final String RPC_METHOD_SETCONFIG = "core.set_config";
    static final String RPC_METHOD_SETFILE = "core.set_torrent_file_priorities";
    static final String RPC_METHOD_SETLABEL = "label.set_torrent";
    static final String RPC_METHOD_SETTRACKERS = "core.set_torrent_trackers";
    static final String RPC_METHOD_SET_TORRENT_OPTIONS = "core.set_torrent_options";
    static final String RPC_METHOD_STATUS = "core.get_torrent_status";
    static final String RPC_NAME = "name";
    static final String RPC_NUMPEERS = "num_peers";
    static final String RPC_NUMSEEDS = "num_seeds";
    static final String RPC_PARAMS = "params";
    static final String RPC_PARTDONE = "progress";
    static final String RPC_PATH = "path";
    static final String RPC_RATEDOWNLOAD = "download_payload_rate";
    static final String RPC_RATEUPLOAD = "upload_payload_rate";
    static final String RPC_RESULT = "result";
    static final String RPC_SAVEPATH = "save_path";
    static final String RPC_SESSION_ID = "_session_id";
    static final String RPC_SIZE = "size";
    static final String RPC_STATUS = "state";
    static final String RPC_TIMEADDED = "time_added";
    static final String RPC_TORRENTS = "torrents";
    static final String RPC_TOTALPEERS = "total_peers";
    static final String RPC_TOTALSEEDS = "total_seeds";
    static final String RPC_TOTALSIZE = "total_size";
    static final String RPC_TRACKERS = "trackers";
    static final String RPC_TRACKER_STATUS = "tracker_status";
    static final String RPC_TRACKER_TIER = "tier";
    static final String RPC_TRACKER_URL = "url";
    static final String RPC_UPLOADEDEVER = "total_uploaded";

    static final String[] RPC_DETAILS_FIELDS_ARRAY = {
        RPC_TRACKERS,
        RPC_TRACKER_STATUS,
    };
    static final String[] RPC_FIELDS_ARRAY = {
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
    static final String[] RPC_FILE_FIELDS_ARRAY = {
        RPC_DETAILS,
        RPC_FILEPROGRESS,
        RPC_FILEPRIORITIES,
    };

    static TorrentStatus convertDelugeState(String state) {
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

    @NonNull
    static Priority convertDelugePriority(int priority, int version) {
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

    static int convertPriority(Priority priority, int version) {
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

    static int getVersionString(String versionString) {
      int version = 0;
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
      return version;
    }
}
