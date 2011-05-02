/*
 * The Fascinator - Portal - Byte Range Request Cache
 * Copyright (C) 2010-2011 University of Southern Queensland
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
package au.edu.usq.fascinator.portal.services.impl;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.portal.services.ByteRangeRequestCache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the logic required to parse, process and respond to HTTP byte-range
 * requests.
 *
 * @author Greg Pendlebury
 */
public class ByteRangeRequestCacheImpl implements ByteRangeRequestCache {

    /** Byte buffering - Size was selected to match Apache Commons IOUtils */
    private static int BUFFER_SIZE = 1024 * 4;

    /** Logging */
    private Logger log = LoggerFactory.getLogger(HouseKeepingManagerImpl.class);

    /** System Configuration */
    private JsonSimpleConfig sysConfig;

    /** Storage layer */
    private Storage storage;

    /** Is the system validly configured and online */
    private boolean isValid;

    /**
     * Basic constructor, run by Tapestry through injection.
     *
     */
    public ByteRangeRequestCacheImpl() {
        isValid = true;

        // System configuration
        try {
            sysConfig = new JsonSimpleConfig();
        } catch (IOException ex) {
            isValid = false;
            log.error("Failed to access system config", ex);
            return;
        }

        // Check if we even want byte range support
        boolean configured = sysConfig.getBoolean(false,
                "portal", "byteRangeSupported");
        if (!configured) {
            log.info("Byte range support disabled by configuration!");
            isValid = false;
            return;
        }

        // Storage plugin
        try {
            String storageType = sysConfig.getString(null, "storage", "type");
            if (storageType == null) {
                log.error("Invalid system configuration. No storage type!");
            } else {
                storage = PluginManager.getStorage(storageType);
                storage.init(sysConfig.toString());
            }
        } catch (PluginException ex) {
            isValid = false;
            log.error("Failed to initialise storage", ex);
        }
    }

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
    @Override
    public boolean processRequest(Request request, Response response,
            Payload payload) {
        // Test for valid startup
        if (!isValid) {
            return false;
        }
        // Check the HTTP request for a byte range header
        String byteHeader = request.getHeader("Range");
        if (byteHeader == null) {
            //log.debug("#### No byte header");
            return false;
        }

        // Ensure storage can support our requests
        if (payload.size() == null) {
            log.error("Range request received, but storage cannot support");
            return false;
        }

        // For statistics, and learning request patterns. Take
        //  a look at the user agent from incoming requests.
        String userAgent = request.getHeader("User-Agent");

        // Break up the range request
        String[] parts = StringUtils.split(byteHeader, "=");
        if (parts.length != 2) {
            log.error("Invalid byte range request received: '{}'", byteHeader);
            log.error("User Agent: '{}'", userAgent);
            return false;
        }
        String[] requests = StringUtils.split(parts[1], ",");

        // Simple, single range requests
        if (requests.length == 1) {
            return processSingle(requests[0], userAgent, response, payload);
        }

        // TODO : For now, we do not support multi-part requests
        log.error("Non supported multi-part range request received: '{}'",
                byteHeader);
        log.error("User Agent: '{}'", userAgent);
        return false;
    }

    /**
     * Parse the provided byte range request and return the data. This method
     * is NOT for multi-part byte range requests.
     *
     * @param request: The byte range request to parse
     * @param agent: The request's user agent
     * @param response: The response object waiting for return data
     * @param payload: The payload in the object
     * @return boolean: True if the request was processed, otherwise False
     * and the request was ignored.
     */
    private boolean processSingle(String request, String agent,
            Response response, Payload payload) {
        long starts = -1;
        long ends = -1;
        long size = payload.size();

        // Parse the request
        //log.debug("Byte Request: '{}'", request);
        String[] parts = StringUtils.split(request, "-");
        if (parts.length != 2) {
            return logInvalid(request, agent);
        }
        //log.debug("Byte Request (1): '{}'", parts[0]);
        //log.debug("Byte Request (2): '{}'", parts[1]);
        if (!parts[0].equals("")) {
            starts = Long.parseLong(parts[0]);
        }
        if (!parts[1].equals("")) {
            ends = Long.parseLong(parts[1]);
        } else {
            ends = size - 1;
        }

        // Check syntax validity
        if (starts > ends || (starts == -1 && ends < 1)) {
            return logInvalid(request, agent);
        }

        // Convert relative ranges to absolute indexes
        if (starts == -1) {
            starts = (size - 1) - ends;
            ends = size - 1;
            if (starts < 0) {
                starts = 0;
            }
        }

        // Check range validity
        if (starts != -1 && starts >= size) {
            return outOfRange(request, agent, response);
        }
        if (ends == -1 || ends >= size) {
            ends = size - 1;
        }

        //log.debug("Byte Request (START): '{}'", starts);
        //log.debug("Byte Request (END)  : '{}'", ends);

        return streamResponse(starts, ends, agent, response, payload);
    }

    /**
     * Stream the byte range from the payload to the response.
     *
     * @param start: The start index of the range
     * @param end: The end index of the range
     * @param agent: The request's user agent
     * @param response: The response object waiting for return data
     * @param payload: The payload in the object
     * @return boolean: True if the request was processed, otherwise False
     * and the request was ignored.
     */
    private boolean streamResponse(long start, long end, String agent,
            Response response, Payload payload) {
        log.debug("Byte Range: {} - {}", start, end);
        log.debug("User Agent: {}", agent);

        long size = payload.size();
        long dataLength = end - start + 1;
        String rangeResponse = "bytes " + start + "-" + end + "/" + size;

        try {
            InputStream in = payload.open();
            String mimeType = payload.getContentType();
            if (mimeType == null) {
                log.warn("Unknown MIME type! Using default");
                mimeType = "application/octet-stream";
            }
            mimeType += "; name=\"" + payload.getId() + "\"";
            String disposition = "attachment; filename=\""
                    + payload.getId() + "\"";

            // 206 = Partial content
            response.setStatus(206);
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Connection", "keep-alive");
            response.setHeader("Keep-Alive", "timeout=2, max=98");
            response.setHeader("Content-Description", "File Transfer");
            response.setHeader("Content-Disposition", disposition);
            response.setHeader("Content-Transfer-Encoding", "binary");
            //log.debug("HEADER 'Content-Type': '{}'", mimeType);
            response.setHeader("Content-Type", mimeType);
            OutputStream out = response.getOutputStream(mimeType);

            // Send valid headers describing our data:  If these headers aren't
            // set before the data starts streaming some clients have been
            // known to terminate the connection early
            //log.debug("HEADER 'Content-Range': '{}'", rangeResponse);
            response.setHeader("Content-Range", rangeResponse);
            //log.debug("HEADER 'Content-Length': '{}'", dataLength);
            response.setContentLength((int) dataLength);

            byte[] buffer = new byte[BUFFER_SIZE];
            int index = 0;
            int read = 0;
            boolean success = false;
            boolean finished= false;

            try {
                int n = 0;
                while (-1 != (n = in.read(buffer)) && !finished) {
                    // Default to full data copy
                    int i = 0;
                    int len = n;
                    // Do we START somewhere in this 'chunk'?
                    if ((index < start) && ((index + n) >= start)) {
                        // Start the data copy later
                        i = (int) (start - index);
                        len -= i;
                    }
                    // Do we END somewhere in this 'chunk'?
                    if ((index <= end) && ((index + n) > end)) {
                        // End the data copy earlier
                        len = (int) (end - index - i + 1);
                        finished = true;
                    }
                    if ((index + i) >= start && (index + i + len - 1) <= end) {
                        //log.debug("Copy buffer: {} => {}", i, len);
                        out.write(buffer, i, len);
                        read += len;
                    }
                    // Update counters
                    index += n;
                }

                out.close();
                success = true;

            } catch (IOException ex) {
                if (read > 0) {
                    log.error("(!) Connection lost: {} - {} bytes of data sent",
                            read - BUFFER_SIZE, read);
                    success = true;
                } else {
                    log.error("Bytes read: {}", read);
                    log.error("Error accessing data", ex);
                    return false;
                }
            }
            //log.debug("Content Read : {} bytes", read);
            payload.close();
            return success;

        } catch (IOException ex) {
            log.error("Error accessing response output", ex);
            return false;
        } catch (StorageException ex) {
            log.error("Error accessing data", ex);
            return false;
        }
    }

    /**
     * Send an appropriate HTTP response for an out-of-range byte range request
     *
     * @param request: The byte range request
     * @param agent: The request's user agent
     * @param response: The HTTP response object
     * @return boolean: True if the request was processed, otherwise False
     * and the request was ignored.
     */
    private boolean outOfRange(String request, String agent,
            Response response) {
        log.error("Out-of-range byte range request received: '{}'", request);
        log.error("User Agent: '{}'", agent);
        response.setStatus(416);
        return true;
    }

    /**
     * Log the invalid request.
     *
     * @param request: The byte range request
     * @param agent: The request's user agent
     * @return boolean: True if the request was processed, otherwise False
     * and the request was ignored.
     */
    private boolean logInvalid(String request, String agent) {
        log.error("Invalid byte range request received: '{}'", request);
        log.error("User Agent: '{}'", agent);
        return false;
    }
}
