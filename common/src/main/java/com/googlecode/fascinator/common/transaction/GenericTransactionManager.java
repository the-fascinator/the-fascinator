/* 
 * The Fascinator - Generic Transaction Manager
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
package com.googlecode.fascinator.common.transaction;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.transaction.TransactionManager;
import com.googlecode.fascinator.api.transaction.TransactionException;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;

import java.io.File;
import java.io.IOException;

/**
 * <p>A transaction manager will parse incoming JSON messages and inform the
 * TransactionManagerQueueConsumer which Transformers, Subscribers, Indexing
 * and Messaging sould occur in response.</p>
 * 
 * <p>Implementations of this Plugin are expected to execute inside the
 * TransactionManagerQueueConsumer and should confirm to the expected message
 * formats, as per documentation.</p>
 * 
 * @author Greg Pendlebury
 */
public abstract class GenericTransactionManager implements TransactionManager {

    /** Harvester id and harvester name */
    private String id, name;

    /** Config file */
    private JsonSimpleConfig config;

    /**
     * Constructor, standard for plugins
     * 
     * @param id Plugin Id
     * @param name Plugin Name
     */
    public GenericTransactionManager(String id, String name) {
        this.id = id;
        this.name = name;
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
            throw new TransactionException(ioe);
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
            throw new TransactionException(ioe);
        }
    }

    /**
     * Abstract method for Harvester plugin
     * 
     * @throws HarvesterException if there was an error during initialisation
     */
    public abstract void init() throws TransactionException;

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
    public JsonSimpleConfig getJsonConfig() throws TransactionException {
        if (config == null) {
            try {
                config = new JsonSimpleConfig();
            } catch (IOException ioe) {
                throw new TransactionException(ioe);
            }
        }
        return config;
    }

    /**
     * <p>This wrapper implements the API and enforces the practical requirement
     * that the I/O Objects are appropriate JSON classes.</p>
     * 
     * <p>Implementations should overwrite the correctly typed method.</p>
     * 
     * @param message The message to parse, in JSON
     * @return Object The actions to take in response, in JSON
     * @throws TransactionException If an error occurred or the the message is
     * in a bad format or otherwise unsuitable.
     */
    @Override
    public final Object parseMessage(Object message)
            throws TransactionException {
        if (message instanceof JsonSimple) {
            return parseMessage((JsonSimple) message);
        }
        throw new TransactionException(
                "Invalid message format recieved. JsonSimple required.");
    }

    /**
     * <p>This method is expected to be overwritten by implementations. This
     * is where the real processing should occur.</p>
     * 
     * @param message The JsonSimple message to process
     * @return JsonSimple The actions to take in response
     * @throws TransactionException If an error occurred
     */
    public abstract JsonSimple parseMessage(JsonSimple message)
            throws TransactionException;
}
