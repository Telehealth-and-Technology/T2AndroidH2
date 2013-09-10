/*****************************************************************
GUIHelper

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

import org.t2health.lib1.R;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

abstract public class GUIHelper {
	
	private static final String TAG = GUIHelper.class.getSimpleName();	
	
	public interface DialogResult {
		void result(boolean res, String text);
	}

	public interface LoginResult {
		void result(boolean res, String username, String password);
	}
	
	public static void showError(Context context, String msg) {
		showMessage(context, msg, "Error", null);
	}
	
	public static void showMessage(Context context, String msg, String title) {
		showMessage(context, msg, title, null);
	}
	
	/**
	 * Show message.
	 * @param context cContext of calling application
	 * @param msg Message to display
	 * @param title Title for message box to display
	 * @param listener Listener for results
	 */
	public static void showMessage(Context context, String msg, String title, final DialogResult listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title);
		builder.setMessage(msg);
		builder.setCancelable(false);
		builder.setPositiveButton("Ok", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				
				if (listener != null)
					listener.result(true, null);
			}
		});
		
		AlertDialog dlg = builder.create();
		dlg.show();
	}
	
	/**
	 * Shows Yes/No dialog
	 * 
	 * @param context Context of calling application
	 * @param msg Message to display
	 * @param listener Listener for results
	 */
	public static void showYesNo(Context context, String msg, final DialogResult listener) {
		DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
			@Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		            //Yes button clicked
		        	listener.result(true, null);
		        	break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		        	listener.result(false, null);
		            break;
		        }
		    }
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(msg).setPositiveButton("Yes", clickListener)
		    .setNegativeButton("No", clickListener).show();
	}
	
	/**
	 * Shows dialog asking user to enter text.
	 * @param context Context of calling application
	 * @param msg message Message to display
	 * @param listener Listener for results
	 */
	public static void showEnterText(Context context, String msg, final DialogResult listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		final EditText edit = new EditText(context);
		
		DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
			@Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		            //Yes button clicked
		        	try {
						listener.result(true, edit.getText().toString());
					} catch (NumberFormatException e) {
						listener.result(true, "");
					}
		        	break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		        	listener.result(false, edit.getText().toString());
		            break;
		        }
		    }
		};
		    
		builder.setView(edit);
		builder.setMessage(msg)
			.setPositiveButton("Ok", clickListener)
	    	.setNegativeButton("Quit", clickListener)
	    	.show();		
	}

	/**
	 * Shows dialog asking user to enter username and password.
	 * 
	 * @param context Context of calling application
	 * @param msg Message to display
	 * @param listener Listener for results
	 */
	public static void showEnterUserAndPassword(Context context, String msg, final LoginResult listener) {

        final String userName;
        String password;
        final AlertDialog alertDialog;
		
		LayoutInflater factory = LayoutInflater.from(context);
        final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);		
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		alertDialogBuilder.setMessage(msg);
		alertDialogBuilder.setView(textEntryView);
		
		final EditText userNameEditText = (EditText) textEntryView.findViewById(R.id.username_edit);
		final EditText passwordEditText = (EditText) textEntryView.findViewById(R.id.password_edit);
		
		userNameEditText.setText("ScottNonAdmin");
		passwordEditText.setText("Abc12345");
		
		alertDialogBuilder
		.setCancelable(false)
		.setPositiveButton("OK",
		  new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog,int id) {
		    	listener.result(true, userNameEditText.getText().toString(), passwordEditText.getText().toString());
		    }
		  })
		.setNegativeButton("Cancel",
		  new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog,int id) {
		    	//listener.result(true, userNameEditText.getText().toString(), passwordEditText.getText().toString());
		    	dialog.cancel();
		    }
		  });

		    
		alertDialog = alertDialogBuilder.create();
		 
		// show it
		alertDialog.show();	
		
//        Button clearButton = (Button) textEntryView.findViewById(R.id.button_social_login);
//        clearButton.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//		    	listener.result(false, "", "");
//		    	alertDialog.cancel();
//		    	
//			}
//		});
	}	
}
