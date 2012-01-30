/*
 * The Fascinator - Portal
 * Copyright (C) 2010 University of Southern Queensland
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
package com.googlecode.fascinator.portal.sso;

import com.googlecode.fascinator.api.authentication.User;
import com.googlecode.fascinator.portal.JsonSessionState;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.expressme.openid.Association;
import org.expressme.openid.Authentication;
import org.expressme.openid.Endpoint;
import org.expressme.openid.OpenIdException;
import org.expressme.openid.OpenIdManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fascinator and OpenID integration.
 * @author Greg Pendlebury
 *
 */

public class OpenID implements SSOInterface {
    /** Logging */
    private Logger log = LoggerFactory.getLogger(OpenID.class);

    /** The incoming server request */
    private HttpServletRequest request;

    /** OpenID Manager */
    private OpenIdManager manager;

    /** Association MAC */
    private byte[] oidAssocicationMac;

    /** Endpoint Association */
    private String oidEndpointAlias;

    /** Remote logon URL */
    private String oidRemoteLogonUrl;

    /** Return address used */
    private String oidReturnAddress;

    /** Portal base url */
    private String portalUrl;

    /** OpenID Provider in use */
    private String oidProvider;

    /**
     * Return the OpenID ID. Must match configuration at instantiation.
     *
     * @return String The SSO implementation ID.
     */
    @Override
    public String getId() {
        return "OpenID";
    }

    /**
     * Return the on-screen label to describing this implementation.
     *
     * @return String The SSO implementation label.
     */
    @Override
    public String getLabel() {
        return "Login via OpenID";
    }

    /**
     * Return the HTML snippet to use in the interface.
     *
     * Implementations can append additional params to URLs.
     * Like so: "?ssoId=OpenID&{customString}"
     * eg: "?ssoId=OpenID&provider=Google"
     *
     * @param ssoUrl The basic ssoUrl for the server.
     * @return String The string to display as link text.
     */
    @Override
    public String getInterface(String ssoUrl) {
        String html = "";

        // TODO: This is starting to get too big for this file.
        // Should be refactored to refer to a velocity template.

        // Google
        html += "<a href=\"" + ssoUrl + "&provider=Google\">" +
                "<img title=\"Google\" src=\"" + portalUrl +
                "/images/google.png\"/ alt=\"Google\"></a>";

        html += "&nbsp;&nbsp;&nbsp; OR &nbsp;&nbsp;&nbsp;";

        // Yahoo
        html += "<a href=\"" + ssoUrl + "&provider=Yahoo\">" +
                "<img title=\"Yahoo\" src=\"" + portalUrl +
                "/images/yahoo.png\" alt=\"Yahoo\"/></a>";

        html += "<hr/>OR<br/>";

        // Custom OpenID
        String txt = " My own OpenID provider ";
        html += "<img title=\"OpenID\" src=\"" + portalUrl +
                "/images/openid.png\" alt=\"OpenID\"/> " +
                "<input class=\"custom\" type=\"text\" id=\"ssoProvider\"" +
                " onblur=\"blurOpenId(this);\" onfocus=\"focusOpenId(this);\"" +
                " value=\""+txt+"\"/> " +
                "<input type=\"hidden\" id=\"ssoUrl\" value=\""
                + ssoUrl + "&provider=\"/>" +
                "<input type=\"button\" name=\"openIdGo\"" +
                " onclick=\"doSso();\" value=\"GO\"/>" +
                "<br/>eg. 'http://yourname.myopenid.com'";

        // Custom OpenID scripts
        html += "<script type=\"text/javascript\">" +
                "function focusOpenId(obj) {" +
                "if (obj.value == \"" + txt + "\") {obj.value = \"\";}}" +
                "function blurOpenId(obj) {" +
                "if (obj.value == \"\") {obj.value = \"" + txt + "\";}}" +
                "function doSso() {" +
                "var ssoUrl = document.getElementById('ssoUrl').value;" +
                "var provider = document.getElementById('ssoProvider').value;" +
                "location.href=ssoUrl + provider;}" +
                "</script>";

        return html;
    }

    /**
     * Get the current user details in a User object.
     *
     * @return User A user object containing the current user.
     */
    @Override
    public User getUserObject(JsonSessionState session) {
        String username = (String) session.get("oidSsoIdentity");
        String fullname = (String) session.get("oidSsoDisplayName");
        String email = (String) session.get("oidSsoEmail");

        if (username == null) {
            return null;
        }

        OpenIDUser user = new OpenIDUser();
        user.setUsername(username);
        user.setSource("OpenID");
        user.set("name", fullname);
        user.set("email", email);
        return user;
    }

    /**
     * We cannot log the user out of UConnect, but we can clear Fascinator
     * session data regarding this user.
     *
     */
    @Override
    public void logout(JsonSessionState session) {
        session.remove("oidAssocicationMac");
        session.remove("oidEndpointAlias");
        session.remove("oidRemoteLogonUrl");
        session.remove("oidReturnAddress");
        session.remove("oidSsoIdentity");
        session.remove("oidSsoEmail");
        session.remove("oidSsoName");
        session.remove("oidProvider");
    }

    /**
     * Initialize the SSO Service
     *
     * @param session The server session data
     * @param request The incoming servlet request
     * @throws Exception if any errors occur
     */
    @Override
    public void ssoInit(JsonSessionState session, HttpServletRequest request)
            throws Exception {
        this.request = request;
        manager = new OpenIdManager();

        // Get/Cache/Retrieve the OpenID provider string
        String provider = request.getParameter("provider");
        if (provider != null) {
            oidProvider = provider;
            session.set("oidProvider", oidProvider);
        }
        oidProvider = (String) session.get("oidProvider");

        // Make sure our link is up-to-date
        portalUrl = (String) session.get("ssoPortalUrl");
    }

    /**
     * Prepare the SSO Service to receive a login from the user
     *
     * @param returnAddress The address to come back to after the login
     * @throws Exception if any errors occur
     */
    @Override
    public void ssoPrepareLogin(JsonSessionState session, String returnAddress,
            String server) throws Exception {
        // Set our data
        manager.setReturnTo(returnAddress);
        manager.setRealm(server);
        session.set("oidReturnAddress", returnAddress);
    }

    /**
     * Retrieve the login URL for redirection.
     *
     * @return String The URL used by the SSO Service for logins
     */
    @Override
    public String ssoGetRemoteLogonURL(JsonSessionState session) {
        if (oidProvider == null) {
            return null;
        }

        // Get the provider's data
        try {
            Endpoint endpoint = manager.lookupEndpoint(oidProvider);
            Association association = manager.lookupAssociation(endpoint);
            oidAssocicationMac = association.getRawMacKey();
            oidEndpointAlias = endpoint.getAlias();
            oidRemoteLogonUrl = manager.getAuthenticationUrl(endpoint, association);
            log.info("OpenID Logon URL: '{}'", oidRemoteLogonUrl);

            // Make sure we don't forget it
            session.set("oidAssocicationMac", oidAssocicationMac);
            session.set("oidEndpointAlias", oidEndpointAlias);
            session.set("oidRemoteLogonUrl", oidRemoteLogonUrl);
        } catch (OpenIdException ex) {
            log.error("OpenID Error: ", ex.getMessage());
            return null;
        }

        return oidRemoteLogonUrl;
    }

    /**
     * Get user details from the SSO Service and set them in the user session.
     *
     */
    @Override
    public void ssoCheckUserDetails(JsonSessionState session) {
        // Check if already logged in
        String username = (String) session.get("oidSsoIdentity");
        if (username != null) {
            return;
        }

        // SSO Service details
        oidReturnAddress = (String) session.get("oidReturnAddress");
        oidAssocicationMac = (byte[]) session.get("oidAssocicationMac");
        oidEndpointAlias = (String) session.get("oidEndpointAlias");
        if (oidAssocicationMac == null || oidEndpointAlias == null) {
            return;
        }

        // Extract any data that was returned
        try {
            manager.setReturnTo(oidReturnAddress);
            Authentication authentication = manager.getAuthentication(request,
                    oidAssocicationMac, oidEndpointAlias);
            String identity = authentication.getIdentity();
            String name = authentication.getFullname();
            String email = authentication.getEmail();
            log.debug("=== SSO: Provider: '{}', Data: '{}'", oidProvider, authentication.toString());

            if (identity != null) {
                session.set("oidSsoIdentity", identity);
            }
            if (email != null && !email.equals("null")) {
                session.set("oidSsoEmail", email);
            }
            // We try to set the display name as:
            // 1) Name > 2) Email > 3) ID
            if (name != null && !name.equals("nullnull")) {
                session.set("oidSsoName", name);
                session.set("oidSsoDisplayName", name);
            } else {
                if (email != null && !email.equals("null")) {
                    session.set("oidSsoDisplayName", email);
                } else {
                    session.set("oidSsoDisplayName", identity);
                }
            }
        } catch (OpenIdException ex) {
            log.error("Login event expected, but not found: '{}'", ex.getMessage());
        }
    }

    /**
     * Get a list of roles possessed by the current user from the SSO Service.
     *
     * @return List<String> Array of roles.
     */
    @Override
    public List<String> getRolesList(JsonSessionState session) {
        // Not supported by this provider
        return new ArrayList<String>();
    }
}
