package com.t2.dataouthandler.dbcache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutPacket;
import com.t2.dataouthandler.DatabaseCacheUpdateListener;
import com.t2.drupalsdk.ServicesClient;
import com.t2.drupalsdk.UserServices;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class DbCache {
	
	private final String TAG = getClass().getName();	

	


	private List<String> mDrupalNodeIdList;	   
	private List<String> mCacheNodeIdList;	   
	private UserServices us;
	private String mRemoteDatabase;
	private Context mContext;
	private DatabaseCacheUpdateListener mDatabaseUpdateListener;
	
	/**
	 * HTTP services client used to talk to Drupal.
	 */
	private ServicesClient mServicesClient;	
	public DatabaseHelper db;	

	

	public DbCache(String remoteDatabase, Context context, DatabaseCacheUpdateListener databaseUpdateListener) {
		mDatabaseUpdateListener = databaseUpdateListener;
		mContext = context;
		mRemoteDatabase = remoteDatabase;
        mServicesClient = new ServicesClient(mRemoteDatabase);
		db = new DatabaseHelper(mContext);        
	}
	
	public void removePacketFromCache(String drupalId) {
		
		int rowsDeleeted = db.deleteSqlPacketByDrupalId(drupalId);
		Log.e(TAG, "rowsDeleted = " + rowsDeleeted);
	}
	
	public void addPacketToCache(String drupalId) {
        UserServices us;
        int nodeNum = 0;
        
        us = new UserServices(mServicesClient);    	

        // First we need to get the packet from the remote database
        
        JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {

			@Override
            public void onSuccess(JSONObject response) {
				
					// Now we've got a response packet that corresponds to
					// the dataOutPacket, we need to convert it to an
					// actual dataOutPacket, then to a sqlPacket so it can be saved in the cache DB
                	DataOutPacket dataOutPacket;
					try {

						// Check to see of the cache has this already, if so then update instead of create
						dataOutPacket = new DataOutPacket(response);

						SqlPacket sqlPacketFromSql = db.getPacketByRecordId(dataOutPacket.mRecordId);
						if (sqlPacketFromSql == null) {
							SqlPacket sqlPacket = new SqlPacket(dataOutPacket);
					        db.createNewSqlPacket(sqlPacket);	
						}
						else {
							SqlPacket sqlPacket = new SqlPacket(dataOutPacket);
							// Need to set sql packet id from the original because this packet was made from the response (no sql packet id)
							sqlPacket.setSqlPacketId(sqlPacketFromSql.getSqlPacketId());
							Log.e(TAG, "updating  dataOutPacket = " + dataOutPacket.toString());
					        int retVal = db.updateSqlPacket(sqlPacket);	
							Log.e(TAG, "retVal = " + retVal);
							SqlPacket updatedSqlPacketFromSql = db.getPacketByRecordId(dataOutPacket.mRecordId);
							Log.e(TAG, "updated  dataOutPacket = " + updatedSqlPacketFromSql.toString());
							
						}
				        
                    	if (mDatabaseUpdateListener != null) {
                    		mDatabaseUpdateListener.remoteDatabasedeGetNodesComplete();
                    	}    				        
						
					} catch (DataOutHandlerException e) {
						Log.e(TAG, e.toString());
						//e.printStackTrace();
					}
            }

			@Override
			public void onSuccess(JSONArray arg0) {
				super.onSuccess(arg0);
			}
            
            @Override
            public void onFailure(Throwable e, JSONObject response) {
                Log.e(TAG, e.toString());
            }
            
            @Override
			public void onFailure(Throwable arg0, JSONArray arg1) {
                Log.e(TAG, arg0.toString());
			}

			@Override
            public void onFinish() {
                Log.d(TAG, "onFinish(addPacketToCache)");
            }
        };        
        
        
    	try {
    		nodeNum = Integer.parseInt(drupalId);
            us.NodeGet(nodeNum, responseHandler);
		} catch (NumberFormatException e1) {
			Log.e(TAG, e1.toString());
		}
		
	}

	public void deletePacketFromCacheWithDeletingStatus(DataOutPacket dataOutPacket) {
		SqlPacket sqlPacket = new SqlPacket(dataOutPacket);
	    sqlPacket.setCacheStatus(SqlPacket.CACHE_DELETING);

	    // Don;t actually delete it, jsut set deleting status
	    db.deleteSqlPacketByRecordId(sqlPacket.getRecordId());
	    
	    
//	    SqlPacket sqlPacketFromSql = db.getPacketByRecordId(dataOutPacket.mRecordId);	   
//	    if (sqlPacketFromSql != null) {
//	    	sqlPacketFromSql.setCacheStatus(SqlPacket.CACHE_DELETING);
//	    	db.updateSqlPacket(sqlPacketFromSql);	    	
//	    	Log.e(TAG, sqlPacketFromSql.toString());
//	    }
	    
	    
	}
	public void deletePacketFromCache(DataOutPacket dataOutPacket) {
		SqlPacket sqlPacket = new SqlPacket(dataOutPacket);
	    db.deleteSqlPacketByRecordId(sqlPacket.getRecordId());
	}
	
	public void deletePacketFromCache(SqlPacket sqlPacket) {
	    sqlPacket.setCacheStatus(SqlPacket.CACHE_DELETING);
	    db.deleteSqlPacketByRecordId(sqlPacket.getRecordId());
	}
	
	
	// --------------------------------------------------
	// New caching method
	// --------------------------------------------------
	public void addPacketToCache(DataOutPacket dataOutcket) {
		SqlPacket sqlPacket = new SqlPacket(dataOutcket);
        db.createNewSqlPacket(sqlPacket);		
	}

	public void addPacketToCacheWithSendingStatus(DataOutPacket dataOutcket) {
		SqlPacket sqlPacket = new SqlPacket(dataOutcket);
		sqlPacket.setCacheStatus(SqlPacket.CACHE_SENDING);
        db.createNewSqlPacket(sqlPacket);		
	}	
	
	public void addPacketToCache(DataOutPacket dataOutcket, String drupalId) {
		SqlPacket sqlPacket = new SqlPacket(dataOutcket);
		sqlPacket.setDrupalId(drupalId);
        db.createNewSqlPacket(sqlPacket);		
	}
	
	
	public List<String> getSqlIdList() {
        mCacheNodeIdList = new ArrayList<String>();
        List<SqlPacket> packetList = db.getPacketList();
		if (packetList != null) {
			for (SqlPacket pkt : packetList) {
				mCacheNodeIdList.add(pkt.getRecordId());
			}
		}
		return mCacheNodeIdList;
	}
	

	/**
	 * Updateds the cache records with drupakl node id's collected from the DB
	 * @param mDrupalIdMap
	 */
	public void updateDrupalIds(HashMap<String, String> mDrupalIdMap) {
        mCacheNodeIdList = new ArrayList<String>();
        List<SqlPacket> packetList = db.getPacketList();
		if (packetList != null) {
			for (SqlPacket pkt : packetList) {
				
				String drupalNid = mDrupalIdMap.get(pkt.getRecordId());
				//Log.e(TAG, "Checking record: " + pkt.getRecordId() + " with drupal node id: " + drupalNid);
				if (drupalNid != null && pkt.getDrupalId() == "") {
					Log.e(TAG, "Updating cache record: " + pkt.getRecordId() + " with drupal node id: " + drupalNid);
					pkt.setDrupalId(drupalNid);
					db.updateSqlPacket(pkt);
					
				}
			}
		}		
		
	} // End updateDrupalIds() 
}
