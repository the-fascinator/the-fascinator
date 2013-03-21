package com.googlecode.fascinator.portal.api.impl;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.tapestry5.services.Request;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.common.solr.SolrResult;
import com.googlecode.fascinator.portal.api.APICallHandler;
import com.googlecode.fascinator.portal.services.ScriptingServices;

public class PublishedRecordsByTypeAPICallHandlerImpl implements APICallHandler {

    private ScriptingServices scriptingServices;
    private Logger log = LoggerFactory
            .getLogger(PublishedRecordsByTypeAPICallHandlerImpl.class);

    @Override
    public String handleRequest(Request request) throws Exception {
        // DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateFromString = request.getParameter("dateFrom");
        String dateToString = request.getParameter("dateTo");

        HashSet<String> publishedOidSet = new HashSet<String>();
        Indexer indexer = scriptingServices.getIndexer();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        String eventLogQuery = "context:\"Curation\" AND eventType:\"Curation completed.\" AND eventTime:["
                + dateFromString
                + "T00:00:00.000Z TO "
                + dateToString
                + "T23:59:59.999Z]";

        SearchRequest searchRequest = new SearchRequest(eventLogQuery);
        int start = 0;
        int pageSize = 10;
        searchRequest.setParam("start", "" + start);
        searchRequest.setParam("rows", "" + pageSize);
        indexer.searchByIndex(searchRequest, result, "eventLog");
        SolrResult resultObject = new SolrResult(result.toString());
        int numFound = resultObject.getNumFound();

        while (true) {
            List<SolrDoc> results = resultObject.getResults();
            for (SolrDoc docObject : results) {
                String oid = docObject.getString(null, "oid");
                if (oid != null) {
                    publishedOidSet.add(oid);
                }
            }
            start += pageSize;
            if (start > numFound) {
                break;
            }
            searchRequest.setParam("start", "" + start);
            result = new ByteArrayOutputStream();
            indexer.searchByIndex(searchRequest, result, "eventLog");
            resultObject = new SolrResult(result.toString());
        }

        // now pull the types ...
        Map<String, Integer> typeCountMap = new HashMap<String, Integer>();
        typeCountMap.put("Activities", 0);
        typeCountMap.put("Parties_People", 0);
        typeCountMap.put("Parties_Groups", 0);
        typeCountMap.put("Services", 0);

        if (publishedOidSet.size() > 0) {
            StringBuilder query = new StringBuilder();
            query.append("id:(");
            for (String publishedOid : publishedOidSet) {
                if (query.length() > 4) {
                    query.append(" OR ");
                }
                query.append(publishedOid);
            }
            query.append(" )");
            searchRequest = new SearchRequest(query.toString());

            result = new ByteArrayOutputStream();
            start = 0;
            pageSize = 10;
            searchRequest.setParam("start", "" + start);
            searchRequest.setParam("rows", "" + pageSize);
            indexer.search(searchRequest, result);
            resultObject = new SolrResult(result.toString());
            numFound = resultObject.getNumFound();
            while (true) {
                List<SolrDoc> results = resultObject.getResults();
                for (SolrDoc docObject : results) {
                    JSONArray oaiSetArr = docObject.getArray("oai_set");
                    if (oaiSetArr != null && oaiSetArr.size() > 0) {
                        String oaiSet = (String) oaiSetArr.get(0);
                        log.debug("OAI Set for "
                                + docObject.getString("<>", "id") + " is:"
                                + oaiSet);
                        if (oaiSet != null) {
                            Integer curCount = typeCountMap.get(oaiSet);
                            // only interested on those 4 above
                            if (curCount != null) {
                                log.debug("CurCount is:" + curCount.intValue());
                                typeCountMap.put(oaiSet,
                                        new Integer(curCount.intValue() + 1));
                            } else {
                                log.debug("CurCount is NULL");
                            }
                        }
                    } else {
                        log.debug("NULL OAISET");
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
