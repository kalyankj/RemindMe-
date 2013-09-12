package gpsalarm.app.controller;
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
import gpsalarm.app.R;
import gpsalarm.app.data.ReminderHelper;
import gpsalarm.app.data.TimelineHelper;
import gpsalarm.app.datatype.Reminder;
import gpsalarm.app.service.PostMonitor;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import gpsalarm.app.IPostMonitor;

public class AlarmEdit3 extends Activity {
	protected static final String TAG = AlarmEdit3.class.getSimpleName();
	static SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm");
	String mlat;
	String mlng;
	static TextView author;
	TextView state;
	TextView nearest;
	TextView team;
	TextView created;
	EditText modified;
	TextView title=null;
	TextView body=null;
	TextView validtill=null;
	TextView priority_text;
	String priority = null;
	
	ReminderHelper rdb=null;
	private TimelineHelper sdb;
	
	static String g_id=null;
	private Date created_date;
	private Date modified_date;
	private static Date valid_date;
    static int year, month, day;
    static final int DATE_DIALOG_ID = 0;
    private String username = null;
    //patchy Code
	private EditText statusTxt=null;
	private IPostMonitor service=null;	
	private TimelineAdapter adapter=null;
	private Pattern regexLocation=Pattern.compile("L\\:((\\-)?[0-9]+(\\.[0-9]+)?)\\,((\\-)?[0-9]+(\\.[0-9]+)?)");
	private View statusRow=null;
	private Animation fadeOut=null;
	private Animation fadeIn=null;	
	private Reminder r = null;
	private ServiceConnection svcConn=new ServiceConnection() {
		public void onServiceConnected(ComponentName className,IBinder binder) {
			service=(IPostMonitor) binder;
			
			try {
//				service.registerAccount(AlarmEdit3.TWEET_USERNAME,AlarmEdit3.PASSWORD,
//																listener);
			}
			catch (Throwable t) {
				Log.e(TAG, "Exception in call to registerAccount()", t);
				goBlooey(t);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			service=null;
		}
	};	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		//Initialise all the widegets
		rdb = new ReminderHelper(this);
		sdb = new TimelineHelper(this);
		
		setContentView(R.layout.note_edit3);	
		author = (TextView) findViewById(R.id.author);	
		state = (TextView) findViewById(R.id.state);		
		team = (TextView) findViewById(R.id.team);	
		title=(TextView)findViewById(R.id.title);
		body=(TextView)findViewById(R.id.body);
		validtill=(TextView)findViewById(R.id.validtill);
		priority_text=(TextView)findViewById(R.id.priority);
		created=(TextView) findViewById(R.id.created);
		modified=(EditText) findViewById(R.id.modified);	
		nearest= (TextView) findViewById(R.id.nearest);
		g_id=getIntent().getStringExtra(ReminderHelper.GLOBAL_ID);
		mlat = getIntent().getStringExtra(ReminderHelper.LATITUDE);
		mlng = getIntent().getStringExtra(ReminderHelper.LONGITUDE);		
		username = getIntent().getStringExtra("user");
		if (g_id!=null) load();
		
		// For status updating
		statusTxt=(EditText)findViewById(R.id.status);		
		Button send=(Button)findViewById(R.id.send);		
		send.setOnClickListener(onSend);
				
		//For viewing timeline
		ListView list=(ListView)findViewById(R.id.posts);
		Cursor c = sdb.getTimeline(" gid = "+g_id);
		adapter=new TimelineAdapter(c);		
		list.setAdapter(adapter);		
		list.setOnItemClickListener(onStatusClick);
		findViewById(R.id.stop).setOnClickListener(onStop);
		findViewById(R.id.snooze).setOnClickListener(onSnooze);
		findViewById(R.id.tweet).setOnClickListener(onTweet);
		
		//set default status
		findViewById(R.id.timelineform).setVisibility(View.VISIBLE);
		findViewById(R.id.tweetform).setVisibility(View.INVISIBLE);		
		clearNotification();
		
		registerReceiver(receiver, new IntentFilter(PostMonitor.STATUS_UPDATE));
		bindService(new Intent(this.getApplicationContext(), PostMonitor.class), svcConn, BIND_AUTO_CREATE);

//		statusRow=findViewById(R.id.status_row);
//		fadeOut=AnimationUtils.loadAnimation(this, R.anim.fade_out);
//		fadeOut.setAnimationListener(fadeOutListener);
//		fadeIn=AnimationUtils.loadAnimation(this, R.anim.fade_in);
		
		if (Intent.ACTION_GET_CONTENT.equals(getIntent().getAction())) {
//			save.setVisibility(Button.INVISIBLE);
//			LinearLayout ll = (LinearLayout) findViewById(R.id.alert);
//			ll.setVisibility(LinearLayout.VISIBLE);
			
		}
	}

	private void load() {
		r=rdb.getReminderByGid(g_id);
		mlat = r.getLatitude().toString();
		mlng = r.getLongitude().toString();
		title.setText(r.getTitle());
		body.setText(r.getDetail());
		state.setText(r.getState());
		priority = r.getPriority();
		priority_text.setText(priority);
		author.setText(r.getAuthor());
		team.setText(r.getTeam());
		created_date = new Date(r.getCreated());
		modified_date = new Date(r.getModified());
		valid_date = new Date(r.getValidTill());		
		created.setText(dateFormat.format(created_date));
		modified.setText(dateFormat.format(modified_date));
		validtill.setText(dateFormat.format(valid_date));
		nearest.setText("Was within "+distance(r.getNearest())+" on "+ dateFormat.format(r.getNearestOn()));
	}

	private String distance(int d) {		
		String dist = "";
		if (d < 1000)
			dist = ""+d+"m ";
		else if (d < 100000) {
			int dis = (int) Math.round (d / 100.0);
			dist = ""+dis/10+"km ";
		}
		else {
			int dis = (int) Math.round (d / 10000.0);
			dist = ""+dis*10+"km ";				
		}
		return dist;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//remove connection to PostMonitor
//		service.removeAccount(listener);
		unbindService(svcConn);
		unregisterReceiver(receiver);
		rdb.close();
	}
	
	@Override
	public void onSaveInstanceState(Bundle saved) {
		super.onSaveInstanceState(saved);
		saved.putString(ReminderHelper.GLOBAL_ID, g_id);
	}

	@Override
	public void onRestoreInstanceState(Bundle saved) {
		super.onRestoreInstanceState(saved);		
		g_id = saved.getString(ReminderHelper.GLOBAL_ID);
		if (g_id!=null) load();
	}

	private OnClickListener onStop= new View.OnClickListener() {		
		@Override
		public void onClick(View v) {
			Long now = Calendar.getInstance().getTime().getTime();
	        r.setModified(now);
			r.setState("A2");
//	        r.setSyncflag("");
			rdb.updateWithG_Id(r, g_id);
			Toast.makeText(AlarmEdit3.this, "Alarm Stopped", Toast.LENGTH_SHORT).show();
			finish();
		}
	};
	
	private OnClickListener onSnooze= new View.OnClickListener() {		
		@Override
		public void onClick(View v) {
			Long now = Calendar.getInstance().getTime().getTime();
	        r.setModified(now);
			r.setState("A1");
			rdb.updateWithG_Id(r, g_id);
			Toast.makeText(AlarmEdit3.this, "Snoozed 1 day", Toast.LENGTH_SHORT).show();
			finish();
		}
	};

	private OnClickListener onTweet= new View.OnClickListener() {		
		@Override
		public void onClick(View v) {
			findViewById(R.id.timelineform).setVisibility(View.INVISIBLE);
			findViewById(R.id.tweetform).setVisibility(View.VISIBLE);	
		}
	};
        ///////////////////////////////////////////
        // code from patchy
	@Override
	public void onNewIntent(Intent i) {
		clearNotification();
	}
	
	private void toggleStatusEntry() {
		if (statusRow.getVisibility()==View.VISIBLE) {
			statusRow.startAnimation(fadeOut);
		}
		else {
			statusRow.setVisibility(View.VISIBLE);
			statusRow.startAnimation(fadeIn);
		}
	}
    	private View.OnClickListener onSend=new View.OnClickListener() {
    		public void onClick(View v) {
    			updateStatus();
    			findViewById(R.id.timelineform).setVisibility(View.VISIBLE);
    			findViewById(R.id.tweetform).setVisibility(View.INVISIBLE);	
    			Toast.makeText(AlarmEdit3.this, "Tweet sent to Server for distribution", Toast.LENGTH_LONG).show();
    		}
    	};        

    	
    	private void updateStatus() {
    		service.updateStatus(g_id,username, statusTxt.getText().toString());
    	} 

    	
    	private void goBlooey(Throwable t) {
    		AlertDialog.Builder builder=new AlertDialog.Builder(this);
    		
    		builder
    			.setTitle("Exception!")
    			.setMessage(t.toString())
    			.setPositiveButton("OK", null)
    			.show();
    	}
   	
    	

    	private BroadcastReceiver receiver=new BroadcastReceiver() {
    		@Override
			public void onReceive(Context context,final Intent intent) {
    			adapter.getCursor().requery();
    		}
    	};    	
 

	private AdapterView.OnItemClickListener onStatusClick = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Matcher r = regexLocation.matcher(sdb.getStatus(adapter.getCursor()));

			if (r.find()) {
				double latitude = Double.valueOf(r.group(1));
				double longitude = Double.valueOf(r.group(4));
				
				Toast.makeText(AlarmEdit3.this, "This is aonStatusClick msg. to be programmed", Toast.LENGTH_SHORT);
//				Intent i = new Intent(Patchy.this, StatusMap.class);
//
//				i.putExtra(LATITUDE, latitude);
//				i.putExtra(LONGITUDE, longitude);
//				i.putExtra(STATUS_TEXT, entry.status);
//
//				startActivity(i);
			}
		}
	};
    	
	private void clearNotification() {
		NotificationManager mgr=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);			
		mgr.cancel(PostMonitor.NOTIFICATION_ID);
	}
	   	    	
    	
    	class TimelineAdapter extends CursorAdapter {		

			TimelineAdapter(Cursor c) {
    			super(AlarmEdit3.this, c);
    		}
    		
    		@Override
    		public void bindView(View row, Context ctxt, Cursor c) {
    			StatusHolder holder=(StatusHolder)row.getTag();
    			holder.populateFrom(c, sdb);
    		}
    		
    		@Override
    		public View newView(Context ctxt, Cursor c, ViewGroup parent) {   			
    			LayoutInflater inflater=getLayoutInflater();
    			View row=inflater.inflate(R.layout.post_row, parent, false);
    			StatusHolder holder=new StatusHolder(row);
    			
    			row.setTag(holder);
    			
    			return(row);
    		}

    	}
       
    	static class StatusHolder {
    		private TextView friend= null;
//    		private TextView createdAt=null;
    		private TextView status=null;
    		private View row=null;
    		
    		StatusHolder(View row) {
    			this.row=row;
    			friend=(TextView)row.findViewById(R.id.friend);
//    			createdAt=(TextView)row.findViewById(R.id.created_at);
    			status=(TextView)row.findViewById(R.id.status);
    		}
    		
    		void populateFrom(Cursor c, TimelineHelper helper) {
    			status.setText(helper.getStatus(c)+" - "+helper.getFriend(c)+" "+dateFormat.format(helper.getCreated(c)));
//    			friend.setText("Says: "+helper.getFriend(c)+" "+dateFormat.format(helper.getCreated(c)));
//    			createdAt.setText());
    		}

    	}        
}
