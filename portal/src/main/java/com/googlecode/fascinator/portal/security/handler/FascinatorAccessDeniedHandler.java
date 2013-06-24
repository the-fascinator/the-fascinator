package com.googlecode.fascinator.portal.security.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.googlecode.fascinator.common.JsonSimpleConfig;

/**
 * Access Denied Handler that redirects a user to the Access Denied page in the
 * correct portal
 * 
 * @author andrewbrazzatti
 * 
 */
public class FascinatorAccessDeniedHandler implements AccessDeniedHandler {
    private String urlBase = null;

    public FascinatorAccessDeniedHandler() throws IOException {
        JsonSimpleConfig config = new JsonSimpleConfig();
        urlBase = config.getString(null, "urlBase");
    }

    @Override
    public void handle(HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException,
            ServletException {
        String path = request.getServletPath();
        path = path.substring(1, path.length());
        String portal = path.substring(0, path.indexOf("/"));
        response.sendRedirect(urlBase + portal + "/accessDenied");

    }
}
