/*
 * The Fascinator - Solr Result object
 * Copyright (C) 2011 University of Southern Queensland
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

package com.googlecode.fascinator.common.solr;

import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;

/**
 * <p>
 * An extension of the JsonSimple class specifically to address Solr result
 * documents and shortcut common access.
 * </p>
 *
 * @author Greg Pendlebury
 */
public class SolrResult extends JsonSimple {
    private List<SolrDoc> results;
    private Map<String, SolrFacet> facets;

    /**
     * Creates SolrResult object from the provided input stream
     *
     * @param jsonIn : The input stream to read
     * @throws IOException if there was an error during creation
     */
    public SolrResult(InputStream jsonIn) throws IOException {
        super(jsonIn);
    }

    /**
     * Creates SolrResult object from the provided string
     *
     * @param jsonString : The JSON in string form
     * @throws IOException if there was an error during creation
     */
    public SolrResult(String jsonString) throws IOException {
        super(jsonString);
    }

    /**
     * Return the List of SolrFacet objects from this result set.
     *
     * @return List<SolrFacet> : The list of facets
     */
    public Map<String, SolrFacet> getFacets() {
        if (facets == null) {
            facets = new LinkedHashMap();
            JsonObject object = getObject("facet_counts", "facet_fields");
            if (object == null) {
                return null;
            }

            for (Object key : object.keySet()) {
                Object value = object.get(key);
                if (value instanceof JSONArray) {
                    facets.put((String) key,
                            new SolrFacet((String) key, (JSONArray) value));
                }
            }
        }
        return facets;
    }

    /**
     * <p>
     * Search for the indicated field in the results set and return a list
     * of the values.
     * </p>
     *
     * @param field : The field to search for
     * @return List<String> : The List of values found from this field
     */
    public List<String> getFieldList(String field) {
        List<String> response = new ArrayList();
        for (SolrDoc doc : getResults()) {
            response.add(doc.get(field));
        }
        return response;
    }

    /**
     * <p>
     * Return the number of documents found by this result set. Note that this
     * is not the same as the number of documents returned [getRows()], which
     * is the current 'page'.
     * </p>
     *
     * @return Integer : The results total for this search
     */
    public Integer getNumFound() {
        return getInteger(null, "response", "numFound");
    }

    /**
     * Return the time taken to perform this search
     *
     * @return Integer : The time spent searching
     */
    public Integer getQueryTime() {
        return getInteger(null, "responseHeader", "QTime");
    }

    /**
     * Return the list of documents from this results set wrapped in utility
     * objects.
     *
     * @return List<SolrDoc> : The list of documents returned
     */
    public List<SolrDoc> getResults() {
        if (results == null) {
            results = new LinkedList();
            JSONArray array = getArray("response", "docs");
            if (array == null) {
                return null;
            }

            for (Object object : array) {
                if (object instanceof JsonObject) {
                    results.add(new SolrDoc((JsonObject) object));
                }
            }
        }
        return results;
    }

    /**
     * <p>
     * Return the number of documents returned by this result set. Note that
     * this is not the same as the number of documents found [getNumFound()].
     * This is just the current 'page'.
     * </p>
     *
     * @return Integer : The results total for this search
     */
    public Integer getRows() {
        List<SolrDoc> rows = getResults();
        if (rows != null) {
            return rows.size();
        }
        return null;
    }

    /**
     * <p>
     * Return the index of the first row in this result set [getRows()]
     * compared to number found [getNumFound()].
     * </p>
     *
     * @return Integer : The results total for this search
     */
    public Integer getStartRow() {
        return getInteger(null, "response", "start");
    }

    /**
     * Return the status code for the search
     *
     * @return Integer : The status code
     */
    public Integer getStatus() {
        return getInteger(null, "responseHeader", "status");
    }
}
