/* 
 * The Fascinator - Portal
 * Copyright (C) 2008-2011 University of Southern Queensland
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
package com.googlecode.fascinator.portal.servlet;

import com.googlecode.fascinator.common.GenericListener;
import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.portal.BrokerMonitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.python.core.PySystemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h3>Introduction</h3>
 * <p>
 * This Servlet is mainly just a shell, because its doGet() and doPost() methods are empty. We are relying on Tapestry to do the true web server work later. What we do want here however is to instantiate anything we require to start with the server. Because Tapestry services are instantiated and injected on demand, we can't rely on them for some of our system components.
 * </p>
 *
 * <h3>Wiki Link</h3>
 * <p>
 * <b>https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Portal/JavaCore#OurWebServlet</b>
 * </p>
 *
 * @author Oliver Lucido
 */
@SuppressWarnings("serial")
public class IndexerServlet extends HttpServlet {

    /** Directory to store operational AMQ data */
    public static final String DEFAULT_MESSAGING_HOME = FascinatorHome
            .getPath("activemq-data");

    /** Logger */
    private Logger log = LoggerFactory.getLogger(IndexerServlet.class);

    /** Timer to support startup */
    private Timer timer;

    /** Configurable list of message queues */
    private List<GenericListener> messageQueues;

    /** Config */
    private JsonSimpleConfig config;

    /** AMQ Broker Monitor */
    private BrokerMonitor monitor;

    /**
     * activemq broker NOTE: Will use Fedora's broker if fedora is running,
     * otherwise use activemq broker
     */
    private BrokerService broker;

    /**
     * Initialise the Servlet, called at Server startup
     *
     * @throws ServletException If it found errors during startup
     */
    @Override
    public void init() throws ServletException {
        // configure the broker
        try {
            config = new JsonSimpleConfig();

            String dataDir = config.getString(DEFAULT_MESSAGING_HOME,
                    "messaging", "home");
            broker = new BrokerService();
            broker.setDataDirectory(dataDir);
            broker.addConnector(config.getString(
                    ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL,
                    "messaging", "url"));
            String stompUrl = config.getString(null, "messaging", "stompUrl");
            if (stompUrl != null) {
                broker.addConnector(stompUrl);
            }
            enableAMQStatistics(broker);
            broker.start();
        } catch (Exception e) {
            log.error("Failed to start broker: {}", e);
            throw new ServletException("Error starting AMQ Broker: ", e);
        }

        // add jars for jython to scan for packages
        String realPath = getServletContext().getRealPath("/");
        if (!realPath.endsWith("/")) {
            realPath += "/";
        }

        String pythonHome = realPath + "WEB-INF/lib";
        Properties props = new Properties();
        props.setProperty("python.home", pythonHome);

        PySystemState.initialize(PySystemState.getBaseProperties(), props,
                new String[] { "" });
        PySystemState.add_classdir(realPath + "WEB-INF/classes");
        PySystemState.add_classdir(realPath + "../../../target/classes");
        PySystemState.add_extdir(pythonHome, true);

        // use a timer to start the indexer because tomcat's deployment order
        // is not reliable, fedora may not have started yet thus the indexer
        // is not able to subscribe to fedora messages. the timer will retry
        // every 15 secs if it was unable to start initially

        timer = new Timer("StartIndexer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                startIndexer();
            }
        }, 0, 15000);
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
        List<BrokerPlugin> lPlugins = new ArrayList();
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
    private void startIndexer() {
        log.info("Starting The Fascinator indexer...");
        if (messageQueues == null) {
            messageQueues = new ArrayList();
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
                        log.error("Failed to find Listener: '{}'", classId);
                        throw new Exception();
                    }
                } else {
                    log.error("No message classId provided: '{}'",
                            thread.toString());
                    throw new Exception();
                }
            }
            log.info("The Fascinator indexer was started successfully");
            // All is good, stop our callback
            timer.cancel();
            timer = null;

        } catch (Exception e) {
            log.warn("Message queues startup failed. Shutting them down...");
            for (GenericListener queue : messageQueues) {
                try {
                    queue.stop();
                } catch (Exception ex) {
                    log.error("Failed to stop listener '{}': {}",
                            queue.getId(), ex.getMessage());
                }
            }
            messageQueues = null;
            log.warn("Will retry in 15 seconds.", e);
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
     * Empty method. Process an incoming GET request.
     *
     * We don't need to do anything as Tapestry will handle this.
     *
     * @param request The incoming request
     * @param response The response object
     * @throws ServletException If errors found
     * @throws IOException If errors found
     */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        // do nothing
    }

    /**
     * Empty method. Process an incoming POST request.
     *
     * We don't need to do anything as Tapestry will handle this.
     *
     * @param request The incoming request
     * @param response The response object
     * @throws ServletException If errors found
     * @throws IOException If errors found
     */
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        // do nothing
    }

    /**
     * Shuts down any objects requiring such.
     *
     */
    @Override
    public void destroy() {
        try {
            broker.stop();
        } catch (Exception e) {
            log.error("Failed to stop message broker: {}", e.getMessage());
        }
        if (monitor != null) {
            monitor.stop();
        }
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
        if (timer != null) {
            timer.purge();
            timer = null;
        }
        super.destroy();
    }
}
