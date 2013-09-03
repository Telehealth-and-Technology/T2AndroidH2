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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.t2.dataouthandler.DataOutHandler;
import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutHandlerTags;
import com.t2.dataouthandler.DataOutPacket;
import com.t2.drupalsdk.DrupalUtils;

/**
 * Encapsulates all parameters having to do with a Habit 
 * @author scott.coleman
 *
 */
public class Habit {
	private static final String TAG = Habit.class.getName();	
	
	// Data contract fields - Primary 
	public String mTitle;

	// Data contract fields - Secondary 
	public String mNote;
	Date mReminderTime;

	
	// Internal fields
	private  String mReminderTimeUnix;
	private DataOutPacket mDataOutPacket;
	
	
	/**
	 * Creates a new Habit given it's  parts 
	 * 
	 * @param title
	 * @param note
	 * @param reminderTime
	 * @throws DataOutHandlerException
	 */
	public Habit(String title, String note, Date reminderTime) throws DataOutHandlerException {
		DataOutHandler sDataOutHandler;		
		
		sDataOutHandler = DataOutHandler.getInstance();			
		
		mTitle = title;
		mNote = note;
		mReminderTime = reminderTime;
		
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTime(reminderTime);
		long currentTime = calendar.getTimeInMillis();	
		mReminderTimeUnix = String.valueOf(currentTime / 1000);
		
		mDataOutPacket = new DataOutPacket(DataOutHandlerTags.STRUCTURE_TYPE_HABIT);
		mDataOutPacket.mTitle = title;		
		mDataOutPacket.add(DataOutHandlerTags.HABIT_NOTE, note);		
		mDataOutPacket.add(DataOutHandlerTags.HABIT_REMINDER_TIME, mReminderTimeUnix);		
		
		sDataOutHandler.handleDataOut(mDataOutPacket);
	}
	
	/**
	 * Creates a habit out of a DataOutPacket (of type habit)
	 * @param dataOutPacket DataOutPacket to create habit from 
	 */
	public Habit(DataOutPacket dataOutPacket) {
		mDataOutPacket = dataOutPacket;
		mTitle = mDataOutPacket.mTitle;
		
		Iterator it = dataOutPacket.mItemsMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        
	        String key = (String) pairs.getKey();
	        
	        if (key.equalsIgnoreCase(DataOutHandlerTags.HABIT_NOTE)) {
	        	mNote = (String) pairs.getValue();
	        }
	        
	        if (key.equalsIgnoreCase(DataOutHandlerTags.HABIT_REMINDER_TIME)) {
	        	mReminderTimeUnix = (String) pairs.getValue();
	        	
	        	long reminderTimeMillis = 0;

	        	try {
		        	reminderTimeMillis= Long.parseLong(mReminderTimeUnix) * 1000;
		        	reminderTimeMillis *= 1000;
	    		} catch (NumberFormatException e1) {
	    			Log.e(TAG, "Bad format for reminder time: " + mReminderTimeUnix + ", Needs to be unix time");
	    			Log.e(TAG, e1.toString());
	    		} 		        	

	        	Calendar calendar = Calendar.getInstance();
	            calendar.setTimeInMillis(reminderTimeMillis);
	            mReminderTime = calendar.getTime();
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
		item.put("title", mDataOutPacket.mTitle);
		item.put("type", mDataOutPacket.mStructureType);
		item.put("language", "und");	
		item.put("promote", "1");	
		
		Iterator it = mDataOutPacket.mItemsMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry)it.next();	
	        if (pairs.getValue() instanceof Integer) {
	        	DrupalUtils.putDrupaFieldlNode((String)pairs.getKey(), (Integer)pairs.getValue(), item);								        	
	        }
	        if (pairs.getValue() instanceof String) {
	        	
	        	String key = (String)pairs.getKey();
	        	
	        	// Skip the following keys (as per data contract)
	        	if (	key.equalsIgnoreCase(DataOutHandlerTags.CHANGED_AT) || 
	        			key.equalsIgnoreCase(DataOutHandlerTags.CREATED_AT) || 
	        			key.equalsIgnoreCase(DataOutHandlerTags.version) || 
//	        			key.equalsIgnoreCase(DataOutHandlerTags.STRUCTURE_TYPE) || 
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
		result += mTitle + ", " + mNote + ", " + reminderTimeString + "\n";
		return result;
	}
	
}