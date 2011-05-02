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
package au.edu.usq.fascinator.portal.services;

import au.edu.usq.fascinator.api.access.AccessControlManager;
import au.edu.usq.fascinator.api.authentication.AuthenticationException;
import au.edu.usq.fascinator.api.authentication.AuthManager;
import au.edu.usq.fascinator.api.authentication.User;
import au.edu.usq.fascinator.api.roles.RolesManager;
import au.edu.usq.fascinator.portal.FormData;
import au.edu.usq.fascinator.portal.JsonSessionState;
import java.util.Map;

/**
 * The security manager coordinates access to various security plugins
 * when cross plugin awareness is required, and executes some server side
 * logic required for features such as single sign-on.
 *
 * @author Greg Pendlebury
 */
public interface PortalSecurityManager {

    /**
     * Return the Access Control Manager
     *
     * @return AccessControlManager
     */
    public AccessControlManager getAccessControlManager();

    /**
     * Return the Authentication Manager
     *
     * @return AuthManager
     */
    public AuthManager getAuthManager();

    /**
     * Return the Role Manager
     *
     * @return RolesManager
     */
    public RolesManager getRoleManager();

    /**
     * Get the list of roles possessed by the current user.
     *
     * @param user The user object of the current user
     * @return String[] A list of roles
     */
    public String[] getRolesList(JsonSessionState session, User user);

    /**
     * Retrieve the details of a user by username
     *
     * @param username The username of a user to retrieve
     * @param source The authentication source if known
     * @return User The user requested
     * @throws AuthenticationException if any errors occur
     */
    public User getUser(JsonSessionState session, String username,
            String source) throws AuthenticationException;

    /**
     * Logout the provided user
     *
     * @return user The user to logout
     */
    public void logout(JsonSessionState session, User user)
            throws AuthenticationException;

    /**
     * Wrapper method for other SSO methods provided by the security manager.
     * If desired, the security manager can take care of the integration using
     * the default usage pattern, rather then calling them individually.
     *
     * @param session : The session of the current request
     * @param formData : FormData object for the current request
     * @return boolean : True if SSO has redirected, in which case no response
     *      should be sent by Dispatch, otherwise False.
     */
    public boolean runSsoIntegration(JsonSessionState session,
            FormData formData);

    /**
     * Initialize the SSO Service, prepare a login if required
     *
     * @param session The server session data
     * @throws Exception if any errors occur
     */
    public String ssoInit(JsonSessionState session) throws Exception;

    /**
     * Retrieve the login URL for redirection against a given provider.
     *
     * @param String The SSO source to use
     * @return String The URL used by the SSO Service for logins
     */
    public String ssoGetRemoteLogonURL(JsonSessionState session, String source);

    /**
     * Get user details from SSO connection and set them in the user session.
     *
     * @return boolean: Flag whether a user was actually logged in or not.
     */
    public boolean ssoCheckUserDetails(JsonSessionState session);

    /**
     * Build a Map of Maps of on-screen string values for each SSO provider.
     * Should be enough to generate a login interface.
     *
     * @return Map Containing the data structure of valid SSO interfaces.
     */
    public Map<String, Map<String, String>> ssoBuildLogonInterface(JsonSessionState session);

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
    public boolean testForSso(JsonSessionState session, String resource,
            String uri);

    /**
     * Validate the provided trust token.
     *
     * @param token : The token to validate
     * @return boolean : True if the token is valid, False otherwise
     */
    public boolean testTrustToken(JsonSessionState session, String token);
}
