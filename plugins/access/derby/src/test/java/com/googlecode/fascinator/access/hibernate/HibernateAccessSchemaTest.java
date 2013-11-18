/*
 * Hibernate Access Schema Test
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
package com.googlecode.fascinator.access.hibernate;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HibernateAccessSchemaTest {


	@Test
	public void testNoNullEquality() {		
		HibernateAccessSchema schema1 = getStandardSchema();		
		HibernateAccessSchema schema2 = getStandardSchema();
		assertEquals(schema1, schema2);
	}
	
//	@Test
	public void testWithNullRoleEquality() {		
		HibernateAccessSchema schema1 = getStandardSchema();		
		HibernateAccessSchema schema2 = getStandardSchema();
		schema2.role = null;
		assertFalse(schema1.equals(schema2));
	}
	
//	@Test
	public void testWithNullUserEquality() {		
		HibernateAccessSchema schema1 = getStandardSchema();		
		HibernateAccessSchema schema2 = getStandardSchema();
		schema2.user= null;
		assertFalse(schema1.equals(schema2));
	}
	
//	@Test
	public void testWithNullRecIdEquality() {		
		HibernateAccessSchema schema1 = getStandardSchema();		
		HibernateAccessSchema schema2 = getStandardSchema();
		schema2.setRecordId(null);
		assertFalse(schema1.equals(schema2));
	}
	
//	@Test
	public void testWithNullEquality() {		
		HibernateAccessSchema schema1 = getStandardSchema();		
		HibernateAccessSchema schema2 = new HibernateAccessSchema();		
		assertFalse(schema1.equals(schema2));
	}

	private HibernateAccessSchema getStandardSchema() {
		HibernateAccessSchema schema1 = new HibernateAccessSchema();
		schema1.role = "role";
		schema1.user = "user";
		schema1.setRecordId("record1");
		return schema1;
	}
}
