/* T2AndroidLib-SG for Signal Processing
 * 
 * Copyright © 2009-2013 United States Government as represented by 
 * the Chief Information Officer of the National Center for Telehealth 
 * and Technology. All Rights Reserved.
 * 
 * Copyright © 2009-2013 Contributors. All Rights Reserved. 
 * 
 * THIS OPEN SOURCE AGREEMENT ("AGREEMENT") DEFINES THE RIGHTS OF USE, 
 * REPRODUCTION, DISTRIBUTION, MODIFICATION AND REDISTRIBUTION OF CERTAIN 
 * COMPUTER SOFTWARE ORIGINALLY RELEASED BY THE UNITED STATES GOVERNMENT 
 * AS REPRESENTED BY THE GOVERNMENT AGENCY LISTED BELOW ("GOVERNMENT AGENCY"). 
 * THE UNITED STATES GOVERNMENT, AS REPRESENTED BY GOVERNMENT AGENCY, IS AN 
 * INTENDED THIRD-PARTY BENEFICIARY OF ALL SUBSEQUENT DISTRIBUTIONS OR 
 * REDISTRIBUTIONS OF THE SUBJECT SOFTWARE. ANYONE WHO USES, REPRODUCES, 
 * DISTRIBUTES, MODIFIES OR REDISTRIBUTES THE SUBJECT SOFTWARE, AS DEFINED 
 * HEREIN, OR ANY PART THEREOF, IS, BY THAT ACTION, ACCEPTING IN FULL THE 
 * RESPONSIBILITIES AND OBLIGATIONS CONTAINED IN THIS AGREEMENT.
 * 
 * Government Agency: The National Center for Telehealth and Technology
 * Government Agency Original Software Designation: T2AndroidLib1021
 * Government Agency Original Software Title: T2AndroidLib for Signal Processing
 * User Registration Requested. Please send email 
 * with your contact information to: robert.kayl2@us.army.mil
 * Government Agency Point of Contact for Original Software: robert.kayl2@us.army.mil
 * 
 */
package com.t2.dataouthandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import org.apache.http.cookie.Cookie;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.t2health.lib1.LogWriter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.PutItemRequest;
import com.amazonaws.tvmclient.AmazonClientManager;
import com.janrain.android.engage.JREngage;
import com.janrain.android.engage.JREngageDelegate;
import com.janrain.android.engage.JREngageError;
import com.janrain.android.engage.net.async.HttpResponseHeaders;
import com.janrain.android.engage.types.JRActivityObject;
import com.janrain.android.engage.types.JRDictionary;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;
import com.t2.aws.DynamoDBManager;
import com.t2.dataouthandler.DataOutHandler;
import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutHandlerTags;
import com.t2.dataouthandler.GUIHelper.LoginResult;
import com.t2.dataouthandler.T2AuthDelegate;
import com.t2.dataouthandler.T2RestClient;
import com.t2.dataouthandler.T2RestPacket;
import com.t2.dataouthandler.dbcache.DbCache;
import com.t2.dataouthandler.dbcache.SqlPacket;
import com.t2.drupalsdk.ServicesClient;
import com.t2.drupalsdk.UserServices;

/**
 * Handles interface to external databases.
 * Also initializes Authentication services (JanRain)
 * 
 * 
 * 
 * 
 * @author scott.coleman
 *
 */
public class DataOutHandler  implements JREngageDelegate {
					
	private final String TAG = getClass().getName();	
	//private static final String DEFAULT_REST_DB_URL = "http://gap.t2health.org/and/phpWebservice/webservice2.php";	 
	// private static final String DEFAULT_REST_DB_URL = "http://gap.t2health.org/and/json.php";	 
	private static final String DEFAULT_REST_DB_URL = "http://ec2-50-112-197-66.us-west-2.compute.amazonaws.com/mongo/json.php";
	private static final String DEFAULT_AWS_DB_URL = "h2tvm.elasticbeanstalk.com";
//	private static final String DEFAULT_DRUPAL_DB_URL = "https://t2health.us/h2/android/";
	private static final String DEFAULT_DRUPAL_DB_URL = "http://t2health.us/h2/android/";
    private static String ENGAGE_TOKEN_URL = "http://t2health.us/h2/rpx/token_handler?destination=node";	
	private static final String DEFAULT_DRUPAL_DB_URL_SSL = "https://t2health.us/h2/android/";
    private static String ENGAGE_TOKEN_URL_SSL = "https://t2health.us/h2/rpx/token_handler?destination=node";	
	
	private static final boolean AWS_USE_SSL = false;
	private static final boolean DRUPAL_USE_SSL = true;

	private static final boolean VERBOSE_LOGGING = true;
	
	
	
    private static String ENGAGE_APP_ID = "khekfggiembncbadmddh";
//    private static String ENGAGE_TOKEN_URL = "http://t2health.us/h2/rpx/token_handler?destination=node";	

	private static final int LOG_FORMAT_JSON = 1;	
	private static final int LOG_FORMAT_FLAT = 2;	
	

	public static final String SHORT_TIME_STAMP = "\"TS\"";

	public static final String DATA_TYPE_RATING = "RatingData";
	public static final String DATA_TYPE_INTERNAL_SENSOR = "InternalSensor";
	public static final String DATA_TYPE_EXTERNAL_SENSOR = "ExternalSensor";
	public static final String DATA_TYPE_USER_ENTERED_DATA = "UserEnteredData";
	
	//public static final int SYNC_TIMEOUT = 20000;
	public static final int SYNC_TIMEOUT = 200000;
	
	private ProgressDialog mProgressDialog;
	
	
    Object addPacketToCacheSyncToken = new Object();	
    Object updateCacheSyncToken = new Object();	
    Object sendPacketToRemoteDbToken = new Object();	
        
	
	public boolean mLogCatEnabled = false;	
	public boolean mLoggingEnabled = false;	
	private boolean mDatabaseEnabled = false;
	private boolean mSessionIdEnabled = false;
	
	private String mResult;

	private boolean mAllowTraditionalLogin = true;
	private boolean mLoggedInAsTraditional = false;
	
	
	/**
	 * User identification to be associated with data stored
	 */
	public String mUserId = "";
	
	/**
	 * Date a particular session started, there can be multiple data 
	 * saves for any session
	 */
	public String mSessionDate = "";
	
	/**
	 * Name of calling application (logged with data)
	 */
	public String mAppName = "";
	
	/**
	 * Source type of data (internal, external, etc.)
	 */
	public String mDataType = "";
	
	/**
	 * Used to write data logs
	 */
	private LogWriter mLogWriter;	

	/**
	 * Context of calling party
	 */
	private Context mContext;

	/**
	 * Desired format of data lof files
	 */
	private int mLogFormat = LOG_FORMAT_JSON;	// Alternatively LOG_FORMAT_FLAT 	

	/**
	 * ID of a particular session (for multiple sessions in an application run
	 */
	private long mSessionId;
	
	/**
	 * URL of the remote database we are saving to
	 */
	String mRemoteDatabase;	
	
	/**
	 * Thread used to communicate messages in mPendingQueue to server
	 */
	private DispatchThread mDispatchThread = null;	
	
	
    public static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	
	/**
	 * Application version info determined by the package manager
	 */
	private String mApplicationVersion = "";

	// JanRain Stuff
	String mEngageAppId = ENGAGE_APP_ID;
	String mEngageTokenUrl = ENGAGE_TOKEN_URL;
	
	// T2 Drupal stuff
	/**
	 * Used to save Drupal session cookies for authentication.
	 */
	private PersistentCookieStore mCookieStore;

	/**
	 * HTTP services client used to talk to Drupal.
	 */
	private ServicesClient mServicesClient;	

	/**
	 * Engage instance for openID authentication
	 */
	private JREngage mEngage;
	
    /**
     * JanRain Callbacks for notification of auth success/fail, etc.
     */
    private T2AuthDelegate mT2AuthDelegate;    
    
    /**
     * Contains information about authenticated user
     */
    private JRDictionary mAuth_info;

    /**
     * The provider the user used to authenticate with (provided by JanRain)
     */
    private String mAuthProvider;	
	
	
	
    /**
	 * True if JanRain has successfully authenticated a user. 
	 */
	private boolean mAuthenticated = false;
	
	/**
	 * Set this to true to require authtication for all database puts. 
	 */
	private boolean mRequiresAuthentication = true;	
	
	/**
	 * Queue for Rest packets waiting to be sent via HTTP
	 */
	List<DataOutPacket> mPendingQueue;	
	
	/**
	 * Database manager when sending data to external Amazon database
	 */
	public static AmazonClientManager sClientManager = null;		

	// Database types. 
	//		Note that different database types
	// 		may need different processing and even 
	//		different structures, thus is it important to
	//		use DataOutPacket structure to add data
	//	
	public final static int DATABASE_TYPE_AWS = 0;			//	AWS (Goes to AWS DynamoDB)
	public final static int DATABASE_TYPE_T2_REST = 1; 		// T2 Rest server (goes to Mongo DB)
	public final static int DATABASE_TYPE_T2_DRUPAL = 2; 	//	T2 Drupal - goes to a Drupal database
	public final static int DATABASE_TYPE_NONE = -1;
	
	/**
	 * sets which type of external database is setup and used
	 */
	private int mDatabaseType;	
	
	/**
	 * Sets the AWS table name into which data is stored (AWS only)
	 */
	private String mAwsTableName = "TestT2"; // Default to TestT2

	/**
	 * Shared preferences for this lib (will be the same as calling party)
	 */
	SharedPreferences mSharedPreferences;	
	
	/**
	 * Static instance of this class
	 */
	private static DataOutHandler sDataOutHandler;	
	
	
	/**
	 * Session cookie Janrain Obtains from Drupal for active session
	 */
	private Cookie drupalSessionCookie;
	
	/**
	 * List of Drupal node id's currently existing in Drupal 
	 */
	private List<String> mRemoteDrupalNodeIdList;

	/**
	 * List of node ids which we have reuqested to be deleted from Drupal
	 */
	private List<String> mNodeDeleteQueue = new ArrayList<String>();	
	

	/**
	 * List of drupal node id's which have been successfully added  to drupal
	 */
	private List<String> mDrupalIdsSuccessfullyAdded = new ArrayList<String>();	
	
	/**
	 * Database cache
	 */
	public DbCache mDbCache;
	
	private DatabaseCacheUpdateListener mDatabaseUpdateListener;
	
	private DataOutHandler mInstance;
	
	
	
	public void setDatabaseUpdateListener(DatabaseCacheUpdateListener mDatabaseUpdateListener) {
		this.mDatabaseUpdateListener = mDatabaseUpdateListener;
	}

	/**
	 * Sets the AWS table name (applicable only if AWS database is chosen)
	 * @param awsTableName Name of the table
	 */
	public void setAwsTableName(String awsTableName) {
		this.mAwsTableName = awsTableName;
	}	
	
	public synchronized static DataOutHandler getInstance(Context context, String userId, String sessionDate, String appName, String dataType, long sessionId) {
		if (sDataOutHandler == null) {
			sDataOutHandler = new DataOutHandler(context, userId, sessionDate, appName, dataType, sessionId);
		}
		
		return sDataOutHandler;
	}
	
	public static DataOutHandler getInstance() throws DataOutHandlerException {
		if (sDataOutHandler == null) {
			throw new DataOutHandlerException("DataOutHandler has not been initialized");
		}
		
		return sDataOutHandler;
	}		
	
	
	public DataOutPacket getPacketByDrupalId(String nodeId) {
		return null;
	}
	
	public DataOutPacket getPacketByRecordId(String recordId) {
		
//		for (DataOutPacket packet : mRemotePacketCache.values()) {
//			if (packet.mRecordId.equalsIgnoreCase(recordId)) {
//				return packet;
//			}
//		}
//		
		return null;
	}	
	
	/**
	 * Constructor. Sets up context and user/session parameters
	 * 
	 * @param context	- Context of calling activity
	 * @param userId	- User ID detected by calling activity 
	 * @param sessionDate - Session date created by the calling activity (data/time stamp)
	 * @param appName - Name of calling application
	 */
	public DataOutHandler(Context context, String userId, String sessionDate, String appName) {
		mAppName = appName;
		mContext = context;
		mUserId = userId;
		mSessionDate = sessionDate;
		mSessionIdEnabled = false;
		mInstance = this;
	}
	
	/**
	 * Constructor. sets up context and user/session parameters
	 * 
	 * @param context	- Context of calling activity
	 * @param userId	- User ID detected by calling activity 
	 * @param sessionDate - Session date created by the calling activity (data/time stamp)
	 * @Param appName		- Name of calling application (for logging)
	 * @Param dataType		- Data type (Internal or external)
	 * @param sessionId - long session ID to be included in all packets
	 */
	public DataOutHandler(Context context, String userId, String sessionDate, String appName, String dataType, long sessionId) {
		mAppName = appName;
		mDataType = dataType;
		mContext = context;
		mUserId = userId;
		mSessionDate = sessionDate;
		mSessionIdEnabled = true;
		mSessionId = sessionId;
		mInstance = this;
	}
	
	/**
	 * Disables database functionality
	 */
	public void disableDatabase() {
		mDatabaseEnabled = false;
	}
	
	/**
	 * Enables database functionality
	 */
	public void enableDatabase() {
		mDatabaseEnabled = true;
	}
	
	/**
	 * Sets formatting of log output
	 * 	LOG_FORMAT_JSON = 1; JSON format	
	 *  LOG_FORMAT_FLAT = 2; Standard column format
	 * 
	 * @param format - format to use
	 */
	public void setLogFormat(int format) {
		mLogFormat = format;		
	}
	
			
	
	/**
	 * @author scott.coleman
	 * Task to check the status of an AWS database table
	 */
	class CheckTableStatusTask extends AsyncTask<String, Void, String> {

	    private Exception exception;

	    protected String doInBackground(String... urls) {
	        try {
				String tableStatus = DynamoDBManager.getTestTableStatus();
				String status = tableStatus;
	        	
	            return status;
	        } catch (Exception e) {
	            this.exception = e;
	            return "";
	        }
	    }

	    protected void onPostExecute(String status) {
	    	Log.d(TAG, "Database status = " + status);
	    }
	 }	
	
	/**
	 * Sets  the RequiresAuthentication flag 
	 * @param mRequiresAuthentication true/false
	 */
	public void setRequiresAuthentication(boolean mRequiresAuthentication) {
		this.mRequiresAuthentication = mRequiresAuthentication;
	}	
	/**
	 * Initialized specified database
	 * 
	 * @param remoteDatabase URL of database to send data to. 
	 * @param databaseType Type of database (AWS, TRest, T2Drupal, etc.).
	 * @param t2AuthDelegate Callbacks to send status to.
	 * @throws DataOutHandlerException
	 */
	public void initializeDatabase(String remoteDatabase, String databaseType) throws DataOutHandlerException {
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		// Do it this way for backward compatibility
		mSharedPreferences.edit().putString("external_database_type", databaseType);
		initializeDatabase("", "", "", "", remoteDatabase);		
	}

	/**
	 * Initialized specified database
	 * 
	 * @param remoteDatabase URL of database to send data to. 
	 * @param databaseType Type of database (AWS, TRest, T2Drupal, etc.).
	 * @param t2AuthDelegate Callbacks to send status to.
	 * @throws DataOutHandlerException
	 */
	public void initializeDatabase(String remoteDatabase, String databaseType, T2AuthDelegate t2AuthDelegate) throws DataOutHandlerException {
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		// Do it this way for backward compatibility
		mSharedPreferences.edit().putString("external_database_type", databaseType);
		mT2AuthDelegate = t2AuthDelegate;
		initializeDatabase("", "", "", "", remoteDatabase);		
	}

	/**
	 * @param remoteDatabase URL of database to send data to. 
	 * @param databaseType Type of database (AWS, TRest, T2Drupal, etc.)
	 * @param t2AuthDelegate Callbacks to send status to.
	 * @param awsTableName AWS table name to use when putting data.
	 * @throws DataOutHandlerException
	 */
	public void initializeDatabase(String remoteDatabase, String databaseType, 
			T2AuthDelegate t2AuthDelegate, String awsTableName) throws DataOutHandlerException {
		mAwsTableName = awsTableName;
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		// Do it this way for backward compatibility
		mSharedPreferences.edit().putString("external_database_type", databaseType);
		mT2AuthDelegate = t2AuthDelegate;
		initializeDatabase("", "", "", "", remoteDatabase);		
	}
	
	/**
	 * Initializes the current database
	 * 
	 * Note that all of the parameters (with the exception of remoteDatabase) sent to this routine are for CouchDB only.
	 * Currently they are all N/A
	 * 
	 * Endpoint for all initialize variants.
	 * 
	 * @param databaseName		N/A Local SQLITE database name
	 * @param designDocName		N/A Design document name
	 * @param designDocId		N/A Design document ID
	 * @param viewName			N/AView associated with database
	 * @param remoteDatabase	Name of external database
	 * @throws DataOutHandlerException 
	 */
	public void initializeDatabase(String databaseName, String designDocName, String designDocId, String viewName, String remoteDatabase) throws DataOutHandlerException {

		mDatabaseEnabled = true;

		// Set database type
		mDatabaseType = DATABASE_TYPE_NONE; 

		// Get chosen database from preferences
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		String databaseTypeString = mSharedPreferences.getString("external_database_type", "AWS");

		
		if (DRUPAL_USE_SSL) {
			mEngageTokenUrl = ENGAGE_TOKEN_URL_SSL;			
		}
		else {
			mEngageTokenUrl = ENGAGE_TOKEN_URL;			
		}
		
		// Based on database type:
		// 	Set up mRemoteDatabase based on either remoteDatabase if it's not blank,
		// 	or default values based on database type
		
		// Then do any database type specific initialization
		
		if (databaseTypeString.equalsIgnoreCase("AWS")) {
			Log.d(TAG, "Using AWS Database type");

			mDatabaseType = DATABASE_TYPE_AWS;
			if (remoteDatabase != null ) {
				if (remoteDatabase.equalsIgnoreCase("")) {
					mRemoteDatabase = DEFAULT_AWS_DB_URL;			
				}
				else {
					mRemoteDatabase = remoteDatabase;
				}
				
				// Note: for AWS we don't supply a token URL, thats
				// only for interfacing with Drupal
		        mEngage = JREngage.initInstance(mContext, mEngageAppId, "", this);

		        // This is to account for a bug in janrain where a delegate might not get added in the initinstance call
		        // As odd as it seems, this ensures that only one delegate gets added per instance.
		        mEngage.removeDelegate(this);
		        mEngage.addDelegate(this);
		        
		        JREngage.blockOnInitialization();

				
				//	clientManager = new AmazonClientManager(mContext.getSharedPreferences("com.amazon.aws.demo.AWSDemo", Context.MODE_PRIVATE), mRemoteDatabase);	
				sClientManager = new AmazonClientManager(mSharedPreferences, mRemoteDatabase);	
				// TBD - we should probably check the table status
				//new CheckTableStatusTask().execute("");			
			}			
			
		}
		else if (databaseTypeString.equalsIgnoreCase("T2REST")) {
			Log.d(TAG, "Using T2 Rest Database type");

			mDatabaseType = DATABASE_TYPE_T2_REST;
			if (remoteDatabase != null ) {
				if (remoteDatabase.equalsIgnoreCase("")) {
					mRemoteDatabase = DEFAULT_REST_DB_URL;			
				}
				else {
					mRemoteDatabase = remoteDatabase;
				}
				
		        mEngage = JREngage.initInstance(mContext, mEngageAppId, mEngageTokenUrl, this);
		        // This is to account for a bug in janrain where a delegate might not get added in the initinstance call
		        // As odd as it seems, this ensures that only one delegate gets added per instance.
		        mEngage.removeDelegate(this);
		        mEngage.addDelegate(this);
		        
		        JREngage.blockOnInitialization();
			}				
			
		}
		else if (databaseTypeString.equalsIgnoreCase("T2DRUPAL")) {
			Log.d(TAG, "Using T2 Drupal Database type");

			mDatabaseType = DATABASE_TYPE_T2_DRUPAL;
			if (remoteDatabase != null ) {
				if (remoteDatabase.equalsIgnoreCase("")) {
					if (DRUPAL_USE_SSL) {
						mRemoteDatabase = DEFAULT_DRUPAL_DB_URL_SSL;			
					}
					else {
						mRemoteDatabase = DEFAULT_DRUPAL_DB_URL;			
					}
				}
				else {
					mRemoteDatabase = remoteDatabase;
				}
				
		        mEngage = JREngage.initInstance(mContext, mEngageAppId, mEngageTokenUrl, this);
		        // This is to account for a bug in janrain where a delegate might not get added in the initinstance call
		        // As odd as it seems, this ensures that only one delegate gets added per instance.
		        mEngage.removeDelegate(this);
		        mEngage.addDelegate(this);
		        
		        JREngage.blockOnInitialization();

		        mServicesClient = new ServicesClient(mRemoteDatabase);
		        mCookieStore = new PersistentCookieStore(mContext);
		        mCookieStore.clear(); // Make sure to start fresh
		        mServicesClient.setCookieStore(mCookieStore);
		        
				try {
					mDbCache = new DbCache(mRemoteDatabase, mContext, mDatabaseUpdateListener);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
					e.printStackTrace();
				}
			}			

		}
		
		// Make sure a valid database was selected
		if (mDatabaseType == DATABASE_TYPE_NONE) {
			throw new DataOutHandlerException("Invalid database type");
		}

		// Now do any global database (ot other)  initialization
		Log.d(TAG, "Initializing T2 database dispatcher");
		Log.d(TAG, "Remote database name = " + mRemoteDatabase);
		mPendingQueue = new ArrayList<DataOutPacket>();		
		mDispatchThread = new DispatchThread();

		mDispatchThread.start();		
	}			
	
	public void logIn(final Activity thisActivity) {
		if (mAuthenticated) {
			new AlertDialog.Builder(mContext).setMessage("Already logged in, please logout first").setPositiveButton("OK", null).setCancelable(true).create().show();
		}
		else {
			if (mAllowTraditionalLogin) {
				GUIHelper.showEnterUserAndPassword(mContext, "", new LoginResult() {

					@Override
					public void result(boolean res, String username, String password) {
						Log.d(TAG, "username/password = " + username + " / " + password);
						
						if (res) {
							traditionalLogin(username, password);
						}
						else {
							// Causes Janrain to initiate login activity by showing login dialog
							// See callbacks jrAuthenticationDidReachTokenUrl() and jrAuthenticationDidSucceedForUser()
							// to see mAuthenticated getting set
							mEngage.showAuthenticationDialog(thisActivity);
							
						}
					}
		    	}); 			
			}
			else {
				// Causes Janrain to initiate login activity by showing login dialog
				// See callbacks jrAuthenticationDidReachTokenUrl() and jrAuthenticationDidSucceedForUser()
				// to see mAuthenticated getting set
				mEngage.showAuthenticationDialog(thisActivity);
			}			
		}		
		
	}
	
	/**
	 * @deprecated use {@link #logIn(final Activity thisActivity)}
	 * Displays authentication dialog and takes the user through
	 * the entire authentication process.
	 * 
	 * @param thisActivity Calling party activity
	 */
	public void showAuthenticationDialog(final Activity thisActivity) {

		if (mAuthenticated) {
			new AlertDialog.Builder(mContext).setMessage("Already logged in, please logout first").setPositiveButton("OK", null).setCancelable(true).create().show();
		}
		else {
			if (mAllowTraditionalLogin) {
				GUIHelper.showEnterUserAndPassword(mContext, "", new LoginResult() {

					@Override
					public void result(boolean res, String username, String password) {
						Log.d(TAG, "username/password = " + username + " / " + password);
						
						if (res) {
							traditionalLogin(username, password);
						}
						else {
							// Causes Janrain to initiate login activity by showing login dialog
							// See callbacks jrAuthenticationDidReachTokenUrl() and jrAuthenticationDidSucceedForUser()
							// to see mAuthenticated getting set
							mEngage.showAuthenticationDialog(thisActivity);
							
						}
					}
		    	}); 			
			}
			else {
				// Causes Janrain to initiate login activity by showing login dialog
				// See callbacks jrAuthenticationDidReachTokenUrl() and jrAuthenticationDidSucceedForUser()
				// to see mAuthenticated getting set
				mEngage.showAuthenticationDialog(thisActivity);
			}			
		}
	}
	
	
	public void traditionalLogin(String username, String password) {
        UserServices us;		
        us = new UserServices(mServicesClient);
        Log.d(TAG, "mServicesClient = " + mServicesClient);
        mProgressDialog = ProgressDialog.show(mContext, "", "Logging you in", true, false);

        us.Login(username, password, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
            	Log.d(TAG, "response = " + response);
            	mLoggedInAsTraditional = true;
            	mAuthenticated = true;
                new AlertDialog.Builder(mContext).setMessage("Login was successful.").setPositiveButton("OK", null).setCancelable(true).create().show();
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Log.e(TAG, e.getMessage());
                new AlertDialog.Builder(mContext).setMessage("Login failed.").setPositiveButton("OK", null).setCancelable(true).create().show();
            }

            @Override
            public void onFinish() {
                mProgressDialog.hide();
                mProgressDialog.dismiss();
            }
        });        
	}
	
	void traditionalLogout() {
	       UserServices us;		
	        us = new UserServices(mServicesClient);
	        Log.d(TAG, "mServicesClient = " + mServicesClient);

	        mServicesClient.setCookieStore(mCookieStore);
	        
	        mProgressDialog = ProgressDialog.show(mContext, "", "Logging you out", true, false);

	        us.Logout(new AsyncHttpResponseHandler() {
	            @Override
	            public void onSuccess(String response) {
	            	Log.d(TAG, "response = " + response);
	            	mLoggedInAsTraditional = false;
	            	mAuthenticated = false;
	                new AlertDialog.Builder(mContext).setMessage("Logout was successful.").setPositiveButton("OK", null).setCancelable(true).create().show();
	            }

	            @Override
	            public void onFailure(Throwable e, String response) {
	                Log.e(TAG, e.getMessage());
	                new AlertDialog.Builder(mContext).setMessage("Logout failed.").setPositiveButton("OK", null).setCancelable(true).create().show();
	            }

	            @Override
	            public void onFinish() {
	                mProgressDialog.hide();
	                mProgressDialog.dismiss();
	            }
	        });		
		
	}
	
	
	
	/**
	 * Cancells authentication
	 */
	public void logOut() {
		Log.d(TAG, "DataOuthandler Logging out");		
		if (mLoggedInAsTraditional) {
			traditionalLogout();
		}
		
		mAuthenticated = false;
		drupalSessionCookie = null;
		
// If we delete the cookie here, traditional logout will fail		
//		if (mCookieStore != null) {
//			mCookieStore.clear();
//	        mServicesClient.setCookieStore(mCookieStore);   
//		}
        

	}
	
	
	/**
	 *  Enables logging to external log file of entries sent to the database
	 *  
	 * @param context	Calling party's context
	 */
	public void enableLogging(Context context) {
		try {
			mLogWriter = new LogWriter(context);	
			String logFileName = mUserId + "_" + mSessionDate + ".log";			
			mLogWriter.open(logFileName, true);	
			mLoggingEnabled = true;
			
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSSZ", Locale.US);
			String timeId = sdf.format(new Date());			
			
			PackageManager packageManager = context.getPackageManager();
			PackageInfo info = packageManager.getPackageInfo(context.getPackageName(), 0);			
			mApplicationVersion = info.versionName;
			
			if (mLogFormat == LOG_FORMAT_JSON) {
				String preamble = String.format(
						"{\"userId\" : \"%s\",\n" +
						"\"sessionDate\" : \"%s\",\n" + 
						"\"timeId\" : \"%s\",\n" + 
						"\"versionId\" : \"%s\",\n" + 
						"\"data\":[",  
						mUserId, mSessionDate, timeId, mApplicationVersion);
				mLogWriter.write(preamble);
			}
		} catch (Exception e) {
			Log.e(TAG, "Exception enabling logging: " + e.toString());
		}
	}

	/**
	 * Enables cat file logging of data puts
	 */
	public void enableLogCat() {
		mLogCatEnabled = true;
	}	
	
	
	/**
	 * Purges and closes the current log file.
	 */
	public void purgeLogFile() {
		if (mLoggingEnabled && mLogWriter != null) {
			if (mLogFormat == LOG_FORMAT_JSON) {
				mLogWriter.write("],}");
			}
			mLogWriter.close();
			
			enableLogging(mContext);			
		}		
	}
	
	/**
	 * Closes out any open log files and data connections
	 */
	public void close() {

		mServicesClient.mAsyncHttpClient.cancelRequests(mContext, true);
		
		Log.d(TAG, " ***********************************closing ******************************");
		if (mLoggingEnabled && mLogWriter != null) {
			if (mLogFormat == LOG_FORMAT_JSON) {
				mLogWriter.write("],}");
			}
			mLogWriter.close();			
		}
		
		if(mDispatchThread != null) {
			mDispatchThread.cancel();
			mDispatchThread.interrupt();
			mDispatchThread = null;
		}
		
        if (mEngage != null) {
//        	mEngage.removeDelegate((JREngageDelegate) context);
        	mEngage.removeDelegate(this);
        }	mT2AuthDelegate = null;
		mAuthenticated = false;
	}
	
	public void handleDataOut(final DataOutPacket dataOutPacket) throws DataOutHandlerException {

		if (mRequiresAuthentication == true && mAuthenticated == false) {
			throw new DataOutHandlerException("User is not authenticated");
		}
		
		if (mDatabaseEnabled) {
			dataOutPacket.mQueuedAction = "C";
			Log.d(TAG, "Queueing document " + dataOutPacket.mRecordId);

			synchronized(mDbCache) {
				mDbCache.addPacketToCacheWithSendingStatus(dataOutPacket);
			}
        	if (mDatabaseUpdateListener != null) {
        		mDatabaseUpdateListener.remoteDatabaseCreateUpdateComplete(dataOutPacket);
        	}						
		}		
	}	

	/**
	 * Updates a packet in the database
	 * 
	 * See handleDataOut() for the data path (Drupal)
	 * 
	 * @param packet Packet to update
	 * @throws DataOutHandlerException
	 */
	public void updateRecord(final DataOutPacket dataOutPacket) throws DataOutHandlerException {
		
		if (mRequiresAuthentication == true && mAuthenticated == false) {
			throw new DataOutHandlerException("User is not authenticated");
		}		
			
		SqlPacket sqlPacket = mDbCache.db.getPacketByRecordId(dataOutPacket.mRecordId);
		if (sqlPacket == null) {
			throw new DataOutHandlerException("Packet RecordID " + dataOutPacket.mRecordId + " does not exist in Cache");
		}
		else {
			Log.d(TAG, "Updating record " + dataOutPacket.mRecordId);
			// We now have the original sql Packet corresponding to this dataOutPacket
			// We must transfer the changes from the dataOutPAcket to the sqlPAcket
			// before we do the update.
			SqlPacket sqlPacketNew = new SqlPacket(dataOutPacket);
			sqlPacketNew.setSqlPacketId(sqlPacket.getSqlPacketId());
			
			int retVal = mDbCache.db.updateSqlPacket(sqlPacketNew);	
			Log.d(TAG, "Updated: retVal = " + retVal + " record id = " + dataOutPacket.mRecordId);
			SqlPacket updatedSqlPacketFromSql = mDbCache.db.getPacketByRecordId(dataOutPacket.mRecordId);
			
			// The timed task will take care of updating it in Drupal 
            if (mInstance.mDatabaseUpdateListener != null) {
            	mInstance.mDatabaseUpdateListener.remoteDatabaseCreateUpdateComplete(dataOutPacket);
            } 			
		}
	}
	
	public void deleteRecord(final DataOutPacket dataOutPacket) throws DataOutHandlerException {
		
		if (mRequiresAuthentication == true && mAuthenticated == false) {
			throw new DataOutHandlerException("User is not authenticated");
		}		

		if (mDatabaseEnabled) {
				mNodeDeleteQueue.add(dataOutPacket.mRecordId);
				mDbCache.deletePacketFromCacheWithDeletingStatus(dataOutPacket);
				// The timed task will take care of deleting it from Drupal 

				if (mInstance.mDatabaseUpdateListener != null) {
                	mInstance.mDatabaseUpdateListener.remoteDatabaseDeleteComplete(dataOutPacket);
                }   				
		}		
	}
	
	/**
	 * @author scott.coleman
	 *
	 * This thread handles maintenance of the mPendingQueue
	 * sending data out if the network is available.
	 * 
	 */
	class DispatchThread extends Thread {
		private boolean isRunning = false;
		private boolean cancelled = false;

		@Override
		public void run() {
			isRunning = true;
			
			while(true) {
				// Break out if this was cancelled.
				if(cancelled) {
					break;
				}
				

				for (int i = 1; i < 4; i++) {
					try {
						Thread.sleep(1000);
						if(cancelled) {
							break;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
				}
				
		

				Log.d(TAG, "Http dispatch thread tick");

				// If the network is available post entries from the PendingQueue
				if (isNetworkAvailable()) {
						if (mRequiresAuthentication) {
							if (mAuthenticated == true) {
								ProcessCacheThread();		
							}
						}
						else {
							ProcessCacheThread();		
						}
						
								

				} // End if (isNetworkAvailable())
			} // End while(true)
				
			isRunning = false;
		} // End public void run() 
		
		/**
		 * Formats a string that can be use for log files.
		 *  format is based on log type and database type 
		 * @param databaseType Type of database
		 * @param pairs Map of data entry
		 * 
		 * @return Formatted string based
		 */
		String formatTextForLog(int databaseType, Map.Entry pairs) {
			String result = "";
			
	        if (pairs.getValue() instanceof Integer) {
				if (mLogFormat == LOG_FORMAT_JSON) {
					result = "\"" + pairs.getKey() + "\":" + pairs.getValue()  + ",";			
				}
				else {
					result = "" + pairs.getValue()  + ",";			
				}							        	
	        }
	        if (pairs.getValue() instanceof String) {
				if (mLogFormat == LOG_FORMAT_JSON) {
					result = "\"" + pairs.getKey() + "\":\"" + pairs.getValue() + "\",";			
				}
				else {
					result = "" + pairs.getValue() + ",";			
				}							        	
	        }
	        if (pairs.getValue() instanceof Long) {
				if (mLogFormat == LOG_FORMAT_JSON) {
					result = "\"" + pairs.getKey() + "\":" + pairs.getValue() + ",";			
				}
				else {
					result = "" + pairs.getValue() + ",";			
				}							        	
	        }
	        if (pairs.getValue() instanceof Double) {
				if (mLogFormat == LOG_FORMAT_JSON) {
					result = String.format("\"%s\":\"%f\",", pairs.getKey(),pairs.getValue());
				}
				else {
					result = "" + pairs.getValue() + ",";			
				}							        	
	        }
	        if (pairs.getValue() instanceof Vector) {
				if (mLogFormat == LOG_FORMAT_JSON) {
					ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();						
					for (Object v : (Vector) pairs.getValue()) {
						ObjectNode valueNode = JsonNodeFactory.instance.objectNode();		
						valueNode.put("value", v.toString());
						arrayNode.add(v.toString());	
					}
					result = "\"" + pairs.getKey() + "\":" + arrayNode.toString() + ",";
				}
				else {
					result = "" + pairs.getValue().toString() + ",";			
				}							        	
	        }								
			
			
			return result;
		}

		/**
		 * Cancel the loop
		 */
		public void cancel() {
			this.cancelled = true;
			Log.e(TAG, "Cancelled");
			
		}
		
		/**
		 * 
		 * @return true if running false otherwise
		 */
		public boolean isRunning() {
			return this.isRunning;
		}
	} // End DispatchThread
	
	

	
    /**
     * @return true if network is available
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) 
          mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }	

	/**
	 * Logs a text note to sinks
	 * 
	 * @param note - Text not to log to sinks
	 * @throws DataOutHandlerException 
	 */
	public void logNote(String note) throws DataOutHandlerException {
		DataOutPacket packet = new DataOutPacket();
		packet.add(DataOutHandlerTags.NOTE, note);
		handleDataOut(packet);				
	}
    
	@Override
	public void jrAuthenticationDidSucceedForUser(JRDictionary auth_info,
			String provider) {
		Log.d(TAG, "jrAuthenticationDidSucceedForUser");		
		
	    mAuth_info = auth_info;
		// Note, if we're using drupal the authentication isn't 
		// really done until the callback URL has been called
		// This sets up the Drupal Database
		if (mDatabaseType == DATABASE_TYPE_T2_DRUPAL) {
			
		} else {
			mAuthProvider = provider;		
			mAuthenticated = true;
			
			if (mT2AuthDelegate != null) {
				mT2AuthDelegate.T2AuthSuccess(mAuth_info, mAuthProvider, null, null);
			}
		}
	}

	@Override
	public void jrAuthenticationDidReachTokenUrl(String tokenUrl,
			HttpResponseHeaders responseHeaders,String responsePayload,
			String provider) {
		Log.d(TAG, "jrAuthenticationDidReachTokenUrl");		

		// Check to see of Janrain is supplying a drupal session cookie
		org.apache.http.cookie.Cookie[] mSessionCookies;
		mSessionCookies = responseHeaders.getCookies();
		
		for (Cookie cookie : mSessionCookies) {
        	System.out.println("Cookie! - " + cookie.toString());
        	System.out.println("Cokie Name - " + cookie.getName());
        	
        	if (cookie.getName().startsWith("SESS") || cookie.getName().startsWith("SSESS")) {
        		drupalSessionCookie = cookie;
        		if (VERBOSE_LOGGING)
        			Log.e(TAG, "saving session cookie: " + cookie.toString()); 
        	}			
		}
		
		mAuthenticated = true;
		if (mT2AuthDelegate != null) {
			mT2AuthDelegate.T2AuthSuccess(mAuth_info, mAuthProvider, responseHeaders, responsePayload);
		}
	}

	@Override
	public void jrSocialDidPublishJRActivity(JRActivityObject activity,
			String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void jrSocialDidCompletePublishing() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void jrEngageDialogDidFailToShowWithError(JREngageError error) {
		Log.d(TAG, "jrEngageDialogDidFailToShowWithError");		

		// TODO Auto-generated method stub
		
	}

	@Override
	public void jrAuthenticationDidNotComplete() {
		Log.d(TAG, "jrAuthenticationDidNotComplete");		
		
	}

	@Override
	public void jrAuthenticationDidFailWithError(JREngageError error,
			String provider) {
		Log.d(TAG, "jrAuthenticationDidFailWithError");		
		mAuthenticated = false;
		
		if (mT2AuthDelegate != null) {
			mT2AuthDelegate.T2AuthFail(error, provider);
		}		
	}

	@Override
	public void jrAuthenticationCallToTokenUrlDidFail(String tokenUrl,
			JREngageError error, String provider) {
		Log.d(TAG, "jrAuthenticationCallToTokenUrlDidFail");		
		mAuthenticated = false;
		if (mT2AuthDelegate != null) {
			mT2AuthDelegate.T2AuthFail(error, provider);
		}		
	}

	@Override
	public void jrSocialDidNotCompletePublishing() {
		if (mT2AuthDelegate != null) {
			mT2AuthDelegate.T2AuthNotCompleted();
		}		
	}

	@Override
	public void jrSocialPublishJRActivityDidFail(JRActivityObject activity,
			JREngageError error, String provider) {
		// TODO Auto-generated method stub
	} 	
	
	
	
	// --------------------------------------------------
	// New caching method
	// --------------------------------------------------
	
	
	private String createDrupalPacketString(DataOutPacket dataOutPacket) {
		ObjectNode item = JsonNodeFactory.instance.objectNode();
		item.put("title", dataOutPacket.mRecordId);
		item.put("type", "sensor_data");
		item.put("language", "und");										

		Iterator it = dataOutPacket.mItemsMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry)it.next();	
	        if (pairs.getValue() instanceof Integer) {
	        	putDrupalNode((String)pairs.getKey(), (Integer)pairs.getValue(), item);								        	
	        	dataOutPacket.mLoggingString += formatTextForLog(mDatabaseType, pairs);
	        }
	        if (pairs.getValue() instanceof String) {
	        	putDrupalNode((String)pairs.getKey(), (String)pairs.getValue(), item);								        	
	        	dataOutPacket.mLoggingString += formatTextForLog(mDatabaseType, pairs);
	        }
	        if (pairs.getValue() instanceof Long) {
	        	putDrupalNode((String)pairs.getKey(), (Long)pairs.getValue(), item);								        	
	        	dataOutPacket.mLoggingString += formatTextForLog(mDatabaseType, pairs);
	        }
	        if (pairs.getValue() instanceof Double) {
	        	putDrupalNode((String)pairs.getKey(), (Double)pairs.getValue(), item);								        	
	        	dataOutPacket.mLoggingString += formatTextForLog(mDatabaseType, pairs);
	        }
	        if (pairs.getValue() instanceof Float) {
	        	putDrupalNode((String)pairs.getKey(), (Float)pairs.getValue(), item);								        	
	        	dataOutPacket.mLoggingString += formatTextForLog(mDatabaseType, pairs);
	        }
	        if (pairs.getValue() instanceof Vector) {
				// Note special format for vector in drupal!
				String newTag = "field_" + ((String) pairs.getKey()).toLowerCase();
				ObjectNode undNode = JsonNodeFactory.instance.objectNode();		
				ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();		

	        	// TODO: Potential problem - saves all of the vector items as type STRING
				for (Object v : (Vector) pairs.getValue()) {
					ObjectNode valueNode = JsonNodeFactory.instance.objectNode();		
					valueNode.put("value", v.toString());
					arrayNode.add(valueNode);	
				}
				
				undNode.put("und", arrayNode);			
				item.put(newTag, undNode);									        	
				dataOutPacket.mLoggingString += formatTextForLog(mDatabaseType, pairs);
	        }										
			
		} // End while (it.hasNext())
		return item.toString();
	}
	
	/**
	 * Writes drual formatted node to specified node (String)
	 * 
	 * @param tag Data Tag
	 * @param value Data Value
	 * @param node Node to write to 
	 */
	private void putDrupalNode(String tag, String value, ObjectNode node) {
		String newTag = "field_" + tag.toLowerCase();
		ObjectNode valueNode = JsonNodeFactory.instance.objectNode();		
		ObjectNode undNode = JsonNodeFactory.instance.objectNode();		
		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();		
		valueNode.put("value", value);
		arrayNode.add(valueNode);	
		undNode.put("und", arrayNode);			
		node.put(newTag, undNode);
	}
	
	/**
	 * Writes drual formatted node to specified node (Long)
	 * 
	 * @param tag Data Tag
	 * @param value Data Value
	 * @param node Node to write to 
	 */
	private void putDrupalNode(String tag, long value, ObjectNode node) {
		String newTag = "field_" + tag.toLowerCase();
		ObjectNode valueNode = JsonNodeFactory.instance.objectNode();		
		ObjectNode undNode = JsonNodeFactory.instance.objectNode();		
		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();		
		valueNode.put("value", value);
		arrayNode.add(valueNode);	
		undNode.put("und", arrayNode);			
		node.put(newTag, undNode);
	}
	
	/**
	 * Writes drual formatted node to specified node (Int)
	 * 
	 * @param tag Data Tag
	 * @param value Data Value
	 * @param node Node to write to 
	 */
	private void putDrupalNode(String tag, int value, ObjectNode node) {
		String newTag = "field_" + tag.toLowerCase();
		ObjectNode valueNode = JsonNodeFactory.instance.objectNode();		
		ObjectNode undNode = JsonNodeFactory.instance.objectNode();		
		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();		
		valueNode.put("value", value);
		arrayNode.add(valueNode);	
		undNode.put("und", arrayNode);			
		node.put(newTag, undNode);
	}
	
	/**
	 * Writes drual formatted node to specified node (Double)
	 * 
	 * @param tag Data Tag
	 * @param value Data Value
	 * @param node Node to write to 
	 */
	private void putDrupalNode(String tag, double value, ObjectNode node) {
		String newTag = "field_" + tag.toLowerCase();
		ObjectNode valueNode = JsonNodeFactory.instance.objectNode();		
		ObjectNode undNode = JsonNodeFactory.instance.objectNode();		
		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();		
		valueNode.put("value", value);
		arrayNode.add(valueNode);	
		undNode.put("und", arrayNode);			
		node.put(newTag, undNode);
	}
	
	/**
	 * Formats a string that can be use for log files.
	 *  format is based on log type and database type 
	 * @param databaseType Type of database
	 * @param pairs Map of data entry
	 * 
	 * @return Formatted string based
	 */
	String formatTextForLog(int databaseType, Map.Entry pairs) {
		String result = "";
		
        if (pairs.getValue() instanceof Integer) {
			if (mLogFormat == LOG_FORMAT_JSON) {
				result = "\"" + pairs.getKey() + "\":" + pairs.getValue()  + ",";			
			}
			else {
				result = "" + pairs.getValue()  + ",";			
			}							        	
        }
        if (pairs.getValue() instanceof String) {
			if (mLogFormat == LOG_FORMAT_JSON) {
				result = "\"" + pairs.getKey() + "\":\"" + pairs.getValue() + "\",";			
			}
			else {
				result = "" + pairs.getValue() + ",";			
			}							        	
        }
        if (pairs.getValue() instanceof Long) {
			if (mLogFormat == LOG_FORMAT_JSON) {
				result = "\"" + pairs.getKey() + "\":" + pairs.getValue() + ",";			
			}
			else {
				result = "" + pairs.getValue() + ",";			
			}							        	
        }
        if (pairs.getValue() instanceof Double) {
			if (mLogFormat == LOG_FORMAT_JSON) {
				result = String.format("\"%s\":\"%f\",", pairs.getKey(),pairs.getValue());
			}
			else {
				result = "" + pairs.getValue() + ",";			
			}							        	
        }
        if (pairs.getValue() instanceof Vector) {
			if (mLogFormat == LOG_FORMAT_JSON) {
				ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();						
				for (Object v : (Vector) pairs.getValue()) {
					ObjectNode valueNode = JsonNodeFactory.instance.objectNode();		
					valueNode.put("value", v.toString());
					arrayNode.add(v.toString());	
				}
				result = "\"" + pairs.getKey() + "\":" + arrayNode.toString() + ",";
			}
			else {
				result = "" + pairs.getValue().toString() + ",";			
			}							        	
        }								
		
		
		return result;
	}
	
	// Rules for presence of drupal node id in the cache
	// If synced from DB (added by another client):
	//		drupal ID is present immediately
	// If added from the UI
	//		drupal id is blank until the first timed update (ProcessCacheThread()).
	
	
	// To be called from DispatchThread
	private void ProcessCacheThread() {
		
		final ArrayList<String> mDrupalRecordIdList = new ArrayList<String>();			
		final ArrayList<String> mSqlRecordIdList;		
		final HashMap<String, String> mRecordIdToDrupalIdMap = new HashMap<String, String>();	
		final HashMap<String, String> mDrupalIdToRecordIdMap = new HashMap<String, String>();	

		Log.d(TAG, "ProcessCacheThread()");
		
		
		mSqlRecordIdList = (ArrayList<String>) mDbCache.getSqlIdList();	             

		
        // -------------------------------------
        // Get Drupal Node Summary:
        // -------------------------------------
		UserServices us;
	   	us = new UserServices(mServicesClient);
	
	    JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {
	    	
	    	@Override
			protected void handleSuccessJsonMessage(Object arg0) {
	    		if (VERBOSE_LOGGING) {
        			Log.e(TAG, "handleSuccessJsonMessage(Object arg0");
//    	     		Log.e(TAG, "Drupal Node Summary: " + array.toString());
        		}
	    		
	     		JSONArray array = (JSONArray) arg0;

	     			
	            for (int i = 0; i < array.length(); i++) {
	            	JSONObject jObject  = (JSONObject) array.opt(i);
	            	try {
	            		String userId = (String) jObject.get("uid");
	            		String nodeId = (String) jObject.get("nid");
	            		String recordId = (String) jObject.get("title");

	            		 // TODO: temp only look at records from scott.coleman
//	            		 if (userId.equalsIgnoreCase("113")) {
		            		 // Check to see if this is a valid record
		            	if (recordId.length()  >= 14 && recordId.charAt(13) == '-') {
		            		mDrupalRecordIdList.add(recordId);
			            	 mRecordIdToDrupalIdMap.put(recordId, nodeId);
			            	 mDrupalIdToRecordIdMap.put(nodeId, recordId);		            		 
//		            		 }
	            		 }
	            		 
	            	}	catch (JSONException e) {
	            		 e.printStackTrace();
	            	}	
	             }

        		if (VERBOSE_LOGGING) {
   	             	Log.e(TAG, "Updated mDrupalRecordIdList, mRecordIdToDrupalIdMap, and mDrupalIdToRecordIdMap");
   	             	Log.e(TAG, "mDrupalRecordIdList = " + mDrupalRecordIdList.toString());
//   	            Log.e(TAG, "mSqlRecordIdList = " + mSqlRecordIdList.toString());
//   	            Log.e(TAG, "mNodeDeleteQueue = " + mNodeDeleteQueue.toString());
        		}

	             
	             
	             // At this point 
	             //   mDrupalIdList contains a list of all record id's in Drupal
	             //   mSqlIdList contains a list of all record id's in the SQL cache
	             //	  mDrupalIdMap contains a map of record id's to node id's in Drupal
	             
	 			synchronized(mDbCache) {
	 				mDbCache.updateDrupalIds(mRecordIdToDrupalIdMap);
				}	             
			}
	
			@Override
	        public void onSuccess(JSONObject response) {
        		if (VERBOSE_LOGGING) {
   	             Log.e(TAG, "onSuccess(JSONObject response) ");
        		}
	        }
	
			@Override
			public void onSuccess(JSONArray arg0) {
        		if (VERBOSE_LOGGING) {
   	             	Log.e(TAG, "onSuccess(JSONArray arg0) ");
        		}
				super.onSuccess(arg0);
			}
	
	         
	         @Override
	        public void onFailure(Throwable e, JSONObject response) {
	        	 Log.e(TAG, "OnFailure(Throwable e, JSONObject response) " + e.toString());
	        }
	         
	         @Override
			public void onFailure(Throwable arg0, JSONArray arg1) {
	             Log.e(TAG, "OnFailure(Throwable arg0, JSONArray arg1) " + arg0.toString());
			}
	
				@Override
	        public void onFinish() {
	             Log.d(TAG, "onFinish(Drupal Node Summary)");
	             
	             // Now do processing of cache items, comparing what's on the device
	             // to what's in the remote database.
	             if (true) {
	            	 
	            	 // We need to update any records that have been successfully added
	            	 // to update their cache status to idle (so they don't get sent again)
			 		 synchronized(mDbCache) {
			 			 Iterator<String> i = mDrupalIdsSuccessfullyAdded.iterator();
		            	 while(i.hasNext()) {
		            		 String drupalId = i.next();
		            		 String recordId = mDrupalIdToRecordIdMap.get(drupalId);
		            		 if (recordId != null) {
		            			 SqlPacket sqlPacket = mDbCache.db.getPacketByRecordId(recordId);
		            			 if (sqlPacket != null) {
		            				 if (VERBOSE_LOGGING) {
			            				 Log.e(TAG, "setting RecordId/DrupalId " + recordId + ", " + drupalId + " to idle");
		            				 }
		     	                    // Now set the status of the cache packet to idle
		     		 				sqlPacket.setCacheStatus(SqlPacket.CACHE_IDLE);
									mDbCache.db.updateSqlPacket(sqlPacket);
		     		 				i.remove();
								}										
		            		}
		            	 }
			 		}
		            	 
		            for (String recordId : mSqlRecordIdList) {
		            	if (!mDrupalRecordIdList.contains(recordId)) {
		            		
		            		if (VERBOSE_LOGGING) {
			        	        Log.e(TAG, "recordId: " + recordId + " - Packet exists in Cache but not in DB");
		            		}
	
	        	        	SqlPacket sqlPacket = mDbCache.db.getPacketByRecordId(recordId); // Since recordId is in mSqlRecordIdList we know this will not return null
			            	// Exists in cache but not on on Drupal
		        	        // Two possible cases here
		        	        // 1 Packet newly inserted by self        -> Add (send) the packet to the DB
		        	        // 2 Packet deleted by other from DB      -> Remove the packet from the cache
	        	        	Boolean isSendingOrSend = (sqlPacket.getCacheStatus() == SqlPacket.CACHE_SENDING || 
	        	        			sqlPacket.getCacheStatus() == SqlPacket.CACHE_SENT);
			            	if (isSendingOrSend) {
			            		
				            	// Packet exists in cache but not on on Drupal
			            		// Case 1 - Packet newly inserted by self        -> Add (send) the packet to the DB
			            		if (VERBOSE_LOGGING) {
			            			Log.e(TAG, "Case 1 - Send the packet to DB");
			            		}
	
		        	        	// Don't send the packet if already sending!
		        	        	if (isSendingOrSend) {
			        	        	DataOutPacket dataOutPacket;
				            		if (VERBOSE_LOGGING) {
				            			Log.e(TAG, "Status Sending/Sent - Sending packet to remote database ");
				            		}
									try {
										
							 			synchronized(mDbCache) {
							 				sqlPacket.setCacheStatus(SqlPacket.CACHE_SENT);
											mDbCache.db.updateSqlPacket(sqlPacket);
										}										
										
										dataOutPacket = new DataOutPacket(sqlPacket);
				        	        	sendPacketToRemoteDbSync(dataOutPacket, "C", "");
				        	        	
									} catch (DataOutHandlerException e) {
										Log.e(TAG, e.toString());
										e.printStackTrace();
									}			            	
		        	        		
		        	        	}
		        	        	else {
				            		if (VERBOSE_LOGGING) {
				            			Log.e(TAG, "Status Sent - Packet already sent");
				            		}
		        	        		
		        	        	}
		        	        	
							}
			            else {
			            		// Packet exists in cache but not on on Drupal
			            		// Case 2 - Packet deleted by other from DB      -> Remove the packet from the cache
		            			if (VERBOSE_LOGGING) {
		            				Log.e(TAG, "Case 2 - Remove the packet from the cache");
		            			}
			            		mDbCache.deletePacketFromCache(sqlPacket);
	
			            		DataOutPacket dataOutPacket;
								try {
									// TODO: calling this too often slows the UI
									dataOutPacket = new DataOutPacket(sqlPacket);
				            		if (mInstance.mDatabaseUpdateListener != null) {
				                    	mInstance.mDatabaseUpdateListener.remoteDatabaseDeleteComplete(dataOutPacket);
				                    } 				            		
								} catch (DataOutHandlerException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
			            	}
			            				 
	
		            	 
		            	 } // if (!mDrupalIdList.contains(id))
		            	 
		            	 
		        	 }	  // end for (String id : mSqlIdList) 	
	 
		             for (String drupalRecordId : mDrupalRecordIdList) {
		            	 
		            	 if (!mSqlRecordIdList.contains(drupalRecordId)) {
		            		 //SqlPacket sqlPacket = mDbCache.db.getPacketByRecordId(drupalRecordId); // Can't do this if not in CACHE!!!!!!
		            		 
			            		if (VERBOSE_LOGGING) {
			            			Log.e(TAG, "recordId: " + drupalRecordId + " - Packet exists in DB but not in Cache");
			            		}
	
		        	          // Packet exists in DB but not in Cache
		        	          // Two possible cases here:
			        	      // 3 Packeted deleted by self                -> Delete packet from DB
		        	          // 4 Packet newly inserted by other into DB  -> Add packet to Cache
	
		        	          //Log.e(TAG, "mNodeDeleteQueue = " + mNodeDeleteQueue.toString());
		        	          
		        	          boolean listContainsId = false;
		        	          for (String id : mNodeDeleteQueue) {
		        	        	  if (id != null && id.equalsIgnoreCase(drupalRecordId)) {
		        	        		  listContainsId = true;
		        	        		  break;
		        	        	  }
		        	          }
		        	          // Determine which of the cases we have here
		        	          if (listContainsId) {

		        	        	  // Packet exists in DB but not in Cache
		        	        	  // Case 3 - Packeted deleted by self                -> Delete packet from DB
		        	        	  if (VERBOSE_LOGGING) {
		        	        		  Log.e(TAG, "Case 3 - Delete packet from DB");
		        	        	  }
			        	          // Get the drupal id for this record id
			        	          String drupalId = mRecordIdToDrupalIdMap.get(drupalRecordId);
			        	          if (drupalId != null) {
			        	        	  sendPacketToRemoteDbSync(null, "D", drupalId);
			        	          }
		        	          }
		        	          else {
			        	          // Packet exists in DB but not in Cache
		        	        	  // Case 4 - Packet newly inserted by other into DB  -> Add packet to Cache
		        	        	  if (VERBOSE_LOGGING) {
		        	        		  Log.e(TAG, "Case 4 - Add packet to cache");
		        	        	  }
			        	          String drupalId = mRecordIdToDrupalIdMap.get(drupalRecordId);
			        	          addPacketToCacheSync(drupalId);	// Grabs the packet from Drupal and adds it to the Cache        	          
		        	          }
		            	 }
		             } // End  for (String drupalRecordId : mDrupalRecordIdList)
			             
	             }
                try {
            		if (VERBOSE_LOGGING) {
      	          		Log.e(TAG, "Done processing all Drupal check entries");
            		}

      	          	synchronized(updateCacheSyncToken) {
					 	updateCacheSyncToken.notifyAll();	             
					}
				} catch (Exception e) {
					Log.e(TAG, e.toString());
					e.printStackTrace();
				}	             
	         } // void onFinish()
	     };        
	     
 		if (VERBOSE_LOGGING) {
 			Log.e(TAG, "Requesting drupal node summary");
 		}
	     us.NodeGet(responseHandler);
	     
 		if (VERBOSE_LOGGING) {
 			Log.e(TAG, "Wait for UpdateCacheSyncToken");
 		}
        synchronized (updateCacheSyncToken)
        {
            try {
            	updateCacheSyncToken.wait(SYNC_TIMEOUT);
            } catch (InterruptedException e) {
            	Log.e(TAG, e.toString());
                e.printStackTrace();
            }
        }
		if (VERBOSE_LOGGING) {
			Log.e(TAG, "Done Waiting for UpdateCacheSyncToken");
		}
	}

    /**
     * Synchronous version of sendPacketToRemoteDbSync.
     * Doesn't return until HTTP transaction is either complete or has timed out.
     * @param dataOutPacket
     * @param queuedAction
     * @param drupalNodeId
     */
    void sendPacketToRemoteDbSync(final DataOutPacket dataOutPacket, final String queuedAction, final String drupalNodeId) {
		if (VERBOSE_LOGGING) {
			Log.e(TAG, "Waiting for sendPacketToRemoteDbToken");
		}
        synchronized (sendPacketToRemoteDbToken)
        {
        	sendPacketToRemoteDb(dataOutPacket, queuedAction, drupalNodeId);
            try {
            	sendPacketToRemoteDbToken.wait(SYNC_TIMEOUT);
            } catch (InterruptedException e) {
                Log.e(TAG, e.toString());
            	e.printStackTrace();
            }
        }
		if (VERBOSE_LOGGING) {
			Log.e(TAG, "Done Waiting for sendPacketToRemoteDbToken");
		}
    }	
	
	
    /**
     * Sends a dataOutPAcket Drupal database for processing
     * 
     * @param jsonString
     */
    void sendPacketToRemoteDb(final DataOutPacket dataOutPacket, final String queuedAction, final String drupalNodeId) {
        UserServices us;

        String jsonString = "";      
        
        if (dataOutPacket != null) {

            Log.d(TAG, "sendPacketToRemoteDb(" + dataOutPacket.mRecordId + ")");
            jsonString = createDrupalPacketString(dataOutPacket);        
        }
        else {
            Log.d(TAG, "sendPacketToRemoteDb - deleting drupal node id  + " + drupalNodeId + ")");
        }
        
		// Check to see if we've stored a Drupal session cookie. If so then attach then to 
        // the http client
        if (drupalSessionCookie != null) {
          mCookieStore.addCookie(drupalSessionCookie);
          mServicesClient.setCookieStore(mCookieStore);        

  			if (VERBOSE_LOGGING) {
  				Log.e(TAG, "Using session cookie: " + drupalSessionCookie.toString());
  			}
        }
        else {
        	if (!mLoggedInAsTraditional) {
        		// For traditional login the cookies are implicit
        		Log.e(TAG, "No Stored Cookies to use: ");
        	}
        }  	        
        
        us = new UserServices(mServicesClient);

        JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {
            
        	// We get here for Create or Update operations
        	@Override
            public void onSuccess(JSONObject response) {
                try {
                    String nid = response.getString("nid");
                    Log.d(TAG, "Successfully submitted article # " + nid);
                    
                    // We can't set the cache to sent yet since it's not been associated with
                    // a drupal id yet. That comes with the first Drupal node summary list 
                    // returned from Drupal. We do this processing in ProcessCache()
                    mDrupalIdsSuccessfullyAdded.add(nid);                    
	 				
                } catch (JSONException e) {
                    Log.e(TAG, e.toString());
                }
            }

        	// We get here for Delete operations
			@Override
			public void onSuccess(JSONArray arg0) {
                Log.d(TAG, "Successfully submitted ARRAY (Deleted record from drupal)" + arg0.toString());
//                removePacketFromRemoteDrupalPacketCache();  
                updatemNodeDeleteQueue();
			}
            
            @Override
            public void onFailure(Throwable e, JSONObject response) {
                Log.e(TAG, e.toString());
            	if (mDatabaseUpdateListener != null) {
            		mDatabaseUpdateListener.remoteDatabaseFailure(e.toString());
            	}	                 
            }
            
            @Override
			public void onFailure(Throwable arg0, JSONArray arg1) {
                Log.e(TAG, arg0.toString());
            	if (mDatabaseUpdateListener != null) {
            		mDatabaseUpdateListener.remoteDatabaseFailure(arg0.toString());
            	}	                 
			}

			@Override
            public void onFinish() {
				
				if (queuedAction.equalsIgnoreCase("D")) {
	                Log.d(TAG, "onFinish(" + drupalNodeId + ")");
				}
				else {
					if ( dataOutPacket != null)		
						Log.d(TAG, "onFinish(" + dataOutPacket.mRecordId + ")");
				}
		           synchronized(sendPacketToRemoteDbToken)
		            {
						Log.d(TAG, "onFinish(addPacketToRemoteDrupalPacketCache)");
						sendPacketToRemoteDbToken.notify();
		            }				
            }
        };        
        
        if (queuedAction.equalsIgnoreCase("C")) {
            us.NodePost(jsonString, responseHandler);
        }
        else if (queuedAction.equalsIgnoreCase("U")) {
            us.NodePut(jsonString, responseHandler, drupalNodeId);
        }
        else if (queuedAction.equalsIgnoreCase("D")) {
            us.NodeDelete(responseHandler, drupalNodeId);
        }
    } 		
    
    /**
     * Synchronous version of addPacketToCache.
     * Doesn't return until HTTP transaction is either complete or has timed out.
     * 
     * @param drupalNodeId
     */
    void addPacketToCacheSync(final String drupalNodeId) {
		if (VERBOSE_LOGGING) {
			Log.e(TAG, "Waiting for addPacketToCacheSyncToken");
		}
        addPacketToCache(drupalNodeId);        
        synchronized (addPacketToCacheSyncToken)
        {
            try {
            	addPacketToCacheSyncToken.wait(SYNC_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
		if (VERBOSE_LOGGING) {
			Log.e(TAG, "Done Waiting for addPacketToCacheSyncToken");
		}
    }    	
    
    
    /**
     * Retrieves the contents of the updated packet from Drupal and  updates the Cache
     * 
     * @param drupalNodeId node id of drupal packet added
     */
    void addPacketToCache(final String drupalNodeId) {
        UserServices us;
        int nodeNum = 0;
        Log.d(TAG, "addPacketToCache(" + drupalNodeId + ")");
        us = new UserServices(mServicesClient);    	

        JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {

			@Override
            public void onSuccess(JSONObject response) {
				
					try {
						String recordId = (String) response.get("title");
						Log.d(TAG, "Got object, now adding to cache, recid = " + recordId);
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

                	String drupalNodeContents = response.toString();
                	// Now convert the drupal node to a dataOutPacket.                    	
                	DataOutPacket dataOutPacket;
					try {
						dataOutPacket = new DataOutPacket(response);
						// Make sure to set the drupal id while adding it to the cache
        				synchronized(mDbCache) {
        					mDbCache.addPacketToCache(dataOutPacket, drupalNodeId);
        				}							
	                	
        				// TODO: calling this too often slows down the UI
	                	if (mDatabaseUpdateListener != null) {
	                		mDatabaseUpdateListener.remoteDatabaseCreateUpdateComplete(dataOutPacket);
	                	}						
						
					} catch (DataOutHandlerException e) {
						Log.e(TAG, e.toString());
						//e.printStackTrace();
					}
            }

			@Override
			public void onSuccess(JSONArray arg0) {
				super.onSuccess(arg0);
			}
            
            @Override
            public void onFailure(Throwable e, JSONObject response) {
                Log.e(TAG, e.toString());
            	if (mDatabaseUpdateListener != null) {
            		mDatabaseUpdateListener.remoteDatabaseFailure(e.toString());
            	}	                
            }
            
            @Override
			public void onFailure(Throwable arg0, JSONArray arg1) {
                Log.e(TAG, arg0.toString());
            	if (mDatabaseUpdateListener != null) {
            		mDatabaseUpdateListener.remoteDatabaseFailure(arg0.toString());
            	}	                
                
			}

			@Override
            public void onFinish() {
	           synchronized(addPacketToCacheSyncToken)
	            {
           			if (VERBOSE_LOGGING) {
           				Log.d(TAG, "onFinish(addPacketToCache)");
           			}
					addPacketToCacheSyncToken.notify();
	            }
                
            	
            }
        };        
        
        
    	try {
    		nodeNum = Integer.parseInt(drupalNodeId);
            us.NodeGet(nodeNum, responseHandler);
            
//            Log.e(TAG, "Waiting for addPacketToCacheSyncToken");
//            synchronized (addPacketToCacheSyncToken)
//            {
//                try {
//                	addPacketToCacheSyncToken.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            Log.e(TAG, "Done Waiting for addPacketToCacheSyncToken");            
            
            
		} catch (NumberFormatException e1) {
			Log.e(TAG, e1.toString());
		}
    }		
	
	void updatemNodeDeleteQueue() {
		//mNodeDeleteQueue
	}
	
	public ArrayList<DataOutPacket> getPacketListDOP() {
		return mDbCache.db.getPacketListDOP();
	}
	
}
