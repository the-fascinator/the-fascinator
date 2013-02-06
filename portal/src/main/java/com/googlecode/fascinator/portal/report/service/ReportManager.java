package com.googlecode.fascinator.portal.report.service;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.portal.report.CustomReport;
import com.googlecode.fascinator.portal.report.Report;
import com.googlecode.fascinator.portal.services.FascinatorService;
import com.ibm.icu.util.Calendar;

public class ReportManager implements FascinatorService {

    private Logger log = LoggerFactory.getLogger(ReportManager.class);

    private JsonSimple config;
    private File reportDir;
    private TreeMap<String, Report> reports;

    @Override
    public JsonSimple getConfig() {
        return config;
    }

    @Override
    public void setConfig(JsonSimple config) {
        this.config = config;
    }

    @Override
    public void init() {
        log.debug("Initializing ReportManager...");
        reportDir = new File(config.getString(null, "config", "home"));
        if (!reportDir.exists()) {
            log.debug("Creating report directory structure...");
            if (!reportDir.mkdirs()) {
                log.error("Error creating report directory structure");
                return;
            }
            // TODO: remove these "default" reports when done...
            // creating the "default" reports...
            CustomReport rptWithoutCitations = new CustomReport(
                    "ReportWithoutCitations", "Records Without Citations");
            SimpleDateFormat dtFormatterWithoutCitations = new SimpleDateFormat(
                    rptWithoutCitations.getStrDateFormat());

            Calendar curCal = Calendar.getInstance();
            curCal.set(Calendar.DATE, 1);
            curCal.set(Calendar.MONTH, Calendar.JANUARY);

            rptWithoutCitations.setQueryFilterVal("dateCreatedFrom",
                    dtFormatterWithoutCitations.format(curCal.getTime()),
                    "createFrom", "Date Created - From");
            rptWithoutCitations.setQueryFilterVal("dateCreatedTo",
                    dtFormatterWithoutCitations.format(curCal.getTime()),
                    "createTo", "Date Created - To");
            rptWithoutCitations.setQueryFilterVal("reportStatus",
                    "All Records", "rptStatus", "Show Records");
            saveReport(rptWithoutCitations);

            CustomReport rptEmbargoed = new CustomReport("EmbargoedReports",
                    "Embargoed Reports");
            rptEmbargoed.setQueryFilterVal("dateCreatedFrom",
                    dtFormatterWithoutCitations.format(curCal.getTime()),
                    "createFrom", "Date Created - From");
            rptEmbargoed.setQueryFilterVal("dateCreatedTo",
                    dtFormatterWithoutCitations.format(curCal.getTime()),
                    "createTo", "Date Created - To");
            rptEmbargoed.setQueryFilterVal("reportStatus", "All Records",
                    "rptStatus", "Show Records");
            saveReport(rptEmbargoed);
        }
        reports = new TreeMap<String, Report>();
        try {
            loadReports();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadReports() throws Exception {
        log.debug("Loading reports...");
        for (File reportConfigFile : reportDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                String name = file.getName();
                return !file.isDirectory() && !name.equals(".svn");
            }
        })) {
            loadReport(reportConfigFile);
        }
    }

    private void loadReport(File reportConfigFile) throws Exception {
        JsonSimple reportConfig = new JsonSimple(reportConfigFile);
        String reportName = reportConfig.getString(null, "report", "name");
        String reportClass = reportConfig
                .getString(null, "report", "className");
        log.debug("Loading report: " + reportName + ", using class:"
                + reportClass);
        Report report = (Report) Class.forName(reportClass).newInstance();
        report.setConfig(reportConfig);
        reports.put(reportName, report);
        log.debug("Successfully loaded report:" + reportName);
    }

    public synchronized void addReport(Report report) {
        reports.put(report.getReportName(), report);
    }

    public synchronized void saveReport(Report report) {
        String name = report.getReportName();
        File reportConfigFile = new File(reportDir, name + ".json");
        try {
            FileWriter writer = new FileWriter(reportConfigFile);
            writer.write(report.toJsonString());
            writer.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public synchronized void deleteReport(String reportName) {
        File reportConfigFile = new File(reportDir, reportName + ".json");
        if (reportConfigFile.delete()) {
            log.debug("Successfully deleted report:" + reportName);
        } else {
            log.error("Failed to delete report:" + reportName);
            return;
        }
        reports.remove(reportName);
    }

    public synchronized TreeMap<String, Report> getReports() {
        return reports;
    }
}