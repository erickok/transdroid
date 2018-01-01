package deluge.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import deluge.api.response.ReturnType;

public class OngoingRequests
{
    public static void put(int requestId, ReturnType type, Object future)
    {
        final OngoingRequest ongoing = new OngoingRequest(type, future);
        OngoingRequests.mOngoingRequests.put(requestId, ongoing);
    }

    public static OngoingRequest remove(int requestId)
    {
        return OngoingRequests.mOngoingRequests.remove(requestId);
    }

    private static Map<Integer, OngoingRequest> mOngoingRequests = new ConcurrentHashMap<Integer, OngoingRequest>();

}
