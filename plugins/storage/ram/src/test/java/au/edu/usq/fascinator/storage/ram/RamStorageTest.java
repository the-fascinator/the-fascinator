/* 
 * The Fascinator - RAM storage plugin
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
package au.edu.usq.fascinator.storage.ram;

import java.io.File;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.StorageException;

public class RamStorageTest {

    private RamStorage ram;

    @Before
    public void setup() throws Exception {
        ram = new RamStorage();
        ram.init(new File(getClass().getResource("/ram-config.json").toURI()));
    }

    @After
    public void cleanup() throws Exception {
        if (ram != null) {
            ram.shutdown();
        }
    }

    @Test
    public void createObject() throws Exception {
        // Create a test object
        DigitalObject newObject = ram
                .createObject("oai:eprints.usq.edu.au:318");
        Payload dcPayload = newObject.createStoredPayload("oai_dc", IOUtils
                .toInputStream("<dc><title>test</title></dc>"));
        dcPayload.setLabel("Dublin Core Metadata");

        // Makes sure the object reports only 1 payload
        Set<String> payloads = newObject.getPayloadIdList();
        Assert.assertEquals(1, payloads.size());

        // Make sure our payload retrieves and is labelled correctly
        Payload payload = newObject.getPayload("oai_dc");
        Assert.assertEquals("Dublin Core Metadata", payload.getLabel());

        // Remove the payload and recheck object payload size
        newObject.removePayload(payload.getId());
        payloads = newObject.getPayloadIdList();
        Assert.assertEquals(0, payloads.size());

        // Remove the object from storage
        try {
            ram.removeObject(newObject.getId());
        } catch (StorageException ex) {
            Assert.fail("Error deleting newObject : " + ex.getMessage());
        }
    }
}
