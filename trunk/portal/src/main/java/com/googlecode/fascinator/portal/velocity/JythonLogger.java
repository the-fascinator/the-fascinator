/* 
 * The Fascinator - Portal
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
package com.googlecode.fascinator.portal.velocity;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to redirect standard out/err for PythonInterpreter.
 * 
 * @author Oliver Lucido
 */
public class JythonLogger extends OutputStream {

    private Logger log = LoggerFactory.getLogger(JythonLogger.class);

    private Logger jythonLogger;

    private boolean dirty;

    private StringBuilder buffer;

    public JythonLogger(String name) {
        String packageName = getClass().getPackage().getName();
        String scriptName = StringUtils.substringBetween(name, "scripts/", ".py");
        scriptName = scriptName.replace("/", "$") + "$py";
        //log.debug("scriptName:{}", scriptName);
        jythonLogger = LoggerFactory.getLogger(packageName + "." + scriptName);
        dirty = false;
        buffer = new StringBuilder();
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    @Override
    public void flush() throws IOException {
        if (dirty) {
            jythonLogger.debug("{}", buffer.toString());
            dirty = false;
            buffer = new StringBuilder();
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (b == '\r' || b == '\n') {
            dirty = true;
            flush();
        } else {
            buffer.append((char) b);
        }
    }
}
