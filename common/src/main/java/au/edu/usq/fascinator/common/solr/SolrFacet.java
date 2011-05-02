/*
 * The Fascinator - Solr Facet object
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

package au.edu.usq.fascinator.common.solr;

import java.util.LinkedHashMap;
import java.util.Map;
import org.json.simple.JSONArray;

/**
 * <p>
 * An extension of the JsonSimple class specifically to address Solr result
 * facets and shortcut common access.
 * </p>
 *
 * @author Greg Pendlebury
 */
public class SolrFacet {
    private String fieldName;
    private Map<String, Integer> values;

    /**
     * Wrap a JSONArray in this class
     *
     * @param newJsonObject : The JSONArray to wrap
     */
    public SolrFacet(String field, JSONArray data) {
        fieldName = field;
        values = new LinkedHashMap();

        /* This code relies on the Solr response coming back in a very specific
         format. Every odd numbered entry is a String and will be the value,
         every even numbered entry is a Long and is the count. */
        try {
            String value = null;
            for (Object object : data) {
                if (object instanceof String) {
                    value = (String) object;
                } else {
                    Integer count = ((Long) object).intValue();
                    if (count != 0) {
                        values.put(value, count);
                    }
                }
            }
        } catch (Exception ex) {
            // Processing problem. The data is in some unexpected format
            values = new LinkedHashMap();
        }
    }

    /**
     * Get the count against a given facet value for this field.
     *
     * @return Integer : The required facet count
     */
    public Integer count(String value) {
        return values.get(value);
    }

    /**
     * Get the name of the facet field
     *
     * @return String : The field name of this facet
     */
    public String field() {
        return fieldName;
    }

    /**
     * Get a Map containing facet values and counts. Internal representation
     * is a LinkedHashMap to maintain original order.
     *
     * @return Map<String, Integer> : The facet data for this field
     */
    public Map<String, Integer> values() {
        return values;
    }
}
