/* 
 * The Fascinator - RAM storage plugin
 * Copyright (C) 2009 University of Southern Queensland
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
package com.googlecode.fascinator.storage.ram;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.storage.impl.GenericDigitalObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RAM based storage. This is not a persistent store, it's primary use is for
 * testing purposes.
 * 
 * @author Oliver Lucido
 */
public class RamStorage implements Storage {

    private final Logger log = LoggerFactory.getLogger(RamStorage.class);

    private Map<String, DigitalObject> manifest;

    @Override
    public String getId() {
        return "ram";
    }

    @Override
    public String getName() {
        return "RAM Storage";
    }

    /**
     * Gets a PluginDescription object relating to this plugin.
     *
     * @return a PluginDescription
     */
    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    @Override
    public void init(File jsonFile) throws StorageException {
        // no configuration needed
    }

    @Override
    public void init(String jsonString) throws StorageException {
        // no configuration needed
    }

    @Override
    public void shutdown() throws StorageException {
        // don't need to do anything
    }

    @Override
    public DigitalObject createObject(String oid) throws StorageException {
        DigitalObject newObject = new GenericDigitalObject(oid);
        getManifest().put(oid, newObject);
        return newObject;
    }

    @Override
    public DigitalObject getObject(String oid) throws StorageException {
        if (getManifest().containsKey(oid)) {
            return getManifest().get(oid);
        }
        throw new StorageException("ID '" + oid + "' does no exist.");
    }

    @Override
    public Set<String> getObjectIdList() {
        return getManifest().keySet();
    }

    @Override
    public void removeObject(String oid) throws StorageException {
        getManifest().remove(oid);
    }

    public Map<String, DigitalObject> getManifest() {
        if (manifest == null) {
            manifest = new HashMap<String, DigitalObject>();
        }
        return manifest;
    }
}
