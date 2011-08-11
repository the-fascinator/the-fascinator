/* 
 * The Fascinator - Core - Re-Index Client
 * Copyright (C) 2011 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
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
package com.googlecode.fascinator;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.MessagingServices;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>ReIndexClient class to rebuild a system's Solr index from a
 * backed up Storage layer.</p>
 * 
 * <p>Code loosely based on the HarvestClient.</p>
 * 
 * @author Greg Pendlebury
 */
public class ReIndexClient {
    /** Logging */
    private static Logger log = LoggerFactory.getLogger(ReIndexClient.class);

    /** Json configuration */
    private JsonSimpleConfig systemConfig;

    /** Storage to store the digital object */
    private Storage storage;

    /** Messaging services */
    private MessagingServices messaging;

    /** Item configuration cache */
    private Map<String, JsonSimple> harvestConfigs;

    /**
     * ReIndex Client Constructor
     * 
     */
    public ReIndexClient() {
        harvestConfigs = new HashMap();

        // Access Configuration
        try {
            if (systemConfig == null) {
                systemConfig = new JsonSimpleConfig();
            }
        } catch (IOException ex) {
            log.error("Error accessing System Configuration: ", ex);
            return;
        }

        // Find and boot our storage layer
        String storageType = systemConfig.getString(null, "storage", "type");
        if (storageType == null) {
            log.error("No storage configured!");
            return;
        }
        storage = PluginManager.getStorage(storageType);
        if (storage == null) {
            log.error("Storage Plugin '{}' failed to instantiate!",
                    storageType);
            return;
        }
        try {
            storage.init(systemConfig.toString());
            log.info("Storage Plugin '{}' instantiated", storage.getName());
        } catch (PluginException ex) {
            log.error("Error initialising Storage Plugin '{}': ",
                    storage.getName(), ex);
            return;
        }

        // Establish a messaging session
        try {
            messaging = MessagingServices.getInstance();
        } catch (JMSException ex) {
            log.error("Error connecting to messaging broker: ", ex);
            return;
        }

        // Go do all the work we require
        logicLoop();
    }

    /**
     * Main logic loop of the process
     * 
     */
    private void logicLoop() {
        log.info("Rebuild commencing...");
        Set objectList = storage.getObjectIdList();
        if (objectList == null) {
            log.error("Unable to access objects in storage!");
            return;
        }

        int numObjects = objectList.size();
        log.debug("Found {} objects in storage. Index process commencing...", numObjects);
        int i = 0;
        //Object object = objectList.toArray()[0];
        for (Object object : objectList) {
            if (object instanceof String) {
                processObject((String) object);
                i++;
                if (i % 50 == 0) {
                    log.info("{} objects rebuilt...", i);
                }
            } else {
                log.error("Unexpected: Non-String OID! '{}'", object);
            }
        }

        log.info("Rebuild complete...");
    }

    /**
     * Process the provided object OID.
     * 
     * This method will retrieve the object from storage, find its config
     * file and then arrange for it to be re-indexed.
     * 
     * @param oid The Object ID to process.
     */
    private void processObject(String oid) {
        // Get the object from storage
        DigitalObject digitalObject = null;
        try {
            digitalObject = storage.getObject(oid);
        } catch (StorageException ex) {
            log.error("Retrieving OID '{}' failed!: ", oid, ex);
        }

        // Retrieve the key/value metadata list
        Properties metadata = null;
        try {
            metadata = digitalObject.getMetadata();
        } catch (StorageException ex) {
            log.error("Retrieving OID '{}' failed!: ", oid, ex);
        }

        // Find which configuration file should be used
        String configOid = metadata.getProperty("jsonConfigOid");
        if (configOid == null) {
            // Make sure it is not a harvest file... don't want to
            //  fill our log with 'fake' errors. Harvest files are
            //  missing the 'objectId' property.
            String objectIdProp = metadata.getProperty("objectId");
            if (objectIdProp != null) {
                log.error("OID '{}' has no harvest configuration!", oid);
                return;
            } else {
                // Just a harvest file
                return;
            }
        }

        // Get our configuration
        JsonSimple itemConfig = getConfiguration(configOid);
        if (itemConfig == null) {
            log.error("Configuration for '{}' is NULL!");
            return;
        }

        // Now send the object to the harvest queue for transformation and index
        queueHarvest(oid, itemConfig);
    }

    /**
     * Get the requested configuration from storage and parse into a JSON
     * object. Will cache results to lower I/O performance hit.
     * 
     * @param The Object ID of the configuration file to retrieve.
     */
    private JsonSimple getConfiguration(String oid) {
        // Try the cache first
        if (harvestConfigs.containsKey(oid)) {
            return harvestConfigs.get(oid);
        }

        // Get the object from storage
        DigitalObject object = null;
        try {
            object = storage.getObject(oid);
        } catch (StorageException ex) {
            log.error("Failed to retrieve configuration '{}' from storage: ",
                    oid, ex);
            harvestConfigs.put(oid, null);
            return null;
        }

        // Find the source payload
        String pid = object.getSourceId();
        if (pid == null) {
            log.error("Configuration object '()' has no SOURCE payload!", oid);
            harvestConfigs.put(oid, null);
            return null;
        }

        // Get the payload from storage
        Payload payload = null;
        try {
            payload = object.getPayload(pid);
        } catch (StorageException ex) {
            log.error("Failed to retrieve payload '{}' > '{}' from storage",
                    oid, pid);
            log.error("Error stacktrace: ", ex);
            harvestConfigs.put(oid, null);
            return null;
        }

        // Parse into JSON
        InputStream in = null;
        try {
            in = payload.open();
            JsonSimple config = new JsonSimple(in);
            // Cache and return
            harvestConfigs.put(oid, config);
            return config;

        // Parse error
        } catch (IOException ex) {
            log.error("Failed to parse JSON payload '{}' > '{}'",
                    oid, pid);
            log.error("Error stacktrace: ", ex);
            harvestConfigs.put(oid, null);
            return null;

        // Access error
        } catch (StorageException ex) {
            log.error("Failed to open payload '{}' > '{}' from storage",
                    oid, pid);
            log.error("Error stacktrace: ", ex);
            harvestConfigs.put(oid, null);
            return null;

        // Make sure we remember to close the inputstream
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // Do nothing, stream is already closed
            }
        }
    }

    /**
     * Send AMQ Message to schedule indicated object is ready for transformation
     * and indexing.
     * 
     * @param oid The Object ID to send.
     * @param config Item configuration for this object.
     */
    private void queueHarvest(String oid, JsonSimple config) {
        // NOTE: The oid is being updated in memory here
        //       (including in the cache)
        // This OK so long as this process remains single threaded
        //  and only keys that are overwritten EVERY time are used;
        //  like 'oid'.
        JsonObject json = config.getJsonObject();
        json.put("oid", oid);
        try {
            messaging.queueMessage(HarvestQueueConsumer.HARVEST_QUEUE,
                    json.toString());
        } catch(Exception ex) {
            log.error("Failed sending OID '{}' to the harvest message queue!",
                    oid);
            log.error("Error stacktrace: ", ex);
        }
    }

    /**
     * Shutdown ReIndex Client.
     * 
     */
    public void shutdown() {
        if (storage != null) {
            try {
                storage.shutdown();
            } catch (PluginException ex) {
                log.error("Failed to shutdown storage: ", ex);
            }
        }
        if (messaging != null) {
            messaging.release();
        }
    }

    /**
     * Main method for ReIndex Client
     * 
     * @param args Argument list
     */
    public static void main(String[] args) {
        ReIndexClient reindexer = null;
        reindexer = new ReIndexClient();
        reindexer.shutdown();
    }
}
