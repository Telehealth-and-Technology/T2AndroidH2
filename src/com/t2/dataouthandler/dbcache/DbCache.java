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
import com.t2.dataouthandler.GlobalH2;
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
	
	public void deletePacketFromCacheWithDeletingStatus(DataOutPacket dataOutPacket) {
		SqlPacket sqlPacket = new SqlPacket(dataOutPacket);
	    sqlPacket.setCacheStatus(GlobalH2.CACHE_DELETING);

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
	    sqlPacket.setCacheStatus(GlobalH2.CACHE_DELETING);
	    db.deleteSqlPacketByRecordId(sqlPacket.getRecordId());
	}
	
	
	// --------------------------------------------------
	// New caching method
	// --------------------------------------------------
	public void addPacketToCache(DataOutPacket dataOutcket) {
		SqlPacket sqlPacket = new SqlPacket(dataOutcket);
		synchronized(db) {
			db.createNewSqlPacket(sqlPacket);
		}
	}

	public void addPacketToCacheWithSendingStatus(DataOutPacket dataOutcket) {
		SqlPacket sqlPacket = new SqlPacket(dataOutcket);
		sqlPacket.setCacheStatus(GlobalH2.CACHE_SENDING);
		synchronized(db) {		
			db.createNewSqlPacket(sqlPacket);
		}
	}	
	
	public void addPacketToCache(DataOutPacket dataOutcket, String drupalId) {
		SqlPacket sqlPacket = new SqlPacket(dataOutcket);
		sqlPacket.setDrupalId(drupalId);
		synchronized(db) {
			db.createNewSqlPacket(sqlPacket);
		}
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
	
	public ArrayList<DataOutPacket> getPacketListDOP() {
		synchronized(db) {
			return db.getPacketListDOP();
		}
	}
	
	
	public SqlPacket getPacketByRecordId(String recordId) {
		synchronized(db) {
			return db.getPacketByRecordId(recordId);
		}		
	}
	
	public int updateSqlPacket(SqlPacket packet) {
		synchronized(db) {
			return db.updateSqlPacket(packet);
		}		
	}
	
}
