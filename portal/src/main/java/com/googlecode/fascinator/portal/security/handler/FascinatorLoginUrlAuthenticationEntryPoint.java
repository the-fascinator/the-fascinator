package com.googlecode.fascinator.portal.security.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * A subclass of LoginUrlAuthenticationEntryPoint that will redirect the user to
 * the login page in the correct portal
 * 
 * @author andrewbrazzatti
 * 
 */
public class FascinatorLoginUrlAuthenticationEntryPoint extends
        LoginUrlAuthenticationEntryPoint {

    @Override
    protected String determineUrlToUseForThisRequest(
            HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) {
        String path = request.getServletPath();
        path = path.substring(1, path.length());
        String portal = path.substring(0, path.indexOf("/"));
        String redirectPath = path.substring(path.indexOf("/") + 1,
                path.length());
        return "/" + portal + "/login?fromUrl=" + redirectPath;
    }
}
