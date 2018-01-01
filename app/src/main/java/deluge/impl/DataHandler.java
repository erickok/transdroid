package deluge.impl;

import java.io.IOException;
import java.util.List;

import se.dimovski.rencode.Rencode;
import deluge.api.DelugeException;
import deluge.api.DelugeFuture;
import deluge.api.response.IntegerResponse;
import deluge.api.response.Response;
import deluge.api.response.TorrentsStatusResponse;
import deluge.impl.net.Session.DataCallback;

public class DataHandler implements DataCallback
{

    @SuppressWarnings("unchecked")
    public void dataRecived(byte[] data)
    {
        Integer requestId = null;
        List<Object> decodedObj;
        try
        {
            decodedObj = (List<Object>) Rencode.decode(data);
            requestId = (Integer) decodedObj.get(1);

            sendSpecificResponse(requestId, decodedObj);
        }
        catch (final IOException e)
        {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void sendSpecificResponse(Integer requestId, List<Object> decodedObj)
    {

        final OngoingRequest req = OngoingRequests.remove(requestId);
        
        try
        {
            switch (req.getType())
            {
                case INTEGER:
                {
                    final DelugeFuture<IntegerResponse> fut = (DelugeFuture<IntegerResponse>) req.getFuture();
                    fut.onResponse(new IntegerResponse(decodedObj));
                }
                    break;
                case TORRENTS_STATUS:
                {
                    final DelugeFuture<TorrentsStatusResponse> fut = (DelugeFuture<TorrentsStatusResponse>) req
                            .getFuture();
                    fut.onResponse(new TorrentsStatusResponse(decodedObj));
                }
                    break;
                default:
                {
                    throw new UnsupportedOperationException("Unknown Request: " + req.getType());
                }
            }
        }
        catch (final DelugeException e)
        {
            final DelugeFuture<Response> fut = (DelugeFuture<Response>) req.getFuture();
            fut.onError(e);
        }
        catch (final Exception e)
        {
            final DelugeFuture<Response> fut = (DelugeFuture<Response>) req.getFuture();
            fut.onServerError(e);
        }
    }
    
    private void onError()
    {
        
    }

}
