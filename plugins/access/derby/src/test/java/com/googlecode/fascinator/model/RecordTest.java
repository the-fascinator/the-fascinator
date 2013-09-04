/*
 * Record Test
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

public class RecordTest {

	@Test
	public void testEqualsObject() {
		Record rec1 = getStandardRecord();
		Record rec2 = getStandardRecord();
		assertEquals(rec1, rec2);
		rec2.setRecordId(null);
		assertFalse(rec1.equals(rec2));
		assertFalse(rec1.equals(new Object()));
	}
	
	private Record getStandardRecord() {
		return new Record("record1");
	}

}
