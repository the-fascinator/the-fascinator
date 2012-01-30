/*
 * The Fascinator - Python Utils
 * Copyright (C) 2008-2011 University of Southern Queensland
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

package com.googlecode.fascinator.common;

import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.access.AccessControlException;
import com.googlecode.fascinator.api.access.AccessControlManager;
import com.googlecode.fascinator.api.access.AccessControlSchema;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.PayloadType;
import com.googlecode.fascinator.api.storage.StorageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.io.SAXReader;
import org.ontoware.rdf2go.Reasoning;
import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.impl.jena24.ModelImplJena24;
import org.ontoware.rdf2go.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to expose common Java classes and methods we use
 * to Python scripts.
 * 
 * Messaging is a duplicate of com.googlecode.fascinator.MessagingServices since
 * Common library cannot acces it.
 * 
 * @author Greg Pendlebury
 */
public class PythonUtils {
    private static Logger log = LoggerFactory.getLogger(PythonUtils.class);

    /** Security */
    private AccessControlManager access;

    /** XML Parsing */
    private Map<String, String> namespaces;
    private SAXReader saxReader;

    /** Message Queues */
    private ActiveMQConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private MessageProducer producer;
    private Map<String, Destination> destinations;

    public PythonUtils(JsonSimpleConfig config) throws PluginException {
        // Security
        String accessControlType = "accessmanager";
        access = PluginManager.getAccessManager(accessControlType);
        access.init(config.toString());

        // XML parsing
        namespaces = new HashMap<String, String>();
        DocumentFactory docFactory = new DocumentFactory();
        docFactory.setXPathNamespaceURIs(namespaces);
        saxReader = new SAXReader(docFactory);

        // Message Queues
        String brokerUrl = config.getString(
                ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL,
                "messaging", "url");
        connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create single producer for multiple destinations
            producer = session.createProducer(null);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // cache destinations
            destinations = new HashMap<String, Destination>();
        } catch (JMSException ex) {
            throw new PluginException(ex);
        }
    }

    // Static lists of mime type substrings used during indexing
    private static final Set<String> majors = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(new String[] {
                "audio", "video", "image"})));
    private static final Set<String> wordMinors = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(new String[] {
                "vnd.ms-word",
                "vnd.oasis.opendocument.text",
                "vnd.openxmlformats-officedocument.wordprocessingml"})));
    private static final Set<String> pptMinors = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(new String[] {
                "vnd.ms-powerpoint",
                "vnd.oasis.opendocument.presentation",
                "vnd.openxmlformats-officedocument.presentationml"})));

    /*****
     * Try to closing any objects that require closure
     * 
     */
    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close connection: {}", jmse.getMessage());
            }
        }
        if (session != null) {
            try {
                session.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close session: {}", jmse.getMessage());
            }
        }
        if (producer != null) {
            try {
                producer.close();
            } catch (JMSException jmse) {
                log.warn("Failed to close producer: {}", jmse.getMessage());
            }
        }
        if (access != null) {
            try {
                access.shutdown();
            } catch (PluginException ex) {
                log.warn("Failed shutting down access control manager:", ex);
            }
        }
    }

    /*****
     * Send a message to the given message queue
     * 
     * @param messageQueue to connect to
     * @param message to send
     * @return boolean flag for success
     */
    public boolean sendMessage(String messageQueue, String message) {
        try {
            // log.debug("Queuing '{}' to '{}'", msg, name);
            sendMessage(getDestination(messageQueue, true), message);
            return true;
        } catch (JMSException jmse) {
            log.error("Failed to queue message", jmse);
            return false;
        }
    }

    /**
     * Sends a message to a JMS destination.
     * 
     * @param name destination name
     * @param msg message to send
     */
    private void sendMessage(Destination destination, String msg)
            throws JMSException {
        TextMessage message = session.createTextMessage(msg);
        producer.send(destination, message);
    }

    /**
     * Gets a JMS destination with the given name. If the destination doesn't
     * exist it is created and cached for reuse.
     * 
     * @param name name of the destination
     * @param queue true if the destination is a queue, false for topic
     * @return a JMS destination
     * @throws JMSException if an error occurred creating the destination
     */
    private Destination getDestination(String name, boolean queue)
            throws JMSException {
        Destination destination = destinations.get(name);
        if (destination == null) {
            destination = queue ? session.createQueue(name) : session
                    .createTopic(name);
            destinations.put(name, destination);
        }
        return destination;
    }

    /*****
     * Get a resource from one of the compiled classes on the classpath
     * 
     * @param path To the requested resource
     * @return InputStream to the resource
     */
    public InputStream getResource(String path) {
        return getClass().getResourceAsStream(path);
    }

    /*****
     * Parse an XML document stored in a payload
     * 
     * @param payload holding the document
     * @return Document object after parsing
     */
    public Document getXmlDocument(Payload payload) {
        try {
            Document doc = getXmlDocument(payload.open());
            payload.close();
            return doc;
        } catch (StorageException ex) {
            log.error("Failed to access payload", ex);
        }
        return null;
    }

    /*****
     * Parse an XML document from a string
     * 
     * @param xmlData to parse
     * @return Document object after parsing
     */
    public Document getXmlDocument(String xmlData) {
        Reader reader = null;
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(xmlData
                    .getBytes("utf-8"));
            return saxReader.read(in);
        } catch (UnsupportedEncodingException uee) {
        } catch (DocumentException de) {
            log.error("Failed to parse XML", de);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                }
            }
        }
        return null;
    }

    /*****
     * Parse an XML document from an inputstream
     * 
     * @param xmlIn, the inputstream to read and parse
     * @return Document object after parsing
     */
    public Document getXmlDocument(InputStream xmlIn) {
        Reader reader = null;
        try {
            reader = new InputStreamReader(xmlIn, "UTF-8");
            return saxReader.read(reader);
        } catch (UnsupportedEncodingException uee) {
        } catch (DocumentException de) {
            log.error("Failed to parse XML", de);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                }
            }
        }
        return null;
    }

    /*****
     * Register a namespace for our XML parser
     * 
     * @param prefix of the namespace
     * @param uri of the namespace
     */
    public void registerNamespace(String prefix, String uri) {
        namespaces.put(prefix, uri);
    }

    /*****
     * UN-register a namespace for our XML parser
     * 
     * @param prefix of the namespace
     */
    public void unregisterNamespace(String prefix) {
        namespaces.remove(prefix);
    }

    /*****
     * Parse a JSON object from an inputstream
     * 
     * @param in, the inputstream to read and parse
     * @return JsonConfigHelper object after parsing
     */
    public JsonSimple getJsonObject(InputStream in) {
        try {
            return new JsonSimple(in);
        } catch (IOException ex) {
            log.error("Failure during stream access", ex);
            return null;
        }
    }

    /*****
     * Parse RDF data stored in a payload
     * 
     * @param payload containing the data
     * @return Model object after parsing
     */
    public Model getRdfModel(Payload payload) {
        try {
            Model model = getRdfModel(payload.open());
            payload.close();
            return model;
        } catch (StorageException ioe) {
            log.info("Failed to read payload stream", ioe);
        }
        return null;
    }

    /*****
     * Parse RDF data from an inputstream
     * 
     * @param rdfIn, the inputstream to read and parse
     * @return Model object after parsing
     */
    public Model getRdfModel(InputStream rdfIn) {
        Model model = null;
        Reader reader = null;
        try {
            reader = new InputStreamReader(rdfIn, "UTF-8");
            model = new ModelImplJena24(Reasoning.rdfs);
            model.open();
            model.readFrom(reader);
        } catch (ModelRuntimeException mre) {
            log.error("Failed to create RDF model", mre);
        } catch (IOException ioe) {
            log.error("Failed to read RDF input", ioe);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                }
            }
        }
        return model;
    }

    /*****
     * Return an empty access control schema from the given plugin
     * 
     * @param plugin to request the schema from
     * @return AccessControlSchema returned by the plugin
     */
    public AccessControlSchema getAccessSchema(String plugin) {
        if (access == null) {
            return null;
        }
        access.setActivePlugin(plugin);
        return access.getEmptySchema();
    }

    /*****
     * Submit a new access control schema to a security plugin
     * 
     * @param schema to submit
     * @param plugin to submit to
     */
    public void setAccessSchema(AccessControlSchema schema, String plugin) {
        if (access == null) {
            return;
        }

        try {
            access.setActivePlugin(plugin);
            access.applySchema(schema);
        } catch (AccessControlException ex) {
            log.error("Failed to add new access schema", ex);
        }
    }

    /*****
     * Remove an access control schema from a security plugin
     * 
     * @param schema to remove
     * @param plugin to remove to
     */
    public void removeAccessSchema(AccessControlSchema schema, String plugin) {
        if (access == null) {
            return;
        }

        try {
            access.setActivePlugin(plugin);
            access.removeSchema(schema);
        } catch (AccessControlException ex) {
            log.error("Failed to revoke existing access schema", ex);
        }
    }

    /*****
     * Find the list of roles with access to the given object
     * 
     * @param recordId the object to query
     * @return List<String> of roles with access to the object
     */
    public List<String> getRolesWithAccess(String recordId) {
        if (access == null) {
            return null;
        }
        try {
            return access.getRoles(recordId);
        } catch (AccessControlException ex) {
            log.error("Failed to query security plugin for roles", ex);
            return null;
        }
    }

    /*****
     * Find the list of roles with access to the given object, but only looking
     * at a single plugin.
     *
     * @param recordId the object to query
     * @param plugin the plugin we are interested in
     * @return List<String> of roles with access to the object
     */
    public List<String> getRolesWithAccess(String recordId, String plugin) {
        if (access == null) {
            return null;
        }
        try {
            List<String> roles = new ArrayList<String>();
            access.setActivePlugin(plugin);
            List<AccessControlSchema> schemas = access.getSchemas(recordId);
            for (AccessControlSchema schema : schemas) {
                String role = schema.get("role");
                if (role != null) {
                    roles.add(role);
                }
            }
            return roles;
        } catch (AccessControlException ex) {
            log.error("Failed to query security plugin for roles", ex);
            return null;
        }
    }

    /*****
     * Find the MIME type to use at display time, giving first priority to the
     * preview payload, then to the source payload.
     *
     * @param indexerFormats The list of types so far allocated by the rules script.
     * @param object The object being indexed.
     * @param preview The preview payload
     * @return String The MIME type to be used at display time.
     */
    public String getDisplayMimeType(String[] indexerFormats,
            DigitalObject object, String preview) {
        // The source should be the first type given by the indexer
        String result = indexerFormats[0];

        // Lets look for a preview payload
        if (preview != null) {
            try {
                Payload payload = object.getPayload(preview);
                PayloadType type = payload.getType();
                if (type != null && type.equals(PayloadType.Preview)) {
                    if (payload.getContentType() != null) {
                        result = payload.getContentType();
                    }
                }
            } catch (StorageException ex) {
                log.error("Error accessing payload: '{}'", preview, ex);
            }
        }

        return result;
    }

    /*****
     * A basic method for selecting common display templates from a given MIME
     * type. This simple algorithm is suitable for most rules files.
     *
     * @param preview The MIME type.
     * @return String The display type.
     */
    public String basicDisplayType(String mimeType) {
        String[] parts = mimeType.split("/");
        // Invalid or unknown MIME types
        if (parts == null || parts.length != 2) {
            return "default";
        }
        // Otherwise, decide based on different parts
        String major = parts[0];
        String minor = parts[1];

        // Top level templates (like 'video')
        if (majors.contains(major)) {
            return major;
        }
        // Common applications
        if (major.equals("application")) {
            // PDF
            if (minor.equals("pdf")) {
                return minor;
            }
            // Word processors
            if (wordMinors.contains(minor)) {
                return "word-processing";
            }
            // Presentations
            if (pptMinors.contains(minor)) {
                return "presentation";
            }
            if (minor.equals("x-fascinator-package")) {
                return "package";
            }
        }
        // Text based: eg. 'plain', 'html'
        if (major.equals("text")) {
            return minor;
        }
        // Unknown
        return "default";
    }

    /*****
     * Add the provided key/value pair into the index.
     *
     * @param index : Data structure to add data into
     * @param field : The field name
     * @param value : The value to store
     */
    public void add(Map<String, List<String>> index, String field, String value) {
        // Adding data
        if (index.containsKey(field)) {
            index.get(field).add(value);
        // New data
        } else {
            List<String> newList = new ArrayList<String>();
            newList.add(value);
            index.put(field, newList);
        }
    }

    /*****
     * Generate a Solr document from a map of provided key/value pairs.
     *
     * @param fields : The lists of evaluated fields for the document
     * @return String : The generated XML snippet
     */
    public String solrDocument(Map<String, List<String>> fields) {
        String result = "<doc>";
        for (String field : fields.keySet()) {
            for (String value : fields.get(field)) {
                result += solrField(field, value);
            }
        }
        result += "</doc>";
        return result;
    }

    /*****
     * Generate an XML snippet representing a key/value pair in a Solr doc.
     *
     * @param field : The field
     * @param value : The value
     * @return String : The generated XML snippet
     */
    public String solrField(String field, String value) {
        if (field == null || value == null) return null;
        return "<field name=\"" + field + "\">" +
                StringEscapeUtils.escapeXml(value) + "</field>";
    }
}
