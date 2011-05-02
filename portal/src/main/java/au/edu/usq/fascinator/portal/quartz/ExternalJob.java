/*
 * The Fascinator - Portal - House Keeping Jobs
 * Copyright (C) 2010 University of Southern Queensland
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
package au.edu.usq.fascinator.portal.quartz;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.codec.digest.DigestUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz job to call an external script via the web.
 *
 * @author Greg Pendlebury
 */
public class ExternalJob implements Job {

    /** Logging */
    private Logger log = LoggerFactory.getLogger(ExternalJob.class);

    /** The name of this job */
    private String name;

    /** External URL */
    private URL url;

    /** Security token */
    private String token;

    /** Stop invalid executions */
    private boolean broken;

    /**
     * This method will be called by quartz when the job trigger fires.
     *
     * @param context The execution context of this job, including data.
     */
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        broken = false;
        // Get data about our job
        name = context.getJobDetail().getName();
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        // Make sure the URL is valid
        String urlString = dataMap.getString("url");
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            // This should never fail, we validated the
            //  url back in housekeeping
            broken = true;
            log.error("URL is invalid: ", ex);
        }
        token = dataMap.getString("token");
        // MD5 hash our token if we have it
        if (token != null) {
            token = DigestUtils.md5Hex(token);
        }

        if (!broken) {
            runJob();
        }
    }

    /**
     * The real work happens here
     *
     */
    private void runJob() {
        HttpURLConnection conn = null;
        log.debug("Job firing: '{}'", name);

        try {
            // Open tasks... much simpler
            if (token == null) {
                conn = (HttpURLConnection) url.openConnection();

            // Secure tasks
            } else {
                String param = "token=" + URLEncoder.encode(token, "UTF-8");

                // Prepare our request
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", "" +
                        Integer.toString(param.getBytes().length));
                conn.setUseCaches(false);
                conn.setDoOutput(true);

                // Send request
                DataOutputStream wr = new DataOutputStream(
                        conn.getOutputStream());
                wr.writeBytes(param);
                wr.flush();
                wr.close();
            }

            // Get Response
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                log.error("Error hitting external script: {}",
                        conn.getResponseMessage());
            }
        } catch (IOException ex) {
            log.error("Error connecting to URL: ", ex);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}