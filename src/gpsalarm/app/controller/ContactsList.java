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
import java.util.ArrayList;
import java.util.List;

import gpsalarm.app.R;
import gpsalarm.app.data.ReminderHelper;
import gpsalarm.app.datatype.Reminder;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ContactsList extends ListActivity {
	static SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm");
	private ReminderHelper db;
	private Cursor c;
	private SimpleCursorAdapter adapter;
	SharedPreferences prefs=null;
	private static final String[] PROJECTION=new String[] {Contacts._ID,Contacts.DISPLAY_NAME,Email.DATA};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//OpenDatabase connection
		prefs=PreferenceManager.getDefaultSharedPreferences(this);
		String friends = prefs.getString("friends", "");
		String ids = prefs.getString("friends", "");
      	Toast.makeText(this, "contactlist-init:"+friends, Toast.LENGTH_LONG).show();
	    //c = getContentResolver().query(Phones.CONTENT_URI, null, null, null, null);
        c=managedQuery(Email.CONTENT_URI,PROJECTION,null,null, null);
		startManagingCursor(c);
		//create an adapter using the dataabse to manage the data .
		adapter=new SimpleCursorAdapter(	this,
				android.R.layout.simple_list_item_multiple_choice,
				c,
				new String[] {
					Contacts.DISPLAY_NAME,
					Email.DATA
				},
				new int[] {
					android.R.id.text1,
					android.R.id.text2
				});
		//Set this adapter as the list's data source. This class is a ListActivity class.
		setListAdapter(adapter);
		this.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	}

	@Override
	protected void onDestroy() {
//		if (!c.isClosed()) SaveSelection();
		super.onDestroy();
	}

	private void SaveSelection() {		
		String friends = "";
		String ids = "";
		SparseBooleanArray a = this.getListView().getCheckedItemPositions();
		if (a.size() < 1 ) return;
		for(int i = 0; i < a.size() ; i++)
		{
			c.moveToPosition(a.keyAt(i));
//			list.add(this.getListView().getAdapter().getItemId((a.keyAt(i))));
			String email = c.getString(2);
			String id = c.getString(0);
			friends +=email+",";
			ids += id;
		}
		
		Editor e = this.getSharedPreferences("friends",Context.MODE_PRIVATE).edit();
		e.putString("friends", friends);
		e.putString("ids", ids);
		e.commit();

//		Bundle bundle = new Bundle();
//		bundle.putStringArrayList("teamlist", list);
//		Intent rslt = new Intent();
//		rslt.putExtras(bundle);
//		setResult(RESULT_OK, rslt);
//		finish();
	}

	@Override
	protected void onPause() {
		if (!c.isClosed()) SaveSelection();
		super.onPause();
	}

	
//    @Override
//    protected void onListItemClick(ListView l, View v, int position, long id) {
//        super.onListItemClick(l, v, position, id);
//       
//        String friend = c.getString(1);
//        String email = c.getString(2);
//		Toast.makeText(this, "Selected"+friend+"'"+email, Toast.LENGTH_LONG).show();
//
////        Log.d("AlertList", "selected item gid=" + id + ": ");
////        
////		Bundle bundle = new Bundle();
////		bundle.putString(ReminderHelper.ROWID, r.getRowid().toString());
////
////		Intent mIntent = new Intent();
////		mIntent.putExtras(bundle);
////		setResult(RESULT_OK, mIntent);
////		finish();
//    } 

}
