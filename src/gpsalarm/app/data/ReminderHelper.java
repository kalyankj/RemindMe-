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
import gpsalarm.app.datatype.Alert;
import gpsalarm.app.datatype.Position;
import gpsalarm.app.datatype.Reminder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

public class ReminderHelper extends AbstractHelper {
    private static final String TAG = ReminderHelper.class.getName();
	
	public static final String DEFAULT_AUTHOR = "user1";
	public static final String DEFAULT_TEAM = "iitmaa";
	public static final String DEFAULT_FORPIN = "Sydney";
  
    //Database field definition
    public static final String ROWID = "_id";
    public static final String TITLE = "title";
    public static final String DETAIL = "detail";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String PRIORITY = "priority";
	public static final String STATE = "state"; // "A" = active, A1=snoozed, A2=alarm stopped permanently, "D" = disabled/deleted
	public static final String VALIDTILL = "valid_till"; //date due
	public static final String DISTANCE = "mydistance"; //date due
	public static final String NEAREST = "nearest"; //date due
	public static final String NEAREST_ON = "nearest_on"; //date due
	
	//audit control tasks.
	public static final String AUTHOR = "author"; //userID
	public static final String TEAM = "team"; //stores place name if found	
	public static final String CREATED = "created"; //stores last modififed.	
	public static final String MODIFIED = "modified"; //stores last modififed.

	//For Sync with Repository
	public static final String SYNCFLAG = "syncflag"; //SQLLite changes I=inserted by this, E-edited by this, D-, belongs to me. delete for all.
	public static final String GLOBAL_ID = "g_id"; //R's id
	public static final String GLOBAL_TIMESTAMP = "g_timestamp"; //R's time stamp

	public ReminderHelper(Context context) {
		super(context);
	}

    public void cleanup() {
    	Long delay = Calendar.getInstance().getTime().getTime() - 86400000;
//    	getWritableDatabase().execSQL("delete from tasks where state = 'D' and syncflag = 'D' and modified > " + delay); //unnecessary
     	getWritableDatabase().execSQL("update tasks set state = 'A' where state = 'I' and modified < " + delay);
         	
    	Log.d(TAG, "removed disabled pins");
    }

    public int countReminders(String user) {
    	Cursor c = getActiveReminders(null, null) ;
    	int count = c.getCount();
    	Log.d(TAG, "Number of active pins="+count);
    	c.close();
    	return count;
    }

	public Long insert(Reminder r) {
    	Long rid = null;
    	if (r.getLatitude() == null) Log.e(TAG, "latitude cannot be null");
    	else if (r.getLongitude() == null) Log.e(TAG, "longitude cannot be null");
    	else if (r.getTitle() == null) Log.e(TAG, "title cannot be null");
    	else { 
	    	ContentValues cv = getAsCV(r);
	        rid = getWritableDatabase().insert("tasks", TITLE, cv);
    	}
        return rid;
	}
 
	public void insert(Position p) {
		ContentValues cv = new ContentValues();
        cv.put("username", p.getUsername());
        cv.put("created", Calendar.getInstance().getTime().getTime());
        cv.put(LATITUDE, p.getLatitude());
        cv.put(LONGITUDE, p.getLongitude());
	    getWritableDatabase().insert("user", "username", cv);
        return;
	}

	public void insert(Alert a) {
		ContentValues cv = new ContentValues();
        cv.put("username", a.getUsername());
        cv.put("modified", a.getTimestamp());
        cv.put("gid", a.getGid());
        cv.put("distance", a.getDist());
        cv.put("statusflag", "");
	    getWritableDatabase().insert("alerts", null, cv);
        return;
	}

	public Alert getAlert(String username, Long gid) {
		StringBuilder buf=new StringBuilder("SELECT username, gid, distance, modified FROM alerts where username = '"+username+"' and gid ="+gid+";");		
		Cursor model = (getReadableDatabase().rawQuery(buf.toString(),null));		
		model.moveToFirst();
		Alert a = null;
		if (model.getCount()>0) {
			a = new Alert();
			a.setUsername(model.getString(0));
			a.setGid(model.getLong(1));
			a.setDist(model.getInt(2));
			a.setTimestamp(model.getLong(3));
		}
		model.close();
		
		return a;
	}	

	
	
	public Cursor getAlertsList(String str) {
		StringBuilder buf=new StringBuilder("select a.gid as _id, a.username, c.title, b.min as distance, a.modified as modified ");
		if (str.equals("username")) {
			buf.append("from alerts a,(select min(distance) as min, username from alerts group by username) as b, tasks c ");
			buf.append("where a.username = b.username and a.distance = b.min and a.gid = c.g_id and a.statusflag != 'D';");				
		}
		else {
			buf.append("from alerts a,(select min(distance) as min, gid from alerts group by gid) as b, tasks c ");
			buf.append("where a.gid = b.gid and a.distance = b.min and a.gid = c.g_id and a.statusflag != 'D';");							
		}
		return(getReadableDatabase().rawQuery(buf.toString(), null));
	}	
	
	public boolean updateAlerts(Alert a) {
		boolean alert = false;
		if (getAlert(a.getUsername(), a.getGid()) != null) {
	    	getWritableDatabase().execSQL("update alerts set distance ="+a.getDist()+", modified ="+a.getTimestamp()
	    			+" where username = '"+a.getUsername()+"' and gid =" +a.getGid()+";");
		}
		else {
			insert(a);
			alert = true;
		}
    	Log.d(TAG, "executed: updateAlerts");		
    	return alert;
	}
	
	//the alerts table will contain only current alerts which is identified by a timestamp.
	// all other alerts are deleted every time there is a update from server. This method is used for that.
	public void markDeleteAlerts(Long timestamp) {		
    	getWritableDatabase().execSQL("update alerts set statusflag = 'D' where modified != "+timestamp+";");
    	Log.d(TAG, "removed disabled pins");		
	}
	
	public Position getPosition(String username) {
		StringBuilder buf=new StringBuilder("SELECT username, created, latitude, longitude FROM user where username = '"+username+"';");		
		Cursor model = (getReadableDatabase().rawQuery(buf.toString(),null));		
		model.moveToFirst();
		Position p = null;
		if (model.getCount()>0) {
			p = new Position();
			p.setUsername(model.getString(0));
			p.setLatitude(model.getInt(1));
			p.setLongitude(model.getInt(2));
			p.setCreated(model.getLong(3));
		}
		model.close();
		
		return p;
	}

	public void updateUser(Position p) {
		if (getPosition(p.getUsername()) != null) {
			ContentValues cv = new ContentValues();
	        cv.put("username", p.getUsername());
	        cv.put(LATITUDE, p.getLatitude());
	        cv.put(LONGITUDE, p.getLongitude());
			String[] args={p.getUsername()};		
			getWritableDatabase().update("user", cv, "username=?",args);
		}
		else {
			insert(p);
		}
    	Log.d(TAG, "executed: updatewithRowid");		
	}
	
	public Cursor getActiveReminders(String where, String orderBy) {
		String defaultWhere = STATE +" like 'A%'" ;
		StringBuilder buf=new StringBuilder("SELECT _id, latitude, longitude, title, detail, priority, state, author, team, created, modified, valid_till, mydistance, nearest, nearest_on, g_id, syncflag, g_timestamp FROM tasks");
		
		buf.append(" WHERE ");
		buf.append(defaultWhere);
		
		if (where!=null) {
			buf.append(" AND ");
			buf.append(where);
		}
		
		if (orderBy!=null) {
			buf.append(" ORDER BY ");
			buf.append(orderBy);
		}
		
		return(getReadableDatabase().rawQuery(buf.toString(), null));
	}

	public List<Reminder> getReminders(String where, String orderBy) {
//		String defaultWhere = STATE +"= 'A'" ;
		StringBuilder buf=new StringBuilder("SELECT _id, latitude, longitude, title, detail, priority, state, " +
				"author, team, created, modified, valid_till, mydistance, nearest, nearest_on, g_id, syncflag, g_timestamp FROM tasks");
		
		buf.append(" WHERE ");
//		buf.append(defaultWhere);
		
		if (where!=null || orderBy == "") {
//			buf.append(" AND ");
			buf.append(where);
		}
		
		if (orderBy !=null || orderBy == "") {
			buf.append(" ORDER BY ");
			buf.append(orderBy);
		}
		
		List<Reminder> rslt = new ArrayList<Reminder>();
		Cursor c = getReadableDatabase().rawQuery(buf.toString(), null);
		if (c.getCount()>0) {
			c.moveToFirst();
			while (!c.isAfterLast()) {
				Reminder r = new Reminder();
				if (c.getCount()>0) r= getAsModel(c);
				rslt.add(r);
				c.moveToNext();
			}
		}
		
		c.close();
		return rslt;
	}	


	public Reminder getReminderByRowid(String rowid) {
		String[] args={rowid};
		StringBuilder buf=new StringBuilder("SELECT _id, latitude, longitude, title, detail, " +
				"priority, state, author, team, " +
				"created, modified, valid_till, mydistance, nearest, nearest_on, g_id, syncflag, g_timestamp " +
				"FROM tasks");
		buf.append(" WHERE _ID=?;");
		
		Cursor model = (getReadableDatabase().rawQuery(buf.toString(),args));		
		model.moveToFirst();
		Reminder r = new Reminder();
		if (model.getCount()>0) r= getAsModel(model);
		model.close();
		return r;
	}

	public Reminder getReminderByGid(String g_id) {
		String[] args={g_id};
		StringBuilder buf=new StringBuilder("SELECT _id, latitude, longitude, title, detail, " +
				"priority, state, author, team, " +
				"created, modified, valid_till, mydistance, nearest, nearest_on, g_id, syncflag, g_timestamp " +
				"FROM tasks");
		buf.append(" WHERE g_id=?");
		Reminder r = null;
		Cursor model = (getReadableDatabase().rawQuery(buf.toString(),args));		
		model.moveToFirst();
		if (model.getCount()>0) r= getAsModel(model);
		model.close();
		return r;
	}
	private ContentValues getAsCV(Reminder r) {
    	Long now = Calendar.getInstance().getTime().getTime();
		ContentValues cv = new ContentValues();
        cv.put(TITLE, r.getTitle());
        cv.put(DETAIL, r.getDetail());
        cv.put(LATITUDE, r.getLatitude());
        cv.put(LONGITUDE, r.getLongitude());
        cv.put(PRIORITY, r.getPriority());
        cv.put(STATE, r.getState());
        cv.put(AUTHOR, r.getAuthor());
        cv.put(TEAM, r.getTeam());
        cv.put(VALIDTILL, r.getValidTill());
        cv.put(DISTANCE, r.getDistance());
        cv.put(NEAREST, r.getNearest());
        cv.put(NEAREST_ON, r.getNearestOn());
        cv.put(SYNCFLAG, r.getSyncflag());
        cv.put(MODIFIED, r.getModified());
        cv.put(GLOBAL_ID, r.getG_id());
        cv.put(CREATED, r.getCreated());
        cv.put(MODIFIED, r.getModified());
        cv.put(GLOBAL_ID, r.getG_id());
        cv.put(GLOBAL_TIMESTAMP, r.getG_timestamp());
        cv.put(SYNCFLAG, r.getSyncflag());
		return cv;
	}	
	
	private Reminder getAsModel(Cursor model) {
		Reminder r = new Reminder();
			r.setRowid(model.getLong(0));
			r.setLatitude(model.getInt(1));
			r.setLongitude(model.getInt(2));
			r.setTitle(model.getString(3));
			r.setDetail(model.getString(4));
			r.setPriority(model.getString(5));
			r.setState(model.getString(6));
			r.setAuthor(model.getString(7));
			r.setTeam(model.getString(8));
			r.setCreated(model.getLong(9));
			r.setModified(model.getLong(10));
			r.setValidTill(model.getLong(11));
			r.setDistance(model.getInt(12));
			r.setNearest(model.getInt(13));
			r.setNearestOn(model.getLong(14));
			r.setG_id(model.getLong(15));
			r.setSyncflag(model.getString(16));
			r.setG_timestamp(model.getLong(17));
		return r;
	}

	public void updateWithRowid(Reminder r, String rowid) {
        ContentValues cv = getAsCV(r);
		String[] args={rowid};		
		getWritableDatabase().update("tasks", cv, "_ID=?",args);
    	Log.d(TAG, "executed: updatewithRowid");		
	}	

	public void updateWithG_Id(Reminder r, String g_id) {
		Long now = Calendar.getInstance().getTime().getTime();
		String[] args={g_id};		
        ContentValues cv = getAsCV(r);
		getWritableDatabase().update("tasks", cv, "G_ID=?",args);
//    	Log.d(TAG, "executed:"+sql);		
	}

	public void delete(String g_id) {		
    	getWritableDatabase().execSQL("delete from tasks where g_id = '"+g_id+"'");
    	Log.d(TAG, "removed disabled pins");		
	}
	
	public void markDelete(Reminder r, String username) {
		Long now = Calendar.getInstance().getTime().getTime();
		String sql = null;
		if (r.getAuthor().equals(username))
			sql = "update tasks set modified="+now+", state = 'D', syncflag = 'D' where g_id = "+r.getG_id();
		else
			sql = "update tasks set modified="+now+", state = 'D' where g_id = "+r.getG_id();
		
    	getWritableDatabase().execSQL(sql);
    	Log.d(TAG, "executed:"+sql);		
	}
	
//"SELECT _id, latitude, longitude, title, detail, priority, state, author, team, created, modified, valid_till, mydistance FROM tasks"
	public Long getRowid(Cursor c) {
		return c.getLong(0);
	}
	
	public Integer getLatitude(Cursor c) {
		return(c.getInt(1));
	}
	
	public Integer getLongitude(Cursor c) {
		return(c.getInt(2));
	}
	
	public String getTitle(Cursor c) {
		return(c.getString(3));
	}
	
	public String getDetail(Cursor c) {
		return(c.getString(4));
	}
	
	public String getPriority(Cursor c) {
		return(c.getString(5));
	}
	
	public String getState(Cursor c) {
		return(c.getString(6));
	}
	
	public String getAuthor(Cursor c) {
		return(c.getString(7));
	}
	
	public String getTeam(Cursor c) {
		return(c.getString(8));
	}
	
	public Long getCreated(Cursor c) {
		return(c.getLong(9));
	}
	
	public Long getModified(Cursor c) {
		return(c.getLong(10));
	}
	
	public Long getValidtill(Cursor c) {
		return(c.getLong(11));
	}
	
	public int getDistance(Cursor c) {
		return(c.getInt(12));
	}
	
	public int getNearest(Cursor c) {
		return(c.getInt(13));
	}
	
	public Long getNearestOn(Cursor c) {
		return(c.getLong(14));
	}

	public Long getG_id(Cursor c) {
		return(c.getLong(15));
	}
	
	public String getSyncflag(Cursor c) {
		return(c.getString(16));
	}
}