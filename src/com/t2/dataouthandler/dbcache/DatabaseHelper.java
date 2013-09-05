/*****************************************************************
DatabaseHelper

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
package com.t2.dataouthandler.dbcache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutPacket;
import com.t2.dataouthandler.GlobalH2;



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

	/**
	 * Requests a list of DataOutPackets from the local cache
	 *  This version returns all data types
     *
	 * @return - List of DataOutPackets in the local cache
	 */
	public ArrayList<DataOutPacket> getPacketList()	{
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		Cursor cursor = null;
		String query = "select PacketID, PacketSql, RecordId, DrupalId, CacheStatus, ChangedDate, StructureType Title, from PERSISTENTCACHE";

		ArrayList<DataOutPacket> packets = new ArrayList<DataOutPacket>();
		try {
			cursor = this.db.rawQuery(query, null);
			if (cursor.moveToFirst()) {
				do 	{
					SqlPacket sqlPacket = new SqlPacket();
					sqlPacket.setSqlPacketId(cursor.getString(0));
					sqlPacket.setPacket(cursor.getString(1));
					sqlPacket.setRecordId(cursor.getString(2));
					sqlPacket.setDrupalId(cursor.getString(3));
					sqlPacket.setCacheStatus(cursor.getInt(4));
					sqlPacket.setChangedDate(cursor.getString(5));
					sqlPacket.setStructureType(cursor.getString(6));
					sqlPacket.setTitle(cursor.getString(7));

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
				return packets;
			}
		} catch (Exception e) {
			Log.e("Exception", e.toString());
			e.printStackTrace();
			return null;
		}
	}	

	private String addQuotes(String string) {
		return "\"" + string + "\"";
	}	
	
	private String addSingleQuotes(String string) {
		return "'" + string + "'";
	}	
	
	/**
	 * Requests a list of DataOutPackets from the local cache
	 * @param whereClause - where clause to use in SQL statement
	 * @return - List of DataOutPackets in the local cache
	 */
	public ArrayList<DataOutPacket> getPacketList(String whereString)	{
		
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		Cursor cursor = null;
		
		String query = "select PacketID, PacketSql, RecordId, DrupalId, CacheStatus, ChangedDate, StructureType, Title from PERSISTENTCACHE " +
				"			where " + whereString;

		ArrayList<DataOutPacket> packets = new ArrayList<DataOutPacket>();
		try {
			cursor = this.db.rawQuery(query, null);
			if (cursor.moveToFirst()) {
				do 	{
					SqlPacket sqlPacket = new SqlPacket();
					sqlPacket.setSqlPacketId(cursor.getString(0));
					sqlPacket.setPacket(cursor.getString(1));
					sqlPacket.setRecordId(cursor.getString(2));
					sqlPacket.setDrupalId(cursor.getString(3));
					sqlPacket.setCacheStatus(cursor.getInt(4));
					sqlPacket.setChangedDate(cursor.getString(5));
					sqlPacket.setStructureType(cursor.getString(6));
					sqlPacket.setTitle(cursor.getString(7));

					//Log.e(TAG, sqlPacket.toString());
					
					
//					if (sqlPacket.getCacheStatus() != SqlPacket.CACHE_DELETING) {
						if (true) {
						try {
							DataOutPacket packetDOP = new DataOutPacket(sqlPacket);
							//Log.e(TAG, packetDOP.toString());
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
				return packets;
			}
		} catch (Exception e) {
			Log.e("Exception", e.toString());
			e.printStackTrace();
			return null;
		}
			
	}	
	
	/**
	 * Requests a list of DataOutPackets from the local cache
	 * @param structureType - Structure types to filter on
	 * @return - List of DataOutPackets in the local cache
	 */
	public ArrayList<DataOutPacket> getPacketList(List<String> structureTypes)	{
//		OpenHelper openHelper = new OpenHelper(this.context);
//		this.db = openHelper.getWritableDatabase();
//		Cursor cursor = null;
		Log.e(TAG, "getPacketList(List<String> structureTypes)");
		String structureTypesString = "";
		for (String type : structureTypes) {
			structureTypesString += "'" + type + "',";
		}
		structureTypesString = structureTypesString.replaceAll("[,]$","");
		String whereString = "StructureType in (" + structureTypesString + ")";
		
		return getPacketList(whereString);

	}		
	
	/**
	 * Requests a list of DataOutPackets from the local cache
	 * @param structureType - Structure types to filter on
	 * @return - List of DataOutPackets in the local cache
	 */
	public ArrayList<DataOutPacket> getPacketListFull(List<String> structureTypes)	{
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		Cursor cursor = null;
		
		String structureTypesString = "";
		for (String type : structureTypes) {
			structureTypesString += "'" + type + "',";
		}
		structureTypesString = structureTypesString.replaceAll("[,]$","");
		
		String query = "select PacketID, PacketSql, RecordId, DrupalId, CacheStatus, ChangedDate, StructureType, Title from PERSISTENTCACHE " +
				"			where StructureType in (" + structureTypesString + ")";

		ArrayList<DataOutPacket> packets = new ArrayList<DataOutPacket>();
		try {
			cursor = this.db.rawQuery(query, null);
			if (cursor.moveToFirst()) {
				do 	{
					SqlPacket sqlPacket = new SqlPacket();
					sqlPacket.setSqlPacketId(cursor.getString(0));
					sqlPacket.setPacket(cursor.getString(1));
					sqlPacket.setRecordId(cursor.getString(2));
					sqlPacket.setDrupalId(cursor.getString(3));
					sqlPacket.setCacheStatus(cursor.getInt(4));
					sqlPacket.setChangedDate(cursor.getString(5));
					sqlPacket.setStructureType(cursor.getString(6));
					sqlPacket.setTitle(cursor.getString(7));

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
				return packets;
			}
		} catch (Exception e) {
			Log.e("Exception", e.toString());
			e.printStackTrace();
			return null;
		}
	}	
	
	/**
	 * Requests a list of SqlPackets from the local cache
	 * @return - List of SqlPackets in the local cache
	 */
	public List<SqlPacket> getPacketListAsSqlPacket()	{
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		Cursor cursor = null;

		String query = "select PacketID, PacketSql, RecordId, DrupalId, CacheStatus, ChangedDate, StructureType, Title from PERSISTENTCACHE";
		cursor = this.db.rawQuery(query, null);

		if (cursor.moveToFirst()) {
			List<SqlPacket> packets = new ArrayList<SqlPacket>();

			do 	{
				SqlPacket sqlPacket = new SqlPacket();
				sqlPacket.setSqlPacketId(cursor.getString(0));
				sqlPacket.setPacket(cursor.getString(1));
				sqlPacket.setRecordId(cursor.getString(2));
				sqlPacket.setDrupalId(cursor.getString(3));
				sqlPacket.setCacheStatus(cursor.getInt(4));
				sqlPacket.setChangedDate(cursor.getString(5));
				sqlPacket.setStructureType(cursor.getString(6));
				sqlPacket.setTitle(cursor.getString(7));

				packets.add(sqlPacket);
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
	
	public void createNewSqlPacket(SqlPacket packet) {

		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();

		try	{
			ContentValues insertValues = new ContentValues();
			insertValues.put("PacketSql", packet.getPacketJson());
			insertValues.put("RecordId", packet.getRecordId());
			insertValues.put("DrupalId", packet.getDrupalId());
			insertValues.put("CacheStatus", packet.getCacheStatus());
			insertValues.put("ChangedDate", packet.getChangedDate());
			insertValues.put("StructureType", packet.getStructureType());
			insertValues.put("Title", packet.getTitle());
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
			updateValues.put("ChangedDate", packet.getChangedDate());
			updateValues.put("StructureType",  packet.getStructureType());
			updateValues.put("Title",  packet.getTitle());
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
	
	public SqlPacket getPacketByRecordId(String recordId) {
		SqlPacket outPacket = null;
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		Cursor cursor = null;

		String query = "select PacketID, RecordId, DrupalId, PacketSql, ChangedDate, StructureType, CacheStatus, Title from PERSISTENTCACHE where RecordId = '" + recordId + "'" ;
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
				outPacket.setChangedDate(cursor.getString(4));
				outPacket.setStructureType(cursor.getString(5));
				outPacket.setCacheStatus(cursor.getInt(6));
				outPacket.setTitle(cursor.getString(7));
				
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
	public SqlPacket getPacketByDrupalId(String drupalId) {
		SqlPacket outPacket = null;
		OpenHelper openHelper = new OpenHelper(this.context);
		this.db = openHelper.getWritableDatabase();
		Cursor cursor = null;

		String query = "select PacketID, RecordId, DrupalId, PacketSql, ChangedDate, StructureType, CacheStatus, Title from PERSISTENTCACHE where DrupalId = '" + drupalId + "'" ;
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
				outPacket.setChangedDate(cursor.getString(4));
				outPacket.setStructureType(cursor.getString(5));
				outPacket.setCacheStatus(cursor.getInt(6));
				outPacket.setTitle(cursor.getString(7));
				
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
			String createPERSISTENTCACHE = "CREATE TABLE IF NOT EXISTS PERSISTENTCACHE ("
					+ "PacketID INTEGER PRIMARY KEY, "
					+ "PacketSql TEXT, "
					+ "RecordId TEXT, "
					+ "DrupalId TEXT, "
					+ "CacheStatus INTEGER, "
					+ "ChangedDate TEXT, "
					+ "StructureType TEXT, "
					+ "Title TEXT);";
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