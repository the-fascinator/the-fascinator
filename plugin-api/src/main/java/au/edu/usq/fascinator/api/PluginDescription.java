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
package au.edu.usq.fascinator.api;

/**
 * Plugins wishing to describe themselves to the fascinator should do so using
 * this class.
 * 
 * @author Greg Pendlebury
 */
public class PluginDescription {
    /** Plugin id */
    private String id;

    /** Plugin name */
    private String name;

    /** Plugin metadata */
    private String metadata;

    /**
     * Plugin Description Constructor
     * 
     * @param plugin Plugin object
     */
    public PluginDescription(Plugin plugin) {
        id = plugin.getId();
        name = plugin.getName();
    }

    /**
     * Get Plugin Id
     * 
     * @return plugin id String
     */
    public String getId() {
        return id;
    }

    /**
     * Get Plugin name
     * 
     * @return plugin name String
     */
    public String getName() {
        return name;
    }

    /**
     * Get plugin metadata
     * 
     * @return plugin metadata String
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * Set plugin metadata
     * 
     * @param newMetadata metadata String
     */
    public void setMetadata(String newMetadata) {
        metadata = newMetadata;
    }
}
