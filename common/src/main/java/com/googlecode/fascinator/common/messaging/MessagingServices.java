/* 
 * The Fascinator - Common Library - Messaging Services
 * Copyright (C) 2009-2011 University of Southern Queensland
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
package com.googlecode.fascinator.common.messaging;

import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Messaging services
 * 
 * @author Oliver Lucido
 */
public class MessagingServices {

    /** Subscriber Queue name */
    public static final String SUBSCRIBER_QUEUE = "subscriber";

    /** Error topic string */
    public static final String ERROR_TOPIC = "error";

    /** Message topic string */
    public static final String MESSAGE_TOPIC = "message";

    /** Logging */
    private static Logger log = LoggerFactory
            .getLogger(MessagingServices.class);

    /** Reference counter */
    private static int refCount = 0;

    /** Messaging Services instance */
    private static MessagingServices instance;

    /**
     * Get messaging service instance
     * 
     * @return Messaging service instance
     * @throws JMSException if an error occurred starting the JMS connections
     */
    public static MessagingServices getInstance() throws MessagingException {
        if (instance == null) {
            instance = new MessagingServices();
        }
        refCount++;
        return instance;
    }

    /** AMQ Connector URL for THIS server */
    private String localBroker =
            ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL;

    /** AMQ Connectors */
    private Map<String, ActiveMQConnectionFactory> connectors;

    /** JMS connections */
    private Map<String, Connection> connections;

    /** JMS sessions */
    private Map<String, Session> sessions;

    /** JMS producers */
    private Map<String, MessageProducer> producers;

    /**
     * Starts a connection to a message broker
     * 
     * @throws JMSException if an error occurred starting the JMS connections
     */
    private MessagingServices() throws MessagingException {
        log.info("Starting message queue services...");

        // Get system config and find our connector URL
        JsonSimpleConfig config = null;
        try {
            config = new JsonSimpleConfig();
            localBroker = config.getString(localBroker, "messaging", "url");
        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
        }

        // Setup our connector factory(s)...
        //    we'll only have a local one usually
        connectors = new HashMap();
        connectors.put(localBroker, new ActiveMQConnectionFactory(localBroker));

        // Setup the first of our JMS objects for a local broker
        connections = new HashMap();
        sessions = new HashMap();
        producers = new HashMap();
        newProducer();
    }

    /**
     * Establish a new JMS Connection to the local broker
     * 
     * @return Connection JMS connection
     * @throws MessagingException if an error occurs
     */
    private Connection newConnection() throws MessagingException {
        return newConnection(localBroker);
    }

    /**
     * Establish a new JMS Connection to any broker
     * 
     * @param The broker URL to use
     * @return Connection JMS connection
     * @throws MessagingException if an error occurs
     */
    private Connection newConnection(String brokerUrl)
            throws MessagingException {
        // Hopefully we've seen this broker before
        if (!connectors.containsKey(brokerUrl))  {
            log.info("Opening new AMQ Connection Factory for broker: '{}'",
                    brokerUrl);
            connectors.put(brokerUrl, new ActiveMQConnectionFactory(brokerUrl));
        }

        // Try creating our connection
        try {
            connections.put(brokerUrl,
                    connectors.get(brokerUrl).createConnection());
        } catch (JMSException ex) {
            log.error("Error creating: ", ex);
            throw new MessagingException(ex);
        }

        // Start and return the new connection
        try {
            connections.get(brokerUrl).start();
        } catch (JMSException ex) {
            log.error("Error starting the new connection: ", ex);
            throw new MessagingException(ex);
        }
        return connections.get(brokerUrl);
    }

    /**
     * Establish a new JMS Session on an existing connection to the local broker
     * 
     * @return Session JMS session
     * @throws MessagingException if an error occurs
     */
    private Session newSession() throws MessagingException {
        return newSession(localBroker);
    }

    /**
     * Establish a new JMS Session on an existing connection to any broker
     * 
     * @param The broker URL to use
     * @return Session JMS session
     * @throws MessagingException if an error occurs
     */
    private Session newSession(String brokerUrl)
            throws MessagingException {
        // Confirm we have a connection to use
        if (!connections.containsKey(brokerUrl)) {
            log.info("Opening new AMQ Session for broker: '{}'", brokerUrl);
            newConnection(brokerUrl);
        }

        // Establish a new session
        try {
            sessions.put(brokerUrl, connections.get(brokerUrl)
                    .createSession(false, Session.AUTO_ACKNOWLEDGE));
        } catch (JMSException ex) {
            log.error("Error establishing a new session: ", ex);
            throw new MessagingException(ex);
        }

        return sessions.get(brokerUrl);
    }

    /**
     * Establish a new JMS Message Producer in an existing session
     * to the local broker
     * 
     * @return MessageProducer JMS Message Producer
     * @throws MessagingException if an error occurs
     */
    private MessageProducer newProducer() throws MessagingException {
        return newProducer(localBroker);
    }

    /**
     * Establish a new JMS Message Producer in an existing session to any broker
     * 
     * @param The broker URL to use
     * @return MessageProducer JMS Message Producer
     * @throws MessagingException if an error occurs
     */
    private MessageProducer newProducer(String brokerUrl)
            throws MessagingException {
        // Confirm we have a session already
        if (!sessions.containsKey(brokerUrl)) {
            log.info("Creating new AMQ Producer for broker: '{}'", brokerUrl);
            newSession(brokerUrl);
        }

        // Establish a new producer
        try {
            producers.put(brokerUrl,
                    sessions.get(brokerUrl).createProducer(null));
            producers.get(brokerUrl).setDeliveryMode(DeliveryMode.PERSISTENT);
        } catch (JMSException ex) {
            log.error("Error starting a new producer: ", ex);
            throw new MessagingException(ex);
        }

        return producers.get(brokerUrl);
    }

    /**
     * Closes the JMS connections. This will only close the connections if there
     * are no longer any references to the instance.
     */
    public void release() {
        refCount--;
        if (instance != null && refCount <= 0) {
            log.info("Closing message queue services...");
            if (producers != null && !producers.isEmpty()) {
                for (String key : producers.keySet()) {
                    log.info("Closing producer for broker '{}'", key);
                    try {
                        producers.get(key).close();
                    } catch (JMSException jmse) {
                        log.warn("... failed: {}", jmse);
                    }
                }
            }
            if (sessions != null && !sessions.isEmpty()) {
                for (String key : sessions.keySet()) {
                    log.info("Closing session for broker '{}'", key);
                    try {
                        sessions.get(key).close();
                    } catch (JMSException jmse) {
                        log.warn("... failed: {}", jmse);
                    }
                }
            }
            if (connections != null && !connections.isEmpty()) {
                for (String key : connections.keySet()) {
                    log.info("Closing connection for broker '{}'", key);
                    try {
                        connections.get(key).close();
                    } catch (JMSException jmse) {
                        log.warn("... failed: {}", jmse);
                    }
                }
            }
            refCount = 0;
            instance = null;
        }
    }

    /**
     * Sends a textual message to the named topic of the local broker.
     * 
     * @param name The topic to send to
     * @param msg The message to send
     * @throws MessagingException If any errors occur in resolving the
     * destination or sending.
     */
    public void publishMessage(String name, String msg)
            throws MessagingException {
        publishMessage(localBroker, name, msg);
    }

    /**
     * Sends a textual message to the named topic on any broker.
     * 
     * @param brokerUrl The broker to send to
     * @param name The topic to send to
     * @param msg The message to send
     * @throws MessagingException If any errors occur in resolving the
     * destination or sending.
     */
    public void publishMessage(String brokerUrl, String name, String msg)
            throws MessagingException {
        Destination destination = getDestination(brokerUrl, name, false);
        TextMessage message = prepareMessage(brokerUrl, msg);
        sendMessage(brokerUrl, destination, message);
    }

    /**
     * Sends a textual message to a named JMS queue on the local broker
     * 
     * @param name The queue to send to
     * @param msg The message to send
     * @throws MessagingException If any errors occur in resolving the
     * destination or sending.
     */
    public void queueMessage(String name, String msg)
            throws MessagingException {
        queueMessage(localBroker, name, msg);
    }

    /**
     * Sends a textual message to a named JMS queue on any broker.
     * 
     * @param brokerUrl The broker to send to
     * @param name The queue to send to
     * @param msg The message to send
     * @throws MessagingException If any errors occur in resolving the
     * destination or sending.
     */
    public void queueMessage(String brokerUrl, String name, String msg)
            throws MessagingException {
        Destination destination = getDestination(brokerUrl, name, true);
        TextMessage message = prepareMessage(brokerUrl, msg);
        sendMessage(brokerUrl, destination, message);
    }

    /**
     * Sends a JMS message to an instantiated JMS destination.
     * 
     * @param brokerUrl The broker with an existing producer to use in sending
     * @param destination The destination to send to
     * @param msg The message to send
     * @throws MessagingException If an error occurs when attempting to send
     */
    public void sendMessage(String brokerUrl, Destination destination,
            TextMessage msg) throws MessagingException {
        // Check the producer first
        if (!producers.containsKey(brokerUrl)) {
            newProducer(brokerUrl);
        }

        try {
            // Try sending
            producers.get(brokerUrl).send(destination, msg);
        } catch (JMSException ex) {
            // Ignore the first error, in case it is just an expired session
            try {
                log.warn("Failed to send message! Trying a new producer.");
                newProducer(brokerUrl);
                producers.get(brokerUrl).send(destination, msg);
            } catch (JMSException ex1) {
                log.error("Failed to send message:", ex1);
                throw new MessagingException(ex1);
            }
        }
    }

    /**
     * Prepares a textual message as an instantiated JMS object ready to send.
     * 
     * @param brokerUrl The broker whose existing session we'll use to parse
     * @param msg The message to send
     * @return TextMessage An instantiated JMS message
     * @throws MessagingException if an error occurred creating the message
     */
    private TextMessage prepareMessage(String brokerUrl, String msg)
            throws MessagingException {
        // Check the session first, we use it to instantiate our message
        if (!sessions.containsKey(brokerUrl)) {
            newSession(brokerUrl);
        }

        try {
            // Create and return a Destination
            return sessions.get(brokerUrl).createTextMessage(msg);
        } catch (JMSException ex) {
            // Ignore the first error, in case it is just an expired session
            try {
                log.warn("Failed to create message! Trying a new session.");
                newSession(brokerUrl);
                return sessions.get(brokerUrl).createTextMessage(msg);
            } catch (JMSException ex1) {
                log.error("Failed to create message:", ex1);
                throw new MessagingException(ex1);
            }
        }
    }

    /**
     * Gets a JMS destination with the given name on the given broker.
     * Destinations are not cached anymore.
     * 
     * @param brokerUrl The broker where the destination should be found
     * @param name The name of the destination
     * @param queue True if the destination is a queue, False for topic
     * @return Destination An instantiated JMS destination
     * @throws MessagingException if an error occurred creating the destination
     */
    private Destination getDestination(String brokerUrl, String name,
            boolean queue) throws MessagingException {
        // Check the session first
        if (!sessions.containsKey(brokerUrl)) {
            newSession(brokerUrl);
        }

        try {
            // Create and return a Destination
            return createDestination(sessions.get(brokerUrl), name, queue);
        } catch (JMSException ex) {
            // Ignore the first error, in case it is just an expired session
            try {
                log.warn("Failed to create Destination! Trying a new session.");
                newSession(brokerUrl);
                return createDestination(sessions.get(brokerUrl), name, queue);
            } catch (JMSException ex1) {
                log.error("Failed to create Destination:", ex1);
                throw new MessagingException(ex1);
            }
        }
    }

    /**
     * Trivial wrapper for destination creation. Original JMSException is
     * preserved to ensure it doesn't escalate upwards by mistake. Makes a
     * trivial 'try again' possible to catch basic expired sessions.
     * 
     * @param session The JMS session to use for the outgoing message
     * @param name The name of the destination
     * @param queue True if the destination is a queue, False for topic
     * @return Destination An instantiated JMS destination
     * @throws MessagingException if an error occurred creating the destination
     */
    private Destination createDestination(Session session, String name,
            boolean queue) throws JMSException {
        if (queue) {
            return session.createQueue(name);
        } else {
            return session.createTopic(name);
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
    public void onEvent(Map<String, String> param) throws MessagingException {
        JsonObject json = new JsonObject();
        String username = param.get("username");
        if (username == null) {
            username = "guest";
        }
        json.put("oid", param.get("oid"));
        json.put("eventType", param.get("eventType"));
        json.put("context", param.get("context"));
        json.put("user", username);
        queueMessage(SUBSCRIBER_QUEUE, json.toString());
    }
}
