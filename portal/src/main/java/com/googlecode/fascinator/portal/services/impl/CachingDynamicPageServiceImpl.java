/* 
 * The Fascinator - Portal - Dynamic Page Service
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Response;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.python.core.Py;
import org.python.core.PyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.portal.FormData;
import com.googlecode.fascinator.portal.JsonSessionState;
import com.googlecode.fascinator.portal.guitoolkit.GUIToolkit;
import com.googlecode.fascinator.portal.services.DynamicPageCache;
import com.googlecode.fascinator.portal.services.DynamicPageService;
import com.googlecode.fascinator.portal.services.HouseKeepingManager;
import com.googlecode.fascinator.portal.services.PortalManager;
import com.googlecode.fascinator.portal.services.PortalSecurityManager;
import com.googlecode.fascinator.portal.services.ScriptingServices;
import com.googlecode.fascinator.portal.services.VelocityService;

/**
 * 
 * 
 * @author Oliver Lucido
 */
public class CachingDynamicPageServiceImpl implements DynamicPageService {

    /** Default layout template name */
    private static final String DEFAULT_LAYOUT = "layout";

    /** Extension for AJAX resources */
    private static final String AJAX_EXT = ".ajax";

    /** Extension for script resources */
    private static final String SCRIPT_EXT = ".script";

    /** Activation method for jython scripts */
    private static final String SCRIPT_ACTIVATE_METHOD = "__activate__";

    /** Logging */
    private Logger log = LoggerFactory
            .getLogger(CachingDynamicPageServiceImpl.class);

    /** Tapestry HTTP servlet request support */
    @Inject
    private RequestGlobals requestGlobals;

    /** HTTP Request */
    @Inject
    private Request request;

    /** HTTP Response */
    @Inject
    private Response response;

    /** Services to expose to the jython scripts */
    @Inject
    private ScriptingServices scriptingServices;

    /** House keeping */
    @Inject
    private HouseKeepingManager houseKeeping;

    /** Security manager */
    @Inject
    private PortalSecurityManager security;

    /** Page caching support */
    @Inject
    private DynamicPageCache pageCache;

    /** Velocity service */
    private VelocityService velocityService;

    /** System configuration */
    private JsonSimpleConfig config;

    /** Layout template name */
    private String layoutName;

    /** Application base URL */
    private String urlBase;

    /** Absolute path to portal base directory */
    private String portalPath;

    /** GUI toolkit */
    private GUIToolkit toolkit;

    /** Default fallback portal id */
    private String defaultPortal;

    /** Default display template */
    private String defaultDisplay;

    /** Default version string */
    private String versionString;

    /**
     * Constructs and configures the service.
     * 
     * @param portalManager PortalManager instance
     * @param velocityService VelocityService instance
     */
    public CachingDynamicPageServiceImpl(PortalManager portalManager,
            VelocityService velocityService) {
        this.velocityService = velocityService;
        try {
            config = new JsonSimpleConfig();
            layoutName = config.getString(DEFAULT_LAYOUT, "portal", "layout");
            urlBase = config.getString(null, "urlBase");
            versionString = config.getString(null, "version.string");
            toolkit = new GUIToolkit();
            portalPath = portalManager.getHomeDir().getAbsolutePath();
            defaultPortal = portalManager.getDefaultPortal();
            defaultDisplay = portalManager.getDefaultDisplay();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a Velocity resource. This method is deprecated, please use
     * VelocityService.getResource() instead.
     * 
     * @param resourcePath valid Velocity resource path
     * @return resource stream or null if not found
     */
    @Override
    @Deprecated
    public InputStream getResource(String resourcePath) {
        log.warn(
                "getResource() is deprecated, use VelocityService.getResource()  ({})",
                resourcePath);
        return velocityService.getResource(resourcePath);
    }

    /**
     * Gets a Velocity resource. This method is deprecated, please use
     * VelocityService.getResource() instead.
     * 
     * @param portalId the portal to get the resource from
     * @param resourceName the resource to get
     * @return resource stream or null if not found
     */
    @Override
    @Deprecated
    public InputStream getResource(String portalId, String resourceName) {
        log.warn(
                "getResource() is deprecated, use VelocityService.getResource()  ({}/{})",
                portalId, resourceName);
        return velocityService.getResource(portalId, resourceName);
    }

    /**
     * Resolves the given resource to a valid Velocity resource if possible.
     * This method is deprecated, please use VelocityService.resourceExists()
     * instead.
     * 
     * @param portalId the portal to get the resource from
     * @param resourceName the resource to check
     * @return a valid Velocity resource path or null if not found
     */
    @Override
    @Deprecated
    public String resourceExists(String portalId, String resourceName) {
        String resourcePath = velocityService.resourceExists(portalId,
                resourceName);
        log.warn(
                "resourceExists() is deprecated, use VelocityService.resourceExists() ({})",
                resourcePath);
        return resourcePath;
    }

    /**
     * Renders the Velocity template with the specified form data.
     * 
     * @param portalId the portal to get the template from
     * @param pageName the page template to render
     * @param out render results will be written to this output stream
     * @param formData request form data
     * @param sessionState current session
     * @return MIME type of the response
     */
    @Override
    public String render(String portalId, String pageName, OutputStream out,
            FormData formData, JsonSessionState sessionState) {

        // remove extension for special cases
        boolean isAjax = pageName.endsWith(AJAX_EXT);
        boolean isScript = pageName.endsWith(SCRIPT_EXT);
        if (isAjax || isScript) {
            pageName = FilenameUtils.removeExtension(pageName);
        }

        // setup script and velocity context
        String contextPath = request.getContextPath();
        int serverPort = requestGlobals.getHTTPServletRequest().getServerPort();

        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("systemConfig", config);
        bindings.put("Services", scriptingServices);
        bindings.put("systemProperties", System.getProperties());
        bindings.put("request", request);
        bindings.put("response", response);
        bindings.put("formData", formData);
        bindings.put("sessionState", sessionState);
        bindings.put("security", security);
        bindings.put("contextPath", contextPath);
        bindings.put("scriptsPath", portalPath + "/" + portalId + "/scripts");
        bindings.put("portalDir", portalPath + "/" + portalId);
        bindings.put("portalId", portalId);
        bindings.put("urlBase", urlBase);
        if (versionString == null) {
            bindings.put("portalPath", urlBase + portalId);
        } else {
            bindings.put("portalPath", urlBase + "verNum" + versionString + "/"
                    + portalId);
        }
        bindings.put("defaultPortal", defaultPortal);
        bindings.put("pageName", pageName);
        bindings.put("serverPort", serverPort);
        bindings.put("toolkit", toolkit);
        bindings.put("log", log);
        bindings.put("notifications", houseKeeping.getUserMessages());
        bindings.put("bindings", bindings);

        // run page and template scripts
        Set<String> messages = new HashSet<String>();
        bindings.put("page",
                evalScript(portalId, layoutName, bindings, messages));
        bindings.put("self", evalScript(portalId, pageName, bindings, messages));

        // try to return the proper MIME type
        String mimeType = "text/html";
        Object mimeTypeAttr = request.getAttribute("Content-Type");
        if (mimeTypeAttr != null) {
            mimeType = mimeTypeAttr.toString();
        }

        // stop here if the scripts have already sent a response
        boolean committed = response.isCommitted();
        if (committed) {
            // log.debug("Response has been sent or redirected");
            return mimeType;
        }

        if (velocityService.resourceExists(portalId, pageName + ".vm") != null) {
            // set up the velocity context
            VelocityContext vc = new VelocityContext();
            for (String key : bindings.keySet()) {
                vc.put(key, bindings.get(key));
            }
            vc.put("velocityContext", vc);
            if (!messages.isEmpty()) {
                vc.put("renderMessages", messages);
            }

            try {
                // render the page content
                log.debug("Rendering page {}/{}.vm...", portalId, pageName);
                StringWriter pageContentWriter = new StringWriter();
                velocityService.renderTemplate(portalId, pageName, vc,
                        pageContentWriter);
                if (isAjax || isScript) {
                    out.write(pageContentWriter.toString().getBytes("UTF-8"));
                } else {
                    vc.put("pageContent", pageContentWriter.toString());
                }
            } catch (Exception e) {
                ByteArrayOutputStream eOut = new ByteArrayOutputStream();
                e.printStackTrace(new PrintStream(eOut));
                String eMsg = eOut.toString();
                log.error("Failed to render page ({})!\n=====\n{}\n=====",
                        isAjax ? "ajax" : (isScript ? "script" : "html"), eMsg);
                String errorMsg = "<pre>Page content template error: "
                        + pageName + "\n" + eMsg + "</pre>";
                if (isAjax || isScript) {
                    try {
                        out.write(errorMsg.getBytes());
                    } catch (Exception e2) {
                        log.error("Failed to output error message!");
                    }
                } else {
                    vc.put("pageContent", errorMsg);
                }
            }

            if (!(isAjax || isScript)) {
                try {
                    // render the page using the layout template
                    log.debug("Rendering layout {}/{}.vm for page {}.vm...",
                            new Object[] { portalId, layoutName, pageName });
                    Writer pageWriter = new OutputStreamWriter(out, "UTF-8");
                    velocityService.renderTemplate(portalId, layoutName, vc,
                            pageWriter);
                    pageWriter.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return mimeType;
    }

    /**
     * Renders a display template. This is generally used by calling the
     * #parseDisplayTemplate() method in the portal-library.vm.
     * 
     * @param context Velocity context
     * @param template display template name
     * @param metadata Solr metadata
     * @return rendered content
     */
    @Override
    public String renderObject(Context context, String template,
            SolrDoc metadata) {
        // log.debug("========== START renderObject ==========");

        // setup script and velocity context
        String portalId = context.get("portalId").toString();
        String displayType = metadata.getString(defaultDisplay, "display_type");
        if ("".equals(displayType)) {
            displayType = defaultDisplay;
        }
        // On the detail page, check for a preview template too
        if (template.startsWith("detail")) {
            String previewType = metadata.getString(null, "preview_type");
            if (previewType != null && !"".equals(previewType)) {
                log.debug("Preview template found: '{}'", previewType);
                displayType = previewType;
            }
        }
        String templateName = "display/" + displayType + "/" + template;

        // log.debug("displayType: '{}'", displayType);
        // log.debug("templateName: '{}'", templateName);

        Object parentPageObject = null;
        Context objectContext = new VelocityContext(context);
        if (objectContext.containsKey("parent")) {
            parentPageObject = objectContext.get("parent");
        } else {
            parentPageObject = objectContext.get("self");
        }
        // log.debug("parentPageObject: '{}'", parentPageObject);

        objectContext.put("pageName", template);
        objectContext.put("displayType", displayType);
        objectContext.put("parent", parentPageObject);
        objectContext.put("metadata", metadata);

        // evaluate the context script if exists
        Set<String> messages = null;
        if (objectContext.containsKey("renderMessages")) {
            messages = (Set<String>) objectContext.get("renderMessages");
        } else {
            messages = new HashSet<String>();
            context.put("renderMessages", messages);
        }
        Map<String, Object> bindings = (Map<String, Object>) objectContext
                .get("bindings");
        bindings.put("metadata", metadata);
        objectContext.put("self",
                evalScript(portalId, templateName, bindings, messages));

        String content = "";
        try {
            // render the page content
            log.debug("Rendering display page {}/{}.vm...", portalId,
                    templateName);
            StringWriter pageContentWriter = new StringWriter();
            velocityService.renderTemplate(portalId, templateName,
                    objectContext, pageContentWriter);
            content = pageContentWriter.toString();
        } catch (Exception e) {
            log.error("Failed rendering display page: {}", templateName);
            ByteArrayOutputStream eOut = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(eOut));
            String eMsg = eOut.toString();
            messages.add("Page content template error: " + templateName + "\n"
                    + eMsg);
        }

        // log.debug("========== END renderObject ==========");
        return content;
    }

    /**
     * Run the jython script with the given context. This method only calls the
     * activation method on the jython script objects which are retrieved from
     * the page cache.
     * 
     * @param portalId portal to get the script from
     * @param pageName page name that the script is supporting
     * @param context context for the script
     * @param messages a list to append error messages to if necessary
     * @return the jython script object
     */
    private PyObject evalScript(String portalId, String pageName,
            Map<String, Object> context, Set<String> messages) {
        PyObject scriptObject = null;
        String scriptName = "scripts/" + pageName + ".py";
        try {
            String path = velocityService.resourceExists(portalId, scriptName);
            if (path == null) {
                log.debug("No script for portalId:'{}' scriptName:'{}'",
                        portalId, scriptName);
            } else {
                scriptObject = pageCache.getScriptObject(path);
                if (scriptObject.__findattr__(SCRIPT_ACTIVATE_METHOD) != null) {
                    // log.debug("activating '{}' within thread '{}'",
                    // scriptObject, Thread.currentThread().getId());
                    scriptObject.invoke(SCRIPT_ACTIVATE_METHOD,
                            Py.java2py(context));
                } else {
                    log.warn("{} method not found for scriptPath:'{}'",
                            SCRIPT_ACTIVATE_METHOD, path);
                }
            }
        } catch (Exception e) {
            ByteArrayOutputStream eOut = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(eOut));
            String eMsg = eOut.toString();
            log.warn("Failed to run script!\n=====\n{}\n=====", eMsg);
            messages.add("Script error: " + scriptName + "\n" + eMsg);
        }
        return scriptObject;
    }
}
