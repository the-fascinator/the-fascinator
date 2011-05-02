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
package au.edu.usq.fascinator.portal.pages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.util.TimeInterval;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.upload.services.MultipartDecoder;
import org.apache.tapestry5.upload.services.UploadedFile;
import org.slf4j.Logger;

import au.edu.usq.fascinator.HarvestClient;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.authentication.AuthenticationException;
import au.edu.usq.fascinator.api.authentication.User;
import au.edu.usq.fascinator.common.JsonSimple;
import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.common.MimeTypeUtil;
import au.edu.usq.fascinator.portal.FormData;
import au.edu.usq.fascinator.portal.JsonSessionState;
import au.edu.usq.fascinator.portal.services.DynamicPageService;
import au.edu.usq.fascinator.portal.services.GenericStreamResponse;
import au.edu.usq.fascinator.portal.services.HttpStatusCodeResponse;
import au.edu.usq.fascinator.portal.services.PortalManager;
import au.edu.usq.fascinator.portal.services.PortalSecurityManager;
import au.edu.usq.fascinator.portal.services.VelocityService;

/**
 * <h3>Introduction</h3>
 * <p>
 * Dispatch is the only Tapestry Page object, and it is responsible for three
 * tasks:
 * </p>
 * 
 * <ul>
 * <li><strong>Resource routing</strong>, mostly URL parsing according to some
 * basic rules, but also looking for special cases.</li>
 * <li><strong>Security</strong>, particularly focusing on Single Sign-On.</li>
 * <li><strong>File upload</strong> handling and storage. Files need to grabbed
 * from the Tapestry framework and moved into our storage and the transformation
 * tool chain.</li>
 * </ul>
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * <b>https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Portal/
 * JavaCore#TapestryPages</b>
 * </p>
 * 
 * @author Oliver Lucido
 */
public class Dispatch {

    private static final String AJAX_EXT = ".ajax";

    private static final String POST_EXT = ".post";

    private static final String SCRIPT_EXT = ".script";

    /** The default resource if none is specified. The home page, generally */
    public static final String DEFAULT_RESOURCE = "home";

    @Inject
    private Logger log;

    @SessionState
    private JsonSessionState sessionState;

    @Inject
    private DynamicPageService pageService;

    @Inject
    private PortalManager portalManager;

    @Inject
    private PortalSecurityManager security;

    @Inject
    private VelocityService velocityService;

    @Inject
    private Request request;

    @Inject
    private Response response;

    @Inject
    private MultipartDecoder decoder;

    @Inject
    private RequestGlobals rg;

    private FormData formData;

    // Resource Processing variable
    private String resourceName;
    private String portalId;
    private String defaultPortal;
    private String requestUri;
    private String[] path;
    private HttpServletRequest hsr;
    private boolean isFile;
    private boolean isSpecial;

    // Rendering variables
    private String mimeType;
    private InputStream stream;

    private JsonSimpleConfig sysConfig;

    /**
     * Entry point for Tapestry to send page requests.
     * 
     * @param params : An array of request parameters from Tapestry
     * @returns StreamResponse : Tapestry object to return streamed response
     */
    public StreamResponse onActivate(Object... params) {
        // log.debug("Dispatch starting : {} {}",
        // request.getMethod(), request.getPath());

        try {
            sysConfig = new JsonSimpleConfig();
            defaultPortal = sysConfig.getString(
                    PortalManager.DEFAULT_PORTAL_NAME, "portal", "defaultView");
        } catch (IOException ex) {
            log.error("Error accessing system config", ex);
            return new HttpStatusCodeResponse(500,
                    "Sorry, an internal server error has occured");
        }

        // Do all our parsing
        resourceName = resourceProcessing();

        // Make sure it's valid
        if (resourceName == null) {
            if (response.isCommitted()) {
                return GenericStreamResponse.noResponse();
            }
            return new HttpStatusCodeResponse(404, "Page not found: "
                    + requestUri);
        }

        // Initialise storage for our form data
        // if there's no persistant data found.
        prepareFormData();

        // Set session timeout (defaults to 2 hours)
        int timeoutMins = sysConfig.getInteger(120, "portal", "sessionTimeout");
        hsr.getSession().setMaxInactiveInterval(timeoutMins * 60);

        // SSO Integration - Ignore AJAX and such
        if (!isSpecial) {
            // Make sure it's not a static resource
            if (security.testForSso(sessionState, resourceName, requestUri)) {
                // Run SSO
                boolean redirected = security.runSsoIntegration(sessionState, formData);
                // Finish here if SSO redirected
                if (redirected) {
                    return GenericStreamResponse.noResponse();
                }
            }
        }

        // Make static resources cacheable
        if (resourceName.indexOf(".") != -1 && !isSpecial) {
            response.setHeader("Cache-Control", "public");
            response.setDateHeader("Expires", System.currentTimeMillis()
                    + new TimeInterval("10y").milliseconds());
        }

        // Are we doing a file upload?
        isFile = ServletFileUpload.isMultipartContent(hsr);
        if (isFile) {
            fileProcessing();
        }

        // Page render time
        renderProcessing();

        return new GenericStreamResponse(mimeType, stream);
    }

    private void prepareFormData() {
        hsr = rg.getHTTPServletRequest();
        if ((resourceName.indexOf(".") == -1) || isSpecial) {
            if (formData == null) {
                formData = new FormData(request, hsr);
                // log.debug("created FormData:{}", formData);
            }
        }
    }

    private void fileProcessing() {
        // What we are looking for
        UploadedFile uploadedFile = null;
        String workflowId = null;

        // Roles of current user
        String username = (String) sessionState.get("username");
        String userSource = (String) sessionState.get("source");
        List<String> roles = null;
        try {
            User user = security.getUser(sessionState, username, userSource);
            String[] roleArray = security.getRolesList(sessionState, user);
            roles = Arrays.asList(roleArray);
        } catch (AuthenticationException ex) {
            log.error("Error retrieving user data.");
            return;
        }

        // The workflow we're using
        List<String> reqParams = request.getParameterNames();
        log.info("REQUEST {}", request);
        log.info("reqParams {}", reqParams);
        if (reqParams.contains("upload-file-workflow")) {
            workflowId = request.getParameter("upload-file-workflow");
        }
        if (workflowId == null) {
            log.error("No workflow provided with form data.");
            return;
        }

		
        JsonSimple workflowConfig = sysConfig.getJsonSimpleMap("uploader").get(
                workflowId);

        // Roles allowed to upload into this workflow
        boolean security_check = false;
        for (String role : workflowConfig.getStringList("security")) {
            if (roles.contains(role)) {
                security_check = true;
            }
        }
        if (!security_check) {
            log.error("Security error, current user not allowed to upload.");
            return;
        }

        // Get the workflow's file directory
        String file_path = workflowConfig.getString(null, "upload-path");

        // Get the uploaded file
        for (String param : reqParams) {
            UploadedFile tmpFile = decoder.getFileUpload(param);
            if (tmpFile != null) {
                // Our file
                uploadedFile = tmpFile;
            }
        }
        if (uploadedFile == null) {
            log.error("No uploaded file found!");
            return;
        }

        // Write the file to that directory
        file_path = file_path + "/" + uploadedFile.getFileName();
        File file = new File(file_path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException ex) {
                log.error("Failed writing file", ex);
                return;
            }
        }
        uploadedFile.write(file);

        // Make sure the new file gets harvested
        String configPath = workflowConfig.getString(null, "json-config");
        if (configPath == null) {
            log.error("No harvest configuration file provided!");
            return;
        }
        File harvestFile = new File(configPath);
        // Get the workflow template needed for stage 1
        String template = "";
        try {
            JsonSimple harvestConfig = new JsonSimple(harvestFile);
            List<JsonSimple> stages = harvestConfig.getJsonSimpleList("stages");
            if (stages.size() > 0) {
                template = stages.get(0).getString(null, "template");
            }
        } catch (IOException ex) {
            log.error("Unable to access workflow config : ", ex);
        }

        HarvestClient harvester = null;
        String oid = null;
        String error = null;
        try {
            harvester = new HarvestClient(harvestFile, file, username);
            harvester.start();
            oid = harvester.getUploadOid();
            harvester.shutdown();
        } catch (PluginException ex) {
            error = "File upload failed : " + ex.getMessage();
            log.error(error);
            harvester.shutdown();
        } catch (Exception ex) {
            log.error("Failed harvest", ex);
            return;
        }
        boolean success = file.delete();
        if (!success) {
            log.error("Error deleting uploaded file from cache: "
                    + file.getAbsolutePath());
        }

        // Now create some session data for use later
        Map<String, String> file_details = new LinkedHashMap<String, String>();
        file_details.put("name", uploadedFile.getFileName());
        if (error != null) {
            // Strip our package/class details from error string
            Pattern pattern = Pattern.compile("au\\..+Exception:");
            Matcher matcher = pattern.matcher(error);
            file_details.put("error", matcher.replaceAll(""));
        }
        file_details.put("workflow", workflowId);
        file_details.put("template", template);
        file_details.put("location", file_path);
        file_details.put("size", String.valueOf(uploadedFile.getSize()));
        file_details.put("type", uploadedFile.getContentType());
        if (oid != null) {
            file_details.put("oid", oid);
        }
        // Helps some browsers (like IE7) resolve the path from the form
        sessionState.set("fileName", uploadedFile.getFileName());
        sessionState.set(uploadedFile.getFileName(), file_details);
        formData.set("fileProcessing", "true");
        formData.set("oid", oid);
        sessionState.set("uploadFormData", formData);
    }

    private void renderProcessing() {
        // render the page or retrieve the resource
        if ((resourceName.indexOf(".") == -1) || isSpecial) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            mimeType = pageService.render(portalId, resourceName, out,
                    formData, sessionState);
            stream = new ByteArrayInputStream(out.toByteArray());
        } else {
            mimeType = MimeTypeUtil.getMimeType(resourceName);
            stream = velocityService.getResource(portalId, resourceName);
        }
    }

    private String resourceProcessing() {
        requestUri = request.getAttribute("RequestURI").toString();
        path = requestUri.split("/");

        // log.debug("requestUri:'{}', path:'{}'", requestUri, path);
        // log.debug("path.length:{}", path.length);
        if ("".equals(requestUri) || path.length == 1) {
            portalId = "".equals(path[0]) ? defaultPortal : path[0];
            String url = sysConfig.getString(null, "urlBase");
            if (url != null) {
                url += portalId + "/" + DEFAULT_RESOURCE;
            } else {
                url = request.getContextPath() + "/" + portalId + "/"
                        + DEFAULT_RESOURCE;
            }
            try {
                log.debug("REDIRECT : '{}'", url);
                response.sendRedirect(url);
            } catch (IOException ioe) {
                log.error("Failed to redirect to default URL: {}", url);
            }
            return null;
        }

        portalId = (String) sessionState.get("portalId", defaultPortal);
        resourceName = DEFAULT_RESOURCE;
        if (path.length > 1) {
            portalId = path[0];
            resourceName = StringUtils.join(path, "/", 1, path.length);
        }

        if (!portalManager.exists(portalId)) {
            return null;
        }

        isSpecial = false;
        String match = getBestMatchResource(resourceName);
        // log.trace("resourceName = {}, match = {}", resourceName, match);

        return match;
    }

    /**
     * Parse the request URL to find the best matching resource from the portal.
     * 
     * This method will recursively call itself if required to break the URL
     * down into constituent parts.
     * 
     * @param resource : The resource we are looking for
     * @returns String : The best matching resource
     */
    public String getBestMatchResource(String resource) {
        String searchable = resource;
        String ext = "";
        // Look for AJAX
        if (resource.endsWith(AJAX_EXT)) {
            isSpecial = true;
            ext = AJAX_EXT;
        }
        // Look for scripts
        if (resource.endsWith(SCRIPT_EXT)) {
            isSpecial = true;
            ext = SCRIPT_EXT;
        }
        // Strip special extensions whilst checking on disk
        if (isSpecial) {
            searchable = resource.substring(0, resource.lastIndexOf(ext));
        }
        // Return if found
        if (velocityService.resourceExists(portalId, searchable) != null) {
            return resource;
        }
        // Keep checking
        int slash = resource.lastIndexOf('/');
        if (slash != -1) {
            return getBestMatchResource(resource.substring(0, slash));
        }
        return null;
    }
}
