/*
 * User Test
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
package com.googlecode.fascinator.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class UserTest {

	@Test
	public void testEqualsObject() {
		User user1 = getStandardUser();
		User user2  = getStandardUser();
		User user3 = getStandardUser();
		assertEquals(user1, user2);
		assertEquals(new User(), new User());
		user2.setUsername(null);
		assertFalse(user1.equals(user2));
		user3.setRecordId(null);
		assertFalse(user1.equals(user3));
		assertFalse(user1.equals(new Object()));
	}

	private User getStandardUser() {
		return new User ("recordId", "user");
	}


}
