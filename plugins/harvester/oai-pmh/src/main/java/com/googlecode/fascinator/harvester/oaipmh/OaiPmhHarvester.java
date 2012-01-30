/* 
 * The Fascinator - Plugin - Harvester - OAI-PMH
 * Copyright (C) 2008-2011 University of Southern Queensland
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
package com.googlecode.fascinator.harvester.oaipmh;

import com.googlecode.fascinator.api.harvester.HarvesterException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.PayloadType;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.harvester.impl.GenericHarvester;
import com.googlecode.fascinator.common.storage.StorageUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kb.oai.OAIException;
import se.kb.oai.pmh.ErrorResponseException;
import se.kb.oai.pmh.OaiPmhServer;
import se.kb.oai.pmh.Record;
import se.kb.oai.pmh.RecordsList;
import se.kb.oai.pmh.ResumptionToken;

/**
 * <p>
 * This plugin harvests metadata records from an OAI-PMH compatible repository
 * using <a href="http://www.openarchives.org/pmh/">OAI-PMH</a> protocol. If the
 * repository returns a 503, the HTTP headers are checked for Retry-After value,
 * in an effort not to hammer the server.
 * </p>
 * 
 * <h3>Configuration</h3>
 * <p>
 * Sample configuration file for OAI PMH harvester: <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/trunk/plugins/harvester/oai-pmh/src/main/resources/harvest/usq.json"
 * >usq.json</a>
 * </p>
 * 
 * <table border="1">
 * <tr>
 * <th>Option</th>
 * <th>Description</th>
 * <th>Required</th>
 * <th>Default</th>
 * </tr>
 * <tr>
 * <td>url</td>
 * <td>The base URL of the OAI-PMH repository to harvest</td>
 * <td><b>Yes</b></td>
 * <td><i>None</i></td>
 * </tr>
 * <tr>
 * <td>maxRequests</td>
 * <td>Limit number of HTTP requests to make. Set this to -1 to configure the
 * harvester to retrieve all records.</td>
 * <td>No</td>
 * <td>-1</td>
 * </tr>
 * <tr>
 * <td>metadataPrefix</td>
 * <td>Set the type of metadata records to harvest, the first prefix in the list
 * will be set as the source payload</td>
 * <td>No</td>
 * <td>oai_dc</td>
 * </tr>
 * <tr>
 * <td>setSpec</td>
 * <td>Set the OAI-PMH set to harvest</td>
 * <td>No</td>
 * <td><i>None</i></td>
 * </tr>
 * <tr>
 * <td>from</td>
 * <td>Harvest records from this date</td>
 * <td>No</td>
 * <td><i>None</i></td>
 * </tr>
 * <tr>
 * <td>until</td>
 * <td>Harvest records up to this date</td>
 * <td>No</td>
 * <td><i>None</i></td>
 * </tr>
 * </table>
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * Get the first page of records from USQ EPrints
 * 
 * <pre>
 * "harvester": {
 *     "type": "oai-pmh",
 *     "oai-pmh": {
 *         "url": "http://eprints.usq.edu.au/cgi/oai2",
 *         "maxRequests": 1
 *     }
 * }
 * </pre>
 * 
 * </li>
 * <li>
 * Get a specific record from USQ EPrints
 * 
 * <pre>
 * "harvester": {
 *     "type": "oai-pmh",
 *     "oai-pmh": {
 *         "url": "http://eprints.usq.edu.au/cgi/oai2",
 *         "recordID": "oai:eprints.usq.edu.au:5"
 *     }
 * }
 * </pre>
 * 
 * </li>
 * <li>
 * Get only records from January 2009 from USQ EPrints
 * 
 * <pre>
 * "harvester": {
 *     "type": "oai-pmh",
 *     "oai-pmh": {
 *         "url": "http://eprints.usq.edu.au/cgi/oai2",
 *         "from": "2009-01-01T00:00:00Z",
 *         "until": "2009-01-31T00:00:00Z"
 *     }
 * }
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Rule file</h3>
 * <p>
 * Sample rule file for the OAI PMH harvester: <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/trunk/plugins/harvester/oai-pmh/src/main/resources/harvest/usq.py"
 * >usq.py</a>
 * </p>
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * <b>None</b>
 * </p>
 * 
 * @author Oliver Lucido
 */
public class OaiPmhHarvester extends GenericHarvester {

    private static final String PROTOCOL_HANDLER_KEY = "java.protocol.handler.pkgs";

    /** Date format */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /** Date and time format */
    public static final String DATETIME_FORMAT = DATE_FORMAT + "'T'hh:mm:ss'Z'";

    /** Default metadataPrefix (Dublin Core) */
    public static final String DEFAULT_METADATA_PREFIX = "oai_dc";

    /** Logging */
    private Logger log = LoggerFactory.getLogger(OaiPmhHarvester.class);

    /** OAI-PMH server */
    private OaiPmhServer server;

    /** Whether or not the harvest has started */
    private boolean started;

    /** Session resumption token */
    private ResumptionToken token;

    /** Current number of requests/objects done */
    private int numRequests;

    private int numObjects;

    /** Maximum requests/objects to do */
    private int maxRequests;

    private int maxObjects;

    /** Request for a specific document */
    private String recordID;

    /** Existing protocol handlers */
    private String protocolHandlerPkgs;

    /** Date limits */
    private String dateFrom;
    private String dateUntil;

    /** Metadata prefix list */
    private List<String> metadataPrefixes;

    /** OAI-PMH Set */
    private String setSpec;

    /**
     * Basic constructor.
     *
     */
    public OaiPmhHarvester() {
        // Just provide GenericHarvester our identity.
        super("oai-pmh", "OAI-PMH Harvester");
    }

    /**
     * Basic init() function. Notice the lack of parameters. This is not part
     * of the Plugin API but from the GenericHarvester implementation. It will
     * be called following the constructor verifies configuration is available.
     *
     * @throws HarvesterException : If there are problems during instantiation
     */
    @Override
    public void init() throws HarvesterException {
        String url = getJsonConfig().getString(null,
                "harvester", "oai-pmh", "url");
        if (url == null) {
            throw new HarvesterException("OAI-PMH : No server URL provided!");
        }
        server = new OaiPmhServer(url);

        /** Check for request on a specific ID */
        recordID = getJsonConfig().getString(null,
                "harvester", "oai-pmh", "recordID");

        /** Check for any specified result set size limits */
        maxRequests = getJsonConfig().getInteger(-1,
                "harvester", "oai-pmh", "maxRequests");
        if (maxRequests == -1) {
            maxRequests = Integer.MAX_VALUE;
        }
        maxObjects = getJsonConfig().getInteger(-1,
                "harvester", "oai-pmh", "maxObjects");
        if (maxObjects == -1) {
            maxObjects = Integer.MAX_VALUE;
        }

        started = false;
        numRequests = 0;
        numObjects = 0;

        // Check for data range requests
        dateFrom = validDate(getJsonConfig().getString(null,
                "harvester", "oai-pmh", "from"));
        dateUntil = validDate(getJsonConfig().getString(null,
                "harvester", "oai-pmh", "until"));
        if (dateFrom == null) {
            log.info("Harvesting all records");
        } else {
            log.info("Harvesting records from {} to {}", dateFrom,
                    dateUntil == null ? dateUntil : " now");
        }

        // Metadata prefixes
        JsonObject configNode = getJsonConfig().getObject(
                "harvester", "oai-pmh");
        metadataPrefixes = JsonSimple.getStringList(configNode,
                "metadataPrefix");
        if (metadataPrefixes == null || metadataPrefixes.isEmpty()) {
            metadataPrefixes = new ArrayList<String>();
            metadataPrefixes.add(DEFAULT_METADATA_PREFIX);
        }

        setSpec = getJsonConfig().getString(null,
                "harvester", "oai-pmh", "setSpec");
    }

    /**
     * Confirm that the data time string provided is valid and reduce the
     * granularity to a basic date. Can be used to assign values by return
     * value since it will convert invalid dates to null.
     *
     * @param date : The basic string to parse
     * @return String : The date without time if valid, otherwise null
     */
    private String basicDate(String date) {
        // We except full date time strings
        DateFormat format = new SimpleDateFormat(DATETIME_FORMAT);
        if (date != null) {
            try {
                Date objDate = format.parse(date);
                // But we are only sending basic dates to the server
                format = new SimpleDateFormat(DATE_FORMAT);
                return format.format(objDate);
            } catch (ParseException pe) {
                log.warn("Failed to parse date: '{}'", date, pe);
                return null;
            }
        }
        return null;
    }

    /**
     * Confirm that the data time string provided is valid. Can be used to
     * assign values by return value since it will convert invalid dates
     * to null.
     *
     * @param date : The basic string to parse
     * @return String : The same string if valid, otherwise null
     */
    private String validDate(String date) {
        // We except full date time strings
        DateFormat format = new SimpleDateFormat(DATETIME_FORMAT);
        if (date != null) {
            try {
                // It's valid if it parses
                format.parse(date);
                return date;
            } catch (ParseException pe) {
                log.warn("Failed to parse date: '{}'", date, pe);
                return null;
            }
        }
        return null;
    }

    /**
     * Gets a list of digital object IDs. If there are no objects, this method
     * should return an empty list, not null.
     *
     * @return a list of object IDs, possibly empty
     * @throws HarvesterException if there was an error retrieving the objects
     */
    @Override
    public Set<String> getObjectIdList() throws HarvesterException {
        // set to use our custom http url handler
        System.setProperty(PROTOCOL_HANDLER_KEY, getClass().getPackage()
                .getName());

        Set<String> items = new HashSet<String>();

        RecordsList records = null;
        try {
            numRequests++;
            /** Request for a specific ID */
            if (recordID != null) {
                log.info("Requesting record {}", recordID);
                for (String metadataPrefix : metadataPrefixes) {
                    Record record = server.getRecord(recordID, metadataPrefix);
                    try {
                        items.add(createOaiPmhDigitalObject(
                                record, metadataPrefix));
                    } catch (StorageException se) {
                        log.error("Failed to create object", se);
                    } catch (IOException ioe) {
                        log.error("Failed to read object", ioe);
                    }
                    return items;
                }

                /** Continue an already running request */
            } else if (started) {
                records = server.listRecords(token);
                log.info("Resuming harvest using token {}", token.getId());

                /** Start a new request */
            } else {
                started = true;
                try {
                    records = server.listRecords(metadataPrefixes.get(0),
                            dateFrom, dateUntil, setSpec);
                } catch (ErrorResponseException ere) {
                    // Some providers will not accept a full datetime
                    if (ere.getMessage().startsWith("Max granularity")) {
                        log.warn(ere.getMessage());
                        dateFrom = basicDate(dateFrom);
                        dateUntil = basicDate(dateUntil);
                    }
                    // Try again
                    log.info("Harvesting records from {} to {}", dateFrom,
                            dateUntil == null ? dateUntil : " now");
                    records = server.listRecords(metadataPrefixes.get(0),
                            dateFrom, dateUntil, setSpec);
                }
            }
            for (Record record : records.asList()) {
                if (numObjects < maxObjects) {
                    numObjects++;
                    try {
                        items.add(createOaiPmhDigitalObject(record,
                                metadataPrefixes.get(0)));
                        // If there is other metadataPrefix, get the record and
                        // add the record to the payload
                        if (metadataPrefixes.size() > 1) {
                            String id = record.getHeader().getIdentifier();
                            for (int count = 1; count < metadataPrefixes.size(); count++) {
                                Record otherRecord = server.getRecord(id,
                                        metadataPrefixes.get(count));
                                log.info("..... recordId {}", otherRecord
                                        .getHeader().getIdentifier());
                                createOaiPmhDigitalObject(otherRecord,
                                        metadataPrefixes.get(count));
                            }
                        }
                    } catch (StorageException se) {
                        log.error("Failed to create object", se);
                    } catch (IOException ioe) {
                        log.error("Failed to read object", ioe);
                    }
                }
            }
            token = records.getResumptionToken();
        } catch (OAIException oe) {
            throw new HarvesterException(oe);
        }

        // reset url handler
        if (protocolHandlerPkgs == null) {
            System.getProperties().remove(PROTOCOL_HANDLER_KEY);
        } else {
            System.setProperty(PROTOCOL_HANDLER_KEY, protocolHandlerPkgs);
        }

        return items;
    }

    /**
     * Store the payload specified by the metadata prefix in the given record.
     *
     * This method will create a new DigitalObject if the record has never been
     * seen before with this payload as the source, otherwise it will add the
     * payload to an existing object as an enrichment.
     *
     * @param record : The OAI-PMH record with the data
     * @param metadataPrefix : The metadata prefix we are interested in
     * @return String : The OID of the stored object
     * @throws HarvesterException : if there was an error accessing storage
     * @throws IOException : if there was an error accessing or parsing the data
     * @throws StorageException : if there was an error writing to storage
     */
    private String createOaiPmhDigitalObject(Record record,
            String metadataPrefix) throws HarvesterException, IOException,
            StorageException {
        Storage storage = getStorage();
        String oid = record.getHeader().getIdentifier();
        oid = DigestUtils.md5Hex(oid);
        DigitalObject object = StorageUtils.getDigitalObject(storage, oid);
        String pid = metadataPrefix + ".xml";

        Payload payload = StorageUtils.createOrUpdatePayload(object, pid,
                IOUtils.toInputStream(record.getMetadataAsString(), "UTF-8"));
        payload.setContentType("text/xml");
        // Make sure only the first metadataPrefix will be set as source
        if (object.getSourceId() == null) {
            payload.setType(PayloadType.Source);
            object.setSourceId(pid);
        } else {
            payload.setType(PayloadType.Enrichment);
        }
        payload.close();

        // update object metadata
        Properties props = object.getMetadata();
        props.setProperty("render-pending", "true");

        object.close();
        return object.getId();
    }

    /**
     * Tests whether there are more objects to retrieve. This method should
     * return true if called before getObjects.
     *
     * @return true if there are more objects to retrieve, false otherwise
     */
    @Override
    public boolean hasMoreObjects() {
        return token != null && numRequests < maxRequests
                && numObjects < maxObjects;
    }
}
