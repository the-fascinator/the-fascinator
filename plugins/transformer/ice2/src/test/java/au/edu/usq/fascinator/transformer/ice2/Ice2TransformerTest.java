/*
 * The Fascinator - Plugin - Transformer - ICE 2
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
package au.edu.usq.fascinator.transformer.ice2;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.Server;

import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.transformer.Transformer;
import au.edu.usq.fascinator.common.storage.StorageUtils;
import org.junit.After;
import org.junit.Before;

/**
 * ICE 2 Transformer tests. Uses a mock ICE server implementation.
 * 
 * @author Linda Octalina
 */
public class Ice2TransformerTest {

    private static Server server;

    private Storage ram;

    private DigitalObject sourceObject, outputObject;

    @BeforeClass
    public static void setup() throws Exception {
        server = new Server(10002);
        server.setHandler(new Ice2Handler());
        server.start();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Before
    public void init() throws Exception {
        ram = PluginManager.getStorage("ram");
        ram.init("{}");
    }

    @After
    public void close() throws Exception {
        if (sourceObject != null) {
            sourceObject.close();
        }
        if (ram != null) {
            ram.shutdown();
        }
    }

    @Test
    public void testSingleFile() throws URISyntaxException, IOException,
            PluginException {
        File file = new File(getClass().getResource("/first-post.odt").toURI());
        sourceObject = StorageUtils.storeFile(ram, file);

        Transformer iceTransformer = PluginManager.getTransformer("ice2");
        iceTransformer.init(new File(getClass().getResource(
                "/ice-transformer.json").toURI()));
        outputObject = iceTransformer.transform(sourceObject, "{}");
        Set<String> payloads = outputObject.getPayloadIdList();

        Assert.assertEquals(4, payloads.size());

        // check for the Preview payload
        boolean foundPreview = false;
        for (String pid : payloads) {
            Payload payload = outputObject.getPayload(pid);
            if (PayloadType.Preview.equals(payload.getType())) {
                foundPreview = true;
            }
        }
        Assert.assertTrue("There should be a Preview payload!", foundPreview);
    }

    @Test
    public void testErrorFile() throws URISyntaxException,
            UnsupportedEncodingException, PluginException {
        File file = new File(getClass().getResource("/somefile.doc").toURI());
        sourceObject = StorageUtils.storeFile(ram, file);

        Transformer iceTransformer = PluginManager.getTransformer("ice2");
        iceTransformer.init(new File(getClass().getResource(
                "/ice-transformer.json").toURI()));

        outputObject = iceTransformer.transform(sourceObject, "{}");
        Set<String> payloads = outputObject.getPayloadIdList();

        Payload icePayload = outputObject.getPayload("somefile_ice_error.htm");
        Assert.assertEquals(2, payloads.size());
        Assert.assertEquals(icePayload.getId(), "somefile_ice_error.htm");
        Assert.assertEquals(icePayload.getLabel(), "ICE conversion errors");
        Assert.assertEquals(icePayload.getType(), PayloadType.Error);
        Assert.assertEquals(icePayload.getContentType(), "text/html");
    }
}
