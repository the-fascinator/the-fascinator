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
package au.edu.usq.fascinator;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.harvester.Harvester;
import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.JsonObject;
import au.edu.usq.fascinator.common.JsonSimple;
import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.common.MessagingServices;
import au.edu.usq.fascinator.common.storage.StorageUtils;

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

import javax.jms.JMSException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * HarvestClient class to handle harvesting of objects to the storage
 * 
 * @author Oliver Lucido
 */
public class HarvestClient {

    /** Date format */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** DateTime format */
    public static final String DATETIME_FORMAT = DATE_FORMAT + "'T'hh:mm:ss'Z'";

    /** Default storage type */
    private static final String DEFAULT_STORAGE_TYPE = "file-system";

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
                config = new JsonSimpleConfig();
            } else {
                config = new JsonSimpleConfig(configFile);
                String rules = config.getString(null,
                        "indexer", "script", "rules");
                rulesFile = new File(configFile.getParent(), rules);
            }
        } catch (IOException ioe) {
            throw new HarvesterException("Failed to read configuration file: '"
                    + configFile + "'", ioe);
        }

        // initialise storage system
        String storageType = config.getString(DEFAULT_STORAGE_TYPE,
                "storage", "type");
        storage = PluginManager.getStorage(storageType);
        if (storage == null) {
            throw new HarvesterException("Storage plugin '" + storageType
                    + "'. Ensure it is in the classpath.");
        }
        try {
            storage.init(config.toString());
            log.info("Loaded {}", storage.getName());
        } catch (PluginException pe) {
            throw new HarvesterException("Failed to initialise storage", pe);
        }

        try {
            messaging = MessagingServices.getInstance();
        } catch (JMSException jmse) {
            log.error("Failed to start connection: {}", jmse.getMessage());
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
        //log.info("=== Check harvest file: '{}'=> '{}'", file.getName(), object);
        if (object != null) {
            // If we got an object back its new or updated
            JsonObject message = new JsonObject();
            message.put("type", "harvest-update");
            message.put("oid", object.getId());
            messaging.queueMessage("houseKeeping", message.toString());
        } else {
            // Otherwise grab the existing object
            String oid = StorageUtils.generateOid(file);
            object = StorageUtils.getDigitalObject(storage, oid);
            //log.info("=== Try again: '{}'=> '{}'", file.getName(), object);
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
            } catch (HarvesterException e) {
                throw new PluginException(e);
            }
        } else {
            // process harvested objects
            do {
                for (String oid : harvester.getObjectIdList()) {
                    processObject(oid);
                }
            } while (harvester.hasMoreObjects());
            // process deleted objects
            do {
                for (String oid : harvester.getDeletedObjectIdList()) {
                    queueDelete(oid, configFile);
                }
            } while (harvester.hasMoreDeletedObjects());
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
     */
    public void reharvest(String oid) throws IOException, PluginException {
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
     */
    public void reharvest(String oid, boolean userPriority) throws IOException,
            PluginException {
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
        if (configOid == null) {
            log.warn("No harvest config for '{}', using defaults...");
            configFile = JsonSimpleConfig.getSystemFile();
        } else {
            log.info("Using config from '{}'", configOid);
            DigitalObject configObj = storage.getObject(configOid);
            Payload payload = configObj.getPayload(configObj.getSourceId());
            configFile = File.createTempFile("reharvest", ".json");
            OutputStream out = new FileOutputStream(configFile);
            IOUtils.copy(payload.open(), out);
            out.close();
            payload.close();
            configObj.close();
            usingTempFile = true;
        }

        // queue for rendering
        queueHarvest(oid, configFile, true, HarvestQueueConsumer.USER_QUEUE);
        log.info("Object '{}' now queued for reindexing...", oid);

        // cleanup
        if (usingTempFile) {
            configFile.delete();
        }
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
     */
    private void processObject(String oid) throws TransformerException,
            StorageException {
        processObject(oid, false);
    }

    /**
     * Process each objects
     * 
     * @param oid Object Id
     * @param commit Flag to commit after indexing
     * @throws StorageException If storage is not found
     * @throws TransformerException If transformer fail to transform the object
     */
    private void processObject(String oid, boolean commit)
            throws TransformerException, StorageException {
        // get the object
        DigitalObject object = storage.getObject(oid);

        // update object metadata
        Properties props = object.getMetadata();
        // TODO - objectId is redundant now?
        props.setProperty("objectId", object.getId());
        props.setProperty("scriptType", config.getString(null,
                "indexer", "script", "type"));
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

        // done with the object
        object.close();

        // put in event log
        sentMessage(oid, "modify");

        // queue the object for indexing
        queueHarvest(oid, configFile, commit);
    }

    /**
     * To queue object to be processed
     * 
     * @param oid Object id
     * @param jsonFile Configuration file
     * @param commit To commit each request to Queue (true) or not (false)
     */
    private void queueHarvest(String oid, File jsonFile, boolean commit) {
        queueHarvest(oid, jsonFile, commit, HarvestQueueConsumer.HARVEST_QUEUE);
    }

    /**
     * To queue object to be processed
     * 
     * @param oid Object id
     * @param jsonFile Configuration file
     * @param commit To commit each request to Queue (true) or not (false)
     * @param queueName Name of the queue to route to
     */
    private void queueHarvest(String oid, File jsonFile, boolean commit,
            String queueName) {
        try {
            JsonObject json = new JsonSimple(jsonFile).getJsonObject();
            json.put("oid", oid);
            if (commit) {
                json.put("commit", "true");
            }
            messaging.queueMessage(queueName, json.toString());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        }
    }

    /**
     * To delete object processing from queue
     * 
     * @param oid Object id
     * @param jsonFile Configuration file
     */
    private void queueDelete(String oid, File jsonFile) {
        try {
            JsonObject json = new JsonSimple(jsonFile).getJsonObject();
            json.put("oid", oid);
            json.put("deleted", "true");
            messaging.queueMessage(HarvestQueueConsumer.HARVEST_QUEUE,
                    json.toString());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
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
    private void sentMessage(String oid, String eventType) {
        Map<String, String> param = new LinkedHashMap<String, String>();
        param.put("oid", oid);
        param.put("eventType", eventType);
        param.put("username", "system");
        param.put("context", "HarvestClient");
        messaging.onEvent(param);
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
