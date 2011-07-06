/* 
 * The Fascinator - Common Library - Generic Harvester
 * Copyright (C) 2008-2011 University of Southern Queensland
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
package com.googlecode.fascinator.common.harvester.impl;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.harvester.Harvester;
import com.googlecode.fascinator.api.harvester.HarvesterException;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.common.JsonSimpleConfig;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Generic Harvester implementation that provides common functionality for
 * subclasses.
 * 
 * @author Oliver Lucido
 */
public abstract class GenericHarvester implements Harvester {

    /** Harvester id and harvester name */
    private String id, name;

    /** Config file */
    private JsonSimpleConfig config;

    /** Storage instance that the Harvester will use to manage objects */
    private Storage storage;

    /**
     * Constructor
     * 
     * @param id Harvester Id
     * @param name Harvester Name
     */
    public GenericHarvester(String id, String name) {
        this.id = id;
        this.name = name;
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
        // By default don't support uploaded files
        throw new HarvesterException(
                "This plugin does not support uploaded files");
    }

    /**
     * Gets a list of deleted digital object IDs. If there are no deleted
     * objects, this method should return an empty list, not null.
     * 
     * @return a list of objects IDs, possibly empty
     * @throws HarvesterException if there was an error retrieving the objects
     */
    @Override
    public Set<String> getDeletedObjectIdList() throws HarvesterException {
        // By default, don't support deleted objects
        return Collections.emptySet();
    }

    /**
     * Tests whether there are more objects to retrieve. This method should
     * return true if called before getObjects.
     * 
     * @return true if there are more objects to retrieve, false otherwise
     */
    @Override
    public boolean hasMoreDeletedObjects() {
        // By default, don't support deleted objects
        return false;
    }

    /**
     * Get storage instance that the Harvester will use to manage objects.
     * 
     * @return storage instance
     * @throws HarvesterException if storage plugin is not set
     */
    public Storage getStorage() throws HarvesterException {
        if (storage == null) {
            throw new HarvesterException("Storage plugin has not been set!");
        }
        return storage;
    }

    /**
     * Sets the Storage instance that the Harvester will use to manage objects.
     * 
     * @param storage a storage instance
     */
    @Override
    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    /**
     * Gets an identifier for Harvester plugin
     * 
     * @return the plugin type id
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Get a name for Harvester plugin
     * 
     * @return the plugin name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets a PluginDescription object relating to this plugin.
     *
     * @return a PluginDescription
     */
    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    /**
     * Initialises the plugin using the specified JSON configuration
     * 
     * @param jsonFile JSON configuration file
     * @throws PluginException if there was an error during initialisation
     */
    @Override
    public void init(File jsonFile) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonFile);
            init();
        } catch (IOException ioe) {
            throw new HarvesterException(ioe);
        }
    }

    /**
     * Initialises the plugin using the specified JSON String
     * 
     * @param jsonFile JSON configuration file
     * @throws PluginException if there was an error during initialisation
     */
    @Override
    public void init(String jsonString) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonString);
            init();
        } catch (IOException ioe) {
            throw new HarvesterException(ioe);
        }
    }

    /**
     * Abstract method for Harvester plugin
     * 
     * @throws HarvesterException if there was an error during initialisation
     */
    public abstract void init() throws HarvesterException;

    /**
     * Shuts down the plugin
     * 
     * @throws PluginException if there was an error during shutdown
     */
    @Override
    public void shutdown() throws PluginException {
        // By default do nothing
    }

    /**
     * Get config file
     * 
     * @return config file
     * @throws HarvesterException if there was an error during retrieval
     */
    public JsonSimpleConfig getJsonConfig() throws HarvesterException {
        if (config == null) {
            try {
                config = new JsonSimpleConfig();
            } catch (IOException ioe) {
                throw new HarvesterException(ioe);
            }
        }
        return config;
    }
}
