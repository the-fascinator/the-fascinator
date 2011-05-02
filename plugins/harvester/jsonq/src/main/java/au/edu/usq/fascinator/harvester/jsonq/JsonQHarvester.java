/* 
 * The Fascinator - Plugin - Harvester - JSON Queue
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.usq.fascinator.api.Configurable;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.harvester.Harvester;
import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Storage;
import au.edu.usq.fascinator.common.BasicHttpClient;
import au.edu.usq.fascinator.common.JsonConfig;

/**
 * Harvests files from a local file system via a JSON queue generated using the
 * Watcher service.
 * <p>
 * Configuration options:
 * <ul>
 * <li>url: the URL for the Watcher queue (default: "http://localhost:9000")</li>
 * <li>lastModified: harvest files modified from this date</li>
 * </ul>
 * 
 * @see http://fascinator.usq.edu.au/trac/wiki/Watcher
 * 
 * @author Duncan Dickinson
 * @author Oliver Lucido
 */
public class JsonQHarvester implements Harvester, Configurable {
    /** log **/
    private Logger log = LoggerFactory.getLogger(JsonQHarvester.class);

    /** default Watcher queue URL */
    private static final String DEFAULT_URL = "http://localhost:9000";

    /** GMT date format */
    private static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss 'GMT'";

    /** Watcher queue URL */
    private String url;

    /** harvest files modified from this date */
    private String lastModified;

    /** configuration */
    private JsonConfig config;

    /** GMT date formatter */
    private DateFormat df;

    /** JSON config file */
    private File jsonFile;

    /** List of modification state */
    private static final List<String> MODS_STATE = Arrays
            .asList("mod", "start");

    /** List of deletion state */
    private static final List<String> DELETE_STATE = Arrays.asList("del",
            "stopmod", "stopdel", "stop");

    /** Map */
    private Map<String, Map<String, String>> map;

    @Override
    public String getId() {
        return "jsonq";
    }

    @Override
    public String getName() {
        return "JSON Queue Harvester";
    }

    /**
     * Initialisation of JsonQ Harvester Plugin
     * 
     * @throws HarvesterException if fails to initialise
     */
    @Override
    public void init(File jsonFile) throws HarvesterException {
        this.jsonFile = jsonFile;
        try {
            df = new SimpleDateFormat(DATE_FORMAT);
            config = new JsonConfig(jsonFile);
            url = config.get("harvester/jsonq/url", DEFAULT_URL);
            lastModified = config.get("harvester/jsonq/lastModified");
            if ("0".equals(lastModified)) {
                lastModified = null;
            }
        } catch (IOException ioe) {
            throw new HarvesterException(ioe);
        }
    }

    @Override
    public void shutdown() throws PluginException {
        // Nothing to be done
    }

    @SuppressWarnings("unchecked")
    public void requestJsonQ() throws HarvesterException {
        BasicHttpClient client = new BasicHttpClient(url);
        GetMethod method = new GetMethod(url);
        if (lastModified != null) {
            method.setRequestHeader("Last-Modified", lastModified);
        }
        config.set("harvester/jsonq/lastModified", now(), false);
        try {
            int status = client.executeMethod(method);
            if (status == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                InputStream in = method.getResponseBodyAsStream();
                IOUtils.copy(in, out);
                in.close();
                ObjectMapper mapper = new ObjectMapper();
                map = mapper.readValue(new ByteArrayInputStream(out
                        .toByteArray()), Map.class);
            }
            method.releaseConnection();
            config.set("harvester/jsonq/state", "OK", false);
        } catch (IOException ioe) {
            config.set("harvester/jsonq/state", "Failed", false);
            throw new HarvesterException(ioe);
        } finally {
            config.set("harvester/jsonq/harvestFinished", now(), false);
            try {
                FileWriter writer = new FileWriter(jsonFile);
                config.store(writer, true);
                writer.close();
            } catch (IOException ioe) {
                throw new HarvesterException(ioe);
            }
        }
    }

    /**
     * Get object list from the state list
     * 
     * @param objectList Object lists
     * @param map
     * @param state State lists
     * @return list of Digital objects
     */
    public List<DigitalObject> getObjectListFromState(
            List<DigitalObject> objectList,
            Map<String, Map<String, String>> map, List<String> state) {
        for (String key : map.keySet()) {
            Map<String, String> info = map.get(key);
            if (state.contains(info.get("state"))) {
                objectList.add(new JsonQDigitalObject(key, map.get(key)));
            }
        }
        return objectList;
    }

    /**
     * Get current date
     * 
     * @return string of current date
     */
    private String now() {
        return df.format(new Date());
    }

    @Override
    public boolean hasMoreObjects() {
        return false;
    }

    @Override
    public boolean hasMoreDeletedObjects() {
        return false;
    }

    @Override
    public String getConfig() {
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(getClass().getResourceAsStream(
                    "/" + getId() + "-config.html"), writer);
        } catch (IOException ioe) {
            writer.write("<span class=\"error\">" + ioe.getMessage()
                    + "</span>");
        }
        return writer.toString();
    }

    @Override
    public void init(String jsonString) throws PluginException {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<String> getDeletedObjectIdList() throws HarvesterException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getObjectId(File arg0) throws HarvesterException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getObjectIdList() throws HarvesterException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setStorage(Storage arg0) {
        // TODO Auto-generated method stub

    }
}
