/* 
 * The Fascinator - Common - Subscriber Queue Consumer
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
package com.googlecode.fascinator;

import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.subscriber.Subscriber;
import com.googlecode.fascinator.common.GenericListener;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimpleConfig;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Consumer for Subscribers. Jobs in this queue should be short running
 * processes as they are run when object is modified/deleted.
 * 
 * @author Oliver Lucido
 * @author Linda Octalina
 */
public class SubscriberQueueConsumer implements GenericListener {

    /** Subscriber Queue name */
    public static final String SUBSCRIBER_QUEUE = "subscriber";

    /** Date format */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** DateTime format */
    public static final String DATETIME_FORMAT = DATE_FORMAT + "'T'HH:mm:ss'Z'";

    /** Render queue string */
    private String QUEUE_ID;

    /** Logging */
    private Logger log = LoggerFactory.getLogger(SubscriberQueueConsumer.class);

    /** JSON configuration */
    private JsonSimpleConfig globalConfig;

    /** JMS connection */
    private Connection connection;

    /** JMS Session */
    private Session session;

    /** JMS Topic */
    // private Topic broadcast;

    /** Message Consumer instance */
    private MessageConsumer consumer;

    /** Message Producer instance */
    private MessageProducer producer;

    /** Name identifier to be put in the queue */
    private String name;

    /** Thread reference */
    private Thread thread;

    /** List of plugins waiting for news */
    private List<Subscriber> subscriberList;

    /** Important field names */
    private List<String> coreFields;

    /**
     * Constructor required by ServiceLoader. Be sure to use init()
     * 
     */
    public SubscriberQueueConsumer() {
        thread = new Thread(this, SUBSCRIBER_QUEUE);
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

        // A list of core fields
        coreFields = new ArrayList();
        coreFields.add("id");
        coreFields.add("oid");
        coreFields.add("eventType");
        coreFields.add("context");
        coreFields.add("user");
        coreFields.add("eventTime");

        try {
            subscriberList = new ArrayList<Subscriber>();
            globalConfig = new JsonSimpleConfig();
            File sysFile = JsonSimpleConfig.getSystemFile();
            List<String> subscribers = config.getStringList(
                    "config", "subscribers");
            if (subscribers != null && !subscribers.isEmpty()) {
                for (String sid : subscribers) {
                    if (!sid.equals("")) {
                        Subscriber subscriber = PluginManager
                                .getSubscriber(sid);
                        if (subscriber != null) {
                            subscriber.init(sysFile);
                            subscriberList.add(subscriber);
                        } else {
                            log.error("Error loading subscriber: '{}'", sid);
                        }
                    }
                }
            }

        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
            throw ioe;
        }
    }

    /**
     * Return the ID string for this listener
     * 
     */
    @Override
    public String getId() {
        return SUBSCRIBER_QUEUE;
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

            // Get the message details
            String text = ((TextMessage) message).getText();
            JsonSimpleConfig config = new JsonSimpleConfig(text);
            String oid = config.getString(null, "oid");
            String context = config.getString("", "context");

            log.info(" *** Received event, object id={}, from={}", oid, context);

            //sendNotification(oid, "logging start", "(" + name
            //        + ") Event Logging starting : '" + oid + "'");

            DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
            String now = df.format(new Date());

            // Convert to Map
            Map<String, String> param = new LinkedHashMap<String, String>();
            String id = UUID.randomUUID().toString();
            param.put("id", id);
            param.put("oid", oid);
            param.put("eventType", config.getString(null, "eventType"));
            param.put("context", context);
            param.put("user", config.getString("{unknown}", "user"));
            param.put("eventTime", now);
            // Now map all non-core fields as well
            for (Object key : config.getJsonObject().keySet()) {
                if (key instanceof String) {
                    String sKey = (String) key;
                    if (!coreFields.contains(sKey)) {
                        String value = config.getString(null, sKey);
                        if (value != null) {
                            param.put(sKey, value);
                        }
                    }
                }
            }

            // Let all subscribers know
            for (Subscriber subscriber : subscriberList) {
                subscriber.onEvent(param);
            }

            //sendNotification(oid, "logging end", "(" + name
            //        + ") Event Logging ending : '" + oid + "'");

        } catch (JMSException jmse) {
            log.error("Failed to send/receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        } catch (Exception e) {
            log.error("Failed to log the event: {}", e.getMessage());
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
