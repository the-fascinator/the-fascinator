/*
 * The Fascinator - CouchDB storage plugin
 * Copyright (C) 2009 University of Southern Queensland
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
package au.edu.usq.fascinator.storage.couchdb;

import java.io.IOException;
import java.io.InputStream;

import au.edu.usq.fascinator.common.storage.impl.GenericPayload;

public class CouchPayload extends GenericPayload {

    private RestClient client;

    private String oid;

    public CouchPayload(RestClient client, String oid) {
        this.client = client;
        this.oid = oid;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return client.getStream(oid, getId());
    }
}
