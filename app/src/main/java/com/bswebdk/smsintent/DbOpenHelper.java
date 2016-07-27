/*
    Copyright © 2016 BSWeb.DK <bsweb@bruchhaus.dk>
    This file is part of SMSIntent (com.bswebdk.smsintent).

    SMSIntent is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SMSIntent is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SMSIntent.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.bswebdk.smsintent;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

//A database helper used to encapsulate functionality to
//create, update and manage rows and columns in the database

public class DbOpenHelper extends SQLiteOpenHelper {

	//Information about the database version, name and table used
	public static final int DATABASE_VERSION = 3;
	public static final String DATABASE_FILENAME = "smsintent.db";
	public static final String TABLE_NAME = "intents";
	
	//Constants for ordering of queries
	public static final int ORDERBY_MOST_USED = 0;
	public static final int ORDERBY_LAST_USED = 1;
	public static final int ORDERBY_TITLE = 2;
	
	//Fields in the table named TABLE_NAME
	public static final String ID = "_id"; //The ID of the intent
	public static final String CREATED = "dtc"; //The datetime when the intent was created
	public static final String MODIFIED = "dtm"; //The datetime when the intent was last modified
	public static final String LAST_USED = "dtu"; //The datetime when the intent was last used
	public static final String HITS = "hit"; //The number of usages for this intent
	public static final String TITLE = "tit"; //The title of the intent
	public static final String ADDRESS = "addr"; //The default address (phone number) used with this intent
	public static final String VALUE = "val"; //The value (text) of the intent
	public static final String TYPE = "typ"; //The type of the inten (see "TYPE_*" below)
	public static final String GROUP = "grp"; //The group to whom the intent belongs
	public static final String PARENT = "prnt"; //If this item is a group, this value is non-zero
	public static final String COUNT = "cnt"; //If this item is a group, this value is the number of intents in the group
	
	//Values used in the field TYPE
	public static final int TYPE_SIMPLE = 0; //Used to identify a simple text intent
	public static final int TYPE_QUERY = 1; //Ysed to define an intent that contains questions
	public static final int TYPE_RECIPIENT = 2; //As TYPE_SIMPLE but with a recipient (ADDRESS defined)
	public static final int TYPE_RECIPIENT_QUERY = 3; //As TYPE_QUERY but with a recipient (ADDRESS defined)
	
	public DbOpenHelper(Context context) {
		//Create the helper object and connect to the database
		super(context, DATABASE_FILENAME, null, DATABASE_VERSION);
		Global.LogI("DbOpenHelper: cTor");
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		//Create the table
		Global.LogI("DbOpenHelper: onCreate");
		db.execSQL(
		  "CREATE TABLE " + TABLE_NAME + " (" +
		  ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		  CREATED + " INTEGER, " +
		  MODIFIED + " INTEGER, " +
		  LAST_USED + " INTEGER, " +
		  HITS + " INTEGER, " +
		  TITLE + " TEXT, " +
		  ADDRESS + " TEXT, " +
		  VALUE + " TEXT, " +
		  TYPE + " INTEGER, " +
		  GROUP + " INTEGER, " +
		  PARENT + " INTEGER, " +
		  COUNT + " INTEGER" +
		  ")"
		);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int fromVer, int toVer) {
		//Upgrade the database from previous version(s)
		Global.LogI("DbOpenHelper: onUpgrade");
		if (fromVer == 1) {
			db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + GROUP + " INTEGER");
			db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + PARENT + " INTEGER");
			db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COUNT + " INTEGER");
			db.execSQL("UPDATE " + TABLE_NAME + " SET " + GROUP + "=0, " + PARENT + "=0, " + COUNT + "=0");
		} else if (fromVer == 2) {
			db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + GROUP + " INTEGER");
			db.execSQL("UPDATE " + TABLE_NAME + " SET " + GROUP + "=0");
		}
	}
	
	public int isGroup(int id, SQLiteDatabase db) {
		//Get the value of the GROUP column for the row with the given id
		if (db == null) db = getReadableDatabase();
		int res = -1;
		String[] cols = { GROUP };
		Cursor c = db.query(TABLE_NAME, cols, ID + "=" + id, null, null, null, null);
		if (c.moveToFirst()) res = c.getInt(0);
		c.close();
		return res;
	}
	
	public Cursor openListCursor(int orderBy, int groupId) {
		//Return a cursor that can be used for the listing in MainActivity
		//with the specified ordering
		SQLiteDatabase db = getReadableDatabase();
		String oBy = GROUP + " DESC, ";
		if (orderBy == ORDERBY_TITLE) oBy += TITLE + " ASC";
		else if (orderBy == ORDERBY_LAST_USED) oBy += LAST_USED + " DESC";
		else oBy += HITS + " DESC, " + LAST_USED + " DESC";
		return db.query(TABLE_NAME, null, PARENT + "=" + groupId, null, null, null, oBy);
	}
	
	public Cursor openEditCursor(int id) {
		//Return a cursor with values used for editing in EditActivity
		SQLiteDatabase db = getReadableDatabase();
		return db.query(TABLE_NAME, null, ID + "=" + id, null, null, null, null);
	}

	public Cursor openCustomCursor(String selection, String order) {
		//Return a cursor based on a custom selection and ordering
		SQLiteDatabase db = getReadableDatabase();
		return db.query(TABLE_NAME, null, selection, null, null, null, order);
	}
	
	public int deleteRow(int id) {

		//Delete the row with the given id
		SQLiteDatabase db = getWritableDatabase();

		//Get info about the row to delete
		String[] cols = {GROUP, PARENT};
		Cursor cur = db.query(TABLE_NAME, cols, ID + "=" + id, null, null, null, null);
		int group = 0, parent = 0, res = 0;
		if (cur.moveToFirst()) {
			group = cur.getInt(0);
			parent = cur.getInt(1);
		}
		cur.close();


		// If the rows is a group, all rows belonging to the group must be deleted
		if (group > 0) res += db.delete(TABLE_NAME, PARENT + "=" + id, null);

		//If row belongs to a group that groups counter must be decremented
		else if (parent > 0) incrementGroupCount(db, parent, true);

		//Now we can delete the row itself
		res += db.delete(TABLE_NAME, ID + "=" + id, null);

		//Return number of rows deleted
		return res;

	}

	private boolean incrementGroupCount(SQLiteDatabase db, long groupId, boolean decrement) {
		//Increment or decrement number of items in a group using sql:
		//update table set count=count[+|-]1 where id=groupId
		try {
			String cc = decrement ? "-" : "+";
			db.execSQL("UPDATE " + TABLE_NAME + " SET " + COUNT + "=" + COUNT + cc + "1 WHERE " + ID + "=" + groupId);
		} catch (Exception e) {
			//Update failed
			return false;
		}
		return true;
	}

	public long insertNewRow(String title, String address, String value, int type, int groupId, boolean isGroup) {

		//Insert a new row in the table and return the id of the created row
		SQLiteDatabase db = getWritableDatabase();
		long time = System.currentTimeMillis();
		ContentValues vals = new ContentValues();
		vals.put(CREATED, time);
		vals.put(MODIFIED, time);
		vals.put(LAST_USED, 0);
		vals.put(HITS, 0);
		vals.put(TITLE, title);
		vals.put(ADDRESS, address == null ? "" : address);
		vals.put(VALUE, value == null ? "" : value);
		vals.put(TYPE, type);
		vals.put(GROUP, isGroup ? 1 : 0);
		vals.put(PARENT, groupId);
		vals.put(COUNT, 0);
		long newId = db.insert(TABLE_NAME, null, vals);
		if (groupId > 0) incrementGroupCount(db, groupId, false);
		return newId;

	}

	public long insertRawRow(long created, long modified, long last_used, long hits,
							 String title, String address, String value, long type,
							 long group, long parent) {

		//Insert a row created from raw values and return id of new row
		//Used for import in AboutActivity
		SQLiteDatabase db = getWritableDatabase();
		ContentValues vals = new ContentValues();
		vals.put(CREATED, created);
		vals.put(MODIFIED, modified);
		vals.put(LAST_USED, last_used);
		vals.put(HITS, hits);
		vals.put(TITLE, title);
		vals.put(ADDRESS, address == null ? "" : address);
		vals.put(VALUE, value == null ? "" : value);
		vals.put(TYPE, type);
		vals.put(GROUP, group);
		vals.put(PARENT, parent);
		vals.put(COUNT, 0);
		long newId = db.insert(TABLE_NAME, null, vals);
		if (parent > 0) incrementGroupCount(db, parent, false);
		return newId;
	}
	
	//public boolean updateRow(long id, String value) { return updateRow(id, null, null, value, -1, -1); }
	public boolean updateRow(int id, String title, String address, String value, int type) {
		//Update a row and adjust the MODIFIED field accordingly
		long time = System.currentTimeMillis();
		ContentValues vals = new ContentValues();
		vals.put(MODIFIED, time);
		if (title != null) vals.put(TITLE, title);
		if (address != null) vals.put(ADDRESS, address);
		if (value != null) vals.put(VALUE, value);
		if (type >= 0) vals.put(TYPE, type);
		SQLiteDatabase db = getWritableDatabase();
		return db.update(TABLE_NAME, vals, ID + "=" + id, null) > 0;
	}
	
	public boolean addHitForRow(int id, int groupId) {
		//Increment the hit counter for a row
		boolean res = false;
		SQLiteDatabase db = getWritableDatabase();
		try {
			long time = System.currentTimeMillis();
			String sql = "UPDATE " + TABLE_NAME + " SET " + HITS + "=" + HITS + "+1," + LAST_USED + "=" + time + " WHERE " + ID + "=";
			if (groupId > 0) db.execSQL(sql + groupId);
			db.execSQL(sql + id);
			res = true;
		} catch (Exception e) {}
		return res;
	}
	
}
