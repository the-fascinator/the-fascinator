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
package com.googlecode.fascinator;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.api.transformer.TransformerException;
import com.googlecode.fascinator.common.GenericListener;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.MessagingServices;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Consumer for harvest transformers. Jobs in this queue should be short running
 * processes as they are run at harvest time.
 * 
 * @author Oliver Lucido
 * @author Linda Octalina
 */
public class HarvestQueueConsumer implements GenericListener {

    /** Harvest Queue name */
    public static final String HARVEST_QUEUE = "harvest";

    /** Harvest Queue name */
    public static final String USER_QUEUE = "harvestUser";

    /** Logging */
    private Logger log = LoggerFactory.getLogger(HarvestQueueConsumer.class);

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

    /** Broadcast Topic */
    // private Topic broadcast;

    /** Render Queues */
    private Map<String, Queue> renderers;

    /** Render Queue Names */
    private Map<String, String> rendererNames;

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

    /** Object being processed */
    private DigitalObject object;

    /** Transformer conveyer belt */
    private ConveyerBelt conveyer;

    /** Messaging services */
    private MessagingServices messaging;

    /**
     * Constructor required by ServiceLoader. Be sure to use init()
     * 
     */
    public HarvestQueueConsumer() {
        thread = new Thread(this, HARVEST_QUEUE);

        try {
            messaging = MessagingServices.getInstance();
        } catch (JMSException jmse) {
            log.error("Failed to start connection: {}", jmse.getMessage());
        }
    }

    /**
     * Start thread running
     * 
     */
    @Override
    public void run() {
        try {
            log.info("Starting {}...", name);

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

            // broadcast = session.createTopic(MessagingServices.MESSAGE_TOPIC);
            renderers = new LinkedHashMap();
            for (String selector : rendererNames.keySet()) {
                renderers.put(selector,
                        session.createQueue(rendererNames.get(selector)));
            }
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

            // Setup render queue logic
            rendererNames = new LinkedHashMap();
            String userQueue = config.getString(null,
                    "config", "user-renderer");
            rendererNames.put(ConveyerBelt.CRITICAL_USER_SELECTOR, userQueue);
            JsonObject map = config.getObject("config", "normal-renderers");
            for (Object selector : map.keySet()) {
                rendererNames.put(selector.toString(),
                        map.get(selector).toString());
            }

            conveyer = new ConveyerBelt(ConveyerBelt.HARVEST);

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
        return HARVEST_QUEUE;
    }

    /**
     * Start the harvest queue consumer
     * 
     * @throws JMSException if an error occurred starting the JMS connections
     */
    @Override
    public void start() throws Exception {
        thread.start();
    }

    /**
     * Stop the Harvest Queue consumer. Including: indexer and storage
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
            // Make sure thread priority is correct
            if (!Thread.currentThread().getName().equals(thread.getName())) {
                Thread.currentThread().setName(thread.getName());
                Thread.currentThread().setPriority(thread.getPriority());
            }

            // Incoming message
            String text = ((TextMessage) message).getText();
            JsonSimpleConfig config = new JsonSimpleConfig(text);
            String oid = config.getString(null, "oid");
            log.info("Received job, object id='{}'", oid);

            // Simple scenario, delete object
            boolean deleted = config.getBoolean(false, "deleted");
            if (deleted) {
                log.info("Removing object {}...", oid);
                storage.removeObject(oid);
                indexer.remove(oid);
                indexer.annotateRemove(oid);

                // Log event
                sentMessage(oid, "delete");
                sentMessage(oid, "delete-anotar");
                return;
            }

            // Retrieve and process the object
            object = storage.getObject(oid);
            object = conveyer.transform(object, config);
            indexObject(config);
            queueRenderJob(config);

            // Log event
            sentMessage(oid, "modify");

        } catch (TransformerException tex) {
            log.error("Error during transformation: {}", tex);
        } catch (JMSException jmse) {
            log.error("Failed to send/receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        } catch (StorageException se) {
            log.error("Failed to update storage: {}", se.getMessage());
        } catch (IndexerException ie) {
            log.error("Failed to index object: {}", ie.getMessage());
        } catch (Exception e) {
            log.error("An unknown error has occurred: {}", e);
        }
    }

    /**
     * Arrange for the item specified by the message to be indexed
     * 
     * @param message The message received by the queue
     * @throws JMSException if there was an error sending messages
     * @throws IndexerException if the solr indexer failed
     * @throws StorageException if the object's metadata was inaccessible
     */
    private void indexObject(JsonSimpleConfig message) throws JMSException,
            IndexerException, StorageException {
        // Are we indexing?
        boolean doIndex = true;
        Properties props = object.getMetadata();
        String indexFlag = props.getProperty("indexOnHarvest");
        if (indexFlag != null) {
            // The harvest process changed the default
            doIndex = Boolean.parseBoolean(indexFlag);
        } else {
            // Nothing specified, use the default
            doIndex = message.getBoolean(true, "transformer", "indexOnHarvest");
        }

        if (doIndex) {
            String oid = object.getId();
            sendNotification(oid, "indexStart", "Indexing '" + oid
                    + "' started");
            log.info("{} : Indexing object {}...", name, oid);
            indexer.index(oid);
            sendNotification(oid, "indexComplete", "Index of '" + oid
                    + "' completed");
        }
    }

    /**
     * Queue the render job
     * 
     * @param message The message received by the queue
     * @throws JMSException if there was an error posting to the queue
     * @throws StorageException if the object's metadata was inaccessible
     */
    private void queueRenderJob(JsonSimpleConfig message) throws JMSException,
            StorageException {
        // What transformations are required at the render step
        List<String> plugins = ConveyerBelt.getTransformList(object, message,
                ConveyerBelt.RENDER, true);

        TextMessage msg = session.createTextMessage(message.toString());
        // 'renderers' is a LinkedHashMap because the key order is significant
        for (String selector : renderers.keySet()) {
            if (plugins.contains(selector)) {
                producer.send(renderers.get(selector), msg);
                return;
            }
        }

        // Default is the fallback
        producer.send(renderers.get("default"), msg);
    }

    /**
     * Send the notification out on the broadcast topic
     * 
     * @param oid Object Id
     * @param status Status of the object
     * @param message Message to be sent
     */
    private void sendNotification(String oid, String status, String message)
            throws JMSException {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.put("id", oid);
        jsonMessage.put("idType", "object");
        jsonMessage.put("status", status);
        jsonMessage.put("message", message);

        TextMessage msg = session.createTextMessage(jsonMessage.toString());
        // producer.send(broadcast, msg);
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
        param.put("context", "HarvestQueueConsumer");
        messaging.onEvent(param);
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
