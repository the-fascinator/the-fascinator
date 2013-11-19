/*
 * Hibernate Access Control Test
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

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.access.AccessControlException;
import com.googlecode.fascinator.api.access.AccessControlSchema;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.model.Record;
import com.googlecode.fascinator.model.service.HibernateAccessControlService;
import com.googlecode.fascinator.spring.ApplicationContextProvider;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"file:src/test/resources/test-applicationContext.xml"})
public class HibernateAccessControlTest {

	@Autowired
	private HibernateAccessControlService hibernateAccessControlService;
	private HibernateAccessControl hibernateAccessControl;
	
	private final Logger log = LoggerFactory
			.getLogger(HibernateAccessControlTest.class);
	
	@Before
	public void setUp() throws Exception {
		hibernateAccessControl = (HibernateAccessControl) PluginManager.getAccessControl("hibernateAccessControl");
		hibernateAccessControl.init(JsonSimpleConfig.getSystemFile());
		ApplicationContextProvider.getApplicationContext().getBean("fascinatorIndexer");
	}

	@After
	public void tearDown() throws Exception {
		hibernateAccessControl.shutdown();
	}

	@Test
	public void testRoleSchema() throws AccessControlException {		
		HibernateAccessSchema inserted = getNewSchema();		
		String recordId = inserted.getRecordId();
		inserted.user = null;
		hibernateAccessControl.applySchema(inserted);
		Record record = hibernateAccessControlService.getRecord(recordId);
		assertNotNull(record);
		assertEquals(recordId, record.getRecordId());
		List<AccessControlSchema> schemaList = hibernateAccessControl.getSchemas(recordId);
		assertEquals(1, schemaList.size());
		dumpSchema(inserted);
		dumpSchema((HibernateAccessSchema)schemaList.get(0));
		assertEquals(inserted, schemaList.get(0));
	}
	
	@Test
	public void testUserSchema() throws AccessControlException {		
		HibernateAccessSchema inserted = getNewSchema();		
		String recordId = inserted.getRecordId();
		inserted.role = null;
		hibernateAccessControl.applySchema(inserted);
		Record record = hibernateAccessControlService.getRecord(recordId);
		assertNotNull(record);
		assertEquals(recordId, record.getRecordId());
		List<AccessControlSchema> schemaList = hibernateAccessControl.getSchemas(recordId);
		assertEquals(1, schemaList.size());
		dumpSchema(inserted);
		dumpSchema((HibernateAccessSchema)schemaList.get(0));
		assertEquals(inserted, schemaList.get(0));
	}

	private HibernateAccessSchema getNewSchema() {
		HibernateAccessSchema newSchema = (HibernateAccessSchema) hibernateAccessControl.getEmptySchema();
		newSchema.role = String.valueOf(System.currentTimeMillis());
		newSchema.user = String.valueOf(System.currentTimeMillis());
		newSchema.setRecordId(String.valueOf(System.currentTimeMillis()));
		return newSchema;
	}
	
	private void dumpSchema(AccessControlSchema schema) {
		log.info("---Schema dump ---");
		log.info("Record: " + schema.getRecordId());
	}
	
	private void dumpSchema(HibernateAccessSchema schema) {
		log.info("---Schema dump ---");
		log.info("User: " + schema.user);
		log.info("Role: " + schema.role);
		log.info("Record: " + schema.getRecordId());
	}
}
