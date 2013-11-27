/* 
 * The Fascinator - Portal - Security
 * Copyright (C) 2013 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
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
package com.googlecode.fascinator.common.storage.impl;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonSimpleConfig;

/**
 * Storage wrapper used in Spring.
 * 
 * @author Andrew Brazzatti
 * @author Jianfeng Li
 * 
 */
@Component(value = "fascinatorStorage")
public class SpringStorageWrapper implements Storage {
    private Storage storagePlugin;
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
        storagePlugin = PluginManager.getStorage(storageType);
        if (storagePlugin == null) {
            throw new RuntimeException("Storage plugin '" + storageType
                    + "'. Ensure it is in the classpath.");
        }
        try {
            storagePlugin.init(systemConfiguration.toString());
            log.debug("Storage service has been initialiased: {}",
                    storagePlugin.getName());
        } catch (PluginException pe) {
            throw new RuntimeException("Failed to initialise storage", pe);
        }
    }

    @Override
    public String getId() {
        return storagePlugin.getId();
    }

    @Override
    public String getName() {
        return storagePlugin.getName();
    }

    @Override
    public PluginDescription getPluginDetails() {
        return storagePlugin.getPluginDetails();
    }

    @Override
    public void init(File jsonFile) throws PluginException {
        storagePlugin.init(jsonFile);
    }

    @Override
    public void init(String jsonString) throws PluginException {
        storagePlugin.init(jsonString);

    }

    @Override
    public void shutdown() throws PluginException {
        storagePlugin.shutdown();
    }

    @Override
    public DigitalObject createObject(String oid) throws StorageException {
        return storagePlugin.createObject(oid);
    }

    @Override
    public DigitalObject getObject(String oid) throws StorageException {
        return storagePlugin.getObject(oid);
    }

    @Override
    public void removeObject(String oid) throws StorageException {
        storagePlugin.removeObject(oid);
    }

    @Override
    public Set<String> getObjectIdList() {
        return storagePlugin.getObjectIdList();
    }

}
