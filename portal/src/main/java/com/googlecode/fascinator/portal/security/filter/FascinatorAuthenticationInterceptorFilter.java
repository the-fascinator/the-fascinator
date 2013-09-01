/* 
 * The Fascinator - Portal
 * Copyright (C) 2008-2009 University of Southern Queensland
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
package com.googlecode.fascinator.portal.security.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.googlecode.fascinator.common.authentication.GenericUser;
import com.googlecode.fascinator.common.authentication.SpringUser;
import com.googlecode.fascinator.portal.JsonSessionState;
import com.googlecode.fascinator.portal.services.PortalSecurityManager;

/**
 * Security Filter designed to pickup changes in the login status of a user and
 * update the Spring Security Context
 * 
 * @author andrewbrazzatti
 * 
 */
public class FascinatorAuthenticationInterceptorFilter extends
        OncePerRequestFilter {

    private AuthenticationManager authManager = null;
    private PortalSecurityManager portalSecurityManager;

    public void setAuthManager(AuthenticationManager authManager) {
        this.authManager = authManager;
    }

    public void setPortalSecurityManager(
            PortalSecurityManager portalSecurityManager) {
        this.portalSecurityManager = portalSecurityManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        JsonSessionState jsonSessionState = (JsonSessionState) request
                .getSession()
                .getAttribute(
                        "sso:com.googlecode.fascinator.portal.JsonSessionState");
        if (jsonSessionState != null) {
            PreAuthenticatedAuthenticationToken token = null;
            if (authentication == null
                    || authentication instanceof AnonymousAuthenticationToken) {
                if (jsonSessionState.get("username") != null) {
                    token = new PreAuthenticatedAuthenticationToken(
                            jsonSessionState.get("username"), "password");
                    SpringUser user = new SpringUser();
                    user.setUsername((String) jsonSessionState.get("username"));
                    user.setSource((String) jsonSessionState.get("source"));
                    token.setDetails(user);
                }
            } else if (jsonSessionState.get("username") != null
                    && !authentication.getName().equals(
                            jsonSessionState.get("username"))) {
                token = new PreAuthenticatedAuthenticationToken(
                        jsonSessionState.get("username"), "password");
                SpringUser user = new SpringUser();
                user.setUsername((String) jsonSessionState.get("username"));
                user.setSource((String) jsonSessionState.get("source"));
                token.setDetails(user);
            } else if (jsonSessionState.get("username") == null) {
                // must have logged out
                SecurityContextHolder.getContext().setAuthentication(null);
            }

            if (token != null) {
                // User has been logged in so let's create their credentials and
                // authenticate them
                authentication = authManager.authenticate(token);

                SecurityContextHolder.getContext().setAuthentication(
                        authentication);
            }
        }

        if (authentication != null
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            // SSO doesn't use a normal Roles plugin so we need to get the
            // roles again here and create a new token
            SpringUser user = (SpringUser) authentication.getCredentials();
            if (!user.isSsoRolesSet()) {
                List<GrantedAuthority> userRoles = buildRoleList(user,
                        jsonSessionState);
                user.setSsoRolesSet(true);
                authentication = new PreAuthenticatedAuthenticationToken(
                        user.getUsername(), user, userRoles);
                SecurityContextHolder.getContext().setAuthentication(
                        authentication);

            }

        }
        filterChain.doFilter(request, response);

    }

    private List<GrantedAuthority> buildRoleList(GenericUser user,
            JsonSessionState jsonSessionState) {
        List<GrantedAuthority> userRoles = new ArrayList<GrantedAuthority>();
        String[] roles = portalSecurityManager.getRolesList(jsonSessionState,
                user);
        for (String role : roles) {
            GrantedAuthority authority = new GrantedAuthorityImpl(role);
            userRoles.add(authority);
        }
        return userRoles;
    }
}
