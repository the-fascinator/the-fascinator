package com.googlecode.fascinator.portal.report;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.googlecode.fascinator.common.FascinatorHome;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;

public class RedboxReport extends Report {

    private static final String AND_OPERATOR = " AND ";
    private String strDateFormat;
    private String solrDateFormat = "yyyy-MM-dd";
    private JsonSimple reportCriteriaOptionsJson;

    public RedboxReport() throws IOException {
        super();
        strDateFormat = "dd/MM/yyyy";
        reportCriteriaOptionsJson = new JsonSimple(new File(
                FascinatorHome.getPath("reports")
                        + "/reportCriteriaOptions.json"));
    }

    public RedboxReport(String name, String label) throws IOException {
        super(name, label);
        strDateFormat = "dd/MM/yyyy";
        reportCriteriaOptionsJson = new JsonSimple(new File(
                FascinatorHome.getPath("reports")
                        + "/reportCriteriaOptions.json"));
    }

    public RedboxReport(JsonSimple config) throws IOException {
        super(config);
        strDateFormat = config.getString("dd/MM/yyyy", "report", "dateFormat");
    }

    /**
     * Generates the report specific query from parameters
     */
    @Override
    public String getQueryAsString() {
        String query = "";
        JsonObject queryFilters = config.getObject("query", "filter");
        String[] keyArray = Arrays.copyOf(
                new ArrayList<Object>(queryFilters.keySet()).toArray(),
                queryFilters.keySet().size(), String[].class);
        List<String> keys = Arrays.asList(keyArray);
        java.util.Collections.sort(keys);
        reportCriteriaOptionsJson.getArray("results");
        query += processDateCriteria(queryFilters);
        query += processShowCriteria(queryFilters);
        int i = 1;
        while (true) {
            if (keys.indexOf("report-criteria." + i + ".dropdown") == -1) {
                break;
            }
            query += processReportCriteria(queryFilters, i);
            i++;
        }
        return query;
    }

    private String processShowCriteria(JsonObject queryFilters) {
        String showOption = (String) ((JsonObject) queryFilters
                .get("showOption")).get("value");
        if ("published".equals(showOption)) {
            return AND_OPERATOR + "published:true";
        }
        return "";
    }

    private String processDateCriteria(JsonObject queryFilters) {
        String dateCriteriaQuery = "";
        String dateType = (String) ((JsonObject) queryFilters
                .get("dateCreatedModified")).get("value");
        if ("created".equals(dateType)) {
            dateCriteriaQuery += "date_created:";
        } else {
            dateCriteriaQuery += "last_modified:";
        }
        DateFormat queryDateFormatter = new SimpleDateFormat(strDateFormat);
        DateFormat solrDateFormatter = new SimpleDateFormat(solrDateFormat);
        String dateFrom, dateTo;
        try {
            dateFrom = solrDateFormatter.format(queryDateFormatter
                    .parse((String) ((JsonObject) queryFilters.get("dateFrom"))
                            .get("value")));

            dateTo = solrDateFormatter.format(queryDateFormatter
                    .parse((String) ((JsonObject) queryFilters.get("dateTo"))
                            .get("value")));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        dateCriteriaQuery += "[" + dateFrom + "T00:00:00.000Z TO " + dateTo
                + "T23:59:59.999Z] ";
        return dateCriteriaQuery;
    }

    private String processReportCriteria(JsonObject queryFilters, int index) {
        String keyName = (String) ((JsonObject) queryFilters
                .get("report-criteria." + index + ".dropdown-input"))
                .get("value");

        JsonObject reportCriteria = findJsonObjectWithKey(keyName);

        String criteriaValue = (String) ((JsonObject) queryFilters
                .get("report-criteria." + index + ".searchcomponent"))
                .get("value");

        String solrField = (String) reportCriteria.get("solrField");
        String queryString = solrField + ":";

        String matchContainsValue = (String) ((JsonObject) queryFilters
                .get("report-criteria." + index + ".match_contains"))
                .get("value");

        if ("field_contains".equals(matchContainsValue)) {
            criteriaValue = criteriaValue + "*";
        }
        queryString = queryString + "\"" + criteriaValue + "\"";

        String includeNullsValue = (String) ((JsonObject) queryFilters
                .get("report-criteria." + index + ".include_nulls"))
                .get("value");

        if ("field_include_null".equals(includeNullsValue)) {
            queryString = "(" + queryString + " OR (-" + solrField
                    + ":[* TO *] AND *:*))";
        }

        String logicOpValue = (String) ((JsonObject) queryFilters
                .get("report-criteria." + index + ".logicalOp")).get("value");
        String logicOperand = AND_OPERATOR;
        if ("OR".equals(logicOpValue)) {
            logicOperand = " OR ";
        }
        return logicOperand + queryString;
    }

    private JsonObject findJsonObjectWithKey(String keyName) {
        Object[] reportCriteriaOptions = reportCriteriaOptionsJson.getArray(
                "results").toArray();
        for (Object object : reportCriteriaOptions) {
            JsonObject jsonObject = (JsonObject) object;
            if (keyName.equals(jsonObject.get("key"))) {
                return jsonObject;
            }
        }

        return null;
    }

    @Override
    public synchronized String toJsonString() {
        JsonObject reportObj = config.writeObject("report");
        reportObj.put("dateFormat", strDateFormat);
        reportObj.put("className", this.getClass().getName());
        return super.toJsonString();
    }

    public String getStrDateFormat() {
        return strDateFormat;
    }

    public void setStrDateFormat(String strDateFormat) {
        this.strDateFormat = strDateFormat;
    }

    public JsonSimple getConfig() {
        return config;
    }

}