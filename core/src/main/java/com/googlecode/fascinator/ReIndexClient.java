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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.messaging.MessagingServices;
import com.googlecode.fascinator.messaging.HarvestQueueConsumer;
import com.googlecode.fascinator.spring.ApplicationContextProvider;

/**
 * <p>
 * ReIndexClient class to rebuild a system's Solr index from a backed up Storage
 * layer.
 * </p>
 *
 * <p>
 * Code loosely based on the HarvestClient.
 * </p>
 *
 * @author Greg Pendlebury
 */
public class ReIndexClient {
    /** The name of the core class inside migration scripts */
    private static String SCRIPT_CLASS_NAME = "MigrateData";

    /** The name of the activation method required on instantiated classes */
    private static String SCRIPT_ACTIVATE_METHOD = "__activate__";

    /** Default tool chain queue */
    private static final String DEFAULT_TOOL_CHAIN_QUEUE = HarvestQueueConsumer.HARVEST_QUEUE;

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

    /** Harvest mapping config */
    private boolean harvestRemap;
    private boolean oldHarvestFiles;
    private boolean failOnMissing;
    private Map<String, String> harvestUpdates;

    /** Migration Script */
    private PyObject migrationScript;

    /** Tool Chain entry queue */
    private String toolChainEntry;

    /**
     * ReIndex Client Constructor
     *
     */
    public ReIndexClient(String scriptMigration, boolean harvestRemap,
            boolean oldHarvestFiles, boolean failOnMissing,
            JsonSimpleConfig config) {
        init(scriptMigration, harvestRemap, oldHarvestFiles, failOnMissing,
                systemConfig);
    }

    public void init(String scriptMigration, boolean harvestRemap,
            boolean oldHarvestFiles, boolean failOnMissing,
            JsonSimpleConfig config) {
        harvestConfigs = new HashMap<String, JsonSimple>();

        // Access Configuration
        try {
            if (config == null) {
                systemConfig = new JsonSimpleConfig();
            } else {
                systemConfig = config;
            }
        } catch (IOException ex) {
            log.error("Error accessing System Configuration: ", ex);
            return;
        }

        // Where are we going to send our messages?
        toolChainEntry = systemConfig.getString(DEFAULT_TOOL_CHAIN_QUEUE,
                "messaging", "toolChainQueue");

        storage = (Storage) ApplicationContextProvider.getApplicationContext()
                .getBean("fascinatorStorage");

        // Establish a messaging session
        try {
            messaging = MessagingServices.getInstance();
        } catch (com.googlecode.fascinator.common.messaging.MessagingException ex) {
            log.error("Error connecting to messaging broker: ", ex);
            return;
        }

        // How are we handling harvest files?
        this.harvestRemap = harvestRemap;
        this.oldHarvestFiles = oldHarvestFiles;
        this.failOnMissing = failOnMissing;
        harvestUpdates = new HashMap<String, String>();

        // Migration Scripting?
        String scriptString = scriptMigration;
        if (scriptString != null && !scriptString.equals("")) {
            migrationScript = evalScript(scriptString);
            if (migrationScript != null) {

                // Make sure our activation method is available
                if (migrationScript
                        .__findattr__(SCRIPT_ACTIVATE_METHOD) == null) {
                    log.error(
                            "Expected method '{}' not found in"
                                    + " migration script!",
                            SCRIPT_ACTIVATE_METHOD);
                    migrationScript = null;
                }

            } else {
                log.error("A migration script has been configured, but there"
                        + " were errors preparing, aborting rebuild!");
                return;
            }
        }

        // Go do all the work we require
        logicLoop();
    }

    /**
     * ReIndex Client Constructor
     *
     */
    public ReIndexClient() {
        harvestConfigs = new HashMap<String, JsonSimple>();

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

        // How are we handling harvest files?
        harvestRemap = systemConfig.getBoolean(false, "restoreTool",
                "harvestRemap", "enabled");
        oldHarvestFiles = systemConfig.getBoolean(false, "restoreTool",
                "harvestRemap", "allowOlder");
        failOnMissing = systemConfig.getBoolean(true, "restoreTool",
                "harvestRemap", "failOnMissing");
        harvestUpdates = new HashMap<String, String>();

        // Migration Scripting?
        String scriptString = systemConfig.getString(null, "restoreTool",
                "migrationScript");
        init(scriptString, harvestRemap, oldHarvestFiles, failOnMissing,
                systemConfig);
    }

    /**
     * Evaluate the requested python script and return the resulting object for
     * later user.
     *
     * @param script The path to the script's location
     * @return PyObject An evaluated PyObject, null for errors
     *
     */
    private PyObject evalScript(String script) {
        // Find the script file
        File file = new File(script);
        if (file == null || !file.exists()) {
            log.error("Could not find script: '{}'", script);
            return null;
        }

        // Open the file for reading
        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(file);
        } catch (Exception ex) {
            log.error("Error accessing script: '{}'", script, ex);
            return null;
        }

        // Run it though an interpreter
        PythonInterpreter python = null;
        try {
            python = PythonInterpreter.threadLocalStateInterpreter(null);
            python.execfile(inStream, "scriptname");
        } catch (Exception ex) {
            log.error("Error evaluating Python script: '{}'", script, ex);
            return null;
        }

        // Get the result and cleanup
        PyObject scriptClass = null;
        try {
            scriptClass = python.get(SCRIPT_CLASS_NAME);
            python.cleanup();
        } catch (Exception ex) {
            log.error("Error accessing class: '{}'", SCRIPT_CLASS_NAME, ex);
            return null;
        }

        // Instantiate and return the result
        return scriptClass.__call__();
    }

    /**
     * Main logic loop of the process
     *
     */
    private void logicLoop() {
        log.info("Rebuild commencing...");
        Set<?> objectList = storage.getObjectIdList();
        if (objectList == null) {
            log.error("Unable to access objects in storage!");
            return;
        }

        log.info(
                "Performing first pass of object list to determine changes that need to be made.");
        firstPass(objectList);
        log.info("First pass complete");
        log.info(
                "Performing second pass. Processing object list to make changes");
        processObjects(objectList);
        log.info("Second pass complete");
        log.info("Rebuild complete...");
    }

    /**
     * First pass processing of objects. This stage is looking for changes that
     * need to be made regarding the mapping of harvest files.
     *
     * @param oids The set of OIDs to process
     */
    private void firstPass(Set<?> oids) {
        int numObjects = oids.size();
        log.info("Found {} objects in storage. Assessing contents...",
                numObjects);
        if (!harvestRemap) {
            log.info("No harvest remapping required in config.");
            return;
        }

        // Prepare some holding variables
        List<String> harvestFiles = new ArrayList<String>();
        Map<String, String> usedHarvestFiles = new HashMap<String, String>();

        // Look through storage and populate them
        for (Object object : oids) {

            if (object instanceof String) {
                String oid = (String) object;
                log.info("First pass processing oid: " + oid);
                assessObject(oid, harvestFiles, usedHarvestFiles);
            } else {
                log.error("Unexpected: Non-String OID! '{}'", object);
            }
        }

        // Now sort out how we need to alter things
        log.info(
                "Checking list of harvest files to see if we have newer versions");
        for (String key : usedHarvestFiles.keySet()) {
            String pid = usedHarvestFiles.get(key);
            log.info("Processing oid: " + key + " pid: " + pid);
            if (pid != null) {
                // Find the version we know was in use
                Payload oldP = getPayload(key, pid);
                // Look for an aternative
                for (String newOid : harvestFiles) {
                    // Make sure we don't find the same one
                    if (!newOid.equals(key)) {
                        Payload newP = getPayload(newOid, pid);
                        // We found one
                        if (newP != null) {
                            // Check it's date
                            if (newP.lastModified() >= oldP.lastModified()) {
                                harvestUpdates.put(key, newOid);
                                // log.debug("'{}' > '{}' ({})", new Object[]
                                // {key, newOid, pid});
                            } else {
                                // Do we allow older harvest files?
                                if (oldHarvestFiles) {
                                    harvestUpdates.put(key, newOid);
                                    // Rejected base on age
                                } else {
                                    log.error(
                                            "Found an older harvest file,"
                                                    + " ignoring: '{}' > '{}'",
                                            newOid, pid);
                                }
                            }
                        }
                    }
                }
            } else {
                log.error(
                        "Errors observed in storage for object(s) using"
                                + " harvest file '{}'. PID should not be null!",
                        key);
            }
        }
        log.info(
                "Completed checking list of harvest files to see if we have newer versions");
    }

    /**
     * A basic loop for the handling of second stage processing.
     *
     * @param oids The set of OIDs to process
     */
    private void processObjects(Set<?> oids) {
        int i = 0;
        for (Object object : oids) {
            if (object instanceof String) {
                String oid = (String) object;
                log.info("Second pass processing oid: " + oid);
                processObject(oid);
                log.info("Second pass processing oid: " + oid + " completed");
                i++;
                if (i % 50 == 0) {
                    log.info("{} objects rebuilt...", i);
                }
            }
        }
    }

    /**
     * Assess the provided object OID. We are looking for whether or not it is a
     * harvest file, or if not what harvest file it is using.
     *
     * @param oid The Object ID to process.
     * @param harvestFiles A List in which to store any found harvest files
     * @param usedHarvestFiles A Map in which to store observed instances of a
     *            harvest file in use
     */
    private void assessObject(String oid, List<String> harvestFiles,
            Map<String, String> usedHarvestFiles) {
        Properties metadata = getMetadata(oid);

        // Config file
        String configOid = metadata.getProperty("jsonConfigOid");
        if (configOid == null) {
            // This is a harvest file
            harvestFiles.add(oid);
            // log.debug("Harvest File: '{}'", oid);
        } else {
            // This is a standard object
            if (!usedHarvestFiles.containsKey(configOid)) {
                String configPid = metadata.getProperty("jsonConfigPid");
                usedHarvestFiles.put(configOid, configPid);
                // log.debug("New Harvest Config File: '{}' > '{}'", configOid,
                // configPid);
            }
        }

        // Rules file
        String rulesOid = metadata.getProperty("rulesOid");
        if (rulesOid != null) {
            // This is a standard object
            if (!usedHarvestFiles.containsKey(rulesOid)) {
                String rulesPid = metadata.getProperty("rulesPid");
                usedHarvestFiles.put(rulesOid, rulesPid);
                // log.debug("New Harvest Rules File: '{}' > '{}'", rulesOid,
                // rulesPid);
            }
        }
    }

    /**
     * Process the provided object OID.
     *
     * This method will retrieve the object from storage, find its config file
     * and then arrange for it to be re-indexed.
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
            return;
        }

        // Retrieve the key/value metadata list
        Properties metadata = null;
        try {
            metadata = digitalObject.getMetadata();
        } catch (StorageException ex) {
            log.error("Retrieving metadata for OID '{}' failed!: ", oid, ex);
            return;
        }

        // Find which configuration file should be used
        String configOid = metadata.getProperty("jsonConfigOid");
        log.info("Checking whether it has a configOid");
        if (configOid == null) {
            // Make sure it is not a harvest file... don't want to
            // fill our log with 'fake' errors. Harvest files are
            // missing the 'objectId' property.
            String objectIdProp = metadata.getProperty("objectId");
            if (objectIdProp != null) {
                log.error("OID '{}' has no harvest configuration!", oid);
                return;
            } else {
                // Just a harvest file
                log.info("It's a harvest file");
                return;
            }
        }
        log.info("It was a configOid: " + configOid);
        // Are we remapping this one?
        if (harvestRemap) {
            String rulesOid = metadata.getProperty("rulesOid");
            // We want a mapping for both files
            if (harvestUpdates.containsKey(configOid)
                    && harvestUpdates.containsKey(rulesOid)) {
                remapHarvestFile(metadata, "jsonConfigOid");
                remapHarvestFile(metadata, "rulesOid");

                // Is a missing harvest file a problem?
            } else {
                if (failOnMissing) {
                    log.error(
                            "Failed to process object '{}'."
                                    + " No mapping exists for harvest file!",
                            oid);
                    return;
                }
                // Otherwise try mapping each alone (they could even both fail)
                remapHarvestFile(metadata, "jsonConfigOid");
                remapHarvestFile(metadata, "rulesOid");
            }
            // If anything was altered it has to be saved
            try {
                digitalObject.close();
            } catch (StorageException ex) {
                log.error("Error saving the object '{}' back to storage: ", oid,
                        ex);
                return;
            }
            // And don't forget to update this variable
            configOid = metadata.getProperty("jsonConfigOid");
        } else {
            log.debug("We are not harvest remapping");
        }

        // Are we running a migration script?
        if (migrationScript != null) {
            // Prepare variables for access
            Map<String, Object> bindings = new HashMap<String, Object>();
            List<String> auditMessages = new ArrayList<String>();
            bindings.put("systemConfig", systemConfig);
            bindings.put("object", digitalObject);
            bindings.put("log", log);
            bindings.put("auditMessages", auditMessages);

            log.info("Running migration script");
            // Execute
            try {
                migrationScript.invoke(SCRIPT_ACTIVATE_METHOD,
                        Py.java2py(bindings));
            } catch (Exception ex) {
                log.error("Error executing migration script"
                        + " against object '{}'", oid, ex);
                return;
            }
            log.info("finished running migration script");
            // Make sure audit entries are sent
            log.info("Sending audit log messages");
            for (String msg : auditMessages) {
                auditLog(oid, msg);
            }
            log.info("Finished sending audit log messages");

            // Sometimes the migration script will alter object contents
            try {
                digitalObject.close();
                log.info("Digital object closed");
            } catch (StorageException ex) {
                log.warn("There is most likely an open File handle"
                        + " left over in the migration script");
                log.error("Error closing object '{}'", oid, ex);
            }

        } else {
            log.info("Not running migration script");
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
     * Remap a harvest file OID stored in the provided properties using the
     * provided key.
     *
     * @param metadata The Properties Object to fix
     * @param key The key storing the harvest file OID
     */
    private void remapHarvestFile(Properties metadata, String key) {
        String oldOid = metadata.getProperty(key);
        if (oldOid == null) {
            return;
        }
        if (!harvestUpdates.containsKey(oldOid)) {
            return;
        }

        String newOid = harvestUpdates.get(oldOid);
        if (newOid == null) {
            return;
        }
        metadata.setProperty(key, newOid);
    }

    /**
     * Retrieve the metadata properties from storage for the request OID.
     *
     * @param oid The Object ID of the object in storage
     * @return Properties An instantiated Properties Object, or null for errors
     */
    private Properties getMetadata(String oid) {
        // Get the object from storage
        DigitalObject digitalObject = null;
        try {
            digitalObject = storage.getObject(oid);
        } catch (StorageException ex) {
            log.error("Retrieving OID '{}' failed!: ", oid, ex);
            return null;
        }

        // Retrieve the key/value metadata list
        try {
            return digitalObject.getMetadata();
        } catch (StorageException ex) {
            log.error("Retrieving metadata for OID '{}' failed!: ", oid, ex);
            return null;
        }
    }

    /**
     * Retrieve a Payload from storage
     *
     * @param oid The Object ID of the object in storage
     * @param pid The Payload ID in the object
     * @return Payload An instantiated Payload Object, or null for errors
     */
    private Payload getPayload(String oid, String pid) {
        // Get the object from storage
        DigitalObject digitalObject = null;
        try {
            digitalObject = storage.getObject(oid);
        } catch (StorageException ex) {
            log.error("Retrieving OID '{}' failed!: ", oid, ex);
            return null;
        }

        // Return the payload... if it exists
        try {
            return digitalObject.getPayload(pid);
        } catch (StorageException ex) {
            return null;
        }
    }

    /**
     * Get the requested configuration from storage and parse into a JSON
     * object. Will cache results to lower I/O performance hit.
     *
     * @param oid The Object ID of the configuration file to retrieve.
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
            log.error("Failed to parse JSON payload '{}' > '{}'", oid, pid);
            log.error("Error stacktrace: ", ex);
            harvestConfigs.put(oid, null);
            return null;

            // Access error
        } catch (StorageException ex) {
            log.error("Failed to open payload '{}' > '{}' from storage", oid,
                    pid);
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
        // (including in the cache)
        // This OK so long as this process remains single threaded
        // and only keys that are overwritten EVERY time are used;
        // like 'oid'.
        log.info("Sending oid: " + oid + " to the harvest queue");
        JsonObject json;
        try {
            json = new JsonSimple(config.toString(false)).getJsonObject();
        } catch (IOException e) {
            json = config.getJsonObject();
        }
        json.put("oid", oid);
        try {
            messaging.queueMessage(toolChainEntry, json.toString());
        } catch (Exception ex) {
            log.error("Failed sending OID '{}' to the harvest message queue!",
                    oid);
            log.error("Error stacktrace: ", ex);
        }
        log.info("Finished sending oid: " + oid + " to the harvest queue");
    }

    /**
     * Send events to subscriber queue for audit logging
     *
     * @param oid Object id
     * @param message Message to send to the log
     * @param context where the event happened
     * @param jsonFile Configuration file
     */
    private void auditLog(String oid, String message) {
        Map<String, String> param = new HashMap<String, String>();
        param.put("oid", oid);
        param.put("eventType", message);
        param.put("username", "system");
        param.put("context", "ReIndexClient");
        try {
            messaging.onEvent(param);
        } catch (MessagingException ex) {
            log.error("Error sending message to audit log: ", ex);
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
