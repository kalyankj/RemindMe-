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
import gpsalarm.app.datatype.Tweet;

import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class TimelineHelper extends AbstractHelper {
    private static final String TAG = TimelineHelper.class.getName();

    public static final String ROWID = "rid";
	public static final String FRIEND = "friend"; //userID
	public static final String STATUS = "status";
	public static final String CREATED = "created"; //stores last modififed.
    public static final String GLOBALID = "gid";
       
	public TimelineHelper(Context context) {
		super(context);
	}

    public void cleanup() {
    	Long delay = Calendar.getInstance().getTime().getTime() - 86400000;
    	getWritableDatabase().execSQL("delete from timeline where created < " + delay);
//     	getWritableDatabase().execSQL("update tasks set state = 'A' where state = 'I' and modified < " + delay);
         	
    	Log.d(TAG, "removed disabled pins");
    }

	public Long insert(Tweet s) {
		Long rid = null;
		if (s.getStatus().length() > 0) {
			Long now = Calendar.getInstance().getTime().getTime();
			ContentValues cv = new ContentValues();
			cv.put(ROWID, s.getRid());
			cv.put(FRIEND, s.getFriend());
			cv.put(STATUS, s.getStatus());
			cv.put(CREATED, now);
			cv.put(GLOBALID, s.getGid());
			rid = getWritableDatabase().insert("timeline", FRIEND, cv);
		}
		return rid;
	}    

	public Cursor getTimeline(String where) {
		String defaultWhere = "" ;
		StringBuilder buf=new StringBuilder("SELECT _id, rid, friend, gid, created, status  FROM timeline");
		
		if (where!=null) {
			buf.append(" WHERE ");
			buf.append(where);
		}
		
		buf.append(" ORDER BY created DESC");
		
		return(getReadableDatabase().rawQuery(buf.toString(), null));
	}	

    public int countEntries(int gid) {
    	Cursor c = getTimeline("gid = "+gid) ;
    	int count = c.getCount();
    	Log.d(TAG, "Number of active pins="+count);
    	c.close();
    	return count;
    }
    
	public boolean contains(String rid) {
		//rid represents a status line's unique id.
		Cursor c = getReadableDatabase().rawQuery("select rid from timeline where rid = " + rid, null);
    	Log.d(TAG, "contains being evaluated");
    	boolean rslt = c.getCount() > 0 ? true : false;
    	c.close();
		return rslt;
	}

	public int getId(Cursor c) {
		return(c.getInt(0));
	}
	
	public int getRid(Cursor c) {
		return(c.getInt(1));
	}	
	
	public String getFriend(Cursor c) {
		return(c.getString(2));
	}

	public String getGid(Cursor c) {
		return(c.getString(3));
	}
	
	public Long getCreated(Cursor c) {
		return(c.getLong(4));
	}


	public String getStatus(Cursor c) {
		return(c.getString(5));
	}	
	
}
