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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for JsonSimple
 * 
 * @author Greg Pendlebury
 */
public class JsonSimpleTest {

    private JsonSimple json;

    @Before
    public void setup() throws Exception {
        json = new JsonSimple(getClass().getResourceAsStream(
                "/test-config.json"));
    }

    /**
     * Tests simple String retrieval
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void simpleString() throws Exception {
        Assert.assertEquals("testing", json.getString(null, "test"));
        Assert.assertEquals("fedora3", json.getString(null, "storage", "type"));
        Assert.assertEquals("http://localhost:8080/fedora",
                json.getString(null, "storage", "config", "uri"));
        Assert.assertEquals("http://localhost:8080/solr",
                json.getString(null, "indexer", "config", "uri"));
        Assert.assertEquals("true",
                json.getString(null, "indexer", "config", "autocommit"));
    }

    /**
     * Tests simple Integer retrieval
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void simpleInteger() throws Exception {
        Assert.assertEquals((Integer) 10,
                json.getInteger(null, "portal", "records-per-page"));
        Assert.assertEquals((Integer) 25,
                json.getInteger(null, "portal", "facet-count"));
    }

    /**
     * Tests simple Boolean retrieval
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void simpleBoolean() throws Exception {
        // Genuine Boolean
        Assert.assertTrue(json.getBoolean(null, "indexer", "config",
                "autocommit"));
        // "true" String
        Assert.assertTrue(json
                .getBoolean(null, "portal", "facet-sort-by-count"));
    }

    /**
     * Tests default value handling is working as intended
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void defaultValues() throws Exception {
        // Boolean
        Assert.assertNull(json.getBoolean(null, "invalid", "path"));
        Assert.assertTrue(json.getBoolean(true, "invalid", "path"));
        Assert.assertFalse(json.getBoolean(false, "invalid", "path"));
        // String => Boolean (valid)
        Assert.assertTrue(json.getBoolean(false, "portal",
                "facet-sort-by-count"));
        // Integer => Boolean (invalid)
        Assert.assertFalse(json.getBoolean(false, "portal", "records-per-page"));
        // Boolean - Parsing a random string (on valid path) will be false
        Assert.assertFalse(json.getBoolean(true, "test"));

        // Integer
        Assert.assertEquals((Integer) 10,
                json.getInteger(10, "invalid", "path"));
        // Integer - Parse error
        Assert.assertEquals((Integer) 10, json.getInteger(10, "test"));

        // String
        Assert.assertNull(json.getString(null, "invalid", "path"));
        Assert.assertEquals("random",
                json.getString("random", "invalid", "path"));
        // Boolean => String, will find genuine boolean 'true' on this path
        Assert.assertEquals("true",
                json.getString(null, "indexer", "config", "autocommit"));
    }

    /**
     * Test more complicated pathing
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void complexPaths() throws Exception {
        Assert.assertEquals("3", json.getString(null, "transformer", "ints", 2));
        Assert.assertEquals((Integer) 3,
                json.getInteger(null, "transformer", "ints", 2));
        Assert.assertEquals("two", json.getString(null, "numbers", 1));

        Assert.assertEquals("map-one",
                json.getString(null, "map-list", 0, "name"));
        Assert.assertEquals((Integer) 3,
                json.getInteger(null, "map-list", 0, "sub-list", 2));
        Assert.assertEquals("map-two",
                json.getString(null, "map-list", 1, "name"));
        Assert.assertTrue(json.getBoolean(false, "map-list", 1, "sub-list", 3));
    }

    /**
     * Test dropping out to the JSON.simple API at a specific node
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void simpleAPI() throws Exception {
        Object object = json.getPath("map-list", 0);
        Assert.assertTrue(object instanceof JsonObject);

        JsonObject jsonObject = (JsonObject) object;
        Assert.assertEquals(2, jsonObject.size());

        object = jsonObject.get("sub-list");
        Assert.assertTrue(object instanceof JSONArray);

        JSONArray jsonArray = (JSONArray) object;
        Assert.assertEquals(4, jsonArray.size());
    }

    /**
     * Build an object using API and round-trip it through the wrapping object.
     * 
     * We do this twice, once using basic Java Objects and parsing properly. And
     * a second time using JSON.simple objects and basic toString().
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void apiComparisonTest() throws Exception {
        Map<String, Serializable> object = new LinkedHashMap<String, Serializable>();
        object.put("name", "Random Name");

        // A simple list of strings
        LinkedList<Object> listOne = new LinkedList<Object>();
        listOne.add("one");
        listOne.add("two");
        listOne.add("three");
        object.put("simple-list", listOne);

        // A complex list of objects containing strings
        Map<String, String> objectOne = new LinkedHashMap<String, String>();
        objectOne.put("name", "object-one");
        Map<String, String> objectTwo = new LinkedHashMap<String, String>();
        objectTwo.put("name", "object-two");
        Map<String, String> objectThree = new LinkedHashMap<String, String>();
        objectThree.put("name", "object-three");

        LinkedList<Map<String, String>> listTwo = new LinkedList<Map<String, String>>();
        listTwo.add(objectOne);
        listTwo.add(objectTwo);
        listTwo.add(objectThree);
        object.put("complex-list", listTwo);

        // Verification. Parse by String
        json = new JsonSimple(JSONValue.toJSONString(object));
        Assert.assertEquals("Random Name", json.getString(null, "name"));

        Assert.assertEquals("one", json.getString(null, "simple-list", 0));
        Assert.assertEquals("two", json.getString(null, "simple-list", 1));
        Assert.assertEquals("three", json.getString(null, "simple-list", 2));

        Assert.assertEquals("object-one",
                json.getString(null, "complex-list", 0, "name"));
        Assert.assertEquals("object-two",
                json.getString(null, "complex-list", 1, "name"));
        Assert.assertEquals("object-three",
                json.getString(null, "complex-list", 2, "name"));

        // Now try again. Parse by object
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("name", "Random Name");

        // A simple list of strings
        JSONArray jsonListOne = new JSONArray();
        jsonListOne.add("one");
        jsonListOne.add("two");
        jsonListOne.add("three");
        jsonObject.put("simple-list", jsonListOne);

        // A complex list of objects containing strings
        JsonObject jsonObjectOne = new JsonObject();
        jsonObjectOne.put("name", "object-one");
        JsonObject jsonObjectTwo = new JsonObject();
        jsonObjectTwo.put("name", "object-two");
        JsonObject jsonObjectThree = new JsonObject();
        jsonObjectThree.put("name", "object-three");

        JSONArray jsonListTwo = new JSONArray();
        jsonListTwo.add(jsonObjectOne);
        jsonListTwo.add(jsonObjectTwo);
        jsonListTwo.add(jsonObjectThree);
        jsonObject.put("complex-list", jsonListTwo);

        json = new JsonSimple(jsonObject.toString());
        Assert.assertEquals("Random Name", json.getString(null, "name"));

        Assert.assertEquals("one", json.getString(null, "simple-list", 0));
        Assert.assertEquals("two", json.getString(null, "simple-list", 1));
        Assert.assertEquals("three", json.getString(null, "simple-list", 2));

        Assert.assertEquals("object-one",
                json.getString(null, "complex-list", 0, "name"));
        Assert.assertEquals("object-two",
                json.getString(null, "complex-list", 1, "name"));
        Assert.assertEquals("object-three",
                json.getString(null, "complex-list", 2, "name"));
    }

    /**
     * Write data into API objects that are still inside the wrapper
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void apiWriteTest() throws Exception {
        // Read a complicated path
        Assert.assertEquals("map-one",
                json.getString(null, "map-list", 0, "name"));
        // Modify the path
        JsonObject object = json.getObject("map-list", 0);
        if (object != null) {
            object.put("name", "map-one-really");
        }
        // Read it again, still from the wrapper
        Assert.assertEquals("map-one-really",
                json.getString(null, "map-list", 0, "name"));

        // Try writing a new path that doesn't exist
        object = json.writeObject("some", "random", "path");
        object.put("key", "value");
        Assert.assertEquals("value",
                json.getString(null, "some", "random", "path", "key"));

        // And an array
        JSONArray array = json.writeArray("some", "random", "array");
        array.add("value");
        Assert.assertEquals("value",
                json.getString(null, "some", "random", "array", 0));

        // Appending arrays
        object = json.writeObject("some", "random", "object", "array", -1);
        object.put("key", "value1");
        Assert.assertEquals("value1", json.getString(null, "some", "random",
                "object", "array", 0, "key"));
        object = json.writeObject("some", "random", "object", "array", -1);
        object.put("key", "value2");
        Assert.assertEquals("value2", json.getString(null, "some", "random",
                "object", "array", 1, "key"));
    }

    /**
     * Test the various utility classes offered
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void utilTest() throws Exception {
        // getStringList() - A list with 4 elements, but only 2 strings
        JsonObject object = json.getObject("map-list", 0);
        List<String> list = JsonSimple.getStringList(object, "sub-list");
        Assert.assertEquals(2, list.size());

        // getStringList() - from an array
        JSONArray array = json.getArray("map-list", 0, "sub-list");
        list = JsonSimple.getStringList(array);
        Assert.assertEquals(2, list.size());

        // toJavaList()
        array = json.getArray("map-list");
        List<JsonSimple> jsonList = JsonSimple.toJavaList(array);
        Assert.assertEquals(2, jsonList.size());
        Assert.assertEquals("3", jsonList.get(0).getString(null, "sub-list", 2));
        Assert.assertEquals(false,
                jsonList.get(1).getBoolean(null, "sub-list", 2));

        // toJavaMap()
        Map<String, JsonSimple> map = JsonSimple
                .toJavaMap(json.getJsonObject());
        // Ensure that non-objects didn't come along
        Assert.assertNotNull(json.getString(null, "comment1"));
        Assert.assertFalse(map.containsKey("comment1"));
        // But objects and sub-objects did.
        Assert.assertEquals("solrAdmin",
                json.getString(null, "indexer", "config", "username"));

        // fromJavaMap()
        map = new LinkedHashMap<String, JsonSimple>();
        JsonObject object1 = new JsonObject();
        object1.put("name", "object1");
        map.put("one", new JsonSimple(object1));
        JsonObject object2 = new JsonObject();
        object2.put("name", "object2");
        map.put("two", new JsonSimple(object2));
        JsonObject object3 = new JsonObject();
        object3.put("name", "object3");
        map.put("three", new JsonSimple(object3));
        Map<String, JsonObject> jsonMap = JsonSimple.fromJavaMap(map);
        Assert.assertEquals("object2", jsonMap.get("two").get("name"));
    }

    /**
     * Test system property substitution
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void substitutionTest() throws Exception {
        JsonObject object = new JsonObject();
        object.put("test", "${test.one}");
        json = new JsonSimple(object);

        Assert.assertEquals("${test.one}", json.getString(null, "test"));
        System.setProperty("test.one", "1");
        Assert.assertEquals("1", json.getString(null, "test"));
    }

    /**
     * Test searching for nodes at any depth
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void searchTest() throws Exception {
        // Basic object structure
        List<Object> labels = json.search("label");
        Assert.assertEquals(7, labels.size());

        // Look for two lists, that are children
        // of objects inside a list themselves
        List<Object> subLists = json.search("sub-list");
        Assert.assertEquals(2, subLists.size());

        Assert.assertTrue(subLists.get(0) instanceof JSONArray);
        Assert.assertEquals(4, ((JSONArray) subLists.get(0)).size());

        Assert.assertTrue(subLists.get(1) instanceof JSONArray);
        Assert.assertEquals(4, ((JSONArray) subLists.get(1)).size());
    }

    /*
     * Written as part of a speed comparison with old JSON Library. Uncomment
     * to execute. Benchmarks from my laptop for reference:
     *
     * === JsonSimple : Parsing : 1000 @ 874ms
     * === JsonSimple : Reading : 1000 @ 5ms
     * === JsonSimple : Objects : 1000 @ 5ms
     * === JsonSimple : Writing : 1000 @ 1ms
     * === JsonSimple : Searching : 1000 @ 33ms
     *
     * Overall, reading and manipulating JSON is faster,
     *   parsing JSON is a touch slower.
     *
    @Test
    public void speedTest() throws Exception {
        int limit = 1000;

        // Parse speed
        long startTime = new Date().getTime();
        for (int i = 0; i < limit; i++) {
            json = new JsonSimple(getClass().getResourceAsStream("/test-config.json"));
        }
        long endTime = new Date().getTime();
        System.out.println("=== JsonSimple : Parsing : " + limit + " @ " + (endTime - startTime) + "ms");

        // Basic retrieval speed
        startTime = new Date().getTime();
        for (int i = 0; i < limit; i++) {
            int output = json.getInteger(null, "map-list", 0, "sub-list", 2);
        }
        endTime = new Date().getTime();
        System.out.println("=== JsonSimple : Reading : " + limit + " @ " + (endTime - startTime) + "ms");

        // Object retrieval speed
        startTime = new Date().getTime();
        for (int i = 0; i < limit; i++) {
            JsonObject resize = json.getObject("transformer", "ice2", "resize");
            Map<String, JsonSimple> resizeMap = JsonSimple.toJavaMap(resize);
        }
        endTime = new Date().getTime();
        System.out.println("=== JsonSimple : Objects : " + limit + " @ " + (endTime - startTime) + "ms");

        // Write speed
        startTime = new Date().getTime();
        for (int i = 0; i < limit; i++) {
            JsonObject object = json.writeObject("some", "random", "path");
            object.put("key", "value");
        }
        endTime = new Date().getTime();
        System.out.println("=== JsonSimple : Writing : " + limit + " @ " + (endTime - startTime) + "ms");

        // Search speed
        startTime = new Date().getTime();
        for (int i = 0; i < limit; i++) {
            List<Object> subLists = json.search("sub-list");
            List<Object> labels = json.search("label");
        }
        endTime = new Date().getTime();
        System.out.println("=== JsonSimple : Searching : " + limit + " @ " + (endTime - startTime) + "ms");
    }
    */

    /**
     * Make sure the toString method hasn't changed any of the more complicated
     * data structures.
     * 
     * @throws Exception if any error occurs
     */
    @Test
    public void toStringTest() throws Exception {
        // We only care about the 'pretty' version
        String newString = json.toString(true);
        JsonSimple newJson = new JsonSimple(newString);

        Object[] path = { "map-list", 0, "name" };
        Assert.assertEquals(newJson.getString(null, path),
                json.getString(null, path));

        Object[] path2 = { "map-list", 0, "sub-list", 2 };
        Assert.assertEquals(newJson.getInteger(null, path2),
                json.getInteger(null, path2));

        Object[] path3 = { "map-list", 1, "sub-list", 3 };
        Assert.assertEquals(newJson.getBoolean(null, path3),
                json.getBoolean(null, path3));

        // Some null handling issues we want to test for.
        // Make sure the node existed... and was null beforehand
        JsonObject raw = json.getJsonObject();
        Assert.assertTrue(raw.containsKey("nullNode"));
        Assert.assertNull(raw.get("nullNode"));

        // Make sure it still exists... and is still null
        raw = newJson.getJsonObject();
        Assert.assertTrue(raw.containsKey("nullNode"));
        Assert.assertNull(raw.get("nullNode"));

        // Specific test for error observed in #1616
        JsonObject object = new JsonObject();
        object.put("quote", "Some \"quoted\" text with \\ escaped characters");
        newJson = new JsonSimple(object);
        // Exception will be thrown here re-parsing incorrectly
        // escaped quote if error is not fixed
        @SuppressWarnings("unused")
        JsonSimple testJson = new JsonSimple(newJson.toString(true));
    }
}
