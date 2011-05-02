/*
 * The Fascinator - Access Manager
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
package au.edu.usq.fascinator;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.access.AccessControl;
import au.edu.usq.fascinator.api.access.AccessControlException;
import au.edu.usq.fascinator.api.access.AccessControlManager;
import au.edu.usq.fascinator.api.access.AccessControlSchema;
import au.edu.usq.fascinator.common.JsonSimpleConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Management of security.
 * 
 * This object manages one or more access control plugins based on
 * configuration. The portal doesn't need to know the details of talking
 * to each data source.
 * 
 * @author Greg Pendlebury
 */
public class AccessManager implements AccessControlManager {

    /** Default aceess plugin */
    private static final String DEFAULT_ACCESS_PLUGIN = "derby";

    /** Logging */
    private final Logger log = LoggerFactory.getLogger(AccessManager.class);

    /** Map of plugins */
    private Map<String, AccessControl> plugins;

    /** Access control */
    private AccessControl p;

    /** Current Active plugin */
    private String active = null;

    @Override
    public String getId() {
        return "accessmanager";
    }

    @Override
    public String getName() {
        return "Access Manager";
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

    @Override
    public void init(String jsonString) throws AccessControlException {
        try {
            setConfig(new JsonSimpleConfig(jsonString));
        } catch (AccessControlException e) {
            throw new AccessControlException(e);
        } catch (IOException e) {
            throw new AccessControlException(e);
        }
    }

    @Override
    public void init(File jsonFile) throws AccessControlException {
        try {
            setConfig(new JsonSimpleConfig(jsonFile));
        } catch (AccessControlException e) {
            throw new AccessControlException(e);
        } catch (IOException ioe) {
            throw new AccessControlException(ioe);
        }
    }

    /**
     * Read the config file and retrieve the plugin list
     * 
     * @param config JSON Configuration
     * @throws AccessControlException
     */
    public void setConfig(JsonSimpleConfig config)
            throws AccessControlException {
        plugins = new LinkedHashMap<String, AccessControl>();
        // Get and parse the config
        String plugin_string = config.getString(DEFAULT_ACCESS_PLUGIN,
                "accesscontrol", "type");
        String[] plugin_list = plugin_string.split(",");
        // Now start each required plugin
        for (String element : plugin_list) {
            // Get the plugin from the service loader
            AccessControl access = PluginManager.getAccessControl(element);
            // Pass it our config file
            try {
                access.init(config.toString());
            } catch (Exception e) {
                log.error("Failed to initialise access plugin '" + element
                        + "'", e);
                throw new AccessControlException(e);
            }
            plugins.put(element, access);
        }
    }

    @Override
    public void shutdown() throws AccessControlException {
        Iterator i = plugins.values().iterator();
        while (i.hasNext()) {
            p = (AccessControl) i.next();
            try {
                p.shutdown();
            } catch (PluginException e) {
                throw new AccessControlException(e);
            }
        }
    }

    /**
     * Return an empty security schema for the portal to investigate and/or
     * populate.
     * 
     * @return An empty security schema
     */
    @Override
    public AccessControlSchema getEmptySchema() {
        if (active == null) {
            return null;
        } else {
            return plugins.get(active).getEmptySchema();
        }
    }

    /**
     * Get a list of schemas that have been applied to a record.
     * 
     * @param recordId The record to retrieve information about.
     * @return A list of access control schemas, possibly zero length.
     * @throws AccessControlException if there was an error during retrieval.
     */
    @Override
    public List<AccessControlSchema> getSchemas(String recordId)
            throws AccessControlException {
        List<AccessControlSchema> found = new ArrayList();
        if (active != null) {
            found = plugins.get(active).getSchemas(recordId);
        }
        return found;
    }

    /**
     * Apply/store a new security implementation. The schema will already have a
     * recordId as a property.
     * 
     * @param newSecurity The new schema to apply.
     * @throws AccessControlException if storage of the schema fails.
     */
    @Override
    public void applySchema(AccessControlSchema newSecurity)
            throws AccessControlException {
        try {
            plugins.get(active).applySchema(newSecurity);
        } catch (AccessControlException e) {
            throw new AccessControlException(e);
        }
    }

    /**
     * Remove a security implementation. The schema will already have a recordId
     * as a property.
     * 
     * @param oldSecurity The schema to remove.
     * @throws AccessControlException if removal of the schema fails.
     */
    @Override
    public void removeSchema(AccessControlSchema oldSecurity)
            throws AccessControlException {
        try {
            plugins.get(active).removeSchema(oldSecurity);
        } catch (AccessControlException e) {
            throw new AccessControlException(e);
        }
    }

    /**
     * Different from getSchemas() in that is returns just the roles of the
     * schemas, and it queries all plugins, not just the active plugin.
     * 
     * Useful during index and/or audit when this is the only data required.
     * 
     * @param recordId The record to retrieve roles for.
     * @return A list fo Strings containing role names.
     * @throws AccessControlException if there was an error during retrieval.
     */
    @Override
    public List<String> getRoles(String recordId) throws AccessControlException {
        // Test for actual return values found
        Boolean valid = false;
        List<String> found = new ArrayList();
        List<String> result;

        // Loop through each plugin
        Iterator i = plugins.values().iterator();
        while (i.hasNext()) {
            p = (AccessControl) i.next();
            result = p.getRoles(recordId);
            // Null objects mean the plugin
            // has managed this object before
            if (result != null) {
                valid = true;
                // But it could be empty,
                // ie. had access revoked.
                if (result.size() > 0) {
                    found.addAll(result);
                }
            }
        }

        if (valid) {
            return found;
        } else {
            return null;
        }
    }

    /**
     * Retrieve a list of possible field values for a given field if the plugin
     * supports this feature.
     * 
     * @param field The field name.
     * @return A list of String containing possible values
     * @throws AccessControlException if the field doesn't exist or there was an
     *             error during retrieval
     */
    @Override
    public List<String> getPossibilities(String field)
            throws AccessControlException {
        try {
            return plugins.get(active).getPossibilities(field);
        } catch (AccessControlException e) {
            throw new AccessControlException(e);
        }
    }

    /**
     * Specifies which plugin the AccessControl manager should use when managing
     * users. This won't effect reading of data, just writing.
     * 
     * @param pluginId The id of the plugin.
     */
    @Override
    public void setActivePlugin(String pluginId) {
        if (pluginId == null) {
            active = null;
            return;
        }

        // Make sure it exists
        Iterator i = plugins.values().iterator();
        while (i.hasNext()) {
            p = (AccessControl) i.next();
            if (pluginId.equals(p.getId())) {
                active = pluginId;
            }
        }
    }

    /**
     * Return the current active plugin.
     * 
     * @return The currently active plugin.
     */
    @Override
    public String getActivePlugin() {
        return active;
    }

    /**
     * Return the list of plugins being managed.
     * 
     * @return A list of plugins.
     */
    @Override
    public List<PluginDescription> getPluginList() {
        List<PluginDescription> found = new ArrayList();
        PluginDescription result;
        AccessControlSchema schema;

        // Loop through each plugin
        Iterator i = plugins.values().iterator();
        while (i.hasNext()) {
            p = (AccessControl) i.next();
            result = new PluginDescription(p);
            schema = p.getEmptySchema();
            result.setMetadata(schema.describeMetadata());
            found.add(result);
        }

        return found;
    }
}
