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
package org.transdroid.daemon.adapters.synology;

import com.android.internal.http.multipart.FilePart;
import com.android.internal.http.multipart.MultipartEntity;
import com.android.internal.http.multipart.Part;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.transdroid.core.gui.log.Log;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.IDaemonAdapter;
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
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.util.Collections2;
import org.transdroid.daemon.util.HttpHelper;
import org.transdroid.multipart.Utf8StringPart;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The daemon adapter from the Synology Download Station torrent client.
 */
public class SynologyAdapter implements IDaemonAdapter {

    private static final String LOG_NAME = "Synology daemon";

    private DaemonSettings settings;
    private DefaultHttpClient httpClient;

    private String sid;

    public SynologyAdapter(DaemonSettings settings) {
        this.settings = settings;
    }

    private synchronized void initialise() throws DaemonException {
        if (httpClient == null) {
            httpClient = HttpHelper.createStandardHttpClient(settings, true);
        }
    }

    @Override
    public DaemonTaskResult executeTask(Log log, DaemonTask task) {
        String tid;
        try {
            initialise();
            switch (task.getMethod()) {
                case Retrieve:
                    return new RetrieveTaskSuccessResult((RetrieveTask) task, tasksList(log), null);
                case GetTorrentDetails:
                    tid = task.getTargetTorrent().getUniqueID();
                    return new GetTorrentDetailsTaskSuccessResult((GetTorrentDetailsTask) task,
                            torrentDetails(log, tid));
                case GetFileList:
                    tid = task.getTargetTorrent().getUniqueID();
                    return new GetFileListTaskSuccessResult((GetFileListTask) task, fileList(log, tid));
                case AddByFile:
                    createTask(log, new File(URI.create(((AddByFileTask) task).getFile())));
                    return new DaemonTaskSuccessResult(task);
                case AddByUrl:
                    String url = ((AddByUrlTask) task).getUrl();
                    createTask(log, url);
                    return new DaemonTaskSuccessResult(task);
                case AddByMagnetUrl:
                    String magnet = ((AddByMagnetUrlTask) task).getUrl();
                    createTask(log, magnet);
                    return new DaemonTaskSuccessResult(task);
                case Remove:
                    tid = task.getTargetTorrent().getUniqueID();
                    removeTask(log, tid);
                    return new DaemonTaskSuccessResult(task);
                case Pause:
                    tid = task.getTargetTorrent().getUniqueID();
                    pauseTask(log, tid);
                    return new DaemonTaskSuccessResult(task);
                case PauseAll:
                    pauseAllTasks(log);
                    return new DaemonTaskSuccessResult(task);
                case Resume:
                    tid = task.getTargetTorrent().getUniqueID();
                    resumeTask(log, tid);
                    return new DaemonTaskSuccessResult(task);
                case ResumeAll:
                    resumeAllTasks(log);
                    return new DaemonTaskSuccessResult(task);
                case SetTransferRates:
                    SetTransferRatesTask ratesTask = (SetTransferRatesTask) task;
                    int uploadRate = ratesTask.getUploadRate() == null ? 0 : ratesTask.getUploadRate();
                    int downloadRate = ratesTask.getDownloadRate() == null ? 0 : ratesTask.getDownloadRate();
                    setTransferRates(log, uploadRate, downloadRate);
                    return new DaemonTaskSuccessResult(task);
                case SetAlternativeMode:
                case GetStats:
                case SetDownloadLocation:
                case SetFilePriorities:
                default:
                    return null;
            }
        } catch (DaemonException e) {
            return new DaemonTaskFailureResult(task, e);
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

    // Synology API

    private String login(Log log) throws DaemonException {
        log.d(LOG_NAME, "login()");
        try {
            return new SynoRequest("auth.cgi", "SYNO.API.Auth", "2")
                    .get("&method=login&account=" + settings.getUsername() + "&passwd=" + settings.getPassword() +
                            "&session=DownloadStation&format=sid").getData(log).getString("sid");
        } catch (JSONException e) {
            throw new DaemonException(ExceptionType.ParsingFailed, e.toString());
        }
    }

    private void setTransferRates(Log log, int uploadRate, int downloadRate) throws DaemonException {
        authGet(log, "SYNO.DownloadStation.Info", "1", "DownloadStation/info.cgi",
                "&method=setserverconfig&bt_max_upload=" + uploadRate + "&bt_max_download=" + downloadRate)
                .ensureSuccess(log);
    }

    private void createTask(Log log, String uri) throws DaemonException {
        try {
            authGet(log, "SYNO.DownloadStation.Task", "1", "DownloadStation/task.cgi",
                    "&method=create&uri=" + URLEncoder.encode(uri, "UTF-8")).ensureSuccess(log);
        } catch (UnsupportedEncodingException e) {
            // Never happens
            throw new DaemonException(ExceptionType.UnexpectedResponse, e.toString());
        }
    }

    private void createTask(Log log, File file) throws DaemonException {
        try {
            authPost(log, "SYNO.DownloadStation.Task", "1", "DownloadStation/task.cgi",
                    new Utf8StringPart("method", "create"),
                    new FilePart("file", file))
                    .ensureSuccess(log);
        } catch (FileNotFoundException e) {
            throw new DaemonException(ExceptionType.FileAccessError, e.getMessage());
        }
    }

    private void removeTask(Log log, String tid) throws DaemonException {
        List<String> tids = new ArrayList<>();
        tids.add(tid);
        removeTasks(log, tids);
    }

    private void pauseTask(Log log, String tid) throws DaemonException {
        List<String> tids = new ArrayList<>();
        tids.add(tid);
        pauseTasks(log, tids);
    }

    private void resumeTask(Log log, String tid) throws DaemonException {
        List<String> tids = new ArrayList<>();
        tids.add(tid);
        resumeTasks(log, tids);
    }

    private void pauseAllTasks(Log log) throws DaemonException {
        List<String> tids = new ArrayList<>();
        for (Torrent torrent : tasksList(log)) {
            tids.add(torrent.getUniqueID());
        }
        pauseTasks(log, tids);
    }

    private void resumeAllTasks(Log log) throws DaemonException {
        List<String> tids = new ArrayList<>();
        for (Torrent torrent : tasksList(log)) {
            tids.add(torrent.getUniqueID());
        }
        resumeTasks(log, tids);
    }

    private void removeTasks(Log log, List<String> tids) throws DaemonException {
        authGet(log, "SYNO.DownloadStation.Task", "1", "DownloadStation/task.cgi",
                "&method=delete&id=" + Collections2.joinString(tids, ",") + "").ensureSuccess(log);
    }

    private void pauseTasks(Log log, List<String> tids) throws DaemonException {
        authGet(log, "SYNO.DownloadStation.Task", "1", "DownloadStation/task.cgi",
                "&method=pause&id=" + Collections2.joinString(tids, ",")).ensureSuccess(log);
    }

    private void resumeTasks(Log log, List<String> tids) throws DaemonException {
        authGet(log, "SYNO.DownloadStation.Task", "1", "DownloadStation/task.cgi",
                "&method=resume&id=" + Collections2.joinString(tids, ",")).ensureSuccess(log);
    }

    private List<Torrent> tasksList(Log log) throws DaemonException {
        try {
            JSONArray jsonTasks = authGet(log, "SYNO.DownloadStation.Task", "1", "DownloadStation/task.cgi",
                    "&method=list&additional=detail,transfer,tracker").getData(log).getJSONArray("tasks");
            log.d(LOG_NAME, "Tasks = " + jsonTasks.toString());
            List<Torrent> result = new ArrayList<>();
            for (int i = 0; i < jsonTasks.length(); i++) {
                result.add(parseTorrent(i, jsonTasks.getJSONObject(i)));
            }
            return result;
        } catch (JSONException e) {
            throw new DaemonException(ExceptionType.ParsingFailed, e.toString());
        }
    }

    private List<TorrentFile> fileList(Log log, String torrentId) throws DaemonException {
        try {
            List<TorrentFile> result = new ArrayList<>();
            JSONObject jsonTask = authGet(log, "SYNO.DownloadStation.Task", "1", "DownloadStation/task.cgi",
                    "&method=getinfo&id=" + torrentId + "&additional=detail,transfer,tracker,file").getData(log)
                    .getJSONArray("tasks").getJSONObject(0);
            log.d(LOG_NAME, "File list = " + jsonTask.toString());
            JSONObject additional = jsonTask.getJSONObject("additional");
            if (!additional.has("file")) {
                return result;
            }
            JSONArray files = additional.getJSONArray("file");
            for (int i = 0; i < files.length(); i++) {
                JSONObject task = files.getJSONObject(i);
                // @formatter:off
                result.add(new TorrentFile(
                        task.getString("filename"),
                        task.getString("filename"),
                        null,
                        null,
                        task.getLong("size"),
                        task.getLong("size_downloaded"),
                        priority(task.getString("priority"))
                        // @formatter:on
                ));
            }
            return result;
        } catch (JSONException e) {
            throw new DaemonException(ExceptionType.ParsingFailed, e.toString());
        }
    }

    private TorrentDetails torrentDetails(Log log, String torrentId) throws DaemonException {
        List<String> trackers = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            JSONObject jsonTorrent = authGet(log, "SYNO.DownloadStation.Task", "1", "DownloadStation/task.cgi",
                    "&method=getinfo&id=" + torrentId + "&additional=tracker").getData(log).getJSONArray("tasks")
                    .getJSONObject(0);
            JSONObject additional = jsonTorrent.getJSONObject("additional");
            if (additional.has("tracker")) {
                JSONArray tracker = additional.getJSONArray("tracker");
                for (int i = 0; i < tracker.length(); i++) {
                    JSONObject t = tracker.getJSONObject(i);
                    if ("Success".equals(t.getString("status"))) {
                        trackers.add(t.getString("url"));
                    } else {
                        errors.add(t.getString("status"));
                    }
                }
            }
            return new TorrentDetails(trackers, errors);
        } catch (JSONException e) {
            throw new DaemonException(ExceptionType.ParsingFailed, e.toString());
        }
    }

    private Torrent parseTorrent(long id, JSONObject jsonTorrent) throws JSONException, DaemonException {
        JSONObject additional = jsonTorrent.getJSONObject("additional");
        JSONObject detail = additional.getJSONObject("detail");
        JSONObject transfer = additional.getJSONObject("transfer");
        long downloaded = transfer.getLong("size_downloaded");
        int speed = transfer.getInt("speed_download");
        long size = jsonTorrent.getLong("size");
        float eta = (float) (size - downloaded) / speed;
        int totalSeeders = 0;
        int totalLeechers = 0;
        if (additional.has("tracker")) {
            JSONArray tracker = additional.getJSONArray("tracker");
            for (int i = 0; i < tracker.length(); i++) {
                JSONObject t = tracker.getJSONObject(i);
                if ("Success".equals(t.getString("status"))) {
                    totalLeechers += t.getInt("peers");
                    totalSeeders += t.getInt("seeds");
                }
            }
        }
        // @formatter:off
        return new Torrent(
                id,
                jsonTorrent.getString("id"),
                jsonTorrent.getString("title"),
                torrentStatus(jsonTorrent.getString("status")),
                detail.getString("destination"),
                speed,
                transfer.getInt("speed_upload"),
                detail.getInt("connected_seeders"),
                totalSeeders,
                detail.getInt("connected_leechers"),
                totalLeechers,
                (int) eta,
                downloaded,
                transfer.getLong("size_uploaded"),
                size,
                (size == 0) ? 0 : ((float) downloaded / size),
                0,
                jsonTorrent.getString("title"),
                new Date(detail.getLong("create_time") * 1000),
                null,
                "",
                settings.getType()
                // @formatter:on
        );
    }

    private TorrentStatus torrentStatus(String status) {
        if ("downloading".equals(status)) {
            return TorrentStatus.Downloading;
        }
        if ("seeding".equals(status)) {
            return TorrentStatus.Seeding;
        }
        if ("finished".equals(status)) {
            return TorrentStatus.Paused;
        }
        if ("finishing".equals(status)) {
            return TorrentStatus.Paused;
        }
        if ("waiting".equals(status)) {
            return TorrentStatus.Waiting;
        }
        if ("paused".equals(status)) {
            return TorrentStatus.Paused;
        }
        if ("error".equals(status)) {
            return TorrentStatus.Error;
        }
        return TorrentStatus.Unknown;
    }

    private Priority priority(String priority) {
        if ("low".equals(priority)) {
            return Priority.Low;
        }
        if ("normal".equals(priority)) {
            return Priority.Normal;
        }
        if ("high".equals(priority)) {
            return Priority.High;
        }
        return Priority.Off;
    }

    /**
     * Authenticated GET. If no session open, a login authGet will be done before-hand.
     */
    private SynoResponse authGet(Log log, String api, String version, String path, String params)
            throws DaemonException {
        if (sid == null) {
            sid = login(log);
        }
        return new SynoRequest(path, api, version).get(params + "&_sid=" + sid);
    }

    /**
     * Authenticated POST. If no session open, a login authGet will be done before-hand. Params are
     * sent as {@link Part}s,
     */
    private SynoResponse authPost(Log log, String api, String version, String path, Part... params)
            throws DaemonException {
        if (sid == null) {
            sid = login(log);
        }
        return new SynoRequest(path, api, version).post(sid, params);
    }

    private static class SynoResponse {

        private final HttpResponse response;

        public SynoResponse(HttpResponse response) {
            this.response = response;
        }

        public JSONObject getData(Log log) throws DaemonException {
            JSONObject json = getJson(log);
            try {
                if (json.getBoolean("success")) {
                    return json.getJSONObject("data");
                } else {
                    log.e(LOG_NAME, "not a success: " + json.toString());
                    throw new DaemonException(ExceptionType.AuthenticationFailure, json.getString("error"));
                }
            } catch (JSONException e) {
                throw new DaemonException(ExceptionType.ParsingFailed, e.toString());
            }
        }

        public JSONObject getJson(Log log) throws DaemonException {
            try {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    log.e(LOG_NAME, "Error: No entity in HTTP response");
                    throw new DaemonException(ExceptionType.UnexpectedResponse, "No HTTP entity object in response.");
                }
                // Read JSON response
                java.io.InputStream instream = entity.getContent();
                String result = HttpHelper.convertStreamToString(instream);
                JSONObject json;
                json = new JSONObject(result);
                instream.close();
                return json;
            } catch (JSONException e) {
                throw new DaemonException(ExceptionType.UnexpectedResponse, "Bad JSON");
            } catch (IOException e) {
                log.e(LOG_NAME, "getJson error: " + e.toString());
                throw new DaemonException(ExceptionType.AuthenticationFailure, e.toString());
            }
        }

        public void ensureSuccess(Log log) throws DaemonException {
            JSONObject json = getJson(log);
            try {
                if (!json.getBoolean("success")) {
                    throw new DaemonException(ExceptionType.UnexpectedResponse, json.getString("error"));
                }
            } catch (JSONException e) {
                throw new DaemonException(ExceptionType.ParsingFailed, e.toString());
            }
        }

    }

    private class SynoRequest {
        private final String path;
        private final String api;
        private final String version;

        public SynoRequest(String path, String api, String version) {
            this.path = path;
            this.api = api;
            this.version = version;
        }

        public SynoResponse get(String params) throws DaemonException {
            try {
                return new SynoResponse(httpClient.execute(new HttpGet(buildURL(params))));
            } catch (IOException e) {
                throw new DaemonException(ExceptionType.ConnectionError, e.toString());
            }
        }

        // Synolgy POST API seems to work only if all params are sent as POST data despite the docs
        // saying otherwise.
        public SynoResponse post(String sid, Part... params) throws DaemonException {
            try {
                final HttpPost request = new HttpPost(buildUrl());
                final Part[] baseParams = {
                        new Utf8StringPart("_sid", sid),
                        new Utf8StringPart("version", version),
                        new Utf8StringPart("api", api),
                };
                final Part[] allParams = new Part[params.length + baseParams.length];
                System.arraycopy(baseParams, 0, allParams, 0, baseParams.length);
                System.arraycopy(params, 0, allParams, baseParams.length, params.length);

                request.setEntity(new MultipartEntity(allParams));
                return new SynoResponse(httpClient.execute(request));
            } catch (IOException e) {
                throw new DaemonException(ExceptionType.ConnectionError, e.toString());
            }
        }

        private String buildURL(String params) {
            return buildUrl() + "?api=" + api + "&version=" + version + params;
        }

        private String buildUrl() {
            return (settings.getSsl() ? "https://" : "http://") + settings.getAddress() + ":" + settings.getPort() +
                    "/webapi/" + path;
        }

    }

}
