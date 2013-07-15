/*******************************************************************************
 * Copyright (C) 2013 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
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
 ******************************************************************************/
package com.googlecode.fascinator.portal.process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.common.solr.SolrResult;

/**
 * Pulls records using a specific Solr Query, as specified in the config file. <br/>
 * 
 * Processing is stage-sensitive.
 * 
 * For "pre"- processing: @see #getRecords(String, String, String, HashMap) 
 * For "post"- processing @see #postProcess(String, String, String, HashMap)
 * 
 * @author Shilo Banihit
 * 
 */
public class RecordProcessor implements Processor {

    private Logger log = LoggerFactory.getLogger(RecordProcessor.class);

    /**
     * Main processing method.
     * 
     */
    @Override
    public boolean process(String id, String inputKey, String outputKey,
            String stage, String configFilePath, HashMap<String, Object> dataMap)
            throws Exception {
        log.debug("PASSED PARAMS-> ID:" + id + ", INPUTKEY: " + inputKey
                + ", OUTPUTKEY:" + outputKey + ", STAGE: " + stage
                + ", CONFIGFILEPATH:" + configFilePath);
        if ("pre".equalsIgnoreCase(stage)) {
            return getRecords(id, outputKey, configFilePath, dataMap);
        } else if ("post".equalsIgnoreCase(stage)) {
            return postProcess(id, inputKey, configFilePath, dataMap);
        }
        return false;
    }

    /**
     * 'pre' - processing method.
     * 
     * The Solr Query is executed and the resulting ID HashSet is stored on the
     * dataMap as the 'outputKey', along with any entries from 'includeList'
     * config entry. To manually process a certain record(s), add the id on the
     * 'includeList'. Processing will read the 'lastRun' config entry and pulls
     * records since then. <br/>
     * 
     * @param id
     * @param outputKey
     * @param configFilePath
     * @param dataMap
     * @return
     * @throws Exception
     */
    private boolean getRecords(String id, String outputKey,
            String configFilePath, HashMap<String, Object> dataMap)
            throws Exception {
        Indexer indexer = (Indexer) dataMap.get("indexer");
        JsonSimple config = new JsonSimple(new File(configFilePath));
        String solrQuery = config.getString("", "query");
        String lastRun = config.getString(null, "lastrun");
        solrQuery += (lastRun != null ? " AND create_timestamp:[" + lastRun
                + " TO NOW]" : "");
        log.debug("Using solrQuery:" + solrQuery);
        SearchRequest searchRequest = new SearchRequest(solrQuery);
        int start = 0;
        int pageSize = 10;
        searchRequest.setParam("start", "" + start);
        searchRequest.setParam("rows", "" + pageSize);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        indexer.search(searchRequest, result);
        SolrResult resultObject = new SolrResult(result.toString());
        int numFound = resultObject.getNumFound();
        log.debug("Number found:" + numFound);
        HashSet<String> newRecords = new HashSet<String>();
        while (true) {
            List<SolrDoc> results = resultObject.getResults();
            for (SolrDoc docObject : results) {
                String oid = docObject.getString(null, "id");
                if (oid != null) {
                    log.debug("Record found: " + oid);
                    newRecords.add(oid);
                } else {
                    log.debug("Record returned but has no id.");
                    log.debug(docObject.toString());
                }
            }
            start += pageSize;
            if (start > numFound) {
                break;
            }
            searchRequest.setParam("start", "" + start);
            result = new ByteArrayOutputStream();
            indexer.search(searchRequest, result);
            resultObject = new SolrResult(result.toString());
        }
        // get the exception list..
        JSONArray includedArr = config.getArray("includeList");
        if (includedArr != null && includedArr.size() > 0) {
            newRecords.addAll(includedArr);
        }
        dataMap.put(outputKey, newRecords);
        return true;
    }

    /**
     * 'post' - processing method.
     * 
     * he 'inputKey' entry in the dataMap contains ids queued for resending.
     * This is merged with the 'includeList' and persisted on the config file.
     * The 'lastRun' is updated and persisted as well.
     * 
     * @param id
     * @param inputKey
     * @param configFilePath
     * @param dataMap
     * @return
     * @throws Exception
     */
    private boolean postProcess(String id, String inputKey,
            String configFilePath, HashMap<String, Object> dataMap)
            throws Exception {
        File configFile = new File(configFilePath);
        SimpleDateFormat dtFormat = new SimpleDateFormat(
                "yyy-MM-dd'T'HH:mm:ss'Z'");
        JsonSimple config = new JsonSimple(configFile);
        config.getJsonObject().put("lastrun", dtFormat.format(new Date()));
        Collection<String> oids = (Collection<String>) dataMap.get(inputKey);
        JSONArray includedArr = config.getArray("includeList");
        if (oids != null && oids.size() > 0) {
            // some oids failed, writing it to inclusion list so it can be sent
            // next time...
            if (includedArr == null) {
                includedArr = config.writeArray("includeList");
            }
            includedArr.clear();
            for (String oid : oids) {
                includedArr.add(oid);
            }
        } else {
            // no oids failed, all good, clearing the list...
            if (includedArr != null && includedArr.size() > 0) {
                includedArr.clear();
            }
        }
        FileWriter writer = new FileWriter(configFile);
        writer.write(config.toString(true));
        writer.close();
        return true;
    }
}
