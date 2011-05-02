/* 
 * The Fascinator - Common Library
 * Copyright (C) 2008 University of Southern Queensland
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
package au.edu.usq.fascinator.common.storage.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.MimeTypeUtil;

/**
 * Generic Payload implementation
 * 
 * @author Oliver Lucido
 */
public class GenericPayload implements Payload {

    /** Logging */
    private static Logger log = LoggerFactory.getLogger(GenericPayload.class);

    /** Payload type */
    private PayloadType type;

    /** Payload storage */
    private boolean linked = false;

    /** Identifier */
    private String id;

    /** Descriptive label */
    private String label;

    /** Content (MIME) type */
    private String contentType;

    /** Input stream to read content data from */
    private InputStream inputStream;

    /** In memory storage for generic implementations */
    private byte[] ramStore;

    /** Input stream to read content data from */
    private boolean metaChanged = false;

    /**
     * Creates an empty payload
     * 
     * @param id an identifier
     */
    public GenericPayload(String id) {
        setId(id);
    }

    /**
     * Creates a data payload with the specified identifier, label and content
     * type, but no content stream
     * 
     * @param id an identifier
     * @param label a descriptive label
     * @param contentType the content type
     */
    public GenericPayload(String id, String label, String contentType) {
        this(id, label, contentType, null);
    }

    /**
     * Creates a data payload with the specified identifier, label, content type
     * and payload type but no content stream
     * 
     * @param id an identifier
     * @param label a descriptive label
     * @param contentType the content type
     * @param payloadType the payload type
     */
    public GenericPayload(String id, String label, String contentType,
            PayloadType payloadType) {
        setId(id);
        setLabel(label);
        setContentType(contentType);
        setType(payloadType);
        metaChanged = false;
    }

    /**
     * Creates an file based payload
     * 
     * @param id an identifier
     * @param payloadFile the file for the payload
     */
    public GenericPayload(String id, File payloadFile) {
        setId(id);
        setLabel(payloadFile.getPath());
        setContentType(MimeTypeUtil.getMimeType(payloadFile));
        try {
            setInputStream(new FileInputStream(payloadFile));
        } catch (IOException e) {
            log.error("Error accessing input stream during payload creation", e);
        }
        metaChanged = false;
    }

    /**
     * Creates a copy of the specified payload
     * 
     * @param payload payload to copy
     */
    public GenericPayload(Payload payload) {
        if (payload != null) {
            setId(payload.getId());
            setLabel(payload.getLabel());
            setContentType(payload.getContentType());
            setType(payload.getType());
            try {
                setInputStream(payload.open());
            } catch (StorageException e) {
                log.error(
                        "Error accessing input stream during payload creation",
                        e);
            }
        }
        metaChanged = false;
    }

    /**
     * Status of the metadata if it has changed
     * 
     * @return <code>true</code> if metadata has changed, <code>false</code>
     *         otherwise
     */
    public boolean hasMetaChanged() {
        return metaChanged;
    }

    /**
     * Set the metadata changed flag to a specific value.
     * 
     * @param metaChanged
     */
    public void setMetaChanged(boolean metaChanged) {
        this.metaChanged = metaChanged;
    }

    /**
     * Gets the identifier for this payload
     * 
     * @return an identifier
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets the identifier for this payload
     * 
     * @param id A string identifier for this payload
     */
    @Override
    public void setId(String id) {
        id = id.replace("\\", "/");
        metaChanged = metaChanged || (this.id != id);
        this.id = id;
    }

    /**
     * Gets the type of this payload
     * 
     * @return payload type
     */
    @Override
    public PayloadType getType() {
        return type;
    }

    /**
     * Returns whether the file is linked or stored
     * 
     * @return <code>true</code> if linked, <code>false</code> if stored
     */
    @Override
    public boolean isLinked() {
        return linked;
    }

    /**
     * Set the link state
     * 
     * @param newLinked True if linked, otherwise false
     */
    public void setLinked(boolean newLinked) {
        metaChanged = metaChanged || (linked != newLinked);
        linked = newLinked;
    }

    /**
     * Sets the type of this payload
     * 
     * @param A PayloadType
     */
    @Override
    public void setType(PayloadType type) {
        metaChanged = metaChanged || (this.type != type);
        this.type = type;
    }

    /**
     * Gets the descriptive label for this payload
     * 
     * @return a label
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * Sets the descriptive label for this payload
     * 
     * @param a String label for this payload
     */
    @Override
    public void setLabel(String label) {
        metaChanged = metaChanged || (this.label != label);
        this.label = label;
    }

    /**
     * Gets the content (MIME) type for this payload
     * 
     * @return a MIME type
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content (MIME) type for this payload
     * 
     * @param a String MIME type
     */
    @Override
    public void setContentType(String contentType) {
        metaChanged = metaChanged || (this.contentType != contentType);
        this.contentType = contentType;
    }

    /**
     * Gets the input stream to access the content for this payload
     * 
     * @return an input stream
     * @throws IOException if there was an error reading the stream
     */
    @Override
    public InputStream open() throws StorageException {
        inputStream = new ByteArrayInputStream(ramStore);
        return inputStream;
    }

    /**
     * Close the input stream for this payload
     * 
     * @throws StorageException if there was an error closing the stream
     */
    @Override
    public void close() throws StorageException {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ex) {
                // Probably already closed
                log.warn("Error closing input stream", ex);
                // throw new StorageException(ex);
            }
        }
    }

    /**
     * Sets the input stream to access the content for this payload. Note this
     * stores the stream into memory, proper Payload implementations should
     * override this method.
     * 
     * @param in the content input stream
     */
    public void setInputStream(InputStream in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            IOUtils.copy(in, out);
            ramStore = out.toByteArray();
            setContentType(MimeTypeUtil.getMimeType(ramStore, getId()));
        } catch (Exception e) {
            log.error("Failed to copy content to memory", e);
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                log.error("Failed to close content input stream", ioe);
            }
        }
    }

    /**
     * Gets the Payload identifier
     * 
     * @return an identifier
     */
    @Override
    public String toString() {
        return getId();
    }

    /**
     * Return the timestamp when the payload was last modified
     *
     * @returns Long: The last modified date of the payload, or NULL if unknown
     */
    @Override
    public Long lastModified() {
        // No support, should be overwritten by real plugins
        return null;
    }

    /**
     * Return the size of the payload in byte
     *
     * @returns Integer: The file size in bytes, or NULL if unknown
     */
    @Override
    public Long size() {
        // No support, should be overwritten by real plugins
        return null;
    }
}
