package au.edu.usq.fascinator.harvester.jsonq;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Unit tests for JsonQDigitalObject
 * 
 * @author Oliver Lucido
 */
public class JsonQDigitalObjectTest {

    /**
     * Tests creating an object
     * 
     * @throws Exception if any error occurred
     */
    @Test
    public void create() throws Exception {
        String uri = getClass().getResource("/sample.xml").toURI().toString();
        Map<String, String> info = new HashMap<String, String>();
        info.put("state", "mod");
        info.put("time", "2009-07-07 16:19:46");
        JsonQDigitalObject obj = new JsonQDigitalObject(uri, info);
        Properties props = new Properties();
        // props.load(obj.getMetadata().getInputStream());
        Assert.assertEquals("mod", props.getProperty("state"));
        Assert.assertEquals("2009-07-07 16:19:46", props.getProperty("time"));
    }
}
