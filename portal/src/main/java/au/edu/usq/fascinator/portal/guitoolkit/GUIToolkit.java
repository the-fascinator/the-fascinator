/*
 * The Fascinator - GUI Toolkit
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
package au.edu.usq.fascinator.portal.guitoolkit;

import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.portal.services.DynamicPageService;

import java.io.IOException;
import java.util.List;

/**
 * A simple wrapper class for the various toolkit objects.
 * Responsible for instantiating them and exposing them to
 * the portal as required.
 *
 * @author Greg Pendlebury
 */
public class GUIToolkit {
    private JsonSimpleConfig sysConfig;

    /**
     * A basic constructor. Simply reads system configuration.
     *
     */
    public GUIToolkit() throws IOException {
        sysConfig = new JsonSimpleConfig();
    }

    /**
     * Instantiate and return a new GUIDisplay component.
     *
     * @param renderer : The page service to use for rendering
     * @return GUIDisplay : The new component
     *
     */
    public GUIDisplay getDisplayComponent(DynamicPageService renderer) {
        return new GUIDisplay(sysConfig, renderer);
    }

    /**
     * Instantiate and return a new GUIFileUploader component.
     *
     * @param user_roles : An array with the list of roles the current user has
     * @return GUIFileUploader : The new component
     *
     */
    public GUIFileUploader getFileUploader(List<String> user_roles) {
        return new GUIFileUploader(sysConfig, user_roles);
    }

    /**
     * Instantiate and return a new GUIFormRenderer component.
     *
     * @return GUIFormRenderer : The new component
     *
     */
    public GUIFormRenderer getFormRenderer() {
        return new GUIFormRenderer(sysConfig);
    }
}
