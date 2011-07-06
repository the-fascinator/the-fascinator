/* 
 * The Fascinator - Plugin API
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
package com.googlecode.fascinator.api;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import com.googlecode.fascinator.api.access.AccessControl;
import com.googlecode.fascinator.api.access.AccessControlManager;
import com.googlecode.fascinator.api.authentication.Authentication;
import com.googlecode.fascinator.api.harvester.Harvester;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.roles.Roles;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.subscriber.Subscriber;
import com.googlecode.fascinator.api.transformer.Transformer;
import com.googlecode.fascinator.api.transformer.TransformerException;

/**
 * Factory class to get plugin instances
 * 
 * @author Oliver Lucido
 */
public class PluginManager {

    /**
     * Gets an access control plugin
     * 
     * @param id plugin identifier
     * @return an access control plugin, or null if not found
     */
    public static AccessControl getAccessControl(String id) {
        ServiceLoader<AccessControl> plugins = ServiceLoader
                .load(AccessControl.class);
        for (AccessControl plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Get a list of access control plugins
     * 
     * @return map of access control plugin, or empty map if not found
     */
    public static Map<String, AccessControl> getAccessControlPlugins() {
        Map<String, AccessControl> access_plugins = new HashMap<String, AccessControl>();
        ServiceLoader<AccessControl> plugins = ServiceLoader
                .load(AccessControl.class);
        for (AccessControl plugin : plugins) {
            access_plugins.put(plugin.getId(), plugin);
        }
        return access_plugins;
    }

    /**
     * Get the access manager. Used in The indexer if the portal isn't running
     * 
     * @param id plugin identifier
     * @return an access manager plugin, or null if not found
     */
    public static AccessControlManager getAccessManager(String id) {
        ServiceLoader<AccessControlManager> plugins = ServiceLoader
                .load(AccessControlManager.class);
        for (AccessControlManager plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Gets an authentication plugin
     * 
     * @param id plugin identifier
     * @return an authentication plugin, or null if not found
     */
    public static Authentication getAuthentication(String id) {
        ServiceLoader<Authentication> plugins = ServiceLoader
                .load(Authentication.class);
        for (Authentication plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Get a list of authentication plugins
     * 
     * @return map of authentication plugins, or empty map if not found
     */
    public static Map<String, Authentication> getAuthenticationPlugins() {
        Map<String, Authentication> authenticators = new HashMap<String, Authentication>();
        ServiceLoader<Authentication> plugins = ServiceLoader
                .load(Authentication.class);
        for (Authentication plugin : plugins) {
            authenticators.put(plugin.getId(), plugin);
        }
        return authenticators;
    }

    /**
     * Gets a harvester plugin
     * 
     * @param id plugin identifier
     * @param storage a storage instance
     * @return a harvester plugin, or null if not found
     */
    public static Harvester getHarvester(String id, Storage storage) {
        ServiceLoader<Harvester> plugins = ServiceLoader.load(Harvester.class);
        for (Harvester plugin : plugins) {
            if (id.equals(plugin.getId())) {
                plugin.setStorage(storage);
                return plugin;
            }
        }
        return null;
    }

    /**
     * Get a list of harvester plugins
     * 
     * @return map of harvester plugins, or empty map if not found
     */
    public static Map<String, Harvester> getHarvesterPlugins() {
        Map<String, Harvester> harvesters = new HashMap<String, Harvester>();
        ServiceLoader<Harvester> plugins = ServiceLoader.load(Harvester.class);
        for (Harvester plugin : plugins) {
            harvesters.put(plugin.getId(), plugin);
        }
        return harvesters;
    }

    /**
     * Gets a indexer plugin
     * 
     * @param id plugin identifier
     * @return a indexer plugin, or null if not found
     */
    public static Indexer getIndexer(String id) {
        ServiceLoader<Indexer> plugins = ServiceLoader.load(Indexer.class);
        for (Indexer plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Get a list of indexer plugins
     * 
     * @return map of indexer plugins, or empty map if not found
     */
    public static Map<String, Indexer> getIndexerPlugins() {
        Map<String, Indexer> indexers = new HashMap<String, Indexer>();
        ServiceLoader<Indexer> plugins = ServiceLoader.load(Indexer.class);
        for (Indexer plugin : plugins) {
            indexers.put(plugin.getId(), plugin);
        }
        return indexers;
    }

    /**
     * Gets a roles plugin
     * 
     * @param id plugin identifier
     * @return a roles plugin, or null if not found
     */
    public static Roles getRoles(String id) {
        ServiceLoader<Roles> plugins = ServiceLoader.load(Roles.class);
        for (Roles plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Get a list of roles plugins
     * 
     * @return map of roles plugins, or empty map if not found
     */
    public static Map<String, Roles> getRolesPlugins() {
        Map<String, Roles> roles = new HashMap<String, Roles>();
        ServiceLoader<Roles> plugins = ServiceLoader.load(Roles.class);
        for (Roles plugin : plugins) {
            roles.put(plugin.getId(), plugin);
        }
        return roles;
    }

    /**
     * Gets a storage plugin
     * 
     * @param id plugin identifier
     * @return a storage plugin, or null if not found
     */
    public static Storage getStorage(String id) {
        ServiceLoader<Storage> plugins = ServiceLoader.load(Storage.class);
        for (Storage plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Get a list of storage plugins
     * 
     * @return map of storage plugins, or empty map if not found
     */
    public static Map<String, Storage> getStoragePlugins() {
        Map<String, Storage> storageMap = new HashMap<String, Storage>();
        ServiceLoader<Storage> plugins = ServiceLoader.load(Storage.class);
        for (Storage plugin : plugins) {
            storageMap.put(plugin.getId(), plugin);
        }
        return storageMap;
    }

    /**
     * Gets a transformer plugin
     * 
     * @param id plugin identifier
     * @return a transformer plugin, or null if not found
     */
    public static Transformer getTransformer(String id)
            throws TransformerException {
        ServiceLoader<Transformer> plugins = ServiceLoader
                .load(Transformer.class);
        for (Transformer plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Get a list of transformer plugins
     * 
     * @return map of transformer plugins, or empty map if not found
     */
    public static Map<String, Transformer> getTransformerPlugins() {
        Map<String, Transformer> transformers = new HashMap<String, Transformer>();
        ServiceLoader<Transformer> plugins = ServiceLoader
                .load(Transformer.class);
        for (Transformer plugin : plugins) {
            transformers.put(plugin.getId(), plugin);
        }
        return transformers;
    }

    /**
     * Gets an subscriber plugin
     * 
     * @param id plugin identifier
     * @return an Subscriber plugin, or null if not found
     */
    public static Subscriber getSubscriber(String id) {
        ServiceLoader<Subscriber> plugins = ServiceLoader
                .load(Subscriber.class);
        for (Subscriber plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Get a list of Subscriber plugins
     * 
     * @return map of Subscriber plugin, or empty map if not found
     */
    public static Map<String, Subscriber> getSubscriberPlugins() {
        Map<String, Subscriber> access_plugins = new HashMap<String, Subscriber>();
        ServiceLoader<Subscriber> plugins = ServiceLoader
                .load(Subscriber.class);
        for (Subscriber plugin : plugins) {
            access_plugins.put(plugin.getId(), plugin);
        }
        return access_plugins;
    }
}
