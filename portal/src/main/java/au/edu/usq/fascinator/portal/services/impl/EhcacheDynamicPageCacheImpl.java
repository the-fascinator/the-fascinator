/*
 * The Fascinator - Portal
 * Copyright (C) 2008-2011 University of Southern Queensland
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
package au.edu.usq.fascinator.portal.services.impl;

import au.edu.usq.fascinator.common.JsonSimple;
import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.portal.services.DynamicPageCache;
import au.edu.usq.fascinator.portal.services.PortalManager;
import au.edu.usq.fascinator.portal.services.ScriptingServices;
import au.edu.usq.fascinator.portal.services.VelocityService;
import au.edu.usq.fascinator.portal.services.cache.JythonCacheEntryFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import org.python.core.PyInstance;
import org.python.core.PyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a DynamicPageCache using Ehcache.
 * 
 * @author Oliver Lucido
 */
public class EhcacheDynamicPageCacheImpl implements DynamicPageCache {

    /** Script cache id */
    private static final String SCRIPT_CACHE_ID = "scriptObjects";

    /** Path lookup cache id */
    private static final String PATH_CACHE_ID = "pathLookup";

    /** Default cache profile */
    private static final String DEFAULT_PROFILE = "default";

    /** Logging */
    private Logger log = LoggerFactory.getLogger(EhcacheDynamicPageCacheImpl.class);

    /** PortalManager instance */
    private PortalManager portalManager;

    /** Ehcache manager */
    private CacheManager cacheManager;

    /** Cache for jython script objects */
    private Ehcache scriptCache;

    /** Cache for path lookups */
    private Ehcache pathCache;

    /** Whether or not to check last modified timestamp on script files */
    private boolean lastModifiedCheck;

    /** Last modified timestamp cache */
    private Map<String, Long> lastModifiedMap;

    /**
     * Construct and configure the caches.
     * 
     * @param portalManager PortalManager instance
     * @param velocityService VelocityService instance
     * @param scriptingServices ScriptingServices instance
     */
    public EhcacheDynamicPageCacheImpl(PortalManager portalManager,
            VelocityService velocityService,
            ScriptingServices scriptingServices) {

        this.portalManager = portalManager;

        cacheManager = new CacheManager();
        cacheManager.addCache(SCRIPT_CACHE_ID);
        cacheManager.addCache(PATH_CACHE_ID);

        try {
            JsonSimpleConfig config = new JsonSimpleConfig();

            Map<String, JsonSimple> cacheProfiles = config.getJsonSimpleMap("portal", "caching", "profiles");
            Map<String, JsonSimple> cacheConfigs = config.getJsonSimpleMap("portal", "caching", "caches");
            for (String cacheId : cacheConfigs.keySet()) {
                //log.debug("{}: {}", cacheId, cacheConfigs.get(cacheId));
                Ehcache cache = cacheManager.getCache(cacheId);
                if (cache == null) {
                    log.warn("Cache '{}' does not exist!", cacheId);
                } else {
                    JsonSimple jsonConfig = cacheConfigs.get(cacheId);
                    String profileId = jsonConfig.getString(DEFAULT_PROFILE, "profile");
                    if (cacheProfiles.containsKey(profileId)) {
                        log.debug("Configuring cache '{}' with profile '{}'", cacheId, profileId);
                        JsonSimple profile = cacheProfiles.get(profileId);
                        CacheConfiguration cacheConfig = cache.getCacheConfiguration();
                        cacheConfig.setMaxElementsInMemory(profile.getInteger(10000, "maxElementsInMemory"));
                        cacheConfig.setEternal(profile.getBoolean(false, "eternal"));
                        if (!cacheConfig.isEternal()) {
                            cacheConfig.setTimeToIdleSeconds(profile.getInteger(120, "timeToIdleSeconds"));
                            cacheConfig.setTimeToLiveSeconds(profile.getInteger(120, "timeToLiveSeconds"));
                        }
                        cacheConfig.setOverflowToDisk(profile.getBoolean(false, "overflowToDisk"));
                        cacheConfig.setMaxElementsOnDisk(profile.getInteger(10000, "maxElementsOnDisk"));
                        cacheConfig.setMemoryStoreEvictionPolicy(profile.getString("LRU", "memoryStoreEvictionPolicy"));
                    } else {
                        log.warn("Cache profile '{}' does not exist!", profileId);
                    }
                }
            }

            lastModifiedCheck = config.getBoolean(false, "portal", "caching",
                    "caches", SCRIPT_CACHE_ID, "lastModifiedCheck");
            if (lastModifiedCheck) {
                lastModifiedMap = new HashMap<String, Long>();
            }

            scriptCache = new SelfPopulatingCache(
                    cacheManager.getCache(SCRIPT_CACHE_ID),
                    new JythonCacheEntryFactory(portalManager, velocityService,
                    scriptingServices));
            pathCache = cacheManager.getCache(PATH_CACHE_ID);

        } catch (IOException ioe) {
            log.warn("Failed to configure caches, using defaults...", ioe);
        }
    }

    /**
     * Shutdown the caches properly when Tapestry shuts down.
     */
    @Override
    public void registryDidShutdown() {
        if (cacheManager != null) {
            cacheManager.shutdown();
        }
    }

    /**
     * Gets the script object with the specified path. If not already cached
     * the script object will be created by the JythonCacheEntryFactory.
     *
     * @param path jython script path - including portal and skin. this should
     * be a valid Velocity resource
     * @return a script object or null if an error occurred
     */
    @Override
    public PyObject getScriptObject(String path) {
        //log.debug("getScriptObject: {} ({})", path, tid);
        if (lastModifiedCheck) {
            // check if the script was modified and remove from cache
            File scriptFile = new File(portalManager.getHomeDir(), path);
            long lastModified = scriptFile.lastModified();
            //log.debug("lastModified: {}:{}", scriptFile, lastModified);
            if (lastModifiedMap.containsKey(path)) {
                if (lastModified > lastModifiedMap.get(path)) {
                    //log.debug("Expiring {} because it was modified!", path);
                    scriptCache.remove(path);
                }
            }
            lastModifiedMap.put(path, lastModified);
        }
        Element element = scriptCache.get(path);
        if (element != null) {
            Object objectValue = element.getObjectValue();
            if (objectValue instanceof PyInstance) {
                return new LocalPyInstance((PyInstance) objectValue);
            }
        }
        return null;
    }

    /**
     * Gets the fully resolved path for the specified path including the skin.
     * 
     * @param pathId a path to resolve
     * @return resolved path
     */
    @Override
    public String getPath(String pathId) {
        //log.debug("getPath: {}", id);
        Element element = pathCache.get(pathId);
        if (element != null) {
            return element.getObjectValue().toString();
        }
        return null;
    }

    /**
     * Puts an entry into the path lookup cache.
     * 
     * @param pathId path to resolve
     * @param path resolved path
     */
    @Override
    public void putPath(String pathId, String path) {
        //log.debug("putPath: {} {}", id, path);
        pathCache.put(new Element(pathId, path));
    }

    /**
     * Internal wrapper class for PyInstance to keep variables contained
     * within the executing thread.
     */
    private class LocalPyInstance extends PyInstance {

        public LocalPyInstance(PyInstance instance) {
            super(instance.instclass);
            // make sure objects assigned in the __init__ method are visible
            for (PyObject key : instance.__dict__.asIterable()) {
                __dict__.__setitem__(key.__str__(),
                        instance.__dict__.__finditem__(key));
            }
        }
    }
}
