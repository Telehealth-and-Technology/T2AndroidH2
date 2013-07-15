package com.t2.dataouthandler;

public interface DatabaseCacheUpdateListener {
    void remoteDatabaseFailure(String msg);
    void remoteDatabasedeGetNodesComplete();
    void remoteDatabaseCreateUpdateComplete(DataOutPacket pkt);
    void remoteDatabaseDeleteComplete(DataOutPacket pkt);

}
