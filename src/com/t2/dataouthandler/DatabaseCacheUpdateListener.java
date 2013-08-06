package com.t2.dataouthandler;

import java.util.HashMap;

public interface DatabaseCacheUpdateListener {
    void remoteDatabaseFailure(String msg);
    void remoteDatabaseCreateUpdateComplete(DataOutPacket pkt);
    void remoteDatabaseDeleteComplete(DataOutPacket pkt);
    void remoteDatabaseSyncComplete(HashMap<String, String> remoteContentsMap);
}
