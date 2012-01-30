/* 
 * The Fascinator - Test Transaction Manager
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

import com.googlecode.fascinator.api.harvester.HarvesterException;
import com.googlecode.fascinator.api.transaction.TransactionException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.transaction.GenericTransactionManager;

/**
 * Provides a sample transaction management implementation for unit tests.
 * 
 * @author Greg Pendlebury
 */
public class FakeTransactionManager extends GenericTransactionManager {
    public void log(String message) {
        log(null, message);
    }

    public void log(String title, String message) {
        String titleLog = "";
        if (title != null) {
            titleLog += "===\n" + title + "\n===\n";
        }
        System.err.println(titleLog + message);
    }

    /**
     * Base constructor
     * 
     */
    public FakeTransactionManager() {
        super("test", "Test Transaction Manager");
    }

    /**
     * Initialise method
     * 
     * @throws HarvesterException if there was an error during initialisation
     */
    @Override
    public void init() throws TransactionException {
        log(" * TRANSACTION: init()");
    }

    /**
     * Processing method
     * 
     * @param message The JsonSimple message to process
     * @return JsonSimple The actions to take in response
     * @throws TransactionException If an error occurred
     */
    @Override
    public JsonSimple parseMessage(JsonSimple message)
            throws TransactionException {
        String oid = message.getString(null, "oid");
        JsonSimple response = new JsonSimple();

        // Unit test ONE
        if (oid.equals("testOneTransform")) {
            JsonObject order1 = newTransform(response, "fake", oid);
            JsonObject config1 = (JsonObject) order1.get("config");
            config1.put("flag", "ORDER1");
            return response;
        }

        // Unit test TWO
        if (oid.equals("testMultipleTransforms")) {
            JsonObject order1 = newTransform(response, "fake", oid);
            JsonObject config1 = (JsonObject) order1.get("config");
            config1.put("flag", "ORDER1");

            JsonObject order2 = newTransform(response, "fake", oid);
            JsonObject config2 = (JsonObject) order2.get("config");
            config2.put("flag", "ORDER3");
            return response;
        }

        // Unit test THREE
        if (oid.equals("testMessaging")) {
            // We are going to trigger a second object to pass through here
            JsonObject order = newMessage(response,
                    TransactionManagerQueueConsumer.LISTENER_ID);
            JsonObject messageObject = (JsonObject) order.get("message");
            messageObject.put("oid", "testMessagingSecondObject");
            log("{Sending message 'testMessaging' => 'testMessagingSecondObject'}");

            // Then have this object transformed
            JsonObject order2 = newTransform(response, "fake", oid);
            JsonObject config2 = (JsonObject) order2.get("config");
            config2.put("flag", "ORDER2");

            return response;
        }
        if (oid.equals("testMessagingSecondObject")) {
            JsonObject order1 = newTransform(response, "fake", oid);
            JsonObject config1 = (JsonObject) order1.get("config");
            config1.put("flag", "ORDER1");
            return response;
        }

        // Unit test FOUR
        if (oid.equals("testMultiBroker")) {
            // We are going to trigger a second object to pass through here
            JsonObject order = newMessage(response,
                    TransactionManagerQueueConsumer.LISTENER_ID);

            String broker = getJsonConfig().getString(null, "messaging",
                    "testingUrl");
            order.put("broker", broker);
            log("{Sending message to another broker: '" + broker + "'}");

            JsonObject messageObject = (JsonObject) order.get("message");
            messageObject.put("oid", "testMultiBrokerSecondObject");
            log("{Sending message 'testMultiBroker' => 'testMultiBrokerSecondObject'}");

            // Then have this object transformed
            JsonObject order2 = newTransform(response, "fake", oid);
            JsonObject config2 = (JsonObject) order2.get("config");
            config2.put("flag", "ORDER2");

            return response;
        }
        if (oid.equals("testMultiBrokerSecondObject")) {
            JsonObject order1 = newTransform(response, "fake", oid);
            JsonObject config1 = (JsonObject) order1.get("config");
            config1.put("flag", "ORDER1");
            return response;
        }

        // Unit test FIVE
        if (oid.equals("testBroadPlugins")) {
            JsonObject order1 = newTransform(response, "fake", oid);
            JsonObject config1 = (JsonObject) order1.get("config");
            config1.put("flag", "ORDER1");

            JsonObject order2 = newTransform(response, "fake", oid);
            JsonObject config2 = (JsonObject) order2.get("config");
            config2.put("flag", "ORDER2");

            @SuppressWarnings("unused")
            JsonObject order3 = newIndex(response, oid);
            @SuppressWarnings("unused")
            JsonObject order4 = newSubscription(response, oid);

            JsonObject order5 = newTransform(response, "fake", oid);
            JsonObject config5 = (JsonObject) order5.get("config");
            config5.put("flag", "ORDER5");

            boolean loopStop = message.getBoolean(false, "loopStop");
            if (!loopStop) {
                JsonObject order6 = newMessage(response,
                        TransactionManagerQueueConsumer.LISTENER_ID);
                JsonObject messageObject = (JsonObject) order6.get("message");
                messageObject.put("oid", oid);
                messageObject.put("loopStop", true);
            }

            return response;
        }

        return response;
    }

    /**
     * Creation of new Orders with appropriate default nodes
     * 
     */
    private JsonObject newIndex(JsonSimple response, String oid) {
        JsonObject order = createNewOrder(response,
                TransactionManagerQueueConsumer.OrderType.INDEXER.toString());
        order.put("oid", oid);
        return order;
    }

    private JsonObject newMessage(JsonSimple response, String target) {
        JsonObject order = createNewOrder(response,
                TransactionManagerQueueConsumer.OrderType.MESSAGE.toString());
        order.put("target", target);
        order.put("message", new JsonObject());
        return order;
    }

    private JsonObject newSubscription(JsonSimple response, String oid) {
        JsonObject order = createNewOrder(response,
                TransactionManagerQueueConsumer.OrderType.SUBSCRIBER.toString());
        order.put("oid", oid);
        JsonObject message = new JsonObject();
        message.put("oid", oid);
        message.put("context", "Unit Test");
        message.put("eventType", "Sending test message");
        message.put("user", "System");
        order.put("message", message);
        return order;
    }

    private JsonObject newTransform(JsonSimple response, String target,
            String oid) {
        JsonObject order = createNewOrder(response,
                TransactionManagerQueueConsumer.OrderType.TRANSFORMER
                        .toString());
        order.put("target", target);
        order.put("oid", oid);
        order.put("config", new JsonObject());
        return order;
    }

    private JsonObject createNewOrder(JsonSimple response, String type) {
        JsonObject order = response.writeObject("orders", -1);
        order.put("type", type);
        return order;
    }
}
