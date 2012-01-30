/*
 * The Fascinator - Plugin - Harvester - Workflows
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
package com.googlecode.fascinator.harvester.workflow;

import com.googlecode.fascinator.api.harvester.HarvesterException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.harvester.impl.GenericHarvester;
import com.googlecode.fascinator.common.storage.StorageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This plugin is a basic harvester for ingesting uploaded content into
 * workflows. It creates the DigitalObject of a source object for the standard
 * harvest/transform/index stack.
 * </p>
 * 
 * <p>
 * A trimmed down version of the file-system harvester but doesn't need
 * recursion or caching.
 * </p>
 * 
 * <h3>Configuration</h3>
 * <p>
 * Sample configuration file for workflow harvester: <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/trunk/plugins/harvester/workflow/src/main/resources/harvest/workflows/workflow-harvester.json"
 * >workflow-harvester.json</a>
 * </p>
 * 
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * Below is the example of workflow stages:
 * 
 * <pre>
 *   "stages": [
 *         {
 *             "name": "pending",
 *             "label": "Pending",
 *             "security": ["metadata", "admin"],
 *             "visibility": ["metadata", "editor", "admin"]
 *         },
 *         {
 *             "name": "metadata",
 *             "label": "Basic Metadata Check",
 *             "security": ["editor", "admin"],
 *             "visibility": ["metadata", "editor", "admin"],
 *             "template": "workflows/basic-init"
 *         },
 *         {
 *             "name": "live",
 *             "label": "Live",
 *             "security": ["editor", "admin"],
 *             "visibility": ["guest"],
 *             "template": "workflows/basic-live"
 *         }
 *     ]
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Rule file</h3>
 * <p>
 * Sample rule file for the workflow harvester: <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/trunk/plugins/harvester/workflow/src/main/resources/harvest/workflows/workflow-harvester.py"
 * >workflow-harvester.py</a>
 * </p>
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * <a href=
 * "https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Plugins/Harvester/Workflow"
 * >https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Plugins/
 * Harvester/Workflow</a>
 * </p>
 * 
 * @author Greg Pendlebury
 */
public class WorkflowHarvester extends GenericHarvester {

    /** logging */
    @SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(WorkflowHarvester.class);

    /** flag for forcing local storage */
    private boolean forceLocalStorage;

    /** flag for forcing local update */
    private boolean forceUpdate;

    /** Render chains */
    private Map<String, Map<String, List<String>>> renderChains;

    /**
     * Basic constructor.
     *
     */
    public WorkflowHarvester() {
        // Just provide GenericHarvester our identity.
        super("workflow-harvester", "Workflow Harvester");
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
        forceLocalStorage = getJsonConfig().getBoolean(true,
                "harvester", "workflow-harvester", "force-storage");
        forceUpdate = getJsonConfig().getBoolean(false,
                "harvester", "workflow-harvester", "force-update");

        // Order is significant
        renderChains = new LinkedHashMap<String, Map<String, List<String>>>();
        JsonObject renderTypes = getJsonConfig().getObject("renderTypes");
        if (renderTypes != null) {
            for (Object name : renderTypes.keySet()) {
                Map<String, List<String>> details = new HashMap<String, List<String>>();
                JsonObject chain = (JsonObject) renderTypes.get(name);
                details.put("fileTypes",
                        JsonSimple.getStringList(chain, "fileTypes"));
                details.put("harvestQueue",
                        JsonSimple.getStringList(chain, "harvestQueue"));
                details.put("indexOnHarvest",
                        JsonSimple.getStringList(chain, "indexOnHarvest"));
                details.put("renderQueue",
                        JsonSimple.getStringList(chain, "renderQueue"));
                renderChains.put((String) name, details);
            }
        }
    }

    /**
     * Gets a list of digital object IDs. If there are no objects, this method
     * should return an empty list, not null.
     *
     * @return a list of object IDs, possibly empty
     * @throws HarvesterException if there was an error retrieving the objects
     */
    @Override
    public Set<String> getObjectIdList() throws HarvesterException {
        return Collections.emptySet();
    }

    /**
     * Get an individual uploaded file as a digital object. For consistency this
     * should be in a list.
     *
     * @return a list of one object ID
     * @throws HarvesterException if there was an error retrieving the objects
     */
    @Override
    public Set<String> getObjectId(File uploadedFile) throws HarvesterException {
        Set<String> objectIds = new HashSet<String>();
        try {
            objectIds.add(createDigitalObject(uploadedFile));
        } catch (StorageException se) {
            throw new HarvesterException(se);
        }
        return objectIds;
    }

    /**
     * Tests whether there are more objects to retrieve. This method should
     * return true if called before getObjects.
     *
     * @return true if there are more objects to retrieve, false otherwise
     */
    @Override
    public boolean hasMoreObjects() {
        return false;
    }

    /**
     * Store the provided file in storage. Ensure proper queue routing is set
     * in object properties.
     *
     * @param file : The file to store
     * @return String : The OID of the stored object
     * @throws HarvesterException : if there was an error accessing storage
     * @throws StorageException : if there was an error writing to storage
     */
    private String createDigitalObject(File file) throws HarvesterException,
            StorageException {
        String objectId;
        DigitalObject object;
        if (forceUpdate) {
            object = StorageUtils.storeFile(getStorage(), file,
                    !forceLocalStorage);
        } else {
            String oid = StorageUtils.generateOid(file);
            String pid = StorageUtils.generatePid(file);
            object = getStorage().createObject(oid);
            if (forceLocalStorage) {
                try {
                    object.createStoredPayload(pid, new FileInputStream(file));
                } catch (FileNotFoundException ex) {
                    throw new HarvesterException(ex);
                }
            } else {
                object.createLinkedPayload(pid, file.getAbsolutePath());
            }

        }
        // update object metadata
        Properties props = object.getMetadata();
        props.setProperty("render-pending", "true");
        props.setProperty("file.path",
                FilenameUtils.separatorsToUnix(file.getAbsolutePath()));
        objectId = object.getId();

        // Store rendition information if we have it
        String ext = FilenameUtils.getExtension(file.getName());
        for (String chain : renderChains.keySet()) {
            Map<String, List<String>> details = renderChains.get(chain);
            if (details.get("fileTypes").contains(ext)) {
                storeList(props, details, "harvestQueue");
                storeList(props, details, "indexOnHarvest");
                storeList(props, details, "renderQueue");
            }
        }

        object.close();
        return objectId;
    }

    /**
     * Take a list of strings from a Java Map, concatenate the values together
     * and store them in a Properties object using the Map's original key.
     * 
     * @param props : Properties object to store into
     * @param details : The full Java Map
     * @param field : The key to use in both objects
     */
    private void storeList(Properties props, Map<String, List<String>> details,
            String field) {
        Set<String> valueSet = new LinkedHashSet<String>();
        // merge with original property value if exists
        String currentValue = props.getProperty(field, "");
        if (!"".equals(currentValue)) {
            String[] currentList = currentValue.split(",");
            valueSet.addAll(Arrays.asList(currentList));
        }
        valueSet.addAll(details.get(field));
        String joinedList = StringUtils.join(valueSet, ",");
        props.setProperty(field, joinedList);
    }
}
