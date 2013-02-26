/*
 * The Fascinator - Plugin API
 * Copyright (C) 2008-2010 University of Southern Queensland
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
package com.googlecode.fascinator.api.access;

import java.util.List;

import com.googlecode.fascinator.api.Plugin;

/**
 * An access control plugin can describe and implement security schemas and
 * supports item level queries of said schemas during index/audit processes.
 * 
 * @author Greg Pendlebury
 */
public interface AccessControl extends Plugin {

    /**
     * Return an empty security schema for the portal to investigate and/or
     * populate.
     * 
     * @return An empty security schema
     */
    public AccessControlSchema getEmptySchema();

    /**
     * Get a list of schemas that have been applied to a record.
     * 
     * @param recordId The record to retrieve information about.
     * @return A list of access control schemas, possibly zero length.
     * @throws AccessControlException if there was an error during retrieval.
     */
    public List<AccessControlSchema> getSchemas(String recordId)
            throws AccessControlException;

    /**
     * Apply/store a new security implementation. The schema will already have a
     * recordId as a property.
     * 
     * @param newSecurity The new schema to apply.
     * @throws AccessControlException if storage of the schema fails.
     */
    public void applySchema(AccessControlSchema newSecurity)
            throws AccessControlException;

    /**
     * Remove a security implementation. The schema will already have a recordId
     * as a property.
     * 
     * @param oldSecurity The schema to remove.
     * @throws AccessControlException if removal of the schema fails.
     */
    public void removeSchema(AccessControlSchema oldSecurity)
            throws AccessControlException;

    /**
     * A basic wrapper for getSchemas() to return just the roles of the schemas.
     * Useful during index and/or audit when this is the only data required.
     * 
     * @param recordId The record to retrieve roles for.
     * @return A list of Strings containing role names.
     * @throws AccessControlException if there was an error during retrieval.
     */
    public List<String> getRoles(String recordId) throws AccessControlException;

    /**
     * A basic wrapper for getSchemas() to return just the users of the schemas.
     * Useful during index and/or audit when this is the only data required.
     * 
     * @param recordId The record to retrieve users for.
     * @return A list of Strings containing user names.
     * @throws AccessControlException if there was an error during retrieval.
     */
    public List<String> getUsers(String recordId) throws AccessControlException;

    /**
     * Retrieve a list of possible field values for a given field if the plugin
     * supports this feature.
     * 
     * @param field The field name.
     * @return A list of String containing possible values
     * @throws AccessControlException if the field doesn't exist or there was an
     *             error during retrieval
     */
    public List<String> getPossibilities(String field)
            throws AccessControlException;

}
