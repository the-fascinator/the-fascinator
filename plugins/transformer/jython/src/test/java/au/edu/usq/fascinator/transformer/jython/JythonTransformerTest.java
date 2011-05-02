/*
 * The Fascinator
 * Copyright (C) 2009  University of Southern Queensland
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
package au.edu.usq.fascinator.transformer.jython;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.storage.StorageUtils;
import au.edu.usq.fascinator.transformer.jython.JythonTransformer;

/**
 * @author Linda Octalina
 * 
 */
@Ignore
public class JythonTransformerTest {
    private JythonTransformer aperture;

    private Storage ram;

    private DigitalObject testObject, testObjectOutput;

    private static String config = "{\"aperture\":{\"outputPath\":\""
            + System.getProperty("java.io.tmpdir").replace("\\", "/") + "\"}}";

    @Before
    public void setup() throws Exception {
        aperture = new JythonTransformer();
        aperture.init(config);
        ram = PluginManager.getStorage("ram");
        ram.init("{}");
    }

    @After
    public void shutdown() throws Exception {
        if (testObject != null) {
            testObject.close();
        }
        if (ram != null) {
            ram.shutdown();
        }
    }

    @Test
    public void testImageFile() throws URISyntaxException,
            TransformerException, StorageException {
        File imageFile = new File(getClass().getResource("/presentation01.jpg")
                .toURI());

        testObject = StorageUtils.storeFile(ram, imageFile);
        testObjectOutput = aperture.transform(testObject, "{}");

        Properties prop = testObjectOutput.getMetadata();
        Assert.assertEquals(prop.getProperty("modified"), "true");
    }
}
