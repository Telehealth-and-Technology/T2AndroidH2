package com.t2.dataouthandler.dbcache;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.t2.dataouthandler.DataOutPacket;

public class SqlPacket {

	private String sqlPacketId = "";
	private String packetJson = "";
	private String recordId = "";
	private String drupalId = "";


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
		recordId = dataOutPacket.mRecordId;
		drupalId = dataOutPacket.mDrupalNid;
		packetJson = dataOutPacket.toString();
		sqlPacketId = dataOutPacket.mSqlPacketId;
		
		String result = "";
		
		
		
		result += "{";
		   Iterator it = dataOutPacket.mItemsMap.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        String key = (String) pairs.getKey();
		        Object objKey = pairs.getKey();
		        Object objValue = pairs.getValue();
		        
		        if (objValue instanceof Vector) {
			        result += addQuotes(objKey.toString()) + ":"; 
		        	result += "[";
		        	String vectorString = "";
		        	for (Object obj : (Vector) objValue) {
		        		vectorString += addQuotes(obj.toString()) + ",";
		        	}
		        	result += vectorString.substring(0, vectorString.length() - 1) + "],";
		        }
		        else {
			        result += addQuotes(objKey.toString()) + ":" + addQuotes(objValue.toString()) + ","; 
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


	
	public String toString() {
		return "packetId: " + sqlPacketId + ", recordId: " + recordId + ", drupalId: " + drupalId + ", packet: " + packetJson;
	}

}
