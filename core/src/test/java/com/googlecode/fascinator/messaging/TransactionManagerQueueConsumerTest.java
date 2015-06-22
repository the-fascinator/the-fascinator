/*
 * The Fascinator - Transaction Management Unit Tests
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

import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.PayloadType;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.common.messaging.MessagingServices;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for Transaction Management. This is quite a complicated setup
 * given that it depends on AMQ and occurs across multiple threads.
 * 
 * It is worth noting that the RAM storage is unsuitable for this unit test as
 * it cannot persist across threads with Fascinator's current architecture.
 *
 * @author Greg Pendlebury
 */
public class TransactionManagerQueueConsumerTest {
//    /** Message broker */
//    private static MessageBroker broker;
//
//    /** Messaging services */
//    private static MessagingServices messaging;
//
//    /** System Configuration */
//    private static File sysConfig;
//
//    /** Storage plugin */
//    private Storage storage;
//
//    public static void startTestLog(String message) {
//        log(null, "\n            ===== "+message+"() =====");
//    }
//    public static void log(String message) {
//        log(null, message);
//    }
//    public static void log(String title, String message) {
//        String titleLog = "";
//        if (title != null) {
//            titleLog += "===\n"+title+"\n===\n";
//        }
//        System.err.println(titleLog + message);
//    }
//
//    private static void wait(int timer) {
//        try {
//            Thread.sleep(timer);
//        } catch (InterruptedException ex) {
//            // Do nothing
//        }
//    }
//
//    // Recursive delete of folder/file
//    public static void delete(File file) {
//        // Directories
//        if (file.isDirectory()) {
//            for (File child : file.listFiles()) {
//                delete(child);
//            }
//        }
//        // Files / top-level directories (after children)
//        file.delete();
//    }
//
//    @BeforeClass
//    public static void startup() {
//        // Find our resources directory
//        URL url = TransactionManagerQueueConsumerTest.class.getResource(
//                "/transactionTest/system-config.json");
//        try {
//            sysConfig = new File(url.toURI());
//        } catch (URISyntaxException ex) {
//            log("TEST ERROR", "Failed to access resource directory!");
//        }
//
//        // Set fascinator home to our test resources
//        String home = sysConfig.getParent();
//        System.setProperty("fascinator.home", home);
//        log("FASCINATOR HOME: "+home, "");
//
//        // Destroy any pre-existing storage
//        File oldHome = new File(home, "storage");
//        if (oldHome.exists() && oldHome.isDirectory()) {
//            log("DELETING OLD STORAGE:", oldHome.getPath());
//            delete(oldHome);
//            if (oldHome.exists()) {
//                log("FAILED TO DELETE OLD STORAGE:", oldHome.getPath());
//                Assert.fail();
//            }
//        }
//
//        // Startup the message broker. It will start messaging queues itself
//        try {
//            broker = MessageBroker.getInstance();
//        } catch (MessagingException ex) {
//            log("STARTUP ERROR", ex.getMessage());
//            Assert.fail();
//        }
//
//        // The various multi-threaded message queues need
//        //    to be given time to start.
//        wait(1000);
//
//        // And finally, for unit testing we want our own
//        //    method of sending outbound messages
//        try {
//            messaging = MessagingServices.getInstance();
//        } catch (MessagingException ex) {
//            log("Failed to access messaging services instance: "
//                    + ex.getMessage());
//            Assert.fail();
//        }
//
//        wait(500);
//        log(" AMQ STARTUP COMPLETE", "");
//    }
//
//    @AfterClass
//    public static void shutdown() {
//        wait(500);
//        log(" AMQ SHUTDOWN BROKER", "");
//
//        // The various multi-threaded message queues need
//        //    to be given time to shutdown.
//        wait(1000);
//        if (messaging != null) {
//            messaging.release();
//        }
//        if (broker != null) {
//            boolean result = broker.shutdown();
//            if (result != true) {
//                log("SHUTDOWN ERROR", "Message broker did not shut down correctly!");
//            }
//        }
//    }
//
//    @Before
//    public void setup() throws Exception {
//        storage = PluginManager.getStorage("ram");
//        storage.init(sysConfig);
//    }
//    
//    @After
//    public void finish() throws Exception {
//        storage.shutdown();
//    }
//
//    /**
//     * Send an object through a single transformer
//     *
//     * @throws Exception if any error occurs
//     */
//    @Test
//    public void testOneTransform() throws Exception {
//        startTestLog("testOneTransform");
//        // Harvest a test object
//        String oid = "testOneTransform";
//        InputStream in = getClass().getResourceAsStream(
//                "/transactionTest/testObject.empty");
//        DigitalObject object = storage.createObject(oid);
//        Payload payload = object.createStoredPayload("SOURCE", in);
//        payload.setType(PayloadType.Source);
//        object.close();
//
//        // Send a message through the system and wait for processing
//        sendMessage("{\"oid\":\""+oid+"\"}");
//        wait(1000);
//
//        // Validate the expected outcome
//        object = storage.getObject(oid);
//        Properties metadata = object.getMetadata();
//        String order1 = metadata.getProperty("ORDER1");
//        String order2 = metadata.getProperty("ORDER2");
//        String order3 = metadata.getProperty("ORDER3");
//        Assert.assertEquals(order1, "ORDER1");
//        Assert.assertNull(order2);
//        Assert.assertNull(order3);
//    }
//
//
//    /**
//     * Send an object through a multiple transformers
//     *
//     * @throws Exception if any error occurs
//     */
//    @Test
//    public void testMultipleTransforms() throws Exception {
//        startTestLog("testMultipleTransforms");
//        // Harvest a test object
//        String oid = "testMultipleTransforms";
//        InputStream in = getClass().getResourceAsStream(
//                "/transactionTest/testObject.empty");
//        DigitalObject object = storage.createObject(oid);
//        Payload payload = object.createStoredPayload("SOURCE", in);
//        payload.setType(PayloadType.Source);
//        object.close();
//
//        // Send a message through the system and wait for processing
//        sendMessage("{\"oid\":\""+oid+"\"}");
//        wait(1000);
//
//        // Validate the expected outcome
//        object = storage.getObject(oid);
//        Properties metadata = object.getMetadata();
//        String order1 = metadata.getProperty("ORDER1");
//        String order2 = metadata.getProperty("ORDER2");
//        String order3 = metadata.getProperty("ORDER3");
//        Assert.assertEquals(order1, "ORDER1");
//        Assert.assertNull(order2);
//        Assert.assertEquals(order3, "ORDER3");
//    }
//
//    /**
//     * Send an object through a single transformer and then send a message
//     * back into the system to bring a second object through a different
//     * transformer.
//     *
//     * @throws Exception if any error occurs
//     */
//    @Test
//    public void testMessaging() throws Exception {
//        startTestLog("testMessaging");
//        String oid1 = "testMessaging";
//        String oid2 = "testMessagingSecondObject";
//
//        // Harvest a test object
//        InputStream in = getClass().getResourceAsStream(
//                "/transactionTest/testObject.empty");
//        DigitalObject object = storage.createObject(oid1);
//        Payload payload = object.createStoredPayload("SOURCE", in);
//        payload.setType(PayloadType.Source);
//        object.close();
//
//        // Harvest a second object
//        in = getClass().getResourceAsStream(
//                "/transactionTest/testObject.empty");
//        object = storage.createObject(oid2);
//        payload = object.createStoredPayload("SOURCE", in);
//        payload.setType(PayloadType.Source);
//        object.close();
//
//        // Send a message through the system and wait for processing,
//        //      note this is the first object ONLY
//        sendMessage("{\"oid\":\""+oid1+"\"}");
//        wait(1000);
//
//        // Validate the expected outcome
//        object = storage.getObject(oid1);
//        Properties metadata = object.getMetadata();
//        String order1 = metadata.getProperty("ORDER1");
//        String order2 = metadata.getProperty("ORDER2");
//        String order3 = metadata.getProperty("ORDER3");
//        Assert.assertNull(order1);
//        Assert.assertEquals(order2, "ORDER2");
//        Assert.assertNull(order3);
//
//        object = storage.getObject(oid2);
//        metadata = object.getMetadata();
//        order1 = metadata.getProperty("ORDER1");
//        order2 = metadata.getProperty("ORDER2");
//        order3 = metadata.getProperty("ORDER3");
//        Assert.assertEquals(order1, "ORDER1");
//        Assert.assertNull(order2);
//        Assert.assertNull(order3);
//    }
//
//    /**
//     * Send a message to two brokers, really just the same broker
//     * by two different addresses:
//     *  - localhost
//     *  - 127.0.0.1
//     * 
//     * But it should be enough to fool the cache.
//     *
//     * @throws Exception if any error occurs
//     */
//    @Test
//    public void testMultiBroker() throws Exception {
//        startTestLog("testMultiBroker");
//        String oid1 = "testMultiBroker";
//        String oid2 = "testMultiBrokerSecondObject";
//
//        // Harvest a test object
//        InputStream in = getClass().getResourceAsStream(
//                "/transactionTest/testObject.empty");
//        DigitalObject object = storage.createObject(oid1);
//        Payload payload = object.createStoredPayload("SOURCE", in);
//        payload.setType(PayloadType.Source);
//        object.close();
//
//        // Harvest a second object
//        in = getClass().getResourceAsStream(
//                "/transactionTest/testObject.empty");
//        object = storage.createObject(oid2);
//        payload = object.createStoredPayload("SOURCE", in);
//        payload.setType(PayloadType.Source);
//        object.close();
//
//        // Send a message through the system and wait for processing,
//        //      note this is the first object ONLY
//        sendMessage("{\"oid\":\""+oid1+"\"}");
//        wait(1000);
//
//        // Validate the expected outcome
//        object = storage.getObject(oid1);
//        Properties metadata = object.getMetadata();
//        String order1 = metadata.getProperty("ORDER1");
//        String order2 = metadata.getProperty("ORDER2");
//        String order3 = metadata.getProperty("ORDER3");
//        Assert.assertNull(order1);
//        Assert.assertEquals(order2, "ORDER2");
//        Assert.assertNull(order3);
//
//        object = storage.getObject(oid2);
//        metadata = object.getMetadata();
//        order1 = metadata.getProperty("ORDER1");
//        order2 = metadata.getProperty("ORDER2");
//        order3 = metadata.getProperty("ORDER3");
//        Assert.assertEquals(order1, "ORDER1");
//        Assert.assertNull(order2);
//        Assert.assertNull(order3);
//    }
//
//    /**
//     * Unlike the tests above, this test isn't so useful as a unit test. It
//     * would catch crash type errors (but those are unlikely) in the Subscriber
//     * and Indexer plugins. There is very little that can be tested on those
//     * plugins however, short of providing complete implementations.
//     * 
//     * This test is more useful during active development when the Core Library
//     * can be compiled separately and the dev can pay close attention to STDOUT.
//     *
//     * @throws Exception if any error occurs
//     */
//    @Test
//    public void testBroadPlugins() throws Exception {
//        startTestLog("testBroadPlugins");
//        // Harvest a test object
//        String oid = "testBroadPlugins";
//        InputStream in = getClass().getResourceAsStream(
//                "/transactionTest/testObject.empty");
//        DigitalObject object = storage.createObject(oid);
//        Payload payload = object.createStoredPayload("SOURCE", in);
//        payload.setType(PayloadType.Source);
//        object.close();
//
//        // Send a message through the system and wait for processing
//        sendMessage("{\"oid\":\""+oid+"\"}");
//        wait(1000);
//
//        // Validate the expected outcome
//        object = storage.getObject(oid);
//        Properties metadata = object.getMetadata();
//        String order1 = metadata.getProperty("ORDER1");
//        String order2 = metadata.getProperty("ORDER2");
//        String order3 = metadata.getProperty("ORDER5");
//        Assert.assertEquals(order1, "ORDER1");
//        Assert.assertEquals(order2, "ORDER2");
//        Assert.assertEquals(order3, "ORDER5");
//    }
//
//    private void sendMessage(String message) throws Exception {
//        messaging.queueMessage(
//                TransactionManagerQueueConsumer.LISTENER_ID,
//                message);
//    }
}
