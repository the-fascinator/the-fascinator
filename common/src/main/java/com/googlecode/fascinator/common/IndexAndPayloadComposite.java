/* 
 * Copyright (C) 2013 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
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
package com.googlecode.fascinator.common;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;

import com.googlecode.fascinator.common.solr.SolrDoc;

/**
 * Combines indexed and payload data to abstract data access in scripts, etc.
 * 
 * @author Shilo Banihit
 * 
 */
public class IndexAndPayloadComposite {

    private SolrDoc indexedData;
    private JsonSimple payloadData;

    /**
     * Builds an instance using the indexed data, accessing the storage for the
     * appropriate payload specified in
     * 
     * @param indexedData
     * @param services
     */
    public IndexAndPayloadComposite(SolrDoc indexedData, JsonSimple payloadData) {
        this.indexedData = indexedData;
        this.payloadData = payloadData;
    }

    public Object getPath(String field) {
        Object object = null;
        if (payloadData != null) {
            object = payloadData.getPath(field);
        }
        if (object == null) {
            object = indexedData.getPath(field);
        }
        return object;
    }

    /**
     * Gets the String value of the specified field.
     * 
     * <ul>
     * <li>Fields that are not found will always return null.</li>
     * <li>Single valued fields will return the value found.</li>
     * <li>Multi valued fields return all concatenated values (bracketed and
     * quoted) if more then one entry is found, otherwise it will be simply the
     * first entry.</li>
     * </ul>
     * 
     * @param field : The field name to query
     * @return String : The value found, possibly null
     */
    public String get(String field) {
        Object object = getPath(field);

        // giving up
        if (object == null) {
            return null;
        }

        // This node is an array
        if (object instanceof JSONArray) {
            List<String> array = JsonSimple.getStringList((JSONArray) object);
            if (array.size() == 1) {
                return array.get(0);
            } else {
                return "[\"" + StringUtils.join(array, "\", \"") + "\"]";
            }
        }

        // Much simpler
        if (object instanceof String) {
            return (String) object;
        }

        // Some fields can be float/double such as "score"
        if (object instanceof Double) {
            return Double.toString((Double) object);
        }

        // Shouldn't occur in a valid Solr response
        return null;
    }

    /**
     * Gets the first String value of the specified field.
     * 
     * <ul>
     * <li>Fields that are not found will always return null.</li>
     * <li>Single valued fields will return the value found.</li>
     * <li>Multi valued fields return the String value of the first entry.</li>
     * </ul>
     * 
     * @param field : The field name to query
     * @return String : The value found, possibly null
     */
    public String getFirst(String field) {
        Object object = getPath(field);

        // Doesn't exist
        if (object == null) {
            return null;
        }

        // This node is an array
        if (object instanceof JSONArray) {
            List<String> array = JsonSimple.getStringList((JSONArray) object);
            Object first = array.get(0);
            if (first instanceof String) {
                return (String) first;
            }
        }

        // Much simpler
        if (object instanceof String) {
            return (String) object;
        }

        // Shouldn't occur in a valid Solr response
        return null;
    }

    /**
     * Gets the list of String values of the specified field.
     * 
     * <ul>
     * <li>Single valued fields will be added to a new List.</li>
     * <li>Multi valued fields will all be returned in a List.</li>
     * <li>Fields that are not found will return an empty list.</li>
     * </ul>
     * 
     * @param field : The field name to query
     * @return List<String> : The List of values found, possibly null
     */
    public List<String> getList(String field) {
        Object object = getPath(field);

        // Doesn't exist
        if (object == null) {
            return new LinkedList<String>();
        }

        // An array = easy
        if (object instanceof JSONArray) {
            return JsonSimple.getStringList((JSONArray) object);
        }

        // String, create a new list
        if (object instanceof String) {
            List<String> response = new LinkedList<String>();
            response.add((String) object);
            return response;
        }

        // Shouldn't occur in a valid Solr response
        return null;
    }

    public JsonObject getJsonObject() {
        if (payloadData != null) {
            return payloadData.getJsonObject();
        }
        return indexedData.getJsonObject();
    }
}
