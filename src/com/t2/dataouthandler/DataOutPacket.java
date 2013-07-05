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
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
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
	 */
	public DataOutPacket(JSONObject drupalObject) {
		
			String itemValue;
			String itemKey;
			
			try {
				mDrupalNid = drupalObject.getString("nid");
				add(DataOutHandlerTags.DRUPAL_NODE_ID, mDrupalNid);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		   Iterator<String> iter = drupalObject.keys();
		    while (iter.hasNext()) {
		        String key = iter.next();
		        
		        // Valid DataOutPacket enteries have keys that start with field_"
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
						            
						            add(itemKey,itemValue);
				            		if (itemKey.equalsIgnoreCase("record_id")) {
				            			mId = itemValue;
				            			mCurrentTime = Long.parseLong(itemValue.substring(0, 13));
				            			
				            		}
			            			
			            		}
			            		
			            		
			            		
			            		
			            	}
			            }
			        } catch (JSONException e) {
			        	Log.e(TAG, e.toString());
			        }
		        	
		        }
		        
		    }
		    
		    Log.d(TAG, "Conversion OK");		    
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
	
	public void add(String tag, double value) {
		mItemsMap.put(tag, value);
	}

	public void add(String tag, double value, String format) {
		String strVal = String.format("%s:" + format + ",", tag,value);		
		mItemsMap.put(tag, strVal);
	}

	public void add(String tag, long value) {
		mItemsMap.put(tag, value);
	}

	public void add(String tag, int value) {
		mItemsMap.put(tag, value);
	}
	
	public void add(String tag, String value) {
		mItemsMap.put(tag, value);
	}

	public void add(String tag, Vector vector) {
		mItemsMap.put(tag, vector);
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
