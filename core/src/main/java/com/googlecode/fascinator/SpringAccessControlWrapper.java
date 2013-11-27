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
package com.googlecode.fascinator;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.access.AccessControlException;
import com.googlecode.fascinator.api.access.AccessControlManager;
import com.googlecode.fascinator.api.access.AccessControlSchema;
import com.googlecode.fascinator.common.JsonSimpleConfig;

/**
 * AccessControlManager wrapper used in Spring.
 * 
 * @author Andrew Brazzatti
 * @author Jianfeng Li
 * 
 */
@Component(value = "fascinatorAccess")
public class SpringAccessControlWrapper implements AccessControlManager {
    private AccessControlManager accessmanagerPlugin;
    private Logger log = LoggerFactory
            .getLogger(SpringAccessControlWrapper.class);

    public SpringAccessControlWrapper() {
        JsonSimpleConfig systemConfiguration;
        try {
            systemConfiguration = new JsonSimpleConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String accessControlType = "accessmanager";
        accessmanagerPlugin = PluginManager.getAccessManager(accessControlType);
        if (accessmanagerPlugin == null) {
            throw new RuntimeException("Storage plugin '" + accessControlType
                    + "'. Ensure it is in the classpath.");
        }
        try {
            accessmanagerPlugin.init(systemConfiguration.toString());
            log.debug(
                    "AccessControlManager plugin with Spring has been initialiased: {}",
                    accessmanagerPlugin.getName());
        } catch (PluginException pe) {
            throw new RuntimeException(
                    "Failed to initialise AccessControlManager plugin with Spring",
                    pe);
        }
    }

    @Override
    public String getId() {
        return accessmanagerPlugin.getId();
    }

    @Override
    public String getName() {
        return accessmanagerPlugin.getName();
    }

    @Override
    public PluginDescription getPluginDetails() {
        return accessmanagerPlugin.getPluginDetails();
    }

    @Override
    public void init(File jsonFile) throws PluginException {
        accessmanagerPlugin.init(jsonFile);
    }

    @Override
    public void init(String jsonString) throws PluginException {
        accessmanagerPlugin.init(jsonString);
    }

    @Override
    public void shutdown() throws PluginException {
        accessmanagerPlugin.shutdown();
    }

    @Override
    public AccessControlSchema getEmptySchema() {
        return accessmanagerPlugin.getEmptySchema();
    }

    @Override
    public List<AccessControlSchema> getSchemas(String recordId)
            throws AccessControlException {
        return accessmanagerPlugin.getSchemas(recordId);
    }

    @Override
    public void applySchema(AccessControlSchema newSecurity)
            throws AccessControlException {
        accessmanagerPlugin.applySchema(newSecurity);
    }

    @Override
    public void removeSchema(AccessControlSchema oldSecurity)
            throws AccessControlException {
        accessmanagerPlugin.removeSchema(oldSecurity);
    }

    @Override
    public List<String> getRoles(String recordId) throws AccessControlException {
        return accessmanagerPlugin.getRoles(recordId);
    }

    @Override
    public List<String> getUsers(String recordId) throws AccessControlException {
        return accessmanagerPlugin.getUsers(recordId);
    }

    @Override
    public List<String> getPossibilities(String field)
            throws AccessControlException {
        return accessmanagerPlugin.getPossibilities(field);
    }

    @Override
    public void setActivePlugin(String pluginId) {
        accessmanagerPlugin.setActivePlugin(pluginId);
    }

    @Override
    public String getActivePlugin() {
        return accessmanagerPlugin.getActivePlugin();
    }

    @Override
    public List<PluginDescription> getPluginList() {
        return accessmanagerPlugin.getPluginList();
    }
}
