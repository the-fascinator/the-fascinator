/* 
 * The Fascinator - Plugin - Harvester - JSON Queue
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
package au.edu.usq.fascinator.harvester.jsonq;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.common.storage.impl.GenericPayload;

/**
 * JsonQ Metadata payload
 * 
 * @author Oliver Lucido
 */
public class JsonQMetadataPayload extends GenericPayload {
    public JsonQMetadataPayload(String id) {
        super(id);
        // TODO Auto-generated constructor stub
    }

    private Logger log = LoggerFactory.getLogger(JsonQMetadataPayload.class);

    /** File state information */
    private Map<String, String> info;

    /**
     * Creates a payload for a file with state changed information from the
     * Watcher service
     * 
     * @param file file content
     * @param info state information
     */
    // public JsonQMetadataPayload(File file, Map<String, String> info) {
    // this.info = info;
    // info.put("uri", file.getAbsolutePath());
    // setId(file.getName() + ".properties");
    // setLabel("File State Metadata");
    // setContentType("text/plain");
    // setType(PayloadType.Annotation);
    // }

    // @Override
    // public InputStream getInputStream() throws IOException {
    // ByteArrayOutputStream out = new ByteArrayOutputStream();
    // Properties props = new Properties();
    // try {
    // // Currently this properties are not used
    // // Maybe we should try to index event time
    // Object t = info.get("time");
    // props.setProperty("uri", info.get("uri"));
    // props.setProperty("state", info.get("state"));
    // props.setProperty("time", t.toString());
    // props.store(out, "File Metadata");
    // return new ByteArrayInputStream(out.toByteArray());
    // } catch (IOException ioe) {
    // throw (ioe);
    // }
    // }
}
