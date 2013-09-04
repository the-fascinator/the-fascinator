/*
 * User Pk Test
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

import static org.junit.Assert.*;

import org.junit.Test;

import com.googlecode.fascinator.model.User;

public class UserPkTest {

	@Test
	public void testEqualsObject() {
		UserPk user1 = getStandardUserPk();
		UserPk user2  = getStandardUserPk();
		UserPk user3 = getStandardUserPk();
		assertEquals(user1, user2);
		assertEquals(new UserPk(), new UserPk());
		user2.setUsername(null);
		assertFalse(user1.equals(user2));
		user3.setRecordId(null);
		assertFalse(user1.equals(user3));
		assertFalse(user1.equals(new Object()));
	}

	private UserPk getStandardUserPk() {
		return new UserPk("recordId", "user");
	}


}
