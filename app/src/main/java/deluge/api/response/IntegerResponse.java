package deluge.api.response;

import java.io.IOException;
import java.util.List;

import deluge.api.DelugeException;

public class IntegerResponse extends Response
{

    public IntegerResponse(List<Object> data) throws IOException, DelugeException
    {
        super(data);
    }

    public Integer getReturnValue()
    {
        return (Integer) this.returnValue.get(2);
    }
}
