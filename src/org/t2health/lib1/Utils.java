/*****************************************************************
Utils

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
package org.t2health.lib1;

import java.io.File;
import java.io.IOException;



import android.os.Environment;
import android.util.Log;

public class Utils {
	private static final String TAG = "Utils";
	/**
	 * Clears the device logcat
	 */
	static void clearLogCat() {
        try {
		    String cmd = "logcat -c ";
		    Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			Log.e(TAG, "Error clearing logcat" + e.toString());
			e.printStackTrace();
		}			
		
	}
	
	/**
	 * Saves the current device logcat to file on external storage
	 * @param fileName - Filename to save to
	 */
	static void SaveLogCatToFile(String fileName) {
		try {
		    File filename = new File(Environment.getExternalStorageDirectory() + "/" + "Logcat_" + fileName+ ".log"); 
		    filename.createNewFile(); 
		    String cmd = "logcat -d -f "+filename.getAbsolutePath();
		    Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}			
	}

	
	
}
