/*****************************************************************
Habit

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

import com.t2.dataouthandler.DataOutHandler;
import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutHandlerTags;
import com.t2.dataouthandler.DataOutPacket;
import com.t2.drupalsdk.DrupalUtils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Encapsulates all parameters having to do with a Habit 
 * @author scott.coleman
 *
 */
public class Habit extends DataOutPacket {
	private static final String TAG = Habit.class.getName();	

	private DataOutHandler sDataOutHandler;		
	
	
	// Data contract fields - Primary 
	//	public String mTitle;	// Contained in DataOutPacket

	// Data contract fields - Secondary 
	public String mNote;
	public Date mReminderTime;

	
	// Internal fields
	private String mReminderTimeUnix;
	private int HabitId;
	
	/**
	 * Gets habit ID for habit
	 * @return Habit id
	 */
	public String getHabitId() {
		return mDrupalId;
	}
	
	/**
	 * Creates a new Habit given its parts 
	 * 
	 * @param title
	 * @param note
	 * @param reminderTime
	 * @throws DataOutHandlerException
	 */
	public Habit(String title, String note, Date reminderTime) throws DataOutHandlerException {

        super(DataOutHandlerTags.STRUCTURE_TYPE_HABIT);

        sDataOutHandler = DataOutHandler.getInstance();			
		
		mTitle = title;
		mNote = note;
		mReminderTime = reminderTime;
		
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTime(reminderTime);
		long currentTime = calendar.getTimeInMillis();	
		mReminderTimeUnix = String.valueOf(currentTime / 1000);
		
		mTitle = title;		
		add(DataOutHandlerTags.HABIT_NOTE, note);		
		add(DataOutHandlerTags.HABIT_REMINDER_TIME, mReminderTimeUnix);		
		sDataOutHandler.handleDataOut(this);

		//	sDataOutHandler.registerDbObject(this);
	}
	
	/**
	 * Creates a habit out of a DataOutPacket (of type habit)
	 * @param dataOutPacket DataOutPacket to create habit from 
	 */
	public Habit(DataOutPacket dataOutPacket) {
		//mDataOutPacket = dataOutPacket;
		mTitle = dataOutPacket.mTitle;
		mRecordId = dataOutPacket.mRecordId;
		mSqlPacketId = dataOutPacket.mSqlPacketId;
		mStructureType = dataOutPacket.mStructureType;
		mTimeStamp = dataOutPacket.mTimeStamp;
		mItemsMap = dataOutPacket.mItemsMap;
		
		mCacheStatus = dataOutPacket.mCacheStatus;
		mChangedDate = dataOutPacket.mChangedDate;
		
		mDrupalId = dataOutPacket.mDrupalId;
		mRecordId = dataOutPacket.mRecordId;
		
		mLoggingString = dataOutPacket.mLoggingString;
		mQueuedAction = dataOutPacket.mQueuedAction;
		
		Iterator it = dataOutPacket.mItemsMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        
	        String key = (String) pairs.getKey();
	        
	        if (key.equalsIgnoreCase(DataOutHandlerTags.HABIT_NOTE)) {
	        	mNote = (String) pairs.getValue();
	        }
	    }			
	}
	
	/**
	 * Serializes the contents of this Habit in Drupal format
	 * 
	 * @return String version of drupalized Habit
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
//	        			key.equalsIgnoreCase(DataOutHandlerTags.CHANGED_AT) || // This is covered in primary fields
	        			key.equalsIgnoreCase(DataOutHandlerTags.CREATED_AT) ||  
	        			key.equalsIgnoreCase(DataOutHandlerTags.version) || 
	        			key.equalsIgnoreCase(DataOutHandlerTags.STRUCTURE_TYPE) || 
	        			key.equalsIgnoreCase(DataOutHandlerTags.PLATFORM) || 
	        			key.equalsIgnoreCase(DataOutHandlerTags.TIME_STAMP) || 
	        			key.equalsIgnoreCase(DataOutHandlerTags.PLATFORM_VERSION)	) {
	        		continue;
	        	}	        	
	        	
	        	// Special case for title and habit id it needs to be a primary key
	        	if (	
	        			key.equalsIgnoreCase(DataOutHandlerTags.STRUCTURE_TYPE_HABIT)	) {
	        			item.put(key, (String)pairs.getValue());
	        	}
		        else if (key.equalsIgnoreCase(DataOutHandlerTags.HABIT_REMINDER_TIME)) {
	        		String timeString = (String)pairs.getValue();
	        		DrupalUtils.putDrupalCheckinFieldNode((String)pairs.getKey(), timeString, item);
	        	}
	        	else {
	        		DrupalUtils.putDrupalFieldNode((String)pairs.getKey(), (String)pairs.getValue(), item);								        	
	        	}
	        }
		} // End while (it.hasNext())		
		
		return item.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String result = "";
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String reminderTimeString;
		if (mReminderTime == null) {
			reminderTimeString = "[null]";
		}
		else {
			reminderTimeString = dateFormatter.format(mReminderTime);
		}
		result += "mTitle: " + mTitle + ", mNote: " + mNote + ", reminder: " + reminderTimeString + ", recordId: " + mRecordId + ", drupalId: " + mDrupalId + "\n";
		return result;
	}

	/**
	 * Returns checkins related to this habit
	 * 
	 * @return List of checkins related to habit
	 * @throws DataOutHandlerException
	 */
	public List<Checkin> getCheckins() throws DataOutHandlerException {

		ArrayList<Checkin> checkins = new ArrayList<Checkin>();
		
		ArrayList<DataOutPacket> habitsDOP = sDataOutHandler.getPacketList("StructureType in ('" + DataOutHandlerTags.STRUCTURE_TYPE_CHECKIN + "')");		

		if (habitsDOP != null) {
			for (DataOutPacket packetDOP : habitsDOP) {
				Checkin checkin = new Checkin(packetDOP);
				
				if (checkin.mHabitId == mDrupalId) {
					checkins.add(checkin);
				}
			}
		}
		
		return checkins;
	}	
}