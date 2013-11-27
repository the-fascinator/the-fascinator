/*
 * Hibernate Access Control
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

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.access.AccessControl;
import com.googlecode.fascinator.api.access.AccessControlException;
import com.googlecode.fascinator.api.access.AccessControlSchema;
import com.googlecode.fascinator.model.User;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.model.Role;
import com.googlecode.fascinator.model.service.HibernateAccessControlService;
import com.googlecode.fascinator.spring.ApplicationContextProvider;
/**
 * 
 * Hibernate-based access control
 * 
 * @author Shilo Banihit
 *
 */
public class HibernateAccessControl implements AccessControl {

	/** Logging */
	private final Logger log = LoggerFactory
			.getLogger(HibernateAccessControl.class);
	
	private JsonSimple config;
	
	private HibernateAccessControlService hibernateService;

	@Override
	public String getId() {
		return "hibernateAccessControl";
	}

	@Override
	public String getName() {
		return "Hibernate Access Control Plugin";
	}

	@Override
	public PluginDescription getPluginDetails() {
		return new PluginDescription(this);
	}

	@Override
	public void init(File jsonFile) throws PluginException {
		try {
			setConfig(new JsonSimple(jsonFile));
		} catch (IOException ioe) {
			throw new AccessControlException(ioe);
		}
	}

	@Override
	public void init(String jsonString) throws PluginException {
		try {
			setConfig(new JsonSimple(jsonString));
		} catch (IOException ioe) {
			throw new AccessControlException(ioe);
		}	
	}
	
	private void setConfig(JsonSimple config) throws AccessControlException {
	    log.debug("Access control getting config...");
		this.config = config;		
		if (!ApplicationContextProvider.getApplicationContext().containsBean("hibernateAccessControlService")) {
			throw new AccessControlException("hibernateAccessControlService bean not available, please check your Spring configuration.");
		}
		hibernateService = (HibernateAccessControlService) ApplicationContextProvider.getApplicationContext().getBean("hibernateAccessControlService");
	}
	
	
	@Override
	public void shutdown() throws PluginException {
		log.debug("Access control shutting down...");
	}

	@Override
	public AccessControlSchema getEmptySchema() {
		return new HibernateAccessSchema();
	}

	/**
	 * Get a list of schemas that have been applied to a record.
	 * 
	 * @param recordId
	 *            The record to retrieve information about.
	 * @return A list of access control schemas, possibly zero length.
	 * @throws AccessControlException
	 *             if there was an error during retrieval.
	 */
	@Override
	public List<AccessControlSchema> getSchemas(String recordId)
			throws AccessControlException {
	    log.debug("Getting schemas for:" + recordId);
		List<AccessControlSchema> schemas = new ArrayList<AccessControlSchema>();
		try {
			List<Role> roles = hibernateService.getRoles(recordId);
			List<User> users = hibernateService.getUsers(recordId);
			if ((roles == null || roles.isEmpty()) && (users == null || users.isEmpty())) {
				return new ArrayList<AccessControlSchema>();
			}
			HibernateAccessSchema schema;
			for (Role role : roles) {
				schema = new HibernateAccessSchema();
				schema.init(recordId);
				schema.set("role", role.getRole());
				schemas.add(schema);
			}
			for (User user : users) {
				schema = new HibernateAccessSchema();
				schema.init(recordId);
				schema.set("user", user.getUsername());
				schemas.add(schema);
			}
		} catch (Exception ex) {
			log.error("Error searching security database: ", ex);
			throw new AccessControlException(
					"Error searching security database");
		}
		return schemas;
	}
	

	/**
	 * Apply/store a new security implementation. The schema will already have a
	 * recordId as a property.
	 * 
	 * @param newSecurity
	 *            The new schema to apply.
	 * @throws AccessControlException
	 *             if storage of the schema fails.
	 */
	@Override
	public void applySchema(AccessControlSchema newSecurity)
			throws AccessControlException {		
	    log.debug("Applying new schema...");
		String recordId = newSecurity.getRecordId();
		if (recordId == null || recordId.equals("")) {
			throw new AccessControlException("No record provided by schema.");
		}
		// Find the new role
		String role = newSecurity.get("role");
		if (role != null && !role.equals("")) {
			processRoleSchema(recordId, role);
			return;
		}

		String user = newSecurity.get("user");
		if (user != null && !user.equals("")) {
			processUserSchema(recordId, user);
			return;
		}

		log.error("Should have returned from applySchema:", newSecurity);
		throw new AccessControlException(
				"No security role or user provided by schema.");		
	}
	
	private void processUserSchema(String recordId, String username)
			throws AccessControlException {
		List<User> users = hibernateService.getUsers(recordId);
		for (User user:users) {
			if (user.getUsername().equalsIgnoreCase(username)) {
				throw new AccessControlException("Duplicate! That user has "
						+ "already been applied to this record.");
			}
		}
		if (users.isEmpty()) {
			hibernateService.createOrGetRecord(recordId);
		}
		hibernateService.grantUserAccess(recordId, username);
	}
	
	private void processRoleSchema(String recordId, String role)
			throws AccessControlException {
		List<Role> roles = hibernateService.getRoles(recordId);
		for (Role curRole:roles) {
			if (curRole.getRole().equalsIgnoreCase(role)) {
				throw new AccessControlException("Duplicate! That role has "
						+ "already been applied to this record.");
			}
		}
		if (roles.isEmpty()) {
			hibernateService.createOrGetRecord(recordId);
		}
		hibernateService.grantRoleAccess(recordId, role);
	}

	/**
	 * Remove a security implementation. The schema will already have a recordId
	 * as a property.
	 * 
	 * @param oldSecurity
	 *            The schema to remove.
	 * @throws AccessControlException
	 *             if removal of the schema fails.
	 */
	@Override
	public void removeSchema(AccessControlSchema oldSecurity)
			throws AccessControlException {
	    log.debug("Removing schema...");
		String recordId = oldSecurity.getRecordId();
		if (StringUtils.isBlank(recordId)) {
			throw new AccessControlException("No record provided by schema.");
		}
		String role = oldSecurity.get("role");
		if (!StringUtils.isBlank(role)) {
			removeRole(recordId, role);
			return;
		}
		String user = oldSecurity.get("user");		
		if (!StringUtils.isBlank(user)) {
			removeUser(recordId, user);
			return;
		}
		throw new AccessControlException("No security role/user provided by schema.");
	}
	
	private void removeUser(String recordId, String user) throws AccessControlException {
		// Retrieve current data
		User curUser = hibernateService.getUser(recordId, user);
		if (curUser == null) {
			throw new AccessControlException("That user does not have access to this record.");		
		}
		try {
			hibernateService.revokeUserAccess(curUser);
		} catch (Exception ex) {
			log.error("Error updating security database: ", ex);
			throw new AccessControlException("Error updating security database");
		}				
	}

	private void removeRole(String recordId, String role) throws AccessControlException {
		// Retrieve current data
		Role curRole = hibernateService.getRole(recordId, role);
		if (curRole == null) {
			throw new AccessControlException(
					"That role does not have access to this record.");
		}
		// Remove from security database
		try {
			hibernateService.revokeRoleAccess(curRole);
		} catch (Exception ex) {
			log.error("Error updating security database: ", ex);
			throw new AccessControlException("Error updating security database");
		}
	}

	@Override
	public List<String> getRoles(String recordId) throws AccessControlException {
	    log.debug("Getting roles...");
		List<String> role_list = new ArrayList<String>();
		List<Role> roles = hibernateService.getRoles(recordId);
		for (Role role : roles) {
			role_list.add(role.getRole());
		}
		return role_list;
	}

	@Override
	public List<String> getUsers(String recordId) throws AccessControlException {
	    log.debug("Getting users...");
		List<String> user_list = new ArrayList<String>();
		List<User> users = hibernateService.getUsers(recordId);
		for (User user : users) {
			user_list.add(user.getUsername());
		}
		return user_list;
	}

	@Override
	public List<String> getPossibilities(String field)
			throws AccessControlException {
		throw new AccessControlException(
				"Not supported by this plugin. Use any freetext role name.");
	}				
}
