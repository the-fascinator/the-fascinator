package com.googlecode.fascinator.common.storage.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.common.JsonSimpleConfig;

public class SpringStorageWrapper {
    private Storage storageService;
    private static final String DEFAULT_STORAGE_TYPE = "file-system";
    private Logger log = LoggerFactory.getLogger(SpringStorageWrapper.class);

    public SpringStorageWrapper() {
        // initialise storage system
        JsonSimpleConfig systemConfiguration;
        try {
            systemConfiguration = new JsonSimpleConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String storageType = systemConfiguration.getString(
                DEFAULT_STORAGE_TYPE, "storage", "type");
        storageService = PluginManager.getStorage(storageType);
        if (storageService == null) {
            throw new RuntimeException("Storage plugin '" + storageType
                    + "'. Ensure it is in the classpath.");
        }
        try {
            storageService.init(systemConfiguration.toString());
            log.debug("Storage service has been initialiased: {}",
                    storageService.getName());
        } catch (PluginException pe) {
            throw new RuntimeException("Failed to initialise storage", pe);
        }
    }

    public Storage getService() {
        return storageService;
    }
}
