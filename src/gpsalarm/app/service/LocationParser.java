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
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicBoolean;
import gpsalarm.app.ILocationParser;
import gpsalarm.app.R;
import gpsalarm.app.controller.AlarmEdit2;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class LocationParser extends WakefulIntentService {
	public static final int NOTIFICATION_ID=1337;
	public static final String STATUS_UPDATE="gpsalarm.app.service.STATUS_UPDATE";
	public static final String FRIEND="gpsalarm.app.service.FRIEND";
	public static final String STATUS="gpsalarm.app.service.STATUS";
	public static final String CREATED_AT="gpsalarm.app.service.CREATED_AT";
	public static final String POLL_ACTION="gpsalarm.app.service.POLL_ACTION";
	private static final String NOTIFY_KEYWORD="snicklefritz";
	private static final int INITIAL_POLL_PERIOD=20000;
	private static final int POLL_PERIOD=100000;
	private final Binder binder=new LocalBinder();
	private AtomicBoolean isBatteryLow=new AtomicBoolean(false);
	public boolean getIsBatteryLow() {
		return isBatteryLow != null;
	}

	private AlarmManager alarm=null;
	private PendingIntent pi=null;

	public LocationParser() {
		super("LocationParser");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		registerReceiver(onBatteryChanged, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
		alarm=(AlarmManager)getSystemService(Context.ALARM_SERVICE);

		Intent i=new Intent(this.getApplicationContext(), OnAlarmLoctionReceiver.class);

		pi=PendingIntent.getBroadcast(this, 0, i, 0);
		setAlarm(INITIAL_POLL_PERIOD);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return(binder);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		alarm.cancel(pi);
		unregisterReceiver(onBatteryChanged);
	}
	
	@Override
	protected void doWakefulWork(Intent i) {
		if (i.getAction().equals(POLL_ACTION)) {
			Toast.makeText(this, "Woke up", Toast.LENGTH_SHORT);
			Log.i("LocationParser", "Did a wakeup");
		}
		
		setAlarm(isBatteryLow.get() ? POLL_PERIOD*10 : POLL_PERIOD);
	}
	
	private void setAlarm(long period) {
		alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
							SystemClock.elapsedRealtime()+period,
							pi);
	}
	

	
	private void showNotification() {
		final NotificationManager mgr=
			(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Notification note=new Notification(R.drawable.status,"New matching post!",System.currentTimeMillis());
		Intent i=new Intent(this, AlarmEdit2.class);
		
//		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
//							 Intent.FLAG_ACTIVITY_SINGLE_TOP);
		i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		PendingIntent pi=PendingIntent.getActivity(this, 0,i,0);
		
		note.setLatestEventInfo(this, "Identi.ca Post!","Found your keyword: "+NOTIFY_KEYWORD,
														pi);
		
		mgr.notify(NOTIFICATION_ID, note);
	}
	
	BroadcastReceiver onBatteryChanged=new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int pct=100
								*intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 1)
								/intent.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
			
			isBatteryLow.set(pct<=25);
		}
	};
	
	public class LocalBinder extends Binder implements ILocationParser {

		public boolean getBatteryStatus() {
			return getIsBatteryLow();
		}

	}
}
