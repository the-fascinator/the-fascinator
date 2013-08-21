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
package com.googlecode.fascinator.portal.security;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.WebSecurityExpressionRoot;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.storage.StorageUtils;
import com.googlecode.fascinator.common.storage.impl.SpringStorageWrapper;

/**
 * Spring security methods for Fascinator.
 * 
 * @author Andrew Brazzatti
 * @author Jianfeng Li
 * 
 */
public class FascinatorWebSecurityExpressionRoot extends
        WebSecurityExpressionRoot {
    private Logger log = LoggerFactory
            .getLogger(FascinatorWebSecurityExpressionRoot.class);
    private Storage storageService;
    private JsonSimpleConfig systemConfiguration;
    private static final String DEFAULT_STORAGE_TYPE = "file-system";

    private static final String OID_PATTERN = ".+([0-9a-f]{32}).*";

    public FascinatorWebSecurityExpressionRoot(Authentication a,
            FilterInvocation fi) {
        super(a, fi);
        try {
            systemConfiguration = new JsonSimpleConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // initialiseStorage();
        SpringStorageWrapper storageServiceWrapper = new SpringStorageWrapper();
        storageService = storageServiceWrapper.getService();
    }

    /**
     * initialise storage system
     */
    private void initialiseStorage() {
        String storageType = systemConfiguration.getString(
                DEFAULT_STORAGE_TYPE, "storage", "type");
        storageService = PluginManager.getStorage(storageType);
        if (storageService == null) {
            throw new RuntimeException("Storage plugin '" + storageType
                    + "'. Ensure it is in the classpath.");
        }
        try {
            storageService.init(systemConfiguration.toString());
            log.debug("Loaded {}", storageService.getName());
        } catch (PluginException pe) {
            throw new RuntimeException("Failed to initialise storage", pe);
        }
    }

    /**
     * get view access of an object defined in request. reference to
     * /fascinator-
     * portal/src/main/config/portal/default/default/scripts/detail.py
     * 
     * @return boolean
     */
    public boolean hasViewAccess() {
        try {
            DigitalObject digitalObject = getDigitalObject();
            if (digitalObject != null) {
                String userName = (String) authentication.getPrincipal();
                Properties tfObjMeta = digitalObject.getMetadata();
                // check if the current user is the record owner
                String owner = tfObjMeta.getProperty("owner");
                if (userName.equals(owner)) {
                    return true;
                }
                // check using role-based security
                JsonObject workflowStageConfiguration = getWorkflowConfig(digitalObject);
                // Check whether user has the correct role to edit
                JSONArray allowedRoles = (JSONArray) workflowStageConfiguration
                        .get("visibility");
                Collection<GrantedAuthority> userRoles = authentication
                        .getAuthorities();
                for (GrantedAuthority grantedAuthority : userRoles) {
                    if (allowedRoles.contains(grantedAuthority.getAuthority())) {
                        return true;
                    }
                }
            } else {
                return true; // not need to be guarded // TODO: check with
                             // Andrew
            }

        } catch (StorageException e) {
            log.error("View access check failed - cannot get digital object: "
                    + e.toString());
        } catch (IOException e) {
            log.error("View access check failed - cannot get digital object: "
                    + e.toString());
        }
        return false;
    }

    /**
     * Default version of workflow access check for fascinator portal
     * 
     * @param String: uriPattern
     * @return boolean
     */
    public boolean hasWorkflowAccess() {
        String defaultRegex = ".+([0-9a-f]{8}[0-9a-f]{4}[0-9a-f]{4}[0-9a-f]{4}[0-9a-f]{12}).*";
        return hasWorkflowAccess(defaultRegex);
    }

    /**
     * Generic version of workflow access check
     * 
     * @param String: uriPattern
     * @return boolean
     */
    public boolean hasWorkflowAccess(String uriPattern) {
        boolean hasAccess = false;
        try {
            Pattern p = Pattern.compile(uriPattern);
            String requestURI = request.getRequestURI();
            if (!StringUtils.isEmpty(request.getQueryString())) {
                requestURI = requestURI + "?" + request.getQueryString();
            }
            Matcher m = p.matcher(requestURI);

            if (m.matches()) {
                String oid = m.group(1);

                DigitalObject digitalObject = getDigitalObject(oid);
                JsonSimple workflowMetadata = getWorkflowMetadata(digitalObject);
                String workflowId = workflowMetadata.getString(null, "id");
                JsonSimple workflowConfiguration = getWorkflowConfiguration(workflowId);

                JSONArray workflowStages = workflowConfiguration
                        .getArray("stages");
                JsonObject workflowStageConfiguration = null;
                for (int i = 0; i < workflowStages.size(); i++) {
                    JsonObject workflowStage = (JsonObject) workflowStages
                            .get(i);
                    if (workflowMetadata.getJsonObject().get("step")
                            .equals(workflowStage.get("name"))) {
                        workflowStageConfiguration = workflowStage;
                        break;
                    }
                }

                String guestOwnerEditAllowed = (String) workflowStageConfiguration
                        .get("guest_owner_edit_allowed");
                if (guestOwnerEditAllowed != null
                        && Boolean.parseBoolean(guestOwnerEditAllowed)) {
                    return true;
                }

                // Check whether user has the correct role to edit
                JSONArray allowedRoles = (JSONArray) workflowStageConfiguration
                        .get("security");
                Collection<GrantedAuthority> userRoles = authentication
                        .getAuthorities();
                for (GrantedAuthority grantedAuthority : userRoles) {
                    if (allowedRoles.contains(grantedAuthority.getAuthority())) {
                        return true;
                    }
                }

                String ownerEditAllowed = (String) workflowStageConfiguration
                        .get("owner_edit_allowed");

                if (ownerEditAllowed != null
                        && Boolean.parseBoolean(ownerEditAllowed)) {
                    // Check if user is owner of the object
                    String userName = (String) authentication.getPrincipal();
                    Properties tfObjMeta = digitalObject.getMetadata();
                    String owner = tfObjMeta.getProperty("owner");
                    if (userName.equals(owner)) {
                        return true;
                    }
                }
            } else {
                hasAccess = false;
            }
        } catch (Exception e) {
            log.error("Failed to check wrokflow access: " + e.toString());
            hasAccess = false;
        }

        return hasAccess;
    }

    /**
     * get the workflow configuration of a digital object at the current stage
     * 
     * @param digitalObject
     * @throws StorageException
     * @throws IOException
     */
    private JsonObject getWorkflowConfig(DigitalObject digitalObject)
            throws StorageException, IOException {
        JsonSimple workflowMetadata = getWorkflowMetadata(digitalObject);
        String workflowId = workflowMetadata.getString(null, "id");
        JsonSimple workflowConfiguration = getWorkflowConfiguration(workflowId);

        JSONArray workflowStages = workflowConfiguration.getArray("stages");
        JsonObject workflowStageConfiguration = null;
        for (int i = 0; i < workflowStages.size(); i++) {
            JsonObject workflowStage = (JsonObject) workflowStages.get(i);
            if (workflowMetadata.getJsonObject().get("step")
                    .equals(workflowStage.get("name"))) {
                workflowStageConfiguration = workflowStage;
                break;
            }
        }
        return workflowStageConfiguration;
    }

    /**
     * get the workflow configuration definition of given workflow ID
     * 
     * @param String workflowId
     * @return JsonSimple
     * @throws IOException
     */
    private JsonSimple getWorkflowConfiguration(String workflowId)
            throws IOException {
        String workflowConfigFileLocation = (String) systemConfiguration
                .getObject(
                        new Object[] { "portal", "packageTypes", workflowId })
                .get("jsonconfig");
        File workflowConfigFile = FascinatorHome
                .getPathFile("harvest/workflows/" + workflowConfigFileLocation);
        return new JsonSimple(workflowConfigFile);
    }

    /*
     * get digital from URI or query string
     */
    private DigitalObject getDigitalObject() {
        Pattern p = Pattern.compile(OID_PATTERN);
        String requestURI = request.getRequestURI();
        if (!StringUtils.isEmpty(request.getQueryString())) {
            requestURI = requestURI + "?" + request.getQueryString();
        }
        Matcher m = p.matcher(requestURI);
        DigitalObject digitalObject = null;
        try {
            if (m.matches()) {
                String oid = m.group(1);
                log.debug("Check permission of object {} ", oid);
                digitalObject = StorageUtils.getDigitalObject(storageService,
                        oid);
            }
        } catch (StorageException e) {
            log.error("Failed to check wrokflow access: " + e.toString());
        }
        return digitalObject;
    }

    private DigitalObject getDigitalObject(String oid) throws StorageException {
        DigitalObject digitalObject = StorageUtils.getDigitalObject(
                storageService, oid);
        return digitalObject;
    }

    private JsonSimple getWorkflowMetadata(DigitalObject digitalObject)
            throws StorageException, IOException {
        return new JsonSimple(digitalObject.getPayload("workflow.metadata")
                .open());
    }

}
