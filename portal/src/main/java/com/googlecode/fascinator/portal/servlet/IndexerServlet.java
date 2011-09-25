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

import com.googlecode.fascinator.common.messaging.GenericListener;
import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.messaging.MessageBroker;
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
    /** Logger */
    private Logger log = LoggerFactory.getLogger(IndexerServlet.class);

    /** Message broker */
    private MessageBroker broker;

    /**
     * Initialise the Servlet, called at Server startup
     *
     * @throws ServletException If it found errors during startup
     */
    @Override
    public void init() throws ServletException {
        // Start the Message Broker
        try {
            broker = MessageBroker.getInstance();
        } catch (MessagingException ex) {
            log.error("Failed to start broker: {}", ex);
            throw new ServletException("Error starting AMQ Broker: ", ex);
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
        // Shutting down the message broker
        if (broker != null) {
            boolean result = broker.shutdown();
            if (result != true) {
                log.error("Message broker did not shut down correctly!");
            }
        }
        super.destroy();
    }
}
