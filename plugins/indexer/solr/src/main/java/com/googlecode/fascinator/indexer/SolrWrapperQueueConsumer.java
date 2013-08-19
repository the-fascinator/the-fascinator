/*
 * The Fascinator - Indexer - SolrWrapper
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
package com.googlecode.fascinator.indexer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.xml.sax.SAXException;

import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.GenericListener;

/**
 * Consumer for documents to index in Solr. Aggregating the final write in this
 * location allows for a common buffer to prevent timing issues from threaded
 * buffers, as well as allowing us to run thread safe embedded solr.
 * 
 * @author Greg Pendlebury
 */
public class SolrWrapperQueueConsumer implements GenericListener {
    /** Default Solr path if running embedded */
    private static final String DEFAULT_SOLR_HOME = FascinatorHome
            .getPath("solr");

    /** Buffer Limit : Document count */
    private static Integer BUFFER_LIMIT_DOCS = 200;

    /** Buffer Limit : Size */
    private static Integer BUFFER_LIMIT_SIZE = 1024 * 200;

    /** Buffer Limit : Time */
    private static Integer BUFFER_LIMIT_TIME = 30;

    /** Queue name */
    public static final String QUEUE_ID = "solrwrapper";

    /** Logging */
    private Logger log = LoggerFactory
            .getLogger(SolrWrapperQueueConsumer.class);

    /** JSON configuration */
    private JsonSimpleConfig globalConfig;

    /** JMS connection */
    private Connection connection;

    /** JMS Session */
    private Session session;

    /** Message Consumer instance */
    private MessageConsumer consumer;

    /** Name identifier to be put in the queue */
    private String name;

    /** Thread reference */
    private Thread thread;

    /** Main Solr core */
    private SolrServer solr;

    /** Main Solr core (over HTTP) */
    private SolrServer commit;

    /** Core container if running embedded */
    private CoreContainer coreContainer;

    /** Auto-commit flag for main core */
    private boolean autoCommit;

    /** Username for Solr */
    private String username;

    /** Password for Solr */
    private String password;

    /** Buffer of documents waiting submission */
    private Map<String, String> docBuffer;

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
     * Constructor required by ServiceLoader. Be sure to use init()
     * 
     */
    public SolrWrapperQueueConsumer() {
        thread = new Thread(this, QUEUE_ID);
    }

    /**
     * Start thread running
     * 
     */
    @Override
    public void run() {
        try {
            MDC.put("name", name);
            log.info("Starting {}...", name);

            // Get a connection to the broker
            String brokerUrl = globalConfig.getString(
                    ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL,
                    "messaging", "url");
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                    brokerUrl);
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            consumer = session.createConsumer(session.createQueue(QUEUE_ID));
            consumer.setMessageListener(this);
            connection.start();

            // Solr
            solr = initCore("solr");
            // Timeout 'tick' for buffer (10s)
            timer = new Timer("SolrWrapper:" + toString(), true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    checkTimeout();
                }
            }, 0, 10000);
        } catch (JMSException ex) {
            log.error("Error starting message thread!", ex);
        }
    }

    /**
     * Initialization method
     * 
     * @param config Configuration to use
     * @throws Exception if any errors occur
     */
    @Override
    public void init(JsonSimpleConfig config) throws Exception {
        name = config.getString(null, "config", "name");
        if (name == null) {
            throw new Exception("Name name provided in queue configuration");
        }
        thread.setName(name);

        try {
            globalConfig = new JsonSimpleConfig();
            autoCommit = globalConfig.getBoolean(true, "indexer", "solr",
                    "autocommit");

            // Buffering
            docBuffer = new LinkedHashMap<String, String>();
            bufferSize = 0;
            bufferOldest = 0;
            bufferDocLimit = globalConfig.getInteger(BUFFER_LIMIT_DOCS,
                    "indexer", "buffer", "docLimit");
            bufferSizeLimit = globalConfig.getInteger(BUFFER_LIMIT_SIZE,
                    "indexer", "buffer", "sizeLimit");
            bufferTimeLimit = globalConfig.getInteger(BUFFER_LIMIT_TIME,
                    "indexer", "buffer", "timeLimit");

        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
            throw ioe;
        }
    }

    /**
     * Initialize a Solr core object.
     * 
     * @param coreName : The core to initialize
     * @return SolrServer : The initialized core
     */
    private SolrServer initCore(String core) {
        boolean isEmbedded = globalConfig.getBoolean(false, "indexer", core,
                "embedded");
        try {
            // Embedded Solr
            if (isEmbedded) {
                // Solr over HTTP - Needed to run commits
                // so the core web server sees them.
                String uri = globalConfig.getString(null, "indexer", core,
                        "uri");
                if (uri == null) {
                    log.error("No URI provided for core: '{}'", core);
                    return null;
                }
                URI solrUri = new URI(uri);
                commit = new CommonsHttpSolrServer(solrUri.toURL());
                username = globalConfig.getString(null, "indexer", core,
                        "username");
                password = globalConfig.getString(null, "indexer", core,
                        "password");
                if (username != null && password != null) {
                    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                            username, password);
                    HttpClient hc = ((CommonsHttpSolrServer) solr)
                            .getHttpClient();
                    hc.getParams().setAuthenticationPreemptive(true);
                    hc.getState().setCredentials(AuthScope.ANY, credentials);
                }

                // First time execution
                if (coreContainer == null) {
                    String home = globalConfig.getString(DEFAULT_SOLR_HOME,
                            "indexer", "home");
                    log.info("Embedded Solr Home = {}", home);
                    File homeDir = new File(home);
                    if (!homeDir.exists()) {
                        log.error("Solr directory does not exist!");
                        return null;
                    }
                    System.setProperty("solr.solr.home",
                            homeDir.getAbsolutePath());
                    File coreXmlFile = new File(homeDir, "solr.xml");
                    coreContainer = new CoreContainer(
                            homeDir.getAbsolutePath(), coreXmlFile);
                    for (SolrCore aCore : coreContainer.getCores()) {
                        log.info("Loaded core: {}", aCore.getName());
                    }
                }
                String coreName = globalConfig.getString(null, "indexer", core,
                        "coreName");
                if (coreName == null) {
                    log.error("No 'coreName' node for core: '{}'", core);
                    return null;
                }
                return new EmbeddedSolrServer(coreContainer, coreName);

                // Solr over HTTP
            } else {
                String uri = globalConfig.getString(null, "indexer", core,
                        "uri");
                if (uri == null) {
                    log.error("No URI provided for core: '{}'", core);
                    return null;
                }

                URI solrUri = new URI(uri);
                CommonsHttpSolrServer thisCore = new CommonsHttpSolrServer(
                        solrUri.toURL());
                username = globalConfig.getString(null, "indexer", core,
                        "username");
                password = globalConfig.getString(null, "indexer", core,
                        "password");
                if (username != null && password != null) {
                    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                            username, password);
                    HttpClient hc = thisCore.getHttpClient();
                    hc.getParams().setAuthenticationPreemptive(true);
                    hc.getState().setCredentials(AuthScope.ANY, credentials);
                }
                return thisCore;
            }

        } catch (MalformedURLException mue) {
            log.error(core + " : Malformed URL", mue);
        } catch (URISyntaxException urise) {
            log.error(core + " : Invalid URI", urise);
        } catch (IOException ioe) {
            log.error(core + " : Failed to read Solr configuration", ioe);
        } catch (ParserConfigurationException pce) {
            log.error(core + " : Failed to parse Solr configuration", pce);
        } catch (SAXException saxe) {
            log.error(core + " : Failed to load Solr configuration", saxe);
        }
        return null;
    }

    /**
     * Return the ID string for this listener
     * 
     */
    @Override
    public String getId() {
        return QUEUE_ID;
    }

    /**
     * Start the queue based on the name identifier
     * 
     * @throws JMSException if an error occurred starting the JMS connections
     */
    @Override
    public void start() throws Exception {
        thread.start();
    }

    /**
     * Stop the Render Queue Consumer. Including stopping the storage and
     * indexer
     */
    @Override
    public void stop() throws Exception {
        log.info("Stopping {}...", name);
        submitBuffer(true);
        if (coreContainer != null) {
            coreContainer.shutdown();
        }
        if (consumer != null) {
            try {
                consumer.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close consumer: {}", jmse.getMessage());
                throw jmse;
            }
        }
        if (session != null) {
            try {
                session.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close consumer session: {}", jmse);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close connection: {}", jmse);
            }
        }
        timer.cancel();
    }

    /**
     * Callback function for incoming messages.
     * 
     * @param message The incoming message
     */
    @Override
    public void onMessage(Message message) {
        MDC.put("name", name);
        try {
            // Make sure thread priority is correct
            if (!Thread.currentThread().getName().equals(thread.getName())) {
                Thread.currentThread().setName(thread.getName());
                Thread.currentThread().setPriority(thread.getPriority());
            }

            // Get the message details
            String text = ((TextMessage) message).getText();
            JsonSimple config = new JsonSimple(text);
            String event = config.getString(null, "event");
            if (event == null) {
                log.error("Invalid message received: '{}'", text);
                return;
            }

            // Commit on the index
            if (event.equals("commit")) {
                log.debug("Commit received");
                submitBuffer(true);
            }
            // Index the incoming document
            if (event.equals("index")) {
                String index = config.getString(null, "index");
                String document = config.getString(null, "document");
                if (index == null || document == null) {
                    log.error("Invalid message received: '{}'", text);
                    return;
                }
                addToBuffer(index, document);
            }

        } catch (JMSException jmse) {
            log.error("Failed to send/receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        }
    }

    /**
     * Add a new document into the buffer, and check if submission is required
     * 
     * @param document : The Solr document to add to the buffer.
     */
    private void addToBuffer(String index, String document) {
        if (timerMDC == null) {
            timerMDC = MDC.get("name");
        }
        // Remove old entries from the buffer
        int removedSize = 0;
        if (docBuffer.containsKey(index)) {
            log.debug("Removing buffer duplicate: '{}'", index);
            removedSize = docBuffer.get(index).length();
            docBuffer.remove(index);
        }

        int length = document.length() - removedSize;
        // If this is the first document in the buffer, record its age
        bufferYoungest = new Date().getTime();
        if (docBuffer.isEmpty()) {
            bufferOldest = new Date().getTime();
            log.debug("=== New buffer starting: {}", bufferOldest);
        }
        // Add to the buffer
        docBuffer.put(index, document);
        bufferSize += length;
        // Check if submission is required
        checkBuffer();
    }

    /**
     * Method to fire on timeout() events to ensure buffers don't go stale after
     * the last item in a harvest passes through.
     * 
     */
    private void checkTimeout() {
        if (timerMDC != null) {
            MDC.put("name", timerMDC);
        }
        if (docBuffer.isEmpty()) {
            return;
        }

        // How long has the NEWest item been waiting?
        long wait = ((new Date().getTime()) - bufferYoungest) / 1000;
        // If the buffer has been updated in the last 20s ignore it
        if (wait < 20) {
            return;
        }

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
            log.debug("=== Buffer check: Doc limit reached '{}'",
                    docBuffer.size());
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
            // String age = String.valueOf(
            // ((new Date().getTime()) - bufferOldest) / 1000);
            // String length = String.valueOf(bufferSize);
            // log.debug("Submitting buffer: " + size + " documents, " + length
            // +
            // " bytes, " + age + "s");
            log.debug("=== Submitting buffer: " + size + " documents");

            // Concatenate all documents in the buffer
            StringBuffer submissionBuffer = new StringBuffer();
            for (String doc : docBuffer.keySet()) {
                submissionBuffer.append(docBuffer.get(doc));
                // log.debug("DOC: {}", doc);
            }

            // Submit if the result is valid
            if (submissionBuffer.length() > 0) {
                // Wrap in the basic Solr 'add' node
                String submission = submissionBuffer.insert(0, "<add>")
                        .append("</add>").toString();
                // And submit
                try {
                    solr.request(new DirectXmlRequest("/update", submission));
                } catch (Exception ex) {
                    log.error("Error submitting documents to Solr!", ex);
                }
                // Commit if required
                if (autoCommit || forceCommit) {
                    log.info("Running forced commit!");
                    try {
                        // HTTP commits for embedded
                        if (commit != null) {
                            solr.commit();
                            commit.commit();
                            // or just HTTP on it's own
                        } else {
                            solr.commit();
                        }
                    } catch (Exception e) {
                        log.warn(
                                "Solr forced commit failed. Document will"
                                        + " not be visible until Solr autocommit fires."
                                        + " Error message: {}", e);
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
     * Sets the priority level for the thread. Used by the OS.
     * 
     * @param newPriority The priority level to set the thread at
     */
    @Override
    public void setPriority(int newPriority) {
        if (newPriority >= Thread.MIN_PRIORITY
                && newPriority <= Thread.MAX_PRIORITY) {
            thread.setPriority(newPriority);
        }
    }
}
