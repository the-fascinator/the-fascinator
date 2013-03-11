package com.googlecode.fascinator.portal.api.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

public class MintStatsAPICallHandlerImpl implements APICallHandler {
    private ScriptingServices scriptingServices;
    private HashMap<String, Stat> statMap;
    private JsonSimple config;
    private String strDateFormat = "dd/MM/yyyy";
    private String solrDateFormat = "yyyy-MM-dd";

    private Logger log = LoggerFactory
            .getLogger(MintStatsAPICallHandlerImpl.class);

    public MintStatsAPICallHandlerImpl() {

    }

    public JsonSimple getConfig() {
        return config;
    }

    public void setConfig(JsonSimple config) {
        this.config = config;
    }

    private void init() {
        log.debug("Initializing MintStatsAPICallHandlerImpl...");
        statMap = new HashMap<String, Stat>();
        JsonObject jsonStat = config.getObject("stats");
        if (jsonStat != null) {
            log.debug(jsonStat.toJSONString());
            String name = (String) jsonStat.get("name");
            String query = (String) jsonStat.get("query");
            List<String> fq = config.getStringList("stats", "params", "fq");
            String rows = config.getString("0", "stats", "params", "rows");
            Stat stat = new Stat(name, query, fq, rows);
            statMap.put(name, stat);
            JSONArray fieldsArray = config.getArray("stats", "fields");
            for (Object fieldObj : fieldsArray) {
                JsonObject field = (JsonObject) fieldObj;
                String resultName = (String) field.get("name");
                String label = (String) field.get("label");
                String solr_field = (String) field.get("solr_field");
                String solr_field_value = (String) field
                        .get("solr_field_value");
                StatResult result = new StatResult(resultName + "-"
                        + solr_field_value, resultName, label, solr_field,
                        solr_field_value);
                String groupBy = (String) field.get("groupby");
                result.setGroupBy(groupBy);
                stat.addResult(result);
            }
        }
        log.debug("Initialized MintStatsAPICallHandlerImpl.");
    }

    @Override
    public String handleRequest(Request request) throws Exception {
        if (statMap == null) {
            init();
        }
        String appendFilter = request.getParameter("appendFilter");
        appendFilter = appendFilter == null ? "" : " AND " + appendFilter;
        Indexer indexer = scriptingServices.getIndexer();
        for (String key : statMap.keySet()) {
            Stat stat = statMap.get(key);
            stat.resetCounts();
            String query = stat.getQuery() + appendFilter;
            log.debug("Using query:" + query);
            SearchRequest solrReq = new SearchRequest(query);
            int start = 0;
            int pageSize = Integer.valueOf(stat.getRows());
            solrReq.setParam("fq", stat.getFq());
            solrReq.setParam("rows", "" + pageSize);
            solrReq.setParam("start", "" + start);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            indexer.search(solrReq, result);
            SolrResult resultObject = new SolrResult(new ByteArrayInputStream(
                    result.toByteArray()));
            int numFound = resultObject.getNumFound();
            stat.setResultCount("mint-total-", numFound);
            log.debug("numFound:" + numFound);
            while (numFound > 0) {
                List<SolrDoc> results = resultObject.getResults();
                for (SolrDoc docObject : results) {
                    for (String resName : stat.getResultsByName().keySet()) {
                        StatResult statRes = stat.getResultByName(resName);
                        String solrFld = statRes.getSolrField();
                        String value = docObject.getString(null, solrFld);
                        if (!solrFld.equals("numFound")) {
                            if (value == null) {
                                JSONArray valueArr = docObject
                                        .getArray(solrFld);
                                if (valueArr != null) {
                                    value = (String) valueArr.get(0);
                                } else {
                                    log.error("Value not found for solr field:"
                                            + solrFld);
                                }
                            }
                            if (resName.indexOf(":") > 0) {
                                if (statRes.getSolrFieldValue()
                                        .equalsIgnoreCase(value)) {
                                    String groupBy = statRes.getGroupBy();
                                    String uniqueVal = docObject.getString(
                                            null, groupBy);
                                    if (uniqueVal == null) {
                                        JSONArray valueArr = docObject
                                                .getArray(groupBy);
                                        if (valueArr != null) {
                                            uniqueVal = (String) valueArr
                                                    .get(0);
                                        } else {
                                            log.error("Value not found for group by field:"
                                                    + groupBy);
                                        }
                                    }

                                    Integer curCountObj = stat
                                            .getResultByName(resName)
                                            .getGroupMap().get(uniqueVal);
                                    int curCount = 0;
                                    if (curCountObj != null) {
                                        curCount = curCountObj.intValue();
                                    }
                                    stat.getResultByName(resName)
                                            .setGroupValues(uniqueVal,
                                                    new Integer(++curCount));
                                }
                            } else {
                                String resultKey = resName + "-" + value;
                                statRes = stat.getResults().get(resultKey);
                                if (statRes != null) {
                                    statRes.incCounts();
                                }
                            }
                        }

                    }
                }
                start += pageSize;
                if (start > numFound) {
                    break;
                }
                solrReq.setParam("start", "" + start);
                result = new ByteArrayOutputStream();
                indexer.search(solrReq, result);
                resultObject = new SolrResult(new ByteArrayInputStream(
                        result.toByteArray()));
            }
        }
        HashMap<String, Object> resMap = new HashMap<String, Object>();
        for (String statKey : statMap.keySet()) {
            log.debug("For stat key: " + statKey);
            Stat stat = statMap.get(statKey);
            for (String resKey : stat.getResults().keySet()) {
                StatResult statRes = stat.getResults().get(resKey);
                if (statRes.getName().indexOf(":") > 0) {
                    resMap.put(statRes.getLabel(), new ArrayList<String>(
                            statRes.getGroupList()));
                    HashMap<String, String> groupMap = new HashMap<String, String>();
                    for (String valKey : statRes.getGroupMap().keySet()) {
                        groupMap.put(valKey, statRes.getGroupMap().get(valKey)
                                .toString());
                    }
                    resMap.put(statRes.getLabel() + "counts", groupMap);
                } else {
                    resMap.put(statRes.getLabel(),
                            String.valueOf(statRes.getCounts()));
                }

                log.debug("Result label:" + statRes.getLabel()
                        + " has counts: " + statRes.getCounts());
            }

        }
        return (new JsonObject(resMap)).toJSONString();
    }

    @Override
    public void setScriptingServices(ScriptingServices scriptingServices) {
        this.scriptingServices = scriptingServices;
    }

    class Stat {
        private String name;
        private String query;
        private List<String> fq;
        private HashMap<String, StatResult> results;
        private Set<String> fields;
        private HashMap<String, StatResult> resultsByName;
        private String rows;

        public Stat(String name, String query, List<String> fq, String rows) {
            results = new HashMap<String, StatResult>();
            resultsByName = new HashMap<String, StatResult>();
            fields = new HashSet<String>();
            this.name = name;
            this.query = query;
            this.fq = fq;
            this.rows = rows;
        }

        public void resetCounts() {
            for (String key : results.keySet()) {
                StatResult statResult = results.get(key);
                statResult.setCounts(0);
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public List<String> getFq() {
            return fq;
        }

        public void setFq(List<String> fq) {
            this.fq = fq;
        }

        public Map<String, StatResult> getResults() {
            return results;
        }

        public String getRows() {
            return rows;
        }

        public void setRows(String rows) {
            this.rows = rows;
        }

        public void addResult(StatResult result) {
            results.put(result.getKey(), result);
            fields.add(result.getSolrField());
            resultsByName.put(result.getName(), result);
        }

        public void setResultCount(String key, int counts) {
            results.get(key).setCounts(counts);
        }

        public Set<String> getFields() {
            return fields;
        }

        public StatResult getResultByName(String name) {
            return resultsByName.get(name);
        }

        public HashMap<String, StatResult> getResultsByName() {
            return resultsByName;
        }

        public void setResultsByName(HashMap<String, StatResult> resultsByName) {
            this.resultsByName = resultsByName;
        }
    }

    class StatResult {
        private String key;
        private String name;
        private String label;
        private String solrField;
        private String solrFieldValue;
        private int counts;
        private HashMap<String, Integer> groupMap;
        private HashSet<String> groupList;
        private String groupBy;

        public StatResult(String key, String name, String label, String field,
                String value) {
            this.key = key;
            this.name = name;
            this.label = label;
            solrField = field;
            solrFieldValue = value;
            counts = 0;
            groupMap = new HashMap<String, Integer>();
            groupList = new HashSet<String>();
        }

        public void setGroupValues(String key, Integer count) {
            groupList.add(key);
            groupMap.put(key, count);
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getSolrField() {
            return solrField;
        }

        public void setSolrField(String solrField) {
            this.solrField = solrField;
        }

        public String getSolrFieldValue() {
            return solrFieldValue;
        }

        public void setSolrFieldValue(String solrFieldValue) {
            this.solrFieldValue = solrFieldValue;
        }

        public int getCounts() {
            return groupMap.size() > 0 ? groupMap.size() : counts;
        }

        public void setCounts(int counts) {
            this.counts = counts;
        }

        public void incCounts() {
            counts++;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public HashMap<String, Integer> getGroupMap() {
            return groupMap;
        }

        public void setGroupMap(HashMap<String, Integer> groupMap) {
            this.groupMap = groupMap;
        }

        public String getGroupBy() {
            return groupBy;
        }

        public void setGroupBy(String groupBy) {
            this.groupBy = groupBy;
        }

        public HashSet<String> getGroupList() {
            return groupList;
        }

        public void setGroupList(HashSet<String> groupList) {
            this.groupList = groupList;
        }
    }
}