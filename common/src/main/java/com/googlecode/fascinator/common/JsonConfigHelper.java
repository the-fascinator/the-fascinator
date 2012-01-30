/* 
 * The Fascinator - Common Library
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
package com.googlecode.fascinator.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.jxpath.AbstractFactory;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.apache.commons.lang.text.StrSubstitutor;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for working with JSON configuration. Uses the JXPath library to
 * use XPath syntax to access JSON nodes.
 * 
 * @author Oliver Lucido
 */
@SuppressWarnings("unchecked")
public class JsonConfigHelper {

    /** Logging */
    @SuppressWarnings("unused")
    private Logger log = LoggerFactory.getLogger(JsonConfigHelper.class);

    /** JXPath factory for creating JSON nodes */
    private class JsonMapFactory extends AbstractFactory {
        @Override
        public boolean createObject(JXPathContext context, Pointer pointer,
                Object parent, String name, int index) {
            if (parent instanceof Map) {
                ((Map<String, Object>) parent).put(name,
                        new LinkedHashMap<String, Object>());
                return true;
            }
            return false;
        }
    }

    /** JSON root node */
    private Map<String, Object> rootNode;

    /** JXPath context */
    private JXPathContext jxPath;

    /**
     * Creates an empty JSON configuration
     */
    public JsonConfigHelper() {
        rootNode = new LinkedHashMap<String, Object>();
    }

    /**
     * Creates a JSON configuration from a map. This is normally used to create
     * an instance for a subNode returned from one of the get methods.
     * 
     * @param rootNode a JSON structured map
     */
    public JsonConfigHelper(Map<String, Object> rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Creates a JSON configuration from the specified string
     * 
     * @param jsonContent a JSON content string
     * @throws IOException if there was an error parsing or reading the content
     */
    public JsonConfigHelper(String jsonContent) throws IOException {
        rootNode = new ObjectMapper().readValue(jsonContent, Map.class);
    }

    /**
     * Creates a JSON configuration from the specified file
     * 
     * @param jsonFile a JSON file
     * @throws IOException if there was an error parsing or reading the file
     */
    public JsonConfigHelper(File jsonFile) throws IOException {
        rootNode = new ObjectMapper().readValue(jsonFile, Map.class);
    }

    /**
     * Creates a JSON configuration from the specified input stream
     * 
     * @param jsonIn a JSON stream
     * @throws IOException if there was an error parsing or reading the stream
     */
    public JsonConfigHelper(InputStream jsonIn) throws IOException {
        rootNode = new ObjectMapper().readValue(jsonIn, Map.class);
    }

    /**
     * Creates a JSON configuration from the specified reader
     * 
     * @param jsonReader a reader for a JSON file
     * @throws IOException if there was an error parsing or reading the reader
     */
    public JsonConfigHelper(Reader jsonReader) throws IOException {
        rootNode = new ObjectMapper().readValue(jsonReader, Map.class);
    }

    /**
     * Gets a JXPath context for selecting and creating JSON nodes and values
     * 
     * @return a JXPath context
     */
    private JXPathContext getJXPath() {
        if (jxPath == null) {
            jxPath = JXPathContext.newContext(rootNode);
            jxPath.setFactory(new JsonMapFactory());
            jxPath.setLenient(true);
        }
        return jxPath;
    }

    /**
     * Gets the value of the specified node
     * 
     * @param path XPath to node
     * @return node value or null if not found
     */
    public String get(String path) {
        return get(path, null);
    }

    /**
     * Gets the value of the specified node, with a specified default if the not
     * was not found
     * 
     * @param path XPath to node
     * @param defaultValue value to return if the node was not found
     * @return node value or defaultValue if not found
     */
    public String get(String path, String defaultValue) {
        Object valueNode = null;
        try {
            valueNode = getJXPath().getValue(path);
        } catch (Exception e) {
        }
        String value = valueNode == null ? defaultValue : valueNode.toString();
        return StrSubstitutor.replaceSystemProperties(value);
    }

    /**
     * Get the value of specified node, with a specified default if it's not
     * found
     * 
     * @param path
     * @param defaultValue
     * @return node value or default Value if not found WITHOUT string
     *         substitution
     */
    public String getPlainText(String path, String defaultValue) {
        Object valueNode = null;
        try {
            valueNode = getJXPath().getValue(path);
        } catch (Exception e) {
        }
        String value = valueNode == null ? defaultValue : valueNode.toString();
        return value;
    }

    /**
     * Gets values of the specified node as a list. Use this method for JSON
     * arrays.
     * 
     * @param path XPath to node
     * @return value list, possibly empty
     */
    public List<Object> getList(String path) {
        List<Object> valueList = new ArrayList<Object>();
        Iterator<?> valueIterator = getJXPath().iterate(path);
        while (valueIterator.hasNext()) {
            Object value = valueIterator.next();
            valueList.add(value instanceof String ? StrSubstitutor
                    .replaceSystemProperties(value) : value);
        }
        return valueList;
    }

    /**
     * Gets a map of the child nodes of the specified node
     * 
     * @param path XPath to node
     * @return node map, possibly empty
     */
    public Map<String, Object> getMap(String path) {
        Map<String, Object> valueMap = new LinkedHashMap<String, Object>();
        Object valueNode = getJXPath().getValue(path);
        if (valueNode instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) valueNode;
            for (String key : map.keySet()) {
                Object value = map.get(key);
                if (value instanceof String) {
                    valueMap.put(key,
                            StrSubstitutor.replaceSystemProperties(value));
                } else {
                    valueMap.put(key, value);
                }
            }
        }
        return valueMap;
    }

    /**
     * Get list of JsonConfigHelper of the specified node
     * 
     * @param path XPath to node
     * @return node list, possibly empty
     */
    public List<JsonConfigHelper> getJsonList(String path) {
        List<Object> list = getList(path);
        List<JsonConfigHelper> newList = new ArrayList<JsonConfigHelper>();
        for (Object obj : list) {
            if (obj instanceof Map) {
                newList.add(new JsonConfigHelper(((Map<String, Object>) obj)));
            }
        }
        return newList;
    }

    /**
     * Get the JSON Map of the specified node
     * 
     * @param path XPath to node
     * @return node map, possibly empty
     */
    public Map<String, JsonConfigHelper> getJsonMap(String path) {
        Map<String, JsonConfigHelper> jsonMap = new LinkedHashMap<String, JsonConfigHelper>();

        Object valueNode;
        try {
            valueNode = getJXPath().getValue(path);
        } catch (Exception e) {
            return null;
        }
        if (valueNode instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) valueNode;
            for (String key : map.keySet()) {
                jsonMap.put(
                        key,
                        new JsonConfigHelper((Map<String, Object>) map.get(key)));
            }
        }
        return jsonMap;
    }

    /**
     * Set map with its child
     * 
     * @param path XPath to node
     * @param map node Map
     */
    public void setMap(String path, Map<String, Object> map) {
        try {
            getJXPath().setValue(path, map);
        } catch (Exception e) {
            getJXPath().createPathAndSetValue(path, map);
        }
    }

    /**
     * Set Multiple nested map on the specified path
     * 
     * @param path XPath to node
     * @param json node Map
     */
    public void setMultiMap(String path, Map<String, Object> json) {
        for (String key : json.keySet()) {
            if (json.get(key) instanceof Map) {
                setMultiMap(path + "/" + key,
                        (Map<String, Object>) json.get(key));
            } else {
                getJXPath().createPathAndSetValue(path + "/" + key,
                        json.get(key));
            }
        }
    }

    /**
     * Set Map on the specified path
     * 
     * @param path XPath to node
     * @param map node Map
     */
    public void setJsonMap(String path, Map<String, JsonConfigHelper> map) {
        for (String key : map.keySet()) {
            JsonConfigHelper json = map.get(key);
            try {
                getJXPath().setValue(path + "/" + key, json.getMap("/"));
            } catch (Exception e) {
                getJXPath().createPathAndSetValue(path + "/" + key,
                        json.getMap("/"));
            }
        }
    }

    /**
     * Set Map on the specified path
     * 
     * @param path XPath to node
     * @param map node Map
     */
    public void setJsonList(String path, List<JsonConfigHelper> jsonList) {
        Object valueNode = getJXPath().getValue(path);
        List<Object> valueList = new ArrayList<Object>();
        if (valueNode == null) {
            getJXPath().createPathAndSetValue(path, valueList);
        }
        for (JsonConfigHelper json : jsonList) {
            valueList.add(json.getMap("/"));
        }
    }

    /**
     * Gets a map of the child (and the 2nd level children) nodes of the
     * specified node
     * 
     * @param path XPath to node
     * @return node map, possibly empty
     */
    public Map<String, Object> getMapWithChild(String path) {
        Map<String, Object> valueMap = new LinkedHashMap<String, Object>();
        Object valueNode = getJXPath().getValue(path);
        if (valueNode instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) valueNode;
            for (String key : map.keySet()) {
                valueMap.put(key, map.get(key));
            }
        }
        return valueMap;
    }

    /**
     * Remove specified path from json
     * 
     * @param path to node
     */
    public void removePath(String path) {
        if (get(path) != null) {
            getJXPath().removePath(path);
        }
    }

    /**
     * Sets the value of the specified node. If the node doesn't exist it is
     * created.
     * 
     * @param path XPath to node
     * @param value value to set
     */
    public void set(String path, String value) {
        try {
            getJXPath().setValue(path, value);
        } catch (Exception e) {
            getJXPath().createPathAndSetValue(path, value);
        }
    }

    /**
     * Move node from one path to another path in the JSON
     * 
     * @param source XPath to node
     * @param dest XPath to node
     */
    public void move(String source, String dest) {
        Object copyValue = getJXPath().getValue(source);
        getJXPath().removePath(source);
        try {
            getJXPath().setValue(dest, copyValue);
        } catch (Exception e) {
            getJXPath().createPathAndSetValue(dest, copyValue);
        }
    }

    /**
     * Move node to a node before the specified node
     * 
     * @param path XPath to node
     * @param refPath XPath to node
     */
    public void moveBefore(String path, String refPath) {
        Map<String, Object> newMap = new LinkedHashMap<String, Object>();
        Object node = getJXPath().getValue(path);
        Object refNode = getJXPath().getValue(refPath);
        Map<String, Object> refParent = getMap(refPath + "/..");
        for (String key : refParent.keySet()) {
            // find the reference node
            Object value = refParent.get(key);
            if (value.equals(refNode)) {
                // and insert the node to be moved before it
                Map<String, Object> parent = getMap(path + "/..");
                for (String nodeKey : parent.keySet()) {
                    if (parent.get(nodeKey).equals(node)) {
                        newMap.put(nodeKey, node);
                        getJXPath().removePath(path);
                        break;
                    }
                }
            }
            if (!value.equals(node)) {
                // insert existing nodes
                newMap.put(key, value);
            }
        }
        setMap(refPath.substring(0, refPath.lastIndexOf('/')), newMap);
    }

    /**
     * Move node to a node after the specified node
     * 
     * @param path XPath to node
     * @param refPath XPath to node
     */
    public void moveAfter(String path, String refPath) {
        Map<String, Object> newMap = new LinkedHashMap<String, Object>();
        Object node = getJXPath().getValue(path);
        Object refNode = getJXPath().getValue(refPath);
        Map<String, Object> refParent = getMap(refPath + "/..");
        for (String key : refParent.keySet()) {
            // find the reference node
            Object value = refParent.get(key);
            if (!value.equals(node)) {
                // insert existing nodes
                newMap.put(key, value);
            }
            if (value.equals(refNode)) {
                // and insert the node to be moved after it
                Map<String, Object> parent = getMap(path + "/..");
                for (String nodeKey : parent.keySet()) {
                    if (parent.get(nodeKey).equals(node)) {
                        newMap.put(nodeKey, node);
                        getJXPath().removePath(path);
                        break;
                    }
                }
            }
        }
        setMap(refPath.substring(0, refPath.lastIndexOf('/')), newMap);
    }

    /**
     * Serialises the current state of the JSON configuration to the specified
     * writer. By default this doesn't use a pretty printer.
     * 
     * @param writer a writer
     * @throws IOException if there was an error writing the configuration
     */
    public void store(Writer writer) throws IOException {
        store(writer, false);
    }

    /**
     * Serialises the current state of the JSON configuration to the specified
     * writer. The output can be set to be pretty printed if required.
     * 
     * @param writer a writer
     * @param pretty use pretty printer
     * @throws IOException if there was an error writing the configuration
     */
    public void store(Writer writer, boolean pretty) throws IOException {
        JsonGenerator generator = new JsonFactory().createJsonGenerator(writer);
        if (pretty) {
            generator.useDefaultPrettyPrinter();
        }
        new ObjectMapper().writeValue(generator, rootNode);
    }

    /**
     * Convert Json to String
     * 
     * @return Json configuration in String
     */
    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * Convert Json to String
     * 
     * @param pretty state to format the layout of the Json configuration file
     * @return Json configuration in String
     */
    public String toString(boolean pretty) {
        String json = "{}";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            store(new OutputStreamWriter(out, "UTF-8"), pretty);
            json = out.toString("UTF-8");
        } catch (IOException e) {
        }
        return json;
    }
}
