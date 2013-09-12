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

//import android.graphics.drawable.Drawable;
//import com.google.android.maps.GeoPoint;
//import com.google.android.maps.OverlayItem;

@Root
public class Position {

	//Control variables
	@Element
	private String username;
	@Element
	Long created = null;
	@Element
	Integer latitude = null;
	@Element
	Integer longitude = null;

	public Position() {
	}

	
	public Position(String username, Integer latitude,
			Integer longitude) {
		super();
		this.username = username;
		this.latitude = latitude;
		this.longitude = longitude;
//		this.created = created;
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

	public Long getCreated() {
		return created;
	}

	public void setCreated(Long created) {
		this.created = created;
	}


	public String getUsername() {
		return username;
	}


	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getXML() {
		String str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><position><username>9999</username><created>9999</created><latitude>9999</latitude>" +
				"<longitude>9999</longitude></position>";
		
		str = str.replaceFirst("9999", username);
		str = str.replaceFirst("9999", latitude.toString());
		str = str.replaceFirst("9999", longitude.toString());
		if (created != null) str = str.replaceFirst("9999", created.toString());
		else str = str.replaceFirst("9999", "");
		return str;
	}
	
	
	// give distance of nearby point in meters
	// useful neighbourhood distance measurement
	//
	public float distance( Position p) {
		long delX = this.latitude-p.getLatitude();
		long delY = this.longitude-p.getLongitude();
		long d2 = (delX * delX) + (delY*delY);
		return  (float) ( 0.11132f * java.lang.Math.sqrt(d2));
	}

}
