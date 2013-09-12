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
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import android.graphics.drawable.Drawable;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

@Root
public class Reminder {

	//Control variables
	private Long rowid;
	@Element
    String title = null; 
	@Element (required=false)
    String detail = null; 
	@Element
	Integer latitude = null;
	@Element
	Integer longitude = null;
	@Element (required=false)
	String priority = ""; //control how important. If required, on the last day -- notify didnt occur. optional -- let it pass away quietly	
	@Element
	String state =""; // "A" = active, A1=snoozed, A2=alarm stopped, "D" = disabled/deleted
	@Element
	String author; 	  //uniquely identifable user
	@Element
	String team = ""; //stores which group of people will be informed	
	@Element (required=false)
	Long validTill =null; //stores the date to which this reminder is valid
	@Element
	Long created = null; //stores last modififed.	
	@Element
	Long modified= null; //stores last modififed.
	@Element (required=false)
	private Long g_id = null;
	@Element (required=false)
	private String syncflag = null;
	@Element (required=false)
	private Long g_timestamp = null;
	//Reminder valid until
//	Drawable mMarkerShadow = null;; already supported within the Overlayitem.it is generated from the marker
	Drawable marker = null;  //stored in overlayitem
    private int distance;
    private int nearest;
	Long nearestOn =null;
	

	public void setMarker(Drawable marker) {
		if (marker != null) this.marker = marker;
//		if (markerShadow != null) this.mMarkerShadow = markerShadow;
	}
	
	public OverlayItem getPin() {
		if (this.latitude == null) return null;
		if (this.longitude == null) return null;
		if (this.title == null) return null;
		
		OverlayItem i = new OverlayItem((new GeoPoint(latitude, longitude)), title, rowid.toString());
		i.setMarker(marker);
		return (i);
	}

	public Long getRowid() {
		return rowid;
	}

	public void setRowid(Long rowid) {
		this.rowid = rowid;
	}

	public Integer getLatitude() {
		return latitude;
	}

	public void setLatitude(Integer latitude) {
		this.latitude = latitude;
	}

	public Integer getLongitude() {
		return longitude;
	}

	public void setLongitude(Integer longitude) {
		this.longitude = longitude;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public Long getValidTill() {
		return validTill;
	}

	public void setValidTill(Long validTill) {
		this.validTill = validTill;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Long getCreated() {
		return created;
	}

	public void setCreated(Long created) {
		this.created = created;
	}

	public Long getModified() {
		return modified;
	}

	public void setModified(Long modified) {
		this.modified = modified;
	}

	public GeoPoint getPoint() {
		return new GeoPoint(this.latitude, this.longitude);
	}

	public void setDistance(int int1) {
		this.distance = int1;
		
	}
	
	public int getDistance() {
		return distance;
	}

	public void setNearest(int int1) {
		this.nearest = int1;
		
	}

	public Long getNearestOn() {
		return nearestOn;
	}

	public void setNearestOn(Long nearestOn) {
		this.nearestOn = nearestOn;
	}

	public int getNearest() {
		return nearest;
	}	

	public String getXML() {
		String str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><task><rowid>9999</rowid><title>9999</title><detail>9999</detail><latitude>9999</latitude>" +
				"<longitude>9999</longitude><priority>9999</priority><state>9999</state><author>9999</author><team>9999</team><validTill>9999</validTill><created>9999</created>" +
				"<modified>9999</modified><g_id>9999</g_id><syncflag>9999</syncflag></task>";
		
		str = str.replaceFirst("9999", this.rowid.toString());
		str = str.replaceFirst("9999", title);
		if (detail != null) str = str.replaceFirst("9999", detail);
		else str = str.replaceFirst("9999", "");
		str = str.replaceFirst("9999", latitude.toString());
		str = str.replaceFirst("9999", longitude.toString());
		str = str.replaceFirst("9999", priority);	
		str = str.replaceFirst("9999", state);
		str = str.replaceFirst("9999", author);
		str = str.replaceFirst("9999", team);
		if (validTill != null) str = str.replaceFirst("9999", validTill.toString());
		else str = str.replaceFirst("9999", "");
		if (created != null) str = str.replaceFirst("9999", created.toString());
		else str = str.replaceFirst("9999", "");
		if (modified != null) str = str.replaceFirst("9999", modified.toString());
		else str = str.replaceFirst("9999", "");
		if (g_id != null) str = str.replaceFirst("9999", g_id.toString());
		else str = str.replaceFirst("9999", "");
		if (syncflag != null) str = str.replaceFirst("9999", syncflag.toString());
		else str = str.replaceFirst("9999", "");
		return str;
	}

	public void setG_id(Long g_id) {
		this.g_id = g_id;
	}

	public Long getG_id() {
		return g_id;
	}

	public String getSyncflag() {
		return syncflag;
	}

	public void setSyncflag(String syncflag) {
		this.syncflag = syncflag;
	}

	public void setG_timestamp(Long g_timestamp) {
		this.g_timestamp = g_timestamp;
	}

	public Long getG_timestamp() {
		return g_timestamp;
	}

	
}
