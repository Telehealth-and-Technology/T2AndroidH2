package com.t2.dataouthandler;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import com.t2.drupalsdk.DrupalUtils;

public class Habit {
	
	DataOutPacket mDataOutPacket;
	

	public Habit(DataOutPacket dataOutPacket) {
		mDataOutPacket = dataOutPacket;
	}
	
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

}
