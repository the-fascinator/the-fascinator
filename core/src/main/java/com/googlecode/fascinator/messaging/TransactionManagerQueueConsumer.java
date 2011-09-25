/* 
 * The Fascinator - Core
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
package com.googlecode.fascinator.messaging;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.api.transaction.TransactionException;
import com.googlecode.fascinator.api.transaction.TransactionManager;
import com.googlecode.fascinator.api.transformer.Transformer;
import com.googlecode.fascinator.api.transformer.TransformerException;
import com.googlecode.fascinator.common.messaging.GenericListener;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.messaging.MessagingServices;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Consumer for rendering transformers. Jobs in this queue are generally longer
 * running running processes and are started after the initial harvest.
 * 
 * @author Oliver Lucido
 * @author Linda Octalina
 */
public class TransactionManagerQueueConsumer implements GenericListener {
    // Static list of order types
    public static enum OrderType {INDEXER, MESSAGE, SUBSCRIBER, TRANSFORMER}

    /** Service Loader will look for this */
    public static final String LISTENER_ID = "transactionManager";

    /** Render queue string */
    private String QUEUE_ID;

    /** Logging */
    private Logger log = LoggerFactory.getLogger(
            TransactionManagerQueueConsumer.class);

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

    /** Messaging services */
    private MessagingServices messaging;

    /** Transaction Management Plugin */
    private TransactionManager manager;

    /** List of Transformers */
    private Map<String, Transformer> transformers;

    /**
     * Constructor required by ServiceLoader. Be sure to use init()
     * 
     */
    public TransactionManagerQueueConsumer() {
        thread = new Thread(this, LISTENER_ID);
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
        File sysFile = null;

        try {
            globalConfig = new JsonSimpleConfig();
            sysFile = JsonSimpleConfig.getSystemFile();

            // Load the indexer plugin
            String indexerId = globalConfig.getString(
                    "solr", "indexer", "type");
            if (indexerId == null) {
                throw new Exception("No Indexer ID provided");
            }
            indexer = PluginManager.getIndexer(indexerId);
            if (indexer == null) {
                throw new Exception("Unable to load Indexer '"+indexerId+"'");
            }
            indexer.init(sysFile);

            // Load the storage plugin
            String storageId = globalConfig.getString(
                    "file-system", "storage", "type");
            if (storageId == null) {
                throw new Exception("No Storage ID provided");
            }
            storage = PluginManager.getStorage(storageId);
            if (storage == null) {
                throw new Exception("Unable to load Storage '"+storageId+"'");
            }
            storage.init(sysFile);

            // Loop through all the system's transformers
            transformers = new LinkedHashMap();
            Map<String, JsonSimple> map = globalConfig.getJsonSimpleMap(
                    "transformerDefaults");
            if (map != null && map.size() > 0) {
                for (String tName : map.keySet()) {
                    String id = map.get(tName).getString(null, "id");
                    if (id != null) {
                        // Instantiate the transformer
                        Transformer transformer =
                                PluginManager.getTransformer(id);
                        if (transformer != null) {
                            try {
                                transformer.init(map.get(tName).toString());
                                // Finally, store it for use later
                                transformers.put(tName, transformer);
                                log.info("Transformer warmed: '{}'", tName);
    
                            } catch (PluginException ex) {
                                throw new TransformerException(ex);
                            }
                        } else {
                            log.info("Transformer not found: '{}'", tName);
                        }
                    } else {
                        log.warn("Invalid transformer config, no ID.");
                    }
                }
            } else {
                log.warn("Transaction Manager instantiated with no Transformers!");
            }
        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
            throw ioe;
        } catch (PluginException pe) {
            log.error("Failed to initialise plugin: {}", pe.getMessage());
            throw pe;
        }

        try {
            messaging = MessagingServices.getInstance();
        } catch (MessagingException ex) {
            log.error("Failed to start connection: {}", ex.getMessage());
            throw ex;
        }

        // Start the brains of this thing
        String managerId = config.getString(null,
                "config", "transactionManagerPlugin");
        if (managerId == null) {
            log.error("No TransactionManagement plugin provided");
            throw new Exception("No TransactionManagement plugin ID"
                    + " found in queue consumer configuration");
        }
        try {
            manager = PluginManager.getTransactionManager(managerId);
            if (manager == null) {
                log.error("Transaction Manager Plugin '{}' not found",
                        managerId);
                throw new PluginException("Transaction Manager Plugin '" +
                        managerId + "' not found");
            }
            manager.init(sysFile);

        } catch (PluginException pe) {
            log.error("Failed to initialise plugin: {}", pe.getMessage());
            throw pe;
        }
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
        if (manager != null) {
            try {
                manager.shutdown();
            } catch (PluginException pe) {
                log.warn("Failed to shutdown transaction manager: {}", pe);
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
        if (messaging != null) {
            messaging.release();
        }
        for (String key : transformers.keySet()) {
            try {
                transformers.get(key).shutdown();
            } catch (PluginException ex) {
                log.warn("Failed to shutdown Transformer: '{}'", key, ex);
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
            JsonSimple json = new JsonSimple(text);

            // Run the message through the transaction manager
            Object objOrders = null;
            try {
                objOrders = manager.parseMessage(json);
            } catch (TransactionException ex) {
                log.error("Error during message processing:", ex);
                log.error("Message details:\n{}", json.toString(true));
                return;
            }
            if (objOrders == null) {
                log.error("Orders object is NULL!");
                return;
            }

            if (objOrders instanceof JsonSimple) {
                try {
                    processOrders((JsonSimple) objOrders);
                } catch (Exception ex) {
                    log.error("An error occurred whilst processing"
                            + " the provided orders:", ex);
                }
            } else {
                log.error("Orders not in format expected (JsonSimple)."
                        + " Found '{}' instead", objOrders.getClass()
                        .getName());
            }

        } catch (JMSException jmse) {
            log.error("Failed to send/receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        }
    }

    /**
     * Process the JSON orders passed along
     * 
     * @param orders The incoming orders that require processing
     */
    private void processOrders(JsonSimple orders) throws Exception {
        List<JsonSimple> orderList = orders.getJsonSimpleList("orders");
        if (orderList == null || orderList.isEmpty()) {
            log.info("No orders provided... nothing to do.");
            return;
        }
        for (JsonSimple order : orderList) {
            OrderType type = OrderType.valueOf(order.getString(null, "type"));
            boolean success = false;
            switch (type) {
                case INDEXER:
                    success = index(order);
                    break;
                case MESSAGE:
                    success = message(order);
                    break;
                case SUBSCRIBER:
                    success = subscriber(order);
                    break;
                case TRANSFORMER:
                    success = transform(order);
                    break;
                default:
                    log.error("Invalid 'type' of order: '{}'", type);
                    break;
            }
            if (!success) {
                log.error(
                        "Error processing order from Transaction Manager:\n{}",
                        order.toString(true));
            }
        }
    }

    /**
     * Process a message order
     * 
     * @param order The complete JSON order
     * @param boolean True if successful, otherwise False
     */
    private boolean message(JsonSimple order) {
        // Make sure we got a message queue to send to
        String target = order.getString(null, "target");
        if (target == null) {
            log.error("Invalid message order, no target supplied!");
            return false;
        }

        // Make sure we got an actual message
        JsonObject message = order.getObject("message");
        if (message == null) {
            log.error("Invalid message order, no message text supplied!");
            return false;
        }

        // An alternate broker is optional
        String broker = order.getString(null, "broker");
        if (broker != null) {
            log.info("Outgoing message using broker: '{}'", broker);
        }

        // Let the indexer do its thing
        try {
            if (broker != null) {
                messaging.queueMessage(broker, target, message.toString());
            } else {
                messaging.queueMessage(target, message.toString());
            }
            return true;
        } catch (Exception ex) {
            log.error("Error sending message to '{}'", target, ex);
            return false;
        }
    }

    /**
     * Process an subscriber order
     * 
     * @param order The complete JSON order
     * @param boolean True if successful, otherwise False
     */
    private boolean subscriber(JsonSimple order) {
        // Sanity check our OID
        String oid = order.getString(null, "oid");
        if (oid == null) {
            log.error("Invalid subscriber order, no OID supplied!");
            return false;
        }

        // Make sure we got an actual message
        JsonObject message = order.getObject("message");
        if (message == null) {
            log.error("Invalid subscriber order, no message object supplied!");
            return false;
        }

        // Double-check that the OID was part of the message
        Object mOid = message.get("oid");
        if (mOid == null) {
            message.put("oid", oid);
            log.warn("Message object missing OID, adding before send");
        } else {
            if (!((String) mOid).equals(oid)) {
                log.error("Invalid subscriber order, OID mismatch in order!");
                return false;
            }
        }

        // Now send our message to the subscriber
        String target = SubscriberQueueConsumer.SUBSCRIBER_QUEUE;
        try {
            messaging.queueMessage(target, message.toString());
            return true;
        } catch (Exception ex) {
            log.error("Error sending message to '{}'", target, ex);
            return false;
        }
    }

    /**
     * Process an index order
     * 
     * @param order The complete JSON order
     * @param boolean True if successful, otherwise False
     */
    private boolean index(JsonSimple order) {
        // Sanity check our OID
        String oid = order.getString(null, "oid");
        if (oid == null) {
            log.error("Invalid indexer order, no OID supplied!");
            return false;
        }

        // Let the indexer do its thing
        try {
            indexer.index(name);
            return true;
        } catch (IndexerException ex) {
            log.error("Error indexing OID '{}'", oid, ex);
            return false;
        }
    }

    /**
     * Process a transform order
     * 
     * @param order The complete JSON order
     * @param boolean True if successful, otherwise False
     */
    private boolean transform(JsonSimple order) {
        // Sanity check our OID and get it from storage
        String oid = order.getString(null, "oid");
        if (oid == null) {
            log.error("Invalid transformation order, no OID supplied!");
            return false;
        }
        DigitalObject object = getObjectFromStorage(oid);
        if (object == null) {
            return false;
        }

        // Make sure a valid target Transformer has been provided
        String target = order.getString(null, "target");
        if (target == null) {
            log.error("Invalid transformation order, no target supplied!");
            return false;
        }
        if (!transformers.containsKey(target)) {
            log.error("Invalid transformation order, '{}' is not"
                    + " a configured transformer!", target);
            return false;
        }

        // It is generally expected that some config comes with the order,
        //    but we don't require it 100%, just warn.
        JsonObject config = order.getObject("config");
        if (config == null) {
            log.warn("Expected some configuration for Transformer,"
                    + " but found none. Will attempt without.");
            config = new JsonObject();
        }

        // Now actually transform
        try {
            transformers.get(target).transform(object, config.toString());
            return true;
        } catch (TransformerException ex) {
            log.error("Error transforming OID '{}'", oid, ex);
            return false;
        }
    }

    /**
     * Retrieve an object from storage and return once instantiated.
     * 
     * @param oid The ID of the object to retrieve
     * @param DigitalObject An instantiated object
     */
    private DigitalObject getObjectFromStorage(String oid) {
        try {
            return storage.getObject(oid);
        } catch (StorageException ex) {
            log.error("Error accessing OID: '{}'", oid, ex);
            return null;
        }
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
