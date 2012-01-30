/* 
 * The Fascinator - Core - Message Broker
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

import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.GenericListener;
import com.googlecode.fascinator.common.messaging.MessagingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An AMQ Message broker
 * 
 * @author Greg Pendlebury
 */
public class MessageBroker {

    /** Directory to store operational AMQ data */
    public static final String DEFAULT_MESSAGING_HOME =
            FascinatorHome.getPath("activemq-data");

    /** Logging */
    private static Logger log = LoggerFactory.getLogger(MessageBroker.class);

    /** Reference counter */
    private static int refCount = 0;

    /** Messaging broker instance */
    private static MessageBroker instance;

    /**
     * Get messaging broker instance
     * 
     * @return Messaging broker instance
     * @throws MessagingException if an error occurred starting the broker
     */
    public static MessageBroker getInstance() throws MessagingException {
        if (instance == null) {
            instance = new MessageBroker();
        }
        refCount++;
        return instance;
    }

    /** System configuration */
    private JsonSimpleConfig config;

    /** AMQ Broker Monitor */
    private BrokerMonitor monitor;

    /** The actual broker */
    private BrokerService broker;

    /** Configurable list of message queues */
    private List<GenericListener> messageQueues;

    /** Timer to support staggered startup */
    private Timer timer;

    /**
     * Starts a connection to a message broker
     * 
     * @throws MessagingException if an error occurred starting the broker
     */
    private MessageBroker() throws MessagingException {
        try {
            log.debug("Starting message broker...");

            // Read configuration
            config = new JsonSimpleConfig();
            String dataDir = config.getString(
                    DEFAULT_MESSAGING_HOME,
                    "messaging", "home");
            String brokerUrl = config.getString(
                    ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL,
                    "messaging", "url");
            String stompUrl = config.getString(null, "messaging", "stompUrl");

            // Build our broker
            broker = new BrokerService();
            broker.setDataDirectory(dataDir);
            try {
                broker.addConnector(brokerUrl);
            } catch (Exception e) {
                log.error("Failed to start broker: {}", e);
                throw new MessagingException("Error starting AMQ Broker: ", e);
            }
            if (stompUrl != null) {
                try {
                    broker.addConnector(stompUrl);
                } catch (Exception e) {
                    log.error("An error occuring adding a stomp connector: {}",
                            e);
                }
            }

            // Attach a monitor for statistics
            enableAMQStatistics(broker);

            // Start it up
            try {
                broker.start();
            } catch (Exception e) {
                log.error("Failed to start broker: {}", e);
                throw new MessagingException("Error starting AMQ Broker: ", e);
            }

            // Wait for startup before we continue
            int counter = 0;
            while (counter < 10 && !broker.isStarted()) {
                log.debug("Waiting for broker to start: {}s...", counter);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // So nothing
                }
                counter++;
            }

            // Sanity check
            if (!broker.isStarted()) {
                log.error("AMQ broker still has not started after 10s.");
                throw new MessagingException("AMQ Broker is taking too long to boot!");
            }

            // Now start configured message queues, there are some timing issues
            //  here because Solr is typically started in a seperate context on
            //  the server. The dev build inside Maven may not notice this.
            // We've delayed first execution by 5s and this is typically enough,
            //  but we'll retry every 15s if the first one doesn't load.
            int delay = config.getInteger(5000, "messaging", "startup", "delay");
            int timeout = config.getInteger(15000, "messaging", "startup", "timer");
            timer = new Timer("StartIndexer", true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    startMessageQueues();
                }
            }, delay, timeout);
        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
        }
    }

    /**
     * Setup the broker so it will respond to statistics queries.
     *
     * @param broker to enable statistics for
     */
    private void enableAMQStatistics(BrokerService brokerService) {
        // Start our stats plugin
        StatisticsBrokerPlugin statsPlugin = new StatisticsBrokerPlugin();
        // Find what plugins are already present
        BrokerPlugin[] aPlugins = brokerService.getPlugins();
        if (aPlugins == null) aPlugins = new BrokerPlugin[] {};
        // Add stats to the list
        List<BrokerPlugin> lPlugins = new ArrayList<BrokerPlugin>();
        lPlugins.addAll(Arrays.asList(aPlugins));
        lPlugins.add(statsPlugin);
        // Setup the broker
        broker.setPlugins(lPlugins.toArray(aPlugins));
        broker.setEnableStatistics(true);
    }

    /**
     * Startup our message queues.
     *
     */
    private void startMessageQueues() {
        log.info("Starting Message Queues...");
        if (messageQueues == null) {
            messageQueues = new ArrayList<GenericListener>();
        }
        List<JsonSimple> threadConfig =
                config.getJsonSimpleList("messaging", "threads");

        try {
            // Start the AMQ monitor
            if (monitor == null) {
                monitor = new BrokerMonitor(broker);
                // One behind house keeping
                monitor.setPriority(Thread.MAX_PRIORITY - 1);
                monitor.start();
            }

            // Start our configurable queues
            for (JsonSimple thread : threadConfig) {
                String classId = thread.getString(null, "id");
                String priority = thread.getString(null, "priority");
                if (classId != null) {
                    GenericListener queue = getListener(classId);
                    if (queue != null) {
                        if (priority != null) {
                            queue.setPriority(Integer.valueOf(priority));
                        }
                        queue.init(new JsonSimpleConfig(thread.toString()));
                        queue.start();
                        messageQueues.add(queue);
                    } else {
                        throw new Exception("Failed to find Listener: '" +
                            classId + "'");
                    }
                } else {
                    throw new Exception("No message classId provided: '" +
                            thread.toString() + "'");
                }
            }
            log.info("All Message Queues started successfully");

            // All is good, stop our callback
            timer.cancel();
            timer = null;

        } catch (Exception e) {
            log.warn("Message queues startup failed. Shutting them down...", e);
            for (GenericListener queue : messageQueues) {
                try {
                    queue.stop();
                } catch (Exception ex) {
                    log.error("Failed to stop listener '{}': {}",
                            queue.getId(), ex.getMessage());
                }
            }
            messageQueues = null;
        }
    }

    /**
     * Get a message listener from the ServiceLoader
     *
     * @param id Listener identifier
     * @return GenericMessageListener implementation matching the ID, if found
     */
    private GenericListener getListener(String id) {
        ServiceLoader<GenericListener> listeners =
                ServiceLoader.load(GenericListener.class);
        for (GenericListener listener : listeners) {
            if (id.equals(listener.getId())) {
                return listener;
            }
        }
        return null;
    }

    /**
     * Get the current number of references in use for this class
     */
    public int getRefCount() {
        return refCount;
    }

    /**
     * Shutdown the message broker.
     * 
     * @return boolean True if shutdown successful, otherwise False
     */
    public boolean shutdown() {
        refCount--;
        if (instance != null && refCount <= 0) {
            log.info("Shutting down message broker");

            if (messageQueues != null) {
                for (GenericListener queue : messageQueues) {
                    try {
                        queue.stop();
                    } catch (Exception e) {
                        log.error("Failed to stop listener '{}': {}",
                                queue.getId(), e.getMessage());
                    }
                }
            }

            try {
                broker.stop();
            } catch (Exception e) {
                log.error("Failed to stop message broker: {}", e.getMessage());
                return false;
            }
            if (monitor != null) {
                monitor.stop();
            }

            refCount = 0;
            instance = null;
            return true;
        }
        return true;
    }
}
