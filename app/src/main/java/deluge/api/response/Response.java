package deluge.api.response;

import java.util.List;

import deluge.api.DelugeException;

public abstract class Response
{
    protected List<Object> returnValue;

    protected final int    RPC_RESPONSE = 1;
    protected final int    RPC_ERROR    = 2;
    protected final int    RPC_EVENT    = 3;

    public Response(List<Object> decodedObj) throws DelugeException
    {
        rawData(decodedObj);
    }

    public int getMessageType()
    {
        return (Integer) this.returnValue.get(0);
    }

    public int getRequestId()
    {
        return (Integer) this.returnValue.get(1);
    }

    public abstract Object getReturnValue();

    private void process() throws DelugeException
    {
        if (getMessageType() == this.RPC_ERROR)
        {
            @SuppressWarnings("unchecked")
            final List<String> params = (List<String>) this.returnValue.get(2);
            final String type = params.get(0);
            final String msg = params.get(1);
            final String trace = params.get(2);

            throw new DelugeException(type, msg, trace);
        }
    }

    public void rawData(List<Object> decodedObj) throws DelugeException
    {
        this.returnValue = decodedObj;
        process();
    }

    @Override
    public String toString()
    {
        String str = "Response { ";

        str += this.returnValue.toString();

        str += " }";
        return str;
    }
}
