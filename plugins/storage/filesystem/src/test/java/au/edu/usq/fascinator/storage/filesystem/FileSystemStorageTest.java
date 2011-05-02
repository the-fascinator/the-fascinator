package au.edu.usq.fascinator.storage.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.StorageException;
import org.apache.commons.codec.digest.DigestUtils;

public class FileSystemStorageTest {
    private FileSystemStorage fs;

    private FileSystemDigitalObject newObject, fileObject;

    private String tmpDir = System.getProperty("java.io.tmpdir");

    @Before
    public void setup() throws Exception {
        // Prep storage
        fs = new FileSystemStorage();
        fs.init(new File(getClass().getResource("/fs-config.json").toURI()));
    }

    @After
    public void cleanup() throws IOException {
        // Purge the storage directory structure
        FileUtils.deleteDirectory(fs.getHomeDir());
    }

    /* Test a basic cycle of:
     *  - Create object
     *  - Add payload
     *  - Delete payload
     *  - Delete object
     */
    @Test
    public void testObject1() throws Exception {
        // Create an object
        String oid = DigestUtils.md5Hex("oai:eprints.usq.edu.au:318");
        newObject = (FileSystemDigitalObject) fs.createObject(oid);

        // Give it a payload
        Payload p = newObject.createStoredPayload("DC", getClass()
                .getResourceAsStream("/dc.xml"));
        p.setLabel("Dublin Core Metadata");

        System.out.println(newObject.getPath());

        // Make sure our object is in the correct location
        Assert.assertEquals(FilenameUtils.normalize(tmpDir
                + "/_fs_test/d0b1c5bd0660ad67a16b7111aafc9389/"
                + "e2/92/37/e292378c5b38b0d5a4aba11fd40e7151"), newObject
                .getPath());

        // Makes sure the object reports only 1 payload
        Set<String> payloads = newObject.getPayloadIdList();
        Assert.assertEquals(1, payloads.size());

        // Make sure our payload retrieves and is labelled correctly
        Payload payload = newObject.getPayload("DC");
        Assert.assertEquals("Dublin Core Metadata", payload.getLabel());

        // Remove the payload and recheck object payload size
        newObject.removePayload(payload.getId());
        payloads = newObject.getPayloadIdList();
        Assert.assertEquals(0, payloads.size());

        // Remove the object from storage
        try {
            fs.removeObject(newObject.getId());
        } catch (StorageException ex) {
            Assert.fail("Error deleting newObject : " + ex.getMessage());
        }
    }

    /* Similar to test1, except:
     *  - Two payloads
     *  - One payload is in a subdirectory
     */
    @Test
    public void testObject2() throws Exception {
        // Create an object
        String oid = DigestUtils.md5Hex("/Users/fascinator/Documents/sample.odt");
        fileObject = (FileSystemDigitalObject) fs.createObject(oid);

        // Give it a payload
        Payload p1 = fileObject.createStoredPayload("sample.odt", getClass()
                .getResourceAsStream("/sample.odt"));
        p1.setLabel("ICE Sample Document");
        // Give it another payload
        Payload p2 = fileObject.createStoredPayload("images/ice-services.png",
                getClass().getResourceAsStream("/images/ice-services.png"));
        p2.setLabel("ICE Services Diagram");

        // Make sure our object is in the correct location
        Assert.assertEquals(FilenameUtils.normalize(tmpDir
                + "/_fs_test/d0b1c5bd0660ad67a16b7111aafc9389/"
                + "11/b4/98/11b498d057256a0b602fa0e7c4073fc3"), fileObject
                .getPath());

        // Makes sure the object reports only 1 payload
        Set<String> payloads = fileObject.getPayloadIdList();
        Assert.assertEquals(2, payloads.size());

        // Make sure our payloads retrieve and are labelled correctly
        Payload payload1 = fileObject.getPayload("sample.odt");
        Assert.assertEquals("ICE Sample Document", payload1.getLabel());
        Payload payload2 = fileObject.getPayload("images/ice-services.png");
        Assert.assertEquals("ICE Services Diagram", payload2.getLabel());

        // Remove the first payload and recheck object payload size
        fileObject.removePayload(payload1.getId());
        payloads = fileObject.getPayloadIdList();
        Assert.assertEquals(1, payloads.size());

        // Remove the second payload and recheck object payload size
        fileObject.removePayload(payload2.getId());
        payloads = fileObject.getPayloadIdList();
        Assert.assertEquals(0, payloads.size());

        // Remove the object from storage
        try {
            fs.removeObject(fileObject.getId());
        } catch (StorageException ex) {
            Assert.fail("Error deleting fileObject : " + ex.getMessage());
        }
    }

    /* Similar to test2, except:
     *  - One payload (in a subdirectory)
     *  - The object is shutdown and reinstantiated from disk
     */
    @Test
    public void testObject3() throws Exception {
        // Create an object
        String oid = "/Users/fascinator/Documents/sample.odt";
        fileObject = (FileSystemDigitalObject) fs.createObject(oid);
        // Give it a payload
        Payload p1 = fileObject.createStoredPayload("sample.odt", getClass()
                .getResourceAsStream("/sample.odt"));
        p1.setLabel("ICE Sample Document");
        // Give it another payload
        Payload p2 = fileObject.createStoredPayload("images/ice-services.png",
                getClass().getResourceAsStream("/images/ice-services.png"));
        p2.setLabel("ICE Services Diagram");
        fileObject.close();

        // Try read object from disk
        fileObject = (FileSystemDigitalObject) fs.getObject(oid);
        Assert.assertEquals(fileObject.getPayload("images/ice-services.png")
                .getId(), "images/ice-services.png");
    }
}
