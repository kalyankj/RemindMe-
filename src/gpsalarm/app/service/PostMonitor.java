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
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import gpsalarm.app.IPostListener;
import gpsalarm.app.IPostMonitor;
import gpsalarm.app.R;
import gpsalarm.app.controller.AlarmEdit3;
import gpsalarm.app.controller.AlertList;
import gpsalarm.app.data.ReminderHelper;
import gpsalarm.app.data.TimelineHelper;
import gpsalarm.app.datatype.Alert;
import gpsalarm.app.datatype.Alerts;
import gpsalarm.app.datatype.Position;
import gpsalarm.app.datatype.Reminder;
import gpsalarm.app.datatype.Tweet;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class PostMonitor extends WakefulIntentService {
	protected static final String TAG = PostMonitor.class.getSimpleName();
	public static final int NOTIFICATION_ID=1337;
	public static final String STATUS_UPDATE="gpsalarm.app.service.STATUS_UPDATE";
	public static final String FRIEND="gpsalarm.app.service.FRIEND";
	public static final String STATUS="gpsalarm.app.service.STATUS";
	public static final String CREATED_AT="gpsalarm.app.service.CREATED_AT";
	public static final String POLL_ACTION="gpsalarm.app.service.POLL_ACTION";
	private static final String NOTIFY_KEYWORD="RemindMe@";
	private static final int INITIAL_POLL_PERIOD=60000; //60000
	private static final int POLL_PERIOD= 119907;//60000; //119907; //
	private Set<Tweet> updatedToSend=new HashSet<Tweet>();
	private Map<IPostListener, Account> accounts=
					new ConcurrentHashMap<IPostListener, Account>();
	private final Binder binder=new LocalBinder();
	private AtomicBoolean isBatteryLow=new AtomicBoolean(false);
	private AlarmManager alarm=null;
	private PendingIntent pi=null;
	SharedPreferences prefs=null;	
	
	private TimelineHelper sdb;
	private ReminderHelper rdb;
	
	Account myAccount = null;
	String team = null;
	Position curr = null;
	Position prev = null;
	
//	String urlToSendRequest = "http://192.168.0.194:8080/wsdbServiceWAR/tasks/-1/";
	String targetDomain = "211.30.144.192";
//	String targetDomain = "kalyankj.no-ip.org";  //"10.3.1.144"; //"192.168.0.153";
	int targetPort = 9998;
	
	public PostMonitor() {
		super("PostMonitor");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		sdb = new TimelineHelper(this);
		rdb = new ReminderHelper(this);
		
		//get user account from preference.
		prefs=PreferenceManager.getDefaultSharedPreferences(this);
		team = prefs.getString("team", "").toUpperCase();
		myAccount = new Account(prefs.getString("user", null), prefs.getString("password", ""), null);

		registerReceiver(onBatteryChanged,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));		
		alarm=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
		Intent i=new Intent(this, OnAlarmReceiver.class);
		pi=PendingIntent.getBroadcast(this, 0, i, 0);
		setAlarm(INITIAL_POLL_PERIOD);
	}

	private SharedPreferences.OnSharedPreferenceChangeListener prefListener=
		 new SharedPreferences.OnSharedPreferenceChangeListener() {

			public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {				
				team = prefs.getString("team", "").toUpperCase();
				myAccount = new Account(prefs.getString("user", null), prefs.getString("password", ""), null);
			}
		};
	
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
			myAccount = new Account(prefs.getString("user", null), prefs.getString("password", ""), null);
			if (myAccount.user != null) {
				pollForReminderUpdates(myAccount, team);
				pollStatusUpdates(myAccount);
//				pollForReminderUpdates(myAccount, "PUBLIC");
				updateLocation(myAccount);
				pollForProximityAlerts(myAccount);
			}
		}
		
		setAlarm(isBatteryLow.get() ? POLL_PERIOD*10 : POLL_PERIOD);
	}
	
	private void pollForProximityAlerts(Account myAccount2) {
		String baseURI = "http://"+targetDomain+":"+targetPort+"/wsdbServiceWAR/position";
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpHost targetHost = new HttpHost(targetDomain, targetPort, "http");
		Serializer s = new Persister();
		
		HttpGet httpget = null;
		String urlToSendRequest = baseURI+"/get/alerts";
		httpget = new HttpGet(urlToSendRequest);
		// Make sure the server knows what kind of a response we will accept
		httpget.addHeader("Accept", "application/xml");
		// Also be sure to tell the server what kind of content we are sending
		httpget.addHeader("Content-Type", "application/xml");

		try {
			// execute is a blocking call, it's best to call this code in a thread separate from the ui's
			HttpResponse response = httpClient.execute(targetHost, httpget);
			
			if (response.getStatusLine().getStatusCode() == 200) {
				HttpEntity en = response.getEntity();
				if (en != null) {
					String str = convertStreamToString(en.getContent());
					Alerts aList = s.read(Alerts.class, str);
					List<Alert> list = aList.getProperties();
					if (list == null ) return;
			    	Long now = Calendar.getInstance().getTime().getTime();
			    	boolean alertFlag = false;
			    	for (Alert a:list) {
			    		Reminder r = rdb.getReminderByGid(a.getGid().toString());
			    		if (r != null && a.getUsername() != null) {
			    			a.setTimestamp(now);
			    			if (rdb.updateAlerts(a)) alertFlag = true;
			    		}
					}
			    	rdb.markDeleteAlerts(now);
			    	if (alertFlag) showProximityNotification("there are "+rdb.getAlertsList("username").getCount()+" notifications");
				}

			}
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.toString(), e);
		} catch (IllegalStateException e) {
			Log.e(TAG, e.toString(), e);
		} catch (IOException e) {
			Log.e(TAG, e.toString(), e);
		} catch (Exception e) {
			Log.e(TAG, e.toString(), e);
		}
		finally {
			rdb.close();
		}
	}

	private void showProximityNotification(String string) {
		final NotificationManager mgr=
			(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Notification note=new Notification(R.drawable.status,"Friend@ notification!",System.currentTimeMillis());
		Intent i=new Intent(this, AlertList.class);
		
//		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
//							 Intent.FLAG_ACTIVITY_SINGLE_TOP);
		i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);		
		PendingIntent pi=PendingIntent.getActivity(this, 0,i,0);		
		note.setLatestEventInfo(this, "Friend@ notification",string,pi);
		
		mgr.notify(NOTIFICATION_ID, note);
		
	}

	private void updateLocation(Account myAccount2) {
		String baseURI = "http://"+targetDomain+":"+targetPort+"/wsdbServiceWAR/position";
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpHost targetHost = new HttpHost(targetDomain, targetPort, "http");
		Serializer s = new Persister();
		
		try
		{
			curr = rdb.getPosition(myAccount2.user);
			if (curr == null) return;
			if ((prev == null) || (curr.distance(prev) > 20)) {
				if (curr.getUsername() == null) return;
				String urlToSendRequest = baseURI+ "/new";
				// Using POST here
				HttpPut httpput = new HttpPut(urlToSendRequest);
				// Make sure the server knows what kind of a response we will accept
				httpput.addHeader("Accept", "text/xml");
				// Also be sure to tell the server what kind of content we are sending
				httpput.addHeader("Content-Type", "application/xml");

				StringEntity entity = new StringEntity(curr.getXML(), "UTF-8");
				entity.setContentType("application/xml");
				httpput.setEntity(entity);

				// execute is a blocking call, it's best to call this code in a thread separate from the ui's
				httpClient.execute(targetHost, httpput);
				prev = curr;
			}
					
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.toString(), ex);
		}
		finally {
			rdb.close();
		}
		
		//Who is near where
	}

	
	private void pollForReminderUpdates(Account account, String team) {
		
		String baseURI = "http://"+targetDomain+":"+targetPort+"/wsdbServiceWAR/tasks/";
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpHost targetHost = new HttpHost(targetDomain, targetPort, "http");
		Serializer s = new Persister();
		
		List<Reminder> rlist = new ArrayList<Reminder>();
		try
		{
			// INSERT in SERVER
			rlist = rdb.getReminders(" syncflag='I' and g_id is NULL ", null);
			if (rlist != null) {
				String urlToSendRequest = baseURI+ "new";
				HttpPut httpput = new HttpPut(urlToSendRequest);
				httpput.addHeader("Accept", "text/xml");
				httpput.addHeader("Content-Type", "application/xml");
				for (Reminder r : rlist) {
					if (r.getAuthor() == null) continue;
					StringEntity entity = new StringEntity(r.getXML(), "UTF-8");
					entity.setContentType("application/xml");
					httpput.setEntity(entity);

					// execute is a blocking call, it's best to call this code in a thread separate from the ui's
					HttpResponse response = httpClient.execute(targetHost, httpput);					
					if (response.getStatusLine().getStatusCode() == 201) {
						HttpEntity en = response.getEntity();
						if (en != null) {
							String str = convertStreamToString(en.getContent());
							StringTokenizer st = new StringTokenizer(str,"||");
							if (st.hasMoreTokens()) {
							Long g_id = Long.valueOf(st.nextToken());
							Long g_timestamp = null;
							if (st.hasMoreTokens()) g_timestamp = Long.valueOf(st.nextToken());
							r.setG_id(g_id);
							r.setG_timestamp(g_timestamp);
							r.setSyncflag("");
							rdb.updateWithRowid(r, r.getRowid().toString());							}
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			Log.e(TAG, ex.toString(), ex);
		}
		finally {
			rdb.close();
		}
		try {
			// UPDATE SERVER
			rlist = rdb.getReminders(" syncflag='E' ", null);
			HttpPost httppost = null;
			if (rlist != null) {
				for (Reminder r : rlist) {
					String urlToSendRequest = baseURI + r.getG_id() + "/";
					httppost = new HttpPost(urlToSendRequest);
					httppost.addHeader("Accept", "application/xml");
					httppost.addHeader("Content-Type", "application/xml");
					StringEntity entity = new StringEntity(r.getXML(), "UTF-8");
					entity.setContentType("application/xml");
					httppost.setEntity(entity);
					HttpResponse response = httpClient.execute(targetHost,httppost);

					if (response.getStatusLine().getStatusCode() == 204) {
						r.setSyncflag("");
						rdb.updateWithG_Id(r, r.getG_id().toString());
					}
				}
			}
		} catch (Exception ex) {
			Log.e(TAG, ex.toString(), ex);
		} finally {
			rdb.close();
		}
			
		try {
			// DELETE IN SERVER.
			// wants to remove the reminder.
			rlist = rdb.getReminders(" syncflag='D' ", null);
			HttpDelete httpdelete = null;
			if (rlist != null) {
				for (Reminder r : rlist) {
					String urlToSendRequest = baseURI + r.getG_id() + "/";
					httpdelete = new HttpDelete(urlToSendRequest);
					httpdelete.addHeader("Accept", "text/xml");
					httpdelete.addHeader("Content-Type", "application/xml");
					HttpResponse response = httpClient.execute(targetHost,
							httpdelete);

					if (response.getStatusLine().getStatusCode() == 204) {
						rdb.delete(r.getG_id().toString()); // After marking
															// server record
															// with 'D' remove
															// local record
					}
				}
			}
		} catch (Exception ex) {
			Log.e(TAG, ex.toString(), ex);
		} finally {
			rdb.close();
		}

		try {
			//GET FROM SERVER
			// rlist = rdb.getReminders(" syncflag='D' ", null);
			HttpGet httpget = null;
			String urlToSendRequest = baseURI + "recent?team=" + team
					+ "&author=" + account.user;
			httpget = new HttpGet(urlToSendRequest);
			httpget.addHeader("Accept", "text/xml");
			httpget.addHeader("Content-Type", "application/xml");
			HttpResponse response = httpClient.execute(targetHost, httpget);

			if (response.getStatusLine().getStatusCode() == 200) {
				HttpEntity en = response.getEntity();
				if (en != null) {
					String str = convertStreamToString(en.getContent());
					gpsalarm.app.datatype.Reminders rList = s.read(
							gpsalarm.app.datatype.Reminders.class, str);
					List<Reminder> list = rList.getProperties();
					if (list == null)
						return;
					for (Reminder r : list) {
						String syncType = r.getSyncflag();
						Reminder rlocal = rdb.getReminderByGid(r.getG_id().toString());
						if (syncType.equals("I") && rlocal == null) {
							r.setSyncflag("");
							rdb.insert(r);
						} else if (syncType.equals("E")
								&& rlocal != null) {
							if (rlocal.getG_timestamp() != r.getG_timestamp()) {
								r.setSyncflag("");
								rdb.updateWithG_Id(r, r.getG_id().toString());
							}
						} else if (syncType.equals("D")
								&& rlocal != null) {
							r.setSyncflag("");
							rdb.delete(r.getG_id().toString()); // server
																// instruction
																// to remove
																// record.
																// remove.
						}
					}
				}

			}
		} catch (Exception ex) {
			Log.e(TAG, ex.toString(), ex);
		} finally {
			rdb.close();
		}
	}

	private void setAlarm(long period) {
		alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
							SystemClock.elapsedRealtime()+period,
							pi);
	}
	
	private void pollStatusUpdates(Account l) {	
		String baseURI = "http://"+targetDomain+":"+targetPort+"/wsdbServiceWAR/tweet/";
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpHost targetHost = new HttpHost(targetDomain, targetPort, "http");
		Serializer s = new Persister();
		
		//Send tweets to Server
		try {
			if (!updatedToSend.isEmpty()) {
				String urlToSendRequest = baseURI + "new";
				// Using POST here
				HttpPut httpput = new HttpPut(urlToSendRequest);
				// Make sure the server knows what kind of a response we will accept
				httpput.addHeader("Accept", "text/xml");
				// Also be sure to tell the server what kind of content we are sending
				httpput.addHeader("Content-Type", "application/xml");
				for (Tweet t : updatedToSend) {
					StringEntity entity = new StringEntity(t.getXML(), "UTF-8");
					entity.setContentType("application/xml");
					httpput.setEntity(entity);

					// execute is a blocking call, it's best to call this code in a thread separate from the ui's
					HttpResponse response = httpClient.execute(targetHost, httpput);					
				}
				updatedToSend.clear();
			}
		}
		catch (Exception ex) {
			Log.e(TAG, ex.toString(), ex);
		} finally {
			rdb.close();
		}
		
		//receive tweets from server
		try {
			HttpGet httpget = null;
			String urlToSendRequest = baseURI + "get";
			httpget = new HttpGet(urlToSendRequest);
			httpget.addHeader("Accept", "application/xml");
			httpget.addHeader("Content-Type", "application/xml");
			HttpResponse response = httpClient.execute(targetHost, httpget);
			String gid = null;
			String msg = null;
			if (response.getStatusLine().getStatusCode() == 200) {
				HttpEntity en = response.getEntity();
				if (en != null) {
					String str = convertStreamToString(en.getContent());
					gpsalarm.app.datatype.Tweets rList = s.read(
							gpsalarm.app.datatype.Tweets.class, str);
					List<Tweet> list = rList.getProperties();
					if (list == null) return;
					boolean updatedFlag = false;
					for (Tweet t : list) {
						if (!sdb.contains(t.getRid().toString())) {
							if (rdb.getReminderByGid(t.getGid()) != null) {
								updatedFlag = true;				
								sdb.insert(t);	
								gid = t.getGid();
								msg = t.getStatus();
							}

						}
					}
					if (updatedFlag) showNotification(msg, gid);
				}
			}
		} catch (Exception ex) {
			Log.e(TAG, ex.toString(), ex);
		} finally {
			rdb.close();
		}
	}


    private String convertStreamToString (InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8*1024);
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(TAG, e.toString(),e);
            }
        }
        
        return sb.toString();

    }

	private void showNotification(String str, String gid) {
		final NotificationManager mgr=
			(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Notification note=new Notification(R.drawable.status,"Locate@ Tweet",System.currentTimeMillis());
		Intent i=new Intent(this, AlarmEdit3.class);
		i.putExtra("user", prefs.getString("user", null));
		i.putExtra(ReminderHelper.GLOBAL_ID, gid);
//		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
//							 Intent.FLAG_ACTIVITY_SINGLE_TOP);
		i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		PendingIntent pi=PendingIntent.getActivity(this, 0,i,0);
		
		note.setLatestEventInfo(this, "Locate@ Tweet",str,pi);
		
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
	
	class Account {
		String user=null;
		String password=null;
		IPostListener callback=null;
		
		Account(String user, String password,
						 IPostListener callback) {
			this.user=user;
			this.password=password;
			this.callback=callback;
		}
	}
	
	public class LocalBinder extends Binder implements IPostMonitor {
		public void registerAccount(String user, String password,IPostListener callback) {
			
			Account l=new Account(user, password, callback);			
			pollStatusUpdates(l);
			accounts.put(callback, l);
		}
		
		public void removeAccount(IPostListener callback) {
			accounts.remove(callback);
		}
		public void updateStatus(String taskid, String author, String status){
			Long now = Calendar.getInstance().getTime().getTime();
			Tweet p = new Tweet();
			p.setFriend(author);
			p.setGid(taskid);
			p.setStatus(status);
			p.setCreated(now);
			updatedToSend.add(p);
		}

		@Override
		public void invokeSync() {
			myAccount = new Account(prefs.getString("user", null), prefs.getString("password", ""), null);
			if (myAccount.user != null) {
				pollForReminderUpdates(myAccount, team);
				pollStatusUpdates(myAccount);
//				pollForReminderUpdates(myAccount, "PUBLIC");
				updateLocation(myAccount);
				pollForProximityAlerts(myAccount);
			}			
			Log.d("PostMonitor", "Sync Manually invoked");
			
		}
	}
}
