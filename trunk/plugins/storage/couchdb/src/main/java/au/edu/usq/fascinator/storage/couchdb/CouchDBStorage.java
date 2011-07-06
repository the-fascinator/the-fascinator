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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.storage.impl.GenericPayload;

//import au.edu.usq.fascinator.storage.couchdb.RestClient;

public class CouchDBStorage implements Storage {

    private Logger log = LoggerFactory.getLogger(CouchDBStorage.class);

    private static final String DEFAULT_URL = "http://localhost:5984/fascinator";

    private RestClient client;

    @Override
    public String getId() {
        return "couchdb";
    }

    @Override
    public String getName() {
        return "CouchDB Storage Module";
    }

    @Override
    public void init(File jsonFile) throws StorageException {
        log.info("*** CouchDBStorage init() called ok! ***+");
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readValue(jsonFile, JsonNode.class);
            JsonNode storageNode = rootNode.get("storage");
            if (storageNode != null) {
                String type = storageNode.get("type").getTextValue();
                if (getId().equals(type)) {
                    JsonNode configNode = storageNode.get("config");
                    String url = configNode.get("uri").getTextValue();
                    client = new RestClient(url);
                    JsonNode usernameNode = configNode.get("username");
                    JsonNode passwordNode = configNode.get("password");
                    // if (usernameNode != null && passwordNode != null) {
                    // client.authenticate(usernameNode.getTextValue(),
                    // passwordNode.getTextValue());
                    // }
                    log.info(" uri='{}'", url);
                } else {
                    throw new StorageException(
                            "not a valid couchdb storage section.");
                }
            } else {
                log.info("No configuration defined, using defaults");
                client = new RestClient(DEFAULT_URL);
            }
        } catch (JsonParseException jpe) {
            throw new StorageException(jpe);
        } catch (IOException ioe) {
            throw new StorageException(ioe);
        }
    }

    @Override
    public void shutdown() throws StorageException {
        // Don't need to do anything
    }

    @Override
    public String addObject(DigitalObject object) throws StorageException {
        log.info("*** addObject()  object.getId()='{}'", object.getId());
        String result = null;
        String rev = null;
        String oid = object.getId();
        try {
            String id = URLEncoder.encode(oid, "UTF-8");
            rev = getRev(oid);
            String json = "{\"type\":\"digitalObject\"}";
            if (rev != null) {
                json = "{\"type\":\"digitalObject\", \"_rev\":\"" + rev + "\"}";
            }
            result = client.put(id, "text/javascript", json);
            rev = getRevFromJson(result);
            log.info(" -- rev='{}'", rev);
            for (Payload payload : object.getPayloadList()) {
                if (payload.getId().equals("renditions.zip")) {
                    File tmpFile = File.createTempFile("_renditions_", ".zip");
                    OutputStream os = new FileOutputStream(tmpFile);
                    IOUtils.copy(payload.getInputStream(), os);
                    os.close();
                    ZipFile zipFile = new ZipFile(tmpFile);
                    for (Payload pload : getPayloadsFromZippedPayload(zipFile,
                            "renditions/")) {
                        result = addPayload2(oid, pload, rev);
                        if (result != null) {
                            rev = getRevFromJson(result);
                        }
                    }
                    zipFile.close();
                    tmpFile.delete();
                } else {
                    result = addPayload2(oid, payload, rev);
                    rev = getRevFromJson(result);
                }
            }
            log.info(" - rev='{}'", rev);
        } catch (Exception e) {
            throw new StorageException("Failed to add object", e);
        }
        return object.getId();
    }

    @Override
    public void removeObject(String oid) {
        log.info("*** removeObject(oid='{}')", oid);
        if (client.delete(oid)) {

        } else {
            log.error("Failed to remove object {}", oid);
        }
    }

    @Override
    public void addPayload(String oid, Payload payload) {
        String rev = getRev(oid);
        addPayload2(oid, payload, rev);
    }

    private String addPayload2(String oid, Payload payload, String rev) {
        // log.info("*** addPayload(oid='{}', payload.getId()='{}')", oid,
        // payload.getId());
        String result = null;
        try {
            String id = URLEncoder.encode(oid, "UTF-8");
            String url = id + "/" + payload.getId();
            if (rev != null) {
                url += "?rev=" + rev;
            }
            // log.info("    url='{}', contentType='{}'", url,
            // payload.getContentType());
            if (!payload.getContentType().equals("")) {
                result = client.put(url, payload.getContentType(), payload
                        .getInputStream());
            }
        } catch (Exception e) {
            log.debug("Failed to add {} to item - {}", oid, e.getMessage());
        }
        return result;
    }

    @Override
    public void removePayload(String oid, String pid) {
        log.info("*** removePayload(oid='{}', pid='{}')", oid, pid);
        // TODO
    }

    @Override
    public DigitalObject getObject(String oid) {
        log.info("*** getObject(oid='{}')", oid);
        CouchDigitalObject object = new CouchDigitalObject(client, oid);

        return object;
    }

    @Override
    public Payload getPayload(String oid, String pid) {
        log.info("*** getPayload(oid='{}', pid='{}')", oid, pid);
        DigitalObject object = getObject(oid);
        if (object != null) {
            return object.getPayload(pid);
        }
        return null;
    }

    private List<Payload> getPayloadsFromZippedPayload(ZipFile zipFile,
            String baseName) {
        List<Payload> payloads = new ArrayList<Payload>();
        InputStream is = null;
        ZipEntry zipEntry = null;
        Enumeration zipEntries = zipFile.entries();

        // log.info("* getPayloadsFromZippedPayload()");
        try {
            zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                zipEntry = (ZipEntry) zipEntries.nextElement();
                if (!zipEntry.isDirectory()) {
                    String name = zipEntry.getName();
                    String id = baseName + name;
                    String label = id;
                    String contentType = URLConnection.getFileNameMap()
                            .getContentTypeFor(name);
                    if (contentType == null) {
                        contentType = "";
                    }
                    // log.info("  name={}, contentType={}", name, contentType);
                    GenericPayload newPayload = new GenericPayload(id, label,
                            contentType);
                    newPayload.setInputStream(zipFile.getInputStream(zipEntry));

                    newPayload.setType(PayloadType.Enrichment);
                    payloads.add(newPayload);
                }
            }
        } catch (Exception e) {
            log.error(" Error in getPayloadsFromZippedPayload - {}", e
                    .getMessage());
        }
        return payloads;
    }

    private String getRev(String oid) {
        String jsonStr = null;
        try {
            String id = URLEncoder.encode(oid, "UTF-8");
            jsonStr = client.getString(id);
        } catch (Exception e) {

        }
        return getRevFromJson(jsonStr);
    }

    private String getRevFromJson(String jsonStr) {
        String rev = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readValue(jsonStr, JsonNode.class);
            JsonNode revNode = rootNode.get("_rev");
            if (revNode == null) {
                revNode = rootNode.get("rev");
            }
            rev = revNode.getTextValue();
        } catch (Exception e) {
            log.error("Failed to get rev number from JSON data - {}", e
                    .getMessage());
        }
        return rev;
    }

    @Override
    public void init(String jsonString) throws PluginException {
        // TODO Auto-generated method stub

    }

    @Override
    public List<DigitalObject> getObjectList() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * Enumeration zipEntries = zipFile.entries(); while
     * (zipEntries.hasMoreElements()) { ZipEntry entry = (ZipEntry)
     * zipEntries.nextElement(); if (!entry.isDirectory()) { String name =
     * entry.getName(); log.info("Processing '{}'", name); try { InputStream in
     * = zipFile.getInputStream(entry); BasicPayload payload = new
     * BasicPayload(); payload.setId(name); payload.setLabel(name);
     * payload.setInputStream(in); Collection mimeTypes =
     * MimeUtil.getMimeTypes(name);
     * payload.setContentType(mimeTypes.iterator().next() .toString());
     * payload.setPayloadType(PayloadType.Enrichment);
     * 
     * 
     * 
     * 
     * ByteArrayOutputStream out = new ByteArrayOutputStream(); IOUtils.copy(in,
     * out); in.close(); setId(out.toString().trim());
     */
}
