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



@Root
public class Tweet {
	@Element
	String friend=null;
	@Element
	String gid;
	@Element
	Long rid;
	@Element (required=false)
	String status=null;
	@Element
	Long created;	
	
	
	public Tweet() {
		super();
	}

	public Tweet(String friend, String gid, Long created, Long id, String status) {
		this.rid = id;  //indicates status record not reminder.
		this.friend=friend;
		this.created=created;
		this.status=status;
		this.gid =gid;
	}

	public String getFriend() {
		return friend;
	}

	public void setFriend(String friend) {
		this.friend = friend;
	}

	public Long getCreated() {
		return created;
	}

	public void setCreated(Long createdAt) {
		this.created = createdAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setGid(String gid) {
		this.gid = gid;
	}

	public String getGid() {
		return gid;
	}

	public Long getRid() {
		return rid;
	}

	public void setRid(Long rowid) {
		this.rid = rowid;
	}	

	public String getXML() {
		String str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><tweet>" +
				"<rid>9999</rid><friend>9999</friend><gid>9999</gid>" +
				"<status>9999</status><created>9999</created>" +
				"</tweet>";
		
		if (rid != null) str = str.replaceFirst("9999", this.rid.toString());
		else str = str.replaceFirst("9999", "");
		str = str.replaceFirst("9999", friend);
		str = str.replaceFirst("9999", gid);		
		str = str.replaceFirst("9999", status);
		if (created != null) str = str.replaceFirst("9999", created.toString());
		else str = str.replaceFirst("9999", "");
		return str;
	}	
}
