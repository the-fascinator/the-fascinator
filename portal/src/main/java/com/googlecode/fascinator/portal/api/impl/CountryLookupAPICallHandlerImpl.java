package com.googlecode.fascinator.portal.api.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.tapestry5.services.Request;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.common.solr.SolrResult;
import com.googlecode.fascinator.portal.api.APICallHandler;
import com.googlecode.fascinator.portal.services.ScriptingServices;

public class CountryLookupAPICallHandlerImpl implements APICallHandler {
    private ScriptingServices scriptingServices;
    private Logger log = LoggerFactory
            .getLogger(CountryLookupAPICallHandlerImpl.class);
    private static int MAX_ROW_COUNT = 1000;
    private static int MAX_PAGE_COUNT = 1000; 
    
    public CountryLookupAPICallHandlerImpl() {

    }

    @Override
    public String handleRequest(Request request) throws Exception {
        Indexer indexer = scriptingServices.getIndexer();        
        String func = request.getParameter("func");
        if ("detail".equals(func)) {
        	return getDetails(request, indexer);
        }   
        return listAll(indexer);
    }
    
    private String getDetails(Request request, Indexer indexer) throws Exception {    	
    	String id = request.getParameter("id");
    	if (id == null || "".equals(id)) {
    		return "Invalid identifier";
    	}
    	id = id.replace(":","\\:");
    	if (id.indexOf(",") != -1) {
    		String[] ids = id.split(",");
    		StringBuilder strIds = new StringBuilder();
    		for (String idEntry : ids) {
    			if (strIds.length() > 0) {
    				strIds.append(" OR ");
    			}
    			strIds.append(idEntry);
    		}
    		id = strIds.toString();
    	} 
    	SearchRequest req = new SearchRequest("repository_name:\"Countries\" AND (dc_identifier:(" + id + "))");	
        req.setParam("fl", "dc_title,dc_identifier");
        req.setParam("rows", String.valueOf(MAX_ROW_COUNT));        
        return getEntries(req, indexer);
    }
    
    private String listAll(Indexer indexer) throws Exception {    	
    	SearchRequest req = new SearchRequest("repository_name:\"Countries\"");
        req.setParam("fl", "dc_title,dc_identifier");
        req.setParam("rows", String.valueOf(MAX_ROW_COUNT));
        req.setParam("sort","dc_title asc");
        return getEntries(req, indexer);
    }
    
    private String getEntries(SearchRequest req, Indexer indexer) throws Exception {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	indexer.search(req, out);
        SolrResult res = new SolrResult(new ByteArrayInputStream(out.toByteArray()));
        int numFound = res.getNumFound();
        int start = 0;
        int pageSize = MAX_PAGE_COUNT;
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("[");
        strBuilder.append(System.getProperty("line.separator"));
        int numCount = 0;
        while (true) {
            List<SolrDoc> results = res.getResults();
            for (SolrDoc docObject : results) {
            	String dc_title = docObject.getString(null, "dc_title");
            	JSONArray dc_identifier = docObject.getArray("dc_identifier");
            	if (dc_identifier != null && dc_identifier.get(0) !=null) {
	            	if (numCount > 0) {
	            		strBuilder.append(",");
	            		strBuilder.append(System.getProperty("line.separator"));
	            	}
	            	strBuilder.append("{\"label\":\"");
	            	strBuilder.append(dc_title);
	            	strBuilder.append("\",\"value\":\"");            	            	
	            	strBuilder.append((String)dc_identifier.get(0));            	             		
	            	strBuilder.append("\"}");
	            	numCount++;
            	} else {
            		log.error("Null dc_identfier:" + dc_title);
            	}
            }
            start += pageSize;
            if (start > numFound) {
                break;
            }
            req.setParam("start", "" + start);
            out = new ByteArrayOutputStream();
            indexer.search(req, out);
            res = new SolrResult(new ByteArrayInputStream(out.toByteArray()));
        }
        strBuilder.append(System.getProperty("line.separator"));
        strBuilder.append("]");
        return strBuilder.toString();
    }

    @Override
    public void setScriptingServices(ScriptingServices scriptingServices) {
        this.scriptingServices = scriptingServices;
    }
}