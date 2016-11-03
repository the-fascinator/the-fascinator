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
package com.googlecode.fascinator.common.storage.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.googlecode.fascinator.common.StorageDataUtil;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.PayloadType;
import com.googlecode.fascinator.api.storage.StorageException;

/**
 * Generic DigitalObject implementation
 *
 * @author Oliver Lucido
 */
public class GenericDigitalObject implements DigitalObject {

    /** Logging */
    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory
    .getLogger(GenericDigitalObject.class);

    /** Default metadata label */
    private static String METADATA_LABEL = "The Fascinator Indexer Metadata";

    /** Default metadata payload name */
    private static String METADATA_PAYLOAD = "TF-OBJ-META";

    /** Manifest Map */
    private Map<String, Payload> manifest;

    /** Metadata of the DigitalObject */
    private Properties metadata;

    /** Id of the DigitalObject */
    private String id;

    /** Source id of the DigitalObject */
    private String sourceId;

    /** Key for date created */
    private final String DATE_CREATED = "date_object_created";

    /**
     * Creates a DigitalObject with the specified identifier and no metadata
     *
     * @param id unique identifier
     */
    public GenericDigitalObject(String id) {
        setId(id);
        manifest = new HashMap<String, Payload>();
    }

    /**
     * Get the manifest of the DigitalObject
     *
     * @return Manifest Map
     */
    public Map<String, Payload> getManifest() {
        return manifest;
    }

    /**
     * Gets the unique identifier for this object
     *
     * @return an identifier
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this object
     *
     * @param a String identifier
     */
    @Override
    public void setId(String oid) {
        // Stop assuming IDs are file paths
        // Should be able to remove replace, as each object now has Unique ID
        id = oid.replace("\\", "/");
    }

    /**
     * Gets the Source related to this object
     *
     * @return a payload id
     */
    @Override
    public String getSourceId() {
        return sourceId;
    }

    /**
     * Sets the Source related to this object
     *
     * @param a payload id
     */
    @Override
    public void setSourceId(String pid) {
        sourceId = pid;
    }

    /**
     * Instantiates a properties object from the object's metadata payload.
     *
     * @return A properties object
     */
    @Override
    public Properties getMetadata() throws StorageException {
        if (metadata == null) {
            Map<String, Payload> man = getManifest();
            // log.debug("Generic Manifest : " + man);
            boolean hasMetadataPayload = true;
            if (!man.containsKey(METADATA_PAYLOAD)) {
                // May have been created since object was loaded, try a
                // different method to find it
                hasMetadataPayload = false;
                Set<String> payloadIdList = getPayloadIdList();
                for (String string : payloadIdList) {
                    if (METADATA_PAYLOAD.equals(string)) {
                        hasMetadataPayload = true;
                        break;
                    }
                }
            }

            if (!hasMetadataPayload) {
                Payload payload = createStoredPayload(METADATA_PAYLOAD,
                        IOUtils.toInputStream("# Object Metadata"));
                if (METADATA_PAYLOAD.equals(getSourceId())) {
                    setSourceId(null);
                }
                payload.setType(PayloadType.Annotation);
                payload.setLabel(METADATA_LABEL);
            }

            try {
                Payload metaPayload = man.get(METADATA_PAYLOAD);
                metadata = new Properties();
                InputStream is = metaPayload.open();
                metadata.load(is);
                metadata.setProperty("metaPid", METADATA_PAYLOAD);
                metaPayload.close();
                is.close();
                log.debug("Closed init metadata input Stream");
            } catch (IOException ex) {
                throw new StorageException(ex);
            }
        }
        return metadata;
    }

    /**
     * Gets the payloads related to this object
     *
     * @return list of payload ids
     */
    @Override
    public Set<String> getPayloadIdList() {
        return getManifest().keySet();
    }

    /**
     * Creates a new stored payload on the object
     *
     * @param pid A string identifier
     * @param in An inputStream to the new payload's contents
     * @return a payload
     * @throws StorageException if there was an error creating the payload or
     *             the ID already exists.
     */
    @Override
    public Payload createStoredPayload(String pid, InputStream in)
            throws StorageException {
        GenericPayload payload = createPayload(pid, false);
        payload.setInputStream(in);
        return payload;
    }

    /**
     * Creates a new linked payload on the object
     *
     * @param pid A string identifier
     * @param linkPath A string showing the path to the linked file
     * @return a payload
     * @throws StorageException if there was an error creating the payload or
     *             the ID already exists.
     */
    @Override
    public Payload createLinkedPayload(String pid, String linkPath)
            throws StorageException {
        GenericPayload payload = createPayload(pid, true);
        try {
            payload.setInputStream(new ByteArrayInputStream(linkPath
                    .getBytes("UTF-8")));
        } catch (UnsupportedEncodingException ex) {
            throw new StorageException(ex);
        }
        return payload;
    }

    /**
     * Create payload for the object
     *
     * @param pid a String identifier
     * @param linked A state showing if the payload is linked or stored
     * @return a payload
     * @throws StorageException if there was an error creating the payload or
     *             the ID already exists
     */
    private GenericPayload createPayload(String pid, boolean linked)
            throws StorageException {
        Map<String, Payload> man = getManifest();
        if (man.containsKey(pid)) {
            throw new StorageException("ID '" + pid + "' already exists.");
        }

        GenericPayload payload = new GenericPayload(pid);
        if (getSourceId() == null) {
            payload.setType(PayloadType.Source);
            setSourceId(pid);
        } else {
            payload.setType(PayloadType.Enrichment);
        }
        payload.setLinked(linked);

        man.put(pid, payload);
        return payload;
    }

    /**
     * Gets the payload with the specified identifier
     *
     * @param pid payload identifier
     * @return a payload
     * @throws StorageException if there was an error instantiating the payload
     *             or the ID does not exist.
     */
    @Override
    public Payload getPayload(String pid) throws StorageException {
        Map<String, Payload> man = getManifest();
        if (man.containsKey(pid)) {
            return man.get(pid);
        } else {
            throw new StorageException("ID '" + pid + "' does not exist.");
        }
    }

    /**
     * Remove a payload from the object
     *
     * @param a payload identifier
     * @throws StorageException if there was an error removing the payload
     */
    @Override
    public void removePayload(String pid) throws StorageException {
        Map<String, Payload> man = getManifest();
        if (man.containsKey(pid)) {
            // Close the payload just in case,
            // since we are about to orphan it
            man.get(pid).close();
            man.remove(pid);
        } else {
            throw new StorageException("ID '" + pid + "' does not exist.");
        }
    }

    /**
     * Updates a payload's contents
     *
     * @param pid A string identifier
     * @param in An InputStream to the new contetnts
     * @return the updated payload
     * @throws StorageException if there was an error updating the payload or
     *             the ID doesn't exist.
     */
    @Override
    public Payload updatePayload(String pid, InputStream in)
            throws StorageException {
        GenericPayload payload = (GenericPayload) getPayload(pid);
        payload.setInputStream(in);
        return payload;
    }

    /**
     * Close the object
     *
     * @throws StorageException if there was an error closing the object
     */
    @Override
    public void close() throws StorageException {
        Map<String, Payload> man = getManifest();
        for (String pid : man.keySet()) {
            man.get(pid).close();
        }

        if (metadata != null) {
            if (!man.containsKey(METADATA_PAYLOAD)) {
                throw new StorageException("Metadata payload not found");
            }
            String date_created = (String) metadata.get(DATE_CREATED);
            if (date_created == null) {
                date_created = new DateTime().toString();
                metadata.put(DATE_CREATED, date_created);
            }
            try {
                ByteArrayOutputStream metaOut = new ByteArrayOutputStream();
                metadata.store(metaOut, METADATA_LABEL);
                InputStream in = new ByteArrayInputStream(metaOut.toByteArray());
                updatePayload(METADATA_PAYLOAD, in);
                in.close();
                log.info("Closed metadata inputstream");
            } catch (IOException ex) {
                throw new StorageException(ex);
            }
        }
    }

    /**
     * Get the id of the DigitalObject
     *
     * @return id of the DigitalObject
     */
    @Override
    public String toString() {
        return getId();
    }
}
