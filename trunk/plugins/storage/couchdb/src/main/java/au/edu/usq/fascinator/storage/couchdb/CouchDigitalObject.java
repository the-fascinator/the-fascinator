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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.common.storage.impl.GenericDigitalObject;

public class CouchDigitalObject extends GenericDigitalObject {

    private Logger log = LoggerFactory.getLogger(CouchDigitalObject.class);

    private RestClient client;

    public CouchDigitalObject(RestClient client, String oid) {
        super(oid);
        this.client = client;
    }

    @Override
    public List<Payload> getPayloadList() {
        List<Payload> dsList = new ArrayList<Payload>();
        try {
            String jsonStr = this.client.getString(getId());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readValue(jsonStr, JsonNode.class);
            JsonNode attsNode = rootNode.get("_attachments");
            if (attsNode != null) {
                Iterator<String> iter = attsNode.getFieldNames();
                while (iter.hasNext()) {
                    String name = iter.next();
                    JsonNode attNode = attsNode.get(name);
                    String contentType = attNode.get("content_type")
                            .getTextValue();

                    CouchPayload payload = new CouchPayload(client, getId());
                    payload.setId(name);
                    payload.setLabel(name);
                    payload.setContentType(contentType);
                    dsList.add(payload);
                }
            }
            // get payload ids - attachment names

        } catch (JsonParseException e) {
            log.error("Failed parse json data for {}", getId());
        } catch (IOException eio) {
            log.error("Failed to list datastreams for {}", getId());
        }

        return dsList;
    }

}
