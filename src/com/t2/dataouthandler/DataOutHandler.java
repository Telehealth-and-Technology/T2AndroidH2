/*****************************************************************
DataOutHandler

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

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.http.cookie.Cookie;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
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
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
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
 * @author scott.coleman
 *
 */
public class DataOutHandler  implements JREngageDelegate {
					
	private static final String TAG = DataOutHandler.class.getName();
	private static final String VERSION_STRING = "2.1.1";
	
	//private static final String DEFAULT_REST_DB_URL 	= "http://gap.t2health.org/and/phpWebservice/webservice2.php";	 
	// private static final String DEFAULT_REST_DB_URL 	= "http://gap.t2health.org/and/json.php";	 
	private static final String DEFAULT_REST_DB_URL 	= "http://ec2-50-112-197-66.us-west-2.compute.amazonaws.com/mongo/json.php";
	private static final String DEFAULT_AWS_DB_URL 		= "h2tvm.elasticbeanstalk.com";

	private static final String DEFAULT_DRUPAL_DB_URL 	= "http://t2health.us/h2/android/";
//	private static final String DEFAULT_DRUPAL_DB_URL 	= "http://t2health.us/h4hnew/api/";

    private String fred = "{    \"type\": \"check_in\",    \"habit_id\": \"23\",    \"uid\": \"44\",    \"title\": \"new checkin blah blah blah 2dsfsdf nid 99 unset\",    \"log\": \"\",    \"status\": \"1\",    \"comment\": \"2\",    \"promote\": \"0\",    \"sticky\": \"0\",    \"type\": \"check_in\",    \"language\": \"und\",    \"created\": \"1376957648\",    \"changed\": \"1376957648\",    \"tnid\": \"0\",    \"translate\": \"0\",    \"revision_timestamp\": \"1376957648\",    \"revision_uid\": \"44\",    \"body\": {        \"und\": [{            \"value\": \"\",            \"summary\": \"\",            \"format\": \"filtered_html\",            \"safe_value\": \"\",            \"safe_summary\": \"\"        }]    },    \"field_checkin_time\": {\"und\": [ { \"value\": { \"date\": \"2013-08-20 13:31\"} } ] }}";

    private static final int INDEX_DRUPAL_SERVICE = 1;
    private static final int INDEX_DRUPAL_REST_ENDPOINT = 2;
    private static final int MIN_PARAMETERS = 3;
    
    
    
	// Database types. 
	//		Note that different database types
	// 		may need different processing and even 
	//		different structures, thus is it important to
	//		use DataOutPacket structure to add data
	public final static int DATABASE_TYPE_AWS = 0;			//	AWS (Goes to AWS DynamoDB)
	public final static int DATABASE_TYPE_T2_REST = 1; 		// T2 Rest server (goes to Mongo DB)
	public final static int DATABASE_TYPE_T2_DRUPAL = 2; 	//	T2 Drupal - goes to a Drupal database
	public final static int DATABASE_TYPE_NONE = -1;
    
	private static final boolean USE_SSL = false;

	private static final boolean VERBOSE_LOGGING = true;
	
    private static String ENGAGE_APP_ID = "khekfggiembncbadmddh";

	private static final int LOG_FORMAT_JSON = 1;	
	private static final int LOG_FORMAT_FLAT = 2;	

	public static final String SHORT_TIME_STAMP = "\"TS\"";

	public static final String DATA_TYPE_RATING = "RatingData";
	public static final String DATA_TYPE_INTERNAL_SENSOR = "InternalSensor";
	public static final String DATA_TYPE_EXTERNAL_SENSOR = "ExternalSensor";
	public static final String DATA_TYPE_USER_ENTERED_DATA = "UserEnteredData";
	
	public static final int SYNC_TIMEOUT = 200000;

	public boolean mLogCatEnabled = false;	
	public boolean mLoggingEnabled = false;	
	private boolean mDatabaseEnabled = false;
	private boolean mSessionIdEnabled = false;

	private boolean mRequiresCSRF = false;
	private String mCSRFToken = "";
	
	public void setRequiresCSRF(boolean requiresCSRF) {
		mRequiresCSRF = requiresCSRF;
	}
	
	/**
	 * Progress dialog used for traditional authentication
	 */
	private ProgressDialog mProgressDialog;

	/// Object tokens for Synchronizing calls to certain routines
	private Object addPacketToCacheSyncToken = new Object();	
	private Object updateCacheSyncToken = new Object();	
	private Object sendPacketToRemoteDbToken = new Object();	
	
    
    
	/**
	 * Selects whether or not to show a traditional login alongside Janrain Social logins.
	 * 
	 * Traditional login speaks directly to the Drupal server.
	 */
	private boolean mAllowTraditionalLogin = true;
	
	/**
	 * Signifies that user was logged in using traditional login (not janrain)
	 */
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
	 * Thread used to communicate messages in background to server
	 */
	private DispatchThread mDispatchThread = null;	
	
	/**
	 * Application version info determined by the package manager
	 */
	private String mApplicationVersion = "";

	/**
	 * Engage App ID - Supplied by JanRain
	 */
	String mEngageAppId = ENGAGE_APP_ID;

	/**
	 * Token URL used for Janrain/Drupal integration
	 */
	String mEngageTokenUrl = "";
	
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
	 * True if this module has successfully authenticated a user. 
	 */
	private boolean mAuthenticated = false;
	
	/**
	 * Set this to true to require authtication for all database puts. 
	 */
	private boolean mRequiresAuthentication = true;	
	
	/**
	 * Database manager when sending data to external Amazon database
	 */
	public static AmazonClientManager sClientManager = null;		

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
	 *   This is used to communicate the login info to Drupal
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
	
	/**
	 * Saved instance for this module
	 */
	private DataOutHandler mInstance;
	
	/**
	 * Listener for database cache events (Updates, etc)
	 */
	private DatabaseCacheUpdateListener mDatabaseUpdateListener;
	
	/**
	 * Sets the database listener
	 * @param mDatabaseUpdateListener
	 */
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
	
	/**
	 * Returns version of this package
	 * @return
	 */
	public static String getVersion() {
		return VERSION_STRING;
	}
	
	/**
	 * Retrieves a static instance of DataOutHandler
	 * 
	 * @param context - Android context of calling party
	 * @param userId - Used id
	 * @param sessionDate	- Date of the session
	 * @param appName - Application name
	 * @param dataType - data type to store
	 * @param sessionId - Session Id
	 * @return Static instance of dataOutHandler
	 */
	public synchronized static DataOutHandler getInstance(Context context, String userId, String sessionDate, String appName, String dataType, long sessionId) {
		if (sDataOutHandler == null) {
			sDataOutHandler = new DataOutHandler(context, userId, sessionDate, appName, dataType, sessionId);
		}

		return sDataOutHandler;
	}
	
	/**
	 * Retrieves a static instance of DataOutHandler
	 * 
	 * @return Static instance of dataOutHandler
	 * @throws DataOutHandlerException
	 */
	public static DataOutHandler getInstance() throws DataOutHandlerException {
		if (sDataOutHandler == null) {
			throw new DataOutHandlerException("DataOutHandler has not been initialized");
		}
		
		return sDataOutHandler;
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
	 * @throws MalformedURLException 
	 */
	public void initializeDatabase(String remoteDatabase, String databaseType) throws DataOutHandlerException, MalformedURLException {
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
	 * @throws MalformedURLException 
	 */
	public void initializeDatabase(String remoteDatabase, String databaseType, T2AuthDelegate t2AuthDelegate) throws DataOutHandlerException, MalformedURLException {
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
	 * @throws MalformedURLException 
	 */
	public void initializeDatabase(String remoteDatabase, String databaseType, 
			T2AuthDelegate t2AuthDelegate, String awsTableName) throws DataOutHandlerException, MalformedURLException {
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
	 * @throws MalformedURLException 
	 */
	public void initializeDatabase(String databaseName, String designDocName, String designDocId, String viewName, String remoteDatabase) throws DataOutHandlerException, MalformedURLException {

		mDatabaseEnabled = true;

		// Set database type
		mDatabaseType = DATABASE_TYPE_NONE; 

		// Get chosen database from preferences
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		String databaseTypeString = mSharedPreferences.getString("external_database_type", "AWS");
		
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
			
			
			initializeDrupalDatabaseNames(remoteDatabase);				

			
	        mEngage = JREngage.initInstance(mContext, mEngageAppId, mEngageTokenUrl, this);
	        // This is to account for a bug in janrain where a delegate might not get added in the initinstance call
	        // As odd as it seems, this ensures that only one delegate gets added per instance.
	        mEngage.removeDelegate(this);
	        mEngage.addDelegate(this);
	        
	        JREngage.blockOnInitialization();

	        try {
				mServicesClient = new ServicesClient(mRemoteDatabase);
			} catch (MalformedURLException e1) {
				Log.e(TAG, e1.toString());
				e1.printStackTrace();
			} catch (DataOutHandlerException e1) {
				Log.e(TAG, e1.toString());
				e1.printStackTrace();
			}
	        
	        
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
		
		// Make sure a valid database was selected
		if (mDatabaseType == DATABASE_TYPE_NONE) {
			throw new DataOutHandlerException("Invalid database type");
		}

		// Now do any global database (ot other)  initialization
		Log.d(TAG, "Initializing T2 database dispatcher");
		Log.d(TAG, "Remote database name = " + mRemoteDatabase);

		mDispatchThread = new DispatchThread();

		mDispatchThread.start();		
	}	
	
	/**
	 * Formats mRemoteDatabase, and mEngageTokenUrl with proper database names (with defaults if blank)
	 * @param remoteDatabase Remote database name
	 * @throws DataOutHandlerException
	 * @throws MalformedURLException
	 */
	void initializeDrupalDatabaseNames(String remoteDatabase) throws DataOutHandlerException, MalformedURLException {
		if (remoteDatabase.equalsIgnoreCase("")) {
			remoteDatabase = DEFAULT_DRUPAL_DB_URL;			
		}

		URL url = new URL(remoteDatabase);
        
        String protocol = url.getProtocol();
        String host = url.getHost();
        String path = url.getPath();
        String[] pathTokens = path.split("/");
        String drupalRestEndpoint = "";
        String drupalService = "";
        
        if (pathTokens.length == MIN_PARAMETERS) {
            drupalService = pathTokens[INDEX_DRUPAL_SERVICE];        	
            drupalRestEndpoint = pathTokens[INDEX_DRUPAL_REST_ENDPOINT];
        }
        else {
			throw new DataOutHandlerException("Remote database URL incorrectly formatted - "
					+ "must include Drupal service and Drupal Rest Endpoint");        
		}

		if (USE_SSL) {
			mRemoteDatabase = "https://" + host + "/" + drupalService + "/" + drupalRestEndpoint;
			mEngageTokenUrl = "https://" + host + "/" + drupalService + "/rpx/token_handler?destination=node";
		}
		else {
			mRemoteDatabase = "http://" + host + "/" + drupalService + "/" + drupalRestEndpoint;
			mEngageTokenUrl = "http://" + host + "/" + drupalService + "/rpx/token_handler?destination=node";
		}
			
	}


	/**
	 * Initializes the current database
	 * 
	 *   All new users should use this entry point for initializing the database
	 * 
	 * @param remoteDatabase 	Name of remote database (URI) to use
	 * @param mDatabaseType		Database type (integer) (See DATABASE_TYPE_xxx)
	 * @param t2AuthDelegate	t2AuthDelegate Callbacks to send status to.	
	 * @throws DataOutHandlerException
	 */
	public void initializeDatabase(String remoteDatabase, int databaseType, 
			T2AuthDelegate t2AuthDelegate) throws DataOutHandlerException, MalformedURLException {
		
		mDatabaseType = databaseType;
		mT2AuthDelegate = t2AuthDelegate;		

		if (remoteDatabase == null) {
			throw new DataOutHandlerException("remoteDatabase must not be null");
		}
		
		// Make sure a valid database was selected
		if (mDatabaseType != this.DATABASE_TYPE_T2_DRUPAL) {
			throw new DataOutHandlerException("Database type invalid or not supported at this time");
		}
		// TODO: re-add support for T2Rest and AWS
		
		
		initializeDrupalDatabaseNames(remoteDatabase);		
		
				
        mEngage = JREngage.initInstance(mContext, mEngageAppId, mEngageTokenUrl, this);
        // This is to account for a bug in janrain where a delegate might not get added in the initinstance call
        // As odd as it seems, this ensures that only one delegate gets added per instance.
        mEngage.removeDelegate(this);
        mEngage.addDelegate(this);
        
        JREngage.blockOnInitialization();

        try {
			mServicesClient = new ServicesClient(mRemoteDatabase);
		} catch (MalformedURLException e1) {
			Log.e(TAG, e1.toString());
			e1.printStackTrace();
		} catch (DataOutHandlerException e1) {
			Log.e(TAG, e1.toString());
			e1.printStackTrace();
		}
        
        mCookieStore = new PersistentCookieStore(mContext);
        mCookieStore.clear(); // Make sure to start fresh
        mServicesClient.setCookieStore(mCookieStore);
        
		try {
			mDbCache = new DbCache(mRemoteDatabase, mContext, mDatabaseUpdateListener);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
			throw new DataOutHandlerException("Can't instantiate Cache");
		}	

		mDatabaseEnabled = true;
		
		
		// Now do any global database (ot other)  initialization
		Log.d(TAG, "Initializing T2 database dispatcher");
		Log.d(TAG, "Remote database name = " + mRemoteDatabase);

		mDispatchThread = new DispatchThread();

		mDispatchThread.start();				
		
		
		
	}	
	
	
	
	/**
	 * Displays authentication dialog and takes the user through
	 * the entire authentication process.
	 * 
	 * @param thisActivity Calling party activity
	 * 
	 */public void logIn(final Activity thisActivity) {
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
	
	
	/**
	 * Performs traditional login via Drupal services.
	 * 
	 * @param username - username to use in Drupal login
	 * @param password - password to use in Drupal login
	 */
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

                mProgressDialog.hide();
                mProgressDialog.dismiss();
            	
            	
            	List<Cookie> list = new ArrayList<Cookie>();
            	list = mInstance.mCookieStore.getCookies();
            	Log.e(TAG, "CSRFSessionLookie = " + list.get(0).toString());

            	
            	// TODO: move this to accomodate Janrain
            	if (mRequiresCSRF) {
            		if (VERBOSE_LOGGING) {
            			Log.e(TAG, "Requesting CSRF Token");
            		}
            		getCSRFToken();            		
            	}
            	
            	mAuthProvider = "Traditional";
            	mAuth_info = new JRDictionary();
            	
    			if (mT2AuthDelegate != null) {
    				mT2AuthDelegate.T2AuthSuccess(mAuth_info, mAuthProvider, null, null);
    			}
            	
            	
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Log.e(TAG, e.toString());
                mProgressDialog.hide();
                mProgressDialog.dismiss();
                
            	mAuthProvider = "Traditional";
            	JREngageError error = new JREngageError(response, JREngageError.AuthenticationError.AUTHENTICATION_FAILED, 
            			JREngageError.ErrorType.AUTHENTICATION_FAILED);
            	
    			if (mT2AuthDelegate != null) {
    				mT2AuthDelegate.T2AuthFail(error, mAuthProvider);
    			}                
            }

            @Override
            public void onFinish() {
                Log.e(TAG, "traditionalLogin onFinish()");
                mProgressDialog.hide();
                mProgressDialog.dismiss();
            }
        });        
	}
	
	/**
	 * Performs traditional logout via Drupal Services
	 */
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
	                mProgressDialog.hide();
	                mProgressDialog.dismiss();
	            	
	                new AlertDialog.Builder(mContext).setMessage("Logout was successful.").setPositiveButton("OK", null).setCancelable(true).create().show();
	            }

	            @Override
	            public void onFailure(Throwable e, String response) {
	                Log.e(TAG, e.toString());
	                mProgressDialog.hide();
	                mProgressDialog.dismiss();
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
	 * Cancels authentication
	 */
	public void logOut() {
		Log.d(TAG, "DataOuthandler Logging out");		
		if (mLoggedInAsTraditional) {
			traditionalLogout();
		}
		
		mAuthenticated = false;
		drupalSessionCookie = null;
		
		// If we delete the cookie here, traditional logout will fail - so don't delete it!	
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
        	mEngage.removeDelegate(this);
        }	mT2AuthDelegate = null;
		mAuthenticated = false;
	}
	
	/**
	 * Handles creation of a new database entry
	 * 
	 * @param dataOutPacket - packet to output to database
	 * @throws DataOutHandlerException
	 */
	public void handleDataOut(final DataOutPacket dataOutPacket) throws DataOutHandlerException {

		if (mRequiresAuthentication == true && mAuthenticated == false) {
			throw new DataOutHandlerException("User is not authenticated");
		}

		if (dataOutPacket == null) {
			throw new DataOutHandlerException("Data packet is null");
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
			
		SqlPacket sqlPacket = mDbCache.getPacketByRecordId(dataOutPacket.mRecordId);
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
			
	    	// Update the changed date
			Calendar calendar = GregorianCalendar.getInstance();
		    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	    	dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	        String currentTimeString = dateFormatter.format(calendar.getTime());
	        sqlPacketNew.setChangedDate(currentTimeString);
			
			int retVal = mDbCache.updateSqlPacket(sqlPacketNew);	
			Log.d(TAG, "Updated: retVal = " + retVal + " record id = " + dataOutPacket.mRecordId);
			SqlPacket updatedSqlPacketFromSql = mDbCache.getPacketByRecordId(dataOutPacket.mRecordId);
			
			// The timed task will take care of updating it in Drupal 
            if (mInstance.mDatabaseUpdateListener != null) {
            	mInstance.mDatabaseUpdateListener.remoteDatabaseCreateUpdateComplete(dataOutPacket);
            } 			
		}
	}
	
	/**
	 * Deletes data packet from the database
	 * 
	 * @param dataOutPacket - packet to delete
	 * @throws DataOutHandlerException
	 */
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
	 * This thread handles maintenance of the cache
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

				//Log.d(TAG, "Http dispatch thread tick");

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
    
	/* (non-Javadoc)
	 * Callback that indicates a successful JanRain authentication.
	 * 
	 * @see com.janrain.android.engage.JREngageDelegate#jrAuthenticationDidSucceedForUser(com.janrain.android.engage.types.JRDictionary, java.lang.String)
	 */
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

	/* (non-Javadoc)
	 * Callback that indicates Janrain successfully contacted the TokenURL server (after authentication)
	 * 
	 * @see com.janrain.android.engage.JREngageDelegate#jrAuthenticationDidReachTokenUrl(java.lang.String, com.janrain.android.engage.net.async.HttpResponseHeaders, java.lang.String, java.lang.String)
	 */
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
	}

	@Override
	public void jrSocialDidCompletePublishing() {
	}

	@Override
	public void jrEngageDialogDidFailToShowWithError(JREngageError error) {
		Log.d(TAG, "jrEngageDialogDidFailToShowWithError");		
	}

	@Override
	public void jrAuthenticationDidNotComplete() {
		Log.d(TAG, "jrAuthenticationDidNotComplete");		
	}

	/* (non-Javadoc)
	 * Callback that indicates JanRain encountered a failure authenticating.
	 * 
	 * @see com.janrain.android.engage.JREngageDelegate#jrAuthenticationDidFailWithError(com.janrain.android.engage.JREngageError, java.lang.String)
	 */
	@Override
	public void jrAuthenticationDidFailWithError(JREngageError error,
			String provider) {
		Log.d(TAG, "jrAuthenticationDidFailWithError");		
		mAuthenticated = false;
		
		if (mT2AuthDelegate != null) {
			mT2AuthDelegate.T2AuthFail(error, provider);
		}		
	}

	/* (non-Javadoc)
	 * Callback that indicates JanRain encountered a failure contacting the token URL	 
	 * 
	 * @see com.janrain.android.engage.JREngageDelegate#jrAuthenticationCallToTokenUrlDidFail(java.lang.String, com.janrain.android.engage.JREngageError, java.lang.String)
	 */
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
	} 	

	/**
	 * Creates a formatted JSON string in the format that Drupal understands for creating a new
	 * database entry in Drupal
	 * 
	 * @param dataOutPacket - packet to create Drupal string for
	 * @return JSON formattted data string for Drupa consumption
	 */
	private String createDrupalPacketString(DataOutPacket dataOutPacket) {
		ObjectNode item = JsonNodeFactory.instance.objectNode();
		item.put("title", dataOutPacket.mRecordId);
		item.put("type", dataOutPacket.mStructureType);
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
	
	private String createDrupalPacketStringVh4h(DataOutPacket dataOutPacket) {
		ObjectNode item = JsonNodeFactory.instance.objectNode();
		item.put("title", dataOutPacket.mRecordId);
		item.put("type", dataOutPacket.mStructureType);
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
	 * Writes Drupal formatted node to specified node (String)
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
	 * Writes Drupal formatted node to specified node (Long)
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
	 * Writes Drupal formatted node to specified node (Int)
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
	 * Writes Drupal formatted node to specified node (Double)
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
	
	/**
	 * Processes the cache (updates the local cache and remote database to they are in sync)
	 *  To be called from DispatchThread
	 * 
	 *  Here is the algorithm:
	 *  1. Request a packet summary from Drupal
	 *  	Update drupal ID's of all records in cache
	 *  2. Check for records in local cache but not in remote DB
	 *  	if Packet exists in Cache but not in remote DB
	 *  		if Packet newly inserted by self 
	 *  			Add (send) the packet to the remote DB
	 *  		else (Packet deleted by other from remote DB) 
	 *  			Remove the packet from the cache
	 *  2. Check for records in remote DB but not in local cache
	 *  	if Packet exists in remote DB but not in local Cache
	 *  		if Packeted deleted by self
	 *  			Delete packet from remote DB
	 * 		else (Packet newly inserted by other into DB)
	 * 				Add packet to local Cache	
	 * 3. If records exist both in local cache and remote DB
	 * 		if the remote DB record changed data is more recent
	 * 			Update the local cache from the remote DB
	 * 		else
	 * 			Update the remote DB from the local cache
	 * 
	 * 
	 * Rules for presence of drupal node id in the cache
	 * If synched from DB (added by another client):
	 *		drupal ID is present immediately
	 * If added from the UI
	 *		drupal id is blank until the first timed update (ProcessCacheThread()).
	 *		at which time the local DB entries are updated with drupal node id	 * 
	 */
	private void ProcessCacheThread() {
		
		final ArrayList<String> mDrupalRecordIdList = new ArrayList<String>();			
		final ArrayList<String> mSqlRecordIdList;		
		final HashMap<String, String> mRecordIdToDrupalIdMap = new HashMap<String, String>();	
		final HashMap<String, String> mDrupalIdToRecordIdMap = new HashMap<String, String>();	
		final HashMap<String, String> mRecordIdToDrupalChangedTime = new HashMap<String, String>();	

		Log.d(TAG, "ProcessCacheThread()");
		
		// Get list of all records in the local database
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
	            		String changedTime = (String) jObject.get("changed");

		            	// Check to see if this is a valid record
	            		// If so then add the record to the summary arrays
		            	if (GlobalH2.isValidRecordId(recordId)) {
		            		mDrupalRecordIdList.add(recordId);
			            	mRecordIdToDrupalIdMap.put(recordId, nodeId);
			            	mDrupalIdToRecordIdMap.put(nodeId, recordId);	
			            	mRecordIdToDrupalChangedTime.put(recordId, changedTime);
//           				 if (VERBOSE_LOGGING) {
//            				 Log.e(TAG, "setting Array RecordId/changed " + recordId + " =  " + changedTime);
//        				 }			            	
			            	
	            		 }
	            		 
	            	} catch (JSONException e) {
	            		 e.printStackTrace();
	            	}	
	             }

        		if (VERBOSE_LOGGING) {
   	             	Log.e(TAG, "Updated mDrupalRecordIdList, mRecordIdToDrupalIdMap, and mDrupalIdToRecordIdMap");
   	             	Log.e(TAG, "mDrupalRecordIdList = " + mDrupalRecordIdList.toString());
   	             	Log.e(TAG, "mSqlRecordIdList = " + mSqlRecordIdList.toString());
//   	            Log.e(TAG, "mNodeDeleteQueue = " + mNodeDeleteQueue.toString());
        		}

	            // At this point 
	            //   mDrupalIdList contains a list of all record id's in Drupal
	            //   mSqlIdList contains a list of all record id's in the SQL cache
	            //	 mDrupalIdMap contains a map of record id's to node id's in Drupal
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
	
			/* (non-Javadoc)
			 * The HTTP call to retrieve the summary contents from Drupal has succeeded.
			 * Now process the results.
			 * 
			 * @see com.loopj.android.http.AsyncHttpResponseHandler#onFinish()
			 */
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
		            			 SqlPacket sqlPacket = mDbCache.getPacketByRecordId(recordId);
		            			 if (sqlPacket != null) {
		            				 if (VERBOSE_LOGGING) {
			            				 Log.e(TAG, "setting RecordId/DrupalId " + recordId + ", " + drupalId + " to idle");
		            				 }

		            				 // Now set the status of the cache packet to idle
		     		 				 sqlPacket.setCacheStatus(GlobalH2.CACHE_IDLE);
		     		 				 
		     		 				 // Now set the changed date to the changed date in from drupal
		     		 				 // we need to do this because the changed date is later than the one
		     		 				 // we set (it's set to the time drupal receives the new packet).
		     		 				 String drupalTime = mRecordIdToDrupalChangedTime.get(recordId);
		     		 				 Long lChangedAt = Long.parseLong((drupalTime)) * 1000;
		     		 				 Time changedAtTime = new Time();
		     		 				 
		     		 				 Date changedAtDate = new Date(lChangedAt);
		     		 			     SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		     		 				 dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));		     		 				 
			     		 			 String changedAtTimeString = dateFormatter.format(changedAtDate);		
//		            				 if (VERBOSE_LOGGING) {
//			            				 Log.e(TAG, "setting RecordId/changed " + recordId + " =  " + drupalTime);
//			            				 Log.e(TAG, "setting RecordId/changed " + recordId + " =  " + changedAtTimeString);
//		            				 }		     
			     		 			 
		     		 				 sqlPacket.setChangedDate(changedAtTimeString);
									 mDbCache.updateSqlPacket(sqlPacket);
				                	 if (mDatabaseUpdateListener != null) {
				                		 
				                		 try {
											mDatabaseUpdateListener.remoteDatabaseCreateUpdateComplete(new DataOutPacket(sqlPacket));
										} catch (DataOutHandlerException e) {
											Log.e(TAG, e.toString());
											e.printStackTrace();
										}
				                	 }						 
									 
		     		 				 i.remove();
								}										
		            		}
		            	 }
			 		}
		            	 
			 		// Now do record by record comparison between the records of the local database (mSqlRecordIdList)
			 		// to the records of the remote database (mDrupalRecordIdList).
			 		 
			 		// Check for records in local cache but not in remote DB
		            for (String recordId : mSqlRecordIdList) {
		            	if (!mDrupalRecordIdList.contains(recordId)) {
		            		
		            		if (VERBOSE_LOGGING) {
			        	        Log.e(TAG, "recordId: " + recordId + " - Packet exists in local Cache but not in remote DB");
		            		}
	
		            		if (recordId == null) {
		            			break;
		            		}
		            		SqlPacket sqlPacket = mDbCache.getPacketByRecordId(recordId); // Since recordId is in mSqlRecordIdList we know this will not return null
			            	// Exists in cache but not on on Drupal
		        	        // Two possible cases here
		        	        // 1 Packet newly inserted by self        			-> Add (send) the packet to the remote DB
		        	        // 2 Packet deleted by other from remote DB      	-> Remove the packet from the local cache
	        	        	Boolean isSendingOrSend = (sqlPacket.getCacheStatus() == GlobalH2.CACHE_SENDING || 
	        	        			sqlPacket.getCacheStatus() == GlobalH2.CACHE_SENT);
			            	if (isSendingOrSend) {
			            		
				            	// Packet exists in cache but not on on Drupal
			            		// Case 1 - Packet newly inserted by self        -> Add (send) the packet to the DB
			            		if (VERBOSE_LOGGING) {
			            			Log.e(TAG, "Case 1 - Send the packet to remoteDB");
			            		}
	
		        	        	// Don't send the packet if already sending!
		        	        	if (isSendingOrSend) {
			        	        	DataOutPacket dataOutPacket;
				            		if (VERBOSE_LOGGING) {
				            			Log.e(TAG, "Status Sending/Sent - Sending packet to remote database ");
				            		}
									try {
										
							 			synchronized(mDbCache) {
							 				sqlPacket.setCacheStatus(GlobalH2.CACHE_SENT);
											mDbCache.updateSqlPacket(sqlPacket);
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
			            		// Case 2 - Packet deleted by other from remote DB      -> Remove the packet from the local cache
		            			if (VERBOSE_LOGGING) {
		            				Log.e(TAG, "Case 2 - Remove the packet from the loal cache");
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
									Log.e(TAG, e.toString());
									e.printStackTrace();
								}
			            	}
		            	 } // if (!mDrupalIdList.contains(id))
		            	else {
		            		// Record exists in both local cache and remote db
		            		// Need to merge the records
//							if (VERBOSE_LOGGING) {
//								Log.e(TAG, "Record exists in both local cache and remote db: " + recordId);
//							}
//		            		
		            		// First see which record is most recent
		            		try {
								SqlPacket sqlPacket = mDbCache.getPacketByRecordId(recordId);
								if (sqlPacket != null) {
									String localChangedAt = sqlPacket.getChangedDate();
									DateTime dtLocalChangedAt = new DateTime(localChangedAt);
	
									long lLocalChangedAt = dtLocalChangedAt.getMillis();
									
									String drupalChangedAt = mRecordIdToDrupalChangedTime.get(recordId);
									long lDrupalChangedAt = Long.parseLong(drupalChangedAt) * 1000; 
	
									if (VERBOSE_LOGGING) {
										//Log.e(TAG, "Record exists in both local and remote, times: " + lLocalChangedAt + ", " + lDrupalChangedAt + ", " + recordId);
	
										if (lDrupalChangedAt == lLocalChangedAt) {
											//Log.e(TAG, "RemoteTime and local time are exactly equal");
										}
										else {
											if (lDrupalChangedAt > lLocalChangedAt) {
												Log.e(TAG, "RemoteTime is most recent - updating cache from remote");
							        	        addPacketToCacheSync(mRecordIdToDrupalIdMap.get(recordId), "U");	// Grabs the packet from Drupal and updates it in the Cache        	          
												
												
											}
											else { 
												Log.e(TAG, "localTime is most recent");
						            			if (VERBOSE_LOGGING) {
						            				Log.e(TAG, "Updatating record to remote database");
						            			}	
						            			
						            			// Should we set status to sending/sent here?
						            			DataOutPacket dataOutPacket = new DataOutPacket(sqlPacket);
						            			sendPacketToRemoteDbSync(dataOutPacket, "U", mRecordIdToDrupalIdMap.get(recordId));
												
											}
										}
									}
	
									}
							} catch (Exception e) {
								Log.e(TAG, e.toString());
								e.printStackTrace();
							}		            		
		            	}
		        	 }	  // end for (String id : mSqlIdList) 	
	 
		             // Check for records in remote DB but not in local cache
		             for (String drupalRecordId : mDrupalRecordIdList) {
		            	 
		            	 if (!mSqlRecordIdList.contains(drupalRecordId)) {
		            		 //SqlPacket sqlPacket = mDbCache.getPacketByRecordId(drupalRecordId); // Can't do this if not in CACHE!!!!!!
		            		 
			            		if (VERBOSE_LOGGING) {
			            			Log.e(TAG, "recordId: " + drupalRecordId + " - Packet exists in remote DB but not in local Cache");
			            		}
	
		        	          // Packet exists in remote DB but not in local Cache
		        	          // Two possible cases here:
			        	      // 3 Packeted deleted by self                			-> Delete packet from  remoteDB
		        	          // 4 Packet newly inserted by other into remote DB  	-> Add packet to local Cache
	
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

		        	        	  // Packet exists in remote DB but not in local Cache
		        	        	  // Case 3 - Packeted deleted by self                -> Delete packet from remote DB
		        	        	  if (VERBOSE_LOGGING) {
		        	        		  Log.e(TAG, "Case 3 - Delete packet from remote DB");
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
		        	        		  Log.e(TAG, "Case 4 - Add packet to local cache");
		        	        	  }
			        	          String drupalId = mRecordIdToDrupalIdMap.get(drupalRecordId);
			        	          addPacketToCacheSync(drupalId, "C");	// Grabs the packet from Drupal and adds it to the Cache        	          
		        	          }
		            	 }
		             } // End  for (String drupalRecordId : mDrupalRecordIdList)
	             }
                try {
            		if (VERBOSE_LOGGING) {
      	          		Log.e(TAG, "Done processing all Drupal check entries");
            		}

            		// Notify self that all entries have been processed
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

		// Wait until all entries have been processed
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
		
		// Give notice that the internal cache should now be in sync with the remote database
		if (mDatabaseUpdateListener != null) {
    		mDatabaseUpdateListener.remoteDatabaseSyncComplete(mRecordIdToDrupalIdMap);
    	}	        		
	}

	
	/**
	 * Requests a CSRF token from the Drupal server
	 *  This token is then send to the Services client to include as a header
	 *  Note that this is only required in newer Drupal Services installations
	 */
	void getCSRFToken() {
        UserServices us;
        int nodeNum = 0;
        us = new UserServices(mServicesClient);    	

        AsyncHttpResponseHandler responseHandler = new AsyncHttpResponseHandler() {


			@Override
			public void onSuccess(String arg0) {
				super.onSuccess(arg0);
                Log.e(TAG, "getCSRFToken() SUCCESS: " + arg0);
                mCSRFToken = arg0;                
                mServicesClient.setCSRFToken(arg0);
				
			}
            
            @Override
            public void onFailure(Throwable e, String response) {
                Log.e(TAG, "getCSRFToken() FAILED: " + response);
                Log.e(TAG, e.toString());
            }
            
			@Override
            public void onFinish() {
                Log.e(TAG, "getCSRFToken() OnFinish()");
            }
        };     
        

        us.RequestCSRFToken(responseHandler);
		
		
	}
	
	public class MyAsyncHttpResponseHandler extends JsonHttpResponseHandler {

	    private DataOutPacket mDataoutPacket;

	    public MyAsyncHttpResponseHandler(DataOutPacket dataoutPacket) {
	    	mDataoutPacket = dataoutPacket;
	    }

	    @Override
	    public void onSuccess(String arg0)
	    {
	        super.onSuccess(arg0);
	        // Use requestId here
	    }

		@Override
		public void onFailure(Throwable arg0, JSONArray arg1) {
            Log.e(TAG, arg0.toString());
        	if (mDatabaseUpdateListener != null) {
        		mDatabaseUpdateListener.remoteDatabaseFailure(arg0.toString());
        	}
			super.onFailure(arg0, arg1);
		}

		@Override
		public void onFailure(Throwable e, JSONObject response) {
 //           Log.e(TAG, e.toString() + recordId);
            
            //need to set flag so this gets set to idle                
        	if (mDatabaseUpdateListener != null) {
        		mDatabaseUpdateListener.remoteDatabaseFailure(e.toString());
        	}		

			super.onFailure(e, response);
		}

		@Override
		public void onSuccess(JSONArray arg0) {
	        Log.d(TAG, "Successfully submitted ARRAY (Deleted record from drupal)" + arg0.toString());
	        updatemNodeDeleteQueue();	
			super.onSuccess(arg0);
		}

		@Override
		public void onSuccess(JSONObject arg0) {
            String nid;
			try {
				nid = arg0.getString("nid");
	            Log.d(TAG, "Myroutine - Successfully submitted article # " + nid);		
	            
	            // We can't set the cache to sent yet since it's not been associated with
                // a drupal id yet. That comes with the first Drupal node summary list 
                // returned from Drupal. We do this processing in ProcessCache()
	            mDrupalIdsSuccessfullyAdded.add(nid);     	            
	            
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// TODO Auto-generated method stub
			super.onSuccess(arg0);
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
        final String recordId;
        String jsonString = "";   
        
        if (dataOutPacket != null) {

            Log.d(TAG, "sendPacketToRemoteDb(" + dataOutPacket.mRecordId + ")");
            jsonString = createDrupalPacketString(dataOutPacket);
            
//            jsonString = fred;
            recordId = dataOutPacket.mRecordId;
        }
        else {
            Log.d(TAG, "sendPacketToRemoteDb - deleting drupal node id  + " + drupalNodeId + ")");
            recordId = "";
        }
        
		// Check to see if we've stored a Drupal session cookie. If so then attach then to 
        // the http client.
        // Note: if logged in as traditional there will be no explicit store of the session cookie
        // (It's done automagically) so skip this
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

        MyAsyncHttpResponseHandler responseHandler = new MyAsyncHttpResponseHandler(dataOutPacket) {
            
        	// We get here for Create or Update operations
//        	@Override
//            public void onSuccess(JSONObject response) {
//                try {
//                    String nid = response.getString("nid");
//                    Log.d(TAG, "Successfully submitted article # " + nid);
//                    
//                    // We can't set the cache to sent yet since it's not been associated with
//                    // a drupal id yet. That comes with the first Drupal node summary list 
//                    // returned from Drupal. We do this processing in ProcessCache()
//                    mDrupalIdsSuccessfullyAdded.add(nid);                    
//	 				
//                } catch (JSONException e) {
//                    Log.e(TAG, e.toString());
//                }
//            }

//        	// We get here for Delete operations
//			@Override
//			public void onSuccess(JSONArray arg0) {
//                Log.d(TAG, "Successfully submitted ARRAY (Deleted record from drupal)" + arg0.toString());
//                updatemNodeDeleteQueue();
//			}
            
//            @Override
//            public void onFailure(Throwable e, JSONObject response) {
//                Log.e(TAG, e.toString() + recordId);
//                
//                //need to set flag so this gets set to idle                
//            	if (mDatabaseUpdateListener != null) {
//            		mDatabaseUpdateListener.remoteDatabaseFailure(e.toString());
//            	}	                 
//            }
            
//            @Override
//			public void onFailure(Throwable arg0, JSONArray arg1) {
//                Log.e(TAG, arg0.toString());
//            	if (mDatabaseUpdateListener != null) {
//            		mDatabaseUpdateListener.remoteDatabaseFailure(arg0.toString());
//            	}	                 
//			}

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
    void addPacketToCacheSync(final String drupalNodeId, String action) {
		if (VERBOSE_LOGGING) {
			Log.e(TAG, "Waiting for addPacketToCacheSyncToken");
		}
        addPacketToCache(drupalNodeId, action);        
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
    void addPacketToCache(final String drupalNodeId, final String action) {
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
					Log.e(TAG, e1.toString());
					e1.printStackTrace();
				}

            	String drupalNodeContents = response.toString();
            	// Now convert the drupal node to a dataOutPacket.                    	
            	DataOutPacket dataOutPacket;
				try {
					dataOutPacket = new DataOutPacket(response);
					
					if (action.equalsIgnoreCase("C")) {
						// Make sure to set the drupal id while adding it to the cache
	    				synchronized(mDbCache) {
	    					mDbCache.addPacketToCache(dataOutPacket, drupalNodeId);
	    				}						
					}
					else {
						updateRecord(dataOutPacket);
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
	}
	
	/**
	 * Requests a list of DataOutPackets from the local cache
	 * 	This version returns all data types
	 * @param mDrupalIdMap
	 */
	public ArrayList<DataOutPacket> getPacketList() {
		return mDbCache.getPacketList();
	}
	
	/**
	 * Requests a list of DataOutPackets from the local cache
	 *  The list returned is filtered by the data types contained in 
	 *  the list structureTypes. Note that you must use only 
	 *  pre-configured data types. ie sensor_data, habit, checkin, etc.
	 * @param structureTypes - List of Structure types to filter on
	 * @return - List of DataOutPackets in the local cache
	 */
	public ArrayList<DataOutPacket> getPacketList(List<String> structureTypes) {
		return mDbCache.getPacketList(structureTypes);
	}	
	
	/**
	 * Gets specific packet based on record id
	 * @param recordId - record id to use in search
	 * @return - Packet corresponding to record id
	 */
	public DataOutPacket getPacketByRecordId(String recordId) {
		SqlPacket sqlPacket = mDbCache.getPacketByRecordId(recordId); 
		DataOutPacket dataOutpacket;
		try {
			dataOutpacket = new DataOutPacket(sqlPacket);
			return dataOutpacket;
		} catch (DataOutHandlerException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
			return null;
		}
	}
}
