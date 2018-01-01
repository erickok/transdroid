package deluge.api;

public abstract class ResponseCallback<R, E extends Exception>
{
    public void onError(E error)
    {
        error.printStackTrace();
    }

    public abstract void onResponse(R response);

    public void onServerError(Exception exception)
    {
        exception.printStackTrace();
    }
}
