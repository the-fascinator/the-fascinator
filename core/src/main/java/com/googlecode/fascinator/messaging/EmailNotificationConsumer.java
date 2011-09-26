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
package com.googlecode.fascinator.messaging;

import com.googlecode.fascinator.common.messaging.GenericListener;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.messaging.MessagingServices;

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
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Consumer for sending email notifications.
 * 
 * @author Oliver Lucido
 */
public class EmailNotificationConsumer implements GenericListener {

    /** Service Loader will look for this */
    public static final String LISTENER_ID = "emailnotification";

    /** Render queue string */
    private String QUEUE_ID;

    /** Logging */
    private Logger log = LoggerFactory
            .getLogger(EmailNotificationConsumer.class);

    /** JSON configuration */
    private JsonSimpleConfig globalConfig;

    /** JMS connection */
    private Connection connection;

    /** JMS Session */
    private Session session;

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

    /** Default subject if not specified in message */
    private String defaultSubject;

    /** Default body if not specified in message */
    private String defaultBody;

    private String smtpHost;
    private int smtpPort;
    private String smtpSslPort;
    private boolean smtpSsl, smtpTls;
    private String smtpUsername;
    private String smtpPassword;

    private String fromAddress;
    private String fromName;

    private boolean debug;

    /**
     * Constructor required by ServiceLoader. Be sure to use init()
     * 
     */
    public EmailNotificationConsumer() {
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

        try {
            globalConfig = new JsonSimpleConfig();

            debug = config.getBoolean(false, "config", "debug");

            smtpHost = config.getString("null", "config", "smtp", "host");
            smtpPort = config.getInteger(25, "config", "smtp", "port");
            smtpSslPort = config.getString("465", "config", "smtp", "sslPort");
            smtpSsl = config.getBoolean(false, "config", "smtp", "ssl");
            smtpTls = config.getBoolean(false, "config", "smtp", "tls");
            smtpUsername = config.getString(null, "config", "smtp", "username");
            smtpPassword = config.getString(null, "config", "smtp", "password");

            defaultSubject = config.getString("Notification",
                    "config", "defaults", "subject");
            defaultBody = config.getString("nt", "config", "defaults", "body");

            fromAddress = config.getString("fascinator@usq.edu.au",
                    "config", "from", "email");
            fromName = config.getString("The Fascinator",
                    "config", "from", "name");

        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
            throw ioe;
        }

        try {
            messaging = MessagingServices.getInstance();
        } catch (MessagingException ex) {
            log.error("Failed to start connection: {}", ex.getMessage());
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
            JsonSimple config = new JsonSimple(text);
            String oid = config.getString(null, "oid");
            log.info("Received notification request, object id={}", oid);

            // Addresses
            List<String> toList = config.getStringList("to");
            List<String> ccList = config.getStringList("cc");

            String subject = config.getString(defaultSubject, "subject");
            String body = config.getString(defaultBody, "body");
            sendEmails(toList, ccList, subject, body);

            // Log event
            sentMessage(oid, "notify");

            // Finish up
            sendNotification(oid, "emailSent", "(" + name
                    + ") Email notification sent : '" + oid + "'");

        } catch (JMSException jmse) {
            log.error("Failed to send/receive message: {}", jmse);
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe);
        } catch (EmailException ee) {
            log.error("Failed to send emails: {}", ee);
        } catch (Exception ex) {
            log.error("Unknown error: {}", ex);
        }
    }

    private void sendEmails(List<String> toList, List<String> ccList,
            String subject, String body) throws EmailException {
        Email email = new SimpleEmail();
        email.setDebug(debug);
        email.setHostName(smtpHost);
        if (smtpUsername != null || smtpPassword != null) {
            email.setAuthentication(smtpUsername, smtpPassword);
        }
        email.setSmtpPort(smtpPort);
        email.setSslSmtpPort(smtpSslPort);
        email.setSSL(smtpSsl);
        email.setTLS(smtpTls);
        email.setSubject(subject);
        email.setMsg(body);
        for (String to : toList) {
            email.addTo(to);
        }
        if (ccList != null) {
            for (String cc : ccList) {
                email.addCc(cc);
            }
        }
        email.setFrom(fromAddress, fromName);
        email.send();
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
        param.put("context", this.getClass().getName());
        try {
            messaging.onEvent(param);
        } catch (MessagingException ex) {
            log.error("Unable to send message: ", ex);
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
