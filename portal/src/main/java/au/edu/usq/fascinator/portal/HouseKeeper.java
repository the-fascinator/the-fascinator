/*
 * The Fascinator - Portal - House Keeper
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
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonObject;
import au.edu.usq.fascinator.common.JsonSimple;
import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.common.storage.StorageUtils;
import au.edu.usq.fascinator.portal.quartz.ExternalJob;
import au.edu.usq.fascinator.portal.quartz.HarvestJob;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.io.IOUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * The House Keeper is a messaging object that periodically wakes itself
 * up to look for routine maintenance tasks requiring attention.
 *
 * @author Greg Pendlebury
 */
public class HouseKeeper implements GenericListener {

    /** House Keeping queue */
    public static final String QUEUE_ID = "houseKeeping";

    /** Default timeout = 5 mins */
    public static final Integer DEFAULT_TIMEOUT = 300;

    /** JDBC Driver */
    private static String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    /** Connection string prefix */
    private static String DERBY_PROTOCOL = "jdbc:derby:";

    /** HouseKeeping database name */
    private static String HK_DATABASE = "housekeeping";

    /** Notifications table */
    private static String NOTIFICATIONS_TABLE = "notifications";

    /** Quartz properties */
    private static String DEFAULT_QUARTZ_FILE = "quartz.properties";

    /** Logging */
    private Logger log = LoggerFactory.getLogger(HouseKeeper.class);

    /** System configuration */
    private JsonSimpleConfig globalConfig;

    /** Database home directory */
    private String derbyHome;

    /** Database connection */
    private java.sql.Connection dbConnection;

    /** JMS connection */
    private javax.jms.Connection connection;

    /** JMS Session - Producer */
    private Session pSession;

    /** JMS Session - Consumer */
    private Session cSession;

    /** Indexer object */
    private Indexer indexer;

    /** Storage */
    private Storage storage;

    /** Message Consumer instance */
    private MessageConsumer consumer;

    /** Message Producer instance */
    private MessageProducer producer;

    /** Message Destination - House Keeping*/
    private Queue destHouseKeeping;

    /** Cached list of actions needing attention */
    private List<UserAction> actions;

    /** Timer for callback events */
    private Timer timer;

    /** Callback timeout for house keeping (in seconds) */
    private long timeout;

    /** Thread reference */
    private Thread thread;

    /** Queue data */
    private Map<String, Map<String, String>> stats;

    /** Startup flag for completion */
    private boolean renderReady = false;

    /** Quartz Scheduler */
    private Scheduler scheduler;

    /** Config for scheduled jobs */
    private List<JsonSimple> jobConfig;

    /**
     * Switch log file
     *
     */
    private void openLog() {
        MDC.put("name", QUEUE_ID);
    }

    /**
     * Revert log file
     *
     */
    private void closeLog() {
        MDC.remove("name");
    }

    /**
     * Constructor required by ServiceLoader. Be sure to use init()
     *
     */
    public HouseKeeper() {
        thread = new Thread(this, QUEUE_ID);
    }

    /**
     * Start thread running
     *
     */
    @Override
    public void run() {
        openLog();
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

            Destination destination = cSession.createQueue(QUEUE_ID);
            consumer = cSession.createConsumer(destination);
            consumer.setMessageListener(this);

            // Producer
            destHouseKeeping = pSession.createQueue(QUEUE_ID);
            producer = pSession.createProducer(null);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            connection.start();

            // Database prep work
            try {
                // Look for our table
                checkTable(NOTIFICATIONS_TABLE);
                // Sync in-memory actions to database
                syncActionList();
                // Purge any old 'block' entries since we just (re)started
                for (UserAction ua : actions) {
                    if (ua.block) {
                        removeAction(ua.id);
                    }
                }
            } catch (SQLException ex) {
                log.error("Error during database preparation:", ex);
            }
            log.debug("Derby house keeping database online!");

            // Quartz Scheduler
            try {
                scheduler = StdSchedulerFactory.getDefaultScheduler();
                scheduler.start();
                quartzScheduling();
            } catch (SchedulerException ex) {
                log.error("Scheduled failed to start: ", ex);
            }

            // Start our callback timer
            log.info("Starting callback timer. Timeout = {}s", timeout);
            timer = new Timer("HouseKeeping", true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    onTimeout();
                }
            }, 0, timeout * 1000);

        } catch (IOException ex) {
            log.error("Unable to read config!", ex);
        } catch (JMSException ex) {
            log.error("Error starting message thread!", ex);
        }
        closeLog();
    }

    private java.sql.Connection dbConnection() throws SQLException {
        if (dbConnection == null || !dbConnection.isValid(1)) {
            // At least try to close if not null... even though its not valid
            if (dbConnection != null) {
                log.error("!!! Database connection has failed, recreating.");
                try {
                    dbConnection.close();
                } catch (SQLException ex) {
                    log.error("Error closing invalid connection, ignoring: {}",
                            ex.getMessage());
                }
            }

            // Open a new connection
            Properties props = new Properties();
            // Load the JDBC driver
            try {
                Class.forName(DERBY_DRIVER).newInstance();
            } catch (Exception ex) {
                log.error("Driver load failed: ", ex);
                throw new SQLException("Driver load failed: ", ex);
            }

            // Establish a database connection
            dbConnection = DriverManager.getConnection(DERBY_PROTOCOL +
                    HK_DATABASE + ";create=true", props);
        }
        return dbConnection;
    }

    /**
     * Wrapper method to encapsulate the scheduling logic.
     *
     */
    private void quartzScheduling() {
        // Loop through each job
        for (JsonSimple thisJob : jobConfig) {
            String name = thisJob.getString(null, "name");
            if (name == null) {
                log.error("Configuration error. " +
                        "All jobs must have a 'name' value.");
                continue;
            }

            // Step 1 - What type of job?
            String type = thisJob.getString(null, "type");
            if (type == null) {
                // External is the default, also provides legacy support
                type = "external";
            }
            if (!(type.equals("external") || type.equals("harvest"))) {
                log.error("Unknown job type provided ('{}')", type);
                continue;
            }

            // Step 2 - Do we have a valid trigger?
            CronTrigger trigger = null;
            try {
                trigger = new CronTrigger(name, null,
                        thisJob.getString(null, "timing"));
                log.info("Scheduling Job: '{}' => '{}'",
                        name, thisJob.getString(null, "timing"));
                //log.debug("Job timing: \n===\n{}\n===\n",
                //        trigger.getExpressionSummary());
            } catch (ParseException ex) {
                log.error("Error in job timing ('{}')", name, ex);
                continue;
            }

            // Step 3 - Prepare the job
            JobDetail job = null;
            // External jobs
            if (type.equals("external")) {
                job = new JobDetail(name, null, ExternalJob.class);
                String token = thisJob.getString(null, "token");
                String urlString = thisJob.getString(null, "url");
                if (urlString == null) {
                    log.error("No job URL in config ('{}')", name);
                    continue;
                }
                try {
                    // Make sure the URL is valid... we don't need it though
                    URL url = new URL(urlString);
                    job.getJobDataMap().put("url", urlString);
                    if (token != null) {
                        job.getJobDataMap().put("token", token);
                    }
                } catch (MalformedURLException ex) {
                    log.error("Invalid URL: '{}'", urlString, ex);
                    continue;
                }
            }
            // Harvest jobs
            if (type.equals("harvest")) {
                job = new JobDetail(name, null, HarvestJob.class);
                String configFile = thisJob.getString(null, "configFile");
                if (configFile == null) {
                    log.error("No config path provided ('{}')", configFile);
                    continue;
                }
                job.getJobDataMap().put("configFile", configFile);
            }

            // Step 4 - Schedule the job
            try {
                scheduler.scheduleJob(job, trigger);
            } catch (SchedulerException ex) {
                log.error("Error scheduling job: ", ex);
            }
        }
    }

    /**
     * Initialization method
     *
     * @param config Configuration to use
     * @throws Exception for any failure
     */
    @Override
    public void init(JsonSimpleConfig config) throws Exception {
        openLog();
        try {
            log.info("=================");
            log.info("Starting House Keeping object");
            // Configuration
            globalConfig = new JsonSimpleConfig();
            timeout = config.getInteger(DEFAULT_TIMEOUT, "config", "frequency");

            // Find data directory
            derbyHome = config.getString(null,
                    "database-service", "derbyHome");
            String oldHome = System.getProperty("derby.system.home");

            // Derby's data directory has already been configured
            if (oldHome != null) {
                if (derbyHome != null) {
                    // Use the existing one, but throw a warning
                    log.warn("Using previously specified data directory:" +
                            " '{}', provided value has been ignored: '{}'",
                            oldHome, derbyHome);
                } else {
                    // This is ok, no configuration conflicts
                    log.info("Using existing data directory: '{}'", oldHome);
                }

            // We don't have one, config MUST have one
            } else {
                if (derbyHome == null) {
                    log.error("No database home directory configured!");
                    return;
                } else {
                    // Establish its validity and existance, create if necessary
                    File file = new File(derbyHome);
                    if (file.exists()) {
                        if (!file.isDirectory()) {
                            throw new Exception("Database home '" +
                                    derbyHome + "' is not a directory!");
                        }
                    } else {
                        file.mkdirs();
                        if (!file.exists()) {
                            throw new Exception("Database home '" +
                                    derbyHome +
                                    "' does not exist and could not be created!");
                        }
                    }
                    System.setProperty("derby.system.home", derbyHome);
                }
            }

            File sysFile = JsonSimpleConfig.getSystemFile();
            stats = new LinkedHashMap();

            // Quartz Scheduler properties file
            String quartzConfig = config.getString(null,
                    "config", "quartzConfig");
            if (quartzConfig == null) {
                throw new Exception(
                    "No scheduling config provided: 'config/quartzConfig'!");
            }
            File quartzProps = new File(quartzConfig);
            if (!quartzProps.exists()) {
                log.warn("Quartz config file '{}' not found, deploying default",
                        quartzConfig);
                quartzProps.getParentFile().mkdirs();
                OutputStream out = new FileOutputStream(quartzProps);
                IOUtils.copy(getClass().getResourceAsStream("/" +
                        DEFAULT_QUARTZ_FILE), out);
                out.close();
                log.info("Default configuration copied to '{}'", quartzProps);
            }
            if (quartzProps != null && quartzProps.exists()) {
                System.setProperty("org.quartz.properties",
                        quartzProps.getAbsolutePath());
            }

            /** Get the config for scheduled jobs */
            jobConfig = JsonSimple.toJavaList(
                    config.getArray("config", "jobs"));

            // Initialise plugins
            indexer = PluginManager.getIndexer(
                    globalConfig.getString("solr", "indexer", "type"));
            indexer.init(sysFile);
            storage = PluginManager.getStorage(
                    globalConfig.getString("file-system", "storage", "type"));
            storage.init(sysFile);

        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
            throw ioe;
        } catch (PluginException pe) {
            log.error("Failed to initialise plugin: {}", pe.getMessage());
            throw pe;
        } finally {
            closeLog();
        }
    }

    /**
     * Return the ID string for this listener
     *
     */
    @Override
    public String getId() {
        return QUEUE_ID;
    }

    /**
     * Start the queue
     *
     * @throws Exception if an error occurred starting the JMS connections
     */
    @Override
    public void start() throws Exception {
        thread.start();
    }

    /**
     * Stop the House Keeper. Including stopping the storage and
     * indexer
     */
    @Override
    public void stop() throws Exception {
        openLog();
        log.info("Stopping House Keeping object...");
        timer.cancel();
        if (indexer != null) {
            try {
                indexer.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown indexer: {}", pe.getMessage());
                closeLog();
                throw pe;
            }
        }
        if (storage != null) {
            try {
                storage.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown storage: {}", pe.getMessage());
                closeLog();
                throw pe;
            }
        }
        if (consumer != null) {
            try {
                consumer.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close consumer: {}", jmse.getMessage());
                closeLog();
                throw jmse;
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

        // Derby can only be shutdown from one thread,
        //    we'll catch errors from the rest.
        String threadedShutdownMessage = DERBY_DRIVER
                + " is not registered with the JDBC driver manager";
        try {
            // Tell the database to close
            DriverManager.getConnection(DERBY_PROTOCOL + ";shutdown=true");
            // Shutdown just this database (but not the engine)
            //DriverManager.getConnection(DERBY_PROTOCOL + SECURITY_DATABASE +
            //        ";shutdown=true");
        } catch (SQLException ex) {
            // These test values are used if the engine is NOT shutdown
            //if (ex.getErrorCode() == 45000 &&
            //        ex.getSQLState().equals("08006")) {

            // Valid response
            if (ex.getErrorCode() == 50000 &&
                    ex.getSQLState().equals("XJ015")) {
            // Error response
            } else {
                // Make sure we ignore simple thread issues
                if (!ex.getMessage().equals(threadedShutdownMessage)) {
                    log.warn("Error during database shutdown:", ex);
                }
            }
        } finally {
            try {
                // Close our connection
                if (dbConnection != null) {
                    dbConnection.close();
                    dbConnection = null;
                }
            } catch (SQLException ex) {
                log.warn("Error closing connection:", ex);
            }
        }

        // Shutdown the scheduler
        if (scheduler != null) {
            try {
                scheduler.shutdown();
            } catch (SchedulerException ex) {
                log.warn("Failed to close connection: {}", ex);
            }
        }

        closeLog();
    }

    /**
     * Callback function for periodic house keeping.
     *
     */
    private void onTimeout() {
        openLog();

        // Make sure thread priority is correct
        if (!Thread.currentThread().getName().equals(thread.getName())) {
            Thread.currentThread().setName(thread.getName());
            Thread.currentThread().setPriority(thread.getPriority());
        }

        // Perform our 'boot time' house keeping
        checkSystemConfig(); /* <<< Always first, likely to request reboot */
        syncHarvestFiles();

        renderReady = true;
        closeLog();
    }

    /**
     * Callback function for incoming messages sent directly to housekeeping.
     *
     * @param message The incoming message
     */
    @Override
    public void onMessage(Message message) {
        openLog();
        try {
            // Make sure thread priority is correct
            if (!Thread.currentThread().getName().equals(thread.getName())) {
                Thread.currentThread().setName(thread.getName());
                Thread.currentThread().setPriority(thread.getPriority());
            }

            String text = ((TextMessage) message).getText();
            JsonSimple msgJson = new JsonSimple(text);
            //log.debug("Message\n{}", msgJson.toString());

            String msgType = msgJson.getString(null, "type");
            if (msgType == null) {
                log.error("No message type set!");
                closeLog();
                return;
            }

            // Stop the system from working until a restart occurs
            if (msgType.equals("blocking-restart")) {
                UserAction ua = new UserAction();
                ua.block = true;
                ua.message = "Changes made to the system require a restart. " +
                        "Please restart the system before normal " +
                        "functionality can resume.";
                storeAction(ua);
            }

            // Request a restart, not required though
            if (msgType.equals("basic-restart")) {
                UserAction ua = new UserAction();
                ua.block = false;
                ua.message = "Changes made to the system require a restart.";
                storeAction(ua);
            }

            // Harvest file update
            if (msgType.equals("harvest-update")) {
                String oid = msgJson.getString(null, "oid");
                if (oid != null) {
                    UserAction ua = new UserAction();
                    ua.block = false;
                    ua.message = ("A harvest file has been updated: '"
                            + oid + "'");
                    storeAction(ua);
                } else {
                    log.error("Invalid message, no harvest file OID provided!");
                }
            }

            // User notications
            if (msgType.equals("user-notice")) {
                String messageText = msgJson.getString(null, "message");
                if (messageText != null) {
                    UserAction ua = new UserAction();
                    ua.block = false;
                    ua.message = messageText;
                    storeAction(ua);
                } else {
                    this.log.error("Invalid notice, no message text provided!");
                }
            }

            // Statistics update from Broker Monitor
            if (msgType.equals("broker-update")) {
                Map<String, JsonSimple> queues = JsonSimple.toJavaMap(
                        msgJson.getObject("stats"));
                for (String q : queues.keySet()) {
                    JsonSimple qData = queues.get(q);
                    Map<String, String> qStats = new HashMap();
                    qStats.put("total",  qData.getString(null, "total"));
                    qStats.put("lost",   qData.getString(null, "lost"));
                    qStats.put("memory", qData.getString(null, "memory"));
                    qStats.put("size",   qData.getString(null, "size"));
                    // Round to an integer value
                    int spd = Float.valueOf(
                            qData.getString(null, "speed")).intValue();
                    qStats.put("speed",   String.valueOf(spd));
                    // Change from milliseconds to seconds
                    float avg = Float.valueOf(
                            qData.getString(null, "average")) / 1000;
                    // Round to two digits
                    avg = Math.round(avg * 100)/100;
                    qStats.put("average", String.valueOf(avg));
                    stats.put(q, qStats);
                }
            }

            // 'Refresh' received, check config and reset timer
            if (msgType.equals("refresh")) {
                log.info("Refreshing House Keeping");
                globalConfig = new JsonSimpleConfig();
                timeout = globalConfig.getInteger(DEFAULT_TIMEOUT,
                        "portal", "houseKeeping", "config", "frequency");
                log.info("Starting callback timer. Timeout = {}s", timeout);
                timer.cancel();
                timer = new Timer("HouseKeeping", true);
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        onTimeout();
                    }
                }, 0, timeout * 1000);

                // Show a message for the user
                UserAction ua = new UserAction();
                ua.block = false;
                ua.message = ("House Keeping is restarting. Frequency = " +
                        timeout + "s");
                storeAction(ua);
            }
        } catch (JMSException jmse) {
            log.error("Failed to receive message: {}", jmse.getMessage());
        } catch (IOException ioe) {
            log.error("Failed to parse message: {}", ioe.getMessage());
        }
        closeLog();
    }

    /**
     * Get the messages to display for the user
     *
     * @returns List<UserAction> The current list of message
     */
    public List<UserAction> getUserMessages() {
        // Only runs on the first page load after server start. Make sure
        //  house keeping has run once before returning
        if (!renderReady) {
            openLog();
            log.debug("Holding page render until first house keeping completes.");
            int i = 0;
            while (!renderReady && i < 200) {
                i++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    // Do nothing
                }
            }

            // We need to make sure problems in house keeping don't
            //   cause larger problems though
            if (renderReady) {
                log.debug("Resuming page render (after {} sleeps).", i);
            } else {
                log.error("House keeping has been holding page render for " +
                        "more than 20s and still has not completed. There " +
                        "are likely house keeping errors to address.");
                renderReady = true;
            }
            closeLog();
        }

        return actions;
    }

    /**
     * Confirm and remove a message/action
     *
     * @param actionId The ID of the action to remove
     */
    public void confirmMessage(String actionId) throws Exception {
        openLog();
        int id = -1;
        try {
            id = Integer.parseInt(actionId);
        } catch (Exception ex) {
            closeLog();
            log.error("Invalid message ID provided: ", ex);
            throw new Exception("Invalid message ID provided!");
        }
        // Find the message first
        boolean found = false;
        for (UserAction ua : actions) {
            if (ua.id == id) {
                found = true;
                // You can't confirm a blocking message from the UI
                if (ua.block) {
                    closeLog();
                    log.error("Trying to delete a blocked message! '{}'",
                            actionId);
                    throw new Exception("Sorry, but you can't delete that" +
                            " message. A restart is required!");
                }
            }
        }
        // Couldn't find the message
        if (!found) {
            closeLog();
            throw new Exception("Message '" + actionId + "' does not exist!");
        }

        // Do the actual removal
        try {
            removeAction(id);
        } catch (SQLException ex) {
            closeLog();
            log.error("Databases access error: ", ex);
            throw new Exception("Error deleting message, " +
                    "please check administration log files.");
        }
        closeLog();
    }

    /**
     * During portal startup, make sure the system config file is up-to-date.
     *
     */
    private void checkSystemConfig() {
        log.info("Checking system config files ...");
        boolean fine = true;

        // Higher priority, so go first
        if (globalConfig.isOutdated()) {
            fine = false;
            UserAction ua = new UserAction();
            ua.block = true;
            // The settings template is looking for this message
            ua.message = "out-of-date";
            storeAction(ua);
        }

        if (!globalConfig.isConfigured()) {
            fine = false;
            UserAction ua = new UserAction();
            ua.block = true;
            // The settings template is looking for this message
            ua.message = "configure";
            storeAction(ua);
        }

        if (fine) {
            log.info("... system config files are OK.");
        } else {
            log.warn("... problems found in system config files.");
        }
    }

    /**
     * During portal startup, we should check to ensure our harvest files
     * are up-to-date with those in storage.
     *
     */
    private void syncHarvestFiles() {
        // Get the harvest files directory
        String harvestPath = globalConfig.getString(null,
                "portal", "harvestFiles");
        if (harvestPath == null) {
            return;
        }

        // Make sure the directory exists
        File harvestDir = new File(harvestPath);
        if (!harvestDir.exists() || !harvestDir.isDirectory()) {
            return;
        }

        // Loop through the files from the directory
        for (File file : getFiles(harvestDir)) {
            DigitalObject object = null;
            try {
                // Check for the file in storage
                object = StorageUtils.checkHarvestFile(storage, file);
            } catch (StorageException ex) {
                log.error("Error during harvest file check: ", ex);
            }

            if (object != null) {
                // Generate a message to ourself. This merges with other places
                //   where the update occurs (like the HarvestClient).
                log.debug("Harvest file updated: '{}'", file.getAbsolutePath());
                JsonObject message = new JsonObject();
                message.put("type", "harvest-update");
                message.put("oid",  object.getId());
                try {
                    sendMessage(destHouseKeeping, message.toString());
                } catch (JMSException ex) {
                    log.error("Couldn't message House Keeping!", ex);
                }
            }
        }
    }

    /**
     * Recursively generate a list of file in directory and sub-directories.
     *
     * @param dir The directory to list
     * @return List<File> The list of files
     */
    private List<File> getFiles(File dir) {
        List files = new ArrayList();
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(getFiles(file));
            } else {
                files.add(file);
            }
        }
        return files;
    }

    /**
     * Send an update to House Keeping
     *
     * @param message Message to be sent
     */
    private void sendMessage(Destination destination, String message)
            throws JMSException {
        TextMessage msg = pSession.createTextMessage(message);
        producer.send(destination, msg);
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

    /**
     * Get the latest statistics on message queues.
     *
     * @return Map<String, Map<String, String>> of all queues and their statistics
     */
    public Map<String, Map<String, String>> getQueueStats() {
        return stats;
    }

    /**
     * Check if the message provided is already in the list of current actions
     *
     * @param message The message to look for
     * @param boolean Flag set if the message is found
     */
    private boolean isCurrentAction(String message) {
        for (UserAction ua : actions) {
            if (ua.message.equals(message)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove an old action from the database and update the action queue.
     *
     * @param action The User action to store
     */
    private void removeAction(int id) throws SQLException {
        log.debug("Removing action: '{}'", id);
        // Prepare our query
        PreparedStatement sql = dbConnection().prepareCall(
                "DELETE FROM " + NOTIFICATIONS_TABLE + " WHERE id = ?");
        // Run the query
        sql.setInt(1, id);
        sql.executeUpdate();
        close(sql);
        // Update memory cache
        syncActionList();
    }

    /**
     * Store a new action in the database and update the action queue.
     *
     * @param action The User action to store
     */
    private void storeAction(UserAction action) {
        // Don't store duplicates
        if (isCurrentAction(action.message)) {
            return;
        }
        // Capture the current time
        if (action.date == null) {
            action.date = new Date();
        }
        // Otherwise proceed as normal
        try {
            log.debug("Storing action: '{}'", action.message);
            // Prepare our query
            PreparedStatement sql = dbConnection().prepareCall(
                    "INSERT INTO " + NOTIFICATIONS_TABLE +
                    " (block, message, datetime) VALUES (?, ?, ?)");
            // Run the query
            sql.setBoolean(1, action.block);
            sql.setString(2, action.message);
            sql.setTimestamp(3, new Timestamp(action.date.getTime()));
            sql.executeUpdate();
            close(sql);
            // Update memory cache
            syncActionList();
        } catch (SQLException ex) {
            log.error("Error accessing database: ", ex);
        }
    }

    /**
     * Rebuild the in-memory list of actions from the database.
     *
     */
    private void syncActionList() {
        // Purge current list
        actions = new ArrayList();
        try {
            // Prepare our query
            PreparedStatement sql = dbConnection().prepareCall(
                    "SELECT * FROM " + NOTIFICATIONS_TABLE +
                    " ORDER BY block DESC, id");
            // Run the query
            ResultSet result = sql.executeQuery();
            // Build our list
            while (result.next()) {
                UserAction ua = new UserAction();
                ua.id = result.getInt("id");
                ua.block = result.getBoolean("block");
                ua.message = result.getString("message");
                ua.date = new Date(result.getTimestamp("datetime").getTime());
                actions.add(ua);
            }
            // Release the resultset
            close(result);
            close(sql);
        } catch (SQLException ex) {
            log.error("Error accessing database: ", ex);
        }
    }

    /**
     * Check for the existence of a table and arrange for its creation if
     * not found.
     *
     * @param table The table to look for and create.
     * @throws SQLException if there was an error.
     */
    private void checkTable(String table) throws SQLException {
        boolean tableFound = findTable(table);

        // Create the table if we couldn't find it
        if (!tableFound) {
            log.debug("Table '{}' not found, creating now!", table);
            createTable(table);

            // Double check it was created
            if (!findTable(table)) {
                log.error("Unknown error creating table '{}'", table);
                throw new SQLException(
                        "Could not find or create table '" + table + "'");
            }
        }
    }

    /**
     * Check if the given table exists in the database.
     *
     * @param table The table to look for
     * @return boolean flag if the table was found or not
     * @throws SQLException if there was an error accessing the database
     */
    private boolean findTable(String table) throws SQLException {
        boolean tableFound = false;
        DatabaseMetaData meta = dbConnection().getMetaData();
        ResultSet result = (ResultSet) meta.getTables(null, null, null, null);
        while (result.next() && !tableFound) {
            if (result.getString("TABLE_NAME").equalsIgnoreCase(table)) {
                tableFound = true;
            }
        }
        close(result);
        return tableFound;
    }

    /**
     * Create the given table in the database.
     *
     * @param table The table to create
     * @throws SQLException if there was an error during creation,
     *                      or an unknown table was specified.
     */
    private void createTable(String table) throws SQLException {
        if (table.equals(NOTIFICATIONS_TABLE)) {
            Statement sql = dbConnection().createStatement();
            sql.execute(
                    "CREATE TABLE " + NOTIFICATIONS_TABLE +
                    "(id INTEGER NOT NULL GENERATED ALWAYS AS " +
                    "IDENTITY (START WITH 1, INCREMENT BY 1), " +
                    "block CHAR(1) NOT NULL, " +
                    "message VARCHAR(4000) NOT NULL, " +
                    "datetime TIMESTAMP NOT NULL, " +
                    "PRIMARY KEY (id))");
            close(sql);
            return;
        }
        throw new SQLException("Unknown table '" + table + "' requested!");
    }

    /**
     * Attempt to close a ResultSet. Basic wrapper for exception
     * catching and logging
     *
     * @param resultSet The ResultSet to try and close.
     */
    private void close(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException ex) {
                log.error("Error closing result set: ", ex);
            }
        }
        resultSet = null;
    }

    /**
     * Attempt to close a Statement. Basic wrapper for exception
     * catching and logging
     *
     * @param statement The Statement to try and close.
     */
    private void close(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ex) {
                log.error("Error closing statement: ", ex);
            }
        }
        statement = null;
    }
}
