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
package au.edu.usq.fascinator.api.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents content for a digital object
 * 
 * @author Oliver Lucido
 */
public interface Payload {

    /**
     * Gets the identifier for this payload
     * 
     * @return an identifier
     */
    public String getId();

    /**
     * Sets the identifier for this payload
     *
     * @param id A string identifier for this payload
     */
    public void setId(String id);

    /**
     * Gets the type of this payload
     *
     * @return payload type
     */
    public PayloadType getType();

    /**
     * Sets the type of this payload
     *
     * @param A PayloadType
     */
    public void setType(PayloadType type);

    /**
     * Returns whether the file is linked or stored
     *
     * @return boolean value of true (linked) or false (stored)
     */
    public boolean isLinked();

    /**
     * Gets the descriptive label for this payload
     * 
     * @return a label
     */
    public String getLabel();

    /**
     * Sets the descriptive label for this payload
     *
     * @param a String label for this payload
     */
    public void setLabel(String label);

    /**
     * Gets the content (MIME) type for this payload
     * 
     * @return a MIME type
     */
    public String getContentType();

    /**
     * Sets the content (MIME) type for this payload
     *
     * @param a String MIME type
     */
    public void setContentType(String mimeType);

    /**
     * Gets the input stream to access the content for this payload
     * 
     * @return an input stream
     * @throws IOException if there was an error reading the stream
     */
    public InputStream open() throws StorageException;

    /**
     * Close the input stream for this payload
     *
     * @throws StorageException if there was an error closing the stream
     */
    public void close() throws StorageException;

    /**
     * Return the timestamp when the payload was last modified
     *
     * @returns Long: The last modified date of the payload, or NULL if unknown
     */
    public Long lastModified();

    /**
     * Return the size of the payload in byte
     *
     * @returns Long: The file size in bytes, or NULL if unknown
     */
    public Long size();
}
