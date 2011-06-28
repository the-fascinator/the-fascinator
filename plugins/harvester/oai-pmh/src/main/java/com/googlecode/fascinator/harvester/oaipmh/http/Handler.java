/* 
 * The Fascinator - Plugin - Harvester - OAI-PMH
 * Copyright (C) 2008-2009 University of Southern Queensland
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
package com.googlecode.fascinator.harvester.oaipmh.http;

import com.googlecode.fascinator.common.BasicHttpClient;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Date;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.httpclient.util.HttpURLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for HTTP URLs to support the Retry-After header when a 503 status
 * code is received. Uses the Commons HttpClient library to do the actual HTTP
 * request.
 * 
 * @author Oliver Lucido
 */
public class Handler extends URLStreamHandler {

    /** Default times to retry */
    private static final int DEFAULT_RETRY_COUNT = 3;

    /** Default time to wait before retrying in seconds */
    private static final int DEFAULT_RETRY_AFTER = 10;

    /** Max time to wait */
    private static final int MAX_RETRY_AFTER = 60000;

    /** Logging */
    private Logger log = LoggerFactory.getLogger(Handler.class);

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        log.debug("openConnection: {}", url);
        String uri = url.toString();
        BasicHttpClient client = new BasicHttpClient(uri);
        GetMethod getMethod = new GetMethod(uri);
        int retryCount = 0;
        int status = 0;
        long retryAfter = DEFAULT_RETRY_AFTER;
        do {
            status = client.executeMethod(getMethod);
            // check for 503 error
            if (status == HttpStatus.SC_SERVICE_UNAVAILABLE) {
                // check for Retry-After
                Header h = getMethod.getResponseHeader("Retry-After");
                if (h != null) {
                    String value = h.getValue();
                    try {
                        // check if it is a date format
                        Date retryAfterDate = DateUtil.parseDate(value);
                        retryAfter = (retryAfterDate.getTime() - System
                                .currentTimeMillis()) * 1000;
                    } catch (DateParseException dpe) {
                        // default to number of seconds
                        retryAfter = Integer.parseInt(value);
                    }
                    if (retryAfter > MAX_RETRY_AFTER) {
                        // have to wait too long so forget it
                        log.warn("Retry-After is too far in the future: {}",
                                value);
                        break;
                    } else if (retryAfter < 0) {
                        // retry immediately
                        log.warn("Retry-After is in the past: {}", value);
                    } else {
                        retryCount++;
                        try {
                            // wait for the specified time
                            log.debug("Waiting for {}s before retrying...",
                                    retryAfter);
                            Thread.sleep(retryAfter * 1000);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        } while (status == HttpStatus.SC_SERVICE_UNAVAILABLE
                && retryCount < DEFAULT_RETRY_COUNT);
        return new HttpURLConnection(getMethod, url);
    }
}
