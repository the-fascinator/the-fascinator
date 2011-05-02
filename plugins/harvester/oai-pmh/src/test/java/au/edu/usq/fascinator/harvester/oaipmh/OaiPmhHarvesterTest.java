package au.edu.usq.fascinator.harvester.oaipmh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.testing.HttpTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.harvester.Harvester;
import au.edu.usq.fascinator.api.storage.Storage;

public class OaiPmhHarvesterTest {

    private static Logger log = LoggerFactory
            .getLogger(OaiPmhHarvesterTest.class);

    private static Storage ram;

    private static Server server;

    private static HttpTester httpTester;

    @BeforeClass
    public static void setup() throws Exception {
        // init storage
        ram = PluginManager.getStorage("ram");
        ram.init("{}");

        // init mock oai-pmh server
        httpTester = new HttpTester();
        Handler handler = new AbstractHandler() {
            private int retries = 0;

            @Override
            public void handle(String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch)
                    throws IOException, ServletException {
                httpTester.parse(getRequestString(request));
                String verb = request.getParameter("verb");
                String resource = "";
                if ("ListRecords".equals(verb)) {
                    if (request.getParameter("metadataPrefix") != null) {
                        resource = "/ListRecordsStart.xml";
                    } else {
                        retries++;
                        if (retries < 2) {
                            response
                                    .setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            response.setIntHeader("Retry-After", 1);
                            ((Request) request).setHandled(true);
                            return;
                        }
                        resource = "/ListRecordsResume.xml";
                    }
                } else if ("GetRecord".equals(verb)) {
                    resource = "/GetRecord.xml";
                }
                response.setContentType("text/xml");
                response.setStatus(HttpServletResponse.SC_OK);
                InputStream in = getClass().getResourceAsStream(resource);
                OutputStream out = response.getOutputStream();
                IOUtils.copy(in, out);
                out.close();
                in.close();
                ((Request) request).setHandled(true);
            }
        };
        server = new Server(10001);
        server.setHandler(handler);
        server.start();
    }

    private static String getRequestString(HttpServletRequest request)
            throws IOException {
        ServletInputStream reqz = request.getInputStream();
        int contentLen = request.getContentLength();
        if (contentLen == -1) {
            return request.toString();
        }
        byte[] buff = new byte[contentLen];
        int realLen = reqz.read(buff);
        Assert.assertEquals(realLen, contentLen);
        return request.toString() + new String(buff);
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (ram != null) {
            ram.shutdown();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void getObjectIdList() throws Exception {
        Harvester harvester = PluginManager.getHarvester("oai-pmh", ram);
        harvester.init(new File(getClass()
                .getResource("/multiple-records.json").toURI()));

        // get initial records
        Set<String> ids = harvester.getObjectIdList();
        Assert.assertEquals("There should be 3 items", 3, ids.size());

        // should be more
        boolean hasMoreObjects = harvester.hasMoreObjects();
        Assert.assertTrue("There should be more objects available",
                hasMoreObjects);

        // get more records
        ids = harvester.getObjectIdList();
        Assert.assertEquals("There should only be 2 items", 2, ids.size());
    }

    @Test
    public void getSingleRecord() throws Exception {
        Harvester harvester = PluginManager.getHarvester("oai-pmh", ram);
        harvester.init(new File(getClass().getResource("/single-record.json")
                .toURI()));

        // get initial records
        Set<String> ids = harvester.getObjectIdList();
        Assert.assertEquals("There should be 1 item", 1, ids.size());

        // should be more
        boolean hasMoreObjects = harvester.hasMoreObjects();
        Assert.assertFalse("There should be no more objects available",
                hasMoreObjects);
    }
}
