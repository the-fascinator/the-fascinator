/*
 * The Fascinator - Plugin - Transformer - Aperture
 * Copyright (C) 2009-2011  University of Southern Queensland
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
package com.googlecode.fascinator.transformer.aperture;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.PayloadType;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.api.transformer.Transformer;
import com.googlecode.fascinator.api.transformer.TransformerException;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.MimeTypeUtil;
import com.googlecode.fascinator.common.storage.StorageUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Syntax;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.semanticdesktop.aperture.extractor.Extractor;
import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.semanticdesktop.aperture.extractor.ExtractorFactory;
import org.semanticdesktop.aperture.extractor.ExtractorRegistry;
import org.semanticdesktop.aperture.extractor.FileExtractor;
import org.semanticdesktop.aperture.extractor.FileExtractorFactory;
import org.semanticdesktop.aperture.extractor.impl.DefaultExtractorRegistry;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.rdf.impl.RDFContainerImpl;
import org.semanticdesktop.aperture.vocabulary.NIE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * In this plugin <a href="http://aperture.sourceforge.net/">Aperture
 * Framework</a> is utilised to extract RDF metadata and full-text from the
 * DigitalObject.
 * </p>
 * 
 * <h3>Configuration</h3>
 * 
 * <table border="1">
 * <tr>
 * <th>Option</th>
 * <th>Description</th>
 * <th>Required</th>
 * <th>Default</th>
 * </tr>
 * 
 * <tr>
 * <td>id</td>
 * <td>Transformer Id</td>
 * <td><b>Yes</b></td>
 * <td>aperture</td>
 * </tr>
 * 
 * <tr>
 * <td>outputPath</td>
 * <td>Path where the aperture will store the temporary files</td>
 * <td><b>Yes</b></td>
 * <td>${java.io.tmpdir}/${user.name}/ice2-output</td>
 * </tr>
 * 
 * </table>
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * Aperture transformer with "${java.io.tmpdir}/${user.name}/ice2-output"
 * specified as the outputPath
 * 
 * <pre>
 * "aperture": {
 *     "id": "aperture",
 *     "outputPath": "${java.io.tmpdir}/${user.name}/ice2-output"
 * }
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * <a href=
 * "https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Plugins/Transformer/Aperture"
 * >https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Plugins/
 * Transformer/Aperture</a>
 * </p>
 * 
 * Presently, only local files are accessible.
 * 
 * @see <a href="http://aperture.wiki.sourceforge.net/Extractors">Aperture
 *      Extractors Tutorial</a>
 * @see <a href="http://www.semanticdesktop.org/ontologies/nie/">NEPOMUK
 *      Information Element Ontology</a>
 * 
 * @author Duncan Dickinson, Linda Octalina
 */
public class ApertureTransformer implements Transformer {
    /** Logger */
    private static Logger log = LoggerFactory
            .getLogger(ApertureTransformer.class);

    /** Json config file **/
    private JsonSimpleConfig config;

    /** Caching directory **/
    private String outputPath = "";

    /** Flag for first execution */
    private boolean firstRun = true;

    /**
     * Testing interface. Takes a file name as either a local file path (e.g.
     * /tmp/me.txt or c:\tmp\me.txt) or an file:// URL (e.g. file:///tmp/me.txt)
     * and returns an RDF/XML representation of the metadata and full-text to
     * standard out.
     * 
     * Note: For large files (esp PDF) this can take a while
     * 
     * @param args The file you wish to process
     */
    public static void main(String[] args) {
        // check if a commandline argument was specified
        if (args.length == 0) {
            System.err.println("Extractor\nUsage: java Extractor <file>");
            System.exit(-1);
        }
        try {
            String sourceId = "/tmp/test.jpg";
            RDFContainer rdf = extractRDF(args[0], sourceId);
            if (rdf != null) {
                System.out.println(rdf.getModel().serialize(Syntax.RdfXml));
            } else {
                System.out.println("Cannot locate file");
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println("EXCEPTION");
            e.printStackTrace();
        }
    }

    /**
     * Extractor Constructor
     */
    public ApertureTransformer() {
    }

    /**
     * Overridden method init to initialize
     * 
     * Configuration sample: "transformer": { "conveyer":
     * "aperture-extractor, ice-transformer", "extractor": { "outputPath" :
     * "${user.home}/ice2-output" }, "ice-transformer": { "url":
     * "http://ice-service.usq.edu.au/api/convert/", "outputPath":
     * "${user.home}/ice2-output" } }
     * 
     * @param jsonString of configuration for Extractor
     * @throws PluginException if fail to parse the config
     */
    @Override
    public void init(String jsonString) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonString);
            reset();
        } catch (IOException e) {
            throw new PluginException(e);
        }
    }

    /**
     * Overridden method init to initialize
     * 
     * Configuration sample: "transformer": { "conveyer":
     * "aperture-extractor, ice-transformer", "extractor": { "outputPath" :
     * "${user.home}/ice2-output" }, "ice-transformer": { "url":
     * "http://ice-service.usq.edu.au/api/convert/", "outputPath":
     * "${user.home}/ice2-output" } }
     * 
     * @param jsonFile to retrieve the configuration for Extractor
     * @throws PluginException if fail to read the config file
     */
    @Override
    public void init(File jsonFile) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonFile);
            reset();
        } catch (IOException e) {
            throw new PluginException(e);
        }
    }

    /**
     * Reset the transformer in preparation for a new object
     */
    private void reset() throws TransformerException {
        if (firstRun) {
            firstRun = false;
            log.info("--Initializing Extractor plugin--");
            // Cache directory
            outputPath = config.getString(System.getProperty("java.io.tmpdir"),
                    "aperture", "outputPath");
        }
    }

    /**
     * Extracts RDF from a file denoted by a String-based descriptor (ie path)
     * 
     * @param file The file to be extracted
     * @return An RDFContainer holding the extracted RDF
     * @throws IOException
     * @throws ExtractorException
     * @throws URISyntaxException
     */
    public static RDFContainer extractRDF(String file, String sourceId)
            throws IOException, ExtractorException, URISyntaxException {
        File f = getFile(file);
        if (f != null) {
            return extractRDF(f, sourceId);
        }
        return null;
    }

    /**
     * Utility function to resolve file:// URL's to a Java File object. If
     * passed a local file path this function just puts it into a file object.
     * 
     * The following file paths (should) work:
     * <ul>
     * <li>/tmp/test\ 1.txt</li>
     * <li>file:///tmp/test%201.txt</li>
     * </ul>
     * 
     * @param file
     * @return A File object
     * @throws URISyntaxException
     */
    public static File getFile(String file) throws URISyntaxException {
        // We need to see if the file path is a URL
        File f = null;
        try {
            URL url = new URL(file);
            if (url.getProtocol().equals("file")) {
                f = new File(url.toURI());
            }
        } catch (MalformedURLException e) {
            // it may be c:\a\b\c or /a/b/c so it's
            // still legitimate (maybe)
            f = new File(file);
        }
        if (f == null) {
            return null;
        }
        if (!f.exists()) {
            return null;
        }
        return f;
    }

    /**
     * Extracts RDF from the given File object. This function will handle
     * MIME-type identification using Aperture.
     * 
     * @see <a
     *      href="http://aperture.wiki.sourceforge.net/MIMETypeIdentification">Aperture
     *      MIME Type Identification Tutorial</a>
     * @param file
     * @return
     * @throws IOException
     * @throws ExtractorException
     */
    public static RDFContainer extractRDF(File file, String sourceId)
            throws IOException, ExtractorException {
        String mimeType = MimeTypeUtil.getMimeType(file);
        if (mimeType == null) {
            log.error("MIME Type = NULL, skipping RDF extraction.");
            return null;
        }
        return extractRDF(file, mimeType, sourceId);
    }

    /**
     * Extracts RDF from the given File object, using the provided MIME Type
     * rather than trying to work it out
     * 
     * @param file
     * @param mimeType
     * @return
     * @throws IOException
     * @throws ExtractorException
     */
    public static RDFContainer extractRDF(File file, String mimeType,
            String sourceId) throws IOException, ExtractorException {
        RDFContainer container = createRDFContainer(sourceId);
        determineExtractor(file, mimeType, container);
        return container;
    }

    /**
     * Create the RDFContainer that will hold the RDF model
     * 
     * @param file The file to be analysed
     * @return
     */
    private static RDFContainer createRDFContainer(String sourceId) {
        URI uri = new URIImpl(sourceId);
        Model model = RDF2Go.getModelFactory().createModel();
        model.open();
        return new RDFContainerImpl(model, uri);
    }

    /**
     * Takes the requested file and mime type and the generated RDFContainer
     * and:
     * <ol>
     * <li>Gets an Aperture extractor (based on mime-type)</li>
     * <li>Applies the extractor to the file</li>
     * <li>Puts the extracted RDF into container</li>
     * </ol>
     * 
     * Question: could this be public?
     * 
     * @param file
     * @param mimeType
     * @param container (in/out) Contains the metadata and full text extracted
     *            from the file
     * @throws IOException
     * @throws ExtractorException
     */
    private static void determineExtractor(File file, String mimeType,
            RDFContainer container) throws IOException, ExtractorException {
        FileInputStream stream;
        BufferedInputStream buffer;
        URI uri = new URIImpl(file.toURI().toString());

        // create an ExtractorRegistry containing all available
        // ExtractorFactories
        ExtractorRegistry extractorRegistry = new DefaultExtractorRegistry();

        // determine and apply an Extractor that can handle this MIME type
        Set<?> factories = extractorRegistry.getExtractorFactories(mimeType);
        if (factories == null || factories.isEmpty()) {
            factories = extractorRegistry.getFileExtractorFactories(mimeType);
        }
        if (factories != null && !factories.isEmpty()) {
            // just fetch the first available Extractor
            Object factory = factories.iterator().next();
            if (factory instanceof ExtractorFactory) {
                Extractor extractor = ((ExtractorFactory) factory).get();
                stream = new FileInputStream(file);
                buffer = new BufferedInputStream(stream, 8192);
                extractor.extract(uri, buffer, null, mimeType, container);
                try {
                    buffer.close();
                    stream.close();
                } catch (IOException ex) {
                }
            } else if (factory instanceof FileExtractorFactory) {
                FileExtractor extractor = ((FileExtractorFactory) factory)
                        .get();
                extractor.extract(uri, file, null, mimeType, container);
            }
        }
        // add the MIME type as an additional statement to the RDF model
        container.add(NIE.mimeType, mimeType);

        // container.getModel().writeTo(new PrintWriter(System.out),
        // Syntax.Ntriples);
    }

    /**
     * Overridden method getId
     * 
     * @return plugin id
     */
    @Override
    public String getId() {
        return "aperture";
    }

    /**
     * Overridden method getName
     * 
     * @return plugin name
     */
    @Override
    public String getName() {
        return "Aperture Extractor";
    }

    /**
     * Gets a PluginDescription object relating to this plugin.
     * 
     * @return a PluginDescription
     */
    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    /**
     * Overridden method shutdown method
     * 
     * @throws PluginException
     */
    @Override
    public void shutdown() throws PluginException {
    }

    /**
     * Overridden transform method
     * 
     * @param DigitalObject to be processed
     * @return processed DigitalObject with the rdf metadata
     */
    @Override
    public DigitalObject transform(DigitalObject in, String jsonConfig)
            throws TransformerException {
        // Purge old data
        reset();

        String sourceId = in.getSourceId();
        File inFile;
        try {
            inFile = new File(outputPath, sourceId);
            inFile.deleteOnExit();
            FileOutputStream tempFileOut = new FileOutputStream(inFile);
            // Payload from storage
            Payload payload = in.getPayload(sourceId);
            // Copy and close
            IOUtils.copy(payload.open(), tempFileOut);
            payload.close();
            tempFileOut.close();
        } catch (IOException ex) {
            log.error("Error writing temp file : ", ex);
            return in;
            // throw new TransformerException(ex);
        } catch (StorageException ex) {
            log.error("Error accessing storage data : ", ex);
            return in;
            // throw new TransformerException(ex);
        }

        try {
            File oid = new File(in.getId());
            if (inFile.exists()) {
                // Never write to file
                RDFContainer rdf = extractRDF(inFile, "urn:oid:" + in.getId());
                if (rdf != null) {
                    log.info("Done extraction: " + rdf.getClass());
                    Payload rdfPayload = StorageUtils.createOrUpdatePayload(
                            in,
                            "aperture.rdf",
                            new ByteArrayInputStream(
                                    stripNonValidXMLCharacters(rdf).getBytes(
                                            "UTF-8")));
                    rdfPayload.setLabel("Aperture rdf");
                    rdfPayload.setContentType("application/xml+rdf");
                    rdfPayload.setType(PayloadType.Enrichment);
                }
            } else {
                log.info("inFile '{}' does not exist!", inFile);
            }
        } catch (IOException e) {
            log.error("Error accessing metadata stream : ", e);
        } catch (ExtractorException e) {
            log.error("Error extracting metadata : ", e);
        } catch (StorageException e) {
            log.error("Error storing payload : ", e);
        } finally {
            if (inFile.exists()) {
                inFile.delete();
            }
        }
        return in;
    }

    public String stripNonValidXMLCharacters(RDFContainer rdf) {
        String rdfString = rdf.getModel().serialize(Syntax.RdfXml).toString();

        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (rdfString == null || ("".equals(rdfString))) {
            return "";
        }
        for (int i = 0; i < rdfString.length(); i++) {
            current = rdfString.charAt(i);
            if ((current == 0x9) || (current == 0xA) || (current == 0xD)
                    || ((current >= 0x20) && (current <= 0xD7FF))
                    || ((current >= 0xE000) && (current <= 0xFFFD))
                    || ((current >= 0x10000) && (current <= 0x10FFFF))) {
                out.append(current);
            }
        }

        return out.toString();
    }
}
