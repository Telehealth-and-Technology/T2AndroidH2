/*****************************************************************
GlobalH2

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

public class GlobalH2 {

	public static final int CACHE_ERROR = -1;	
	public static final int CACHE_IDLE = 0;	
	public static final int CACHE_SENDING = 1;	
	public static final int CACHE_DELETING = 2;	
	public static final int CACHE_SENT = 3;	

	
	public static final String[] VALID_DATA_TYPES = new String[] {
		DataOutHandlerTags.STRUCTURE_TYPE_SENSOR_DATA, 
		DataOutHandlerTags.STRUCTURE_TYPE_HABIT, 
		DataOutHandlerTags.STRUCTURE_TYPE_CHECKIN
//		DataOutHandlerTags.STRUCTURE_TYPE_CHECKIN_H4H
		};
	
	
	// A valid record id (title) looks like this:
	// 1376345039840-4f34d60c-aa58-45fb-9da4-0aea6459dedc
	// TimeStamp-GUID
	public static boolean isValidRecordId(String recordId) {
		
		if (recordId == null)
				return false;
		
		if (recordId.length()  >= 14 && recordId.charAt(13) == '-') 
			return true;
		else
			return false;
	}
	
	
	public static boolean isValidRecordType(String type) {
		
		
		if (type == null)
				return false;
		
		for (String validType : VALID_DATA_TYPES) {
			if (type.equalsIgnoreCase(validType))
				return true;
		}
		
		return false;
	}	
}


