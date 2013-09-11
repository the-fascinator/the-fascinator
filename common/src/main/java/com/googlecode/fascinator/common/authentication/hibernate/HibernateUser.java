/*
 * Hibernate User
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.authentication.User;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.authentication.GenericUser;

/**
 * Allows properties to be persisted through Hibernate.
 * 
 * @author Shilo Banihit
 * 
 */
@Entity
@Table(name = "authuser", uniqueConstraints = @UniqueConstraint(columnNames = { "username" }))
public class HibernateUser extends GenericUser {
    private final Logger log = LoggerFactory.getLogger(HibernateUser.class);
    private Long id;

    private Map<String, HibernateUserAttribute> attributes;

    public HibernateUser() {
        attributes = new HashMap<String, HibernateUserAttribute>();
    }

    public HibernateUser(User user) {
        this();
        mergeAttributes(user);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    @Column(nullable = false)
    public String getUsername() {
        return super.getUsername();
    }

    @Override
    @Column(nullable = false)
    public String getSource() {
        return super.getSource();
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @MapKey(name = "keyStr")
    public Map<String, HibernateUserAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, HibernateUserAttribute> attr) {
        attributes = attr;
    }

    public void mergeAttributes(User user) {
        try {
            attributes.clear();
            setUsername(user.getUsername());
            setSource(user.getSource());
            String metaDataStr = user.describeMetadata();
            Map<Object, Object> metaMap = new JsonSimple(metaDataStr)
                    .getJsonObject();
            for (Object keyObj : metaMap.keySet()) {
                String keyVal = keyObj.toString();
                String valueType = metaMap.get(keyObj).toString();
                if ("String".equalsIgnoreCase(valueType)
                        && !keyVal.equals("username")) {
                    String value = user.get(keyVal);
                    HibernateUserAttribute attr = new HibernateUserAttribute(
                            keyVal, value);
                    attr.setUser(this);
                    attributes.put(keyVal, attr);
                    set(keyVal, value);
                }
            }
        } catch (IOException e) {
            log.debug("Error:", e);
        }
    }
}
