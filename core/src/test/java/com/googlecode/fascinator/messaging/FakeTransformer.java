/*
 * The Fascinator - Fake Subscriber
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

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.transformer.Transformer;
import com.googlecode.fascinator.api.transformer.TransformerException;
import com.googlecode.fascinator.common.JsonSimple;

import java.io.File;
import java.util.Properties;


/**
 * A fake Transformer plugin for unit testing against. This isn't even an
 * implementation in RAM, it just fakes the interface and reports activity to
 * STDOUT. It's usage outside of unit tests is pointless, and even in unit
 * tests you cannot trust what it is *doing*, just that it is being called.
 * 
 * @author Greg Pendlebury
 */

public class FakeTransformer implements Transformer {
    public void log(String message) {
        log(null, message);
    }
    public void log(String title, String message) {
        String titleLog = "";
        if (title != null) {
            titleLog += "===\n"+title+"\n===\n";
        }
        System.err.println(titleLog + message);
    }

    @Override
    public String getId() {
        return "fake";
    }

    @Override
    public String getName() {
        return "Fake Transformer";
    }

    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    @Override
    public void init(String jsonString) throws PluginException {
        log(" * TRANSFORMER: init(String)");
    }

    @Override
    public void init(File jsonFile) throws PluginException {
        log(" * TRANSFORMER: init(File)");
    }

    @Override
    public void shutdown() throws PluginException {
        log(" * TRANSFORMER: shutdown()");
    }

    @Override
    public DigitalObject transform(DigitalObject in, String jsonConfig)
            throws TransformerException {
        log(" * TRANSFORMER: transform(OID = '"+in.getId()+"')");

        try {
            JsonSimple config = new JsonSimple(jsonConfig);
            String flag = config.getString(null, "flag");
            if (flag == null) {
                return in;
            }

            Properties metadata = in.getMetadata();
            metadata.setProperty(flag, flag);
            in.close();

            log("   ===> Property set: " + flag);
        } catch (Exception ex) {
            throw new TransformerException(ex);
        }
        return in;
    }
}
