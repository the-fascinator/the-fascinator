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
import com.googlecode.fascinator.api.subscriber.Subscriber;
import com.googlecode.fascinator.api.subscriber.SubscriberException;

import java.io.File;
import java.util.Map;


/**
 * A fake subscriber plugin for unit testing against. It just fakes the
 * interface and prints messages to STDOUT. It's usage outside of unit tests
 * is pointless.
 * 
 * @author Greg Pendlebury
 */

public class FakeSubscriber implements Subscriber {
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
        return "Fake Subscriber";
    }

    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    @Override
    public void init(String jsonString) throws SubscriberException {
        log(" * SUBSCRIBER: init(String)");
    }

    @Override
    public void init(File jsonFile) throws SubscriberException {
        log(" * SUBSCRIBER: init(File)");
    }

    @Override
    public void shutdown() throws SubscriberException {
        log(" * SUBSCRIBER: shutdown()");
    }

    @Override
    public void onEvent(Map<String, String> param) throws SubscriberException {
        log(" * SUBSCRIBER: onEvent() => " + param.toString());
    }

}
