package deluge.impl;

public class DelugeClient
{
    public static DelugeSession getSession(String host)
    {
        final String[] parts = host.split(":");
        final int port = parts.length < 2 ? DelugeClient.DEFAULT_PORT : Integer.parseInt(parts[1]);
        return DelugeClient.getSession(parts[0], port);
    }

    public static DelugeSession getSession(String host, int port)
    {
        return DelugeSession.connect(host, port);
    }

    public static DelugeSession getSessionDefault()
    {
        return DelugeClient.getSession("127.0.0.1", DelugeClient.DEFAULT_PORT);
    }

    public static final int DEFAULT_PORT = 58846;

}
