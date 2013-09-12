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

public class Alert {
	
	@Element (required=false)
	private String username = null;
	@Element (required=false)
	private Long gid = null;
	@Element (required=false)
	private int dist = 9999;
	@Element (required=false)
	private Long timestamp = null;
	
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public Long getGid() {
		return gid;
	}
	public void setGid(Long gid) {
		this.gid = gid;
	}
	public int getDist() {
		return dist;
	}
	public void setDist(int dist) {
		this.dist = dist;
	}
	public Long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
	
}
