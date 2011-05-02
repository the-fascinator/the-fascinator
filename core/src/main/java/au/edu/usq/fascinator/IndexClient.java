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

import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.indexer.SearchRequest;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonObject;
import au.edu.usq.fascinator.common.JsonSimpleConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Index Client class to Re-index the storage
 * 
 * @author Linda Octalina
 * 
 */
public class IndexClient {
    /** Date format **/
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** DateTime format **/
    public static final String DATETIME_FORMAT = DATE_FORMAT + "'T'hh:mm:ss'Z'";

    /** Default indexer type will be used if none defined **/
    private static final String DEFAULT_INDEXER_TYPE = "solr";

    /** Default storage type will be used if none defined **/
    private static final String DEFAULT_STORAGE_TYPE = "file-system";

    /** Logging **/
    private static Logger log = LoggerFactory.getLogger(IndexClient.class);

    /** Configuration file **/
    private File configFile;

    /** JsonConfiguration for the configuration file **/
    private JsonSimpleConfig config;

    /** rules file **/
    private File rulesFile;
    private List<String> rulesList;

    /** Indexer **/
    private Indexer indexer;

    /** Indexed storage and real storage **/
    private Storage storage, realStorage;

    /**
     * IndexClient Constructor
     * 
     * @throws IOException If initialisation fail
     */
    public IndexClient() throws IOException {
        config = new JsonSimpleConfig();
        configFile = JsonSimpleConfig.getSystemFile();
        setSetting();
    }

    /**
     * IndexClient Constructor
     * 
     * @param jsonFile Configuration file
     * @throws IOException If initialisation fail
     */
    public IndexClient(File jsonFile) throws IOException {
        config = new JsonSimpleConfig(jsonFile);
        configFile = jsonFile;
        setSetting();
    }

    /**
     * Set the default setting
     */
    public void setSetting() {
        // Get the storage type to be indexed...
        try {
            storage = PluginManager.getStorage(config.getString(
                    DEFAULT_STORAGE_TYPE, "storage", "type"));
            storage.init(configFile);
            indexer = PluginManager.getIndexer(config.getString(
                    DEFAULT_INDEXER_TYPE, "indexer", "type"));
            indexer.init(configFile);
            rulesList = new ArrayList<String>();
            log.info("Loaded {} and {}", realStorage.getName(), indexer
                    .getName());
        } catch (Exception e) {
            log.error("Failed to initialise storage", e);
            return;
        }
    }

    /**
     * Start to run the indexing
     */
    public void run() {
        DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        String now = df.format(new Date());
        long start = System.currentTimeMillis();
        log.info("Started at " + now);

        rulesFile = new File(configFile.getParentFile(), config.getString(
                null, "indexer", "script", "rules"));
        log.debug("rulesFile=" + rulesFile);

        // Check storage for our rules file
        String rulesOid = rulesFile.getAbsolutePath();
        updateRules(rulesOid);

        // List all the DigitalObjects in the storages
        Set<String> objectIdList = realStorage.getObjectIdList();
        for (String objectId : objectIdList) {
            try {
                DigitalObject object = realStorage.getObject(objectId);
                processObject(object, rulesOid,
                        config.getObject("indexer", "params"), false);
            } catch (StorageException ex) {
                log.error("Error getting rules file", ex);
            } catch (IOException ex) {
                log.error("Error Processing object", ex);
            }
        }

        log.info("Completed in "
                + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
    }

    /**
     * Indexing single object
     * 
     * TODO: Might let the user to fill in form in the portal regards to which
     * rules to be used
     * 
     * @param objectId Object Id to be indexed
     */
    public void indexObject(String objectId) {
        DigitalObject object = null;
        try {
            object = realStorage.getObject(objectId);
        } catch (StorageException ex) {
            log.error("Error getting object", ex);
        }

        try {
            Properties sofMeta = object.getMetadata();
            String rulesOid = sofMeta.getProperty("rulesOid");
            if (!rulesList.contains(rulesOid)) {
                updateRules(rulesOid);
                rulesList.add(rulesOid);
            }
            processObject(object, rulesOid, null, true);
        } catch (StorageException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Index objects found in the portal
     * 
     * @param portalQuery Portal query to retrieve the objects to be indexed
     */
    public void indexPortal(String portalQuery) {
        DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        String now = df.format(new Date());
        long start = System.currentTimeMillis();
        log.info("Started at " + now);

        // Get all the records from solr
        int startRow = 0;
        int numPerPage = 5;
        int numFound = 0;
        do {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            SearchRequest request = new SearchRequest("*:*");
            request.addParam("rows", String.valueOf(numPerPage));
            request.addParam("fq", "item_type:\"object\"");
            request.setParam("start", String.valueOf(startRow));

            if (portalQuery != null && !portalQuery.isEmpty()) {
                request.addParam("fq", portalQuery);
            }

            try {
                indexer.search(request, result);
                JsonSimpleConfig js = new JsonSimpleConfig(result.toString());
                for (String oid : js.getStringList("response", "docs", "id")) {
                    DigitalObject object = null;
                    try {
                        object = realStorage.getObject(oid);
                    } catch (StorageException ex) {
                        log.error("Error getting object", ex);
                    }

                    try {
                        Properties sofMeta = object.getMetadata();
                        String rulesOid = sofMeta.getProperty("rulesOid");
                        if (!rulesList.contains(rulesOid)) {
                            updateRules(rulesOid);
                            rulesList.add(rulesOid);
                        }
                        processObject(object, rulesOid, null, false);
                    } catch (StorageException ex) {
                        log.error("Error indexing object", ex);
                    }
                }

                startRow += numPerPage;
                numFound = js.getInteger(0, "response", "numFound");

            } catch (IndexerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (startRow < numFound);

        log.info("Completed in "
                + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
    }

    /**
     * Start to process indexing
     * 
     * @param object Object to be processed
     * @param rulesOid Rule used for indexing the object
     * @param indexerParams Additional parameter for indexing purpose
     * @return Processed object id
     * @throws StorageException If error when retrieveing the object
     * @throws IOException If object file not found
     */
    private String processObject(DigitalObject object, String rulesOid,
            JsonObject indexerParams, boolean commit)
            throws StorageException, IOException {
        String oid = object.getId();
        String sid = null;

        log.info("Processing " + oid + "...");

        try {
            indexer.index(oid);
            if (commit) {
                indexer.commit();
            }
        } catch (IndexerException e) {
            e.printStackTrace();
        }

        return sid;
    }

    /**
     * Main function of IndexClient
     * 
     * @param args Argument list
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            log.info("Usage: index <json-config>");
        } else {
            File jsonFile = new File(args[0]);
            try {
                IndexClient index = new IndexClient(jsonFile);
                index.run();
            } catch (IOException ioe) {
                log.error("Failed to initialise client: {}", ioe.getMessage());
            }
        }
    }

    /**
     * Update the rules object associated with this object
     * 
     * @param rulesOid The rulesOid to update
     */
    private void updateRules(String rulesOid) {
        File externalFile = new File(rulesOid);
        DigitalObject rulesObj = null;

        // Make sure our rules file in storage is up-to-date
        try {
            rulesObj = realStorage.getObject(rulesOid);
            // Update if the external rules file exists
            if (externalFile.exists()) {
                InputStream in = new FileInputStream(externalFile);
                rulesObj.updatePayload(externalFile.getName(), in);
            }

            // InputStream problem
        } catch (FileNotFoundException fex) {
            log.error("Error reading rules file", fex);
            // Doesn't exist, we need to add it
        } catch (StorageException ex) {
            try {
                rulesObj = realStorage.createObject(rulesOid);
                InputStream in = new FileInputStream(externalFile);
                Payload p = rulesObj.createStoredPayload(
                        externalFile.getName(), in);
                p.setLabel("Fascinator Indexing Rules");
            } catch (FileNotFoundException fex) {
                log.error("Error reading rules file", fex);
            } catch (StorageException sex) {
                log.error("Error creating rules object", sex);
            }
        }
    }
}
