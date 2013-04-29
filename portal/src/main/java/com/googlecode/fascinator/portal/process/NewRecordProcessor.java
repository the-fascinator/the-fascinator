package com.googlecode.fascinator.portal.process;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.common.solr.SolrResult;

public class NewRecordProcessor implements Processor {

    private Logger log = LoggerFactory.getLogger(NewRecordProcessor.class);
    
    @Override
    public boolean process(String id, String inputKey, String outputKey,
            String stage, String configFilePath, HashMap dataMap) throws Exception {
        log.debug("PASSED PARAMS-> ID:" + id + ", INPUTKEY: " + inputKey + ", OUTPUTKEY:" + outputKey + ", STAGE: " + stage + ", CONFIGFILEPATH:" + configFilePath);
        if ("pre".equalsIgnoreCase(stage)) {
            return getNewRecords(id, outputKey, configFilePath, dataMap);
        } else
        if ("post".equalsIgnoreCase(stage)) {
            return postProcess(id, inputKey, configFilePath, dataMap);
        }
        return false;
    }

    private boolean getNewRecords(String id, String outputKey, String configFilePath, HashMap dataMap) throws Exception 
    {
       Indexer indexer = (Indexer) dataMap.get("indexer");
       JsonSimple config = new JsonSimple(new File(configFilePath));
       String solrQuery = config.getString("","query");
       String lastRun = config.getString(null, "lastrun");
       solrQuery += (lastRun != null ? " AND create_timestamp:["+lastRun+" TO NOW]" : "");
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
       ArrayList<String> newRecords = new ArrayList<String>();
       while(true) {
           List<SolrDoc> results = resultObject.getResults();
           for (SolrDoc docObject : results) {
               String oid = docObject.getString(null, "id");
               if (oid != null) {
                   log.debug("New record found: "+oid);
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
    
    private boolean postProcess(String id, String inputKey, String configFilePath, HashMap dataMap) throws Exception 
    {
        File configFile = new File(configFilePath);
        SimpleDateFormat dtFormat = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss'Z'");
        JsonSimple config = new JsonSimple(configFile);
        config.getJsonObject().put("lastrun", dtFormat.format(new Date()));
        List<String> oids = (List<String>) dataMap.get(inputKey);
        JSONArray includedArr = config.getArray("includeList");
        if (oids != null && oids.size() > 0) {
            // some oids failed, writing it to inclusion list so it can be sent next time...
            if (includedArr == null) {
                includedArr = config.writeArray("includeList");
            }
            includedArr.clear();
            for (String oid:oids) {
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