/*****************************************************************
Checkin

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
package com.t2.h2h4h;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import android.util.Log;

import com.t2.dataouthandler.DataOutHandler;
import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutHandlerTags;
import com.t2.dataouthandler.DataOutPacket;
import com.t2.drupalsdk.DrupalUtils;

/**
 * Encapsulates all parameters having to do with a Checkin
 * @author scott.coleman
 *
 */
public class Checkin  extends DataOutPacket {
	
	private static final String TAG = Checkin.class.getName();	
	
	
	// Data contract fields - Primary
//	public String mTitle;	// Contained in DataOutPacket

	// Data contract fields - Secondary 
	public Date mCheckinTime;
	public String mHabitId;

	
	// Internal fields	
	private String mCheckinTimeUnix;		
	private Habit mHabit;
	

	public Checkin(Habit habit, String title, Date checkinTime) {
        super(DataOutHandlerTags.STRUCTURE_TYPE_CHECKIN);
        
		DataOutHandler sDataOutHandler;		
		try {
			sDataOutHandler = DataOutHandler.getInstance();
			mHabit = habit;
			mTitle = title;
			mCheckinTime = checkinTime;
			
			Calendar calendar = GregorianCalendar.getInstance();
			calendar.setTime(mCheckinTime);
			long currentTime = calendar.getTimeInMillis();	
			mCheckinTimeUnix = String.valueOf(currentTime / 1000);		
			
		    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//	    	dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));   // Drupal wants normal format
	        String timeString = dateFormatter.format(calendar.getTime());		
			
			add(DataOutHandlerTags.CHECKIN_CHECKIN_TIME, timeString);		
			add(DataOutHandlerTags.CHECKIN_HABIT_ID, habit.getHabitId());		
			
			sDataOutHandler.handleDataOut(this);

			
//			sDataOutHandler.registerDbObject(this);		
		
		
		} catch (DataOutHandlerException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}			
	}
	
	
	/**
	 * Creates a Checkin out of a DataOutPacket (of type Checkin)
	 * @param dataOutPacket DataOutPacket to create Checkin from 
	 */
	public Checkin(DataOutPacket dataOutPacket) {
		
		mTitle = dataOutPacket.mTitle;
		mRecordId = dataOutPacket.mRecordId;
		mDrupalId = dataOutPacket.mDrupalId;
		mStructureType = dataOutPacket.mStructureType;
		mChangedDate = dataOutPacket.mChangedDate;

		mTimeStamp = dataOutPacket.mTimeStamp;
		mSqlPacketId = dataOutPacket.mSqlPacketId;

		mItemsMap = dataOutPacket.mItemsMap;
		
		mLoggingString = dataOutPacket.mLoggingString;
		mQueuedAction = dataOutPacket.mQueuedAction;		
		
		mCacheStatus = dataOutPacket.mCacheStatus;
		
		
		Iterator it = dataOutPacket.mItemsMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        
	        String key = (String) pairs.getKey();
	        
//	        if (key.equalsIgnoreCase(DataOutHandlerTags.h)) {
//	        	mHabitId = (String) pairs.getValue();
//	        }
	        
	        // TODO: REminder time fix
	        if (key.equalsIgnoreCase(DataOutHandlerTags.CHECKIN_CHECKIN_TIME)) {
//	        	mCheckinTime = xxx;
	        }
	    }		
	}
	
	/**
	 * Serializes the contents of this Checkin in Drupal format
	 * 
	 * @return String version of drupalized Checkin
	 */	
	public String drupalize() {
		ObjectNode item = JsonNodeFactory.instance.objectNode();
		item.put("title", mTitle);
		item.put("type", mStructureType);
		item.put("language", "und");	
		item.put("promote", "1");	
		item.put("changed", mChangedDate);	
		
		
		Iterator it = mItemsMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry)it.next();	
	        if (pairs.getValue() instanceof Integer) {
	        	DrupalUtils.putDrupaFieldlNode((String)pairs.getKey(), (Integer)pairs.getValue(), item);								        	
	        }
	        if (pairs.getValue() instanceof String) {
	        	
	        	String key = (String)pairs.getKey();
	        	
	        	// Skip the following keys (as per data contract)
	        	if (	
//	        			key.equalsIgnoreCase(DataOutHandlerTags.CHANGED_AT) ||  // This is covered in primary fields
//	        			key.equalsIgnoreCase(DataOutHandlerTags.CREATED_AT) || 
	        			key.equalsIgnoreCase(DataOutHandlerTags.version) || 
	        			key.equalsIgnoreCase(DataOutHandlerTags.STRUCTURE_TYPE) || 
	        			key.equalsIgnoreCase(DataOutHandlerTags.PLATFORM) || 
	        			key.equalsIgnoreCase(DataOutHandlerTags.TIME_STAMP) || 
	        			key.equalsIgnoreCase(DataOutHandlerTags.PLATFORM_VERSION)	) {
	        		continue;
	        	}	        	
	        	
	        	// Special case for title and habit id it needs to be a primary key
	        	if (	
	        			key.equalsIgnoreCase(DataOutHandlerTags.PLATFORM) ||
	        			key.equalsIgnoreCase(DataOutHandlerTags.CHECKIN_HABIT_ID) || 
	        			key.equalsIgnoreCase(DataOutHandlerTags.STRUCTURE_TYPE_CHECKIN)	) {
	        		item.put(key, (String)pairs.getValue());
	        	}
	        	else if (key.equalsIgnoreCase(DataOutHandlerTags.CHECKIN_CHECKIN_TIME)) {
	        		DrupalUtils.putDrupalCheckinFieldNode((String)pairs.getKey(), (String)pairs.getValue(), item);
	        	}
	        	else {
	        		DrupalUtils.putDrupalFieldNode((String)pairs.getKey(), (String)pairs.getValue(), item);								        	
	        	}
	        }
		} // End while (it.hasNext())		
		
		return item.toString();
	}
	
	public String toString() {
		String result = "";
		result += "mTitle: " + mTitle + ", mHabitId: " + mHabitId + ", checkinTime: " + mCheckinTime + ", recordId: " + mRecordId + ", drupalId: " + mDrupalId + "\n";
		return result;
	}
	
	
}