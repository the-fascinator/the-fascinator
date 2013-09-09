/*
 * Hibernate Access Control Service Test
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
package com.googlecode.fascinator.model.service;

import static org.junit.Assert.*;

import java.util.List;

import com.googlecode.fascinator.model.Record;
import com.googlecode.fascinator.model.Role;
import com.googlecode.fascinator.model.User;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"file:src/test/resources/test-applicationContext.xml"})
public class HibernateAccessControlServiceTest {
	
	@Autowired
	public String databaseString;
	public static String staticDBString;

	@Autowired
	private HibernateAccessControlService hibernateAccessControlService;

	@Test
	public void testNewRecord() {
		String recordId = String.valueOf(System.currentTimeMillis());
		Record inserted = hibernateAccessControlService.createOrGetRecord(recordId);
		Record fromDb = hibernateAccessControlService.getRecord(recordId);
		assertEquals(inserted, fromDb);
	}
	
	@Test
	public void testGetRole() {		
		Role inserted = newRole();
		Role fromDb = hibernateAccessControlService.getRole(inserted.getRecordId(), inserted.getRole());
		assertEquals(inserted, fromDb);
	}

	@Test
	public void testGetUser() {		
		User inserted = newUser();
		User fromDb = hibernateAccessControlService.getUser(inserted.getRecordId(), inserted.getUsername());
		assertEquals(inserted, fromDb);
	}

	@Test
	public void testGetRoles() {		
		Role inserted = newRole();
		List<Role> roles = hibernateAccessControlService.getRoles(inserted.getRecordId());
		assertEquals(1, roles.size());
		assertEquals(inserted, roles.get(0));		
	}

	@Test
	public void testGetUsers() {		
		User inserted = newUser();
		List<User> users = hibernateAccessControlService.getUsers(inserted.getRecordId());
		assertEquals(1, users.size());
		assertEquals(inserted, users.get(0));
	}

	@Test
	public void testRevokeUserAccess() {
		User inserted = newUser();
		hibernateAccessControlService.revokeUserAccess(inserted);		
		User fromDb = hibernateAccessControlService.getUser(inserted.getRecordId(), inserted.getUsername());
		assertNull(fromDb);
	}

	@Test
	public void testRevokeRoleAccess() {
		Role inserted = newRole();
		hibernateAccessControlService.revokeRoleAccess(inserted);
		Role fromDb = hibernateAccessControlService.getRole(inserted.getRecordId(), inserted.getRole());
		assertNull(fromDb);
	}
	
	@Test
	public void testRoleInserts() {
		Role inserted = newRole();
		Role inserted2 = newRole();
		assertTrue(!inserted.equals(inserted2));
	}
	
	private User newUser() {
		String userName = String.valueOf(System.currentTimeMillis());
		String recordId = String.valueOf(System.currentTimeMillis());
		User inserted = hibernateAccessControlService.grantUserAccess(recordId, userName);
		return inserted;
	}
	
	private Role newRole() {
		String roleName = String.valueOf(System.currentTimeMillis());
		String recordId = String.valueOf(System.currentTimeMillis());
		Role inserted = hibernateAccessControlService.grantRoleAccess(recordId, roleName);
		return inserted;
	}

}
