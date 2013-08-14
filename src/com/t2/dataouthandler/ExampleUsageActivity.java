/*****************************************************************
ExampleUsageActivity

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.t2health.lib1.SharedPref;




import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;


//**********************************************************
// Note that this is out of date.
// Please follow example in DataOutHandlerTest - Main.java
//**********************************************************


/**
 * @author scott.coleman
 * 
 * This is a set up example of how to use the DataOutHandler classes.
 * It's not meant to be working code.
 *
 */
public class ExampleUsageActivity extends Activity {

	private static final String TAG = "BigBrotherService";	
	private DataOutHandler mDataOutHandler;	

	private static final String mAppId = "BigBrotherService";	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private String sessionDate = sdf.format(new Date());
	private String userId = SharedPref.getString(this, "SelectedUser", 	"");
	private long sessionId = SharedPref.getLong(this, "bio_session_start_time", 0);
	private String appName = SharedPref.getString(this, "app_name", mAppId);

	/**
	 * Database uri that the service will sync to 
	 */
	private String mRemoteDatabaseUri = 
			"http://ec2-50-112-197-66.us-west-2.compute.amazonaws.com/mongo/json.php";	

	/**
	 * Constructor - Start up a new instance of the data output handler
	 */
	public ExampleUsageActivity() {
		// ----------------------------------------------------
		// Create a data handler to handle outputting data
		//	to files and database
		// ----------------------------------------------------		
		try {
			mDataOutHandler = new DataOutHandler(this, 
					userId,sessionDate, 
					appName, DataOutHandler.DATA_TYPE_INTERNAL_SENSOR, 
					sessionId );
			mDataOutHandler.enableLogging(this);
			mDataOutHandler.enableLogCat();
			Log.d(TAG, "Initializing DataoutHandler");
		} catch (Exception e1) {
			Log.e(TAG, e1.toString());
			e1.printStackTrace();
		}		
		
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Initialize (and enable) the database
		Log.d(TAG, "Initializing database at " + mRemoteDatabaseUri);
		try {
			mDataOutHandler.initializeDatabase("","","","", mRemoteDatabaseUri);
		} catch (DataOutHandlerException e1) {
			e1.printStackTrace();
		}
		
		// Log the version
		try {
			PackageManager packageManager = getPackageManager();
			PackageInfo info = packageManager.getPackageInfo(getPackageName(), 0);			
			String applicationVersion = info.versionName;
			String versionString = mAppId + 
					" application version: " + applicationVersion;

			
			DataOutPacket packet = new DataOutPacket();			
			packet.add("version", versionString);
			mDataOutHandler.handleDataOut(packet);				

		}
		catch (Exception e) {
		   	Log.e(TAG, e.toString());
		} 			
	}
	
	public void LogSomeData(String model) {

		// Log an arbitrary piece of data
		if (mDataOutHandler != null) {
			DataOutPacket packet = new DataOutPacket();			
			packet.add("MODEL", model);		
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

		// We're done with the database
        mDataOutHandler.close();
	}
}