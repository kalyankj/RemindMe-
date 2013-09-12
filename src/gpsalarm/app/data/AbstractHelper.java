package gpsalarm.app.data;
/*
* ****************************************************************************
*
* Copyright (C) 2013 Geosai Pty Ltd, Sydney, Australia.
* 
* Author: Kalyan Kumar Janakiraman (kalyankj @ gmail.com)
* Dated : 14th Feb 2011
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* *****************************************************************************
*/
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public abstract class AbstractHelper extends SQLiteOpenHelper {
	private static final String TAG = ReminderHelper.class.getSimpleName();
	private static final String DATABASE_NAME="tasks.db";
	private static final int SCHEMA_VERSION=1;
    
//    protected DatabaseHelper mDbHelper;
//    protected SQLiteDatabase mDb;

    private static final String TABLE_CREATE_TASKS =
        "create table tasks ( " +
        "_id integer primary key autoincrement, "+
        "title text, " +
        "detail text, " +
    	"latitude integer, " +
    	"longitude integer, "+
    	"priority text, "+
        "state text, " +
        "author text, " +
        "team text, " +
        "valid_till timestamp, " +
        "created timestamp, " +
        "modified timestamp, " +
        "syncflag text, " +
        "g_id integer key, " +
        "g_timestamp timestamp, " +
        "mydistance integer, " +
        "nearest integer, " +
        "nearest_on timestamp " +
        ");";

    private static final String TABLE_CREATE_TIMELINE =
        "create table timeline ( " +
        "_id integer primary key autoincrement, "+    
        "rid integer, " +
        "friend text, " +
        "status text, " +
        "created timestamp, " +
        "gid integer " +
        ");";

    private static final String TABLE_CREATE_POSITION=
        "create table user ( " + 
        "_id integer primary key autoincrement, "+  
        "username text not null, " +
        "created timestamp, " +
    	"latitude integer, " +
    	"longitude integer "+
        ");";
    
    private static final String TABLE_CREATE_ALERTS=
        "create table alerts ( " + 
        "_id integer primary key autoincrement, "+  
        "username text not null, " +
        "gid integer, " +
    	"distance integer, " +
    	"modified timestamp, "+
    	"statusflag text " +
        ");";
//    protected final Context mCtx;

    public AbstractHelper(Context context) {
            super(context, DATABASE_NAME, null, SCHEMA_VERSION);
    }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TABLE_CREATE_TASKS);
            db.execSQL(TABLE_CREATE_TIMELINE);
            db.execSQL(TABLE_CREATE_POSITION);
            db.execSQL(TABLE_CREATE_ALERTS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS routes");
            onCreate(db);
        }
  
//    public AbstractHelper(Context ctx) {
//        this.mCtx = ctx;
//    }
//
//    public SQLiteDatabase getwDatabase() throws SQLException {
//        mDbHelper = new DatabaseHelper(mCtx);
//        return mDbHelper.getWritableDatabase();
//    }
//
//    public SQLiteDatabase getrDatabase() throws SQLException {
//        mDbHelper = new DatabaseHelper(mCtx);
//        return mDbHelper.getReadableDatabase();
//    }
//    public void close() {
//        mDbHelper.close();
//    }

}
