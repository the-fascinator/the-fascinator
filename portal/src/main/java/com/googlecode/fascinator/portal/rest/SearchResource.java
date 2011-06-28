/* 
 * The Fascinator - Portal
 * Copyright (C) 2009-2011 University of Southern Queensland
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
package com.googlecode.fascinator.portal.rest;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.common.JsonSimpleConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/search")
public class SearchResource {

    private JsonSimpleConfig config;

    public SearchResource() {
        try {
            config = new JsonSimpleConfig(
                    getClass().getResourceAsStream("/config.json"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @GET
    @Produces("text/plain")
    @Path("{portal}/{query}")
    public Response get(@PathParam("id") String id,
            @PathParam("query") String query) {
        try {
            Indexer indexer = PluginManager.getIndexer(
                    config.getString(null, "indexer", "type"));
            indexer.init(JsonSimpleConfig.getSystemFile());

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            SearchRequest request = new SearchRequest(query);
            indexer.search(request, result);

            return Response.ok(result.toByteArray()).build();
        } catch (IOException ioe) {
            // TODO Auto-generated catch block
            ioe.printStackTrace();
        } catch (IndexerException ie) {
            // TODO Auto-generated catch block
            ie.printStackTrace();
        } catch (PluginException pe) {
            pe.printStackTrace();
        }
        return Response.ok().build();
    }

}
