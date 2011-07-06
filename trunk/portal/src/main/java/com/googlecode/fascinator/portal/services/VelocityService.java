/*
 * The Fascinator - Portal
 * Copyright (C) 2008-2011 University of Southern Queensland
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
package com.googlecode.fascinator.portal.services;

import java.io.InputStream;
import java.io.Writer;

import org.apache.velocity.context.Context;

/**
 * Service to render and retrieve resources from Velocity.
 *
 * @author Oliver Lucido
 */
public interface VelocityService {

    /**
     * Gets a Velocity resource.
     * 
     * @param resourcePath a valid Velocity resource
     * @return resource stream or null if not found
     */
    public InputStream getResource(String resourcePath);

    /**
     * Gets a Velocity resource.
     *
     * @param portalId the portal to get the resource from
     * @param resourceName the resource to get
     * @return resource stream or null if not found
     */
    public InputStream getResource(String portalId, String resourceName);

    /**
     * Gets the resolved path to the specified resource.
     *
     * @param portalId the portal to get the resource from
     * @param resourceName the resource to check for
     * @return the fully resolved resourcePath if the resource exists, null
     * otherwise
     */
    public String resourceExists(String portalId, String resourceName);

    /**
     * Renders a Velocity template with the given context.
     *
     * @param portalId the portal to render the resource from
     * @param templateName the template to render
     * @param context the Velocity context to render
     * @param writer a writer to output the render result to
     * @throws Exception if an error occurred while rendering the template
     */
    public void renderTemplate(String portalId, String templateName,
            Context context, Writer writer) throws Exception;
}
