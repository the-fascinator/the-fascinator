/* 
 * The Fascinator - Plugin API
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
package com.googlecode.fascinator.api.transaction;

import com.googlecode.fascinator.api.Plugin;

/**
 * <p>A transaction manager will parse incoming JSON messages and inform the
 * TransactionManagerQueueConsumer which Transformers, Subscribers, Indexing
 * and Messaging sould occur in response.</p>
 * 
 * <p>Implementations of this Plugin are expected to execute inside the
 * TransactionManagerQueueConsumer and should confirm to the expected message
 * formats, as per documentation.</p>
 * 
 * @author Greg Pendlebury
 */
public interface TransactionManager extends Plugin {

    /**
     * <p>Parse an incoming JSON message, process the contents and return a
     * schedule of activities for the TransactionManagerQueueConsumer to
     * perform.</p>
     * 
     * <p>Please note. The API cannot defined the I/O Objects as JsonSimple,
     * because of this classes' location in the Maven dependency tree. The
     * GenericTransactionManager from the Common Library will enforce this
     * however, and implementations would would be advised to extend that class,
     * rather then implement directly from the API.</p>
     * 
     * @param message The message to parse, in JSON
     * @return Object The actions to take in response, in JSON
     * @throws TransactionException If an error occurred or the the message is
     * in a bad format or otherwise unsuitable.
     */
    public Object parseMessage(Object message) throws TransactionException;
}
