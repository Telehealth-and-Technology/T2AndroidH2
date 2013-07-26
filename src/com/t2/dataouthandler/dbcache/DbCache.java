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
	
	
	
	public void updateCacheFromDb()
	{
		new AsyncTask<Void, Integer, Boolean>() { 
			protected void onPostExecute(Boolean result) {
			}

			@Override
			protected Boolean doInBackground(Void... params) {
		        us = new UserServices(mServicesClient);

		         JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {
		         	
		         	@Override
		 			protected void handleSuccessJsonMessage(Object arg0) {
		         		// Accumulate a list of all of the current node id's in mDrupalNodeIdList
		         		JSONArray array = (JSONArray) arg0;
		         		Log.e(TAG, "Drupal Node Summary: " + array.toString());
		         		
		         		mDrupalNodeIdList = new ArrayList<String>();	        		
		                 for (int i = 0; i < array.length(); i++) {
		                 	JSONObject jObject  = (JSONObject) array.opt(i);
		                 	try {
		 						String nodeId = (String) jObject.get("nid");
		 						mDrupalNodeIdList.add(nodeId);
		 					} catch (JSONException e) {
		 						e.printStackTrace();
		 					}
		                 }
		                 
		                 // At this point mDrupalNodeIdList has a list of all of the Id's
		                 // of records in the drupal Database.
		                 
		         		// Now get the corresponding list of ids of packets in the Cache
		                // into mCacheNodeIdList
		                mCacheNodeIdList = new ArrayList<String>();
		                List<SqlPacket> packetList = db.getPacketList();
		        		if (packetList != null) {
		        			for (SqlPacket pkt : packetList) {
		        				mCacheNodeIdList.add(pkt.getDrupalId());
		        			}
		        		}

		                // Now do something with this list
		        		// 1 - For each packet in Drupal not in Cache -> Add packet to cache
		        		// 2 - For each packet in Cache not in Drupal -> Remove packet from cache  
		        		for (String id : mDrupalNodeIdList) {
		        			if (!mCacheNodeIdList.contains(id)) {
		        				Log.d(TAG, "Adding packet to cache: " + id);
		        				addPacketToCache(id);
		        				
		        				
		        			}
		        		}
		        		
		        		for (String id : mCacheNodeIdList) {
		        			if (!mDrupalNodeIdList.contains(id)) {
		        				Log.d(TAG, "Removing packet to cache: " + id);
		        				int rowsDeleeted = db.deleteSqlPacketByDrupalId(id);
		        				Log.d(TAG, "rowsDeleeted =  " + rowsDeleeted);
		        			}
		        		}		        		

		        		// This is only for the packets already in the cache 		        		
		        		if (true) {
	                    	if (mDatabaseUpdateListener != null) {
	                    		mDatabaseUpdateListener.remoteDatabasedeGetNodesComplete();
	                    	}    		        			
		        		}
		                 
		                 
		 			}

		 			@Override
		             public void onSuccess(JSONObject response) {
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
		                 Log.d(TAG, "onFinish()");
		             	
		             }
		         };        
		         
		         us.NodeGet(responseHandler);				
				return null;
			}
		}.execute(); // start the background processing
	}	
	
	public void removePacketFromCache(String drupalId) {
		
		int rowsDeleeted = db.deleteSqlPacketByDrupalId(drupalId);
		Log.e(TAG, "rowsDeleeted = " + rowsDeleeted);
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

	public void deletePacketFromCache1(DataOutPacket dataOutcket) {
		SqlPacket sqlPacket = new SqlPacket(dataOutcket);
		db.deleteSqlPacketByRecordId(sqlPacket.getRecordId());
	}

	
	
	
	// --------------------------------------------------
	// New caching method
	// --------------------------------------------------
	public void addPacketToCache1(DataOutPacket dataOutcket) {
		SqlPacket sqlPacket = new SqlPacket(dataOutcket);
        db.createNewSqlPacket(sqlPacket);		
	}

	
	public void addPacketToCache1(DataOutPacket dataOutcket, String drupalId) {
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
		
	}
	
	
	
	
}
