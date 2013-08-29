package com.t2.drupalsdk;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;


public class DrupalRegistrationResponse {
	private final String TAG = getClass().getName();	

	public String mUid = "";
	
	public DrupalRegistrationResponse(String response) {

		try {
			JSONObject jsonResponse = new JSONObject(response);
			
			   Iterator<String> iter = jsonResponse.keys();
			    while (iter.hasNext()) {
			        String key = iter.next();
			        
			        if (key.startsWith("user")) {
				            Object userObject = jsonResponse.get(key);
				            
				            if (userObject instanceof JSONObject) {
				               	JSONObject obj = (JSONObject)userObject;
				               	mUid = obj.getString("uid");
				            	Object obj1 = obj.get("uid");
				            }
			        }
			        
			        Log.e(TAG, key);
			    }			
			
		} catch (JSONException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}
	}
}
