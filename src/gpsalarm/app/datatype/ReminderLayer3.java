package gpsalarm.app.datatype;
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
import gpsalarm.app.data.ReminderHelper;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class ReminderLayer3  extends Overlay
{	
	private static final String TAG = ReminderLayer3.class.getSimpleName();

	private Drawable defaultMarker, shadowMarker;    
	Cursor model=null;
	private ReminderHelper db;
	private Paint	innerPaint, borderPaint, textPaint;

	private long begin;
	private boolean triggerflag = false;
    //  The currently selected Map Location...if any is selected.  This tracks whether an information  
    //  window should be displayed & where...i.e. whether a user 'clicked' on a known map location
//    private OverlayItem selectedPin;  

	private GeoPoint selectPoint;          
	public void setSelectPoint(GeoPoint selectPoint) {
		this.selectPoint = selectPoint;
	}

	public GeoPoint getSelectPoint() {
		return selectPoint;
	}
	
    private Reminder selectedReminder;
    public void setSelectedReminder(Reminder selectedReminder) {
		this.selectedReminder = selectedReminder;
	}

	private Reminder previousSelected = null;
	public Reminder getSelectedReminder() {
		return selectedReminder;
	}

	public ReminderLayer3(ReminderHelper db, Drawable marker, Drawable shadow) {
		this.db = db;
		defaultMarker = marker;
		shadowMarker = shadow;
		defaultMarker.setBounds(0, 0, defaultMarker.getIntrinsicWidth(),defaultMarker.getIntrinsicHeight());
	}

	public Long addReminderPin(Reminder r) {
		return  db.insert(r);
	}
	
	public void removeReminderPin(Reminder r, String user) {
		db.markDelete(r, user);
		this.setSelectedReminder(null);
	}
	
	@Override
	public boolean onTap(GeoPoint p, MapView mapView)  {		
		//  Next test whether a new popup should be displayed
		selectedReminder = getHitMapLocation(mapView,p);
		if (selectedReminder != null && previousSelected != null && 
				selectedReminder.getRowid() == previousSelected.getRowid()) {
			mapView.performLongClick();
		}
		else if (selectedReminder != null){
			previousSelected = selectedReminder;
			mapView.postInvalidate();
		}
		
		//  Lastly return true if we handled this onTap()
		return selectedReminder != null;
	}

	
    @Override
	public boolean onTouchEvent(MotionEvent e, MapView mapView) {
		if (e.getAction() == MotionEvent.ACTION_DOWN) {
			if (!triggerflag) {
				begin = e.getEventTime();
				triggerflag = true;
				Log.d(TAG, "trigger="+triggerflag+" : begin time="+begin);
			}
			else {
				Log.d(TAG, "trigger="+triggerflag+" : duration="+(e.getEventTime() - begin));
				if (triggerflag && (e.getEventTime() - begin) < 500) {
					selectPoint = (mapView).getProjection().fromPixels((int) e.getX(), (int) e.getY());				
					mapView.performLongClick();
					triggerflag = false;
					begin = 0;
				}
				else {
					triggerflag = false;
					begin = 0;
				}
			}
		}
		return super.onTouchEvent(e, mapView);
	}

	@Override
	public void draw(Canvas canvas, MapView	mapView, boolean shadow) {
   		drawMapLocations(canvas, mapView, shadow);
   		drawInfoWindow(canvas, mapView, shadow);
    }

//    private void drawPinBounds(Canvas canvas, MapView mapView, boolean shadow) {
//    	Rect wm = defaultMarker.copyBounds();
//
//    	Rect ws = defaultMarker.copyBounds();
//    	//Model is database access. if database is open. close
//		if (model!=null) {
//			model.close();
//		}		
//		String where=null;
//		model=db.getActiveReminders(where, null);
//		model.moveToFirst();
//		for (int i=0; i< model.getCount(); i++) {
//			GeoPoint p = new GeoPoint(model.getInt(model.getColumnIndexOrThrow(ReminderHelper.LATITUDE)), model.getInt(model.getColumnIndexOrThrow(ReminderHelper.LONGITUDE)));
//			Point sp = new Point();
//    		mapView.getProjection().toPixels(p, sp);
//			Log.d(TAG, "point="+p.getLatitudeE6()+","+p.getLongitudeE6()+"A: "+ wm.contains(sp.x, sp.y));
//			if (wm.contains(sp.x, sp.y)) {
//				if (!shadow) {
//					wm.offset(sp.x, sp.y);
//					canvas.drawRect(wm, getBorderPaint());
//				}
//			}
//			model.moveToNext();
//		}
//		
//	}

	/**
     * Test whether an information balloon should be displayed or a prior balloon hidden.
     */
    public Reminder getHitMapLocation(MapView	mapView, GeoPoint	tapPoint) {
    	RectF hitTestRecr = new RectF();
		Point screenCoords = new Point();
    	
    	//  Track which MapLocation was hit...if any
    	Reminder hitReminder = null;
    	
    	//Model is database access. if database is open. close
		if (model!=null) {
			model.close();
		}		
		String where=null;
		model=db.getActiveReminders(where, null);
		model.moveToFirst();
		for (int i=0; i< model.getCount(); i++) {
			GeoPoint p = new GeoPoint(model.getInt(model.getColumnIndexOrThrow(ReminderHelper.LATITUDE)), model.getInt(model.getColumnIndexOrThrow(ReminderHelper.LONGITUDE)));
    		mapView.getProjection().toPixels(p, screenCoords);

	    	// Create a 'hit' testing Rectangle w/size and coordinates of our icon
	    	// Set the 'hit' testing Rectangle with the size and coordinates of our on screen icon
    		hitTestRecr.set(-defaultMarker.getIntrinsicWidth()/2,-defaultMarker.getIntrinsicHeight(),defaultMarker.getIntrinsicWidth()/2,0);
    		hitTestRecr.offset(screenCoords.x,screenCoords.y);

	    	//  Finally test for a match between our 'hit' Rectangle and the location clicked by the user
    		mapView.getProjection().toPixels(tapPoint, screenCoords);
    		if (hitTestRecr.contains(screenCoords.x,screenCoords.y)) {
    			Long rowid = model.getLong(model.getColumnIndexOrThrow(ReminderHelper.ROWID));
    			hitReminder = db.getReminderByRowid(rowid.toString());
    			break;
    		}
    		model.moveToNext();
    	}
    	
    	//  Lastly clear the newMouseSelection as it has now been processed
    	tapPoint = null;
    	
    	return hitReminder; 
    }
    
    private void drawMapLocations(Canvas canvas, MapView	mapView, boolean shadow) {
    	RectF w = new RectF();
    	w.set(0, 0, mapView.getWidth(), mapView.getHeight());

    	//Model is database access. if database is open. close
		if (model!=null) {
			model.close();
		}		
		String where=null;
		model=db.getActiveReminders(where, null);
		model.moveToFirst();
		for (int i=0; i< model.getCount(); i++) {
			GeoPoint p = new GeoPoint(model.getInt(model.getColumnIndexOrThrow(ReminderHelper.LATITUDE)), model.getInt(model.getColumnIndexOrThrow(ReminderHelper.LONGITUDE)));
			Point sp = new Point();
    		mapView.getProjection().toPixels(p, sp);
//			Log.d(TAG, "point="+p.getLatitudeE6()+","+p.getLongitudeE6()+"A: "+ w.contains(sp.x, sp.y));
			if (w.contains(sp.x, sp.y)) {
//				Log.d(TAG, "inside");
				if (shadow)
					drawAt(canvas, shadowMarker, sp.x, sp.y - defaultMarker.getIntrinsicHeight(), shadow);
				else
					drawAt(canvas, defaultMarker, sp.x - defaultMarker.getIntrinsicWidth()/2, sp.y - defaultMarker.getIntrinsicHeight(), shadow);

			}
			model.moveToNext();
		}
    }

    private void drawInfoWindow(Canvas canvas, MapView	mapView, boolean shadow) {
    	
    	if ( selectedReminder != null) {
    		if ( shadow) {
    			//  Skip painting a shadow in this tutorial
    		} else {
				//  First determine the screen coordinates of the selected MapLocation
				Point selDestinationOffset = new Point();
				mapView.getProjection().toPixels(selectedReminder.getPoint(), selDestinationOffset);
		    	
		    	//  Setup the info window with the right size & location
				int INFO_WINDOW_WIDTH = 125;
				int INFO_WINDOW_HEIGHT = 30;
				RectF infoWindowRect = new RectF(0,0,INFO_WINDOW_WIDTH,INFO_WINDOW_HEIGHT);				
				int infoWindowOffsetX = selDestinationOffset.x-INFO_WINDOW_WIDTH/2;
				int infoWindowOffsetY = selDestinationOffset.y-INFO_WINDOW_HEIGHT-defaultMarker.getIntrinsicHeight();
				infoWindowRect.offset(infoWindowOffsetX,infoWindowOffsetY);

				//  Draw inner info window
				canvas.drawRoundRect(infoWindowRect, 5, 5, getInnerPaint());
				
				//  Draw border for info window
				canvas.drawRoundRect(infoWindowRect, 5, 5, getBorderPaint());
					
				//  Draw the MapLocation's name
				int TEXT_OFFSET_X = 5;
				int TEXT_OFFSET_Y = 21;
				canvas.drawText(selectedReminder.getTitle(),infoWindowOffsetX+TEXT_OFFSET_X,infoWindowOffsetY+TEXT_OFFSET_Y,getTextPaint());
			}
    	}
    }
    
	public Paint getInnerPaint() {
		if ( innerPaint == null) {
			innerPaint = new Paint();
			innerPaint.setARGB(225, 75, 75, 75); //gray
			innerPaint.setAntiAlias(true);
		}
		return innerPaint;
	}

	public Paint getBorderPaint() {
		if ( borderPaint == null) {
			borderPaint = new Paint();
			borderPaint.setARGB(255, 255, 255, 255);
			borderPaint.setAntiAlias(true);
			borderPaint.setStyle(Style.STROKE);
			borderPaint.setStrokeWidth(2);
		}
		return borderPaint;
	}

	public Paint getTextPaint() {
		if ( textPaint == null) {
			textPaint = new Paint();
			textPaint.setARGB(255, 255, 255, 255);
			textPaint.setAntiAlias(true);
			textPaint.setTextSize(21);
		}
		return textPaint;
	}

	public GeoPoint getCenter() {
		if (selectedReminder != null) return selectedReminder.getPoint();
		else return new GeoPoint(-33840029, 150969963);
	}
}