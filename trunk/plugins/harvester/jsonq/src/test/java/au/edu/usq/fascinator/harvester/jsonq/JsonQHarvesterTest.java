/* 
 * The Fascinator - Plugin - Harvester - File System
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

package au.edu.usq.fascinator.harvester.jsonq;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.storage.DigitalObject;

/**
 * Unit tests for the Jsonq harvester plugin
 * 
 * @author Linda Octalina
 */

public class JsonQHarvesterTest {
    String jsonQueue;
    List<DigitalObject> objectList;
    Map<String, Map<String, String>> map;

    private static final List<String> MODS_STATE = Arrays
            .asList("mod", "start");
    private static final List<String> DELETE_STATE = Arrays.asList("del",
            "stopmod", "stopdel", "stop");

    @Before
    public void setup() throws JsonParseException, JsonMappingException,
            IOException {
        jsonQueue = "{\"file:///home/octalina/Documents/picture/text/newText.txt\":"
                + "{\"state\":\"del\",\"time\":63392322147},"
                + "\"file:///home/octalina/Documents/picture/text/newText3.txt\":"
                + "{\"state\":\"stop\",\"time\":63392321962},"
                + "\"file:///home/octalina/Documents/picture/text/newText4.txt\":"
                + "{\"state\":\"stopmod\",\"time\":63392321962},"
                + "\"file:///home/octalina/Documents/picture/text/newText5.txt\":"
                + "{\"state\":\"stopdel\",\"time\":63392321962},"
                + "\"file:///home/octalina/Documents/picture/text/newText1.txt\":"
                + "{\"state\":\"start\",\"time\":63392321962},"
                + "\"file:///home/octalina/Documents/picture/text/newText2.txt\":"
                + "{\"state\":\"mod\",\"time\":63392321962}" + "}";
        ObjectMapper mapper = new ObjectMapper();
        map = mapper.readValue(jsonQueue, Map.class);
    }

    @Test
    public void getStartModifiedObject() throws HarvesterException, Exception {
        objectList = new ArrayList<DigitalObject>();
        JsonQHarvester jsonQHarvester = new JsonQHarvester();
        jsonQHarvester.init(getConfig("/jsonq.json"));
        objectList = jsonQHarvester.getObjectListFromState(objectList, map,
                MODS_STATE);
        Assert.assertEquals(2, objectList.size());
    }

    @Test
    public void getStopDelObject() throws HarvesterException, Exception {
        objectList = new ArrayList<DigitalObject>();
        JsonQHarvester jsonQHarvester = new JsonQHarvester();
        jsonQHarvester.init(getConfig("/jsonq.json"));
        objectList = jsonQHarvester.getObjectListFromState(objectList, map,
                DELETE_STATE);
        Assert.assertEquals(4, objectList.size());
    }

    private File getConfig(String filename) throws Exception {
        return new File(getClass().getResource(filename).toURI());
    }
}
