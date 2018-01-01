package deluge.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import se.dimovski.rencode.Rencode;

public class Request
{
    private static AtomicInteger      requestCounter = new AtomicInteger();

    private final Integer             requestId;
    private final String              method;
    private final Object[]            args;
    private final Map<Object, Object> kwargs;

    public Request(String method)
    {
        this(method, new Object[0]);
    }

    public Request(String method, Object[] args)
    {
        this(method, args, new HashMap<Object, Object>());
    }

    public Request(String method, Object[] args, Map<Object, Object> kwargs)
    {
        this.requestId = Request.requestCounter.getAndIncrement();
        this.method = method;
        this.args = args;
        this.kwargs = kwargs;
    }

    public Integer getRequestId()
    {
        return this.requestId;
    }

    public byte[] toByteArray()
    {
        final Object obj = new Object[] { new Object[] { this.requestId, this.method, this.args, this.kwargs } };
        try
        {
            return Rencode.encode(obj);
        }
        catch (final IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
