/*****************************************************************
DataOutPacket

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
package com.t2.dataouthandler;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.ParseException;
import android.os.Build;
import android.util.Log;

import com.t2.dataouthandler.DataOutHandlerTags;
import com.t2.dataouthandler.dbcache.SqlPacket;

// TODO: update for all primary types

public class DataOutPacket implements Serializable {

	private final String TAG = getClass().getName();	
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");    

    // Official Record Fields

	// Drupal primary keys
	public String mTitle; 			// USED TO BE Same as mRecordId - now they are two entities
	public String mDrupalNid = "";		// This is assigned by Drupal ("NID")
	public String mStructureType;
	public String mChangedDate;		//

    
    // Primary keys
	public String mRecordId;
	public long mTimeStamp;
	public String mSqlPacketId;		// This is the SQLite row number

	
	// Additional record properties (Secondary keys)
	public HashMap<String, Object> mItemsMap = new HashMap<String, Object>();

	// Private record properties
	public String mLoggingString;
	public String mQueuedAction = "C";		// Assume all actions are Create unless specifically set otherwise
	
	
	public int mCacheStatus = GlobalH2.CACHE_IDLE;	
	

	/**
	 * Construct a DataOutPacket from a SQL packet
	 * @param drupalObject
	 * @throws DataOutHandlerException 
	 */
	public DataOutPacket(SqlPacket sqlPacket) throws DataOutHandlerException {

		if (sqlPacket == null) {
			throw new DataOutHandlerException("null sql packet");			
		}
		
		this.mRecordId = sqlPacket.getRecordId();
		this.mDrupalNid = sqlPacket.getDrupalId();
		this.mSqlPacketId = sqlPacket.getSqlPacketId();
		this.mChangedDate = sqlPacket.getChangedDate();
		this.mCacheStatus = sqlPacket.getCacheStatus();
		this.mStructureType = sqlPacket.getStructureType();
		this.mTitle = sqlPacket.getTitle();

		
		try {
			JSONObject mainObject = new JSONObject(sqlPacket.getPacketJson());
			
	        Iterator<String> keys = mainObject.keys();
	        while (keys.hasNext()) {
	            String key = keys.next();
	            try {
	                Object val = mainObject.get(key);
	                if (val instanceof JSONArray) {
	                	JSONArray vals = (JSONArray) val;
	            		Vector<String> taskVector = new Vector<String>();
	                	
	                	for (int i = 0; i < vals.length(); i++) {
	                		String s = vals.getString(i);
		            		taskVector.add(s);
	                	}
	                	add(key, taskVector);

	                
	                } else if (val instanceof JSONObject) {
	                	throw new DataOutHandlerException("JSON Object not valid at this point");	                	
	                } else if (val instanceof String) {
	                	add(key, (String) val);
	                	
	                	// Set up any specific members
	                	if (key.equalsIgnoreCase(DataOutHandlerTags.TIME_STAMP)) {
	                		mTimeStamp = Long.parseLong((String) val);
	                	}
	                }
	            } catch (JSONException e) {
	                throw new RuntimeException("Unexpected", e);
	            }
	        }		
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}	
	
	
	

	/**
	 * Construct a DataOutPacket from a JSON string (supplied by Drupal)
	 *  A drupal object contains a number of primary and secondary keys
	 *  The primary keys are in the first tier of the hierarchy and set 
	 *  members of the dataOutPacket directly
	 *  e.g. "nid": "1211" Sets mDrupalNid
	 *  
	 *  Secondary keys are encapsulated in a further hierarchy and are
	 *  distinguished by having  "field_" prepended to them. These secondary
	 *  keys go into the mItemsMap member of the dataOutPacket
	 *  
	 *  e.g. 
	 *   "field_platform": {
     *   	"und": [
     *       	{
     *          	"value": "Android Modified 8/12/2013 9:08 AM",
     *           	"format": null,
     *           	"safe_value": "Android Modified 8/12/2013 9:08 AM"
     *       	}
     *   	]
     *   },
	 *  
	 *  
	 *  
	 * @param drupalObject
	 * @throws DataOutHandlerException 
	 */
	public DataOutPacket(JSONObject drupalObject) throws DataOutHandlerException {
		
			String itemValue;
			String itemKey;


			String recordType;
			try {
				recordType = drupalObject.getString("type");
				if (!GlobalH2.isValidRecordType(recordType)) {			// Cheap trick to see if record is is good
    				throw new DataOutHandlerException("Unrecognizable as DataOutPacket");
    		}			
			} catch (JSONException e2) {
				throw new DataOutHandlerException("Unrecognizable as DataOutPacket");
			}
			
			// Check for and add primary keys
			try {
				mTitle = drupalObject.getString("title");
				mDrupalNid = drupalObject.getString("nid");
				mStructureType = drupalObject.getString("type");
				mChangedDate = drupalObject.getString("changed");

				// TODO: Probably not necessary - but it breaks the build if you remove them
				// probably because of an empty itemsMap
				add(DataOutHandlerTags.DRUPAL_NODE_ID, mDrupalNid);
				add(DataOutHandlerTags.STRUCTURE_TYPE, mStructureType);
			
			
			} catch (JSONException e1) {
				e1.printStackTrace();
			}

			
			// Now parse the secondary keys
			
		   Iterator<String> iter = drupalObject.keys();
		    while (iter.hasNext()) {
		        String key = iter.next();
		        
		        // Valid DataOutPacket entries have keys that start with field_"
		        // and contain another object (key = und"
		        if (key.startsWith("field")) {
			        try {
			            Object undObject = drupalObject.get(key);

			            if (undObject instanceof JSONObject) {
			            	
			            	JSONObject obj = (JSONObject)undObject;
			            	Object obj1 = obj.get("und");
			            	if (obj1 instanceof JSONArray) {
			            		
			            		JSONArray undArrayObj = (JSONArray)obj1;

			            		// If the array holds only 1 entry then just add  it
			            		// Otherwise the array must be converted to a vector before adding it
			            		if (undArrayObj.length() > 1) {
			            			
		            				Vector vector = new Vector();	
			            			for (int i = 0; i < undArrayObj.length(); i++ ) {
			            				JSONObject obj3 = undArrayObj.getJSONObject(i);	
			            				itemValue = obj3.getString("value");
			            				vector.add(itemValue);
			            			}
			            			itemKey = key.substring(6, key.length()); // Remove the field_
			            			add(itemKey,vector);			            			
			            		}
			            		else {
				            		JSONObject obj3 = undArrayObj.getJSONObject(0);
				            		itemValue = obj3.getString("value");
				            		itemKey = key.substring(6, key.length()); // Remove the field_
						            

				            		if (itemKey.equalsIgnoreCase(DataOutHandlerTags.CHECKIN_CHECKIN_TIME)) {
				            			Log.e(TAG, "bad time: " + itemValue);
				            			// Special case for checkin time. since Drupal sends it to us in a wonky way
				            			SimpleDateFormat  badFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");  
				            			try {  
				            			    Date date = new Date();
											date = badFormat.parse(itemValue);
					            			SimpleDateFormat  goodFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");  
				            			    itemValue = goodFormat.format(date);
				            			    add(itemKey,itemValue);				            			    
					            			Log.e(TAG, "good time: " + itemValue);
				            			    
				            			} catch (ParseException e) {  
				            			    e.printStackTrace();  
				            			} catch (java.text.ParseException e) {
											e.printStackTrace();
										} 				            			
				            		}
				            		else {
					            		add(itemKey,itemValue);
				            		}

			            		}
			            	}
			            }
			        } catch (JSONException e) {
			        	Log.e(TAG, e.toString());
			        }
		        }
		    }
	//	    Log.d(TAG, "Conversion OK");		    
	}	
	
	public void updateChangedDate() {
    	Calendar calendar = GregorianCalendar.getInstance();
    	mTimeStamp = calendar.getTimeInMillis();
    	mChangedDate = "" + mTimeStamp/1000;		
	}
	
	/**
	 * Create a DataOutPacket
	 *  By default it's a STRUCTURE_TYPE_SENSOR_DATA structure
	 */
	public DataOutPacket() {
    	UUID uuid = UUID.randomUUID();
    	Calendar calendar = GregorianCalendar.getInstance();
    	mTimeStamp = calendar.getTimeInMillis();
    	dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentTimeString = dateFormatter.format(calendar.getTime());
        String simpleTimeString = simpleDateFormatter.format(calendar.getTime());
    	mRecordId = mTimeStamp + "-" + uuid.toString();
//    	mChangedDate = currentTimeString;
    	mChangedDate = "" + mTimeStamp/1000;
    	mTitle = "";
    	
    	// For drupal we start out with the node id equal to the record id
    	// It will be replace when drupal returns the actual node id
    	mDrupalNid = mRecordId;     	
    	
    	// If structure type not specified, then default to sensor data
    	add(DataOutHandlerTags.STRUCTURE_TYPE, DataOutHandlerTags.STRUCTURE_TYPE_SENSOR_DATA);	    	
    	mStructureType = DataOutHandlerTags.STRUCTURE_TYPE_SENSOR_DATA;
    	
    	add(DataOutHandlerTags.RECORD_ID, mRecordId);
    	add(DataOutHandlerTags.TIME_STAMP, mTimeStamp);
    	add(DataOutHandlerTags.CREATED_AT, currentTimeString);
    	add(DataOutHandlerTags.CHANGED_AT, simpleTimeString);
    	add(DataOutHandlerTags.PLATFORM, "Android");		    	
    	add(DataOutHandlerTags.PLATFORM_VERSION, Build.VERSION.RELEASE);	    	
	}
	
	/**
	 * Create a DataOutPacket
	 * @param structureType - Structure type of packet to create (e.g. Create a STRUCTURE_TYPE_SENSOR_DATA)
	 */
	public DataOutPacket(String structureType) {
    	UUID uuid = UUID.randomUUID();
    	Calendar calendar = GregorianCalendar.getInstance();
    	mTimeStamp = calendar.getTimeInMillis();
    	dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentTimeString = dateFormatter.format(calendar.getTime());
        String simpleTimeString = simpleDateFormatter.format(calendar.getTime());        
    	//mChangedDate = currentTimeString;
    	mChangedDate = "" + mTimeStamp/1000;
    	
    	mRecordId = mTimeStamp + "-" + uuid.toString();
    	mTitle = "";
    	
    	// For drupal we start out with the node id equal to the record id
    	// It will be replace when drupal returns the actual node id
    	mDrupalNid = mRecordId;     	
    	
    	mStructureType = structureType;
    	add(DataOutHandlerTags.RECORD_ID, mRecordId);
    	add(DataOutHandlerTags.TIME_STAMP, mTimeStamp);
    	add(DataOutHandlerTags.CREATED_AT, currentTimeString);
    	add(DataOutHandlerTags.CHANGED_AT, simpleTimeString);
    	add(DataOutHandlerTags.PLATFORM, "Android");		    	
    	add(DataOutHandlerTags.PLATFORM_VERSION, Build.VERSION.RELEASE);	    	
    	add(DataOutHandlerTags.STRUCTURE_TYPE, structureType);	    	
	}	
	
	public void add(String tag, float value) {
		mItemsMap.put(tag.toLowerCase(), value);
	}

	public void add(String tag, float value, String format) {
		String strVal = String.format("%s:" + format + ",", tag,value);		
		mItemsMap.put(tag.toLowerCase(), strVal);
	}

	public void add(String tag, double value) {
		mItemsMap.put(tag.toLowerCase(), value);
	}

	public void add(String tag, double value, String format) {
		String strVal = String.format("%s:" + format + ",", tag,value);		
		mItemsMap.put(tag.toLowerCase(), strVal);
	}

	public void add(String tag, long value) {
		mItemsMap.put(tag.toLowerCase(), value);
	}

	public void add(String tag, int value) {
		mItemsMap.put(tag.toLowerCase(), value);
	}
	
	public void add(String tag, String value) {
		mItemsMap.put(tag.toLowerCase(), value);
	}

	public void add(String tag, Vector vector) {
		mItemsMap.put(tag.toLowerCase(), vector);
	}
	
	public String get(String tag) {
		return (String) mItemsMap.get(tag.toLowerCase().toString());
	}
	
	boolean compareMaps(HashMap map1, HashMap map2, List ignoreList) {
		Boolean result = true;
		
		Set<Object> keys1 = new HashSet<Object>(map1.keySet());
		Set<Object> keys2 = new HashSet<Object>(map2.keySet());		
		
		Set<String> allKeys = new HashSet<String>();
	    allKeys.addAll(map1.keySet());
	    allKeys.addAll(map2.keySet());	
	    
	    for (String key : allKeys) {
//	    	Log.i(TAG, "checking key " + key);
	    	
	    	// For now ignore drupal id since the remote version might no have it
	    	if (key.equalsIgnoreCase("drupal_nid")) {
	    		continue;
	    	}
	    	
	    	if (ignoreList != null) {
	    		if (ignoreList.contains(key.toLowerCase()))
	    			continue;
	    	}
	    	
	    	
	    	Object s1 = (Object) map1.get(key.toLowerCase());
	    	Object s2 = (Object) map2.get(key.toLowerCase());
	    	if (s1 != null && s2 != null) {
	    		
	    		// Handle odd case of a float value with no dec points
	    		if (s1 instanceof Double && s2 instanceof String) {
		    		double ss1 = (Double)s1;
		    		double ss2 = Double.parseDouble((String)s2.toString());

		    		if (ss1 != ss2) {
		    			Log.e(TAG, "Key (" +key + ") - Unequal parameter: " + ss1 + "!= " + ss2);
		    			result = false;
		    		}			    		
	    		} else if (s1 instanceof Float && s2 instanceof String) {
		    		float ss1 = (Float)s1;
		    		float ss2 = Float.parseFloat((String)s2.toString());
		    		if (ss1 != ss2) {
		    			Log.e(TAG, "Key (" + key + ") - Unequal parameter: " + ss1 + "!= " + ss2);
		    			result = false;
		    		}			    		
	    		} else {
		    		String ss1 = (String)s1.toString();
		    		String ss2 = (String)s2.toString();
		    		
		    		if (!ss1.equalsIgnoreCase(ss2)) {
		    			Log.e(TAG, "Key (" + key + ") - Unequal parameter: " + ss1 + "!= " + ss2);
		    			result = false;
		    		}	    			
	    		}
	    	}
	    	else {
	    		String sss1, sss2;
	    		if (s1 != null)
		    		sss1 = (String)s1.toString();
	    		else
	    			sss1 = "null";

	    		if (s2 != null)
		    		sss2 = (String)s2.toString();
	    		else
	    			sss2 = "null";
    			Log.e(TAG, "Key (" + key + ") - Unequal parameter: " + sss1 + "!= " + sss2);

	    			
	    			result = false;
	    	}
	    }
		return result;
	}
	
	public boolean equalsIgnoreTag(DataOutPacket packet, List ignoreList) {
		if (!ignoreList.contains("time_stamp")) {
			if (this.mTimeStamp != packet.mTimeStamp) {
    			Log.e(TAG, "Key (" + "time_stamp" + ") - Unequal parameter: " + this.mTimeStamp + "!= " + packet.mTimeStamp);
				return false;
			}
		}		
		if(!ignoreList.contains("record_id")) {
			if (!this.mRecordId.equalsIgnoreCase(packet.mRecordId)) {
    			Log.e(TAG, "Key (" + "record_id" + ") - Unequal parameter: " + this.mTimeStamp + "!= " + packet.mTimeStamp);
				return false;
			}
		}

		// This might not pass if the node id hasn't been updated yet by the dataOutHandler
//		if (!this.mDrupalNid.equalsIgnoreCase(packet.mDrupalNid))
//			return false;

		HashMap<String, Object> map1 = packet.mItemsMap;
		HashMap<String, Object> map2 = this.mItemsMap;
		
		if (!compareMaps(map1, map2, ignoreList)) {
			return false;
		}
		return true;
	}	
	
	public boolean equals(DataOutPacket packet) {
		if (this.mTimeStamp != packet.mTimeStamp) {
			Log.e(TAG, "Key (" + "packet.mCurrentTime" + ") - Unequal parameter: " + this.mTimeStamp + "!= " + packet.mTimeStamp);
			return false;
		}
		
		if (!this.mRecordId.equalsIgnoreCase(packet.mRecordId)) {
			Log.e(TAG, "Key (" + "packet.mId" + ") - Unequal parameter: " + this.mTimeStamp + "!= " + packet.mTimeStamp);
			return false;
		}

		// This might not pass if the node id hasn't been updated yet by the dataOutHandler
//		if (!this.mDrupalNid.equalsIgnoreCase(packet.mDrupalNid))
//			return false;

		HashMap<String, Object> map1= packet.mItemsMap;
		HashMap<String, Object> map2= this.mItemsMap;
		if (!compareMaps(map1, map2, null)) {
			return false;
		}
		return true;
	}
	
	public String toString() {
		String result = "";
		
		result += mRecordId + ", ";
		result += mDrupalNid + ", ";
		result += mSqlPacketId + ", ";
		result += mChangedDate + ", ";
		result += mCacheStatus + ", ";
		result += mStructureType + ", ";
		result += mTitle + ", ";
		   Iterator it = mItemsMap.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        result += pairs.getKey() + " = " + pairs.getValue() + ", ";
		        
		        if (pairs.getValue() instanceof Integer) {
		        	result += "{INTEGER} + ";
		        }
		        if (pairs.getValue() instanceof String) {
		        	result += "{String} + ";
		        }
		        if (pairs.getValue() instanceof Long) {
		        	result += "{Long} + ";
		        }
		        if (pairs.getValue() instanceof Double) {
		        	result += "{Long} + ";
		        }
		        if (pairs.getValue() instanceof Vector) {
		        	result += "{Vector} + ";
		        }
		    }		
		return result;
	}

}
