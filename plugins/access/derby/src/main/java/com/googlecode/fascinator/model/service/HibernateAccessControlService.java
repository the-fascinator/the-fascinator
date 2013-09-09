/*
 * Hibernate Access Control Service
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

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.googlecode.fascinator.dao.GenericDao;
import com.googlecode.fascinator.model.Record;
import com.googlecode.fascinator.model.Role;
import com.googlecode.fascinator.model.User;
import com.googlecode.fascinator.model.pk.RolePk;
import com.googlecode.fascinator.model.pk.UserPk;

@Component(value = "hibernateAccessControlService")
public class HibernateAccessControlService {

	/** Logging */
	private final Logger log = LoggerFactory.getLogger(HibernateAccessControlService.class);
	@Autowired
	private GenericDao<User, UserPk> userDao;
	
	@Autowired
	private GenericDao<Role, RolePk> roleDao;
	
	@Autowired
	private GenericDao<Record, String> recordDao;
	
	public Role getRole(String recordId, String role) {
		return roleDao.get(new RolePk(recordId, role));
	}
	
	public User getUser(String recordId, String username) {
		return userDao.get(new UserPk(recordId, username));
	}
	
	@Transactional
	public List<Role> getRoles(String recordId) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("recordId", recordId);
		return roleDao.query("getRoles", params);
	}
	
	@Transactional
	public List<User> getUsers(String recordId) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("recordId", recordId);
		return userDao.query("getUsers", params);
	}
	
	public Record createOrGetRecord(String recordId) {
		Record oldRec = getRecord(recordId);
		if (oldRec != null) {
			return oldRec;
		} else {
			Record newRec = new Record(recordId);
			recordDao.create(newRec);
			return newRec;
		}
	}
	
	public Record getRecord(String recordId) {
		return recordDao.get(recordId);
	}
	
	public User grantUserAccess(String recordId, String username) {
		User user = new User(recordId, username);
		userDao.create(user);
		return user;
	}
	
	@Transactional
	public Role grantRoleAccess(String recordId, String role) {
		Role newRole = new Role(recordId, role);
		roleDao.create(newRole);
		return newRole;
	}
	
	@Transactional
	public void revokeUserAccess(User user) {
		userDao.delete(user);
	}
	
	@Transactional
	public void revokeRoleAccess(Role role) {
		roleDao.delete(role);
	}
}
