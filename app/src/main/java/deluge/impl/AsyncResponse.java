package deluge.impl;

import java.util.ArrayList;
import java.util.List;

import deluge.api.ResponseCallback;

public class AsyncResponse<R, E extends Exception> extends ResponseCallback<R, E>
{
    private final List<ResponseCallback<R, E>> callbacks;

    public AsyncResponse()
    {
        this.callbacks = new ArrayList<ResponseCallback<R, E>>();
    }

    public void addCallback(ResponseCallback<R, E> callback)
    {
        this.callbacks.add(callback);
    }

    @Override
    public void onError(E error)
    {
        for (final ResponseCallback<R, E> cb : this.callbacks)
        {
            cb.onError(error);
        }
    }

    @Override
    public void onResponse(R response)
    {
        for (final ResponseCallback<R, E> cb : this.callbacks)
        {
            cb.onResponse(response);
        }
    }

    @Override
    public void onServerError(Exception exception)
    {
        for (final ResponseCallback<R, E> cb : this.callbacks)
        {
            cb.onServerError(exception);
        }
    }

    public void removeCallback(ResponseCallback<R, E> callback)
    {
        this.callbacks.remove(callback);
    }

    public void then(ResponseCallback<R, E> callback)
    {
        addCallback(callback);
    }
}
