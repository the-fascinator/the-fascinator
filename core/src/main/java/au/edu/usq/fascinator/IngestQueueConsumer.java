/* 
 * The Fascinator - Core
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

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.GenericListener;
import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.common.MessagingServices;
import au.edu.usq.fascinator.common.storage.StorageUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Consumer for Ingest Queue. Jobs in this queue should be short running
 * processes as they are run at harvest time.
 * 
 * @author Linda Octalina
 */
public class IngestQueueConsumer implements GenericListener {

    /** Harvest Queue name */
    public static final String INGEST_QUEUE = "ingest";

    /** Logging */
    private Logger log = LoggerFactory.getLogger(IngestQueueConsumer.class);

    /** Render queue string */
    private String QUEUE_ID;

    /** Name identifier to be put in the queue */
    private String name;

    /** JSON configuration */
    private JsonSimpleConfig globalConfig;

    /** JMS connection */
    private Connection connection;

    /** JMS Session */
    private Session session;

    // /** Render Queues */
    // private Map<String, Queue> renderers;

    // /** Render Queue Names */
    // private Map<String, String> rendererNames;

    /** Indexer object */
    private Indexer indexer;

    /** Storage */
    private Storage storage;

    /** Messaging Consumer */
    private MessageConsumer consumer;

    /** Message Producer instance */
    private MessageProducer producer;

    /** Thread reference */
    private Thread thread;

    /** Messaging services */
    private MessagingServices messaging;

    /**
     * Constructor required by ServiceLoader. Be sure to use init()
     * 
     */
    public IngestQueueConsumer() {
        thread = new Thread(this, INGEST_QUEUE);
    }

    /**
     * Start thread running
     * 
     */
    @Override
    public void run() {
        try {
            log.info("Starting {}", name);

            // Get a connection to the broker
            String brokerUrl = globalConfig.getString(
                    ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL,
                    "messaging", "url");
            ActiveMQConnectionFactory connectionFactory =
                    new ActiveMQConnectionFactory(brokerUrl);
            connection = connectionFactory.createConnection();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            consumer = session.createConsumer(session.createQueue(QUEUE_ID));
            consumer.setMessageListener(this);

            // renderers = new LinkedHashMap();
            // for (String selector : rendererNames.keySet()) {
            // renderers.put(selector, session.createQueue(rendererNames
            // .get(selector)));
            // }
            producer = session.createProducer(null);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            connection.start();
        } catch (JMSException ex) {
            log.error("Error starting message thread!", ex);
        }
    }

    /**
     * Initialization method
     * 
     * @param config Configuration to use
     * @throws IOException if the configuration file not found
     */
    @Override
    public void init(JsonSimpleConfig config) throws Exception {
        try {
            name = config.getString(null, "config", "name");
            QUEUE_ID = name;
            thread.setName(name);

            globalConfig = new JsonSimpleConfig();
            File sysFile = JsonSimpleConfig.getSystemFile();
            indexer = PluginManager.getIndexer(
                    globalConfig.getString("solr", "indexer", "type"));
            indexer.init(sysFile);
            storage = PluginManager.getStorage(
                    globalConfig.getString("file-system", "storage", "type"));
            storage.init(sysFile);

            try {
                messaging = MessagingServices.getInstance();
            } catch (JMSException jmse) {
                log.error("Failed to start connection: {}", jmse.getMessage());
            }

            // // Setup render queue logic
            // rendererNames = new LinkedHashMap();
            // String userQueue = config.get("config/user-renderer");
            // rendererNames.put(ConveyerBelt.CRITICAL_USER_SELECTOR,
            // userQueue);
            // Map<String, Object> map =
            // config.getMap("config/normal-renderers");
            // for (String selector : map.keySet()) {
            // rendererNames.put(selector, (String) map.get(selector));
            // }

        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
            throw ioe;
        } catch (PluginException pe) {
            log.error("Failed to initialise plugin: {}", pe.getMessage());
            throw pe;
        }
    }

    /**
     * Return the ID string for this listener
     * 
     */
    @Override
    public String getId() {
        return INGEST_QUEUE;
    }

    /**
     * Start the ingest queue consumer
     * 
     * @throws JMSException if an error occurred starting the JMS connections
     */
    @Override
    public void start() throws Exception {
        thread.start();
    }

    /**
     * Stop the Ingest Queue consumer. Including: indexer and storage
     */
    @Override
    public void stop() throws Exception {
        log.info("Stopping {}...", name);
        if (indexer != null) {
            try {
                indexer.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown indexer: {}", pe.getMessage());
                throw pe;
            }
        }
        if (storage != null) {
            try {
                storage.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown storage: {}", pe.getMessage());
                throw pe;
            }
        }
        if (producer != null) {
            try {
                producer.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close producer: {}", jmse);
            }
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
            // Incoming message
            String text = ((TextMessage) message).getText();
            JsonSimpleConfig config = new JsonSimpleConfig(text);
            String oid = config.getString(null, "oid");
            log.info("Received job, object id={}, text={}", oid, text);

            File configFile = new File(config.getString(null, "configFile"));
            File uploadedFile = new File(oid);

            Boolean deleted = config.getBoolean(false, "deleted");
            try {
                HarvestClient harvestClient = new HarvestClient(configFile,
                        uploadedFile, "guest");
                if (!deleted) {
                    harvestClient.start();
                }
            } catch (PluginException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Delete object
            if (deleted) {
                String objectId = StorageUtils.generateOid(uploadedFile);
                log.info("Removing object {}...", oid);
                storage.removeObject(objectId);
                indexer.remove(objectId);
                indexer.annotateRemove(objectId);

                // Log event
                sentMessage(oid, "delete");
                sentMessage(oid, "delete-anotar");

                return;
            } else {
                // Log event
                sentMessage(oid, "modify");
            }

        } catch (JMSException jmse) {
            log.error("Failed to send/receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        } catch (IndexerException ie) {
            log.error("Failed to index object: {}", ie.getMessage());
        } catch (StorageException e) {
            log.error("Failed to delete object: {}", e.getMessage());
        }
    }

    /**
     * To put events to subscriber queue
     * 
     * @param oid Object id
     * @param eventType type of events happened
     * @param context where the event happened
     * @param jsonFile Configuration file
     */
    private void sentMessage(String oid, String eventType) {
        Map<String, String> param = new LinkedHashMap<String, String>();
        param.put("oid", oid);
        param.put("eventType", eventType);
        param.put("username", "system");
        param.put("context", "RenderQueueConsumer");
        messaging.onEvent(param);
    }

    /**
     * Sets the priority level for the thread. Used by the OS.
     * 
     * @param newPriority The priority level to set the thread at
     */
    @Override
    public void setPriority(int newPriority) {
    }
}
