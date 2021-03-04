/*
 * Hibernate User Attribute
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Attributes for each user.
 * 
 * @author Shilo Banihit
 * 
 */
@Entity
@Table(name = "authuser_attributes")
public class HibernateUserAttribute {
    private Long id;
    private String keyStr;
    private String valStr;
    private HibernateUser user;

    public HibernateUserAttribute() {
    }

    public HibernateUserAttribute(String key, String val) {
        keyStr = key;
        valStr = val;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(nullable = false)
    public String getKeyStr() {
        return keyStr;
    }

    @Column(length = 6000)
    public String getValStr() {
        return valStr;
    }

    public void setKeyStr(String key) {
        keyStr = key;
    }

    public void setValStr(String val) {
        valStr = val;
    }

    @ManyToOne
    public HibernateUser getUser() {
        return user;
    }

    public void setUser(HibernateUser user) {
        this.user = user;
    }
}
