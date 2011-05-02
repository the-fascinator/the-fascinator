/* 
 * The Fascinator - Solr Portal
 * Copyright (C) 2008  University of Southern Queensland
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.services.Response;

public class HttpStatusCodeResponse implements StreamResponse {

    private int sc;

    private String message;

    public HttpStatusCodeResponse(int sc, String message) {
        this.sc = sc;
        this.message = message;
    }

    public String getContentType() {
        return "text/html";
    }

    public InputStream getStream() throws IOException {
        return new ByteArrayInputStream("".getBytes());
    }

    public void prepareResponse(Response response) {
        try {
            response.sendError(sc, message);
        } catch (IOException ioe) {
        }
    }
}
