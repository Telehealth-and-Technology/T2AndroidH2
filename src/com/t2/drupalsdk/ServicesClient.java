/*****************************************************************
ServicesClient

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

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;
import com.t2.dataouthandler.DataOutHandlerException;

import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class ServicesClient {
	
	private static final String TAG = ServicesClient.class.getSimpleName();		

    private String mUrlString;
    private String mDrupalService;
    public String mDrupalRestEndpoint;
    private String mProtocol;
    private String mHost;
    
    private static final int INDEX_DRUPAL_SERVICE = 3;
    private static final int INDEX_DRUPAL_REST_ENDPOINT = 4;
    private static final int MIN_PARAMETERS = 5 ;
    
    
    public static AsyncHttpClient mAsyncHttpClient = new AsyncHttpClient();

    public void setCSRFToken(String cSRFToken) {
    	mAsyncHttpClient.addHeader("X_CSRF_TOKEN", cSRFToken);
    }
    
    public ServicesClient(String server, String base) {
        this.mUrlString = server + '/' + base + '/';
        mAsyncHttpClient.setTimeout(60000);
    }

    public ServicesClient(String urlString) throws DataOutHandlerException, MalformedURLException {
    	
    	if (urlString == null || urlString == "") {
			throw new DataOutHandlerException("Remote database URL must not be null or blank");
    	}
        // Break down the remote URL string into constituant parts
        String[] tokens = urlString.split("/");        

        if (tokens.length < MIN_PARAMETERS) {
			throw new DataOutHandlerException("Remote database URL incorrectly formatted - "
					+ "must include Drupal service and Drupal Rest Endpoint");
        }
        this.mDrupalRestEndpoint = tokens[INDEX_DRUPAL_REST_ENDPOINT];
        this.mDrupalService = tokens[INDEX_DRUPAL_SERVICE];
        this.mUrlString = urlString;

        URL url = new URL(urlString);        
        
        mProtocol = url.getProtocol();
        mHost = url.getHost();
        
        mAsyncHttpClient.setTimeout(60000);
    }

    public void setCookieStore(PersistentCookieStore cookieStore) {
        mAsyncHttpClient.setCookieStore(cookieStore);
    }

    private String getAbsoluteUrl(String relativeUrl) {
//        return this.mUrlString + relativeUrl;
    	return mProtocol + "://" + mHost + "/" + mDrupalService + "/" + relativeUrl;
        
    }

    public void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        Log.d(TAG, "url = " + getAbsoluteUrl(url));
    	
        mAsyncHttpClient.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        mAsyncHttpClient.post(getAbsoluteUrl(url), params, responseHandler);
    }

    public void post(String url, JSONObject params, AsyncHttpResponseHandler responseHandler) {
        StringEntity se = null;
        try {
            se = new StringEntity(params.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        Log.d(TAG, "url = " + getAbsoluteUrl(url));
//        Log.d(TAG, "mAsyncHttpClient = " + mAsyncHttpClient);
        
        
//        // TODO: change to debug - it's at error now simply for readability
//        HttpContext context = mAsyncHttpClient.getHttpContext();
//        CookieStore store1 = (CookieStore) context.getAttribute(ClientContext.COOKIE_STORE);
//        Log.e(TAG, "Cookies for AsyncClient = " + store1.getCookies().toString());       
        

        mAsyncHttpClient.post(null, getAbsoluteUrl(url), se, "application/json", responseHandler);
    }

    public void put(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        mAsyncHttpClient.post(getAbsoluteUrl(url), params, responseHandler);
    }

    public void put(String url, JSONObject params, AsyncHttpResponseHandler responseHandler) {
        StringEntity se = null;
        try {
            se = new StringEntity(params.toString());
        } catch (UnsupportedEncodingException e) {
        	Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        Log.d(TAG, "url = " + getAbsoluteUrl(url));
        

        mAsyncHttpClient.put(null, getAbsoluteUrl(url), se, "application/json", responseHandler);
    }
    
    public void delete(String url, AsyncHttpResponseHandler responseHandler) {

        Log.d(TAG, "Delete url = " + getAbsoluteUrl(url));
        mAsyncHttpClient.delete(null, getAbsoluteUrl(url), responseHandler);
    }    
}
