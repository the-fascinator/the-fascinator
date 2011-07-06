/* 
 * The Fascinator - Plugin API
 * Copyright (C) 2008-2009 University of Southern Queensland
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
package com.googlecode.fascinator.api.indexer.rule;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An indexer rule generates a Solr document based on a specified input. The
 * input is generally XML, usually a Solr document, as a result of running the
 * rules in a sequential order.
 * 
 * @author Oliver Lucido
 */
public abstract class Rule {

    /** Logging */
    private Logger log;

    /** Logging */
    private OutputStream logOut;

    /** Logging */
    private PrintWriter logWriter;

    /** The name of this rule */
    private String name;

    /** Whether or not this rule is required for indexing to continue */
    private boolean required;

    /**
     * Creates an optional indexing rule with the specified name
     * 
     * @param name A name for this rule
     */
    public Rule(String name) {
        this(name, false);
    }

    /**
     * Creates an indexing rule with the specified name. If required is set to
     * <code>true</code>, the rule must be run successfully for indexing to
     * continue.
     * 
     * @param name A name for this rule
     * @param required <code>true</code> if this rule is required,
     *        <code>false</code> otherwise
     */
    public Rule(String name, boolean required) {
        this.name = name;
        this.required = required;
        logOut = new ByteArrayOutputStream();
        logWriter = new PrintWriter(logOut);
    }

    /**
     * Log a message
     * 
     * @param msg The message to log
     */
    public void log(String msg) {
        log(msg, null);
    }

    /**
     * Log a message with a stack trace
     * 
     * @param msg The message to log
     * @param t The exception to trace
     */
    public void log(String msg, Throwable t) {
        logWriter.println(msg);
        if (t != null) {
            t.printStackTrace(logWriter);
        }
        if (log == null) {
            log = LoggerFactory.getLogger(getClass());
        }
        log.debug(msg, t);
    }

    /**
     * Gets the log messages generated while processing
     * 
     * @return The log messages for this rule
     * @throws RuntimeException If the log could not be encoded properly
     */
    public String getLog() {
        try {
            return ((ByteArrayOutputStream) logOut).toString("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee.getMessage());
        }
    }

    /**
     * Gets the name of this rule
     * 
     * @return The name of this rule
     */
    public String getName() {
        return name;
    }

    /**
     * Tests if this rule is required for indexing to continue
     * 
     * @return <code>true</code> to stop indexing if this rule fails,
     *         <code>false</code> to ignore any failures and continue
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Runs the rule on the specified input and return the results. The result
     * should be a Solr document. The input is usually a Solr document or XML
     * but may also be <code>null</code>.
     * 
     * @param in The source input
     * @param out The result after processing
     * @throws RuleException If an error occurs
     */
    public abstract void run(Reader in, Writer out) throws RuleException;

    /**
     * Gets the name of this rule
     * 
     * @return The name of this rule
     */
    @Override
    public String toString() {
        return getName();
    }
}
