/*****************************************************************
DataOutHandlerTags

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

/**
 * Static tags for use with DataOutHandler.
 * Note: these have a one to one relationship with the Drupal database, so it you add a 
 * tag here you MUST add it to the Drupal database as well
 * 
 * @author scott.coleman
 *
 */
public class DataOutHandlerTags {

	public static final String SENSOR_TIME_STAMP = "STS";		// Integer
	public static final String RAW_GSR = "GSR";					// Microsiemens
	public static final String AVERAGE_GSR = "GSRAVG";			// Microsiemens 1 sec average
	public static final String SENSOR_ID = "SID"; 	
	public static final String RAW_HEARTRATE = "HR"; 			// BPM
	public static final String RAW_SKINTEMP = "ST"; 			// Degrees
	public static final String RAW_EMG = "EMG"; 				// 
	public static final String AVERAGE_HEARTRATE = "HRAVG"; 	// BPM 3 sec average
	public static final String RAW_ECG = "ECG";
	public static final String FILTERED_ECG = "FECG";
	public static final String RAW_RESP_RATE = "RR";			// BPM
	public static final String AVERAGE_RESP_RATE = "RRAVG";		// BPM 10 sec average
	public static final String COMPLETION_PERCENT = "COM";
	public static final String DURATION = "DUR";					// seconds
	public static final String NOTATION = "NOT";
	public static final String CATEGORY = "CAT";
	public static final String EEG_SPECTRAL = "EEG";
	public static final String EEG_SIG_STRENGTH = "EEGSIG";
	public static final String EEG_ATTENTION = "EEGATT";
	public static final String EEG_MEDITATION = "EEGMED";
	public static final String HRV_RAW_IBI = "IBI";					// HR inter-beat interval (Ms)
	public static final String HRV_LF_NU = "LFNU";					// HR low frequency content normalized units (0 - 100)
	public static final String HRV_HF_NU = "HFNU";					// HR low frequency content normalized units (0 - 100)
	public static final String HRV_FFT = "HRVFFT";					// FFT of IBI
	public static final String HRV_RAW_SDNN = "SDNN";				// SDNN
	public static final String NOTE = "note";
	public static final String AIRFLOW = "AIRFLOW";
	public static final String SPO2 = "SPO2";	
	public static final String version = "version";
	public static final String ACCEL_Z = "ACCEL_Z";
	public static final String ACCEL_Y = "ACCEL_Y";
	public static final String ACCEL_X = "ACCEL_X";
	public static final String ORIENT_Z = "ORIENT_Z";
	public static final String ORIENT_Y = "ORIENT_Y";
	public static final String ORIENT_X = "ORIENT_X";
	public static final String LIGHT = "LIGHT";
	public static final String PROXIMITY = "PROXIMITY";
	public static final String BATTERY_LEVEL = "BATTERY_LEVEL";
	public static final String BATTERY_STATUS = "BATTERY_STATUS";
	public static final String SCREEN = "SCREEN";
	public static final String MODEL = "MODEL";
	public static final String LOCALE_LANGUAGE = "LOCALE_LANGUAGE";
	public static final String LOCALE_COUNTRY = "LOCALE_COUNTRY";
	public static final String TEL_CELLID = "TEL_CELLID";
	public static final String TEL_MDN = "TEL_MDN";
	public static final String TEL_NETWORK = "TEL_NETWORK";
	public static final String GPS_LON = "GPS_LON";
	public static final String GPS_LAT = "GPS_LAT";
	public static final String GPS_SPEED = "GPS_SPEED";
	public static final String GPS_TIME = "GPS_TIME";
	public static final String KEYLOCKED = "KEYLOCKED";
	public static final String TASKS = "TASKS";
	public static final String BLUETOOTH_ENABLED = "BLUETOOTH_ENABLED";
	public static final String BLUETOOTH_PAIREDDEVICES = "BLUETOOTH_PAIREDDEVICES";
	public static final String WIFI_ENABLED = "WIFI_ENABLED";
	public static final String WIFI_APSCAN = "WIFI_APSCAN";
	public static final String WIFI_CONNECTED_AP = "WIFI_CONNECTED_AP";
	public static final String CALL_DIR = "CALL_DIR";
	public static final String CALL_REMOTENUM = "CALL_REMOTENUM";
	public static final String CALL_DURATION = "CALL_DURATION";
	public static final String SMS_DIR = "SMS_DIR";
	public static final String SMS_REMOTENUM = "SMS_REMOTENUM";
	public static final String SMS_LENGTH = "SMS_LENGTH";
	public static final String MMS_DIR = "MMS_DIR";
	public static final String MMS_REMOTENUM = "MMS_REMOTENUM";
	public static final String MMS_LENGTH = "MMS_LENGTH";
	public static final String WEBPAGE = "WEBPAGE";
	
	public static final String TIME_STAMP = "time_stamp";
	public static final String USER_ID = "user_id";
	public static final String SESSION_ID = "session_id";
	public static final String RECORD_ID = "record_id";
	public static final String CREATED_AT = "created_at";
	public static final String CHANGED_AT = "changed_at";
	public static final String SESSION_DATE = "session_date";
	public static final String APP_NAME = "app_name";
	public static final String DATA_TYPE = "data_type";
	public static final String PLATFORM = "platform";
	public static final String PLATFORM_VERSION = "platform_version";
	public static final String STRUCTURE_TYPE = "structure_type";
	public static final String STRUCTURE_TYPE_SENSOR_DATA = "sensor_data";

	
	
	// Tags for H4H
	public static final String STRUCTURE_TYPE_HABIT = "habit";
	public static final String HABIT_NAME = "name";
	public static final String HABIT_REMINDER_TIME = "reminder_time";
	public static final String HABIT_NOTE = "note";

	public static final String STRUCTURE_TYPE_CHECKIN = "check_in";
	public static final String STRUCTURE_TYPE_CHECKIN_H4H = "check_in";
	public static final String CHECKIN_CHECKIN_TIME = "checkin_time";
	public static final String CHECKIN_CHECKIN_TITLE = "title";
	public static final String CHECKIN_HABIT_ID = "habit_id";
	
	public static final String TITLE = "title";
	
	// Tags for Pill Planner
	public static final String USER_EMAIL = "user_email";
	public static final String DRUG_NAME = "drug_name";
	public static final String DRUG_FORM = "drug_form";
	public static final String DRUG_DOSAGE = "drug_dosage";
	public static final String DRUG_REASUN = "drug_reason";
	public static final String DRUG_WARNINGS = "drug_warnings";
	public static final String DRUG_NOTES = "drug_notes";

	public static final String USERMED = "usermed";
	public static final String NOTIFICATION_ENABLED = "notificationenabled";
	public static final String REPEATING_COUNT = "repeatingcount";
	public static final String NOTIFICATION_MINUTES = "notificationminutes";
	public static final String AS_NEEDED = "AsNeeded";
	public static final String STATIC_TIME = "StaticTime";
	public static final String DAY_OF_WEEK = "DayOfWeek";
	public static final String HOUR_OF_DAY = "HourOfDay";
	public static final String MINUTE_OF_DAY = "MinuteOfDay";

//	public static final String NAME = "name";
	public static final String PASSWORD = "password";

	
	public static final String TRANSACTION_TYPE = "transaction_type";
	public static final String ADD_USER = "add_user";
	public static final String ADD_DRUG = "add_drug";
	public static final String UPDATE_DRUG = "update_drug";
	public static final String DELETE_DRUG = "delete_drug";
	public static final String ADD_TIME_USER_SCHDEULE = "add_time_user_schdeule";
	public static final String ADD_REMINDER_USER_MED = "add_reminder_user_med";
	
	
	
}
