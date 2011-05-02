/*
 * The Fascinator - Portal
 * Copyright (C) 2011 University of Southern Queensland
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
package au.edu.usq.fascinator.portal.services.cache;

import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.portal.services.ByteRangeRequestCache;
import au.edu.usq.fascinator.portal.services.DatabaseServices;
import au.edu.usq.fascinator.portal.services.DynamicPageService;
import au.edu.usq.fascinator.portal.services.HarvestManager;
import au.edu.usq.fascinator.portal.services.HouseKeepingManager;
import au.edu.usq.fascinator.portal.services.PortalManager;
import au.edu.usq.fascinator.portal.services.ScriptingServices;
import au.edu.usq.fascinator.portal.services.VelocityService;
import au.edu.usq.fascinator.portal.velocity.JythonLogger;
import java.io.InputStream;
import java.util.List;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for instantiating jython scripts to be cached.
 *
 * @author Oliver Lucido
 */
public class JythonCacheEntryFactory implements CacheEntryFactory {

    /** Logging */
    private Logger log = LoggerFactory.getLogger(JythonCacheEntryFactory.class);

    /** Velocity service */
    private VelocityService velocityService;

    /** Scripting services */
    private ScriptingServices scriptingServices;

    /** Absolute path to base portal directory */
    private String portalPath;

    /** Default fallback portal id */
    private String defaultPortal;

    /** Skin priority */
    private List<String> skinPriority;

    /**
     * Constructs the factory.
     *
     * @param portalManager a PortalManager instance
     * @param velocityService a VelocityService instance
     * @param scriptingServices a ScriptingServices instance
     */
    public JythonCacheEntryFactory(PortalManager portalManager,
            VelocityService velocityService,
            ScriptingServices scriptingServices) {
        this.velocityService = velocityService;
        this.scriptingServices = new DeprecatedServicesWrapper(scriptingServices);
        defaultPortal = portalManager.getDefaultPortal();
        portalPath = portalManager.getHomeDir().toString();
        skinPriority = portalManager.getSkinPriority();
    }

    /**
     * Creates a jython object instance for the script cache.
     * 
     * @param key the path to the jython script
     * @return an instantiated jython object
     * @throws Exception if the jython object failed to be instantiated
     */
    @Override
    public Object createEntry(Object key) throws Exception {
        //log.debug("createEntry({})", key);
        String path = key.toString();
        int qmarkPos = path.lastIndexOf("?");
        if (qmarkPos != -1) {
            path = path.substring(0, qmarkPos);
        }
        int slashPos = path.indexOf("/");
        String portalId = path.substring(0, slashPos);
        PyObject scriptObject = null;
        InputStream in = velocityService.getResource(path);
        if (in == null) {
            log.debug("Failed to load script: '{}'", path);
        } else {
            // add current and default portal directories to python sys.path
            addSysPaths(portalId, Py.getSystemState());
            // setup the python interpreter
            PythonInterpreter python = PythonInterpreter.threadLocalStateInterpreter(null);
            // expose services for backward compatibility - deprecated
            python.set("Services", scriptingServices);
            //python.setLocals(scriptObject);
            JythonLogger jythonLogger = new JythonLogger(path);
            python.setOut(jythonLogger);
            python.setErr(jythonLogger);
            python.execfile(in, path);
            String scriptClassName = StringUtils.capitalize(
                    FilenameUtils.getBaseName(path)) + "Data";
            PyObject scriptClass = python.get(scriptClassName);
            if (scriptClass != null) {
                scriptObject = scriptClass.__call__();
            } else {
                log.debug("Failed to find class '{}'", scriptClassName);
            }
            python.cleanup();
        }
        return scriptObject;
    }

    /**
     * Add class paths for the jython interpreter to find other modules within
     * the portal directory structure.
     *
     * @param portalId a portal id
     * @param sys jython system state
     */
    private void addSysPaths(String portalId, PySystemState sys) {
        for (String skin : skinPriority) {
            String sysPath = portalPath + "/" + portalId + "/" + skin
                    + "/scripts";
            sys.path.append(Py.newString(sysPath));
        }
        if (!defaultPortal.equals(portalId)) {
            addSysPaths(defaultPortal, sys);
        }
    }

    /**
     * Internal class to wrap used to expose ScriptingServices to the global
     * jython scope as deprecated. The services should be retrieved from the
     * provided activation context.
     */
    private class DeprecatedServicesWrapper implements ScriptingServices {

        private ScriptingServices scriptingServices;

        public DeprecatedServicesWrapper(ScriptingServices scriptingServices) {
            this.scriptingServices = scriptingServices;
        }

        @Override
        public DatabaseServices getDatabase() {
            log.warn("WARN: getDatabase(): " +
                    "Global scope Services is deprecated, use the context");
            return scriptingServices.getDatabase();
        }

        @Override
        public DynamicPageService getPageService() {
            log.warn("WARN: getPageService(): " +
                    "Global scope Services is deprecated, use the context");
            return scriptingServices.getPageService();
        }

        @Override
        public Indexer getIndexer() {
            log.warn("WARN: getIndexer(): " +
                    "Global scope Services is deprecated, use the context");
            return scriptingServices.getIndexer();
        }

        @Override
        public Storage getStorage() {
            log.warn("WARN: getStorage(): " +
                    "Global scope Services is deprecated, use the context");
            return scriptingServices.getStorage();
        }

        @Override
        public HarvestManager getHarvestManager() {
            log.warn("WARN: getHarvestManager(): " +
                    "Global scope Services is deprecated, use the context");
            return scriptingServices.getHarvestManager();
        }

        @Override
        public HouseKeepingManager getHouseKeepingManager() {
            log.warn("WARN: getHouseKeepingManager(): " +
                    "Global scope Services is deprecated, use the context");
            return scriptingServices.getHouseKeepingManager();
        }

        @Override
        public PortalManager getPortalManager() {
            log.warn("WARN: getPortalManager(): " +
                    "Global scope Services is deprecated, use the context");
            return scriptingServices.getPortalManager();
        }

        @Override
        public ByteRangeRequestCache getByteRangeCache() {
            log.warn("WARN: getByteRangeCache(): " +
                    "Global scope Services is deprecated, use the context");
            return scriptingServices.getByteRangeCache();
        }

        @Override
        public VelocityService getVelocityService() {
            log.warn("WARN: getVelocityService(): " +
                    "Global scope Services is deprecated, use the context");
            return scriptingServices.getVelocityService();
        }
    }
}
