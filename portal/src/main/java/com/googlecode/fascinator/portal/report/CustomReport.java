package com.googlecode.fascinator.portal.report;

import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;

public class CustomReport extends Report {

    private String strDateFormat;

    public CustomReport() {
        super();
    }

    public CustomReport(String name, String label) {
        super(name, label);
        strDateFormat = "dd/MM/yyyy";
    }

    public CustomReport(JsonSimple config) {
        super(config);
        strDateFormat = config.getString("dd/MM/yyyy", "report", "dateFormat");
    }

    /**
     * Generates the report specific query from parameters
     */
    @Override
    public String getQueryAsString() {
        return "";
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

}