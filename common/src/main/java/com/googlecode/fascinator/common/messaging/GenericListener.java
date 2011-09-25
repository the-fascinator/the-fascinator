/*
 * The Fascinator - Common - GenericListener
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
package com.googlecode.fascinator.common.messaging;

import com.googlecode.fascinator.common.JsonSimpleConfig;
import java.io.IOException;

import javax.jms.JMSException;
import javax.jms.MessageListener;

/**
 * A Basic message listener for use in IndexerServlet.
 * Real message listeners are expected to extend this class.
 *
 * @author Greg Pendlebury
 */
public interface GenericListener extends MessageListener, Runnable {

    /**
     * Initialization method
     *
     * @param config Configuration to use
     * @throws IOException if the configuration file not found
     */
    public void init(JsonSimpleConfig config) throws Exception;

    /**
     * Return the ID string for this listener
     *
     */
    public String getId();

    /**
     * Start the queue
     *
     * @throws JMSException if an error occurred starting the JMS connections
     */
    public void start() throws Exception;

    /**
     * Stop the Queue Consumer
     *
     * @throws Exception if an error occurred stopping
     */
    public void stop() throws Exception;

    /**
     * Sets the priority level for the thread. Used by the OS.
     *
     * @param newPriority The priority level to set the thread at
     */
    public void setPriority(int newPriority);
}
