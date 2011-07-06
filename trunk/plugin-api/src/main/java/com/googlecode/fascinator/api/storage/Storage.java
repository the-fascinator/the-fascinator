/* 
 * The Fascinator - Plugin API
 * Copyright (C) 2008-2009 University of Southern Queensland
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
package com.googlecode.fascinator.api.storage;

import java.util.Set;

import com.googlecode.fascinator.api.Plugin;

/**
 * Provides storage, retrieval and management of digital objects and payloads
 * 
 * @author Oliver Lucido
 */
public interface Storage extends Plugin {

    /**
     * Create and return a new digital object to/from the store
     * 
     * @param A string identifier for the new object
     * @return An empty DigitalObject
     * @throws StorageException if there was an error creating the object or the
     *         ID already exists
     */
    public DigitalObject createObject(String oid) throws StorageException;

    /**
     * Gets the object with the specified identifier
     * 
     * @param oid an object identifier
     * @return a DigitalObject
     * @throws StorageException if there was an error instantiating the object
     *         or if the ID does not exist
     */
    public DigitalObject getObject(String oid) throws StorageException;

    /**
     * Removes the specified object from the store
     * 
     * @param oid an object identifier
     * @throws StorageException if there was an error deleting the object or if
     *         the ID does not exist
     */
    public void removeObject(String oid) throws StorageException;

    /**
     * Gets all the objects IDs from the storage
     * 
     * @return List of DigitalObject IDs
     */
    public Set<String> getObjectIdList();
}
