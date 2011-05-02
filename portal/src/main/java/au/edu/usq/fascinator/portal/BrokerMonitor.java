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
package au.edu.usq.fascinator.portal;

import au.edu.usq.fascinator.common.GenericListener;
import au.edu.usq.fascinator.common.JsonObject;
import au.edu.usq.fascinator.common.JsonSimple;
import au.edu.usq.fascinator.common.JsonSimpleConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.BrokerView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Monitoring class for the AMQ Broker. Periodically sends updated statistics
 * to the house keeper.
 *
 * @author Greg Pendlebury
 */
public class BrokerMonitor implements GenericListener {

    /** Queue string */
    public static final String QUEUE_ID = "BrokerMonitor";

    /** Broker queue prefix for stats */
    public static final String STATS_PREFIX = "ActiveMQ.Statistics.Destination";

    /** Default timeout for callback 'tick' = 10 seconds */
    public static final long DEFAULT_TIMEOUT = 10;

    /** Default timeout for AMQ stats response = 500 milliseconds */
    public static final long AMQ_TIMEOUT = 500;

    /** Logging */
    private Logger log = LoggerFactory.getLogger(BrokerMonitor.class);

    /** Global Configuration */
    private JsonSimpleConfig globalConfig;

    /** JMS connection */
    private Connection connection;

    /** JMS Session - Producer */
    private Session pSession;

    /** JMS Session - Consumer */
    private Session cSession;

    /** Message Destination - This object */
    private Queue destStatsUpdate;

    /** Message Destination - House Keeping*/
    private Queue destHouseKeeping;

    /** Message Consumer instance */
    private MessageConsumer consumer;

    /** Message Producer instance */
    private MessageProducer producer;

    /** JMX view of the AMQ Broker */
    private BrokerView monitor;

    /** JMX view of the AMQ Broker */
    private BrokerService broker;

    /** Timer for callback events */
    private Timer timer;

    /** Queue list */
    private List<String> queues;

    /** Number of queues */
    private int numQueues = -1;

    /** Queue data */
    private Map<String, Map<String, String>> stats;

    /** Queue display order **/
    private List<String> statsOrder;

    /** Flag when the broker has responded with stats */
    private boolean statsReceived = false;

    /** Stats Queues */
    private Map<String, Queue> targetQueues;

    /** Flag for first callback execution */
    private boolean firstRun = true;

    /** Thread reference */
    private Thread thread;

    /**
     * Constructor required by ServiceLoader
     *
     */
    public BrokerMonitor() {}

    /**
     * Constructor.
     *
     * @param brokerService AMQ Broker service we're monitoring
     * @throws Exception If unable to start properly
     */
    public BrokerMonitor(BrokerService brokerService) throws Exception {
        log.info("Starting Broker Monitor...");
        broker = brokerService;
        monitor = broker.getAdminView();

        // Local record keeping
        queues = new ArrayList();
        stats = new LinkedHashMap();
        targetQueues = new HashMap();

        // Thready stuff
        thread = new Thread(this, QUEUE_ID);
    }

    /**
     * Start thread running
     *
     */
    @Override
    public void run() {
        try {
            globalConfig = new JsonSimpleConfig();

            // Get a connection to the broker
            String brokerUrl = globalConfig.getString(
                    ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL,
                    "messaging", "url");
            ActiveMQConnectionFactory connectionFactory =
                    new ActiveMQConnectionFactory(brokerUrl);
            connection = connectionFactory.createConnection();

            // Sessions are not thread safe, to send a message outside
            //  of the onMessage() callback you need another session.
            cSession = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
            pSession = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);

            // Consumer - 'us'
            destStatsUpdate = cSession.createQueue(QUEUE_ID);
            consumer = cSession.createConsumer(destStatsUpdate);
            consumer.setMessageListener(this);

            // Producer
            destHouseKeeping = pSession.createQueue(HouseKeeper.QUEUE_ID);
            producer = pSession.createProducer(null);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            connection.start();

            // Display statistics in the order found in config
            statsOrder = new ArrayList();

            List<JsonSimple> qConfig = globalConfig.getJsonSimpleList(
                    "messaging", "threads");
            for (JsonSimple q : qConfig) {
                String name = q.getString(null, "config", "name");
                if (name != null) {
                    statsOrder.add(name);
                }
            }

            // Start our callback timer
            log.info("Starting callback timer. Timeout = {}s", DEFAULT_TIMEOUT);
            startTimer();

        } catch (IOException ex) {
            log.error("Unable to read config!", ex);
        } catch (JMSException ex) {
            log.error("Error starting message thread!", ex);
        }
    }

    /**
     * Start the callback timer
     *
     */
    private void startTimer(){
        // Stop the old timer if it is online
        if (timer != null) {
            // A tiny sleep should allow the broker time
            //  to come online if it isn't already.
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                // Just interrupted sleep, no big deal
            }
            //log.info("Restarting timer. Timeout = {}s", DEFAULT_TIMEOUT);
            timer.cancel();
            timer = null;
        }

        timer = new Timer("BrokerMonitor", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                onTimeout();
            }
        }, 0, DEFAULT_TIMEOUT * 1000);
    }

    /**
     * Initialization method
     *
     * @param config Configuration to use
     * @throws Exception if an error occurred
     */
    @Override
    public void init(JsonSimpleConfig config) throws Exception {
        // Doesn't matter for this class since
        //  the ServiceLoader doesn't see it
    }

    /**
     * Start the queue
     *
     * @throws Exception if an error occurred
     */
    @Override
    public void start() throws Exception {
        thread.start();
    }

    /**
     * Return the ID string for this listener.
     *
     */
    @Override
    public String getId() {
        // Doesn't matter for this class since
        //  the ServiceLoader doesn't see it
        return QUEUE_ID;
    }

    /**
     * Callback function for periodic house keeping.
     *
     */
    private void onTimeout() {
        MDC.put("name", QUEUE_ID);
        boolean send = false;

        // Make sure thread priority is correct
        if (!Thread.currentThread().getName().equals(thread.getName())) {
            Thread.currentThread().setName(thread.getName());
            Thread.currentThread().setPriority(thread.getPriority());
        }

        // Check our queue lists are up-to-date
        if (monitor.getQueues().length != numQueues) {
            // We use this variable because we will ignore
            //  statistics queues for our list
            numQueues = monitor.getQueues().length;
            refreshQueues();
            send = true;
        }

        // Update stats
        for (String q : queues) {
            // Send request for stats
            updateStats(q);
        }

        // Make sure we don't send until the
        //  broker has given us some stats
        if (!statsReceived) {
            startTimer();
            return;
        }

        // Force send on first (valid) execution
        //   if House Keeping is online
        if (firstRun) {
            firstRun = false;
            send = true;
        }

        // Build a stats message. Whilst we're at
        // it, check to see if anything changed.
        JsonObject msgJson = new JsonObject();
        JsonObject queueStats = new JsonObject();
        for (String queue : stats.keySet()) {
            JsonObject thisQueue = new JsonObject();
            Map<String, String> map = stats.get(queue);
            for (String key : map.keySet()) {
                String value = map.get(key);
                thisQueue.put(key, value);

                // Make sure we don't check housekeeping for
                // the send flag, it causes an infinite loop
                if (!queue.equals(HouseKeeper.QUEUE_ID)) {
                    // If the queue has new items, send
                    if (key.equals("size") && !value.equals("0")) send = true;
                    // Or we processed new items, send
                    if (key.equals("change") && !value.equals("0")) send = true;
                }
            }
            queueStats.put(queue, thisQueue);
        }
        msgJson.put("stats", queueStats);

        // Update housekeeping
        if (send) {
            // A small sleep to allow Broker time to respond to stats query
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                log.warn("Sleep interrupted!");
            }

            try {
                msgJson.put("type", "broker-update");
                sendUpdate(msgJson.toString());
            } catch (JMSException ex) {
                log.error("Failed messaging House Keeping!", ex);
            }
        }
    }

    /**
     * Check the active queues on the Broker and update our list if needed.
     *
     */
    private void refreshQueues() {
        // Get all the active queues
        for (Object object : monitor.getQueues()) {
            String rawData = object.toString();
            String[] datums = rawData.split(",");
            // We should have three fields
            if (datums.length == 3) {
                String queue = datums[2];
                if (queue.startsWith("Destination=")) {
                    queue = queue.substring(12);
                    // Ignore stats queues, and this queue
                    if (!(queue.startsWith(STATS_PREFIX)
                            || queue.equals(QUEUE_ID)
                            || queues.contains(queue))) {
                        log.debug("New queue found: '{}'", queue);
                        queues.add(queue);
                    }
                } else {
                    log.error("Unknown queue output format: '{}'", rawData);
                }
            } else {
                log.error("Unknown queue output string: '{}'", rawData);
            }
        }

        // Change the order to match the config file, followed
        // by any additional queues (eg. house keeping)
        List<String> temp = new ArrayList();
        temp.addAll(queues);
        queues = new ArrayList();
        // We need to keep our stats in order too
        Map<String, Map<String, String>> oldStats = new LinkedHashMap();
        oldStats.putAll(stats);
        Map<String, String> oldData;
        stats = new LinkedHashMap();

        // Config queues first
        for (String q : statsOrder) {
            if (temp.contains(q)) {
                queues.add(q);
                oldData = oldStats.get(q);
                if (oldData == null) {
                    stats.put(q, new HashMap());
                } else {
                    stats.put(q, oldData);
                }
            }
        }
        // Now the rest
        for (String q : temp) {
            if (!queues.contains(q)) {
                queues.add(q);
                oldData = oldStats.get(q);
                if (oldData == null) {
                    stats.put(q, new HashMap());
                } else {
                    stats.put(q, oldData);
                }
            }
        }
    }

    /**
     * Send a request to the broker for a statistics update.
     *
     */
    private void updateStats(String queue) {
        // Send/Receive requirements
        try {
            // The fake stats queue for our target
            String statsQueue = STATS_PREFIX + queue;
            if (!targetQueues.containsKey(statsQueue)) {
                targetQueues.put(statsQueue, pSession.createQueue(statsQueue));
            }
            Queue query = targetQueues.get(statsQueue);

            // Send an empty message
            Message msg = pSession.createMessage();
            msg.setJMSReplyTo(destStatsUpdate);
            producer.send(query, msg);

        } catch (JMSException ex) {
            // TODO : Write empty stats and an error message
            log.error("Failed to send statistics update request", ex);
        }
    }

    /**
     * Stop the Render Queue Consumer. Including stopping the storage and
     * indexer
     */
    @Override
    public void stop() {
        log.info("Stopping Broker Monitor...");
        timer.cancel();

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
                log.warn("Failed to close consumer: {}", jmse);
            }
        }
        if (cSession != null) {
            try {
                cSession.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close consumer session: {}", jmse);
            }
        }
        if (pSession != null) {
            try {
                pSession.close();
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
        try {
            // Make sure thread priority is correct
            if (!Thread.currentThread().getName().equals(thread.getName())) {
                Thread.currentThread().setName(thread.getName());
                Thread.currentThread().setPriority(thread.getPriority());
            }

            MapMessage reply = (MapMessage) message;
            if (reply != null && reply.getMapNames().hasMoreElements()) {
                parseStats(reply);
            }
        } catch (JMSException jmse) {
            log.error("Failed to parse message: {}", jmse.getMessage());
        }
    }

    /**
     * Parse the statistics response from the AMQ broker.
     *
     * @param stats The MapMessage response containing stats
     */
    private void parseStats(MapMessage message) throws JMSException {
        // Variables
        String queue, memory, size, average, oldTotalStr;
        int change, total, oldTotal, target, lost;
        float speed;
        // Get new data
        queue   = message.getString("destinationName").replace("queue://", "");
        memory  = message.getString("memoryPercentUsage");
        size    = message.getString("size");
        average = message.getString("averageEnqueueTime");
        total   = Integer.valueOf(message.getString("dequeueCount"));
        target  = Integer.valueOf(message.getString("enqueueCount"));
        lost    = -1 * (target - (total + Integer.valueOf(size)));

        if (queue != null) {
            statsReceived = true;

            // Get our old data and determine the change
            oldTotalStr = stats.get(queue).get("total");
            if (oldTotalStr == null) {
                // Use this to force a send, we just received stats
                //   for this queue for the first time
                firstRun = true;
                oldTotal = 0;
            } else {
                oldTotal = Integer.valueOf(oldTotalStr);
            }
            change = total - oldTotal;
            // * (60 / DEFAULT_TIMEOUT) = Messages per minute
            speed = (float) change * (60 / DEFAULT_TIMEOUT);

            Map<String, String> newData = new HashMap();
            newData.put("size",    size);
            newData.put("memory",  memory);
            newData.put("average", average);
            newData.put("total",   String.valueOf(total));
            newData.put("change",  String.valueOf(change));
            newData.put("speed",   String.valueOf(speed));
            newData.put("lost",    String.valueOf(lost));

            stats.put(queue, newData);
        }

        // Debugging - show everything
        /*
        for (Enumeration e = message.getMapNames();e.hasMoreElements();) {
            String name = e.nextElement().toString();
            log.debug("'{}' => '{}'", name, message.getObject(name));
        }
        */
    }

    /**
     * Send an update to House Keeping
     *
     * @param message Message to be sent
     */
    private void sendUpdate(String message) throws JMSException {
        TextMessage msg = pSession.createTextMessage(message);
        producer.send(destHouseKeeping, msg);
    }

    /**
     * Sets the priority level for the thread. Used by the OS.
     *
     * @param newPriority The priority level to set the thread at
     */
    @Override
    public void setPriority(int newPriority) {
        if (newPriority >= Thread.MIN_PRIORITY &&
            newPriority <= Thread.MAX_PRIORITY) {
            thread.setPriority(newPriority);
        }
    }
}
