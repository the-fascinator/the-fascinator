/*
 * The Fascinator - Solr Event Log Subscriber
 * Copyright (C) 2010-2011 University of Southern Queensland
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
package com.googlecode.fascinator.subscriber.solrEventLog;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.authentication.AuthenticationException;
import com.googlecode.fascinator.api.subscriber.Subscriber;
import com.googlecode.fascinator.api.subscriber.SubscriberException;
import com.googlecode.fascinator.common.JsonSimpleConfig;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * <p>
 * This plugin implements Event log subscriber using security solr 
 * core within the Fascinator.
 * 
 * <h3>Configuration</h3> 
 * <p>Standard configuration table:</p>
 * <table border="1">
 * <tr>
 * <th>Option</th>
 * <th>Description</th>
 * <th>Required</th>
 * <th>Default</th>
 * </tr>
 * 
 * <tr>
 * <td>uri</td>
 * <td>The URI of the Solr event log service</td>
 * <td><b>Yes</b></td>
 * <td>http://localhost:9997/solr/eventlog</td>
 * </tr>
 * 
 * <tr>
 * <td>docLimit</td>
 * <td>Document Limit before commit to Solr</td>
 * <td></td>
 * <td>200</td>
 * </tr>
 * 
 * <tr>
 * <td>sizeLimit</td>
 * <td>Size Limit before commit to Solr</td>
 * <td></td>
 * <td>204800</td>
 * </tr>
 * 
 * <tr>
 * <td>timeLimit</td>
 * <td>Time Limit in minutes before commit to Solr</td>
 * <td></td>
 * <td>30</td>
 * </tr>
 * 
 * </table>
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * Using Solr event subscriber plugin in The Fascinator
 * 
 * <pre>
 *      "subscriber": {
 *      "solr": {
 *          "uri": "http://localhost:9997/solr/eventlog",
 *          "buffer": {
 *              "docLimit" : "200",
 *              "sizeLimit" : "204800",
 *              "timeLimit" : "30"
 *          }
 *      }
 *  }
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
 * @author Linda Octalina
 */

public class SolrEventLogSubscriber implements Subscriber {

    /** Logging */
    private final Logger log = LoggerFactory
            .getLogger(SolrEventLogSubscriber.class);

    /** Buffer Limit : Document count */
    private static Integer BUFFER_LIMIT_DOCS = 200;

    /** Buffer Limit : Size */
    private static Integer BUFFER_LIMIT_SIZE = 1024 * 200;

    /** Buffer Limit : Time */
    private static Integer BUFFER_LIMIT_TIME = 30;

    /** Solr URI */
    private URI uri;

    /** Solr Core */
    private CommonsHttpSolrServer core;

    /** Buffer of documents waiting submission */
    private List<String> docBuffer;

    /** Time the oldest document was written into the buffer */
    private long bufferOldest;

    /** Time the youngest document was written into the buffer */
    private long bufferYoungest;

    /** Total size of documents currently in the buffer */
    private int bufferSize;

    /** Buffer Limit : Number of documents */
    private int bufferDocLimit;

    /** Buffer Limit : Total data size */
    private int bufferSizeLimit;

    /** Buffer Limit : Maximum age of oldest document */
    private int bufferTimeLimit;

    /** Run a timer to check the buffer periodically */
    private Timer timer;

    /** Logging context for timer */
    private String timerMDC;

    /**
     * Gets an identifier for this type of plugin. This should be a simple name
     * such as "file-system" for a storage plugin, for example.
     * 
     * @return the plugin type id
     */
    @Override
    public String getId() {
        return "solr-event-log";
    }

    /**
     * Gets a name for this plugin. This should be a descriptive name.
     * 
     * @return the plugin name
     */
    @Override
    public String getName() {
        return "Solr Event Log Subscriber";
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
     * Initializes the plugin using the specified JSON String
     * 
     * @param jsonString JSON configuration string
     * @throws PluginException if there was an error in initialization
     */
    @Override
    public void init(String jsonString) throws SubscriberException {
        try {
            setConfig(new JsonSimpleConfig(jsonString));
        } catch (IOException e) {
            throw new SubscriberException(e);
        }
    }

    /**
     * Initializes the plugin using the specified JSON configuration
     * 
     * @param jsonFile JSON configuration file
     * @throws SubscriberException if there was an error in initialization
     */
    @Override
    public void init(File jsonFile) throws SubscriberException {
        try {
            setConfig(new JsonSimpleConfig(jsonFile));
        } catch (IOException ioe) {
            throw new SubscriberException(ioe);
        }
    }

    /**
     * Initialization of Solr Access Control plugin
     * 
     * @param config The configuration to use
     * @throws AuthenticationException if fails to initialize
     */
    private void setConfig(JsonSimpleConfig config) throws SubscriberException {
        try {
            // Find our solr index
            uri = new URI(config.getString(null, "subscriber", getId(), "uri"));
            if (uri == null) {
                throw new SubscriberException("No Solr URI provided");
            }
            core = new CommonsHttpSolrServer(uri.toURL());

            // Small sleep whilst the solr index is still coming online
            Thread.sleep(200);
            // Make sure it is online
            core.ping();

            // Buffering
            docBuffer = new ArrayList<String>();
            bufferSize = 0;
            bufferOldest = 0;
            bufferDocLimit = config.getInteger(BUFFER_LIMIT_DOCS,
                    "subscriber", getId(), "buffer", "docLimit");
            bufferSizeLimit = config.getInteger(BUFFER_LIMIT_SIZE,
                    "subscriber", getId(), "buffer", "sizeLimit");
            bufferTimeLimit = config.getInteger(BUFFER_LIMIT_TIME,
                    "subscriber", getId(), "buffer", "timeLimit");

            // Timeout 'tick' for buffer (10s)
            timer = new Timer("SolrEventLog:" + this.toString(), true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    checkTimeout();
                }
            }, 0, 10000);
        } catch (Exception ex) {
            throw new SubscriberException(ex);
        }
    }

    /**
     * Add a new document into the buffer, and check if submission is required
     *
     * @param document : The Solr document to add to the buffer.
     */
    private void addToBuffer(String document) {
        if (timerMDC == null) {
            timerMDC = MDC.get("name");
        }

        int length = document.length();
        // If this is the first document in the buffer, record its age
        bufferYoungest = new Date().getTime();
        if (docBuffer.isEmpty()) {
            bufferOldest = new Date().getTime();
            log.debug("=== New buffer starting: {}", bufferOldest);
        }
        // Add to the buffer
        docBuffer.add(document);
        bufferSize += length;
        // Check if submission is required
        checkBuffer();
    }

    /**
     * Method to fire on timeout() events to ensure buffers don't go stale
     * after the last item in a harvest passes through.
     *
     */
    private void checkTimeout() {
        if (timerMDC != null) {
            MDC.put("name", timerMDC);
        }
        if (docBuffer.isEmpty()) return;

        // How long has the NEWest item been waiting?
        long wait = ((new Date().getTime()) - bufferYoungest) / 1000;
        // If the buffer has been updated in the last 20s ignore it
        if (wait < 20) return;

        // Else, time to flush the buffer
        log.debug("=== Flushing old buffer: {}s", wait);
        submitBuffer(true);
    }

    /**
     * Assess the document buffer and decide is it is ready to submit
     *
     */
    private void checkBuffer() {
        // Doc count limit
        if (docBuffer.size() >= bufferDocLimit) {
            log.debug("=== Buffer check: Doc limit reached '{}'", docBuffer.size());
            submitBuffer(false);
            return;
        }
        // Size limit
        if (bufferSize > bufferSizeLimit) {
            log.debug("=== Buffer check: Size exceeded '{}'", bufferSize);
            submitBuffer(false);
            return;
        }
        // Time limit
        long age = ((new Date().getTime()) - bufferOldest) / 1000;
        if (age > bufferTimeLimit) {
            log.debug("=== Buffer check: Age exceeded '{}s'", age);
            submitBuffer(false);
            return;
        }
    }

    /**
     * Submit all documents currently in the buffer to Solr, then purge
     *
     */
    private void submitBuffer(boolean forceCommit) {
        int size = docBuffer.size();
        if (size > 0) {
            // Debugging
            //String age = String.valueOf(
            //        ((new Date().getTime()) - bufferOldest) / 1000);
            //String length = String.valueOf(bufferSize);
            //log.debug("Submitting buffer: " + size + " documents, " + length +
            //        " bytes, " + age + "s");
            log.debug("=== Submitting buffer: " + size + " documents");

            // Concatenate all documents in the buffer
            String submission = "";
            for (String doc : docBuffer) {
                submission += doc;
                //log.debug("DOC: {}", doc);
            }

            // Submit if the result is valid
            if (!submission.equals("")) {
                // Wrap in the basic Solr 'add' node
                submission = "<add>" + submission + "</add>";
                // And submit
                try {
                    core.request(new DirectXmlRequest("/update", submission));
                } catch (Exception ex) {
                    log.error("Error submitting documents to Solr!", ex);
                }
                // Commit if required
                if (forceCommit) {
                    log.info("Running forced commit!");
                    try {
                        core.commit();
                    } catch (Exception e) {
                        log.warn("Solr forced commit failed. Document will" +
                                " not be visible until Solr autocommit fires." +
                                " Error message: {}", e);
                    }
                }
            }
        }
        purgeBuffer();
    }

    /**
     * Purge the document buffer
     *
     */
    private void purgeBuffer() {
        docBuffer.clear();
        bufferSize = 0;
        bufferOldest = 0;
        bufferYoungest = 0;
    }

    /**
     * Shuts down the plugin
     * 
     * @throws SubscriberException if there was an error during shutdown
     */
    @Override
    public void shutdown() throws SubscriberException {
        timer.cancel();
    }

    /**
     * Add the event to the index
     *
     * @param param : Map of key/value pairs to add to the index
     * @throws Exception if there was an error
     */
    private void addToIndex(Map<String, String> param) throws Exception {
        String doc = writeUpdateString(param);
        addToBuffer(doc);
    }

    /**
     * Turn a a message into a Solr document
     *
     * @param param : Map of key/value pairs to add to the index
     */
    private String writeUpdateString(Map<String, String> param) {
        String fieldStr = "";
        for (String paramName : param.keySet()) {
            fieldStr += "<field name=\"" + paramName + "\">" +
                    StringEscapeUtils.escapeXml(param.get(paramName)) +
                    "</field>";
        }
        return "<add><doc>" + fieldStr + "</doc></add>";
    }

    /**
     * Method to fire for incoming events
     *
     * @param param : Map of key/value pairs to add to the index
     * @throws SubscriberException if there was an error
     */
    @Override
    public void onEvent(Map<String, String> param) throws SubscriberException {
        try {
            addToIndex(param);
        } catch (Exception e) {
            throw new SubscriberException("Fail to add log to solr"
                    + e.getMessage());
        }
    }

}
