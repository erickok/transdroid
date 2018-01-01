package deluge.api.response;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import deluge.api.DelugeException;

public class TorrentsStatusResponse extends Response
{

    public TorrentsStatusResponse(List<Object> data) throws IOException, DelugeException
    {
        super(data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> getReturnValue()
    {
        return (Map<String, Map<String, Object>>) this.returnValue.get(2);
    }
}
