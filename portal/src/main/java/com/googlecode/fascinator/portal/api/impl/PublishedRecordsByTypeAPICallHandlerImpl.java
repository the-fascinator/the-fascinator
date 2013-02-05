package com.googlecode.fascinator.portal.api.impl;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tapestry5.services.Request;

import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.common.solr.SolrResult;
import com.googlecode.fascinator.portal.api.APICallHandler;
import com.googlecode.fascinator.portal.services.ScriptingServices;

public class PublishedRecordsByTypeAPICallHandlerImpl implements APICallHandler {

    private ScriptingServices scriptingServices;

    @Override
    public String handleRequest(Request request) throws Exception {
        // DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateFromString = request.getParameter("dateFrom");
        String dateToString = request.getParameter("dateTo");
        Indexer indexer = scriptingServices.getIndexer();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        String query = "published:true";

        SearchRequest searchRequest = new SearchRequest(query);

        int start = 0;
        int pageSize = 10;
        searchRequest.setParam("start", "" + start);
        searchRequest.setParam("rows", "" + pageSize);
        indexer.search(searchRequest, result);
        SolrResult resultObject = new SolrResult(result.toString());
        int numFound = resultObject.getNumFound();

        Map<String, String> publishedOidMap = new HashMap<String, String>();

        while (true) {
            List<SolrDoc> results = resultObject.getResults();
            for (SolrDoc docObject : results) {
                publishedOidMap.put(docObject.get("storage_id"),
                        (String) docObject.getArray("oai_set").toArray()[0]);
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

        Map<String, Integer> typeCountMap = new HashMap<String, Integer>();
        typeCountMap.put("Activities", 0);
        typeCountMap.put("Parties_People", 0);
        typeCountMap.put("Parties", 0);
        typeCountMap.put("Services", 0);

        if (publishedOidMap.size() > 0) {
            Set<String> publishedOids = publishedOidMap.keySet();
            String eventLogQuery = "oid:(";
            int i = 0;
            for (String publishedOid : publishedOids) {

                eventLogQuery += publishedOid;
                if (i < publishedOidMap.size() - 1) {
                    eventLogQuery += " OR ";
                }
                i++;
            }
            eventLogQuery += ")";

            eventLogQuery += "AND eventTime:["
                    + dateFromString
                    + "T00:00:00.000Z TO "
                    + dateToString
                    + "T23:59:59.999Z] AND eventType:modify AND context:HarvestClient";

            searchRequest = new SearchRequest(eventLogQuery);
            result = new ByteArrayOutputStream();
            start = 0;
            pageSize = 10;
            searchRequest.setParam("start", "" + start);
            searchRequest.setParam("rows", "" + pageSize);
            indexer.searchByIndex(searchRequest, result, "eventLog");
            resultObject = new SolrResult(result.toString());
            numFound = resultObject.getNumFound();

            while (true) {
                List<SolrDoc> results = resultObject.getResults();
                for (SolrDoc docObject : results) {
                    String oid = docObject.getString(null, "oid");
                    if (oid != null) {
                        int count = typeCountMap.get(publishedOidMap.get(oid));
                        typeCountMap.put(publishedOidMap.get(oid), ++count);
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
        }
        return (new JsonObject(typeCountMap)).toJSONString();
    }

    @Override
    public void setScriptingServices(ScriptingServices scriptingServices) {
        this.scriptingServices = scriptingServices;
    }

}
