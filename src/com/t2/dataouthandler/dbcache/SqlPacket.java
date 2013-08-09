package com.t2.dataouthandler.dbcache;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import android.util.Log;

import com.t2.dataouthandler.GlobalH2;

import com.t2.dataouthandler.DataOutPacket;

public class SqlPacket {

	private static final String TAG = SqlPacket.class.getSimpleName();

	
	private String sqlPacketId = "";
	private String packetJson = "";
	private String recordId = "";
	private String drupalId = "";
	private String changedDate = "";
	private String structureType = "";

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
		drupalId = dataOutPacket.mDrupalNid;
		packetJson = dataOutPacket.toString();
		sqlPacketId = dataOutPacket.mSqlPacketId;
		changedDate = dataOutPacket.mChangedDate;
		structureType = dataOutPacket.mStructureType;
		
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
				packetJson = result.substring(0, result.length()-1) + "}";
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


	public String getDrupalId() {
		return drupalId;
	}


	public void setDrupalId(String drupalId) {
		this.drupalId = drupalId;
	}


	public void setPacket(String packetJson) {
		this.packetJson = packetJson;
	}


	public String getRecordId() {
		return recordId;
	}


	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}

	
	public int getCacheStatus() {
		return cacheStatus;
	}

	public void setCacheStatus(int cache_Status) {
		this.cacheStatus = cache_Status;
	}

	public String toString() {
		return "structureType: " + structureType + "cacheStatus: " + cacheStatus + " packetId: " + sqlPacketId + ", recordId: " + recordId + ", drupalId: " + drupalId + ", changedDate: " + changedDate + ", packet: " + packetJson;
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
