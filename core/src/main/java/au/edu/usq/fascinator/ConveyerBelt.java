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
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.api.transformer.Transformer;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.JsonSimple;
import au.edu.usq.fascinator.common.JsonSimpleConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConveyerBelt class to handle transformation of an object
 * 
 * @author Oliver Lucido
 * @author Linda Octalina
 */
public class ConveyerBelt {

    /** Harvest Queue transformer */
    public static final String HARVEST = "harvestQueue";

    /** Render Queue transformer */
    public static final String RENDER = "renderQueue";

    /** Logging */
    private static Logger log = LoggerFactory.getLogger(ConveyerBelt.class);

    /** Queue selector for critical user interface jobs */
    public static final String CRITICAL_USER_SELECTOR = "userPriority";

    /** Json configuration */
    private JsonSimpleConfig sysConfig;

    /** Type of the transformer to be process e.g: harvest or render */
    private String type;

    /** List of plugins to use */
    private Map<String, Transformer> transformers;

    /**
     * Find out what transformers are required to run for
     *  a particular render step.
     *
     * @param object The digital object to transform.
     * @param config The configuration for the particular harvester.
     * @param thisType The type of render chain (step).
     * @param routing Flag if query is for routing. Set this value will force a
     * check for user priority, and then clear the flag if found.
     * @return List<String> A list of names for instantiated transformers.
     */
    public static List<String> getTransformList(DigitalObject object,
            JsonSimpleConfig config, String thisType, boolean routing)
            throws StorageException {
        List<String> plugins = new ArrayList();
        Properties props = object.getMetadata();

        // User initiated event
        if (routing) {
            String user = props.getProperty("userPriority");
            if (user != null && user.equals("true")) {
                log.info("User priority flag set: '{}'", object.getId());
                plugins.add(CRITICAL_USER_SELECTOR);
                props.remove("userPriority");
                object.close();
            }
        }

        // Property data, highest priority
        String pluginList = props.getProperty(thisType);
        if (pluginList != null) {
            // Turn the string into a real list
            for (String plugin : StringUtils.split(pluginList, ",")) {
                plugins.add(StringUtils.trim(plugin));
            }

        } else {
            // The harvester specified none, fallback to the
            //  default list for this harvest source.
            List<String> transformerList = config.getStringList("transformer", thisType);
            if (transformerList != null) {
                for (String entry : transformerList) {
                    plugins.add(StringUtils.trim(entry));
                }
            } else {
                log.info("No transformers configured!");
            }
        }
        return plugins;
    }

    /**
     * Conveyer Belt Constructor
     *
     * @param jsonFile configuration file
     * @param type of transformer
     */
    public ConveyerBelt(String newType) throws TransformerException {
        try {
            sysConfig = new JsonSimpleConfig();
            type = newType;
            // More than meets the eye
            transformers = new LinkedHashMap();
            // Loop through all the system's transformers
            Map<String, JsonSimple> map = sysConfig.getJsonSimpleMap(
                    "transformerDefaults");
            if (map != null && map.size() > 0) {
                for (String name : map.keySet()) {
                    String id = map.get(name).getString(null, "id");
                    if (id != null) {
                        // Instantiate the transformer
                        Transformer transformer =
                                PluginManager.getTransformer(id);
                        if (transformer != null) {
                            try {
                                transformer.init(map.get(name).toString());
                                // Finally, store it for use later
                                transformers.put(name, transformer);
                                log.info("Transformer warmed: '{}'", name);
    
                            } catch (PluginException ex) {
                                throw new TransformerException(ex);
                            }
                        } else {
                            log.info("Transformer not found: '{}'", name);
                        }
                    } else {
                        log.warn("Invalid transformer config, no ID.");
                    }
                }
            } else {
                log.warn("Conveyer Belt instantiated with no Transformers!");
            }
        } catch (IOException ioe) {
            log.warn("Failed to load system configuration", ioe);
        }
    }

    /**
     * Transform digital object based on transformer type
     *
     * @param object The object to be transformed
     * @param config Configuration for this item transformation
     * @return DigitalObject Transformed obect
     * @throws TransformerException if transformation fails
     */
    public DigitalObject transform(DigitalObject object,
            JsonSimpleConfig config) throws TransformerException {
        Map<String, JsonSimple> itemConfigs;
        JsonSimple itemConfig;

        // Get the list of transformers to run
        List<String> pluginList;
        try {
            pluginList = getTransformList(object, config, type, false);
        } catch (StorageException ex) {
            throw new TransformerException(ex);
        }

        // Loop through the list
        if (pluginList != null) {
            for (String name : pluginList) {
                if (transformers.containsKey(name)) {
                    log.info("Starting '{}' on '{}'...", name, object.getId());

                    // Grab any overriding transformer config this item has
                    itemConfigs = config.getJsonSimpleMap(
                            "transformerOverrides");
                    if (itemConfigs != null && itemConfigs.containsKey(name)) {
                        itemConfig = itemConfigs.get(name);
                    } else {
                        itemConfig = new JsonSimple();
                    }

                    // Perform the transformation
                    try {
                        object = transformers.get(name).
                                transform(object, itemConfig.toString());
                        log.info("Finished '{}' on '{}'", name, object.getId());
                    } catch (Exception ex) {
                        log.error("Error during transformer '" + name +
                                "' on object '" + object.getId() + "'", ex);
                    }

                } else {
                    log.error("Transformer not in conveyer belt! '{}'", name);
                }
            }
        }
        return object;
    }
}
