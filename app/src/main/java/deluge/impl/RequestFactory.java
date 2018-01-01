package deluge.impl;

import java.util.Map;

import deluge.impl.net.TorrentField;

public class RequestFactory
{

    public static Request getSessionState()
    {
        return new Request("core.get_session_state");
    }

    public static Request getTorrentsStatus()
    {
        return RequestFactory.getTorrentsStatus(null, null);
    }

    public static Request getTorrentsStatus(Map<Object, Object> filter)
    {
        return RequestFactory.getTorrentsStatus(filter, null);
    }

    public static Request getTorrentsStatus(Map<Object, Object> filter, TorrentField[] fields)
    {
        Object[] fieldNames = new Object[0];
        if (fields != null)
        {
            fieldNames = new Object[fields.length];
            for (int i = 0; i < fields.length; i++)
            {
                fieldNames[i] = fields[i].toString();
            }
        }

        return new Request("core.get_torrents_status", Util.objects(filter, fieldNames));
    }

    public static Request getTorrentsStatus(TorrentField[] fields)
    {
        return RequestFactory.getTorrentsStatus(null, null);
    }
}
