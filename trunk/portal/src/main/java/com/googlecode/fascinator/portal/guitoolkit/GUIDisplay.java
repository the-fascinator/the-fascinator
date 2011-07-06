/*
 * The Fascinator - GUI Display
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
package com.googlecode.fascinator.portal.guitoolkit;

import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.portal.FormData;
import com.googlecode.fascinator.portal.JsonSessionState;
import com.googlecode.fascinator.portal.services.DynamicPageService;

import java.io.ByteArrayOutputStream;

/**
 * Displays arbitrary content in the portal.
 * 
 * @author Greg Pendlebury
 */
public class GUIDisplay {
    private DynamicPageService pageService;

    /**
     * Constructor. Access to system configuration and a page renderer is
     * required
     *
     * @param config : System configuration
     * @param renderer : The page service to use for rendering
     */
    public GUIDisplay(JsonSimpleConfig config, DynamicPageService renderer) {
        pageService = renderer;
    }

    /**
     * Render the template requested and return the contents.
     *
     * Will never render the content with branding/layout.
     *
     * @param portalId : ID of the portal the template should be found in
     * @param template : The template required to be rendered
     * @param formData : The form data to be accessible during the render
     * @param sessionState : The session data to be accessible during the render
     * @return String : The page content
     */
    public String renderTemplate(String portalId, String template,
            FormData formData, JsonSessionState sessionState) {
        return renderTemplate(portalId, template, formData, sessionState, false);
    }

    /**
     * Render the template requested and return the contents.
     *
     * @param portalId : ID of the portal the template should be found in
     * @param template : The template required to be rendered
     * @param formData : The form data to be accessible during the render
     * @param sessionState : The session data to be accessible during the render
     * @param useLayout : Flag to indicate if the content should be wrapped in page layout
     * @return String : The page content
     */
    public String renderTemplate(String portalId, String template,
            FormData formData, JsonSessionState sessionState, boolean useLayout) {
        // Do we want to see our content wrapped in app. layout?
        if (!useLayout) {
            // If not, pretend this is an ajax call
            template += ".ajax";
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pageService.render(portalId, template, out, formData, sessionState);

        return out.toString();
    }
}
