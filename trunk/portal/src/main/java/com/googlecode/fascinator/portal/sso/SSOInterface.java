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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * An interface defining what methods SSO implementations should expose to
 * The Fascinator. Modeled off (and valid for) OpenID and USQSSO.
 *
 * @author Greg Pendlebury
 */
public interface SSOInterface {

    /**
     * Return the ID of a given SSO implementation. Used with the
     * ServiceLoader for instantiation.
     *
     * @return String The SSO implementation ID.
     */
    public String getId();

    /**
     * Return the on-screen label to describing this implementation.
     *
     * @return String The SSO implementation label.
     */
    public String getLabel();

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
    public String getInterface(String ssoUrl);

    /**
     * Get a list of roles possessed by the current user if the SSO provider
     * supports such.
     *
     * @return List<String> A list of roles.
     */
    public List<String> getRolesList(JsonSessionState session);

    /**
     * Get the current user details in a User object.
     *
     * @return User A user object containing the current user.
     */
    public User getUserObject(JsonSessionState session);

    /**
     * Logout the current user, if implementing providers do not support this
     * at least clear Fascinator session data regarding this user.
     *
     */
    public void logout(JsonSessionState session);

    /**
     * Initialize the SSO Service
     *
     * @param session The server session data
     * @param request The incoming servlet request
     * @throws Exception if any errors occur
     */
    public void ssoInit(JsonSessionState session, HttpServletRequest request)
            throws Exception;

    /**
     * Get user details and set them in the user session. Some implementations
     * may establish a provider connection to get these, others will already
     * have them from a callback.
     *
     */
    public void ssoCheckUserDetails(JsonSessionState session);

    /**
     * Retrieve the login URL for redirection.
     *
     * @return String The URL used by the SSO Service for logins
     */
    public String ssoGetRemoteLogonURL(JsonSessionState session);

    /**
     * Prepare a login with the SSO provider
     *
     * @param returnAddress The return address after login
     * @param server The server domain
     * @throws Exception if any errors occur
     */
    public void ssoPrepareLogin(JsonSessionState session, String returnAddress,
            String server) throws Exception;
}
