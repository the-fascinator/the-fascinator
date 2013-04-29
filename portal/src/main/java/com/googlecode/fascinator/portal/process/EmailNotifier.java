package com.googlecode.fascinator.portal.process;

import com.googlecode.fascinator.messaging.TransactionManagerQueueConsumer;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.common.solr.SolrResult;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.messaging.EmailNotificationConsumer;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.messaging.MessagingServices;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.SimpleEmail;

import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class EmailNotifier implements Processor {

    private Logger log = LoggerFactory.getLogger(EmailNotifier.class);
    
    private String host;
    private String port;
    private String tls;
    private String ssl;
    private String from;
    private String to;
    private String username;
    private String password;
    
    private void init(JsonSimple config) {
        host = config.getString("", "host");
        port = config.getString("", "port");
        from = config.getString("", "from");
        to = config.getString("", "to");
        username = config.getString("", "username");
        password = config.getString("", "password");
        tls = config.getString("false", "tls");
        ssl = config.getString("false", "ssl");
        Properties props = System.getProperties();

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", tls);
        
    }
    
    private String replaceVars(SolrDoc solrDoc, String text, List<String> vars, JsonSimple config) {
        for (String var : vars) {
            String varField = config.getString("", "mapping", var);
            log.debug("Replacing '" + var + "' using field '" + varField + "'");
            String replacement = solrDoc.getString(var, varField);
            if (replacement == null || "".equals(replacement)) {
                JSONArray arr = solrDoc.getArray(varField);
                if (arr != null) {
                    replacement = (String) arr.get(0);
                    if (replacement == null) {
                        // giving up, setting back to source value so caller can evaluate
                        replacement = var;
                    }
                } else {
                    // giving up, setting back to source value so caller can evaluate
                    replacement = var;
                }
            }
            text = text.replace(var, replacement);
        }
        return text;
    }
    
    @Override
    public boolean process(String id, String inputKey, String outputKey,
            String stage, String configFilePath, HashMap dataMap)
            throws Exception {

        JsonSimple config = new JsonSimple(new File(configFilePath));
        init(config);

        Indexer indexer = (Indexer) dataMap.get("indexer");
        
        ArrayList<String> failedOids = new ArrayList<String>();
        List<String> oids = (List<String>) dataMap.get(inputKey);
        String subjectTemplate = config.getString("", "subject");
        String bodyTemplate = config.getString("", "body");
        List<String> vars = config.getStringList("vars");
        for (String oid : oids) {
            log.debug("Sending email notification for oid:"+oid);
            // get the solr doc 
            SearchRequest searchRequest = new SearchRequest("id:" + oid);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            indexer.search(searchRequest, result);
            SolrResult resultObject = new SolrResult(result.toString());
            List<SolrDoc> results = resultObject.getResults();
            SolrDoc solrDoc = results.get(0);
            String subject = replaceVars(solrDoc, subjectTemplate, vars, config);
            String body = replaceVars(solrDoc, bodyTemplate, vars, config);
            String recipient = to;
            if (to.startsWith("$")) {
                recipient = replaceVars(solrDoc, to, vars, config);
                if (recipient.startsWith("$")) {
                    // exception encountered...
                    log.error("Failed to build the email recipient:'" + recipient + "'. Please check the mapping field and verify that it exists and is populated in Solr.");
                    failedOids.add(oid);
                    continue;
                }
            }
            if (!email(oid, recipient, subject, body)) {
                failedOids.add(oid);
            }
        }
        dataMap.put(outputKey, failedOids);
        return true;
    }
    
    private boolean email(String oid, String recipient, String subject, String body) {
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
            // the method setSSL is deprecated on the newer versions of commons email...
            email.setSSL("true".equalsIgnoreCase(ssl));
            email.setTLS("true".equalsIgnoreCase(tls));
            email.setFrom(from);
            email.setSubject(subject);
            email.setMsg(body);
            if (recipient.indexOf(",") >= 0) {
                String[] recs= recipient.split(",");
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