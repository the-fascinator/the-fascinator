/*
 * The Fascinator - Portal
 * Copyright (C) 2010-2011 University of Southern Queensland
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.googlecode.fascinator.api.access.AccessControlManager;
import com.googlecode.fascinator.api.authentication.AuthManager;
import com.googlecode.fascinator.api.authentication.AuthenticationException;
import com.googlecode.fascinator.api.authentication.User;
import com.googlecode.fascinator.api.roles.RolesManager;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.authentication.GenericUser;
import com.googlecode.fascinator.portal.FormData;
import com.googlecode.fascinator.portal.JsonSessionState;
import com.googlecode.fascinator.portal.services.PortalManager;
import com.googlecode.fascinator.portal.services.PortalSecurityManager;
import com.googlecode.fascinator.portal.sso.SSOInterface;

/**
 * The security manager coordinates access to various security plugins when
 * cross plugin awareness is required, and executes some server side logic
 * required for features such as single sign-on.
 * 
 * @author Greg Pendlebury
 */
@Component(value = "portalSecurityManager")
public class PortalSecurityManagerImpl implements PortalSecurityManager {

    /** Prefix for storing SSO parameters whilst round-tripping */
    private static String SSO_STORAGE_PREFIX = "ssoStoredParam_";

    /** Prefix to use for 'source' with trust tokens */
    private static String TRUST_TOKEN_PREFIX = "TrustToken_";

    /** Default trust token expiry period */
    private static String TRUST_TOKEN_EXPIRY = "600";

    /** Logging */
    private Logger log = LoggerFactory
            .getLogger(PortalSecurityManagerImpl.class);

    /** User entry point for SSO logon */
    private String SSO_LOGIN_PAGE = "/sso";

    /** Form data */
    private FormData formData;

    /** Access Manager - item level security */
    @Inject
    private AccessControlManager accessManager;

    /** Authentication Manager - logging in */
    @Inject
    private AuthManager authManager;

    /** Role Manager - user groups */
    @Resource(name = "fascinatorRoleManager")
    @Inject
    private RolesManager roleManager;

    /** HTTP Request */
    @Inject
    private Request request;

    /** HTTP Response */
    @Inject
    private Response response;

    /** Request globals */
    @Inject
    private RequestGlobals rg;

    /** System Configuration */
    private JsonSimpleConfig config;

    /** Single Sign-On providers */
    private Map<String, SSOInterface> sso;

    /** Server public URL base */
    private String serverUrlBase;

    /** Default Portal */
    private String defaultPortal;

    /** SSO Login URL */
    private String ssoLoginUrl;

    /** detailSubPage detection */
    private Pattern detailPattern;

    /** URL Exclusions : Starts with */
    private List<String> excStarts;

    /** URL Exclusions : Ends with */
    private List<String> excEnds;

    /** URL Exclusions : Equals */
    private List<String> excEquals;

    /** Trust tokens */
    private Map<String, String> tokens;

    /** Trust tokens - Expiry period */
    private Map<String, Long> tokenExpiry;

    /**
     * Basic constructor, should be run automatically by Tapestry.
     * 
     */
    public PortalSecurityManagerImpl() throws IOException {
        // Get system configuration
        config = new JsonSimpleConfig();

        // For all SSO providers configured
        sso = new LinkedHashMap<String, SSOInterface>();
        for (String ssoId : config.getStringList("sso", "plugins")) {
            // Instantiate from the ServiceLoader
            SSOInterface valid = getSSOProvider(ssoId);
            if (valid == null) {
                log.error("Invalid SSO Implementation requested: '{}'", ssoId);
            } else {
                // Store valid implementations
                sso.put(ssoId, valid);
                log.info("SSO Provider instantiated: '{}'", ssoId);
            }
        }

        defaultPortal = config.getString(PortalManager.DEFAULT_PORTAL_NAME,
                "portal", "defaultView");
        serverUrlBase = config.getString(null, "urlBase");
        ssoLoginUrl = serverUrlBase + defaultPortal + SSO_LOGIN_PAGE;

        // Get exclusions Strings from config
        excStarts = config.getStringList("sso", "urlExclusions", "startsWith");
        excEnds = config.getStringList("sso", "urlExclusions", "endsWith");
        excEquals = config.getStringList("sso", "urlExclusions", "equals");

        // Trust tokens
        Map<String, JsonSimple> tokenMap = config.getJsonSimpleMap("sso",
                "trustTokens");
        tokens = new HashMap<String, String>();
        tokenExpiry = new HashMap<String, Long>();
        for (String key : tokenMap.keySet()) {
            JsonSimple tok = tokenMap.get(key);
            String publicKey = tok.getString(null, "publicKey");
            String privateKey = tok.getString(null, "privateKey");
            String expiry = tok.getString(TRUST_TOKEN_EXPIRY, "expiry");
            if (publicKey != null && privateKey != null) {
                // Valid key
                tokens.put(publicKey, privateKey);
                tokenExpiry.put(publicKey, Long.valueOf(expiry));
            } else {
                log.error("Invalid token data: '{}'", key);
            }
        }
    }

    /**
     * Get a SSO Provider from the ServiceLoader
     * 
     * @param id SSO Implementation ID
     * @return SSOInterface implementation matching the ID, if found
     */
    private SSOInterface getSSOProvider(String id) {
        ServiceLoader<SSOInterface> providers = ServiceLoader
                .load(SSOInterface.class);
        for (SSOInterface provider : providers) {
            if (id.equals(provider.getId())) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Return the Access Control Manager
     * 
     * @return AccessControlManager
     */
    @Override
    public AccessControlManager getAccessControlManager() {
        return accessManager;
    }

    /**
     * Return the Authentication Manager
     * 
     * @return AuthManager
     */
    @Override
    public AuthManager getAuthManager() {
        return authManager;
    }

    /**
     * Return the Role Manager
     * 
     * @return RolesManager
     */
    @Override
    public RolesManager getRoleManager() {
        return roleManager;
    }

    /**
     * Get the list of roles possessed by the current user.
     * 
     * @param user The user object of the current user
     * @return String[] A list of roles
     */
    @Override
    public String[] getRolesList(JsonSessionState session, User user) {
        String source = user.getSource();
        List<String> ssoRoles = new ArrayList<String>();

        // SSO Users
        if (sso.containsKey(source)) {
            ssoRoles.addAll(sso.get(source).getRolesList(session));
        }

        // Standard Users
        GenericUser gUser = (GenericUser) user;
        String[] standardRoles = roleManager.getRoles(gUser.getUsername());
        for (String role : standardRoles) {
            // Merge the two
            if (!ssoRoles.contains(role)) {
                ssoRoles.add(role);
            }
        }

        // Cast to array and return
        return ssoRoles.toArray(standardRoles);
    }

    /**
     * Retrieve the details of a user by username
     * 
     * @param username The username of a user to retrieve
     * @param source The authentication source if known
     * @return User The user requested
     * @throws AuthenticationException if any errors occur
     */
    @Override
    public User getUser(JsonSessionState session, String username, String source)
            throws AuthenticationException {
        // Sanity check
        if (username == null || username.equals("") || source == null
                || source.equals("")) {
            throw new AuthenticationException("Invalid user data requested");
        }

        // SSO Users
        if (sso.containsKey(source)) {
            GenericUser user = (GenericUser) sso.get(source).getUserObject(
                    session);
            // Sanity check our data
            if (user == null || !user.getUsername().equals(username)) {
                throw new AuthenticationException("Unknown user '" + username
                        + "'");
            }
            return user;
        }

        // Trust token users
        if (source.startsWith(TRUST_TOKEN_PREFIX)) {
            String sUsername = (String) session.get("username");
            String sSource = (String) session.get("source");

            // We can't lookup token users so it must match
            if (sUsername == null || !username.equals(sUsername)
                    || sSource == null || !source.equals(sSource)) {
                throw new AuthenticationException("Unknown user '" + username
                        + "'");
            }

            // Seems valid, create a basic user object and return
            GenericUser user = new GenericUser();
            user.setUsername(username);
            user.setSource(source);
            return user;
        }

        // Standard users
        authManager.setActivePlugin(source);
        return authManager.getUser(username);
    }

    /**
     * Logout the provided user
     * 
     * @return user The user to logout
     */
    @Override
    public void logout(JsonSessionState session, User user)
            throws AuthenticationException {
        String source = user.getSource();

        // Clear session
        session.remove("username");
        session.remove("source");

        // SSO Users
        if (sso.containsKey(source)) {
            sso.get(source).logout(session);
            return;
        }

        // Trust token users
        if (source.startsWith(TRUST_TOKEN_PREFIX)) {
            session.remove("validToken");
            return;
        }

        // Standard users
        authManager.logOut(user);
    }

    /**
     * Wrapper method for other SSO methods provided by the security manager. If
     * desired, the security manager can take care of the integration using the
     * default usage pattern, rather then calling them individually.
     * 
     * @param session : The session of the current request
     * @param formData : FormData object for the current request
     * @return boolean : True if SSO has redirected, in which case no response
     *         should be sent by Dispatch, otherwise False.
     */
    @Override
    public boolean runSsoIntegration(JsonSessionState session, FormData formData) {
        this.formData = formData;

        // Used in integrating with thrid party systems. They can send us a
        // user, we will log them in via a SSO round-trip, then send them back
        // to the external system
        String returnUrl = request.getParameter("returnUrl");
        if (returnUrl != null) {
            log.info("External redirect requested: '{}'", returnUrl);
            session.set("ssoReturnUrl", returnUrl);
        }

        // The URL parameters can contain a trust token
        String utoken = request.getParameter("token");
        String stoken = (String) session.get("validToken");
        String token = null;
        // Or an 'old' token still in the session
        if (stoken != null) {
            token = stoken;
        }
        // But give the URL priority
        if (utoken != null) {
            token = utoken;
        }
        if (token != null) {
            // Valid token
            if (testTrustToken(session, token)) {
                // Dispatch can continue
                return false;
            }

            // Invalid token
            // Given that trust tokens are designed for system integration
            // we are going to fail with a non-branded error message
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Invalid or expired security token!");
            } catch (IOException ex) {
                log.error("Error sending 403 response to client!");
            }
            // We don't want Dispatch to send a response
            return true;
        }

        // Single Sign-On integration
        try {
            // Instantiate with access to the session
            String ssoId = ssoInit(session);
            if (ssoId != null) {
                // We are logging in, so send them to the SSO portal
                String ssoUrl = ssoGetRemoteLogonURL(session, ssoId);
                if (ssoUrl != null) {
                    log.info("Redirect to external URL: '{}'", ssoUrl);
                    response.sendRedirect(ssoUrl);
                    return true;
                }
            } else {
                // Otherwise, check if we have user's details
                boolean valid = ssoCheckUserDetails(session);
                // If we validly logged in an SSO user, check for an
                // external redirect to third party systems
                if (valid) {
                    returnUrl = (String) session.get("ssoReturnUrl");
                    if (returnUrl != null) {
                        log.info("Redirect to external URL: '{}'", returnUrl);
                        session.remove("ssoReturnUrl");
                        response.sendRedirect(returnUrl);
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            log.error("SSO Error!", ex);
        }

        return false;
    }

    /**
     * Initialize the SSO Service, prepare a login if required
     * 
     * @param session The server session data
     * @throws Exception if any errors occur
     */
    @Override
    public String ssoInit(JsonSessionState session) throws Exception {
        // Keep track of the user switching portals for
        // link building in other methods
        String portalId = (String) session.get("portalId", defaultPortal);
        ssoLoginUrl = serverUrlBase + portalId + SSO_LOGIN_PAGE;

        // Find out what page we are on
        String path = request.getAttribute("RequestURI").toString();
        String currentAddress = serverUrlBase + path;

        // Store the portal URL, might be required by implementers to build
        // an interface (images etc).
        session.set("ssoPortalUrl", serverUrlBase + portalId);

        // Makes sure all SSO plugins get initialised
        for (String ssoId : sso.keySet()) {
            sso.get(ssoId).ssoInit(session, rg.getHTTPServletRequest());
        }

        // Are we logging in right now?
        String ssoId = request.getParameter("ssoId");

        // If this isn't the login page...
        if (!currentAddress.contains(SSO_LOGIN_PAGE)) {
            // Store the current address for use later
            session.set("returnAddress", currentAddress);
            // We might still be logging in from a deep link
            if (ssoId == null) {
                // No we're not, finished now
                return null;
            } else {
                // Yes it's a deep link, store any extra query params
                // since they probably won't survive the round-trip
                // through SSO.
                for (String param : request.getParameterNames()) {
                    if (!param.equals("ssoId")) {
                        // Store all the other parameters
                        session.set(SSO_STORAGE_PREFIX + param,
                                request.getParameter(param));
                    }
                }
            }
        }

        // Get the last address to return the user to
        String returnAddress = (String) session.get("returnAddress");
        if (returnAddress == null) {
            // Or use the home page
            returnAddress = serverUrlBase + portalId + "/home";
        }

        // Which SSO provider did the user request?
        if (ssoId == null) {
            log.error("==== SSO: SSO ID not found!");
            return null;
        }
        if (!sso.containsKey(ssoId)) {
            log.error("==== SSO: SSO ID invalid: '{}'!", ssoId);
            return null;
        }

        // The main event... finally
        sso.get(ssoId).ssoPrepareLogin(session, returnAddress, serverUrlBase);
        return ssoId;
    }

    /**
     * Get user details from SSO connection and set them in the user session.
     * 
     * @return boolean: Flag whether a user was actually logged in or not.
     */
    @Override
    public boolean ssoCheckUserDetails(JsonSessionState session) {
        // After the SSO roun-trip, restore any old query parameters we lost
        List<String> currentParams = request.getParameterNames();
        // Cast a copy of keySet() to array to avoid errors as we modify
        String[] oldParams = session.keySet().toArray(new String[0]);
        // Loop through session data...
        for (String key : oldParams) {
            // ... looking for SSO stored params
            if (key.startsWith(SSO_STORAGE_PREFIX)) {
                // Remove our prefix...
                String oldParam = key.replace(SSO_STORAGE_PREFIX, "");
                // ... and check if it survived the trip
                if (!currentParams.contains(oldParam)) {
                    // No it didn't, add it to form data... the parameters are
                    // already accessible from there in Jython
                    String data = (String) session.get(key);
                    formData.set(oldParam, data);
                    // Don't forget to clear it from the session
                    session.remove(key);
                }
            }
        }

        // Check our SSO providers for valid logins
        for (String ssoId : sso.keySet()) {
            sso.get(ssoId).ssoCheckUserDetails(session);
            GenericUser user = (GenericUser) sso.get(ssoId).getUserObject(
                    session);
            if (user != null) {
                session.set("username", user.getUsername());
                session.set("source", ssoId);
                return true;
            }
        }
        return false;
    }

    /**
     * Build a Map of Maps of on-screen string values for each SSO provider.
     * Should be enough to generate a login interface.
     * 
     * @return Map Containing the data structure of valid SSO interfaces.
     */
    @Override
    public Map<String, Map<String, String>> ssoBuildLogonInterface(
            JsonSessionState session) {
        Map<String, Map<String, String>> ssoInterface = new LinkedHashMap<String, Map<String, String>>();
        for (String ssoId : sso.keySet()) {
            SSOInterface provider = sso.get(ssoId);
            Map<String, String> map = new HashMap<String, String>();
            map.put("label", provider.getLabel());
            map.put("interface",
                    provider.getInterface(ssoLoginUrl + "?ssoId=" + ssoId));
            ssoInterface.put(ssoId, map);
        }
        return ssoInterface;
    }

    /**
     * Retrieve the login URL for redirection against a given provider.
     * 
     * @param String The SSO source to use
     * @return String The URL used by the SSO Service for logins
     */
    public String ssoGetRemoteLogoutURL(JsonSessionState session, String source) {
        if (!sso.containsKey(source)) {
            return null;
        } else {
            // return sso.get(source).ssoGetRemoteLogonURL(session);
            Class ssoInterfaceClass = sso.get(source).getClass();
            Method remoteLogoutURLMethod;
            try {
                remoteLogoutURLMethod = ssoInterfaceClass.getMethod(
                        "ssoGetRemoteLogoutURL", JsonSessionState.class);
                if (remoteLogoutURLMethod != null) {
                    return (String) remoteLogoutURLMethod.invoke(
                            sso.get(source), session);
                }
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }
    }

    /**
     * Retrieve the login URL for redirection against a given provider.
     * 
     * @param String The SSO source to use
     * @return String The URL used by the SSO Service for logins
     */
    @Override
    public String ssoGetRemoteLogonURL(JsonSessionState session, String source) {
        if (!sso.containsKey(source)) {
            return null;
        } else {
            return sso.get(source).ssoGetRemoteLogonURL(session);
        }
    }

    /**
     * Given the provided resource, test whether SSO should be 'aware' of this
     * resource. 'Aware' resources are valid return return points after SSO
     * redirects, so the test should return false on (for examples) static
     * resources and utilities such as atom feeds.
     * 
     * @param session : The session for this request
     * @param resource : The name of the resource being accessed
     * @param uri : The full URI of the resource if simple matches fail
     * @return boolean : True if SSO should be evaluated, False otherwise
     */
    @Override
    public boolean testForSso(JsonSessionState session, String resource,
            String uri) {
        // The URL parameters can request forced SSO to this URL
        String ssoId = request.getParameter("ssoId");
        if (ssoId != null) {
            return true;
        }

        // The URL parameters can contain a trust token
        String utoken = request.getParameter("token");
        String stoken = (String) session.get("validToken");
        if (utoken != null || stoken != null) {
            return true;
        }

        // Test for resources that start with unwanted values
        for (String test : excStarts) {
            if (resource.startsWith(test)) {
                return false;
            }
        }

        // Test for resources that end with unwanted values
        for (String test : excEnds) {
            if (resource.endsWith(test)) {
                return false;
            }
        }

        // Test for resources that equal unwanted values
        for (String test : excEquals) {
            if (resource.equals(test)) {
                return false;
            }
        }

        // Detail screen - specific payload target
        // This is an edge case, where the payload was a deep link,
        // it's not a subpage we can ignore
        String returnAddress = (String) session.get("returnAddress");
        if (returnAddress != null && returnAddress.endsWith(uri)) {
            return true;
        }

        // The detail screen generates a lot of background calls to the server
        if (resource.equals("detail") || resource.equals("download")
                || resource.equals("preview")) {
            // Now check for the core page
            if (resource.equals("detail")) {
                if (detailPattern == null) {
                    detailPattern = Pattern.compile("detail/\\w+/*$");
                }
                Matcher matcher = detailPattern.matcher(uri);
                if (matcher.find()) {
                    // This is actually the 'core' detail page
                    return true;
                }
            }

            // This is just a subpage
            return false;
        }

        // Every other page
        return true;
    }

    /**
     * Validate the provided trust token.
     * 
     * @param token : The token to validate
     * @return boolean : True if the token is valid, False otherwise
     */
    @Override
    public boolean testTrustToken(JsonSessionState session, String token) {
        String[] parts = StringUtils.split(token, ":");

        // Check the length
        if (parts.length != 4) {
            log.error("TOKEN: Should have 4 parts, not {} : '{}'",
                    parts.length, token);
            return false;
        }

        // Check the parts
        String username = parts[0];
        String timestamp = parts[1];
        String publicKey = parts[2];
        String userToken = parts[3];
        if (username.isEmpty() || timestamp.isEmpty() || publicKey.isEmpty()
                || userToken.isEmpty()) {
            log.error("TOKEN: One or more parts are empty : '{}'", token);
            return false;
        }

        // Make sure the publicKey is valid
        if (!tokens.containsKey(publicKey)) {
            log.error("TOKEN: Invalid public key : '{}'", publicKey);
            return false;
        }
        String privateKey = tokens.get(publicKey);
        Long expiry = tokenExpiry.get(publicKey);

        // Check for valid timestamp & expiry
        timestamp = getFormattedTime(timestamp);
        if (timestamp == null) {
            log.error("TOKEN: Invalid timestamp : '{}'", timestamp);
            return false;
        }
        Long tokenTime = Long.valueOf(timestamp);
        Long currentTime = Long.valueOf(getFormattedTime(null));
        Long age = currentTime - tokenTime;
        if (age > expiry) {
            log.error("Token is passed its expiry : {}s old", age);
            return false;
        }

        // Now validate the token itself
        String tokenSeed = username + ":" + timestamp + ":" + privateKey;
        String expectedToken = DigestUtils.md5Hex(tokenSeed);
        if (userToken.equals(expectedToken)) {
            // The token is valid
            session.set("username", username);
            session.set("source", TRUST_TOKEN_PREFIX + publicKey);
            // Store it in case we redirect later
            session.set("validToken", token);
            return true;
        }

        // Token was not valid
        log.error("TOKEN: Invalid token, hash does not match: '{}'", userToken);
        return false;
    }

    /**
     * Get (or validate) a formatted time string. If the input is null, the
     * current time will be returned, otherwise it will validate the provided
     * string, returning null if it is invalid.
     * 
     * @param input : A time string to validate, null will use current time
     * @return String : A formatted time string, null if input is invalid
     */
    private String getFormattedTime(String input) {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date dateData;
        if (input == null) {
            // Now
            dateData = new Date();
        } else {
            try {
                // Parse provided date
                dateData = dateFormat.parse(input);
            } catch (ParseException ex) {
                // Invalid date provided
                return null;
            }
        }

        // Return a long containing the time
        return dateFormat.format(dateData);
    }
}
