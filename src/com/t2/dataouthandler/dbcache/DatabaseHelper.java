package com.t2.dataouthandler.dbcache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutPacket;



import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Handles all database operations
 * 
 * @author Scott Coleman (scott.coleman@tee2.org)
 */

public class DatabaseHelper
{

	private final String TAG = getClass().getName();	
	private static final String DATABASE_NAME = "t2androidh2.db";
	private static final int DATABASE_VERSION = 1;

	private Context context;
	private SQLiteDatabase db;

	public DatabaseHelper(Context context) 
	{
		this.context = context;      
	}

	public static String scrubInput(String input)
	{
		//add more reserved SQL characters to prevent a sql injection attack or just a crash
		String Output = input.replace("'", "''");
		return Output;
	}

	public List<SqlPacket> getPacketList()	{
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		Cursor cursor = null;

		String query = "select PacketID, PacketSql, DrupalId, RecordId, CacheStatus from PERSISTENTCACHE";
		cursor = this.db.rawQuery(query, null);

		if (cursor.moveToFirst()) {
			List<SqlPacket> packets = new ArrayList<SqlPacket>();

			do 	{
				SqlPacket packet = new SqlPacket();
				packet.setSqlPacketId(cursor.getString(0));
				packet.setPacket(cursor.getString(1));
				packet.setDrupalId(cursor.getString(2));
				packet.setRecordId(cursor.getString(3));
				packet.setCacheStatus(cursor.getInt(4));
				packets.add(packet);
			}
			while (cursor.moveToNext());

			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}

			db.close();
			return packets;
		}
		else {
			cursor.close();
			db.close();
			return null;
		}
	}	
	
	public ArrayList<DataOutPacket> getPacketListDOP()	{
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		Cursor cursor = null;
		String query = "select PacketID, PacketSql, DrupalId, RecordId, CacheStatus from PERSISTENTCACHE";
		try {
			cursor = this.db.rawQuery(query, null);
			if (cursor.moveToFirst()) {
				ArrayList<DataOutPacket> packets = new ArrayList<DataOutPacket>();
				do 	{
					SqlPacket sqlPacket = new SqlPacket();
					sqlPacket.setSqlPacketId(cursor.getString(0));
					sqlPacket.setPacket(cursor.getString(1));
					sqlPacket.setDrupalId(cursor.getString(2));
					sqlPacket.setRecordId(cursor.getString(3));
					sqlPacket.setCacheStatus(cursor.getInt(4));

//					if (sqlPacket.getCacheStatus() != SqlPacket.CACHE_DELETING) {
						if (true) {
						try {
							DataOutPacket packetDOP = new DataOutPacket(sqlPacket);
							packets.add(packetDOP);
						} catch (DataOutHandlerException e) {
							e.printStackTrace();
						}
						
					}
				}
				while (cursor.moveToNext());

				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}

				db.close();
				return packets;
			}
			else {
				cursor.close();
				db.close();
				return null;
			}
		} catch (Exception e) {
			if (e != null) {
				Log.e("Exception", e.toString());
				e.printStackTrace();
			}
			else {
				Log.e("Unknown Exception", e.toString());
			}
			return null;
		}
	}	
	
	public void createNewSqlPacket(SqlPacket packet) {

		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();

		try	{
			ContentValues insertValues = new ContentValues();
			insertValues.put("PacketSql", packet.getPacketJson());
			insertValues.put("RecordId", packet.getRecordId());
			insertValues.put("DrupalId", packet.getDrupalId());
			insertValues.put("CacheStatus", packet.getCacheStatus());
			db.insert("PERSISTENTCACHE", null, insertValues);

			return;
		}
		catch(Exception ex)	{
			return;
		}
		finally	{
			db.close();
		}
	}	
	
	public int updateSqlPacket(SqlPacket packet) {

		int retVal = 0;
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();

		try	{
			
//			ContentValues updateValues = new ContentValues();
//			updateValues.put("photo", photo);
//			db.update("PERSISTENTCACHE", updateValues, "userid = " + userID, null);
			
			ContentValues updateValues = new ContentValues();
			updateValues.put("PacketSql", packet.getPacketJson());
			updateValues.put("RecordId", packet.getRecordId());
			updateValues.put("DrupalId", packet.getDrupalId());
			updateValues.put("CacheStatus", packet.getCacheStatus());
			retVal = db.update("PERSISTENTCACHE", updateValues, "PacketID = " + packet.getSqlPacketId(), null);

			if (retVal != 1) {
				Log.e(TAG, "ERROR updating database!");
			}
			
			return retVal;
		}
		catch(Exception ex)	{
			return retVal;
		}
		finally	{
			db.close();
		}
	}	
		
	public int deleteSqlPacketByRecordId(String recordId) {

		SqlPacket sqlPacket = getPacketByRecordId(recordId);		
		
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		int retval = 0;

		try	{
			retval = db.delete("PERSISTENTCACHE", " PacketID='"+ sqlPacket.getSqlPacketId() +"'", null);			
			return retval;
		}
		catch(Exception ex)	{
			return retval;
		}
		finally	{
			db.close();
		}		
	}
	
	public int deleteSqlPacketByDrupalId(String drupalId) {

		SqlPacket sqlPacket = getPacketByDrupalId(drupalId);		
		
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		int retval = 0;

		try	{
			retval = db.delete("PERSISTENTCACHE", " PacketID='"+ sqlPacket.getSqlPacketId() +"'", null);			
			return retval;
		}
		catch(Exception ex)	{
			return retval;
		}
		finally	{
			db.close();
		}		
	}
	
	public void createNewSqlPacket(String packet, String recordId, String drupalId) {

		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();

		try	{
			ContentValues insertValues = new ContentValues();
			insertValues.put("PacketSql", packet);
			insertValues.put("RecordId", recordId);
			insertValues.put("DrupalId", drupalId);
			insertValues.put("CacheStatus", SqlPacket.CACHE_IDLE);
			
			db.insert("PERSISTENTCACHE", null, insertValues);

			return;
		}
		catch(Exception ex)	{
			return;
		}
		finally	{
			db.close();
		}
	}	
	
	
	public SqlPacket getPacketByDrupalId(String drupalId)
	{
		SqlPacket outPacket = null;
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		Cursor cursor = null;

		String query = "select PacketID, RecordId, DrupalID, PacketSql from PERSISTENTCACHE where DrupalID = '" + drupalId + "'" ;
		cursor = this.db.rawQuery(query, null);

		if (cursor.moveToFirst()) 
		{
			do 
			{
				outPacket = new SqlPacket();
				outPacket.setSqlPacketId(cursor.getString(0));
				outPacket.setRecordId(cursor.getString(1));
				outPacket.setDrupalId(cursor.getString(2));
				outPacket.setPacket(cursor.getString(3));
				
			}
			while (cursor.moveToNext());

			if (cursor != null && !cursor.isClosed()) 
			{
				cursor.close();
			}

			db.close();
			return outPacket;
		}
		else
		{
			cursor.close();
			db.close();
			return null;
		}
	}		
	
	public SqlPacket getPacketByRecordId(String recordId)
	{
		SqlPacket outPacket = null;
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		Cursor cursor = null;

		String query = "select PacketID, RecordId, DrupalID, PacketSql, CacheStatus from PERSISTENTCACHE where RecordId = '" + recordId + "'" ;
		cursor = this.db.rawQuery(query, null);

		if (cursor.moveToFirst()) 
		{
			do 
			{
				outPacket = new SqlPacket();
				outPacket.setSqlPacketId(cursor.getString(0));
				outPacket.setRecordId(cursor.getString(1));
				outPacket.setDrupalId(cursor.getString(2));
				outPacket.setPacket(cursor.getString(3));
				outPacket.setCacheStatus(cursor.getInt(4));
				
			}
			while (cursor.moveToNext());

			if (cursor != null && !cursor.isClosed()) 
			{
				cursor.close();
			}

			db.close();
			return outPacket;
		}
		else
		{
			cursor.close();
			db.close();
			return null;
		}
	}	
	
	private static class OpenHelper extends SQLiteOpenHelper 
	{
		Context dbContext;

		OpenHelper(Context context) 
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			dbContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) 
		{	//TODO: DATABSE STRUCTURE MARKING

			//PERSISTENTCACHE
			String createPERSISTENTCACHE = "CREATE TABLE IF NOT EXISTS PERSISTENTCACHE (PacketID INTEGER PRIMARY KEY, RecordId TEXT, PacketSql TEXT, DrupalId TEXT, CacheStatus INTEGER);";
			db.execSQL(createPERSISTENTCACHE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
		{
			//needs to NOT drop users,usermeds,reminders, and pillstaken
			//else the user will lose data on upgrade.
			try
			{
				db.execSQL("drop table PERSISTENTCACHE");
			}
			catch(Exception ex)
			{}
			onCreate(db);
		}	
	
	}
}