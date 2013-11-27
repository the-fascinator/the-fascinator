package com.googlecode.fascinator.portal.quartz;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.portal.process.Processor;

/**
 * Glue code to execute processing sets from a Quartz job.
 * 
 * @author Shilo Banihit
 * 
 */
public class ProcessingSetJob implements StatefulJob {

    /** Logging */
    private Logger log = LoggerFactory.getLogger(ProcessingSetJob.class);

    /** Job name */
    private String name;

    /**
     * Main execute method.
     * 
     */
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        // Get data about our job
        name = context.getJobDetail().getName();
        log.debug("ProcessingSetJob executing:" + name);
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        JsonSimple jobConfig = (JsonSimple) dataMap.get("jobConfig");
        String configPath = jobConfig.getString("", "configFile");
        JsonSimple procConfig = null;
        try {
            File procConfigFile = new File(configPath);
            if (!procConfigFile.exists()) {
                log.error("Config file does not exist: " + configPath);
                throw new JobExecutionException("Config file does not exist:"
                        + configPath);
            }
            procConfig = new JsonSimple(procConfigFile);
        } catch (IOException e) {
            log.error("Error loading config file: {}", configPath, e);
            throw new JobExecutionException(e);
        }
        String setId = jobConfig.getString("", "setId");
        log.debug("Using setId: " + setId);
        for (Object procObj : procConfig.getJsonArray()) {
            JsonSimple pconfig = new JsonSimple((JsonObject) procObj);
            String procId = pconfig.getString("", "id");
            HashMap<String, Object> procDataMap = new HashMap<String, Object>();
            procDataMap.put("indexer", dataMap.get("indexer"));
            try {
                if ("".equals(setId)) {
                    execProc(procId, pconfig, procDataMap);
                } else {
                    if (setId.equalsIgnoreCase(procId)) {
                        execProc(procId, pconfig, procDataMap);
                    }
                }
            } catch (Exception e) {
                log.error("Error executing processing set:" + procId, e);
                throw new JobExecutionException(e);
            }
        }
    }

    /**
     * Execute a processing set.
     * 
     * @param procId
     * @param config
     * @param dataMap
     * @throws Exception
     */
    private void execProc(String procId, JsonSimple config,
            HashMap<String, Object> dataMap) throws Exception {
        execProcStage(procId, config, dataMap, "pre");
        execProcStage(procId, config, dataMap, "main");
        execProcStage(procId, config, dataMap, "post");
    }

    /**
     * Execute a processing set stage.
     * 
     * @param procId
     * @param configJson
     * @param dataMap
     * @param stageName
     * @throws Exception
     */
    private void execProcStage(String procId, JsonSimple configJson,
            HashMap<String, Object> dataMap, String stageName) throws Exception {
        for (Object procObj : configJson.getArray(stageName)) {
            JsonSimple procJson = new JsonSimple((JsonObject) procObj);
            String procClassName = procJson.getString("", "class");
            String procConfigPath = procJson.getString("", "config");
            String procInputKey = procJson.getString("", "inputKey");
            String procOutputKey = procJson.getString("", "outputKey");
            log.debug("Executing procId: " + procId + ", using class: "
                    + procClassName + ", with stage: " + stageName);
            try {
                Class procClass = Class.forName(procClassName);
                Processor procInst = (Processor) procClass.newInstance();
                dataMap.put(procClassName, procInst);
                procInst.process(procId, procInputKey, procOutputKey,
                        stageName, procConfigPath, dataMap);
            } catch (Exception e) {
                throw e;
            }
        }
    }
}
