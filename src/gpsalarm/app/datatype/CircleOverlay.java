/*
 * Copyright 2010 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
package gpsalarm.app.datatype;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

/**
 * CircleOverlay is a special Overlay to display a circle on top of the map. Center point and
 * radius of the circle are adjustable.
 * <p>
 * All rendering parameters like color, stroke width, pattern and transparency can be configured
 * via the two {@link android.graphics.Paint Paint} objects in the
 * {@link #CircleOverlay(Paint,Paint) constructor}. Anti-aliasing is always used to improve the
 * visual quality of the image.
 * <p>
 * <b>The implementation of this class is not complete. Its functionality and visible methods
 * are likely to change in a future release.</b>
 */
public class CircleOverlay extends Overlay {
	private static final String THREAD_NAME = "CircleOverlay";

	private Point cachedCenterPosition;
	private GeoPoint center;
	private Paint fillPaint = new Paint();
	private Paint outlinePaint = new Paint();
	private final Path path;
	private int radiusE6;

	public CircleOverlay(GeoPoint center, int radiusE6) {
		this.center = center;
		this.radiusE6 = radiusE6;
		this.path = new Path();
	}
	/**
	 * Sets the parameters of the circle.
	 * 
	 * @param center
	 *            the geographical coordinates of the center point.
	 * @param radius
	 *            the radius of the circle.
	 */
	public synchronized void setCircleData(GeoPoint center, int radius) {
		this.center = center;
		if (this.center != null) {
			// create the array for the cached center point position
			this.cachedCenterPosition = new Point();
		}
		this.radiusE6 = radius;
	}

	//returns true if the geopoint is within the circle.
	public boolean contains(GeoPoint p) {
		boolean result = false;
		int d = distance (p, center);		
		if (d <= radiusE6) 
			return true;
		return result;
		
	}
	
	public int distance(GeoPoint p1, GeoPoint p2) {
		long delX = p1.getLatitudeE6()-p2.getLatitudeE6();
		long delY = p1.getLongitudeE6()-p2.getLongitudeE6();
		long d2 = (delX * delX) + (delY*delY);

		return (int) java.lang.Math.sqrt(d2);
	}

	/**
	 * Sets the paint parameters which will be used to draw the circle.
	 * 
	 * @param fillPaint
	 *            the paint object which will be used to fill the circle.
	 * @param outlinePaint
	 *            the paint object which will be used to draw the outline of the circle.
	 */
	public synchronized void setPaint(Paint fillPaint, Paint outlinePaint) {
		this.fillPaint = fillPaint;
		if (this.fillPaint != null) {
			this.fillPaint.setAntiAlias(true);
		}
		this.outlinePaint = outlinePaint;
		if (this.outlinePaint != null) {
			this.outlinePaint.setAntiAlias(true);
		}
	}

//	/**
//	 * This method should be called after a center point has been added to the Overlay.
//	 */
//	protected final void populate() {
//		super.requestRedraw();
//	}


	public final synchronized void drawOverlayBitmap(Canvas canvas, Point drawPosition,
			Projection projection) {
		if (this.center == null || this.radiusE6 < 0) {
			// no valid parameters to draw the circle
			return;
		} else if (this.fillPaint == null && this.outlinePaint == null) {
			// no paint to draw
			return;
		}

		// make sure that the cached center position is valid
		this.cachedCenterPosition = projection.toPixels(this.center, this.cachedCenterPosition);
//		canvas.drawCircle(this.cachedCenterPosition.x, cachedCenterPosition.y, 5000, fillPaint);
		
		// assemble the path
		this.path.reset();
		this.path.addCircle(this.cachedCenterPosition.x - drawPosition.x,
				this.cachedCenterPosition.y - drawPosition.y, this.radiusE6, Path.Direction.CCW);

		// draw the path on the canvas
		if (this.fillPaint != null) {
			canvas.drawPath(this.path, this.fillPaint);
		}
		if (this.outlinePaint != null) {
			canvas.drawPath(this.path, this.outlinePaint);
		}
	}

	public String getThreadName() {
		return THREAD_NAME;
	}
}