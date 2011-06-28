/* 
 * The Fascinator - Plugin API
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
package com.googlecode.fascinator.api.indexer;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generic search request
 * 
 * @author Oliver Lucido
 */
public class SearchRequest {

    /** Search query */
    public String query;

    /** Search parameters */
    public Map<String, Set<String>> params;

    /**
     * Creates an empty search request
     */
    public SearchRequest() {
    }

    /**
     * Creates a search request with the specified query
     * 
     * @param query a query
     */
    public SearchRequest(String query) {
        this.query = query;
    }

    /**
     * Gets the search query
     * 
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Gets the first value for the specified parameter
     * 
     * @param name parameter key
     * @return parameter value
     */
    public String getParam(String name) {
        Set<String> values = getParams(name);
        if (values != null && !values.isEmpty()) {
            return values.iterator().next();
        }
        return null;
    }

    /**
     * Gets the values for the specified parameter
     * 
     * @param name parameter key
     * @return parameter values
     */
    public Set<String> getParams(String name) {
        return getParamsMap().get(name);
    }

    /**
     * Adds a parameter value
     * 
     * @param name parameter key
     * @param value parameter value
     */
    public void addParam(String name, String value) {
        Set<String> values = getParams(name);
        if (values == null) {
            setParam(name, value);
        } else {
            values.add(value);
        }
    }

    /**
     * Sets the value for the specified parameter. Note this the parameter is
     * still multi-valued, this is convenience method to wrap the value as an
     * array with one item.
     * 
     * @param name parameter key
     * @param value parameter value
     */
    public void setParam(String name, String value) {
        Set<String> values = new LinkedHashSet<String>();
        values.add(value);
        getParamsMap().put(name, values);
    }

    /**
     * Sets the values for the specified parameter
     * 
     * @param name parameter key
     * @param values parameter values
     */
    public void setParam(String name, Set<String> values) {
        getParamsMap().put(name, values);
    }

    /**
     * Sets the values for the specified parameter
     * 
     * @param name parameter key
     * @param values parameter values
     */
    public void setParam(String name, Collection<String> values) {
        getParamsMap().put(name, new LinkedHashSet<String>(values));
    }

    /**
     * Removes the specified parameter
     * 
     * @param name parameter key
     */
    public void removeParam(String name) {
        getParamsMap().remove(name);
    }

    /**
     * Removes the specified value from a parameter
     * 
     * @param name parameter key
     * @param value parameter value
     */
    public void removeParam(String name, String value) {
        getParams(name).remove(value);
    }

    /**
     * Gets all the parameters
     * 
     * @return parameter map
     */
    public Map<String, Set<String>> getParamsMap() {
        if (params == null) {
            params = new HashMap<String, Set<String>>();
        }
        return params;
    }

    /**
     * Convert search request to string
     * 
     * @return converted string
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("SearchRequest[query: ");
        str.append(query);
        if (!getParamsMap().isEmpty()) {
            str.append(", params: ");
            str.append(getParamsMap().toString());
        }
        str.append("]");
        return str.toString();
    }
}
