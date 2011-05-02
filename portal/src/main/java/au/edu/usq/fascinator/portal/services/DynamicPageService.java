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
package au.edu.usq.fascinator.portal.services;

import au.edu.usq.fascinator.common.solr.SolrDoc;
import au.edu.usq.fascinator.portal.FormData;
import au.edu.usq.fascinator.portal.JsonSessionState;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.velocity.context.Context;

public interface DynamicPageService {

    @Deprecated
    public InputStream getResource(String resourcePath);

    @Deprecated
    public InputStream getResource(String portalId, String resourceName);

    @Deprecated
    public String resourceExists(String portalId, String resourceName);

    public String render(String portalId, String pageName, OutputStream out,
            FormData formData, JsonSessionState sessionState);

    public String renderObject(Context context, String template,
            SolrDoc metadata);
}
