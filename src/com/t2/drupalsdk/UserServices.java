/*****************************************************************
UserServices

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
package com.t2.drupalsdk;

import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

public class UserServices {
	private static final String TAG = UserServices.class.getSimpleName();		

	private ServicesClient mServicesClient;

    public UserServices(ServicesClient c) {
        mServicesClient = c;
    }

    public void RegisterNewUser(String username, String password, String email,
    		AsyncHttpResponseHandler responseHandler) {
    	
        JSONObject params = new JSONObject();
        try {
            params.put("username", username);
            params.put("password", password);
            params.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        mServicesClient.post("android/user/register", params, responseHandler);    	
        mServicesClient.post(mServicesClient.mDrupalRestEndpoint + "/user/register", params, responseHandler);    	
    }

    public void Login(String username, String password, AsyncHttpResponseHandler responseHandler) {
        JSONObject params = new JSONObject();
        try {
            params.put("username", username);
            params.put("password", password);
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        mServicesClient.post("android/user/login", params, responseHandler);
        mServicesClient.post(mServicesClient.mDrupalRestEndpoint + "/user/login", params, responseHandler);
    }

    public void Logout(AsyncHttpResponseHandler responseHandler) {
        mServicesClient.post(mServicesClient.mDrupalRestEndpoint + "/user/logout", new JSONObject(), responseHandler);
    }

    public void RequestCSRFToken(AsyncHttpResponseHandler responseHandler) {
        mServicesClient.post("services/session/token", new JSONObject(), responseHandler);
    }

    /**
     * Gets a specific drupal node
     * 
     * @param node Node to retrieve
     * @param responseHandler Handler for response
     */
    public void NodeGet( int node, AsyncHttpResponseHandler responseHandler) {
        mServicesClient.get(mServicesClient.mDrupalRestEndpoint + "/node/" + node, new RequestParams(), responseHandler);
    }

    /**
     * Gets all Drupal nodes
     * @param responseHandler Handler for response
     */
    public void NodeGet( AsyncHttpResponseHandler responseHandler) {
        mServicesClient.get(mServicesClient.mDrupalRestEndpoint + "/node/", new RequestParams(), responseHandler);
    }

    public void NodePost( String jsonString, AsyncHttpResponseHandler responseHandler) {
        JSONObject params;
		try {
			params = new JSONObject(jsonString);
	        mServicesClient.post(mServicesClient.mDrupalRestEndpoint + "/node", params, responseHandler);
//		} catch (JSONException e) {
		} catch (Exception e) {
			Log.e(TAG,  e.toString());
			e.printStackTrace();
		}
    }
    
    public void NodePut( String jsonString, AsyncHttpResponseHandler responseHandler, String drupalNodeId) {
        JSONObject params;
		try {
			params = new JSONObject(jsonString);
	        mServicesClient.put(mServicesClient.mDrupalRestEndpoint + "/node/" + drupalNodeId, params, responseHandler);
		} catch (JSONException e) {
			Log.e(TAG,  e.toString());
			e.printStackTrace();
		}
    }    
    
    public void NodeDelete(AsyncHttpResponseHandler responseHandler, String drupalNodeId) {
	        mServicesClient.delete(mServicesClient.mDrupalRestEndpoint + "/node/" + drupalNodeId, responseHandler);
    }       
}
