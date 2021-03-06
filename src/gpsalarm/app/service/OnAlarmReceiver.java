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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class OnAlarmReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent i=new Intent(context, PostMonitor.class);
		
		i.setAction(PostMonitor.POLL_ACTION);
		
		WakefulIntentService.sendWakefulWork(context, i);
		
		Log.i("AlarmReceiver", "AlarmReceiver was triggered");
	}
}
