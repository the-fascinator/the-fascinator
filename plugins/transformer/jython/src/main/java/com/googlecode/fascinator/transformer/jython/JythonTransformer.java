/*
 * The Fascinator - Plugin - Transformer - Jython
 * Copyright (C) 2010-2011  University of Southern Queensland
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
package com.googlecode.fascinator.transformer.jython;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.transformer.Transformer;
import com.googlecode.fascinator.api.transformer.TransformerException;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.PythonUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This plugin provides method for batch processing the DigitalObjects
 * </p>
 * 
 * <h3>Configuration</h3> Standard configuration table:
 * <table border="1">
 * <tr>
 * <th>Option</th>
 * <th>Description</th>
 * <th>Required</th>
 * <th>Default</th>
 * </tr>
 * 
 * <tr>
 * <td>id</td>
 * <td>Transformer Id</td>
 * <td><b>Yes</b></td>
 * <td>jython</td>
 * </tr>
 * 
 * </table>
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * Jython transformer attached to the transformer list in The Fascinator
 * 
 * <pre>
 *      "jython": {
 *             "id": "jython"
 *         }
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * None
 * </p>
 * 
 * 
 * @author Linda Octalina
 */
public class JythonTransformer implements Transformer {
    /** Logger */
    private static Logger log = LoggerFactory
            .getLogger(JythonTransformer.class);

    /** Json config file **/
    private JsonSimpleConfig config;

    private JsonSimple itemConfig;

    private PythonUtils pyUtils;

    /**
     * Extractor Constructor
     */
    public JythonTransformer() {
    }

    /**
     * Overridden method init to initialize
     * 
     * @param jsonString of configuration for Extractor
     * @throws PluginException if fail to parse the config
     */
    @Override
    public void init(String jsonString) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonString);
            reset();
        } catch (IOException e) {
            throw new PluginException(e);
        }
    }

    /**
     * Overridden method init to initialize
     * 
     * @param jsonFile to retrieve the configuration for Extractor
     * @throws PluginException if fail to read the config file
     */
    @Override
    public void init(File jsonFile) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonFile);
            reset();
        } catch (IOException e) {
            throw new PluginException(e);
        }
    }

    /**
     * Reset the transformer in preparation for a new object
     */
    private void reset() throws TransformerException {
        if (pyUtils == null) {
            log.info("--Initializing Extractor plugin--");
            try {
                pyUtils = new PythonUtils(new JsonSimpleConfig());
            } catch (IOException e) {
                log.error("Fail to initialise pyUtils : ", e);
            } catch (PluginException e) {
                log.error("Fail to initialise pyUtils : ", e);
            }
        }
    }

    /**
     * Overridden method getId
     * 
     * @return plugin id
     */
    @Override
    public String getId() {
        return "jython";
    }

    /**
     * Overridden method getName
     * 
     * @return plugin name
     */
    @Override
    public String getName() {
        return "Jython Extractor";
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
     * Overridden method shutdown method
     * 
     * @throws PluginException
     */
    @Override
    public void shutdown() throws PluginException {
    }

    /**
     * Overridden transform method
     * 
     * @param DigitalObject to be processed
     * @return processed DigitalObject with the rdf metadata
     */
    @Override
    public DigitalObject transform(DigitalObject in, String jsonConfig)
            throws TransformerException {
        // Purge old data
        try {
            itemConfig = new JsonSimple(jsonConfig);
        } catch (IOException ex) {
            throw new TransformerException("Invalid configuration provided.");
        }
        reset();

        try {
            Properties props = in.getMetadata();
            //String scriptFile = json.get("transformer/jython/script",
            //        props.getProperty("jythonScript"));
            String scriptFile = props.getProperty("jythonScript");
            if (scriptFile != null) {
                File script = new File(scriptFile);
                if (!script.exists()) {
                    log.error("Script file not found: '{}'", scriptFile);
                    restoreAllProperty(props);
                    return in;
                }

                log.info("Running script: '{}'", scriptFile);
                // Run the script
                PythonInterpreter python = new PythonInterpreter();
                python.set("object", in);
                python.set("config", itemConfig);
                python.set("pyUtils", pyUtils);
                python.set("log", log);
                python.execfile(scriptFile);
                python.cleanup();

            } else {
                log.info("Script file not found");
            }
            restoreAllProperty(props);

        } catch (Exception e) {
            ByteArrayOutputStream eOut = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(eOut));
            String eMsg = eOut.toString();
            log.warn("Failed to run script!\n=====\n{}\n=====", eMsg);
        }
        return in;
    }

    /**
     * Restore all properties
     * 
     * @param props properties of an object
     */
    private void restoreAllProperty(Properties props) {
        restoreProperty(props, "indexOnHarvest");
        restoreProperty(props, "harvestQueue");
        restoreProperty(props, "renderQueue");
        restoreProperty(props, "jythonScript");
    }

    /**
     * Restoring individual property and removing the unnecessary property
     * 
     * @param props properties of an object
     * @param key property to be restored
     */
    private void restoreProperty(Properties props, String key) {
        String copyKey = "copyOf_" + key;
        if (props.containsKey(copyKey)) {
            String copyValue = props.remove(copyKey).toString();
            props.setProperty(key, copyValue);
        } else {
            props.remove(key);
        }
    }
}
