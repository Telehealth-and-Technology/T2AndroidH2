/*****************************************************************
DbCache

Copyright (C) 2011-2013 The National Center for Telehealth and 
Technology

Eclipse Public License 1.0 (EPL-1.0)

This library is free software; you can redistribute it and/or
modify it under the terms of the Eclipse Public License as
published by the Free Software Foundation, version 1.0 of the 
License.

The Eclipse Public License is a reciprocal license, under 
Section 3. REQUIREMENTS iv) states that source code for the 
Program is available from such Contributor, and informs licensees 
how to obtain it in a reasonable manner on or through a medium 
customarily used for software exchange.

Post your updates and modifications to our GitHub or email to 
t2@tee2.org.

This library is distributed WITHOUT ANY WARRANTY; without 
the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the Eclipse Public License 1.0 (EPL-1.0)
for more details.
 
You should have received a copy of the Eclipse Public License
along with this library; if not, 
visit http://www.opensource.org/licenses/EPL-1.0

*****************************************************************/
package com.t2.dataouthandler.dbcache;

import java.net.MalformedURLException;
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

	


	private UserServices us;
	private String mRemoteDatabase;
	private Context mContext;
	private DatabaseCacheUpdateListener mDatabaseUpdateListener;
	
	/**
	 * HTTP services client used to talk to Drupal.
	 */
	private ServicesClient mServicesClient;	
	public DatabaseHelper db;	

	

	public DbCache(String remoteDatabase, Context context, DatabaseCacheUpdateListener databaseUpdateListener) throws MalformedURLException, DataOutHandlerException {
		mDatabaseUpdateListener = databaseUpdateListener;
		mContext = context;
		mRemoteDatabase = remoteDatabase;
        mServicesClient = new ServicesClient(mRemoteDatabase);
		db = new DatabaseHelper(mContext);        
	}

	public void deletePacketFromCacheWithDeletingStatus(DataOutPacket dataOutPacket) {
		synchronized(db) {
			SqlPacket sqlPacket = new SqlPacket(dataOutPacket);
		    sqlPacket.setCacheStatus(GlobalH2.CACHE_DELETING);
	
		    db.deleteSqlPacketByRecordId(sqlPacket.getRecordId());
		}	    
	}
	public void deletePacketFromCache(DataOutPacket dataOutPacket) {
		synchronized(db) {
			SqlPacket sqlPacket = new SqlPacket(dataOutPacket);
			db.deleteSqlPacketByRecordId(sqlPacket.getRecordId());
		}
	}
	
	public void deletePacketFromCache(SqlPacket sqlPacket) {
		synchronized(db) {
			sqlPacket.setCacheStatus(GlobalH2.CACHE_DELETING);
			db.deleteSqlPacketByRecordId(sqlPacket.getRecordId());
		}
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
	
	public void addPacketToCache(DataOutPacket dataOutcket, String recordId) {
		SqlPacket sqlPacket = new SqlPacket(dataOutcket);
		sqlPacket.setRecordId(recordId);
		synchronized(db) {
			db.createNewSqlPacket(sqlPacket);
		}
	}
	
	public List<String> getSqlNodeIdList() {
		ArrayList<String> cacheNodeIdList = new ArrayList<String>();
        List<SqlPacket> packetList = db.getPacketListAsSqlPacket();
		if (packetList != null) {
			for (SqlPacket pkt : packetList) {
					cacheNodeIdList.add(pkt.getDrupalId());
			}
		}
		return cacheNodeIdList;
	}
		
	/**
	 * Requests a list of DataOutPackets from the local cache
	 * @return - List of DataOutPackets in the local cache
	 */	public ArrayList<DataOutPacket> getPacketList() {
		synchronized(db) {
			return db.getPacketList();
		}
	}
	
	/**
	 * Requests a list of DataOutPackets from the local cache
	 * @param structureTypes - List of types to filter on
	 * @return - List of DataOutPackets in the local cache
	 */
	public ArrayList<DataOutPacket> getPacketList(List<String> structureTypes) {
		synchronized(db) {
			return db.getPacketList(structureTypes);
		}
	}
	
	/**
	 * Requests a list of DataOutPackets from the local cache
	 * @param whereClause - where clause to use in SQL statement
	 * @return - List of DataOutPackets in the local cache
	 */
	public ArrayList<DataOutPacket> getPacketList(String whereClause) {
		synchronized(db) {
			return db.getPacketList(whereClause);
		}
	}	
	 
	public SqlPacket getPacketByRecordId(String recordId) {
		synchronized(db) {
			return db.getPacketByRecordId(recordId);
		}		
	}
	 
	public SqlPacket getPacketByDrupalId(String nodeId) {
		synchronized(db) {
			return db.getPacketByDrupalId(nodeId);
		}		
	}
	 

	
	public int updateSqlPacket(SqlPacket packet) {
		synchronized(db) {
			return db.updateSqlPacket(packet);
		}		
	}
	
}
