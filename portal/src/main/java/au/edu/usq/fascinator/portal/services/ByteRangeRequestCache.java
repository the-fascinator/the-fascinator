/*
 * The Fascinator - Portal - Byte Range Request Cache
 * Copyright (C) 2010 University of Southern Queensland
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

import au.edu.usq.fascinator.api.storage.Payload;

import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;

/**
 * Contains the logic required to parse, process and respond to HTTP byte-range
 * requests.
 *
 * @author Greg Pendlebury
 */
public interface ByteRangeRequestCache {

    /**
     * Access the provided HTTP request for a byte range header and return data
     * from indicated object payload via the provided response object.
     *
     * @param request: The incoming HTTP request
     * @param response: The response object waiting for return data
     * @param payload: The payload in the object
     * @return boolean: True if the request was processed, otherwise False
     * and the request was ignored.
     */
    public boolean processRequest(Request request, Response response,
            Payload payload);
}
