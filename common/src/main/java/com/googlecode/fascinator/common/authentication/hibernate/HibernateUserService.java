/*
 * Hibernate User Service
 * 
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
package com.googlecode.fascinator.common.authentication.hibernate;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.googlecode.fascinator.dao.GenericDao;

/**
 * Hibernate user service.
 * 
 * @author Shilo Banihit
 * 
 * 
 */
@Component(value = "hibernateAuthUserService")
public class HibernateUserService {
    @Autowired
    private GenericDao<HibernateUser, Long> hibernateAuthUserDao;
    @Autowired
    private GenericDao<HibernateUserAttribute, Long> hibernateAuthUserAttributeDao;

    private final Logger log = LoggerFactory
            .getLogger(HibernateUserService.class);

    @Transactional
    public synchronized void addUser(HibernateUser user) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("username", user.getUsername());
        params.put("source", user.getSource());
        List<HibernateUser> users = hibernateAuthUserDao.query(
                "getUserWithSource", params);

        if (users.size() == 0) {
            hibernateAuthUserDao.create(user);
        } else {
            HibernateUser fromdb = users.get(0);
            if (!user.getSource().equalsIgnoreCase(fromdb.getSource())) {
                throw new RuntimeException(
                        "Username exists in the database from a different source");
            } else {
                fromdb.mergeAttributes(user);
                saveUser(fromdb);
            }
        }
    }

    @Transactional
    public synchronized void saveUser(HibernateUser user) {
        hibernateAuthUserDao.update(user);
    }

    @Transactional
    public synchronized HibernateUser getUser(String username) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("username", username);
        List<HibernateUser> users = hibernateAuthUserDao.query("getUser",
                params);
        if (users.size() == 0) {
            return null;
        }
        HibernateUser user = users.get(0);
        user.getAttributes();
        return user;

    }

}
