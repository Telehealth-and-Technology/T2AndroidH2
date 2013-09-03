/*****************************************************************
SqlPacket

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

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import android.util.Log;

import com.t2.dataouthandler.GlobalH2;

import com.t2.dataouthandler.DataOutPacket;

public class SqlPacket {

	private static final String TAG = SqlPacket.class.getSimpleName();

	
	
	// Drupal primary keys - never written to sql packet - 
	// always present regardless of remote database type
	// These also correspond to the keys in Drupal that are present in the summary record
	private String recordId = "";
	private String changedDate = "";			// Unix time GMT
	private String structureType = "";
	private String title = "";

	// Drupal Secondary keys
	private String packetJson = "";

	private String sqlPacketId = "";

	
	private int cacheStatus = GlobalH2.CACHE_IDLE;


	public SqlPacket() {
	}
	
	private String addQuotes(String string) {
		return "\"" + string + "\"";
	}
	
	/**
	 * Construct a SqlPacket from a dataOutPacket
	 * 
	 * @param dataOutPacket Packet to construct from
	 */
	public SqlPacket(DataOutPacket dataOutPacket) {

    	//Log.e(TAG, "Creating Sql packet for " + dataOutPacket.toString());
		
		recordId = dataOutPacket.mRecordId;
		changedDate = dataOutPacket.mChangedDate;
		structureType = dataOutPacket.mStructureType;
		packetJson = dataOutPacket.toString();
		title = dataOutPacket.mTitle;
		sqlPacketId = dataOutPacket.mSqlPacketId;
		
		String result = "";
		
		
		
		result += "{";
		   Iterator it = dataOutPacket.mItemsMap.entrySet().iterator();
		    while (it.hasNext()) {
		        try {
					Map.Entry pairs = (Map.Entry)it.next();
					String key = (String) pairs.getKey();
					Object objKey = pairs.getKey();
					Object objValue = pairs.getValue();
					
					if (objValue instanceof Vector) {
					    result += addQuotes(objKey.toString()) + ":"; 
					    Vector v = (Vector) objValue;
						String vectorString = "";
					    if (v.size() == 0) {
					    	result += addQuotes("[]") + ",";
					    }
					    else {
					    	result += "[";
					    	for (Object obj : (Vector) objValue) {
					    		vectorString += addQuotes(obj.toString()) + ",";
					    	}
					    	result += vectorString.substring(0, vectorString.length() - 1) + "],";
					    }
						
					}
					else {
					    result += addQuotes(objKey.toString()) + ":" + addQuotes(objValue.toString()) + ","; 
					}
				} catch (Exception e) {
					Log.e(TAG, e.toString());
					e.printStackTrace();
				}
		        
		        
		        
//		        if (pairs.getValue() instanceof Integer) {
//		        	result += "{INTEGER} + ";
//		        }
//		        if (pairs.getValue() instanceof String) {
//		        	result += "{String} + ";
//		        }
//		        if (pairs.getValue() instanceof Long) {
//		        	result += "{Long} + ";
//		        }
//		        if (pairs.getValue() instanceof Double) {
//		        	result += "{Long} + ";
//		        }
//		        if (pairs.getValue() instanceof Vector) {
//		        	result += "{Vector} + ";
//		        }
		    }				
		    try {
		    	
		    	if (result.equalsIgnoreCase("{")) {
		    		packetJson = "{}";
		    	}
		    	else {
					packetJson = result.substring(0, result.length()-1) + "}";
		    	}
			} catch (Exception e) {
				Log.e(TAG, e.toString());
				e.printStackTrace();
			}
	}
	
	
	public String getPacketJson() {
		return packetJson;
	}


	public String getSqlPacketId() {
		return sqlPacketId;
	}


	public void setSqlPacketId(String packetId) {
		
		this.sqlPacketId = packetId;
	}


	public String getRecordId() {
		return recordId;
	}


	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}


	public void setPacket(String packetJson) {
		this.packetJson = packetJson;
	}


	public int getCacheStatus() {
		return cacheStatus;
	}

	public void setCacheStatus(int cache_Status) {
		this.cacheStatus = cache_Status;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String toString() {
		return "title: " + title + " structureType: " + structureType + " cacheStatus: " + cacheStatus + " packetId: " + sqlPacketId + ", recordId: " + recordId + ", changedDate: " + changedDate + ", packet: " + packetJson;
	}

	public String getChangedDate() {
		return changedDate;
	}

	public void setChangedDate(String changed_At) {
		changedDate = changed_At;
	}

	public String getStructureType() {
		return structureType;
	}

	public void setStructureType(String structureType) {
		this.structureType = structureType;
	}
	
	

}
