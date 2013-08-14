/*****************************************************************
T2RestPacket

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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.t2.aws.Constants;

import android.util.Log;

/**
 * @author scott.coleman
 * Encapsulates a database entry
 *
 */
public class T2RestPacket {
	
	private static final String TAG = "BFDemo";	
	public static final int STATUS_PENDING = 0;	// Waiting to be sent to server
	public static final int STATUS_POSTED = 1;		// Posted to server but no response received
	public static final int STATUS_RECEIVED = 2;	// Positive ack received from server
	
	public String mId = "nothing";
	public String mJson;
	public int mStatus;
	HashMap<String, AttributeValue> mHashMap = new HashMap<String, AttributeValue>();		
	
	T2RestPacket(String json) {
		mJson = json;
		mStatus = STATUS_PENDING;

		// This is a hokey way to getting the record id!
		// It might present itself differently depending on the database type
		Pattern p = Pattern.compile("\"record_id\":\"[0-9a-zA-Z-]*\"");
		Matcher m = p.matcher(json);	
		if (m.find()) {
			mId = m.group(0);
		}
		
		p = Pattern.compile("\"title\":\"[0-9a-zA-Z-]*\"");
		m = p.matcher(json);	
		if (m.find()) {
			mId = m.group(0);
		}
		
		AttributeValue recordId =  mHashMap.get("record_id");
		
		if (recordId != null) {
			mId = recordId.getS();
		}
	}
	
	T2RestPacket(String json, HashMap<String, AttributeValue> _hashMap) {
		mJson = json;
		mStatus = STATUS_PENDING;
		
		mHashMap = _hashMap;
		
		// This is a hokey way to getting the record id!
		// It might present itself differently depending on the database type
		Pattern p = Pattern.compile("\"record_id\":\"[0-9a-zA-Z-]*\"");
		Matcher m = p.matcher(json);	
		if (m.find()) {
			mId = m.group(0);
		}

		p = Pattern.compile("\"title\":\"[0-9a-zA-Z-]*\"");
		m = p.matcher(json);	
		if (m.find()) {
			mId = m.group(0);
		}
		
		AttributeValue recordId =  mHashMap.get("record_id");
		
		if (recordId != null) {
			mId = recordId.getS();
		}
	}
}
