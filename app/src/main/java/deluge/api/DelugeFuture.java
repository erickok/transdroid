package deluge.api;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import deluge.impl.AsyncResponse;

public class DelugeFuture<V> extends AsyncResponse<V, DelugeException> implements Future<V>
{
    private final CountDownLatch latch = new CountDownLatch(1);
    private V                    value;

    public DelugeFuture()
    {
    }

    public boolean cancel(boolean mayInterruptIfRunning)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public V get() throws InterruptedException, ExecutionException
    {
        this.latch.await();
        return this.value;
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
    {
        if (!this.latch.await(timeout, unit))
        {
            throw new TimeoutException();
        }
        return this.value;
    }

    public boolean isCancelled()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isDone()
    {
        return this.value != null;
    }

    @Override
    public void onError(DelugeException error)
    {
        setValue(null);
        super.onError(error);
    }

    @Override
    public void onResponse(V response)
    {
        setValue(response);
        super.onResponse(response);
    }

    @Override
    public void onServerError(Exception exception)
    {
        setValue(null);
        super.onServerError(exception);
    }

    public void setValue(V val)
    {
        this.value = val;
        this.latch.countDown();
    }
}
