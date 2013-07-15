/*******************************************************************************
 * Copyright (C) 2013 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
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
 ******************************************************************************/
package com.googlecode.fascinator.portal.process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.common.solr.SolrResult;

/**
 * Template-aware email utility class.
 * 
 * 
 * @author Shilo Banihit
 * 
 */
public class EmailNotifier implements Processor {

    private Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    private String host;
    private String port;
    private String tls;
    private String ssl;
    private String username;
    private String password;

    /**
     * Initialises this instance.
     * 
     * @param config
     */
    private void init(JsonSimple config) {
        host = config.getString("", "host");
        port = config.getString("", "port");
        username = config.getString("", "username");
        password = config.getString("", "password");
        tls = config.getString("false", "tls");
        ssl = config.getString("false", "ssl");
        Properties props = System.getProperties();

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", tls);

    }

    /**
     * Replaces any variables in the templates using the mapping specified in
     * the config.
     * 
     * @param solrDoc
     * @param text
     * @param vars
     * @param config
     * @return
     */
    private String replaceVars(SolrDoc solrDoc, String text, List<String> vars,
            JsonSimple config) {
        for (String var : vars) {
            String varField = config.getString("", "mapping", var);
            log.debug("Replacing '" + var + "' using field '" + varField + "'");
            String replacement = solrDoc.getString(var, varField);
            if (replacement == null || "".equals(replacement)) {
                JSONArray arr = solrDoc.getArray(varField);
                if (arr != null) {
                    replacement = (String) arr.get(0);
                    if (replacement == null) {
                        // giving up, setting back to source value so caller can
                        // evaluate
                        replacement = var;
                    }
                } else {
                    // giving up, setting back to source value so caller can
                    // evaluate
                    replacement = var;
                }
            }
            text = text.replace(var, replacement);
        }
        return text;
    }

    /**
     * Main processing method. This class can comprise of multiple email
     * configuration blocks, specified by 'id' array.
     * 
     */
    @Override
    public boolean process(String id, String inputKey, String outputKey,
            String stage, String configFilePath, HashMap<String, Object> dataMap)
            throws Exception {
        log.debug("Email notifier starting:" + id);
        JsonSimple config = new JsonSimple(new File(configFilePath));
        init(config);

        Indexer indexer = (Indexer) dataMap.get("indexer");

        HashSet<String> failedOids = (HashSet<String>) dataMap.get(outputKey);
        if (failedOids == null) {
            failedOids = new HashSet<String>();
        }
        Collection<String> oids = (Collection<String>) dataMap.get(inputKey);

        JSONArray emailConfigBlocks = config.getArray(id);
        if (emailConfigBlocks != null) {
            for (Object configBlockObj : emailConfigBlocks) {
                JsonSimple emailConfig = new JsonSimple(
                        (JsonObject) configBlockObj);
                doEmail(outputKey, dataMap, emailConfig, indexer, failedOids,
                        oids);
            }
        } else {
            doEmail(outputKey, dataMap, config, indexer, failedOids, oids);
        }
        dataMap.put(outputKey, failedOids);
        return true;
    }

    /**
     * Process a particular email config.
     * 
     * @param outputKey
     * @param dataMap
     * @param config
     * @param indexer
     * @param failedOids
     * @param oids
     * @throws IndexerException
     * @throws IOException
     */
    private void doEmail(String outputKey, HashMap dataMap, JsonSimple config,
            Indexer indexer, HashSet<String> failedOids, Collection<String> oids)
            throws IndexerException, IOException {
        String subjectTemplate = config.getString("", "subject");
        String bodyTemplate = config.getString("", "body");
        List<String> vars = config.getStringList("vars");
        log.debug("Email step with subject template:" + subjectTemplate);
        for (String oid : oids) {
            log.debug("Sending email notification for oid:" + oid);
            // get the solr doc
            SearchRequest searchRequest = new SearchRequest("id:" + oid);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            indexer.search(searchRequest, result);
            SolrResult resultObject = new SolrResult(result.toString());
            List<SolrDoc> results = resultObject.getResults();
            SolrDoc solrDoc = results.get(0);
            String subject = replaceVars(solrDoc, subjectTemplate, vars, config);
            String body = replaceVars(solrDoc, bodyTemplate, vars, config);
            String to = config.getString("", "to");
            String from = config.getString("", "from");
            String recipient = to;
            if (to.startsWith("$")) {
                recipient = replaceVars(solrDoc, to, vars, config);
                if (recipient.startsWith("$")) {
                    // exception encountered...
                    log.error("Failed to build the email recipient:'"
                            + recipient
                            + "'. Please check the mapping field and verify that it exists and is populated in Solr.");
                    failedOids.add(oid);
                    continue;
                }
            }
            if (!email(oid, from, recipient, subject, body)) {
                failedOids.add(oid);
            }
        }
    }

    /**
     * Send the actual email.
     * 
     * @param oid
     * @param from
     * @param recipient
     * @param subject
     * @param body
     * @return
     */
    private boolean email(String oid, String from, String recipient,
            String subject, String body) {
        try {
            Email email = new SimpleEmail();
            log.debug("Email host: " + host);
            log.debug("Email port: " + port);
            log.debug("Email username: " + username);
            log.debug("Email from: " + from);
            log.debug("Email to: " + recipient);
            log.debug("Email Subject is: " + subject);
            log.debug("Email Body is: " + body);
            email.setHostName(host);
            email.setSmtpPort(Integer.parseInt(port));
            email.setAuthenticator(new DefaultAuthenticator(username, password));
            // the method setSSL is deprecated on the newer versions of commons
            // email...
            email.setSSL("true".equalsIgnoreCase(ssl));
            email.setTLS("true".equalsIgnoreCase(tls));
            email.setFrom(from);
            email.setSubject(subject);
            email.setMsg(body);
            if (recipient.indexOf(",") >= 0) {
                String[] recs = recipient.split(",");
                for (String rec : recs) {
                    email.addTo(rec);
                }
            } else {
                email.addTo(recipient);
            }
            email.send();
        } catch (Exception ex) {
            log.debug("Error sending notification mail for oid:" + oid, ex);
            return false;
        }
        return true;
    }
}
