/* 
 * The Fascinator - Plugin API
 * Copyright (C) 2008-2009 University of Southern Queensland
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
package com.googlecode.fascinator.api;

import java.io.File;

/**
 * Generic plugin interface
 * 
 * @author Oliver Lucido
 */
public interface Plugin {

    /**
     * Gets an identifier for this type of plugin. This should be a simple name
     * such as "file-system" for a storage plugin, for example.
     * 
     * @return the plugin type id
     */
    public String getId();

    /**
     * Gets a name for this plugin. This should be a descriptive name.
     * 
     * @return the plugin name
     */
    public String getName();

    /**
     * Gets a PluginDescription object relating to this plugin.
     *
     * @return a PluginDescription
     */
    public PluginDescription getPluginDetails();

    /**
     * Initialises the plugin using the specified JSON configuration
     * 
     * @param jsonFile JSON configuration file
     * @throws PluginException if there was an error during initialisation
     */
    public void init(File jsonFile) throws PluginException;

    /**
     * Initalises the plugin using the sepcified JSON String
     * 
     * @param jsonString JSON configuration string
     * @throws PluginException if there was an error during initialisation
     */
    public void init(String jsonString) throws PluginException;

    /**
     * Shuts down the plugin
     * 
     * @throws PluginException if there was an error during shutdown
     */
    public void shutdown() throws PluginException;

}
