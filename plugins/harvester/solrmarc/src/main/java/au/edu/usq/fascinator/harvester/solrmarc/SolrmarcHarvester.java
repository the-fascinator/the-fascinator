/*
 * The Fascinator - Solrmarc Harvester Plugin
 * Copyright (C) 2010-2011 University of Southern Queensland
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
package au.edu.usq.fascinator.harvester.solrmarc;

import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.harvester.impl.GenericHarvester;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.marc4j.marc.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solrmarc.marc.MarcImporter;

/**
 * Makes use of Solrmarc : http://code.google.com/p/solrmarc/,
 * adapted for use as a library to harvest marc data.
 *
 * @author Greg Pendlebury
 */
public class SolrmarcHarvester extends GenericHarvester {

    /** Process marc file in batches */
    private static Integer DEFAULT_BATCH_SIZE = 100;

    /** Limit the number of records imported */
    private static Integer DEFAULT_RECORD_LIMIT = Integer.MAX_VALUE;

    /** PID for binary marc payload **/
    private static String BINARY_PAYLOAD = "binary.marc";

    /** PID for marc xml payload **/
    private static String XML_PAYLOAD = "marc.xml";

    /** PID for metadata output payload **/
    private static String METADATA_PAYLOAD = "metadata.json";

    /** logging */
    private Logger log = LoggerFactory.getLogger(SolrmarcHarvester.class);

    /** Solrmarc **/
    private MarcImporter solrmarc;

    /** Batch size **/
    private int batchSize;

    /** Limit to record processing **/
    private int recordLimit;

    /** Counter for record processing **/
    private int recordCount;

    /** Flag to continue **/
    private boolean hasMore;

    /**
     * Constructor
     */
    public SolrmarcHarvester() {
        // Just provide GenericHarvester our identity.
        super("solrmarc", "Solrmarc Harvester");
    }

    /**
     * Basic init() function. Notice the lack of parameters. This is not part
     * of the Plugin API but from the GenericHarvester implementation. It will
     * be called following the constructor verifies configuration is available.
     *
     * @throws HarvesterException : If there are problems during instantiation
     */
    @Override
    public void init() throws HarvesterException {
        // Read solrmarc config
        String configPath = getJsonConfig().getString(null,
                "harvester", "solrmarc", "configPath");
        if (configPath == null) {
            throw new HarvesterException(
                    "No valid Solrmarc configuration provided");
        }
        log.debug("Using Solrmarc config: '{}'", configPath);

        // Find our marc data to import
        String marcPath = getJsonConfig().getString(null,
                "harvester", "solrmarc", "marcFile");
        if (marcPath == null) {
            throw new HarvesterException("No marc data provided to import");
        }
        File marc = new File(marcPath);
        if (marc == null || !marc.exists()) {
            throw new HarvesterException("The marc file indicated: '" +
                    marcPath + "' does not exist");
        }
        log.debug("FILE SIZE : '{}'", marc.length());
        FileInputStream is = null;
        try {
            is = new FileInputStream(marc);
        } catch (FileNotFoundException ex) {
            throw new HarvesterException("Error reading marc file: ", ex);
        }
        log.debug("MARC File confirmed: '{}'", marcPath);

        // Find the parameters for processing batches etc.
        batchSize = getJsonConfig().getInteger(DEFAULT_BATCH_SIZE,
                "harvester", "solrmarc", "batchSize");
        recordLimit = getJsonConfig().getInteger(DEFAULT_RECORD_LIMIT,
                "harvester", "solrmarc", "limit");
        // In-case '-1' or similar is used to set no limit
        if (recordLimit < 0) {
            recordLimit = DEFAULT_RECORD_LIMIT;
        }
        recordCount = 0;

        // Now start solrmarc
        try {
            solrmarc = new MarcImporter(is, configPath);
        } catch (Exception ex) {
            throw new HarvesterException("Solrmarc failed to initialise: ", ex);
        }
        log.debug("Solrmarc online!");

        // Basic variables
        hasMore = solrmarc.hasNext();
    }

    /**
     * Shutdown the plugin
     *
     * @throws HarvesterException is there are errors
     */
    @Override
    public void shutdown() throws HarvesterException {
        if (solrmarc != null && !solrmarc.shutdownComplete()) {
            log.info("Shutting down Solrmarc");
            solrmarc.shutDown();
            int waitCount = 0;

            // We will wait up to 20s for Solrmarc to close
            while (!solrmarc.shutdownComplete() && waitCount < 20) {
                try {
                    waitCount++;
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    log.error("Shutdown interrupted: ", ex);
                }
            }

            // Double-check the shutdown went well
            if (!solrmarc.shutdownComplete()) {
                log.error("Solrmarc did not shutdown in 20s, giving up.");
            } else {
                log.info("Solrmarc shutdown successfully.");
            }
        }
    }

    /**
     * Harvest the next marc record, and return the Object ID
     *
     * @return Set<String> The next object ID
     * @throws HarvesterException is there are errors
     */
    @Override
    public Set<String> getObjectIdList() throws HarvesterException {
        Set<String> fileObjectIdList = new HashSet<String>();
        if (solrmarc == null) {
            return fileObjectIdList;
        }

        int batchCount = 0;
        while (solrmarc.hasNext() && // STOP if no records left
                batchCount < batchSize && // STOP if batch is finished
                recordCount < recordLimit) { // STOP if we've reached our limit

            String oid = null;
            try {
                Record record = solrmarc.getNext();
                oid = processRecord(record);
            } catch (HarvesterException ex) {
                log.error("Error processing record: ", ex);
            } catch (StorageException ex) {
                log.error("Error updating new object metadata: ", ex);
            } catch (Exception ex) {
                log.error("Solrmarc error reading record: ", ex);
            }

            // Add to the list
            if (oid != null) {
                fileObjectIdList.add(oid);

                if (recordCount % 100 == 0) {
                    log.info("Record {} harvested.", recordCount + 1);
                }

                // Increment counters
                batchCount++;
                recordCount++;
            }
        }

        // Why did we finish?
        hasMore = solrmarc.hasNext();
        if (hasMore && recordCount >= recordLimit) {
            hasMore = false;
        }

        return fileObjectIdList;
    }

    /**
     * Process a record into a digital object
     *
     * @param record The record to process
     * @return String The OID of the new object
     * @throws HarvesterException is there are errors
     * @throws StorageException if the payload metadata failed to update
     */
    private String processRecord(Record record) throws HarvesterException,
            StorageException {
        String oid = DigestUtils.md5Hex(record.getControlNumber());
        DigitalObject object = null;

        // Check if the object is already in storage
        boolean inStorage = true;
        try {
            object = getStorage().getObject(oid);
        } catch (StorageException ex) {
            inStorage = false;
        }

        // New items
        if (!inStorage) {
            try {
                object = getStorage().createObject(oid);
            } catch (StorageException ex) {
                throw new HarvesterException("Error creating new object: ", ex);
            }
            storePayload(object, BINARY_PAYLOAD,
                    getStream(record, BINARY_PAYLOAD));
            storePayload(object, XML_PAYLOAD,
                    getStream(record, XML_PAYLOAD));
            storePayload(object, METADATA_PAYLOAD,
                    getStream(record, METADATA_PAYLOAD));

        // Update an existing item
        } else {
            updateOrCreate(BINARY_PAYLOAD, object, record);
            updateOrCreate(XML_PAYLOAD, object, record);
            updateOrCreate(METADATA_PAYLOAD, object, record);
        }

        // Update object metadata
        Properties props = object.getMetadata();
        props.setProperty("render-pending", "true");
        // We don't want to transform the object on harvest, the rules file
        //  will alter this value later so that re-harvests go through the
        //  transformer.
        props.setProperty("renderQueue", "");
        object.close();
        return object.getId();
    }

    /**
     * Update a payload in the object, or create if it doesn't exist
     *
     * @param pid The Payload id to update/create
     * @param object The object to hold the payload
     * @param record The record to source the data from
     * @throws HarvesterException If unable to do update or create payload
     */
    private void updateOrCreate(String pid, DigitalObject object,
            Record record) throws HarvesterException {
        try {
            // Confirm it exists
            object.getPayload(pid);
            object.updatePayload(pid,
                    getStream(record, pid));
        } catch (StorageException ex) {
            log.error("Existing object '{}' has no payload '{}', adding",
                    object.getId(), pid);
            storePayload(object, pid,
                    getStream(record, pid));
        }
    }

    /**
     * Get the record data from Solrmarc in create an inputstream ready
     * for storage
     *
     * @param record The record to read from Solrmarc
     * @param type The type of output required
     * @return InputStream of the data
     */
    private InputStream getStream(Record record, String type) {
        // Marc data
        if (type.equals(BINARY_PAYLOAD) || type.equals(XML_PAYLOAD)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (type.equals(BINARY_PAYLOAD)) {
                solrmarc.writeBinary(record, out);
            } else {
                solrmarc.writeXml(record, out);
            }
            return new ByteArrayInputStream(out.toByteArray());
        }

        // Metadata mappings
        if (type.equals(METADATA_PAYLOAD)) {
            String metadata = solrmarc.mapData(record);
            try {
                return new ByteArrayInputStream(metadata.getBytes("utf-8"));
            } catch (UnsupportedEncodingException ex) {
                log.error("Error parsing metadata, invalid UTF-8");
                return null;
            }
        }

        return null;
    }

    /**
     * Check if there are more objects to harvest
     *
     * @return <code>true</code> if there are more, <code>false</code> otherwise
     */
    @Override
    public boolean hasMoreObjects() {
        return hasMore;
    }

    /**
     * Delete any marc records requiring removal.
     *
     * @return Set<String> The set of object IDs to delete
     * @throws HarvesterException is there are errors
     */
    @Override
    public Set<String> getDeletedObjectIdList() throws HarvesterException {
        Set<String> fileObjects = new HashSet<String>();
        return fileObjects;
    }

    /**
     * Check if there are more objects to delete
     *
     * @return <code>true</code> if there are more, <code>false</code> otherwise
     */
    @Override
    public boolean hasMoreDeletedObjects() {
        return false;
    }

    /**
     * Store an inputstream as a payload for the given object.
     *
     * @param object The object to store the payload in
     * @param pid The payload ID to use when storing
     * @param in The inputstream to store data from
     * @return Payload The resulting payload stored
     * @throws HarvesterException on storage errors
     */
    private Payload storePayload(DigitalObject object, String pid,
            InputStream in) throws HarvesterException {
        try {
            return object.createStoredPayload(pid, in);
        } catch (StorageException ex) {
            throw new HarvesterException("Error storing payload: ", ex);
        }
    }
}
