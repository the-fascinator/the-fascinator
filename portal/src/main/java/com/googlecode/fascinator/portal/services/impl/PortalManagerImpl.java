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
package com.googlecode.fascinator.portal.services.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.HarvestClient;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.portal.Portal;
import com.googlecode.fascinator.portal.services.PortalManager;

public class PortalManagerImpl implements PortalManager {

    private static final String PORTAL_JSON = "portal.json";

    private Logger log = LoggerFactory.getLogger(PortalManagerImpl.class);

    private Map<String, Portal> portals;

    private Map<String, Long> lastModified;

    private Map<String, File> portalFiles;

    private File portalsDir;

    private String defaultPortal;

    private String defaultSkin;

    private String defaultDisplay;

    private List<String> skinPriority;

    public PortalManagerImpl() {
        log.debug("Creating PortalManagerImpl");
        try {
            JsonSimpleConfig config = new JsonSimpleConfig();

            // Default templates
            defaultPortal = config.getString(DEFAULT_PORTAL_NAME, "portal",
                    "defaultView");
            defaultSkin = config.getString(DEFAULT_SKIN, "portal", "skins",
                    "default");
            defaultDisplay = config.getString(DEFAULT_DISPLAY, "portal",
                    "displays", "default");

            skinPriority = config.getStringList("portal", "skins", "order");
            if (!skinPriority.contains(defaultSkin)) {
                skinPriority.add(defaultSkin);
            }

            // Path to the files on disk
            String home = config.getString(DEFAULT_PORTAL_HOME, "portal",
                    "home");
            File homeDir = new File(home);
            if (!homeDir.exists()) {
                home = DEFAULT_PORTAL_HOME_DEV;
            }
            init(home);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void init(String portalsDir) {
        try {
            this.portalsDir = new File(portalsDir);
            lastModified = new HashMap<String, Long>();
            portalFiles = new HashMap<String, File>();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Portal> getPortals() {
        if (portals == null) {
            portals = new HashMap<String, Portal>();
            loadPortals();
        }
        return portals;
    }

    @Override
    public Portal getDefault() {
        return get(defaultPortal);
    }

    @Override
    public File getHomeDir() {
        return portalsDir;
    }

    @Override
    public Portal get(String name) {
        Portal portal = null;
        if (getPortals().containsKey(name)) {
            if (lastModified.containsKey(name)
                    && lastModified.get(name) < portalFiles.get(name)
                            .lastModified()) {
                loadPortal(name);
            }
            portal = getPortals().get(name);
        } else {
            portal = loadPortal(name);
        }
        return portal;
    }

    @Override
    public boolean exists(String name) {
        return getPortals().containsKey(name);
    }

    @Override
    public void add(Portal portal) {
        String portalName = portal.getName();
        // log.info("PORTAL name: " + portalName);
        getPortals().put(portalName, portal);
    }

    @Override
    public void remove(String name) {
        File portalDir = new File(portalsDir, name);
        File portalFile = new File(portalDir, PORTAL_JSON);
        portalFile.delete();
        getPortals().remove(name);
    }

    @Override
    public void save(Portal portal) {
        String portalName = portal.getName();

        File portalFile = new File(new File(portalsDir,
                FilenameUtils.normalize(portalName)), PORTAL_JSON);
        portalFile.getParentFile().mkdirs();
        try {
            FileWriter writer = new FileWriter(portalFile);
            writer.write(portal.toString(true));
            writer.close();
        } catch (IOException ioe) {
        }
    }

    private void loadPortals() {
        File[] portalDirs = portalsDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                String name = file.getName();
                return file.isDirectory() && !name.equals(".svn");
            }
        });
        for (File dir : portalDirs) {
            loadPortal(dir.getName());
        }
    }

    private Portal loadPortal(String name) {
        Portal portal = null;
        File portalFile = new File(new File(portalsDir, name), PORTAL_JSON);
        if (portalFile.exists()) {
            lastModified.put(name, portalFile.lastModified());
            portalFiles.put(name, portalFile);
            try {
                portal = new Portal(portalFile);
                add(portal);
                // log.info("Loaded portal: " + portal);
            } catch (IOException e) {
                log.warn("Portal: " + name + " failed to load", e);
            }
        }
        return portal;
    }

    @Override
    public void reharvest(String objectId) {
        try {
            HarvestClient client = new HarvestClient();
            // High priority when user requests
            // single object be reharvested
            client.reharvest(objectId, true);
        } catch (Exception e) {
            log.error("Object reharvest failed", e);
        }
    }

    @Override
    public void reharvest(Set<String> objectIds) {
        try {
            HarvestClient client = new HarvestClient();
            for (String oid : objectIds) {
                client.reharvest(oid);
            }
        } catch (Exception e) {
            log.error("Portal reharvest failed", e);
        }
    }

    @Override
    public String getDefaultPortal() {
        return defaultPortal;
    }

    @Override
    public String getDefaultDisplay() {
        return defaultDisplay;
    }

    @Override
    public List<String> getSkinPriority() {
        return skinPriority;
    }
}
