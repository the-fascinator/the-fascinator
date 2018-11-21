/*
 * The Fascinator - Core
 * Copyright (C) 2009-2011 University of Southern Queensland
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.harvester.Harvester;
import com.googlecode.fascinator.api.harvester.HarvesterException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.api.transformer.TransformerException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.messaging.MessagingServices;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.messaging.HarvestQueueConsumer;
import com.googlecode.fascinator.spring.ApplicationContextProvider;

/**
 *
 * HarvestClient class to handle harvesting of objects to the storage
 *
 * @author Oliver Lucido
 */
@Component("harvestClient")
public class HarvestClient {

    /** Date format */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** DateTime format */
    public static final String DATETIME_FORMAT = DATE_FORMAT + "'T'hh:mm:ss'Z'";

    /** Default storage type */
    private static final String DEFAULT_STORAGE_TYPE = "file-system";

    /** Default tool chain queue */
    private static final String DEFAULT_TOOL_CHAIN_QUEUE = HarvestQueueConsumer.HARVEST_QUEUE;

    /** Logging */
    private static Logger log = LoggerFactory.getLogger(HarvestClient.class);

    /** Configuration file */
    private File configFile;

    /** Configuration Digital Object */
    private DigitalObject configObject;

    /** Rule file */
    private File rulesFile;

    /** Rule Digital object */
    private DigitalObject rulesObject;

    /** Uploaded file */
    private File uploadedFile;

    /** Uploaded file object id */
    private String uploadedOid;

    /** File owner for the uploaded file */
    private String fileOwner;

    /** Json configuration */
    private JsonSimpleConfig config;

    /** Storage to store the digital object */
    private Storage storage;

    /** Messaging services */
    private MessagingServices messaging;

    /** Tool Chain entry queue */
    private String toolChainEntry;

    /** Harvest id for reports */
    private String harvestId;

    private String repoType;

    private String repoName;

    /**
     * Harvest Client Constructor
     *
     * @throws HarvesterException if fail to initialise
     */
    public HarvestClient() throws HarvesterException {
        this(null, null, null);
    }

    /**
     * Harvest Client Constructor
     *
     * @param configFile configuration file
     * @throws HarvesterException if fail to initialise
     */
    public HarvestClient(File configFile) throws HarvesterException {
        this(configFile, null, null);
    }

    /**
     * Harvest Client Constructor
     *
     * @param configFile Configuration file
     * @param uploadedFile Uploaded file
     * @param owner Owner of the file
     * @throws HarvesterException if fail to initialise
     */
    public HarvestClient(File configFile, File uploadedFile, String owner)
            throws HarvesterException {
        this.configFile = configFile;
        this.uploadedFile = uploadedFile;
        fileOwner = owner;

        try {
            if (configFile == null) {
                config = (JsonSimpleConfig) ApplicationContextProvider
                        .getApplicationContext().getBean("fascinatorConfig");
            } else {
                config = new JsonSimpleConfig(configFile);
                String rules = config.getString(null, "indexer", "script",
                        "rules");
                rulesFile = new File(configFile.getParent(), rules);
            }
        } catch (IOException ioe) {
            throw new HarvesterException(
                    "Failed to read configuration file: '" + configFile + "'",
                    ioe);
        }

        // initialise storage system
        String storageType = config.getString(DEFAULT_STORAGE_TYPE, "storage",
                "type");
        storage = (Storage) ApplicationContextProvider.getApplicationContext()
                .getBean("fascinatorStorage");
        if (storage == null) {
            throw new HarvesterException("Storage plugin '" + storageType
                    + "'. Ensure it is in the classpath.");
        }

        toolChainEntry = config.getString(DEFAULT_TOOL_CHAIN_QUEUE, "messaging",
                "toolChainQueue");

        try {
            messaging = MessagingServices.getInstance();
        } catch (MessagingException ex) {
            log.error("Failed to start connection: {}", ex.getMessage());
        }
    }

    /**
     * Update the harvest file in storage if required
     *
     * @param file The harvest file to store
     * @return DigitalObject The storage object with the file
     * @throws StorageException If storage failed
     */
    private DigitalObject updateHarvestFile(File file) throws StorageException {
        // Check the file in storage
        DigitalObject object = StorageUtils.checkHarvestFile(storage, file);
        // log.info("=== Check harvest file: '{}'=> '{}'", file.getName(),
        // object);
        if (object != null) {
            // If we got an object back its new or updated
            JsonObject message = new JsonObject();
            message.put("type", "harvest-update");
            message.put("oid", object.getId());
            try {
                messaging.queueMessage("houseKeeping", message.toString());
            } catch (MessagingException ex) {
                log.error("Error sending message: ", ex);
            }
        } else {
            // Otherwise grab the existing object
            String oid = StorageUtils.generateOid(file);
            object = StorageUtils.getDigitalObject(storage, oid);
            // log.info("=== Try again: '{}'=> '{}'", file.getName(), object);
        }
        return object;
    }

    /**
     * Start Harvesting Digital objects
     *
     * @throws PluginException If harvest plugin not found
     */
    public void start() throws PluginException {
        DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        String now = df.format(new Date());
        long start = System.currentTimeMillis();
        log.info("Started at " + now);

        // Generate harvest id. This is just a string representation of current
        // date and time
        harvestId = now;

        repoType = config.getString("", "indexer", "params", "repository.type");
        repoName = config.getString("", "indexer", "params", "repository.name");

        // Put in event log
        Map<String, String> startMsgs = new LinkedHashMap<String, String>();
        startMsgs.put("harvestId", harvestId);
        startMsgs.put("repository_type", repoType);
        startMsgs.put("repository_name", repoName);
        sentMessage("-1", "harvestStart", startMsgs);

        // cache harvester config and indexer rules
        configObject = updateHarvestFile(configFile);
        rulesObject = updateHarvestFile(rulesFile);

        // initialise the harvester
        Harvester harvester = null;
        String harvesterType = config.getString(null, "harvester", "type");
        harvester = PluginManager.getHarvester(harvesterType, storage);
        if (harvester == null) {
            throw new HarvesterException("Harvester plugin '" + harvesterType
                    + "'. Ensure it is in the classpath.");
        }
        harvester.init(configFile);
        log.info("Loaded harvester: " + harvester.getName());

        if (uploadedFile != null) {
            // process the uploaded file only
            try {
                Set<String> objectIds = harvester.getObjectId(uploadedFile);
                if (!objectIds.isEmpty()) {
                    uploadedOid = objectIds.iterator().next();
                    processObject(uploadedOid, true);
                }
            } catch (MessagingException e) {
                log.error("Could not queue the object: '{}'", uploadedOid, e);
            } catch (HarvesterException e) {
                throw new PluginException(e);
            }
        } else {
            // process harvested objects
            do {
                for (String oid : harvester.getObjectIdList()) {
                    try {
                        processObject(oid);
                    } catch (MessagingException e) {
                        log.error("Could not queue the object: '{}'", oid, e);
                    }
                }
            } while (harvester.hasMoreObjects());
            // process deleted objects
            do {
                for (String oid : harvester.getDeletedObjectIdList()) {
                    try {
                        queueDelete(oid, configFile);
                    } catch (MessagingException e) {
                        log.error("Could not queue the object: '{}'", oid, e);
                    }
                }
            } while (harvester.hasMoreDeletedObjects());

            // Send harvest end message to event log
            Map<String, String> endMsgs = new LinkedHashMap<String, String>();
            endMsgs.put("harvestId", harvestId);
            endMsgs.put("repository_type", repoType);
            endMsgs.put("repository_name", repoName);
            // endMsgs.put("totalInStorage", getTotal(repoType, repoName));
            sentMessage("-1", "harvestEnd", endMsgs);
        }

        // Shutdown the harvester
        if (harvester != null) {
            harvester.shutdown();
        }

        log.info("Completed in "
                + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");

    }

    /**
     * Reharvest Digital Object when there's a request to reharvest from the
     * portal.
     *
     * @param oid Object Id
     * @throws IOException If necessary files not found
     * @throws PluginException If the harvester plugin not found
     * @throws MessagingException If the object could not be queue'd
     */
    public void reharvest(String oid)
            throws IOException, PluginException, MessagingException {
        reharvest(oid, false);
    }

    /**
     * Reharvest Digital Object when there's a request to reharvest from the
     * portal. The portal can flag items for priority rendering.
     *
     * @param oid Object Id
     * @param userPriority Set flag to have high priority render
     * @throws IOException If necessary files not found
     * @throws PluginException If the harvester plugin not found
     * @throws MessagingException If the object could not be queue'd
     */
    public void reharvest(String oid, boolean userPriority)
            throws IOException, PluginException, MessagingException {
        log.info("Reharvest '{}'...", oid);

        // get the object from storage
        DigitalObject object = storage.getObject(oid);

        // Get/set properties
        Properties props = object.getMetadata();
        props.setProperty("render-pending", "true");
        String configOid = props.getProperty("jsonConfigOid");
        if (userPriority) {
            props.setProperty("userPriority", "true");
        } else {
            props.remove("userPriority");
        }
        object.close();

        // get its harvest config
        boolean usingTempFile = false;
        JsonSimple jsonSimple = null;

        if (configOid == null) {
            log.warn("No harvest config for '{}', using defaults...");
            configFile = JsonSimpleConfig.getSystemFile();
        } else {
            log.info("Using config from '{}'", configOid);
            DigitalObject configObj = storage.getObject(configOid);
            Payload payload = configObj.getPayload(configObj.getSourceId());
            jsonSimple = new JsonSimple(payload.open());
            usingTempFile = true;
        }

        if (usingTempFile) {
            queueHarvest(oid, jsonSimple, true, toolChainEntry);
        } else {
            // queue for rendering
            queueHarvest(oid, configFile, true, toolChainEntry);
        }
        log.info("Object '{}' now queued for reindexing...", oid);

        // cleanup

    }

    public void reharvest(String oid, DigitalObject configObj,
            boolean userPriority)
            throws IOException, PluginException, MessagingException {
        log.info("Reharvest '{}'...", oid);

        // get the object from storage
        DigitalObject object = storage.getObject(oid);

        // Get/set properties
        Properties props = object.getMetadata();
        props.setProperty("render-pending", "true");
        String configOid = props.getProperty("jsonConfigOid");
        if (userPriority) {
            props.setProperty("userPriority", "true");
        } else {
            props.remove("userPriority");
        }
        object.close();

        // get its harvest config

        log.info("Using config from '{}'", configOid);

        Payload payload = configObj.getPayload(configObj.getSourceId());
        configFile = File.createTempFile("reharvest", ".json");
        OutputStream out = new FileOutputStream(configFile);
        IOUtils.copy(payload.open(), out);
        out.close();
        payload.close();
        configObj.close();

        // queue for rendering
        queueHarvest(oid, configFile, true, toolChainEntry);
        log.info("Object '{}' now queued for reindexing...", oid);

        // cleanup
        configFile.delete();

    }

    /**
     * Shutdown Harvester Client. Including: Storage, Message Producer, Session
     * and Connection
     */
    public void shutdown() {
        if (storage != null) {
            try {
                storage.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown storage", pe);
            }
        }
        if (messaging != null) {
            messaging.release();
        }
    }

    /**
     * Process each objects
     *
     * @param oid Object Id
     * @throws StorageException If storage is not found
     * @throws TransformerException If transformer fail to transform the object
     * @throws MessagingException If the object could not be queue'd
     */
    private void processObject(String oid)
            throws TransformerException, StorageException, MessagingException {
        processObject(oid, false);
    }

    /**
     * Process each objects
     *
     * @param oid Object Id
     * @param commit Flag to commit after indexing
     * @throws StorageException If storage is not found
     * @throws TransformerException If transformer fail to transform the object
     * @throws MessagingException If the object could not be queue'd
     */
    private void processObject(String oid, boolean commit)
            throws TransformerException, StorageException, MessagingException {
        // get the object
        DigitalObject object = storage.getObject(oid);

        String isNew = "false";
        String isModified = "false";

        // update object metadata
        Properties props = object.getMetadata();
        // TODO - objectId is redundant now?
        props.setProperty("objectId", object.getId());
        props.setProperty("scriptType",
                config.getString(null, "indexer", "script", "type"));
        // Set our config and rules data as properties on the object
        props.setProperty("rulesOid", rulesObject.getId());
        props.setProperty("rulesPid", rulesObject.getSourceId());
        props.setProperty("jsonConfigOid", configObject.getId());
        props.setProperty("jsonConfigPid", configObject.getSourceId());

        if (fileOwner != null) {
            props.setProperty("owner", fileOwner);
        }
        JsonObject params = config.getObject("indexer", "params");
        for (Object key : params.keySet()) {
            props.setProperty(key.toString(), params.get(key).toString());
        }

        // check this object's status (i.e. new or modified) and count
        if (props.containsKey("isNew")
                && Boolean.parseBoolean(props.getProperty("isNew"))) {
            isNew = "true";
        } else if (props.containsKey("isModified")) {
            if (Boolean.parseBoolean(props.getProperty("isModified"))) {
                isModified = "true";
            }
        }

        // now remove these properties. We don't need them anymore
        props.remove("isNew");
        props.remove("isModified");

        // done with the object
        object.close();

        // put in event log
        Map<String, String> msgs = new LinkedHashMap<String, String>();
        msgs.put("harvestId", harvestId);
        msgs.put("isNew", isNew);
        msgs.put("isModified", isModified);
        msgs.put("repository_type", repoType);
        msgs.put("repository_name", repoName);
        sentMessage(oid, "modify", msgs);

        // queue the object for indexing
        queueHarvest(oid, configFile, commit);
    }

    /**
     * To queue object to be processed
     *
     * @param oid Object id
     * @param jsonFile Configuration file
     * @param commit To commit each request to Queue (true) or not (false)
     * @throws MessagingException if the message could not be sent
     */
    private void queueHarvest(String oid, File jsonFile, boolean commit)
            throws MessagingException {
        queueHarvest(oid, jsonFile, commit, toolChainEntry);
    }

    /**
     * To queue object to be processed
     *
     * @param oid Object id
     * @param jsonFile Configuration file
     * @param commit To commit each request to Queue (true) or not (false)
     * @param queueName Name of the queue to route to
     * @throws MessagingException if the message could not be sent
     */
    private void queueHarvest(String oid, File jsonFile, boolean commit,
            String queueName) throws MessagingException {
        try {
            JsonSimple jsonSimple = new JsonSimple(jsonFile);
            this.queueHarvest(oid, jsonSimple, commit, queueName);
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
            throw new MessagingException(ioe);
        }
    }

    /**
     * To queue object to be processed
     *
     * @param oid Object id
     * @param jsonFile Configuration file
     * @param commit To commit each request to Queue (true) or not (false)
     * @param queueName Name of the queue to route to
     * @throws MessagingException if the message could not be sent
     */
    private void queueHarvest(String oid, JsonSimple jsonSimple, boolean commit,
            String queueName) throws MessagingException {

        JsonObject json = jsonSimple.getJsonObject();
        json.put("oid", oid);
        if (commit) {
            json.put("commit", "true");
        }
        messaging.queueMessage(queueName, json.toString());

    }

    /**
     * To delete object processing from queue
     *
     * @param oid Object id
     * @param jsonFile Configuration file
     * @throws MessagingException if the message could not be sent
     */
    private void queueDelete(String oid, File jsonFile)
            throws MessagingException {
        try {
            JsonObject json = new JsonSimple(jsonFile).getJsonObject();
            json.put("oid", oid);
            json.put("deleted", "true");
            messaging.queueMessage(toolChainEntry, json.toString());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
            throw new MessagingException(ioe);
        }
    }

    /*
     * Useful only for uploaded files.
     *
     * @return The object ID the uploaded file was given by harvester.
     */
    public String getUploadOid() {
        if (uploadedFile == null) {
            return null;
        } else {
            return uploadedOid;
        }
    }

    /**
     * To put events to subscriber queue
     *
     * @param oid Object id
     * @param eventType type of events happened
     * @param context where the event happened
     * @param jsonFile Configuration file
     */
    private void sentMessage(String oid, String eventType,
            Map<String, String> optionalParams) {
        Map<String, String> param = new LinkedHashMap<String, String>();
        param.put("oid", oid);
        param.put("eventType", eventType);
        param.put("username", "system");
        param.put("context", "HarvestClient");

        param.putAll(optionalParams);
        try {
            messaging.onEvent(param);
        } catch (MessagingException ex) {
            log.error("Unable to send message: ", ex);
        }
    }

    /**
     * Main method for Harvest Client
     *
     * @param args Argument list
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            log.info("Usage: harvest <json-config>");
        } else {
            // TODO - http://jira.codehaus.org/browse/MEXEC-37
            // Because of the bug in maven exec spaces in the
            // path will result in incorrect arguements.
            String filePath;
            if (args.length > 1) {
                filePath = StringUtils.join(args, " ");
            } else {
                filePath = args[0];
            }

            File jsonFile = new File(filePath);
            HarvestClient harvest = null;
            try {
                harvest = new HarvestClient(jsonFile);
                harvest.start();
                harvest.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to initialise client: ", pe);
                if (harvest != null) {
                    harvest.shutdown();
                }
            }
        }
    }
}
