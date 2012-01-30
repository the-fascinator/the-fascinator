/*
 * The Fascinator - Generic User
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

package com.googlecode.fascinator.common.authentication;

import com.googlecode.fascinator.api.authentication.User;
import com.googlecode.fascinator.common.JsonObject;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic user object, does not define its metadata schema, that is left to
 * extending classes, but creates access methods against an unknown schema.
 * 
 * @author Greg Pendlebury
 */
public class GenericUser implements User {
    private Logger log = LoggerFactory.getLogger(GenericUser.class);
    private JsonObject response;
    private String username;
    private String authenticationSource;

    /**
     * Will return a JSON string description of an extending classes' fields.
     * 
     * @param property The class field to retrieve
     * @return The value of the property
     */
    @Override
    public final String describeMetadata() {
        // Grab the class name which is extending
        String class_name = this.getClass().getCanonicalName();
        try {
            Class<?> ref_class = Class.forName(class_name);
            Field field_list[] = ref_class.getDeclaredFields();
            response = new JsonObject();

            // Arbitrarily set username first, as its a private field
            // of GenericUser inheriting classes can't access it.
            response.put("username", "String");

            for (Field element : field_list) {
                response.put(element.getName(), element.getType()
                        .getSimpleName());
            }
            return response.toString();
        } catch (ClassNotFoundException ex) {
            log.error("Error retrieving user specification", ex);
            return "Error retrieving user specification";
        }
    }

    /**
     * Retrieves a given property for this user object.
     * 
     * @param property The class field to retrieve
     * @return The value of the property
     */
    @Override
    public final String get(String property) {
        String class_name = this.getClass().getCanonicalName();
        try {
            Class<?> ref_class = Class.forName(class_name);
            Field field_list[] = ref_class.getDeclaredFields();
            for (Field element : field_list) {
                if (property.equals(element.getName())) {
                    try {
                        if (element.get(this) != null) {
                            return element.get(this).toString();
                        }
                        return null;
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
            log.error("Error accessing user data", ex);
            return null;
        }

        return null;
    }

    /**
     * Sets a given property for this user object.
     * 
     * @param property The class field to retrieve
     * @return The value of the property
     */
    @Override
    public final void set(String property, String value) {
        String class_name = this.getClass().getCanonicalName();
        try {
            Class<?> ref_class = Class.forName(class_name);
            Field field_list[] = ref_class.getDeclaredFields();
            for (Field element : field_list) {
                if (property.equals(element.getName())) {
                    try {
                        element.set(this, value);
                    } catch (IllegalArgumentException ex) {
                        log.error("Security Object, Illegal argument : {}", ex);
                    } catch (IllegalAccessException ex) {
                        log.error("Security Object, Illegal access : {}", ex);
                    }
                }
            }
        } catch (ClassNotFoundException ex) {
            log.error("Error setting user data", ex);
            return;
        }
    }

    /**
     * Retrieves how the user should be shown on-screen.
     * 
     * @return The value of the property
     */
    @Override
    public String realName() {
        return username;
    }

    /**
     * Set user's name
     * 
     * @param newName
     */
    public void setUsername(String newName) {
        username = newName;
    }

    /**
     * Retrieve the user's name
     * 
     * @return The value of user's name
     */
    public String getUsername() {
        return username;
    }

    /**
     * Used by the authentication manager to track the user's origin
     * 
     * @param plugin The id of the authentication plugin
     */
    @Override
    public void setSource(String plugin) {
        authenticationSource = plugin;
    }

    /**
     * Used by the authentication manager to track the user's origin
     * 
     * @return The id of the authentication plugin
     */
    @Override
    public String getSource() {
        return authenticationSource;
    }
}
