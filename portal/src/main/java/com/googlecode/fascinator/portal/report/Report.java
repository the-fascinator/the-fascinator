package com.googlecode.fascinator.portal.report;

import java.io.File;
import java.io.IOException;

import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;

public abstract class Report {

    protected JsonSimple config;
    protected String reportName;
    protected String label;
    protected JsonObject query;
    protected String configPath;

    protected static String FLD_QUERY = "query";
    protected static String FLD_QUERY_FILTER = "filter";
    protected static String FLD_QUERY_FILTER_VALUE = "value";
    protected static String FLD_QUERY_FILTER_UIKEY = "uiKey";
    protected static String FLD_QUERY_FILTER_COND = "queryCond";
    protected static String FLD_QUERY_FILTER_COND_AND = "and";
    protected static String FLD_QUERY_FILTER_COND_OR = "or";

    public Report() {
        config = new JsonSimple();
    }

    public Report(String reportName, String label) {
        this.reportName = reportName;
        this.label = label;
        config = new JsonSimple();
    }

    public Report(File reportConfigFile) throws IOException {
        this(new JsonSimple(reportConfigFile));
    }

    public Report(JsonSimple config) {
        setConfig(config);
    }

    public void setConfig(JsonSimple config) {
        this.config = config;
        reportName = config.getString("UnnamedReport", "report", "name");
        label = config.getString("Unnamed Report", "report", "label");
    }

    protected JsonObject getQueryFilter(String key) {
        return config.writeObject(FLD_QUERY, FLD_QUERY_FILTER, key);
    }

    public synchronized void setQueryFilterVal(String key, Object value,
            String uiKey, String uiLabel) {
        JsonObject param = getQueryFilter(key);
        param.put("value", value);
        param.put("uiKey", uiKey);
        param.put("uiLabel", uiLabel);
    }

    public synchronized Object getQueryFilterVal(String key) {
        return config.getString(null, FLD_QUERY, FLD_QUERY_FILTER, key,
                FLD_QUERY_FILTER_VALUE);
    }

    public synchronized String getQueryUiKey(String key) {
        return config.getString(null, FLD_QUERY, FLD_QUERY_FILTER, key,
                FLD_QUERY_FILTER_UIKEY);
    }

    public synchronized void addAndQueryCond(String filterName) {
        addQueryCond(FLD_QUERY_FILTER_COND_AND, filterName);
    }

    public synchronized void addOrQueryCond(String filterName) {
        addQueryCond(FLD_QUERY_FILTER_COND_OR, filterName);
    }

    protected synchronized void addQueryCond(String cond, String filterName) {
        JsonObject newCond = config.writeObject(FLD_QUERY,
                FLD_QUERY_FILTER_COND, cond, -1);
        newCond.put("filterName", filterName);
    }

    public synchronized String toJsonString() {
        JsonObject reportObj = config.writeObject("report");
        reportObj.put("name", reportName);
        reportObj.put("label", label);
        return config.toString(true);
    }

    public abstract String getQueryAsString();

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }
}