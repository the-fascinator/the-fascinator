/*
 * The Fascinator - Portal - House Keeping Jobs
 * Copyright (C) 2010-2011 University of Southern Queensland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.googlecode.fascinator.portal.quartz;

import com.googlecode.fascinator.HarvestClient;
import com.googlecode.fascinator.api.PluginException;

import java.io.File;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz job to run a harvest at routine intervals. This class implements the
 * StatefulJob interface and will never run concurrently.
 *
 * @author Greg Pendlebury
 */
public class HarvestJob implements StatefulJob {

    /** Logging */
    private Logger log = LoggerFactory.getLogger(HarvestJob.class);

    /** Job name */
    private String name;

    /** The harvest client */
    private HarvestClient harvester;

    /** Config file */
    private File config;

    /**
     * This method will be called by quartz when the job trigger fires.
     *
     * @param context The execution context of this job, including data.
     */
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {

        // Get data about our job
        name = context.getJobDetail().getName();
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        // Where is out config file?
        String configFile = dataMap.getString("configFile");
        if (configFile == null) {
            log.error("No configuration provided");
            return;
        }

        // Make sure it is really there
        config = new File(configFile);
        if (config == null || !config.exists()) {
            log.error("Error finding config file: '{}'", configFile);
            return;
        }

        // Instantiate the harvester
        try {
            harvester = new HarvestClient(config);
        } catch (PluginException pe) {
            log.error("Failed to initialise harvester: ", pe);
            return;
        }

        // Run the actual job
        runJob();
    }

    /**
     * The real work happens here
     *
     */
    private void runJob() {
        log.info("========================================");
        log.info("Executing harvest job: '{}'", name);
        try {
            harvester.start();
        } catch (PluginException pe) {
            log.error("Error during harvest: ", pe);
        }
        log.info("Completed harvest job: '{}'", name);
        log.info("========================================");
    }
}