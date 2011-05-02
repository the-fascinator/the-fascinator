/*
 * The Fascinator - Plugin - Transformer - Solrmarc
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
package au.edu.usq.fascinator.transformer.solrmarc;

import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.api.transformer.Transformer;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.JsonSimpleConfig;
import java.io.ByteArrayInputStream;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.marc4j.marc.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solrmarc.marc.MarcImporter;

/**
 * Makes use of Solrmarc : http://code.google.com/p/solrmarc/,
 * adapted for use as a library to transform marc data.
 *
 * @author Greg Pendlebury
 */
public class SolrmarcTransformer implements Transformer {

    /** PID for binary marc payload **/
    private static String BINARY_PAYLOAD = "binary.marc";

    /** PID for marc xml payload **/
    private static String XML_PAYLOAD = "marc.xml";

    /** PID for metadata output payload **/
    private static String METADATA_PAYLOAD = "metadata.json";

    /** Logging **/
    private static Logger log = LoggerFactory.getLogger(SolrmarcTransformer.class);

    /** Json config file **/
    private JsonSimpleConfig config;

    /** Solrmarc **/
    private MarcImporter solrmarc;

    /** Flag for first execution */
    private boolean firstRun = true;

    /** Solrmarc configuration path **/
    private String configPath;
    /**
     * Constructor
     */
    public SolrmarcTransformer() {

    }

    /**
     * Init method from file
     *
     * @param jsonFile
     * @throws IOException
     * @throws PluginException
     */
    @Override
    public void init(File jsonFile) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonFile);
            reset();
        } catch (IOException e) {
            throw new PluginException("Error reading config: ", e);
        }
    }

    /**
     * Init method from String
     *
     * @param jsonString
     * @throws IOException
     * @throws PluginException
     */
    @Override
    public void init(String jsonString) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonString);
            reset();
        } catch (IOException e) {
            throw new PluginException("Error reading config: ", e);
        }
    }

    /**
     * Reset the transformer in preparation for a new object
     */
    private void reset() throws TransformerException {
        if (firstRun) {
            firstRun = false;
            // Read solrmarc config
            configPath = config.getString(null, "configPath");
            if (configPath == null) {
                throw new TransformerException(
                        "No valid Solrmarc configuration provided");
            }
            log.debug("Using Solrmarc config: '{}'", configPath);
        }
    }

    /**
     * Transform method
     *
     * @param object : DigitalObject to be transformed
     * @param jsonConfig : String containing configuration for this item
     * @return DigitalObject The object after being transformed
     * @throws TransformerException
     */
    @Override
    public DigitalObject transform(DigitalObject in, String jsonConfig)
            throws TransformerException {
        // Not necessary for this transformer... YET
        //reset();
        //JsonConfigHelper itemConfig;
        //try {
        //    itemConfig = new JsonConfigHelper(jsonConfig);
        //} catch (IOException ex) {
        //    throw new TransformerException("Invalid configuration! '{}'", ex);
        //}

        // Get the payload to transform
        Payload payload = null;
        try {
            payload = in.getPayload(XML_PAYLOAD);
        } catch (StorageException ex) {
            // No marc to transform
            log.error("No MARC data found to transform.");
            return in;
        }

        // Start solrmarc
        try {
            solrmarc = new MarcImporter(payload.open(), configPath);
        } catch (Exception ex) {
            throwEx("Solrmarc failed to read file: ", ex);
        }

        // No record to read?
        if (!solrmarc.hasNext()) {
            throwEx("Could not read the marc data provided", null);
        }

        // Get the parsed record
        Record record = null;
        try {
            record = solrmarc.getNext();
        } catch (Exception ex) {
            throwEx("Error reading marc data: ", ex);
        }

        // Map it to metadata
        String metadata = solrmarc.mapData(record);
        metadata = cleanMetadata(metadata);
        ByteArrayInputStream input = null;
        try {
            input = new ByteArrayInputStream(metadata.getBytes("utf-8"));
        } catch (UnsupportedEncodingException ex) {
            throwEx("Invalid characters: ", ex);
        }

        // Does it exist already?
        try {
            in.getPayload(METADATA_PAYLOAD);
            // Yes, update it
            try {
                in.updatePayload(METADATA_PAYLOAD, input);
            } catch (StorageException ex) {
                throwEx("Error storing payload: ", ex);
            }
        } catch (StorageException ex) {
            // No, time to create
            try {
                in.createStoredPayload(METADATA_PAYLOAD, input);
            } catch (StorageException ex1) {
                throwEx("Error storing payload: ", ex);
            }
        }

        shutdownSolrmarc();
        return in;
    }

    /**
     * Remove control characters from the metadata string.
     *
     * @param metadata The string to clean
     * @return String The cleaned output
     */
    private String cleanMetadata(String metadata) {
        // TODO
        return metadata;
        //Pattern p = Pattern.compile("{cntrl}");
        //Matcher m = p.matcher(metadata);
        //log.debug("Replaced {} control characters.", m.groupCount());
        //return m.replaceAll("");
    }

    /**
     * Brief wrapper to shutdown solrmarc before throwing a transformer
     * exception along with a message from any other type of exception.
     *
     * @param msg : The message to throw in the exception
     * @param ex : The exception to throw
     * @throws TransformerException
     */
    private void throwEx(String msg, Exception ex) throws TransformerException {
        shutdownSolrmarc();
        if (ex == null) {
            throw new TransformerException(msg);
        } else {
            throw new TransformerException(msg, ex);
        }
    }

    /**
     * Tell solrmarc to shutdown and wait for it to finish. With our
     * configuration it should be almost instantaneous.
     *
     */
    private void shutdownSolrmarc() {
        if (solrmarc != null && !solrmarc.shutdownComplete()) {
            solrmarc.shutDown();
            int waitCount = 0;
            // We will wait up to 5s for Solrmarc to close
            while (!solrmarc.shutdownComplete() && waitCount < 5) {
                try {
                    waitCount++;
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    log.error("Shutdown interrupted: ", ex);
                }
            }

            // Double-check the shutdown went well
            if (!solrmarc.shutdownComplete()) {
                log.error("Solrmarc did not shutdown in 5s, giving up.");
            }
        }
    }

    /**
     * Get Transformer ID
     *
     * @return id
     */
    @Override
    public String getId() {
        return "solrmarc";
    }

    /**
     * Get Transformer Name
     *
     * @return name
     */
    @Override
    public String getName() {
        return "Solrmarc Transformer";
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
     * Shut down the transformer plugin
     */
    @Override
    public void shutdown() throws PluginException {

    }
}
