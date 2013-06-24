package com.googlecode.fascinator.portal.security.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.googlecode.fascinator.common.authentication.GenericUser;
import com.googlecode.fascinator.portal.JsonSessionState;

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

    public void setAuthManager(AuthenticationManager authManager) {
        this.authManager = authManager;
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
                    GenericUser user = new GenericUser();
                    user.setUsername((String) jsonSessionState.get("username"));
                    user.setSource((String) jsonSessionState.get("source"));
                    token.setDetails(user);
                }
            } else if (jsonSessionState.get("username") != null
                    && !authentication.getName().equals(
                            jsonSessionState.get("username"))) {
                token = new PreAuthenticatedAuthenticationToken(
                        jsonSessionState.get("username"), "password");
                GenericUser user = new GenericUser();
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
        filterChain.doFilter(request, response);

    }
}
