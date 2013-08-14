/*****************************************************************
LogWriter

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;

public class LogWriter {
	private static final String TAG = "BFDemo";
	
	
	private BufferedWriter mLogWriter = null;
	private String mFileName = "";
	private File mLogFile;	
	public Context mContext;

	public LogWriter(Context context) {
		mContext = context;
	}
	
	public void open(String fileName, boolean showWarning) {
		
		mFileName = fileName;
		
		try {
		    File root = Environment.getExternalStorageDirectory();
		    if (root.canWrite()){
		        mLogFile = new File(root, fileName);
		        mFileName = mLogFile.getAbsolutePath();
		        
		        FileWriter gpxwriter = new FileWriter(mLogFile, true); // open for append
		        mLogWriter = new BufferedWriter(gpxwriter);

//		        try {
//		        	if (mLogWriter != null) {
//		        		mLogWriter.write(mLogHeader + "\n");
//		        	}
//				} catch (IOException e) {
//					Log.e(TAG, e.toString());
//				}
		        
		        
		    } 
		    else {
    		    Log.e(TAG, "Cannot write to log file" );
    		    
    		    if (showWarning) {
        			AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        			alert.setTitle("ERROR");
        			alert.setMessage("Cannot write to log file");	
        			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        				public void onClick(DialogInterface dialog, int whichButton) {
        				}
       				});    			
        			alert.show();
    		    }
    			
		    }
		} catch (IOException e) {
		    Log.e(TAG, "Cannot write to log file" + e.getMessage());
		    if (showWarning) {
				AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
				alert.setTitle("ERROR");
				alert.setMessage("Cannot write to file");
				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
				});    			
			
				alert.show();			
		    }		    
		    
		}		
		
		
	}
	
	public void close() {
    	try {
        	if (mLogWriter != null) {
        		Log.d(TAG, "Closing file");
        		mLogWriter.close();
        		mLogWriter = null;
        	}
		} catch (IOException e) {
			Log.e(TAG, "Exeption closing file " + e.toString());
			e.printStackTrace();
			mLogWriter = null;
		}    		
	}

	public void write(String line) {
        line += "\n";
		try {
        	if (mLogWriter != null)
        		mLogWriter.write(line);
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}			
	}
	
}
