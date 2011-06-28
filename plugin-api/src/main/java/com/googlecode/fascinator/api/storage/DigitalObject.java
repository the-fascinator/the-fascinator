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

import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * Represents an object and its related payloads (or attachments)
 * 
 * @author Oliver Lucido
 */
public interface DigitalObject {

    /**
     * Gets the unique identifier for this object
     *
     * @return an identifier
     */
    public String getId();

    /**
     * Sets the unique identifier for this object
     *
     * @param a String identifier
     */
    public void setId(String oid);

    /**
     * Gets the Source related to this object
     *
     * @return a payload id
     */
    public String getSourceId();

    /**
     * Sets the Source related to this object
     *
     * @param a payload id
     */
    public void setSourceId(String pid);

    /**
     * Instantiates a properties object from the
     * object's metadata payload.
     *
     * @return A properties object
     */
    public Properties getMetadata() throws StorageException;

    /**
     * Gets the payloads related to this object
     *
     * @return list of payload ids
     */
    public Set<String> getPayloadIdList();

    /**
     * Creates a new stored payload on the object
     *
     * @param pid A string identifier
     * @param in An inputStream to the new payload's contents
     * @return a payload
     * @throws StorageException if there was an error creating the payload
     *          or the ID already exists.
     */
    public Payload createStoredPayload(String pid, InputStream in)
            throws StorageException;

    /**
     * Creates a new linked payload on the object
     *
     * @param pid A string identifier
     * @param linkPath A string showing the path to the linked file
     * @return a payload
     * @throws StorageException if there was an error creating the payload
     *          or the ID already exists.
     */
    public Payload createLinkedPayload(String pid, String linkPath)
            throws StorageException;

    /**
     * Gets the payload with the specified identifier
     * 
     * @param pid payload identifier
     * @return a payload
     * @throws StorageException if there was an error instantiating the payload
     *          or the ID does not exist.
     */
    public Payload getPayload(String pid) throws StorageException;

    /**
     * Remove a payload from the object
     * 
     * @param a payload identifier
     * @throws StorageException if there was an error removing the payload
     */
    public void removePayload(String pid) throws StorageException;

    /**
     * Updates a payload's contents
     *
     * @param pid A string identifier
     * @param in An InputStream to the new contetnts
     * @return the updated payload
     * @throws StorageException if there was an error updating the payload
     *          or the ID doesn't exist.
     */
    public Payload updatePayload(String pid, InputStream in)
            throws StorageException;

    /**
     * Close the object
     *
     * @throws StorageException if there was an error closing the object
     */
    public void close() throws StorageException;
}
