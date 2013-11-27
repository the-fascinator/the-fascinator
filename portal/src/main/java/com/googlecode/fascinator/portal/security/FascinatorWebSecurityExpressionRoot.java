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
import java.util.List;
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

import com.googlecode.fascinator.api.access.AccessControl;
import com.googlecode.fascinator.api.access.AccessControlException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.storage.StorageUtils;

/**
 * Spring security check methods for Fascinator portal.
 * 
 * @author Andrew Brazzatti
 * @author Jianfeng Li
 * 
 */
public class FascinatorWebSecurityExpressionRoot extends
        WebSecurityExpressionRoot {
    private Logger log = LoggerFactory
            .getLogger(FascinatorWebSecurityExpressionRoot.class);
    private Storage storage;
    private JsonSimpleConfig systemConfiguration;
    private AccessControl accessControl;
    private JsonSimple workflowConfigJson;

    private static final String OID_PATTERN = ".+([0-9a-f]{32}).*";

    public FascinatorWebSecurityExpressionRoot(Authentication a,
            FilterInvocation fi) {
        super(a, fi);
        try {
            systemConfiguration = new JsonSimpleConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FascinatorWebSecurityExpressionRoot(Authentication authentication,
            FilterInvocation fi, Storage storage, AccessControl accessControl) {
        this(authentication, fi);
        this.storage = storage;
        this.accessControl = accessControl;
    }

    public boolean hasDownloadAccess() {
        return hasDownloadAccess(OID_PATTERN);
    }

    private boolean hasDownloadAccess(String oidPattern) {
        if (isAdmin()) {
            return true;
        }
        String oid = getOid(oidPattern);
        DigitalObject digitalObject = getDigitalObject(oid);
        Properties tfObjMeta;
        try {
            tfObjMeta = digitalObject.getMetadata();

            if (isPublished(tfObjMeta)) {
                return true;
            }

            if (isOwner(tfObjMeta)) {
                return true;
            }

            if (isInAllowedRoles(oid)) {
                return true;
            }

            if (isInAllowedUsers(oid)) {
                return true;
            }
            // TODO: Check whether attachment is set to public or not
        } catch (Exception e) {
            log.error("hasDownloadAccess check failed", e);

        }
        return false;
    }

    private boolean isAdmin() {
        if (authentication.getAuthorities().contains("admin")) {
            return true;
        }
        return false;
    }

    /**
     * get view access of an object defined in request. reference to
     * /fascinator-
     * portal/src/main/config/portal/default/default/scripts/detail.py
     * 
     * @return boolean
     */
    public boolean hasViewAccess() {
        return hasViewAccess(OID_PATTERN);
    }

    public boolean hasViewAccess(String oidPattern) {
        String oid = getOid(oidPattern);
        if (oid != null) {
            try {
                DigitalObject digitalObject = getDigitalObject(oid);
                if (digitalObject != null) {

                    Properties tfObjMeta = digitalObject.getMetadata();

                    if (isPublished(tfObjMeta)) {
                        return true;
                    }

                    if (isOwner(tfObjMeta)) {
                        return true;
                    }

                    if (isInAllowedRoles(oid)) {
                        return true;
                    }

                    if (isInAllowedUsers(oid)) {
                        return true;
                    }

                } else {
                    return false;
                }

            } catch (Exception e) {
                log.error("Failed to check view access: ", e);
            }
        }
        return false;
    }

    public boolean isInAllowedUsers() {
        return isInAllowedUsers(OID_PATTERN);
    }

    private boolean isInAllowedUsers(String oid) {
        String userName = (String) authentication.getPrincipal();
        try {
            List<String> allowedUsers = accessControl.getUsers(oid);
            if (allowedUsers != null) {
                for (String allowedUserName : allowedUsers) {
                    if (allowedUserName.equals(userName)) {
                        return true;
                    }
                }
            }
        } catch (AccessControlException e) {
            return false;
        }
        return false;
    }

    public boolean isPublished() {
        String oid = getOid(OID_PATTERN);
        if (oid != null) {
            try {
                DigitalObject digitalObject = getDigitalObject(oid);
                if (digitalObject != null) {
                    Properties tfObjMeta = digitalObject.getMetadata();
                    return isPublished(tfObjMeta);
                }
            } catch (Exception e) {
                log.error("Failed to check property of isPublished.", e);
            }
        }
        return false;
    }

    private boolean isPublished(Properties tfObjMeta) {
        if ("true".equals(tfObjMeta.get("published"))) {
            return true;
        }
        return false;
    }

    public boolean isOwner() {
        return isOwner(OID_PATTERN);
    }

    public boolean isOwner(String oidPattern) {
        String oid = getOid(oidPattern);
        if (oid != null) {
            try {
                DigitalObject digitalObject = getDigitalObject(oid);
                if (digitalObject != null) {
                    Properties tfObjMeta = digitalObject.getMetadata();
                    return isOwner(tfObjMeta);
                }
            } catch (Exception e) {
                log.error("Failed to check ownership", e);
            }
        }
        return false;
    }

    private boolean isOwner(Properties tfObjMeta) {
        String userName = (String) authentication.getPrincipal();
        // check if the current user is the record owner
        String owner = tfObjMeta.getProperty("owner");
        if (userName.equals(owner)) {
            return true;
        }
        return false;
    }

    public boolean isInAllowedRoles() {
        String oid = getOid(OID_PATTERN);
        return isInAllowedRoles(oid);
    }

    /**
     * Check whether user has the correct role to edit
     */
    private boolean isInAllowedRoles(String oid) {
        List<String> allowedRoles;
        try {
            allowedRoles = accessControl.getRoles(oid);
            if (allowedRoles != null) {
                Collection<GrantedAuthority> userRoles = authentication
                        .getAuthorities();
                for (GrantedAuthority grantedAuthority : userRoles) {
                    if (allowedRoles.contains(grantedAuthority.getAuthority())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to check allowed roles", e);
        }
        return false;
    }

    /**
     * Check editing access
     */
    public boolean hasWorkflowAccess() {
        return hasWorkflowAccess(OID_PATTERN);
    }

    /**
     * Check editing access
     */
    private boolean hasWorkflowAccess(String oidPattern) {
        boolean hasAccess = false;
        try {
            String oid = getOid(oidPattern);
            if (oid != null) {
                DigitalObject digitalObject = getDigitalObject(oid);
                JsonObject workflowStageConfiguration = getWorkflowStageConfig(digitalObject);

                if (isGuestOwnerEditAllowed(workflowStageConfiguration)) {
                    return true;
                }

                if (isOwnerEditAllowed(digitalObject,
                        workflowStageConfiguration)) {
                    return true;
                }

                if (isInAllowedRoles(oid)) {
                    return true;
                }

            } else {
                hasAccess = false;
            }
        } catch (Exception e) {
            log.error("Failed to check workflow access: ", e);
            hasAccess = false;
        }
        return hasAccess;
    }

    public boolean isOwnerEditAllowed() {
        String oid = getOid(OID_PATTERN);
        try {
            if (oid != null) {
                DigitalObject digitalObject = getDigitalObject(oid);
                JsonObject workflowStageConfiguration = getWorkflowStageConfig(digitalObject);
                return isOwnerEditAllowed(digitalObject,
                        workflowStageConfiguration);
            }
        } catch (Exception e) {
            log.error("Failed to check OwnerEditAllowed property", e);
        }
        return false;
    }

    private boolean isOwnerEditAllowed(DigitalObject digitalObject,
            JsonObject workflowStageConfiguration) {
        String ownerEditAllowed = (String) workflowStageConfiguration
                .get("owner_edit_allowed");

        if (ownerEditAllowed != null && Boolean.parseBoolean(ownerEditAllowed)) {
            // Check if user is the owner of the object
            Properties tfObjMeta;
            try {
                tfObjMeta = digitalObject.getMetadata();
            } catch (StorageException e) {
                log.error("Failed to read metadata from object", e);
                return false;
            }
            if (isOwner(tfObjMeta)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean isGuestOwnerEditAllowed() {
        String oid = getOid(OID_PATTERN);
        try {
            if (oid != null) {
                DigitalObject digitalObject = getDigitalObject(oid);
                JsonObject workflowStageConfiguration = getWorkflowStageConfig(digitalObject);
                return isGuestOwnerEditAllowed(workflowStageConfiguration);
            }
        } catch (Exception e) {
            log.error("Failed to check GuestOwnerEditAllowed property", e);
        }
        return false;
    }

    private boolean isGuestOwnerEditAllowed(
            JsonObject workflowStageConfiguration) {
        String guestOwnerEditAllowed = (String) workflowStageConfiguration
                .get("guest_owner_edit_allowed");
        if (guestOwnerEditAllowed != null
                && Boolean.parseBoolean(guestOwnerEditAllowed)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Extract object ID from requestURI using
     * 
     * @param oidPatthern
     */
    private String getOid(String oidPatthern) {
        Pattern p = Pattern.compile(oidPatthern);
        String requestURI = request.getRequestURI();
        if (!StringUtils.isEmpty(request.getQueryString())) {
            requestURI = requestURI + "?" + request.getQueryString();
        }
        Matcher m = p.matcher(requestURI);

        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * get the workflow configuration of a digital object at the current stage
     * 
     * @param digitalObject
     * @throws StorageException
     * @throws IOException
     */
    private JsonObject getWorkflowStageConfig(DigitalObject digitalObject)
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
        if (workflowConfigJson == null) {
            String workflowConfigFileLocation = (String) systemConfiguration
                    .getObject(
                            new Object[] { "portal", "packageTypes", workflowId })
                    .get("jsonconfig");
            File workflowConfigFile = FascinatorHome
                    .getPathFile("harvest/workflows/"
                            + workflowConfigFileLocation);
            workflowConfigJson = new JsonSimple(workflowConfigFile);
        }
        return workflowConfigJson;
    }

    private DigitalObject getDigitalObject(String oid) {
        DigitalObject digitalObject = null;
        try {
            digitalObject = StorageUtils.getDigitalObject(storage, oid);
        } catch (StorageException e) {
            log.error("When checking access, cannot get object {}", oid, e);
        }
        return digitalObject;
    }

    private JsonSimple getWorkflowMetadata(DigitalObject digitalObject)
            throws StorageException, IOException {
        return new JsonSimple(digitalObject.getPayload("workflow.metadata")
                .open());
    }

}
