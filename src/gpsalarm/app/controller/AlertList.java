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
import java.text.SimpleDateFormat;

import gpsalarm.app.R;
import gpsalarm.app.data.ReminderHelper;
import gpsalarm.app.datatype.Reminder;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AlertList extends ListActivity {
	static SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm");
	private ReminderHelper db;
	private AlertAdapter adapter;
	SharedPreferences prefs=null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//OpenDatabase connection
		db = new ReminderHelper(this);
//		prefs=PreferenceManager.getDefaultSharedPreferences(this);
		

//		String orderBy = prefs.getString("sort_order", "created");
		// Get all of the rows from the database and create the item list
        setListAdapter("username");
	}


	private void setListAdapter(String str) {
        String where = null;
		Cursor c = db.getAlertsList(str);
		startManagingCursor(c);
		//create an adapter using the dataabse to manage the data .
		adapter=new AlertAdapter(c);
		//Set this adapter as the list's data source. This class is a ListActivity class.
		setListAdapter(adapter);
	}

	
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
       
        Reminder r = this.db.getReminderByGid(Long.toString(id));
        Log.d("AlertList", "selected item gid=" + id + ": ");
        
		Bundle bundle = new Bundle();
		bundle.putString(ReminderHelper.ROWID, r.getRowid().toString());

		Intent mIntent = new Intent();
		mIntent.putExtras(bundle);
		setResult(RESULT_OK, mIntent);
		finish();
    } 

	//This is for the menu options when standard menu is invoked.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(this).inflate(R.menu.alert_menu, menu);

		return(super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case R.id.friends_distance:
            	this.setListAdapter("username");
            	return true;
            case R.id.tasks_near_friends:
            	this.setListAdapter("gid");
            	return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
    
	class AlertAdapter extends CursorAdapter {		
		AlertAdapter(Cursor c) {
			super(AlertList.this, c);
		}
		
		
		//when record is added executed next while rendering (2)
		// still not clear.
		//when search was selected, only bind view was executed
		@Override
		public void bindView(View row, Context ctxt, Cursor c) {
			
			//Holder links a row view to data in the database table.
			//Not sure what the row.getTag() is. best to follow as is.
			AlertHolder holder=(AlertHolder)row.getTag();
			
			//populates data identififed by cursor into view.
			holder.populateFrom(c);
		}
		
		//when record is added, executed (1)
		//executed once for every record when view is being rendered.
		//it connects the view shell and the row
		@Override
		public View newView(Context ctxt, Cursor c,
												 ViewGroup parent) {
			
			//this looks like adding a new row.
			LayoutInflater inflater=getLayoutInflater();
			View row=inflater.inflate(R.layout.notes_row, parent, false);
			AlertHolder holder=new AlertHolder(row);
			
			row.setTag(holder);
			
			return(row);
		}
	}
    
	static class AlertHolder {
		private TextView title=null;
		private TextView details1=null;
		private TextView details2=null;
		private ImageView icon=null;
		private View row=null;
		
		AlertHolder(View row) {
			this.row=row;
			
			title=(TextView)row.findViewById(R.id.title);
			details1=(TextView)row.findViewById(R.id.details1);
			details2=(TextView)row.findViewById(R.id.details2);
			icon=(ImageView)row.findViewById(R.id.icon);
		}
		
		void populateFrom(Cursor c) {
//			title.setText(helper.getTitle(c));
			title.setText(c.getString(1)+" is "+ distance(c.getInt(3))+ " from "+c.getString(2));
			details1.setText("- "+dateFormat.format(c.getLong(4)));
//			details2.setText("id:"+helper.getRowid(c)+";gid:"+helper.getG_id(c)+"; sync:"+helper.getSyncflag(c)+";"+helper.getState(c));
//			if (helper.getPriority(c) == null) {
//				
//			}
//			else if (helper.getPriority(c).equals("required")) {
//				icon.setImageResource(R.drawable.ball_red);
//			}
//			else if (helper.getPriority(c).equals("optional")) {
//				icon.setImageResource(R.drawable.ball_yellow);
//			}
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
	}
}
