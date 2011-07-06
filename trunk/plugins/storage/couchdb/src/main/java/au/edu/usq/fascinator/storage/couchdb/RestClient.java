/*
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
import java.io.OutputStream;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.common.BasicHttpClient;

public class RestClient extends BasicHttpClient {

    private Logger log = LoggerFactory.getLogger(RestClient.class);

    public RestClient(String baseUrl) {
        super(baseUrl);
    }

    // Access methods

    public String getString(String uri) {
        int status = 0;
        String result = null;
        uri = getBaseUrl().concat("/").concat(uri);
        GetMethod method = new GetMethod(uri);
        try {
            status = executeMethod(method);
            if (status == HttpStatus.SC_OK) {
                result = method.getResponseBodyAsString();
            }
        } catch (Exception e) {
            log.error("Failed to getString() - {}", e.getMessage());
        }
        method.releaseConnection();
        return result;
    }

    public int get(String pid, String dsId, OutputStream out)
            throws IOException {
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/get/");
        uri.append(pid);
        if (dsId != null) {
            uri.append("/");
            uri.append(dsId);
        }
        GetMethod method = new GetMethod(uri.toString());
        int status = executeMethod(method);
        if (status == HttpStatus.SC_OK) {
            InputStream in = method.getResponseBodyAsStream();
            IOUtils.copy(in, out);
            in.close();
        }
        method.releaseConnection();
        return status;
    }

    public InputStream getStream(String url) {
        // uri = getBaseUrl().concat("/").concat(uri);
        return null;
    }

    public InputStream getStream(String pid, String dsId) throws IOException {
        // uri = getBaseUrl().concat("/").concat(uri);
        StringBuilder uri = new StringBuilder(getBaseUrl());
        uri.append("/get/");
        uri.append(pid);
        if (dsId != null) {
            uri.append("/");
            uri.append(dsId);
        }
        GetMethod method = new GetMethod(uri.toString());
        int status = executeMethod(method);
        if (status == HttpStatus.SC_OK) {
            return method.getResponseBodyAsStream();
        }
        method.releaseConnection();
        return null;
    }

    public boolean delete(String uri) {
        uri = getBaseUrl().concat("/").concat(uri);
        int status = 0;
        DeleteMethod method = new DeleteMethod(uri.toString());
        try {
            status = executeMethod(method);
        } catch (IOException e) {

        }
        method.releaseConnection();
        if (status == HttpStatus.SC_OK)
            return true;
        else
            return false;
    }

    public String put(String uri, String contentType, String content) {
        String result = null;
        uri = getBaseUrl().concat("/").concat(uri);
        // log.info(" uri='{}'", uri);
        int status = 0;
        RequestEntity requestEntity = null;
        try {
            PutMethod method = new PutMethod(uri);
            try {
                requestEntity = new StringRequestEntity(content, contentType,
                        "UTF-8");
            } catch (IOException e) {
                log.error("Failed to create StringRequestEntity - {}", e
                        .getMessage());
                return result;
            }
            method.setRequestEntity(requestEntity);
            try {
                status = executeMethod(method);
                if (status == HttpStatus.SC_OK) {
                    result = method.getResponseBodyAsString();
                }
                result = method.getResponseBodyAsString();
            } catch (IOException e) {
                log.error("Failed to execute PUT - {}", e.getMessage());
            }
            method.releaseConnection();
        } catch (Exception e) {
            log.error("put() Failed - {}", e.getMessage());
        }
        return result;
    }

    public String put(String uri, String contentType, InputStream in) {
        String result = null;
        // log.info("RestClient.put(uri={}, contentType='{}'", uri,
        // contentType);
        uri = getBaseUrl().concat("/").concat(uri);
        int status = 0;
        PutMethod method = new PutMethod(uri.toString());
        method.addRequestHeader("Content-Type", contentType);
        RequestEntity requestEntity = new InputStreamRequestEntity(in);
        method.setRequestEntity(requestEntity);
        try {
            status = executeMethod(method);
            if (status == HttpStatus.SC_OK) {
            }
            result = method.getResponseBodyAsString();
        } catch (IOException e) {
            log.error("Failed to execute PUT - {}", e.getMessage());
        }
        method.releaseConnection();
        return result;
    }

    /*
     * import javax.activation.MimetypesFileTypeMap; private String
     * getMimeType(File file) { return
     * MimetypesFileTypeMap.getDefaultFileTypeMap() .getContentType(file); }
     * import eu.medsea.mimeutil.MimeUtil; private String getMimeType(String
     * fileName){ Collection mimeTypes = MimeUtil.getMimeTypes(name); return
     * mimeTypes.iterator().next().toString(); return ""; }
     */
}
