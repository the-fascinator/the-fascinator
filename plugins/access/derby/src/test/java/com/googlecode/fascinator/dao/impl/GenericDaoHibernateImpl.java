/*
 * The Fascinator - GenericDaoHibernate
 * Copyright (C) 2008-2010 University of Southern Queensland
 * Copyright (C) 2012 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
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

package com.googlecode.fascinator.dao.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.googlecode.fascinator.dao.GenericDao;

public class GenericDaoHibernateImpl<T, PK extends Serializable> implements
		GenericDao<T, PK> {

	protected Class<T> type;
	protected Map<String, String> queryMap;
	
	@Autowired
	private SessionFactory sessionFactory;

	public GenericDaoHibernateImpl(Class<T> type) {
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	@Transactional 
	public PK create(T o) {
		return (PK) getSession().save(o);
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public T get(PK id) {
		return (T) getSession().get(type, id);
	}

	@Transactional
	public void update(T o) {
		getSession().update(o);
	}

	public void delete(T o) {
		getSession().delete(o);
	}

	private Session getSession() {
		return sessionFactory.getCurrentSession();
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void setQueryMap(Map<String, String> queryMap) {	
		this.queryMap = queryMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public List<T> query(String name, Map<String, Object> properties) {
		return getSession().createQuery(queryMap.get(name)).setProperties(properties).list();
	}
}