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
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.GenericListener;
import au.edu.usq.fascinator.common.JsonObject;
import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.common.MessagingServices;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

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
 * Consumer for rendering transformers. Jobs in this queue are generally longer
 * running running processes and are started after the initial harvest.
 * 
 * @author Oliver Lucido
 * @author Linda Octalina
 */
public class RenderQueueConsumer implements GenericListener {

    /** Service Loader will look for this */
    public static final String LISTENER_ID = "render";

    /** Render queue string */
    private String QUEUE_ID;

    /** Logging */
    private Logger log = LoggerFactory.getLogger(RenderQueueConsumer.class);

    /** JSON configuration */
    private JsonSimpleConfig globalConfig;

    /** JMS connection */
    private Connection connection;

    /** JMS Session */
    private Session session;

    /** JMS Topic */
    // private Topic broadcast;

    /** Indexer object */
    private Indexer indexer;

    /** Storage */
    private Storage storage;

    /** Message Consumer instance */
    private MessageConsumer consumer;

    /** Message Producer instance */
    private MessageProducer producer;

    /** Name identifier to be put in the queue */
    private String name;

    /** Thread reference */
    private Thread thread;

    /** Object being processed */
    private DigitalObject object;

    /** Transformer conveyer belt */
    private ConveyerBelt conveyer;

    /** Auto commit flag on index */
    private boolean autoCommit;

    /** Messaging services */
    private MessagingServices messaging;

    /**
     * Constructor required by ServiceLoader. Be sure to use init()
     * 
     */
    public RenderQueueConsumer() {
        thread = new Thread(this, LISTENER_ID);
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
        name = config.getString(null, "config", "name");
        QUEUE_ID = name;
        thread.setName(name);

        // Set autoCommit if we are the user priority queue
        if (name.equals(ConveyerBelt.CRITICAL_USER_SELECTOR)) {
            autoCommit = true;
        } else {
            autoCommit = false;
        }

        try {
            globalConfig = new JsonSimpleConfig();
            File sysFile = JsonSimpleConfig.getSystemFile();
            indexer = PluginManager.getIndexer(
                    globalConfig.getString("solr", "indexer", "type"));
            indexer.init(sysFile);
            storage = PluginManager.getStorage(
                    globalConfig.getString("file-system", "storage", "type"));
            storage.init(sysFile);

            conveyer = new ConveyerBelt(ConveyerBelt.RENDER);

        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
            throw ioe;
        } catch (PluginException pe) {
            log.error("Failed to initialise plugin: {}", pe.getMessage());
            throw pe;
        }

        try {
            messaging = MessagingServices.getInstance();
        } catch (JMSException jmse) {
            log.error("Failed to start connection: {}", jmse.getMessage());
        }
    }

    /**
     * Return the ID string for this listener
     * 
     */
    @Override
    public String getId() {
        return LISTENER_ID;
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

            // Get the message deatils
            String text = ((TextMessage) message).getText();
            JsonSimpleConfig config = new JsonSimpleConfig(text);
            String oid = config.getString(null, "oid");
            log.info("Received job, object id={}", oid);

            // Get our object from storage
            object = storage.getObject(oid);
            sendNotification(oid, "renderStart", "(" + name
                    + ") Renderer starting : '" + oid + "'");

            // Push through the conveyer belt
            log.info("Updating object...");
            object = conveyer.transform(object, config);

            // Index the object
            log.info("Indexing object...");
            indexer.index(object.getId());
            if (autoCommit || config.getBoolean(false, "commit")) {
                indexer.commit();
            }

            // Log event
            sentMessage(oid, "modify");

            // Finish up
            sendNotification(oid, "renderComplete", "(" + name
                    + ") Renderer complete : '" + oid + "'");
            Properties props = object.getMetadata();
            props.setProperty("render-pending", "false");
            object.close();

        } catch (JMSException jmse) {
            log.error("Failed to send/receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        } catch (StorageException se) {
            log.error("Failed to update storage: {}", se.getMessage());
        } catch (TransformerException te) {
            log.error("Failed to transform object: {}", te.getMessage());
        } catch (IndexerException ie) {
            log.error("Failed to index object: {}", ie.getMessage());
        }
    }

    /**
     * Send the notification out on the broadcast topic
     * 
     * @param oid Object id
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
        if (newPriority >= Thread.MIN_PRIORITY
                && newPriority <= Thread.MAX_PRIORITY) {
            thread.setPriority(newPriority);
        }
    }
}
