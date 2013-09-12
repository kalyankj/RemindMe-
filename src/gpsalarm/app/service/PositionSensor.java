package gpsalarm.app.service;
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
import java.util.Calendar;
import java.util.List;

import gpsalarm.app.R;
import gpsalarm.app.controller.AlarmEdit3;
import gpsalarm.app.data.ReminderHelper;
import gpsalarm.app.datatype.CircleOverlay;
import gpsalarm.app.datatype.Position;
import gpsalarm.app.datatype.Reminder;
import gpsalarm.app.service.PostMonitor.Account;

import com.google.android.maps.GeoPoint;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public abstract class PositionSensor extends Service {

	private static final String TAG = PositionSensor.class.getSimpleName();
	protected NotificationManager mNM;
	protected ReminderHelper db;
	long minUpdateTime = 3600000;
	float minUpdateDistance = 30;
	protected LocationManager locMgr;
	private Integer triggerDistance = 6000; //micro degrees
	private Location location;
	public Location getLocation() {
		return location;
	}

	public LocationManager getLocMgr() {
		return locMgr;
	}
	SharedPreferences prefs=null;	
	String username = null;
	float nearBy = (float) 5.0;
	protected LocationListener locListener = new LocationListener() {

			public void onLocationChanged(Location location) {
				p1 = getMyGeoPoint(location);
				Log.d(TAG, "Location=("+p1.getLatitudeE6()+","+p1.getLongitudeE6()+")");
				parsePinsForReminder();
			}
	
			public void onProviderDisabled(String provider) {
				doProviderDisabledAction();
			}
	
			public void onProviderEnabled(String provider) {
				doProviderEnabledAction();
			}
	
			public void onStatusChanged(String provider, int status, Bundle extras) {
				doProviderStatusChanged(provider, status, extras);
			}
		};
	private GeoPoint p1;
	private float speed = (float) 0.0;
	private String provider;

	public PositionSensor() {
		super();
		Log.d(TAG, "Position Sensor Created");
	}

	private SharedPreferences.OnSharedPreferenceChangeListener prefListener=
		 new SharedPreferences.OnSharedPreferenceChangeListener() {

			public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {				
				username = prefs.getString("user", null);
			}
		};	

		
		
	@Override
	public void onCreate() {
		super.onCreate();

	}

	protected void registerPositionSensor() {
		Log.d(TAG, "Executing registerPositionSensor");
		if (locMgr == null) {
			locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}
		if (locMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, minUpdateTime, minUpdateDistance, locListener);
			provider = LocationManager.GPS_PROVIDER;
		}
		else if (locMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			locMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minUpdateTime, minUpdateDistance, locListener);
			provider = LocationManager.NETWORK_PROVIDER;
		}
		else provider = null;
		
		//Register Preference listener
		prefs=PreferenceManager.getDefaultSharedPreferences(this);
		username = prefs.getString("user", null);
	}

	protected void doProviderStatusChanged(String provider, int status,
			Bundle extras) {
				if (status == LocationProvider.OUT_OF_SERVICE ) {
					Toast.makeText(this, "Sensor out-of-service", Toast.LENGTH_SHORT).show();
				}
				else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
					Toast.makeText(this, "Sensor Temprorily-unavailable", Toast.LENGTH_SHORT).show();
				}
				else if (status == LocationProvider.AVAILABLE) {
					registerPositionSensor();
				}
				
			}

	protected void doProviderEnabledAction() {
		Toast.makeText(this, "Location Sensor enabled", Toast.LENGTH_SHORT).show();
		registerPositionSensor();
	}

	protected void doProviderDisabledAction() {
		Toast.makeText(this, "Location Sensor not available", Toast.LENGTH_SHORT).show();
	}

	protected boolean monitorReminder(Reminder pin, PendingIntent pIntent) {
		float radius = (float) 1000.0;
		double latitude = ((double) pin.getPoint().getLatitudeE6())/1000000;
		double longitude = ((double) pin.getPoint().getLongitudeE6())/1000000;
		locMgr.addProximityAlert(latitude, longitude, radius, 1000000, pIntent);
		return true;
	}
	

	protected boolean deactivateReminderMonitoring(PendingIntent pIntent) {
		locMgr.removeProximityAlert(pIntent);
		return true;
	}
	
	private void parsePinsForReminder() {
			Log.i("GPSObserver", "Executing parsePinsForReminder");
//			if (!this.mBound) return;
			CircleOverlay circle = null;
			if (triggerDistance != null) {
				circle = new CircleOverlay(p1, triggerDistance);
				Log.i("GPSObserver", "myLoc=("+p1.getLatitudeE6()+","+p1.getLongitudeE6()+")");
				if (username != null) {
					username = prefs.getString("user", null);
					if (username != null) db.updateUser(new Position(username, p1.getLatitudeE6(), p1.getLongitudeE6()));
					Log.d("GPSObserver", "updated position -"+username+"("+p1.getLatitudeE6()+"'"+p1.getLongitudeE6());
				}
			}
			else {
				Log.e("GPSOverser", "Reminder trigger distance has not be set");
			}
		    // Get all of the rows from the database and create the item list
			String where = " state like 'A%'";
			String orderBy = null;
			List<Reminder> rlist = this.db.getReminders(where, orderBy);
			if (rlist.size() <= 0) return;
			for (Reminder r: rlist) {
				Long rowid = r.getRowid();
				int lat = r.getLatitude();
				int lng = r.getLongitude();
				GeoPoint p = new GeoPoint(lat,lng);
				float dist = (float)  0.11132 * circle.distance(p, p1); //dist in meters
				r.setDistance((int)dist);
				Log.d("GPSObserver", "rec-"+rowid+":("+lat+","+lng+") dist="+dist + "in microdegree"+circle.distance(p, p1));
				if (circle.contains(p)) {
//					Intent pData = new Intent(this.getBaseContext(), AlarmEdit2.class);
					Intent pData = new Intent(Intent.ACTION_GET_CONTENT, null, this.getBaseContext(), AlarmEdit3.class);
					pData.putExtra(ReminderHelper.GLOBAL_ID, String.valueOf(r.getG_id()));				
					pData.putExtra(ReminderHelper.LATITUDE, String.valueOf(lat));
					pData.putExtra(ReminderHelper.LONGITUDE, String.valueOf(lng));
					pData.putExtra("user", prefs.getString("user", null));
					String title = r.getTitle();
					title += " By:"+r.getAuthor();
					String detail = r.getDetail();
					if (r.getState().equals("A")) {
						this.showNotification(pData, dist, title, detail);
						r.setState("A1");
					}
					Log.d("GPSObserver", "rec-"+rowid+":("+lat+","+lng+") dist="+dist+": "+ r.getTitle());
				}
				else {
					if (r.getNearest() > dist) {
						Long now = Calendar.getInstance().getTime().getTime();
						r.setNearestOn(now);
					}
				}
				db.updateWithRowid(r, rowid.toString());
			}
			db.close();
//			Toast.makeText(this, "Parsed for Alerts", Toast.LENGTH_SHORT).show();
			
		}

	private GeoPoint getMyGeoPoint(Location location) {
			GeoPoint p1 = null;	
			this.location = location;
			if (location != null) {
				double lat = location.getLatitude();
				double lng = location.getLongitude();
				p1 = new GeoPoint((int) (lat * 1000000),(int) (lng * 1000000));
				
	//			if (location.hasSpeed()){
	//				float curSpeed = location.getSpeed();
	//				speed  = (float) (speed <= 0.0 ? curSpeed :(speed+curSpeed)/2.0);	
	//				if (speed < 2.0) triggerDistance = 4500 ;  //9 * 500m
	//				else if (speed <= 15.0) triggerDistance = 9000;  //9 * 1km
	//				else if (speed > 15.0) triggerDistance = (int) (300.0 * speed* 9.0);
	//				Toast.makeText(this, "speed="+speed+"; nearDistance"+triggerDistance, Toast.LENGTH_SHORT).show();
	//			}
				return p1;
			}
			
			return p1;
		}

	/**
	 * Show a notification while this service is running.
	 * @param dist 
	 * @param detail 
	 */
	private void showNotification(Intent alertPin, float dist, String title, String detail) {
	        // In this sample, we'll use the same text for the ticker and the expanded notification
	        CharSequence text = getText(R.string.reminder_alert);
	
	        // Set the icon, scrolling text and timestamp
	        Notification notification = new Notification(R.drawable.yellowpin, text, System.currentTimeMillis());
	
	        //This intent will start an activity when user selects the notification 
	        // The PendingIntent to launch our activity if the user selects this notification
	        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, alertPin, Intent.FLAG_ACTIVITY_NEW_TASK);
	        notification.defaults |= Notification.DEFAULT_SOUND;
	//        notification.flags |= Notification.FLAG_INSISTENT;
	        notification.flags |= Notification.FLAG_AUTO_CANCEL;
	        notification.defaults |= Notification.DEFAULT_VIBRATE;
	
	        // Set the info for the views that show in the notification panel.
	        notification.setLatestEventInfo(this,"At: "+Math.round(dist)+"m "+title,detail,  contentIntent);

	        
	        // Send the notification.
	        // We use a layout id because it is a unique number.  We use it later to cancel.
	        mNM.notify(R.string.reminder_alert, notification);
	    }

}