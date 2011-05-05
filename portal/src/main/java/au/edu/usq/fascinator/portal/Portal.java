/* 
 * The Fascinator - Portal
 * Copyright (C) 2008-2011 University of Southern Queensland
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
package au.edu.usq.fascinator.portal;

import au.edu.usq.fascinator.common.JsonObject;
import au.edu.usq.fascinator.common.JsonSimple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Portal configuration
 * 
 * @author Linda Octalina
 */
public class Portal extends JsonSimple {

    public static final String PORTAL_JSON = "portal.json";

    private Logger log = LoggerFactory.getLogger(Portal.class);

    /**
     * Construct a portal instance with the specified name
     */
    public Portal(String portalName) throws IOException {
        setName(portalName);
    }

    /**
     * Construct a portal instance for the specified JSON file
     * 
     * @throws IOException if there was an error reading the JSON file
     */
    public Portal(File portalConfig) throws IOException {
        super(portalConfig);
    }

    public String getName() {
        return getString("undefined", "portal", "name");
    }

    public void setName(String name) {
        JsonObject portal = writeObject("portal");
        portal.put("name", name.replace(' ', '_'));
    }

    public String getDescription() {
        return getString("Undefined", "portal", "description");
    }

    public void setDescription(String description) {
        JsonObject portal = writeObject("portal");
        portal.put("description", description);
    }

    public String getQuery() {
        return getString("", "portal", "query");
    }

    public void setQuery(String query) {
        JsonObject portal = writeObject("portal");
        portal.put("query", query);
    }

    public String getSearchQuery() {
        return getString("", "portal", "searchQuery");
    }

    public void setSearchQuery(String query) {
        JsonObject portal = writeObject("portal");
        portal.put("searchQuery", query);
    }

    public int getRecordsPerPage() {
        return getInteger(10, "portal", "records-per-page");
    }

    public void setRecordsPerPage(int recordsPerPage) {
        JsonObject portal = writeObject("portal");
        portal.put("records-per-page", recordsPerPage);
    }

    public int getFacetCount() {
        return getInteger(25, "portal", "facet-count");
    }

    public void setFacetCount(int facetCount) {
        JsonObject portal = writeObject("portal");
        portal.put("facet-count", facetCount);
    }

    public boolean getFacetSort() {
        return getBoolean(false, "portal", "facet-sort-by-count");
    }

    public void setFacetSort(boolean facetSort) {
        JsonObject portal = writeObject("portal");
        portal.put("facet-sort-by-count", facetSort);
    }

    public Map<String, JsonSimple> getFacetFields() {
        return JsonSimple.toJavaMap(getObject("portal", "facet-fields"));
    }

    public void setFacetFields(Map<String, JsonSimple> map) {
        JsonObject facets = writeObject("portal", "facet-fields");
        for (String key : map.keySet()) {
            facets.put(key, map.get(key).getJsonObject());
        }
    }

    public List<String> getFacetFieldList() {
        return new ArrayList<String>(getFacetFields().keySet());
    }

    public int getFacetDisplay() {
        return getInteger(10, "portal", "facet-display");
    }

    public void setFacetDisplay(int facetDisplay) {
        JsonObject portal = writeObject("portal");
        portal.put("facet-display", facetDisplay);
    }

    public Map<String, String> getSortFields() {
        JsonObject object = getObject("portal", "sort-fields");
        Map<String, String> sortFields = new LinkedHashMap<String, String>();
        if (object == null) {
            sortFields.put("last_modified", "Last modified");
            sortFields.put("f_dc_title", "Title");
        } else {
            for (Object key : object.keySet()) {
                sortFields.put((String) key, (String) object.get(key));
            }
        }
        return sortFields;
    }

    public void setSortFields(Map<String, String> sortFields) {
        JsonObject object = getObject("portal", "sort-fields");
        for (String key : sortFields.keySet()) {
            object.put(key, sortFields.get(key));
        }
    }

    public List<String> getSortFieldList() {
        return new ArrayList<String>(getSortFields().keySet());
    }

    public String getSortFieldDefault() {
        return getString("", "portal", "sort-field-default");
    }

    public void setSortFieldDefault(String sortFieldDefault) {
        JsonObject portal = writeObject("portal");
        portal.put("sort-field-default", sortFieldDefault);
    }

    public String getSortFieldDefaultOrder() {
        return getString("", "portal", "sort-field-default-order");
    }

    public void setSortFieldDefaultOrder(String sortFieldDefaultOrder) {
        JsonObject portal = writeObject("portal");
        portal.put("sort-field-default-order", sortFieldDefaultOrder);
    }
}
