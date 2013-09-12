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
import java.util.Calendar;
import java.util.Date;

import gpsalarm.app.R;
import gpsalarm.app.data.ReminderHelper;
import gpsalarm.app.datatype.Reminder;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class AlarmEdit2 extends Activity {
	SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy");
	String mlat;
	String mlng;
	EditText author;
	EditText state;
	EditText forpin;
	String team;
	EditText team1;
	TextView created;
	EditText modified;
	EditText title=null;
	EditText body=null;
	Button validtill=null;
	RadioGroup priority_radio;
	String priority = null;
	Reminder r = null;
	ReminderHelper db=null;
	String rowId=null;
	private Date created_date;
//	private Reminder pin = new Reminder();
	private Date modified_date;
	private static Date valid_date;
	SharedPreferences prefs=null;

    static int year, month, day;
    static final int DATE_DIALOG_ID = 0;

    private static final String[] items={"FAMILY", "FRIENDS", "BUSINESS", "PUBLIC"};
	private static final int CONTACT_LIST = 0;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.note_edit);

		db = new ReminderHelper(this);
		
//		mlat = (EditText) findViewById(R.id.latitude);
//		mlng = (EditText) findViewById(R.id.longitude);	
		author = (EditText) findViewById(R.id.author);	
		state = (EditText) findViewById(R.id.state);	
//		team = (TextView) findViewById(R.id.team);	
		team1 = (EditText) findViewById(R.id.team1);	
		title=(EditText)findViewById(R.id.title);
		body=(EditText)findViewById(R.id.body);
		validtill=(Button)findViewById(R.id.validpicker);
		priority_radio=(RadioGroup)findViewById(R.id.priority);
		created=(TextView) findViewById(R.id.created);
		modified=(EditText) findViewById(R.id.modified);	
//		Button mValidDate = (Button) findViewById(R.id.validpicker);

		Button save = (Button) findViewById(R.id.save);
		Button stop = (Button) findViewById(R.id.stop);
		Button snooze = (Button) findViewById(R.id.snooze);
		save.setOnClickListener(onSave);

		rowId=getIntent().getStringExtra(ReminderHelper.ROWID);
		mlat = getIntent().getStringExtra(ReminderHelper.LATITUDE);
		mlng = getIntent().getStringExtra(ReminderHelper.LONGITUDE);
		author.setText(getIntent().getStringExtra(ReminderHelper.AUTHOR));
		
		created_date = new Date();
		modified_date = new Date();
		valid_date = new Date(created_date.getTime() + Long.valueOf(259200000));

		
		created.setText(dateFormat.format(created_date));
		modified.setText(dateFormat.format(modified_date));
		validtill.setText(dateFormat.format(valid_date));
//		mValidDate.setText(dateFormat.format(valid_date));
		
		OnClickListener dateListener ;
		validtill.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	        	year = valid_date.getYear()+1900;
	        	month = valid_date.getMonth()+1;
	        	day = valid_date.getDate();
	            showDialog(DATE_DIALOG_ID);
	        }
	    });
		ImageButton arrow = (ImageButton) findViewById(R.id.arrow);
		arrow.setOnClickListener(new View.OnClickListener() {
		
			@Override
			public void onClick(View v) {
				InvokeTeamSelection();				
			}
		});
//	    Spinner spin=(Spinner)findViewById(R.id.spinner);
//	    spin.setOnItemSelectedListener(new OnItemSelectedListener () {
//
//			@Override
//			public void onItemSelected(AdapterView<?> parent,
//		            View v, int position, long id) {
//				team = items[position];
//			}
//
//			@Override
//			public void onNothingSelected(AdapterView<?> parent) {
//				team=null;
//				}
//			});
//	    ArrayAdapter<String> aa=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,items);
//	    aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//	    spin.setAdapter(aa);
	    
		if (rowId!=null) {
			load();
		}		
	}

    private void InvokeTeamSelection() {
		 Intent i=new Intent(this, ContactsList.class);
		startActivityForResult(i, AlarmEdit2.CONTACT_LIST);
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode,Intent intent) {
		switch (requestCode) {
		case CONTACT_LIST:
			String friends = this.getSharedPreferences("friends",Context.MODE_PRIVATE).getString("friends", "");
//			Bundle extras = intent.getExtras();
//			ArrayList<String> list = extras.getStringArrayList("teamlist");
//			String teamlist = "";
//			for (String contact: list) {
//				teamlist += contact + ",";
//			}
			team1.setText(friends);
	      	Toast.makeText(this, "alarmedit friends:"+friends, Toast.LENGTH_LONG).show();	        

	      	break;
		}
	}
	private void load() {

		r=db.getReminderByRowid(rowId);

		mlat = r.getLatitude().toString();
		mlng = r.getLongitude().toString();
		title.setText(r.getTitle());
		body.setText(r.getDetail());
		state.setText(r.getState());
		priority = r.getPriority();
		author.setText(r.getAuthor());
		team =r.getTeam();
		
		created.setText(dateFormat.format(new Date(r.getCreated())));
		modified.setText(dateFormat.format(new Date(r.getModified())));
		validtill.setText(dateFormat.format(new Date(r.getValidTill())));

		//conversion of type to drawable.
		if (priority.equals("required")) {
			priority_radio.check(R.id.required);
		}
		else if (priority.equals("optional")) {
			priority_radio.check(R.id.optional);
		}

		

		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	
		db.close();
	}
	
	@Override
	public void onSaveInstanceState(Bundle saved) {
		super.onSaveInstanceState(saved);
		saved.putString(ReminderHelper.ROWID, rowId);
		saved.putString(ReminderHelper.TITLE, title.getText().toString());
		saved.putString(ReminderHelper.DETAIL, body.getText().toString());
		saved.putString(ReminderHelper.LATITUDE, mlat);
		saved.putString(ReminderHelper.LONGITUDE, mlng);
		saved.putInt("types", priority_radio.getCheckedRadioButtonId());
		saved.putString(ReminderHelper.PRIORITY, priority);
		saved.putString(ReminderHelper.STATE, state.getText().toString());
		saved.putString(ReminderHelper.VALIDTILL, validtill.getText().toString());
		saved.putString(ReminderHelper.AUTHOR, author.getText().toString());
		saved.putString(ReminderHelper.TEAM, team);
		saved.putString(ReminderHelper.CREATED, created.getText().toString());
		saved.putString(ReminderHelper.MODIFIED, modified.getText().toString());
	}

	@Override
	public void onRestoreInstanceState(Bundle saved) {
		super.onRestoreInstanceState(saved);
		
		rowId = saved.getString(ReminderHelper.ROWID);
		mlat = saved.getString(ReminderHelper.LATITUDE);
		mlng = saved.getString(ReminderHelper.LONGITUDE);
		title.setText(saved.getString(ReminderHelper.TITLE));
		body.setText(saved.getString(ReminderHelper.DETAIL));
		state.setText(saved.getString(ReminderHelper.STATE));
		priority = saved.getString(ReminderHelper.PRIORITY);
		priority_radio.check(saved.getInt("types"));
		author.setText(saved.getString(ReminderHelper.AUTHOR));
		team=saved.getString(ReminderHelper.TEAM);
		validtill.setText(saved.getString(ReminderHelper.VALIDTILL));
		created.setText(saved.getString(ReminderHelper.CREATED));
		modified.setText(saved.getString(ReminderHelper.MODIFIED));
	}
	
	private View.OnClickListener onSave=new View.OnClickListener() {
		public void onClick(View v) {
			String type=null;
	    	Long now = Calendar.getInstance().getTime().getTime();
	    	
			switch (priority_radio.getCheckedRadioButtonId()) {
				case R.id.required:
					priority="required";
					break;
				case R.id.optional:
					priority="optional";
					break;
			}

			if (rowId==null) {
				Reminder r = new Reminder();
				r.setLatitude(Integer.valueOf(mlat)); 
				r.setLongitude(Integer.valueOf(mlng)); 
				r.setTitle(title.getText().toString());
				r.setDetail(body.getText().toString());
				r.setState(state.getText().toString());
				r.setPriority(priority);
				r.setAuthor(author.getText().toString());
				r.setTeam(team);
				r.setValidTill(Long.valueOf(valid_date.getTime()));		
				r.setSyncflag("I");
				r.setState("A");
				r.setCreated(now);
				r.setModified(now);
				r.setG_id(null);
				Long rowid = db.insert(r);
				rowId = rowid.toString();
			}
			else {
//				r.setRowid(Long.valueOf(g_id));
		        r.setModified(now);
				r.setSyncflag("E");
				r.setState("A");
				db.updateWithRowid(r, rowId);
			}
			
			Bundle bundle = new Bundle();
			bundle.putString(ReminderHelper.ROWID, rowId);
			Intent mIntent = new Intent();
			mIntent.putExtras(bundle);
			setResult(RESULT_OK, mIntent);
			finish();
		}
	};    

	@Override
	protected Dialog onCreateDialog(int id) {
	   switch (id) {
	   case DATE_DIALOG_ID:
	      return new DatePickerDialog(this,
	                mDateSetListener,
	                year, month, day);
	   }
	   return null;
	}
    
   private DatePickerDialog.OnDateSetListener mDateSetListener =
        new DatePickerDialog.OnDateSetListener() 
        {
            public void onDateSet(DatePicker view, int setyear, int monthOfYear,
                    int dayOfMonth) 
            {
                year = setyear-1900;
                month = monthOfYear;
                day = dayOfMonth;
                valid_date = new Date(year, month, day);
        		validtill.setText(dateFormat.format(valid_date));
            }
        };

	
}

