package com.googlecode.fascinator.portal.services.impl;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.messaging.MessagingServices;
import com.googlecode.fascinator.portal.JsonSessionState;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageWrapperImpl implements Storage {

    private Storage storage;

    private String username;

    @Inject
    private JsonSessionState sessionState;

    /** Messaging services */
    private MessagingServices messaging;

    /** Logging */
    private Logger log = LoggerFactory.getLogger(StorageWrapperImpl.class);

    public StorageWrapperImpl(Storage storage, ApplicationStateManager asm) {
        this.storage = storage;

        if (sessionState == null) {
            sessionState = asm.get(JsonSessionState.class);
        }
        username = (String) sessionState.get("username");

        try {
            messaging = MessagingServices.getInstance();
        } catch (MessagingException ex) {
            log.error("Failed to start connection: {}", ex.getMessage());
        }
    }

    @Override
    public DigitalObject createObject(String oid) throws StorageException {
        DigitalObject object = storage.createObject(oid);

        // Send the message to subscriber queue if object is created
        sentMessage(object.getId(), "create");
        return object;
    }

    @Override
    public DigitalObject getObject(String oid) throws StorageException {
        DigitalObject object = storage.getObject(oid);

        // Send the message to subscriber queue if object is accessed
        sentMessage(object.getId(), "access");
        return object;
    }

    @Override
    public Set<String> getObjectIdList() {
        return storage.getObjectIdList();
    }

    @Override
    public void removeObject(String oid) throws StorageException {

        // Send the message to subscriber queue if object is deleted
        sentMessage(oid, "delete");
        storage.removeObject(oid);
    }

    @Override
    public String getId() {
        return storage.getId();
    }

    @Override
    public String getName() {
        return storage.getName();
    }

    @Override
    public PluginDescription getPluginDetails() {
        return storage.getPluginDetails();
    }

    @Override
    public void init(File jsonFile) throws PluginException {
        storage.init(jsonFile);
    }

    @Override
    public void init(String jsonString) throws PluginException {
        storage.init(jsonString);
    }

    @Override
    public void shutdown() throws PluginException {
        if (messaging != null) {
            messaging.release();
        }
    }

    private void sentMessage(String oid, String eventType) {
        log.info(" * Sending message: {} with event {}", oid, eventType);
        Map<String, String> param = new LinkedHashMap<String, String>();
        param.put("oid", oid);
        param.put("eventType", eventType);
        param.put("username", username);
        param.put("context", storage.getName());
        try {
            messaging.onEvent(param);
        } catch (MessagingException ex) {
            log.error("Unable to send message: ", ex);
        }
    }
}