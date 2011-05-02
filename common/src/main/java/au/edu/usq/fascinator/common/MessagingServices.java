/* 
 * The Fascinator - Common Library - Messaging Services
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
package au.edu.usq.fascinator.common;

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
    public static MessagingServices getInstance() throws JMSException {
        if (instance == null) {
            instance = new MessagingServices();
        }
        refCount++;
        return instance;
    }

    /** JMS connection */
    private Connection connection;

    /** JMS session */
    private Session session;

    /** JMS producer */
    private MessageProducer producer;

    /** JMS destinations cache */
    private Map<String, Destination> destinations;

    /**
     * Starts a connection to a message broker
     * 
     * @throws JMSException if an error occurred starting the JMS connections
     */
    private MessagingServices() throws JMSException {
        try {
            log.debug("Starting message queue services...");

            // create activemq connection and session
            JsonSimpleConfig config = new JsonSimpleConfig();
            String brokerUrl = config.getString(
                    ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL,
                    "messaging", "url");
            ActiveMQConnectionFactory connectionFactory =
                    new ActiveMQConnectionFactory(brokerUrl);
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create single producer for multiple destinations
            producer = session.createProducer(null);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // cache destinations
            destinations = new HashMap<String, Destination>();
        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
        }
    }

    /**
     * Gets the JMS connection
     * 
     * @return JMS connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Gets the JMS session
     * 
     * @return JMS session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Gets the JMS message producer with no default destination
     * 
     * @return JMS message producer
     */
    public MessageProducer getProducer() {
        return producer;
    }

    /**
     * Closes the JMS connections. This will only close the connections if there
     * are no longer any references to the instance.
     */
    public void release() {
        refCount--;
        if (instance != null && refCount <= 0) {
            log.info("Closing message queue services...");
            if (producer != null) {
                try {
                    producer.close();
                } catch (JMSException jmse) {
                    log.warn("Failed to close producer: {}", jmse.getMessage());
                }
            }
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException jmse) {
                    log.warn("Failed to close session: {}", jmse.getMessage());
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException jmse) {
                    log.warn("Failed to close connection: {}", jmse
                            .getMessage());
                }
            }
            refCount = 0;
            instance = null;
        }
    }

    /**
     * Sends a message to a JMS topic.
     * 
     * @param name topic name
     * @param msg message to send
     */
    public void publishMessage(String name, String msg) {
        try {
            // log.debug("Publishing '{}' to '{}'", msg, name);
            sendMessage(getDestination(name, false), msg);
        } catch (JMSException jmse) {
            log.error("Failed to publish message", jmse);
        }
    }

    /**
     * Sends a message to a JMS queue.
     * 
     * @param name queue name
     * @param msg message to send
     */
    public void queueMessage(String name, String msg) {
        try {
            // log.debug("Queuing '{}' to '{}'", msg, name);
            sendMessage(getDestination(name, true), msg);
        } catch (JMSException jmse) {
            log.error("Failed to queue message", jmse);
        }
    }

    /**
     * Sends a message to a JMS destination.
     * 
     * @param name destination name
     * @param msg message to send
     */
    public void sendMessage(Destination destination, String msg)
            throws JMSException {
        // log.debug("Sending message to '{}': '{}'", destination.toString(),
        // msg);
        TextMessage message = session.createTextMessage(msg);
        producer.send(destination, message);
    }

    /**
     * Gets a JMS destination with the given name. If the destination doesn't
     * exist it is created and cached for reuse.
     * 
     * @param name name of the destination
     * @param queue true if the destination is a queue, false for topic
     * @return a JMS destination
     * @throws JMSException if an error occurred creating the destination
     */
    private Destination getDestination(String name, boolean queue)
            throws JMSException {
        Destination destination = destinations.get(name);
        if (destination == null) {
            destination = queue ? session.createQueue(name) : session
                    .createTopic(name);
            destinations.put(name, destination);
        }
        return destination;
    }

    /**
     * To put events to subscriber queue
     * 
     * @param oid Object id
     * @param eventType type of events happened
     * @param context where the event happened
     * @param jsonFile Configuration file
     */
    public void onEvent(Map<String, String> param) {
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
