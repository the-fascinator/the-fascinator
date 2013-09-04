/*
 * User Model Composite PK
 * 
 * Copyright (C) 2008-2010 University of Southern Queensland
 * Copyright (C) 2013 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.googlecode.fascinator.model.pk;

import java.io.Serializable;

public class UserPk implements Serializable{
	private String recordId;
	private String username;
	
	public UserPk() {
		
	}
	
	public UserPk(String recordId, String username) {
		this.recordId = recordId;
		this.username = username;
	}
	
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (! (obj instanceof UserPk)) return false;
		UserPk inst = (UserPk) obj;
		return ( (inst.getRecordId() == null && getRecordId() == null) || (inst.getRecordId()!=null && inst.getRecordId().equalsIgnoreCase(recordId))
				&& ( (inst.getUsername() == null && username == null) || (inst.getUsername()!= null && inst.getUsername().equalsIgnoreCase(username)) )				
				);
	}
	
	public String getRecordId() {
		return recordId;
	}
	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
}
