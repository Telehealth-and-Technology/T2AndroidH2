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

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import com.t2.dataouthandler.DataOutHandlerTags;
import com.t2.dataouthandler.DataOutPacket;
import com.t2.drupalsdk.DrupalUtils;

/**
 * Encapsulates all parameters having to do with a Checkin
 * @author scott.coleman
 *
 */
public class Checkin {
	
	DataOutPacket mDataOutPacket;

	public Checkin(DataOutPacket dataOutPacket) {
		mDataOutPacket = dataOutPacket;
	}
	
	/**
	 * Serializes the contents of this Checkin in Drupal format
	 * 
	 * @return String version of drupalized Checkin
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
}