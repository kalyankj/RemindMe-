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


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import gpsalarm.app.ILocationParser;
import gpsalarm.app.IPostMonitor;
import gpsalarm.app.data.ReminderHelper;
import gpsalarm.app.datatype.Reminder;
import gpsalarm.app.datatype.ReminderLayer3;
import gpsalarm.app.service.PostMonitor;
import gpsalarm.app.service.Remote_PositionService;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class MainActivity2 extends MapActivity {
    private static final int ACTIVITY_CREATE=0;
	private static final int ACTIVITY_EDIT = 1;
	private static final int ACTIVITY_LIST = 2;
	protected static final String TAG = MainActivity2.class.getSimpleName();
	
	Cursor model=null;
	SharedPreferences prefs=null;
	
	private MapView mapView;
	private ReminderHelper db;
	AtomicBoolean isActive=new AtomicBoolean(true);
	private ReminderLayer3 pinLayer;
	private Reminder selectedReminder;
	MyGeocoder mycoder;

	protected Location gpsLocation;
	private MyLocationOverlay gps;
	private LocationManager locMgr = null;
	myLocation myloc = null;
	private IPostMonitor service=null;
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
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainlayout); 
        
        //Set up the Map Display
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.getController().setZoom(14);
		mapView.setBuiltInZoomControls(true);
		mapView.getZoomButtonsController().setAutoDismissed(false);
		mapView.setLongClickable(true);	
				
    	//Register Context menu 
		prefs=PreferenceManager.getDefaultSharedPreferences(this);
		db = new ReminderHelper(this);
		mycoder = new MyGeocoder(this);
		
		//do database cleanup control as necessary
        db.cleanup();
		
		//Add overlay layer
		pinLayer = new ReminderLayer3(db, getResources().getDrawable(R.drawable.yellowpin),
				getResources().getDrawable(R.drawable.shadow)); //MapLocationOverlay overlay;
		mapView.getOverlays().add(pinLayer);
//		mapView.getController().setCenter(pinLayer.getCenter());
		
		//register periphery functions
	    prefs.registerOnSharedPreferenceChangeListener(prefListener);
		registerForContextMenu(mapView);
		
//		//start remote monitoring service
        Intent intent = new Intent();
        intent.setClass(this, Remote_PositionService.class);
        startService(intent);
        
		bindService(new Intent(this.getApplicationContext(), PostMonitor.class), svcConn, BIND_AUTO_CREATE);

		if (prefs.getString("user", null) == null || prefs.getString("team", null) == null) {
    		setpreference("You will first need to set username and team");
    	}
	}
	
	private SharedPreferences.OnSharedPreferenceChangeListener prefListener=
		 new SharedPreferences.OnSharedPreferenceChangeListener() {
			private String username;
			private String password;

			public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {
//				if (key.equals("sort_order")) {
//					loadList();
//				}				
//				if (key.equals("user")) username = prefs.getString("user", "public");
//				if (key.equals("password")) password = prefs.getString("password", "password1");
			}
		};
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		selectedReminder = this.pinLayer.getSelectedReminder();	
		GeoPoint p = this.pinLayer.getSelectPoint();
		if (selectedReminder != null) {
		    MenuInflater inflater = getMenuInflater();
		    inflater.inflate(R.menu.contextmenu, menu);				
		}
//		GeoPoint p = ((MapView) mView).getProjection().fromPixels((int) event.getX(), (int) event.getY());
		if ( p != null) {
				String user = prefs.getString("user", null);
				String team = prefs.getString("team", null);
				if (user == null || team == null) {
	            	setpreference("First set username and team");
				}
				else {
		        	Toast.makeText(mapView.getContext(), "Adding a New Reminder here. wait", Toast.LENGTH_SHORT).show();
					Intent i=new Intent(mapView.getContext(), AlarmEdit2.class);
					i.putExtra(ReminderHelper.LATITUDE, String.valueOf(p.getLatitudeE6()));
					i.putExtra(ReminderHelper.LONGITUDE, String.valueOf(p.getLongitudeE6()));
					i.putExtra(ReminderHelper.AUTHOR, user);  //person operating is the author.
					this.pinLayer.setSelectPoint(null);
					startActivityForResult(i, ACTIVITY_CREATE);
				}
		}
	}
	
	private void setpreference(String str) {
        	startActivity(new Intent(this, EditPreferences.class));
        	Toast.makeText(mapView.getContext(), str, Toast.LENGTH_LONG).show();        	

	}
	
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.EDIT_ID:
            	//Present item in AlarmEdit for for updating.
            	if (prefs.getString("user", null) == null || prefs.getString("team", null) == null) {
            		setpreference("First set username and team");
            		return true;
            	}
            	if (selectedReminder.getG_id() == 0) {
                	Toast.makeText(mapView.getContext(), "Server need to be Synced before you can tweet. Try after sometime.", Toast.LENGTH_SHORT).show();
                	return true;
            	}
    			Intent i=new Intent(MainActivity2.this, AlarmEdit2.class);
    			i.putExtra(ReminderHelper.ROWID, String.valueOf(selectedReminder.getRowid()));
    			i.putExtra(ReminderHelper.LONGITUDE, String.valueOf(selectedReminder.getPoint().getLatitudeE6()));
    			i.putExtra(ReminderHelper.LATITUDE, String.valueOf(selectedReminder.getPoint().getLongitudeE6()));
    			startActivityForResult(i, ACTIVITY_EDIT);
                return true;
            case R.id.TWEET_ID:
            	if (prefs.getString("user", null) == null || prefs.getString("team", null) == null) {
            		setpreference("You need to set username and team first");
            		return true;
            	}
            	if (selectedReminder.getG_id() == 0) {
                	Toast.makeText(mapView.getContext(), "Server need to be Synced before you can tweet. Try after sometime.", Toast.LENGTH_SHORT).show();
                	return true;
            	}
    			Intent j=new Intent(MainActivity2.this, AlarmEdit3.class);
    			j.putExtra(ReminderHelper.GLOBAL_ID, String.valueOf(selectedReminder.getG_id()));
    			j.putExtra(ReminderHelper.LONGITUDE, String.valueOf(selectedReminder.getPoint().getLatitudeE6()));
    			j.putExtra(ReminderHelper.LATITUDE, String.valueOf(selectedReminder.getPoint().getLongitudeE6()));
    			j.putExtra("user", prefs.getString("user", null));
    			startActivityForResult(j, ACTIVITY_EDIT);
                return true;
            case R.id.DELETE_ID:
            	if (prefs.getString("user", null) == null || prefs.getString("team", null) == null) {
            		setpreference("First set username and team");
            		return true;
            	}
            	//Delete the selected pin identified by the g_id
            	Toast.makeText(mapView.getContext(), "Deleting selected reminder"+selectedReminder.getTitle(), Toast.LENGTH_SHORT).show();
            	pinLayer.removeReminderPin(selectedReminder, prefs.getString("user", null));
                mapView.postInvalidate(); //postInvalidate();
                return true;                
        }
        return super.onContextItemSelected(item);
    }	
	
	//This is for the menu options when standard menu is invoked.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(this).inflate(R.menu.menu, menu);

		return(super.onCreateOptionsMenu(menu));
    }	

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case R.id.search:
//    			onSearchRequested();
    			LinearLayout ll = (LinearLayout) findViewById(R.id.geocoder);
    			ll.setVisibility(View.VISIBLE);
    			mycoder.geoBtn = (ImageButton) findViewById(R.id.geocodeBtn);
//    			geocoder = new Geocoder(this);
    			mycoder.loc = (AutoCompleteTextView) findViewById(R.id.location);
				mycoder.loc.setOnTouchListener(searchTouchListener);
				mycoder.loc.addTextChangedListener(txtWatcher);
				mycoder.loc.setAdapter(new ArrayAdapter<Address>(this, android.R.layout.simple_dropdown_item_1line, mycoder.addressList));
				mycoder.geoBtn.setOnClickListener(searchClickListener);
            	return true;
            case R.id.prefs:
            	startActivity(new Intent(this, EditPreferences.class));
            	return true;
            case R.id.mylocation_id:
            	enableGPS();       		
                return true;
            case R.id.List_id:
            	Intent rList = new Intent(this, ReminderListActivity2.class);
//            	startActivity(rList); 
    			startActivityForResult(rList, ACTIVITY_LIST);
                return true;
            case R.id.List_alert_id:
            	Intent rList3 = new Intent(this, AlertList.class);
//            	startActivity(rList); 
    			startActivityForResult(rList3, ACTIVITY_LIST);
                return true;
            case R.id.List_debug_id:
            	Intent rList2 = new Intent(this, ReminderDebugger.class);
//            	startActivity(rList); 
    			startActivityForResult(rList2, ACTIVITY_LIST);
                return true;
            case R.id.List_contact_id:
            	Intent rList4 = new Intent(this, ContactsList.class);
//            	startActivity(rList); 
    			startActivityForResult(rList4, ACTIVITY_LIST);
                return true;
            case R.id.sync_id:
            	service.invokeSync();
                return true;
            case R.id.help:
            	startActivity(new Intent(this, gpsalarm.app.utility.HelpPage.class));
                return true;  
            case R.id.Exit:
               	// unbind Service and end background Serive.
                // Detach our existing connection.
        		AlertDialog alertDialog = new AlertDialog.Builder(mapView.getContext()).create();
        		alertDialog.setMessage("Remove backgroud GPS Observation ?");
        		alertDialog.setButton("Yes", new DialogInterface.OnClickListener() {
            			public void onClick(DialogInterface dialog, int which) {
            				stopBackgroundService();
//                            mDbHelper.close();  //Close database
                            finish();
        			}
        		});
        		alertDialog.setButton2("No",
        				new DialogInterface.OnClickListener() {
        					public void onClick(DialogInterface dialog, int which) {   
//        		                mDbHelper.close();  //Close database
        		                finish();
        					}
        				});
        		alertDialog.show();
				try { finalize();
				} catch (Throwable e) {
					e.printStackTrace();}
                return true;                
        }

        return super.onMenuItemSelected(featureId, item);
    }       
 
    private void stopBackgroundService() {
    	stopService(new Intent(this, Remote_PositionService.class));
    }
    
    public TextWatcher txtWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
			
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start,
				int count, int after) {
			
		}

		@Override
		public void onTextChanged(CharSequence s, int start,
				int before, int count) {
			
		}
    };
    
	private void enableGPS() {
		if (gps == null) {
			gps = new MyLocationOverlay(this, mapView);
			mapView.getOverlays().add(gps);
		}
		if (!gps.isMyLocationEnabled()) {
			myloc = new myLocation();
			myloc.registerPositionSensor();
			
			gps.enableMyLocation();
		}
		else {
			gps.disableMyLocation();
			myloc.removeUpdates();
		}
	}    
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		switch (requestCode) {
		case ACTIVITY_CREATE:
			if (intent != null) {
				Bundle extras = intent.getExtras();
				String rowId = extras.getString(ReminderHelper.ROWID);
				selectedReminder = db.getReminderByRowid(rowId);
//				if (Integer.valueOf(rowid) > 20  ) {
//					db.delete(selectedReminder);
//					Dialog notify = onCreateDialog(0);
//					notify.show();
//					break;
//				}
				mapView.getController().setCenter(selectedReminder.getPoint());
				this.pinLayer.setSelectedReminder(null);
				mapView.postInvalidate();
			}
			break;				
		case ACTIVITY_EDIT:
			mapView.getController().setCenter(selectedReminder.getPoint());
			this.pinLayer.setSelectedReminder(null);
			mapView.postInvalidate();
//			this.pinLayer.setSelectedPin(null);
			break;
		case ACTIVITY_LIST:
			if (intent != null) {
				Bundle extras = intent.getExtras();
				String rowId = extras.getString(ReminderHelper.ROWID);
				selectedReminder = db.getReminderByRowid(rowId);
				this.pinLayer.setSelectedReminder(selectedReminder);
				mapView.getController().animateTo(selectedReminder.getPoint());
			}
			break;
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		isActive.set(false);
	}	

	@Override
	public void onDestroy() {
		super.onDestroy();
//		unbindService(svcConn);
		isActive.set(false);
	}	
	
	@Override
	public void onResume() {
		super.onResume();
		isActive.set(true);
	}	
	
	public void test1() {
		Intent i=new Intent(MainActivity2.this, AlarmEdit2.class);
		i.putExtra(ReminderHelper.LONGITUDE, String.valueOf(15596000));
		i.putExtra(ReminderHelper.LATITUDE, String.valueOf(-33840029));
		startActivity(i);
	}
	
	public void test2() {
		int id = 1;
		Intent i=new Intent(MainActivity2.this, AlarmEdit2.class);
		i.putExtra(ReminderHelper.ROWID, String.valueOf(id));
		i.putExtra(ReminderHelper.LONGITUDE, String.valueOf(15596));
		i.putExtra(ReminderHelper.LATITUDE, String.valueOf(-33840));
		startActivity(i);
	}
	
	public OnTouchListener searchTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (mycoder.loc.didTouchFocusSelect()) mycoder.loc.setText("");
			return false;
		}		
	};	
	
	public OnClickListener searchClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			InputMethodManager mgr=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
			mycoder.loc = (AutoCompleteTextView) findViewById(R.id.location);
			mgr.hideSoftInputFromWindow(mycoder.loc.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
			String locationName = mycoder.loc.getText().toString();
			mycoder.progDialog = ProgressDialog.show(mapView.getContext(), "Processing...",
							"Finding Location...", true, true);
			mycoder.findLocation(locationName);
		}
	};
	
	public class MyGeocoder {
		public ImageButton geoBtn;
		public AutoCompleteTextView loc;
		
		Geocoder geocoder;
		public List<Address> addressList = new ArrayList<Address>();
		public Dialog progDialog;

		public MyGeocoder(Context context) {
			super();
			geocoder = new Geocoder(context);
		}
		
		public void findLocation(final String locationName) {
			Thread thrd = new Thread() {

				@Override
				public void run() {
					try {
						// do backgrond work
						List<Address> curr = geocoder.getFromLocationName(locationName, 5);
						addressList.addAll(curr);						// send message to handler to process results
						uiCallback.sendEmptyMessage(0);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			thrd.start();
		}

		// ui thread callback handler
		private Handler uiCallback = new Handler() {
			
			@Override
			public void handleMessage(Message msg) {
				progDialog.dismiss();
				if (addressList != null && addressList.size() > 0) {
					int lat = (int) (addressList.get(0).getLatitude() * 1000000);
					int lng = (int) (addressList.get(0).getLongitude() * 1000000);
					GeoPoint p = new GeoPoint(lat, lng);
//					mapView.getController().setZoom(15);
					mapView.getController().setCenter(p);
//					handleCreateReminder();
				} else {
					Dialog foundNothingDlg = new AlertDialog.Builder(mapView
							.getContext()).setIcon(0).setTitle(
							"Failed to Find Location")
							.setPositiveButton("Ok", null).setMessage(
									"Location Not Found...").create();
					foundNothingDlg.show();
				}
				LinearLayout ll = (LinearLayout) findViewById(R.id.geocoder);
				ll.setVisibility(View.INVISIBLE);
				super.handleMessage(msg);
			}
		};
	}
	
	class myLocation {
		protected void registerPositionSensor() {
			Log.d(TAG, "Executing registerPositionSensor");
			if (locMgr == null) {
				locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			}
			if (locMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 20, onLocationChange);
			}
			else if (locMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				locMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 20, onLocationChange);
			}
		}
		
		public void removeUpdates() {
			locMgr.removeUpdates(onLocationChange);
			
		}

		private LocationListener onLocationChange=new LocationListener() {
		public void onLocationChanged(Location location) {
			double lat = location.getLatitude();
			double lng = location.getLongitude();
			GeoPoint myLocation = new GeoPoint((int) (lat * 1000000),(int) (lng * 1000000));
			mapView.getController().animateTo(myLocation);
		}
		public void onProviderDisabled(String provider) {
			// required for interface, not used
		}
		public void onProviderEnabled(String provider) {
			// required for interface, not used
		}
		public void onStatusChanged(String provider, int status,
																 Bundle extras) {
			// required for interface, not used
		}
	};
	}

	private void goBlooey(Throwable t) {
		AlertDialog.Builder builder=new AlertDialog.Builder(this);
		
		builder
			.setTitle("Exception!")
			.setMessage(t.toString())
			.setPositiveButton("OK", null)
			.show();
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////
}