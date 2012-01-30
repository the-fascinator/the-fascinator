/*
 * The Fascinator - Portal
 * Copyright (C) 2010-2011 University of Southern Queensland
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
package com.googlecode.fascinator.portal.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.messaging.MessagingServices;
import com.googlecode.fascinator.portal.HouseKeeper;
import com.googlecode.fascinator.portal.UserAction;
import com.googlecode.fascinator.portal.services.HouseKeepingManager;

/**
 * Implements the House Keeping Manager interface, providing access to the
 * housekeeping message queue and its outputs.
 * 
 * @author Greg Pendlebury
 */
public class HouseKeepingManagerImpl implements HouseKeepingManager {

    /** Logging */
    private Logger log = LoggerFactory.getLogger(HouseKeepingManagerImpl.class);

    /** System Configuration */
    private JsonSimpleConfig sysConfig;

    /** House Keeper object */
    private HouseKeeper houseKeeper;

    /** Messaging service instance */
    private MessagingServices services;

    /**
     * Basic constructor, run by Tapestry through injection.
     * 
     */
    public HouseKeepingManagerImpl() {
        try {
            services = MessagingServices.getInstance();
            sysConfig = new JsonSimpleConfig();

            JsonObject object = sysConfig.getObject("portal", "houseKeeping");
            JsonSimpleConfig config = new JsonSimpleConfig();
            if (object != null) {
                // We need a JsonSimpleConfig Object to instantiate
                // HouseKeeping and it lacks a JsonObject constructor
                JsonSimple json = new JsonSimple(object);
                config = new JsonSimpleConfig(json.toString());
            } else {
                log.warn("Invalid config for house keeping!");
                // We really need housekeeper to start, so
                // some fake, empty config is fine
            }

            // Create
            houseKeeper = new HouseKeeper();
            houseKeeper.setPriority(Thread.MAX_PRIORITY);
            // Initialise
            houseKeeper.init(config);
            houseKeeper.start();

        } catch (IOException ex) {
            log.error("Failed to access system config", ex);
        } catch (Exception ex) {
            log.error("Failed to start House Keeping", ex);
            houseKeeper = null;
        }
    }

    /**
     * Tapestry notification that server is shutting down
     * 
     */
    @Override
    public void registryDidShutdown() {
        try {
            if (houseKeeper != null) {
                houseKeeper.stop();
            }
            services.release();
        } catch (Exception ex) {
            log.error("Error shutting down!", ex);
        }
    }

    /**
     * Get the messages to display for the user
     * 
     * @returns List<UserAction> The current list of message
     */
    @Override
    public List<UserAction> getUserMessages() {
        if (houseKeeper == null) {
            return new ArrayList<UserAction>();
        } else {
            return houseKeeper.getUserMessages();
        }
    }

    /**
     * Confirm and remove a message/action
     * 
     * @param actionId The ID of the action to remove
     */
    @Override
    public void confirmMessage(String actionId) throws Exception {
        if (houseKeeper != null) {
            houseKeeper.confirmMessage(actionId);
        }
    }

    /**
     * Send a message to HouseKeeping.
     * 
     */
    @Override
    @SuppressWarnings("static-access")
    public void sendMessage(String message) throws MessagingException {
        services.queueMessage(HouseKeeper.QUEUE_ID, message);
    }

    /**
     * Request a low priority restart from HouseKeeping.
     * 
     */
    @Override
    public void requestRestart() throws MessagingException {
        log.info("System restart has been requested");
        JsonObject msg = new JsonObject();
        msg.put("type", "basic-restart");
        sendMessage(msg.toString());
    }

    /**
     * Request a high priority restart from HouseKeeping. High priority will
     * stop all user actions until the restart occurs.
     * 
     */
    @Override
    public void requestUrgentRestart() throws MessagingException {
        log.info("Urgent system restart has been requested");
        JsonObject msg = new JsonObject();
        msg.put("type", "blocking-restart");
        sendMessage(msg.toString());
    }

    /**
     * Get the latest statistics on message queues.
     * 
     * @return Map<String, Map<String, String>> of all queues and their
     *         statistics
     */
    @Override
    public Map<String, Map<String, String>> getQueueStats() {
        if (houseKeeper != null) {
            return houseKeeper.getQueueStats();
        } else {
            return new LinkedHashMap<String, Map<String, String>>();
        }
    }
}
