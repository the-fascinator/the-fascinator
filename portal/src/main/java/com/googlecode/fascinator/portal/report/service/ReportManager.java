package com.googlecode.fascinator.portal.report.service;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.portal.report.Report;
import com.googlecode.fascinator.portal.services.FascinatorService;

public class ReportManager implements FascinatorService {

    private Logger log = LoggerFactory.getLogger(ReportManager.class);

    private JsonSimple config;
    private File reportDir;
    private TreeMap<String, Report> reports;
    private HashMap<String, Report> reportsByLabel;
    private Random random;
    private SimpleDateFormat dtFormatter;

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
        random = new Random(System.currentTimeMillis());
        dtFormatter = new SimpleDateFormat("dd/mm/yyyy HH:mm:ss:SSSS");

        reportDir = new File(config.getString(null, "config", "home"));
        if (!reportDir.exists()) {
            log.debug("Creating report directory structure...");
            if (!reportDir.mkdirs()) {
                log.error("Error creating report directory structure");
                return;
            }
        }
        reports = new TreeMap<String, Report>();
        reportsByLabel = new HashMap<String, Report>();
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
            loadAndAddReport(reportConfigFile);
        }
    }

    private void loadAndAddReport(File reportConfigFile) throws Exception {
        JsonSimple reportConfig = new JsonSimple(reportConfigFile);
        String reportName = reportConfig.getString(null, "report", "name");
        String reportLabel = reportConfig.getString(null, "report", "label");
        String reportClass = reportConfig
                .getString(null, "report", "className");
        log.debug("Loading report: " + reportName + ", using class:"
                + reportClass);
        Report report = (Report) Class.forName(reportClass).newInstance();
        report.setConfig(reportConfig);
        report.setConfigPath(reportConfigFile.getAbsolutePath());
        reports.put(reportName, report);
        reportsByLabel.put(reportLabel, report);
        log.debug("Successfully loaded and added report:" + reportName);
    }

    private Report loadReport(String configPath) throws Exception {
        JsonSimple reportConfig = new JsonSimple(new File(configPath));
        String reportName = reportConfig.getString(null, "report", "name");
        String reportClass = reportConfig
                .getString(null, "report", "className");
        log.debug("Loading report: " + reportName + ", using class:"
                + reportClass);
        Report report = (Report) Class.forName(reportClass).newInstance();
        report.setConfig(reportConfig);
        report.setConfigPath(configPath);
        log.debug("Successfully loaded report:" + reportName);
        return report;
    }

    /**
     * Method to add report to the map. Must be called before saveReport()
     * 
     * @param report
     */
    public synchronized void addReport(Report report) throws Exception {
        report.setReportName(getUniqueReportName(report.getReportName()));
        reports.put(report.getReportName(), report);
        reportsByLabel.put(report.getLabel(), report);
    }

    /**
     * Make sure to call addReport() before saving...
     * 
     * @param report
     */
    public synchronized void saveReport(Report report) {
        String name = report.getReportName() + ".json";
        File reportConfigFile = new File(reportDir, name);
        try {
            FileWriter writer = new FileWriter(reportConfigFile);
            writer.write(report.toJsonString());
            writer.close();
            if (report.getConfigPath() == null) {
                report.setConfigPath(reportConfigFile.getAbsolutePath());
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public synchronized void duplicateReport(String reportName)
            throws Exception {
        Report rptSource = getReport(reportName);
        Report rptCopy = loadReport(rptSource.getConfigPath());
        String copyLabel = getNextCopyLabel(rptCopy.getLabel());
        rptCopy.setReportName(copyLabel.replaceAll(" ", ""));
        rptCopy.setLabel(copyLabel);
        rptCopy.setConfigPath(null);
        saveReport(rptCopy);
        addReport(rptCopy);
    }

    public synchronized void deleteReport(String reportName) {
        Report report = getReport(reportName);
        File reportConfigFile = new File(report.getConfigPath());
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

    public synchronized Report getReport(String reportName) {
        return reports.get(reportName);
    }

    public synchronized Report getReportByLabel(String label) {
        return reportsByLabel.get(label);
    }

    private String getNextCopyLabel(String label) {
        int copynum = 0;
        Report curCopy = null;
        String nextLabel = null;
        do {
            copynum++;
            nextLabel = (copynum == 1 ? "Copy" : "Copy-" + copynum) + " Of "
                    + label;
            curCopy = getReportByLabel(nextLabel);
        } while (curCopy != null);
        return nextLabel;
    }

    public String getUniqueReportName(String reportName) throws Exception {
        Calendar curCal = Calendar.getInstance();
        String name = URLEncoder.encode(
                DigestUtils.md5Hex(reportName
                        + dtFormatter.format(curCal.getTime())
                        + random.nextFloat()), "utf-8");
        return name;
    }
}