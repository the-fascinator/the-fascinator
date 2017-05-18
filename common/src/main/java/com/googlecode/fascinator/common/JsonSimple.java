/*
 * The Fascinator - JSON Simple
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

package com.googlecode.fascinator.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.json.simple.JSONArray;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h3>Introduction</h3>
 * <p>
 * This class wraps objects and methods of the
 * <a href="http://code.google.com/p/json-simple/">JSON.simple library</a>.
 * </p>
 *
 * <h3>Purpose</h3>
 * It provides the following functionality:
 * <ul>
 *   <li>Basic retrieval methods for complicated paths / data structures.</li>
 *   <li>Typed retrieval for String, Integer and Boolean data types.</li>
 *   <li>Object retrieval from the JSON.simple API (JsonObject and JSONArray).</li>
 *   <li>Static utility methods for common operations manipulating data structures.</li>
 *   <li>The ability to use place holder string for runtime substitution with system properties.</li>
 * </ul>
 *
 * @author Greg Pendlebury
 */
public class JsonSimple {

    /** Logging */
    private static Logger log = LoggerFactory.getLogger(JsonSimple.class);

    /** Holds this object's JSON */
    private JsonObject jsonObject;
    
    /** Hold the unmodified json object as parse(String) essentially throws away the JSONArray */
    
    private JSONArray jsonArray;

    /** Flag for system property substitution */
    private boolean substitueProperties;

    /**
     * Creates an empty JSON object
     *
     * @throws IOException if there was an error during creation
     */
    public JsonSimple() {
        substitueProperties = true;
        jsonObject = new JsonObject();
    }

    /**
     * Creates a JSON object from the specified file
     *
     * @param jsonFile a JSON file
     * @throws IOException if there was an error parsing or reading the file
     */
    public JsonSimple(File jsonFile) throws IOException {
        substitueProperties = true;
        if (jsonFile == null) {
            jsonObject = new JsonObject();
        } else {
            InputStream is = new FileInputStream(jsonFile);
            String json = IOUtils.toString(is, "UTF-8");
            is.close();
            parse(json);
        }
    }

    /**
     * Creates a JSON object from the specified input stream
     *
     * @param jsonIn a JSON stream
     * @throws IOException if there was an error parsing or reading the stream
     */
    public JsonSimple(InputStream jsonIn) throws IOException {
        substitueProperties = true;
        if (jsonIn == null) {
            jsonObject = new JsonObject();
        } else {
            // Stream the data into a string
            parse(IOUtils.toString(jsonIn, "UTF-8"));
            jsonIn.close();
        }
    }

    /**
     * Creates a JSON object from the specified string
     *
     * @param jsonIn a JSON string
     * @throws IOException if there was an error parsing the string
     */
    public JsonSimple(String jsonString) throws IOException {
        substitueProperties = true;
        if (jsonString == null) {
            jsonObject = new JsonObject();
        } else {
            parse(jsonString);
        }
    }

    /**
     * Wrap a JsonObject in this class
     *
     * @param newJsonObject : The JsonObject to wrap
     */
    public JsonSimple(JsonObject newJsonObject) {
        substitueProperties = true;
        if (newJsonObject == null) {
            newJsonObject = new JsonObject();
        }
        jsonObject = newJsonObject;
    }

    /**
     * Parse the provided JSON
     *
     * @param jsonString a JSON string
     * @throws IOException if there was an error parsing the string
     */
    private void parse(String jsonString) throws IOException {
        JSONParser parser = new JSONParser();
        ContainerFactory containerFactory = new ContainerFactory() {
            @Override
            public List<?> creatArrayContainer() {
                return new JSONArray();
            }
            @Override
            public Map<?, ?> createObjectContainer() {
                return new JsonObject();
            }
        };

        // Parse the String
        Object object;
        try {
            object = parser.parse(jsonString, containerFactory);
        } catch(ParseException pe) {
            log.error("JSON Parse Error: attempting to parse json string: {}: {}", jsonString, pe);
            throw new IOException(pe);
        }

        // Take a look at what we have now
        if (object instanceof JsonObject) {
            jsonObject = (JsonObject) object;
        } else {
            if (object instanceof JSONArray) {
                // @TODO: the reasons as to why the original JSONArray is discarded has been lost in time - will need to determine this is by design
                jsonArray = (JSONArray) object;
                jsonObject = getFromArray((JSONArray) object);
            } else {
                log.error("Expected JsonObject or at least JSONArray, but" +
                        " found neither. Please check JSON syntax: '{}'",
                        jsonString);
                jsonObject = null;
            }
        }
    }

    /**
     * <p>
     * Set method for behaviour flag on substituting system properties during
     * string retrieval.
     * </p>
     *
     * <p>
     * If set to <b>true</b> (default) strings of the form "{$system.property}"
     * will be substituted for matching system properties during retrieval.
     * </p>
     *
     * @param newFlag : The new flag value to set for this behaviour
     */
    public void setPropertySubstitution(boolean newFlag) {
        substitueProperties = newFlag;
    }

    /**
     * Find the first valid JsonObject in a JSONArray.
     *
     * @param array : The array to search
     * @return JsonObject : A JSON object
     * @throws IOException if there was an error
     */
    private JsonObject getFromArray(JSONArray array) {
        if (array.isEmpty()) {
            log.warn("Found only empty array, starting new object");
            return new JsonObject();
        }
        // Grab the first element
        Object object = array.get(0);
        if (object == null) {
            log.warn("Null entry, starting new object");
            return new JsonObject();
        }
        // Nested array, go deeper
        if (object instanceof JSONArray) {
            return getFromArray((JSONArray) object);
        }
        return (JsonObject) object;
    }

    /**
     * Retrieve the given node from the provided object.
     *
     * @param path : An array of indeterminate length to use as the path
     * @return JsonObject : The JSON representation
     */
    private Object getNode(Object object, Object path) {
        if (isArray(object)) {
            try {
                return ((JSONArray) object).get((Integer) path);
            } catch(ArrayIndexOutOfBoundsException ex) {
                return null;
            }
        }
        if (isObject(object)) {
            return ((JsonObject) object).get(path);
        }
        return null;
    }

    /**
     * Return the JsonObject holding this object's JSON representation
     *
     * @return JsonObject : The JSON representation
     */
    public JsonObject getJsonObject() {
        return jsonObject;
    }

    /**
     * Walk down the JSON nodes specified by the path and retrieve the target
     * JSONArray.
     *
     * @param path : Variable length array of path segments
     * @return JSONArray : The target node, or NULL if path invalid or not an
     * array
     */
    public JSONArray getArray(Object... path) {
        Object object = getPath(path);
        if (object instanceof JSONArray) {
            return (JSONArray) object;
        }
        return null;
    }

    /**
     * Walk down the JSON nodes specified by the path and retrieve the target
     * JsonObject.
     *
     * @param path : Variable length array of path segments
     * @return JsonObject : The target node, or NULL if path invalid or not an
     * object
     */
    public JsonObject getObject(Object... path) {
        Object object = getPath(path);
        if (object instanceof JsonObject) {
            return (JsonObject) object;
        }
        return null;
    }

    /**
     * Walk down the JSON nodes specified by the path and retrieve the target.
     *
     * @param path : Variable length array of path segments
     * @return Object : The target node, or NULL if invalid
     */
    public Object getPath(Object... path) {
        Object object = jsonObject;
        boolean valid = true;
        for (Object node : path) {
            if (isValidPath(object, node)) {
                object = getNode(object, node);
            } else {
                valid = false;
                break;
            }
        }
        if (valid) {
            return object;
        }
        return null;
    }

    /**
     * Retrieve the Boolean value on the given path.
     *
     * <strong>IMPORTANT:</strong> The default value only applies if the path is
     * not found. If a string on the path is found it will be considered
     * <b>false</b> unless the value is 'true' (ignoring case). This is the
     * default behaviour of the Boolean.parseBoolean() method.
     *
     * @param defaultValue : The fallback value to use if the path is
     * invalid or not found
     * @param path : An array of indeterminate length to use as the path
     * @return Boolean : The Boolean value found on the given path, or null if
     * no default provided
     */
    public Boolean getBoolean(Boolean defaultValue, Object... path) {
        Object object = getPath(path);
        if (object == null) {
            return defaultValue;
        }
        if (isNumber(object)) {
            log.warn("getBoolean() : Integer value targeted. Expected Boolean");
            return defaultValue;
        }
        if (object instanceof String) {
            return Boolean.parseBoolean((String) object);
        }
        if (object instanceof Boolean) {
            return (Boolean) object;
        }
        return null;
    }

    /**
     * Retrieve the Integer value on the given path.
     *
     * @param defaultValue : The fallback value to use if the path is
     * invalid or not found
     * @param path : An array of indeterminate length to use as the path
     * @return Integer : The Integer value found on the given path, or null if
     * no default provided
     */
    public Integer getInteger(Integer defaultValue, Object... path) {
        Object object = getPath(path);
        if (object == null) {
            return defaultValue;
        }
        if (isNumber(object)) {
            return makeNumber(object);
        }
        if (object instanceof String) {
            try {
                return Integer.parseInt((String) object);
            } catch (NumberFormatException ex) {
                log.warn("getInteger() : String is not a parsable Integer '{}'",
                        (String) object);
                return defaultValue;
            }
        }
        if (object instanceof Boolean) {
            log.warn("getInteger() : Boolean value targeted. Expected Integer");
            return defaultValue;
        }
        return null;
    }

    /**
     * Retrieve the String value on the given path.
     *
     * @param defaultValue : The fallback value to use if the path is
     * invalid or not found
     * @param path : An array of indeterminate length to use as the path
     * @return String : The String value found on the given path, or null if
     * no default provided
     */
    public String getString(String defaultValue, Object... path) {
        String response = null;

        Object object = getPath(path);
        if (object == null) {
            response = defaultValue;
        } else {
        }
        if (isNumber(object)) {
            response = Integer.toString(makeNumber(object));
        }
        if (object instanceof String) {
            response = (String) object;
        }
        if (object instanceof Boolean) {
            response = Boolean.toString((Boolean) object);
        }

        if (object instanceof JSONArray) {
            jsonArray = (JSONArray)object;
            if (jsonArray.size() == 1) {
                Object value = jsonArray.get(0);
                if (value instanceof String) {
                    response = (String)value;
                }
            }
            if (! (response instanceof String)) {
                log.warn("Unable to convert JSONArray: " + object + " to string.");
            }
        }

        // Are we substituting system properites?
        if (substitueProperties) {
            response = StrSubstitutor.replaceSystemProperties(response);
        }

        return response;
    }

    /**
     * <p>
     * Retrieve a list of Strings found on the given path. Note that this is a
     * utility function, and not designed for data traversal. It <b>will</b>
     * only retrieve Strings found on the provided node, and the node must be
     * a JSONArray.
     * </p>
     *
     * @param path : An array of indeterminate length to use as the path
     * @return List<String> : A list of Strings, null if the node is not found
     */
    public List<String> getStringList(Object... path) {
        Object target = getPath(path);
        List<String> response = new LinkedList<String>();
        if (isArray(target)) {
            if (substitueProperties) {
                List<String> temp = JsonSimple.getStringList((JSONArray) target);
                for (String string : temp) {
                    response.add(StrSubstitutor.replaceSystemProperties(string));
                }
                return response;
            } else {
                return JsonSimple.getStringList((JSONArray) target);
            }
        }
        if (isString(target)) {
            // Are we substituting system properites?
            if (substitueProperties) {
                response.add(StrSubstitutor.replaceSystemProperties((String) target));
            } else {
                response.add((String) target);
            }
            return response;
        }
        return null;
    }

    /**
     * <p>
     * Retrieve a list of JsonSimple objects found on the given path. Note that
     * this is a utility function, and not designed for data traversal. It
     * <b>will</b> only retrieve valid JsonObjects found on the provided node,
     * and wrap them in JsonSimple objects.
     * </p>
     *
     * <p>
     * Other objects found on that path will be ignored, and if the path itself
     * is not a JSONArray or not found, the function will return NULL.
     * </p>
     *
     * @param path : An array of indeterminate length to use as the path
     * @return List<JsonSimple> : A list of JSONSimple objects, or null
     */
    public List<JsonSimple> getJsonSimpleList(Object... path) {
        JSONArray array = getArray(path);
        if (isArray(array)) {
            return JsonSimple.toJavaList(array);
        }
        return null;
    }

    /**
     * <p>
     * Retrieve a map of JsonSimple objects found on the given path. Note that
     * this is a utility function, and not designed for data traversal. It
     * <b>will</b> only retrieve valid JsonObjects found on the provided node,
     * and wrap them in JsonSimple objects.
     * </p>
     *
     * <p>
     * Other objects found on that path will be ignored, and if the path itself
     * is not a JsonObject or not found, the function will return NULL.
     * </p>
     *
     * @param path : An array of indeterminate length to use as the path
     * @return Map<String, JsonSimple> : A map of JSONSimple objects, or null
     */
    public Map<String, JsonSimple> getJsonSimpleMap(Object... path) {
        JsonObject object = getObject(path);
        if (isObject(object)) {
            return JsonSimple.toJavaMap(object);
        }
        return null;
    }

    /**
     * <p>
     * Search through the JSON for any nodes (at any depth) matching the
     * requested name and return them. The returned List will be of type
     * Object and require type interrogation for detailed use, but will be
     * implemented as a LinkedList to preserve order.
     * </p>
     *
     * @param node : The node name we are looking for
     * @return List<Object> : A list of matching Objects from the data
     */
    public List<Object> search(String node) {
        List<Object> response = new LinkedList<Object>();
        for (Object key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            // Check this node
            if (node.equals((String) key)) {
                response.add(value);
            }
            // Check Object children
            if (isObject(value)) {
                JsonSimple child = new JsonSimple((JsonObject) value);
                response.addAll(child.search(node));
            }
            // Check Object grand-children if hidden in arrays
            if (isArray(value)) {
                List<JsonSimple> grandChildren =
                        JsonSimple.toJavaList((JSONArray) value);
                for (JsonSimple grandChild : grandChildren) {
                    response.addAll(grandChild.search(node));
                }
            }
        }
        return response;
    }

    /**
     * <p>
     * Walk down the JSON nodes specified by the path and retrieve the target
     * array, writing each node that doesn't exist along the way.
     * </p>
     *
     * <p>
     * Note, when addressing path segments that are array indices you can use
     * '-1' to indicate that the next object should always be created fresh
     * and appended to the array.
     * </p>
     *
     * <p>
     * Array indices that are standard (ie. not -1) integers will be read as
     * normal, but return a null value from the method if they are not found.
     * This method will not attempt to write to a non-existing index of an
     * array.
     * </p>
     *
     * @param path : Variable length array of path segments
     * @return JSONArray : The target node, or NULL if path is invalid
     */
    public JSONArray writeArray(Object... path) {
        Object response = writePath(new JSONArray(), path);
        if (isArray(response)) {
            return (JSONArray) response;
        }
        // The node already exists and it is not an array
        return null;
    }

    /**
     * <p>
     * Walk down the JSON nodes specified by the path and retrieve the target
     * object, writing each node that doesn't exist along the way.
     * </p>
     *
     * <p>
     * Note, when addressing path segments that are array indices you can use
     * '-1' to indicate that the next object should always be created fresh
     * and appended to the array.
     * </p>
     *
     * <p>
     * Array indices that are standard (ie. not -1) integers will be read as
     * normal, but return a null value from the method if they are not found.
     * This method will not attempt to write to a non-existing index of an
     * array.
     * </p>
     *
     * @param path : Variable length array of path segments
     * @return Object : The target node, or NULL if path is invalid
     */
    public JsonObject writeObject(Object... path) {
        Object response = writePath(new JsonObject(), path);
        if (isObject(response)) {
            return (JsonObject) response;
        }
        // The node already exists and it is not a JsonObject
        return null;
    }

    /**
     * Walk down the JSON nodes specified by the path and retrieve the target,
     * writing each node that doesn't exist along the way.
     *
     * @param defaultNode : The Object to place at the end of the path if it
     * doesn't exist
     * @param path : Variable length array of path segments
     * @return Object : The target node, or NULL if path is invalid
     */
    private Object writePath(Object defaultNode, Object... path) {
        Object object = jsonObject;
        Object child = null;

        for (int i = 0; i < path.length; i++) {
            Object node = path[i];

            // Make sure the path provided is valid to address this node
            if (isValidPath(object, node)) {
                child = getNode(object, node);

                // What to do about a non-existant node
                if (child == null) {
                    // Check the next node first
                    int j = i + 1;
                    if (j >= path.length) {
                        // End of the path, use the default value from param
                        child = writeNode(object, defaultNode, node);
                    } else {
                        // We aren't at the end of the path, what is
                        //  the next node expecting to find?
                        Object nextNode = path[j];
                        if (isString(nextNode)) {
                            // An object
                            child = writeNode(object, new JsonObject(), node);
                        } else {
                            if (isNumber(nextNode)) {
                                // An array
                                child = writeNode(
                                        object, new JSONArray(), node);
                            } else {
                                // Neither... that path won't work
                                return null;
                            }
                        }
                    }
                }
                // Continue the loop
                object = child;
            } else {
                return null;
            }
        }

        return object;
    }

    /**
     * Retrieve the specified node for writing. Will create if necessary.
     *
     * @param parent : The object to get the node from (or write to)
     * @param newChild : The new node to write if none is found there
     * @param nodeName : The node name
     * @return Object : The node found at that path, or provided child.
     * Null if path is invalid
     */
    private Object writeNode(Object parent, Object newChild, Object nodeName) {
        if (nodeName == null || parent == null) {
            return null;
        }
        if (!isValidPath(parent, nodeName)) {
            return null;
        }

        // Can we just return a valid node?
        Object oldChild = getNode(parent, nodeName);
        if (oldChild != null) {
            return oldChild;
        }

        // Are we writing into an array?
        if (isArray(parent)) {
            // We will only append to arrays
            if (-1 == (Integer) nodeName) {
                JSONArray array = (JSONArray) parent;
                array.add(newChild);
                return newChild;
            }
        }

        // Or an object?
        if (isObject(parent)) {
            JsonObject object = (JsonObject) parent;
            object.put(nodeName, newChild);
            return newChild;
        }

        // Something is wrong with the data types of the path if we got here
        return null;
    }

    /*===============================================
     *
     * A series of small wrappers for type testing.
     *
     * ==============================================
     */

    // JSONArray
    private boolean isArray(Object object) {
        return (object instanceof JSONArray);
    }

    // Integer or Long
    private boolean isNumber(Object object) {
        return (object instanceof Integer || object instanceof Long);
    }

    // JsonObject
    private boolean isObject(Object object) {
        return (object instanceof JsonObject);
    }

    // String
    private boolean isString(Object object) {
        return (object instanceof String);
    }

    // Test that path is valid for the object
    private boolean isValidPath(Object object, Object path) {
        if (isArray(object) && path instanceof Integer) {
            return true;
        }
        if (isObject(object) && path instanceof String) {
            return true;
        }
        return false;
    }

    // Integer or Long
    private Integer makeNumber(Object object) {
        if (object instanceof Integer) {
            return (Integer) object;
        }
        if (object instanceof Long) {
            return Integer.parseInt(Long.toString((Long) object));
        }
        return null;
    }

    private String printNode(String label, Object data, String prefix) {
        // Prefix
        String tab = "    ";

        // Label
        String labelString = "";
        if (label != null) {
            labelString = "\"" + label + "\": ";
        }

        // Data - Arrays
        String dataString = null;
        if (data instanceof JSONArray) {
            List<String> lines = new LinkedList<String>();
            JSONArray array = (JSONArray) data;
            for (Object entry : array) {
                lines.add(printNode(null, entry, prefix + tab));
            }
            String output = StringUtils.join(lines, ",\n");
            dataString = "[\n" + output + "\n" + prefix + "]";
        }
        // Data - Objects
        if (data instanceof JsonObject) {
            List<String> lines = new LinkedList<String>();
            JsonObject object = (JsonObject) data;
            for (Object key : object.keySet()) {
                lines.add(printNode(
                        (String) key, object.get(key), prefix + tab));
            }
            String output = StringUtils.join(lines, ",\n");
            dataString = "{\n" + output + "\n" + prefix + "}";
        }
        // Data - Boolean
        if (data instanceof Boolean) {
            dataString = ((Boolean) data).toString();
        }
        // Data - Numbers
        if (data instanceof Long) {
            dataString = ((Long) data).toString();
        }
        // Data - Everything else... Strings
        if (dataString == null) {
            if (data == null) {
                dataString = "null";
            } else {
                // Escape quotes and slashes
                String value = data.toString();
                value = value.replace("\\", "\\\\");
                value = value.replace("\"", "\\\"");
                dataString = "\"" + value + "\"";
            }
        }

        return prefix + labelString + dataString;
    }

    /**
     * Return the String representation of this object's JSON
     *
     * @return String : The JSON String
     */
    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * Return the String representation of this object's JSON, optionally
     * formatted for human readability.
     *
     * @return String : The JSON String
     */
    public String toString(boolean pretty) {
        // Simple, just a plain old string
        if (!pretty) {
            return jsonObject.toJSONString();
        }

        // More complicated
        return printNode(null, getJsonObject(), "");
    }

    /* ===============================================
     *   Below can be found a selection of static
     *   utilities for common JSON manipulation
     * ===============================================
     */

    /**
     * Get a list of strings found on the specified node
     *
     * @param json object to retrieve from
     * @param field The field that has the list
     * @return List<String> The resulting list
     */
    public static List<String> getStringList(JsonObject json, String field) {
        List<String> response = new LinkedList<String>();
        Object object = json.get(field);
        if (object instanceof JSONArray) {
            return getStringList((JSONArray) object);
        }
        return response;
    }

    /**
     * Get a list of strings from the provided JSONArray
     *
     * @param json Array to retrieve strings from
     * @return List<String> The resulting list
     */
    public static List<String> getStringList(JSONArray json) {
        List<String> response = new LinkedList<String>();
        for (Object obj : json) {
            if (obj instanceof String) {
                response.add((String) obj);
            }
        }
        return response;
    }

    /**
     * <p>
     * Take all of the JsonObjects found in a JSONArray, wrap them in
     * JsonSimple objects, then add to a Java list and return.
     * </p>
     *
     * All entries found that are not JsonObjects are ignored.
     *
     * @return String : The JSON String
     */
    public static List<JsonSimple> toJavaList(JSONArray array) {
        List<JsonSimple> response = new LinkedList<JsonSimple>();
        if (array != null && !array.isEmpty()) {
            for (Object object : array) {
                if (object != null && object instanceof JsonObject) {
                    response.add(new JsonSimple((JsonObject) object));
                }
            }
        }
        return response;
    }

    /**
     * <p>
     * Take all of the JsonObjects found in a JsonObject, wrap them in
     * JsonSimple objects, then add to a Java Map and return.
     * </p>
     *
     * All entries found that are not JsonObjects are ignored.
     *
     * @return String : The JSON String
     */
    public static Map<String, JsonSimple> toJavaMap(JsonObject object) {
        Map<String, JsonSimple> response = new LinkedHashMap<String, JsonSimple>();
        if (object != null && !object.isEmpty()) {
            for (Object key : object.keySet()) {
                Object child = object.get(key);
                if (child != null && child instanceof JsonObject) {
                    response.put((String) key,
                            new JsonSimple((JsonObject) child));
                }
            }
        }
        return response;
    }

    /**
     * <p>
     * Take all of the JsonSimple objects in the given Map and return a Map
     * having replace them all with their base JsonObjects.
     * </p>
     *
     * Useful in when combining stacks of JSON objects.
     *
     * @return String : The JSON String
     */
    public static Map<String, JsonObject> fromJavaMap(
            Map<String, JsonSimple> from) {
        Map<String, JsonObject> response = new LinkedHashMap<String, JsonObject>();
        if (from != null && !from.isEmpty()) {
            for (String key : from.keySet()) {
                response.put(key, from.get(key).getJsonObject());
            }
        }
        return response;
    }
    
    public JSONArray getJsonArray() {
        return jsonArray;
    }
}
