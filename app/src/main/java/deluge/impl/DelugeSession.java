package deluge.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import deluge.api.DelugeFuture;
import deluge.api.response.IntegerResponse;
import deluge.api.response.ReturnType;
import deluge.api.response.TorrentsStatusResponse;
import deluge.impl.net.Session;
import deluge.impl.net.TorrentField;

public class DelugeSession
{
    public static DelugeSession connect(String host, int port)
    {
        final Session session = new Session(host, port);
        try
        {
            session.listen(new DataHandler());
        }
        catch (final IOException e)
        {
            e.printStackTrace();
        }
        return new DelugeSession(session);
    }

    private final Session session;

    private DelugeSession(Session session)

    {
        this.session = session;
    }

    public DelugeFuture<TorrentsStatusResponse> getTorrentsStatus(Map<Object, Object> filter, TorrentField[] fields)
    {
        final DelugeFuture<TorrentsStatusResponse> future = new DelugeFuture<TorrentsStatusResponse>();
        final Request request = RequestFactory.getTorrentsStatus(filter, fields);
        send(request, ReturnType.TORRENTS_STATUS, future);
        return future;
    }

    public DelugeFuture<IntegerResponse> login(String username, String password)
    {
        final DelugeFuture<IntegerResponse> future = new DelugeFuture<IntegerResponse>();
        final Request request = new Request("daemon.login", Util.objects(username, password));
        send(request, ReturnType.INTEGER, future);
        return future;
    }

    public DelugeFuture<IntegerResponse> pauseTorrent(List<String> torrentIds)
    {
        final DelugeFuture<IntegerResponse> future = new DelugeFuture<IntegerResponse>();
        final Request request = new Request("core.pause_torrent", Util.objects(torrentIds));
        send(request, ReturnType.INTEGER, future);
        return future;
    }

    public DelugeFuture<IntegerResponse> resumeTorrent(List<String> torrentIds)
    {
        final DelugeFuture<IntegerResponse> future = new DelugeFuture<IntegerResponse>();
        final Request request = new Request("core.resume_torrent", Util.objects(torrentIds));
        send(request, ReturnType.INTEGER, future);
        return future;
    }

    public DelugeFuture<IntegerResponse> addTorrentFile(String name, String encodedContents, Map<String, Object> options)
    {
        final DelugeFuture<IntegerResponse> future = new DelugeFuture<IntegerResponse>();
        Request request = new Request("core.add_torrent_file", Util.objects(name, encodedContents, options));
        send(request, ReturnType.INTEGER, future);
        return future;
    }
    
    public DelugeFuture<IntegerResponse> removeTorrent(String torrentId, Boolean removeData)
    {
        final DelugeFuture<IntegerResponse> future = new DelugeFuture<IntegerResponse>();
        Request request = new Request("core.remove_torrent", Util.objects(torrentId, removeData));
        send(request, ReturnType.INTEGER, future);
        return future;
    }
    
    private void send(Request request, ReturnType type, Object future)
    {
        OngoingRequests.put(request.getRequestId(), type, future);

        try
        {
            this.session.send(request.toByteArray());
        }
        catch (final IOException e)
        {
            e.printStackTrace();
        }
    }

}
