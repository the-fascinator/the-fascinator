/*******************************************************************************
 *Copyright (C) 2014 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
 *
 *This program is free software: you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation; either version 2 of the License, or
 *(at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License along
 *with this program; if not, write to the Free Software Foundation, Inc.,
 *51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/
package com.googlecode.fascinator.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.json.simple.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Tests the JsonSimpleConfig external config inclusion process.
 * 
 * @author <a href="https://github.com/shilob">Shilo Banihit</a>
 * 
 */
public class JsonSimpleConfigTest {
    private static Logger log = LoggerFactory
            .getLogger(JsonSimpleConfigTest.class);
    private JsonSimpleConfig config;

    @Before
    public void setup() throws Exception {
        config = new JsonSimpleConfig(new File(
                "src/test/resources/test-simple-config.json"));
    }

    @Test
    public void testInclusion() {
        log.debug(config.toString(true));
        String storageType = config.getString(null, "storage", "type");
        System.setProperty("solr.password", "solrpw");
        assertNotNull(storageType);
        assertEquals("fedora3", storageType);
        assertEquals("fedoraAdmin",
                config.getString(null, "storage", "config", "username"));
        assertEquals("fedoraAdmin",
                config.getString(null, "storage", "config", "password"));
        assertEquals("solr", config.getString(null, "indexer", "type"));
        assertEquals("solrpw",
                config.getString(null, "indexer", "config", "password"));
        JSONArray mapArray = config.getArray("map-list");
        for (Object jsonObj : mapArray) {
            JsonObject json = (JsonObject) jsonObj;
            assertTrue("map-one".equals(json.get("name"))
                    || "map-two".equals(json.get("name")));
        }
        assertEquals("testing2", config.getString(null, "test"));
        assertEquals("value1", config.getString(null, "entry1", "field1"));
        assertEquals("value2", config.getString(null, "entry1", "field2"));
        assertEquals("value3", config.getString(null, "entry1", "field3"));
        JSONArray entry3 = config.getArray("entry3");
        assertEquals(4, entry3.size());
        assertEquals("value1", config.getString(null, "entry4", "field1"));
        JSONArray entry5 = config.getArray("entry5");
        assertEquals(3, entry5.size());
        assertNull(config.getObject("nullNode"));
    }

}
