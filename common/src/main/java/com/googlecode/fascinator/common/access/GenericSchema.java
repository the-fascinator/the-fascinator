/*
 * The Fascinator - Generic Schema
 * Copyright (C) 2008-2011 University of Southern Queensland
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

package com.googlecode.fascinator.common.access;

import com.googlecode.fascinator.api.access.AccessControlSchema;
import com.googlecode.fascinator.common.JsonObject;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic security schema, does not define its metadata schema, that is left
 * to extending classes, but creates access methods against an unknown
 * schema.
 *
 * @author Greg Pendlebury
 */
public class GenericSchema implements AccessControlSchema {
    private Logger log = LoggerFactory.getLogger(GenericSchema.class);
    private JsonObject response;
    private String recordId;
    private String securitySource;

    /**
     * Will return a JSON string description of an extending classes'
     * fields.
     *
     * @param property The class field to retrieve
     * @return The value of the property
     */
    @Override
    public final String describeMetadata() {
        // Grab the class name which is extending
        String class_name = this.getClass().getCanonicalName();
        try {
            Class ref_class = Class.forName(class_name);
            Field field_list[] = ref_class.getDeclaredFields();
            response = new JsonObject();

            // Arbitrarily set recordId first, as its a private field
            // of GenericSchema inheriting classes can't access it.
            response.put("recordId", "String");

            for (int i = 0; i < field_list.length; i++) {
                response.put(field_list[i].getName(), field_list[i].getType().getSimpleName());
            }
            return response.toString();
        } catch (ClassNotFoundException ex) {
            log.error(ex.getMessage());
            return "Error retrieving user specification";
        }
    }

    /**
     * Retrieves a given property for this security object.
     *
     * @param property The class field to retrieve
     * @return The value of the property
     */
    @Override
    public final String get(String property) {
        String class_name = this.getClass().getCanonicalName();
        try {
            Class ref_class = Class.forName(class_name);
            Field field_list[] = ref_class.getDeclaredFields();
            for (int i = 0; i < field_list.length; i++) {
                if (property.equals(field_list[i].getName())) {
                    try {
                        if (field_list[i].get(this) == null) {
                            return null;
                        } else {
                            return field_list[i].get(this).toString();
                        }
                    } catch (IllegalArgumentException ex) {
                        log.error("User Object, Illegal argument : {}", ex);
                        return null;
                    } catch (IllegalAccessException ex) {
                        log.error("User Object, Illegal access : {}", ex);
                        return null;
                    }
                }
            }
        } catch (ClassNotFoundException ex) {
            log.error(ex.getMessage());
            return null;
        }

        return null;
    }

    /**
     * Sets a given property for this security object.
     *
     * @param property The class field to retrieve
     * @return The value of the property
     */
    @Override
    public final void set(String property, String value) {
        String class_name = this.getClass().getCanonicalName();
        try {
            Class ref_class = Class.forName(class_name);
            Field field_list[] = ref_class.getDeclaredFields();
            for (int i = 0; i < field_list.length; i++) {
                if (property.equals(field_list[i].getName())) {
                    try {
                        field_list[i].set(this, value);
                    } catch (IllegalArgumentException ex) {
                        log.error("Security Object, Illegal argument : {}", ex);
                    } catch (IllegalAccessException ex) {
                        log.error("Security Object, Illegal access : {}", ex);
                    }
                }
            }
        } catch (ClassNotFoundException ex) {
            log.error(ex.getMessage());
            return;
        }
    }

    @Override
    public void setRecordId(String newid) {
        recordId = newid;
    }

    @Override
    public String getRecordId() {
        return recordId;
    }

    @Override
    public void setSource(String plugin) {
        securitySource = plugin;
    }

    @Override
    public String getSource() {
        return securitySource;
    }
}
