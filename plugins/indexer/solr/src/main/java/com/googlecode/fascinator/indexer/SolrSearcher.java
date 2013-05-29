/* 
 * The Fascinator - Solr Indexer Plugin
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
package com.googlecode.fascinator.indexer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.common.BasicHttpClient;

public class SolrSearcher {

    public static String QUERY_ALL = "*:*";

    private Logger log = LoggerFactory.getLogger(SolrSearcher.class);

    private BasicHttpClient client;

    private String baseUrl;

    public SolrSearcher(String solrBaseUrl) {
        baseUrl = solrBaseUrl;
        client = new BasicHttpClient(solrBaseUrl);
    }

    public void authenticate(String username, String password) {
        client.authenticate(username, password);
    }

    public InputStream get() throws IOException {
        return get(null);
    }

    public InputStream get(String query) throws IOException {
        return get(query, true);
    }

    public InputStream get(String query, boolean escape) throws IOException {
        return get(query, null, escape);
    }

    public InputStream get(String query, Map<String, Set<String>> extras,
            boolean escape) throws IOException {
        if (query == null) {
            query = "*:*";
        } else if (!QUERY_ALL.equals(query) && escape) {
            query = query.replaceAll(":", "\\\\:");
        }
        String selectUrl = baseUrl + "/select";
        NameValuePair[] postData = getPostData(query, extras);
        log.debug("URL:{}, POSTDATA:{}", selectUrl, postData);
        PostMethod method = new PostMethod(selectUrl);
        method.addRequestHeader("Content-type",
                "application/x-www-form-urlencoded; charset=UTF-8");
        method.setRequestBody(postData);
        int status = client.executeMethod(method, true);
        if (status == HttpStatus.SC_OK) {
            return method.getResponseBodyAsStream();
        }
        return null;
    }

    private NameValuePair[] getPostData(String query,
            Map<String, Set<String>> extras)
            throws UnsupportedEncodingException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("q", query));
        if (extras != null) {
            for (String key : extras.keySet()) {
                Set<String> values = extras.get(key);
                for (String value : values) {
                    params.add(new NameValuePair(key, value));
                }
            }
        }
        return params.toArray(new NameValuePair[] {});
    }
}
