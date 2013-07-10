/* T2AndroidLib-SG for Signal Processing
 * 
 * Copyright © 2009-2013 United States Government as represented by 
 * the Chief Information Officer of the National Center for Telehealth 
 * and Technology. All Rights Reserved.
 * 
 * Copyright © 2009-2013 Contributors. All Rights Reserved. 
 * 
 * THIS OPEN SOURCE AGREEMENT ("AGREEMENT") DEFINES THE RIGHTS OF USE, 
 * REPRODUCTION, DISTRIBUTION, MODIFICATION AND REDISTRIBUTION OF CERTAIN 
 * COMPUTER SOFTWARE ORIGINALLY RELEASED BY THE UNITED STATES GOVERNMENT 
 * AS REPRESENTED BY THE GOVERNMENT AGENCY LISTED BELOW ("GOVERNMENT AGENCY"). 
 * THE UNITED STATES GOVERNMENT, AS REPRESENTED BY GOVERNMENT AGENCY, IS AN 
 * INTENDED THIRD-PARTY BENEFICIARY OF ALL SUBSEQUENT DISTRIBUTIONS OR 
 * REDISTRIBUTIONS OF THE SUBJECT SOFTWARE. ANYONE WHO USES, REPRODUCES, 
 * DISTRIBUTES, MODIFIES OR REDISTRIBUTES THE SUBJECT SOFTWARE, AS DEFINED 
 * HEREIN, OR ANY PART THEREOF, IS, BY THAT ACTION, ACCEPTING IN FULL THE 
 * RESPONSIBILITIES AND OBLIGATIONS CONTAINED IN THIS AGREEMENT.
 * 
 * Government Agency: The National Center for Telehealth and Technology
 * Government Agency Original Software Designation: T2AndroidLib1021
 * Government Agency Original Software Title: T2AndroidLib for Signal Processing
 * User Registration Requested. Please send email 
 * with your contact information to: robert.kayl2@us.army.mil
 * Government Agency Point of Contact for Original Software: robert.kayl2@us.army.mil
 * 
 */
package com.t2.dataouthandler;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.util.Log;

import com.t2.dataouthandler.DataOutHandlerTags;


public class DataOutPacket implements Serializable {

	private final String TAG = getClass().getName();	

	public HashMap<String, Object> mItemsMap = new HashMap<String, Object>();
	public String mLoggingString;
	public String mId;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	public long mCurrentTime;
	public String mDrupalNid;
	public String mQueuedAction = "C";		// Assume all actions are Create unless specifically set otherwise
	
	
	/**
	 * Reconstruct a DataOutPacket from a JSON string (supplied by Drupal)
	 * @param drupalObject
	 * @throws DataOutHandlerException 
	 */
	public DataOutPacket(JSONObject drupalObject) throws DataOutHandlerException {
		
			String itemValue;
			String itemKey;

			// A valid record MUST have a record_id
			String recordId;
			try {
				recordId = drupalObject.getString("field_record_id");
				if (recordId == null) {
    				throw new DataOutHandlerException("Unrecognizable as DataOutPacket");
    		}			
			} catch (JSONException e2) {
				throw new DataOutHandlerException("Unrecognizable as DataOutPacket");
			}
			
			try {
				mDrupalNid = drupalObject.getString("nid");
				add(DataOutHandlerTags.DRUPAL_NODE_ID, mDrupalNid);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			
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
			            			itemKey = key.substring(6, key.length());
			            			add(itemKey,vector);			            			
			            		}
			            		else {
				            		JSONObject obj3 = undArrayObj.getJSONObject(0);
				            		itemValue = obj3.getString("value");
				            		itemKey = key.substring(6, key.length());
						            
				            		// Make sure we have a valid record (Record_id must be greater than 13 characteers
						            add(itemKey,itemValue);
				            		if (itemKey.equalsIgnoreCase("record_id")) {
				            			mId = itemValue;
				            			
				            			if (itemValue.length() >= 13)
				            				mCurrentTime = Long.parseLong(itemValue.substring(0, 13));
				            			else
				            				throw new DataOutHandlerException("Unrecognizable as DataOutPacket");
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
	
	public DataOutPacket() {
    	UUID uuid = UUID.randomUUID();
    	Calendar calendar = GregorianCalendar.getInstance();
    	mCurrentTime = calendar.getTimeInMillis();
    	dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentTimeString = dateFormatter.format(calendar.getTime());
    	mId = mCurrentTime + "-" + uuid.toString();

    	add(DataOutHandlerTags.RECORD_ID, mId);
    	add(DataOutHandlerTags.TIME_STAMP, mCurrentTime);
    	add(DataOutHandlerTags.CREATED_AT, currentTimeString);
    	add(DataOutHandlerTags.PLATFORM, "Android");		    	
    	add(DataOutHandlerTags.PLATFORM_VERSION, Build.VERSION.RELEASE);	    	
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
	
	

	
	/**
	 * Works with any two maps with common key / value types.
	 * The key type must implement Comparable though (for sorting).
	 * Returns a map containing all keys that appear in either of the supplied maps.
	 * The values will be true if and only if either
	 *   - map1.get(key)==map2.get(key) (values may be null) or
	 *   - map1.get(key).equals(map2.get(key)).
	 */
	public static <K extends Comparable<? super K>, V>
	Map<K, Boolean> compareEntries(final Map<K, V> map1,
	    final Map<K, V> map2){
	    final Collection<K> allKeys = new HashSet<K>();
	    allKeys.addAll(map1.keySet());
	    allKeys.addAll(map2.keySet());
	    final Map<K, Boolean> result = new TreeMap<K, Boolean>();
	    for(final K key : allKeys){
	     //   result.put(key,
	       //     map1.containsKey(key) == map2.containsKey(key) &&
	      //      Boolean.valueOf(equal(map1.get(key), map2.get(key))));
	    }
	    return result;
	}	
	
	boolean compareMaps(HashMap map1, HashMap map2) {
		
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
	    	
//	    	Boolean b1 = map1.containsKey(key);
//	    	Boolean b2 = map2.containsKey(key);
	    	Object s1 = (Object) map1.get(key.toLowerCase());
	    	Object s2 = (Object) map2.get(key.toLowerCase());
	    	if (s1 != null && s2 != null) {
	    		
	    		// Handle odd case of a float value with no dec points
	    		if (s1 instanceof Double && s2 instanceof String) {
		    		double ss1 = (Double)s1;
		    		double ss2 = Double.parseDouble((String)s2.toString());

		    		if (ss1 != ss2) {
		    			result = false;
		    		}			    		
	    		} else if (s1 instanceof Float && s2 instanceof String) {
		    		float ss1 = (Float)s1;
		    		float ss2 = Float.parseFloat((String)s2.toString());
		    		if (ss1 != ss2) {
		    			result = false;
		    		}			    		
	    		} else {
		    		String ss1 = (String)s1.toString();
		    		String ss2 = (String)s2.toString();
		    		
		    		if (!ss1.equalsIgnoreCase(ss2)) {
		    			result = false;
		    		}	    			
	    		}
	    		
	    		

	    		

	    		
	    	}
	    	else {
	    		result = false;
	    	}
	    	
	    	
	    }
		

		
		return result;
	}	
	
	
	public boolean equals(DataOutPacket packet) {
		if (this.mCurrentTime != packet.mCurrentTime)
			return false;
		
		if (!this.mId.equalsIgnoreCase(packet.mId))
			return false;
		

		// This might not pass if the node id hasn't been updated yet by the dataOutHandler
//		if (!this.mDrupalNid.equalsIgnoreCase(packet.mDrupalNid))
//			return false;

		HashMap<String, Object> map1= packet.mItemsMap;
		HashMap<String, Object> map2= this.mItemsMap;
		if (!compareMaps(map1, map2)) {
			return false;
		}
		
		
		
		
		
//		
//		add(DataOutHandlerTags.RECORD_ID, mId);
//    	add(DataOutHandlerTags.TIME_STAMP, mCurrentTime);
//    	add(DataOutHandlerTags.CREATED_AT, currentTimeString);
//    	add(DataOutHandlerTags.PLATFORM, "Android");		    	
//    	add(DataOutHandlerTags.PLATFORM_VERSION, Build.VERSION.RELEASE);		
		
		
		
		
		return true;
	}
	
	public String toString() {
		String result = "";
		
		result += mId + ", ";
//		   Iterator it = mItemsMap.entrySet().iterator();
//		    while (it.hasNext()) {
//		        Map.Entry pairs = (Map.Entry)it.next();
//		        result += pairs.getKey() + " = " + pairs.getValue() + ", ";
//		        
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
//		    }		
		return result;
	}

}
