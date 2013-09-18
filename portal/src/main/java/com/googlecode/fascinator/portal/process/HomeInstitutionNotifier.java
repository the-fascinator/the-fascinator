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
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.common.solr.SolrResult;

/**
 * @author Shilo Banihit
 * 
 */
public class HomeInstitutionNotifier implements Processor {
    private Logger log = LoggerFactory.getLogger(HomeInstitutionNotifier.class);

    /* (non-Javadoc)
     * @see com.googlecode.fascinator.portal.process.Processor#process(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.HashMap)
     */
    @Override
    public boolean process(String id, String inputKey, String outputKey,
            String stage, String configFilePath, HashMap<String, Object> dataMap)
            throws Exception {
        Indexer indexer = (Indexer) dataMap.get("indexer");

        HashSet<String> failedOids = (HashSet<String>) dataMap.get(outputKey);
        if (failedOids == null) {
            failedOids = new HashSet<String>();
        }

        Collection<String> oids = (Collection<String>) dataMap.get(inputKey);

        // load up the list of institutions...
        JsonSimple config = new JsonSimple(new File(configFilePath));
        HashMap<String, JsonObject> homes = new HashMap<String, JsonObject>();
        for (Object homeObj : config.getArray("institutions")) {
            JsonObject home = (JsonObject) homeObj;
            homes.put((String) home.get("name"), home);
        }

        File sysFile = JsonSimpleConfig.getSystemFile();
        Storage storage = PluginManager.getStorage("file-system");
        storage.init(sysFile);
        ByteArrayOutputStream out;
        String targetPayload = "arms.xml";
        String targetProperty = "dataprovider:organization";

        for (String oid : oids) {
            log.debug("Processing oid:" + oid);
            out = null;
            // get the solr doc
            SearchRequest searchRequest = new SearchRequest("id:" + oid);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            indexer.search(searchRequest, result);
            SolrResult resultObject = new SolrResult(result.toString());
            List<SolrDoc> results = resultObject.getResults();
            SolrDoc solrDoc = results.get(0);

            // get the target property
            String targetProp = solrDoc.getString("", targetProperty);
            if (targetProp == null) {
                JSONArray array = solrDoc.getArray(targetProperty);
                if (array.size() > 0) {
                    targetProp = (String) array.get(0);
                }
            }
            log.debug("Target property: " + targetProp);
            if (targetProp != null && targetProp.length() > 0) {
                String channel = (String) homes.get(targetProp).get("channel");
                log.debug("Using channel:" + channel);
                if (channel != null) {
                    String subjectTemplate = config.getString("", channel,
                            "subject");
                    String bodyTemplate = config.getString("", channel, "body");
                    List<String> vars = config.getStringList(channel, "vars");

                    VelocityContext context = new VelocityContext();
                    initVars(solrDoc, vars, config, context, channel);

                    String subject = evaluateStr(subjectTemplate, context);
                    String body = evaluateStr(bodyTemplate, context);
                    String to = config.getString("", channel, "to");
                    String from = config.getString("", channel, "from");
                    String recipient = evaluateStr(to, context);
                    String attachDesc = config.getString("", channel,
                            "attachDesc");
                    String attachType = config.getString("", channel,
                            "attachType");
                    if (recipient.startsWith("$")) {
                        // exception encountered...
                        log.error("Failed to build the email recipient:'"
                                + recipient
                                + "'. Please check the mapping field and verify that it exists and is populated in Solr.");
                        failedOids.add(oid);
                        continue;
                    }

                    // send the message to the channel
                    log.debug("Sending message to channel: " + channel);
                    // find the proper payload
                    DigitalObject object = storage.getObject(oid);
                    Payload payload = object.getPayload(targetPayload);
                    if (payload != null) {
                        out = new ByteArrayOutputStream();
                        IOUtils.copy(payload.open(), out);
                        payload.close();
                    } else {
                        log.debug("Target payload not found:" + targetPayload);
                    }
                    if (out != null) {
                        EmailNotifier notifier = (EmailNotifier) dataMap
                                .get(EmailNotifier.class.getName());

                        if (!notifier.emailAttachment(recipient, from, subject,
                                body, out.toByteArray(), attachType,
                                targetPayload, attachDesc)) {
                            failedOids.add(oid);
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Replaces any variables in the templates using the mapping specified in
     * the config.
     * 
     * @param solrDoc
     * @param vars
     * @param config
     * @param context
     */
    private void initVars(SolrDoc solrDoc, List<String> vars,
            JsonSimple config, VelocityContext context, String channel) {
        for (String var : vars) {
            String varField = config.getString("", channel, "mapping", var);
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
            log.debug("Getting variable value '" + var + "' using field '"
                    + varField + "', value:" + replacement);
            context.put(var.replace("$", ""), replacement);
        }
    }

    private String evaluateStr(String source, VelocityContext context)
            throws ParseErrorException, MethodInvocationException,
            ResourceNotFoundException, IOException {
        StringWriter writer = new StringWriter();
        Velocity.evaluate(context, writer, "evaluateStr", source);
        return writer.toString();
    }
}
