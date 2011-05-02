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
package au.edu.usq.fascinator.transformer.aperture;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.api.transformer.TransformerException;
import au.edu.usq.fascinator.common.storage.StorageUtils;

/**
 * @author Linda Octalina
 * 
 */

public class ApertureTransformerTest {
    private ApertureTransformer aperture;

    private Storage ram;

    private DigitalObject testObject, testObjectOutput;

    private static String config = "{\"aperture\":{\"outputPath\":\""
            + System.getProperty("java.io.tmpdir").replace("\\", "/") + "\"}}";

    @Before
    public void setup() throws Exception {
        aperture = new ApertureTransformer();
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
    public void testPdfFile() throws URISyntaxException, TransformerException,
            StorageException {
        File fileNamepdf = new File(getClass().getResource("/AboutStacks.pdf")
                .toURI());

        testObject = StorageUtils.storeFile(ram, fileNamepdf);
        testObjectOutput = aperture.transform(testObject, "{}");
        Payload rdfPayload = testObjectOutput.getPayload("aperture.rdf");
        Assert.assertEquals("aperture.rdf", rdfPayload.getId());
        Assert.assertEquals("Aperture rdf", rdfPayload.getLabel());
        Assert.assertEquals("application/xml+rdf", rdfPayload.getContentType());
        Assert.assertEquals(PayloadType.Enrichment, rdfPayload.getType());
    }

    @Test
    public void testOdtFile() throws URISyntaxException, TransformerException,
            StorageException {
        File fileNameodt = new File(getClass().getResource("/test Image.odt")
                .toURI());

        testObject = StorageUtils.storeFile(ram, fileNameodt);
        testObjectOutput = aperture.transform(testObject, "{}");
        Payload rdfPayload = testObjectOutput.getPayload("aperture.rdf");
        Assert.assertEquals("aperture.rdf", rdfPayload.getId());
        Assert.assertEquals("Aperture rdf", rdfPayload.getLabel());
        Assert.assertEquals("application/xml+rdf", rdfPayload.getContentType());
        Assert.assertEquals(PayloadType.Enrichment, rdfPayload.getType());
    }

    // Test unknown file type
    @Test
    public void testSfkFileType() throws URISyntaxException,
            TransformerException, StorageException {
        File fileName = new File(getClass().getResource("/sample.sfk").toURI());

        testObject = StorageUtils.storeFile(ram, fileName);
        testObjectOutput = aperture.transform(testObject, "{}");
        Assert.assertEquals(1, testObject.getPayloadIdList().size());
    }

    // Image file?
    @Test
    public void testImageFile() throws URISyntaxException,
            TransformerException, StorageException {
        File imageFile = new File(getClass().getResource("/presentation01.jpg")
                .toURI());

        testObject = StorageUtils.storeFile(ram, imageFile);
        testObjectOutput = aperture.transform(testObject, "{}");

        // Try to print out the rdf content
        try {
            Payload rdfPayload = testObjectOutput.getPayload("aperture.rdf");
            BufferedReader r = new BufferedReader(new InputStreamReader(
                    rdfPayload.open()));
            StringBuilder sb = new StringBuilder();

            String line = null;
            try {
                while ((line = r.readLine()) != null) {
                    sb.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                rdfPayload.close();
            }
        } catch (StorageException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWithoutExtension() throws URISyntaxException,
            TransformerException, StorageException {
        File fileWOExt = new File(getClass().getResource("/somefile").toURI());

        testObject = StorageUtils.storeFile(ram, fileWOExt);
        testObjectOutput = aperture.transform(testObject, "{}");

        // Try to print out the rdf content
        try {
            Payload rdfPayload = testObjectOutput.getPayload("aperture.rdf");
            BufferedReader r = new BufferedReader(new InputStreamReader(
                    rdfPayload.open()));
            StringBuilder sb = new StringBuilder();

            String line = null;
            try {
                while ((line = r.readLine()) != null) {
                    sb.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                rdfPayload.close();
            }
        } catch (StorageException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testPackageManifest() throws URISyntaxException,
            TransformerException, StorageException {
        File fileName = new File(getClass().getResource("/manifest.tfpackage")
                .toURI());
        testObject = StorageUtils.storeFile(ram, fileName);
        testObjectOutput = aperture.transform(testObject, "{}");
        System.out.println(testObject.getPayload("manifest.tfpackage")
                .getContentType());
        Assert.assertEquals("application/x-fascinator-package", testObject
                .getPayload("manifest.tfpackage").getContentType());
    }
}
