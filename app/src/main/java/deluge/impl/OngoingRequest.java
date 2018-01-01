package deluge.impl;

import deluge.api.response.ReturnType;

public class OngoingRequest
{
    private final ReturnType type;
    private final Object     future;

    public OngoingRequest(ReturnType type, Object future)
    {
        this.type = type;
        this.future = future;
    }

    public Object getFuture()
    {
        return this.future;
    }

    public ReturnType getType()
    {
        return this.type;
    }

}
