/*
 * The Fascinator - Plugin - Transformer - ICE2
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
package com.googlecode.fascinator.transformer.ice2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.PayloadType;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.api.transformer.Transformer;
import com.googlecode.fascinator.api.transformer.TransformerException;
import com.googlecode.fascinator.common.BasicHttpClient;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.MimeTypeUtil;
import com.googlecode.fascinator.common.sax.SafeSAXReader;
import com.googlecode.fascinator.common.storage.StorageUtils;

/**
 * <p>
 * This plugin uses <a href="http://ice.usq.edu.au">Integrated Content
 * Environment (ICE)</a> conversion services to get the rendition version of a
 * file.
 * </p>
 * 
 * <h3>Configuration</h3>
 * <p>
 * Standard configuration table:
 * </p>
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
 * <td>ice2</td>
 * </tr>
 * 
 * <tr>
 * <td>url</td>
 * <td>Address of where the ICE service located</td>
 * <td><b>Yes</b></td>
 * <td>http://ice-service.usq.edu.au/api/convert/</td>
 * </tr>
 * 
 * <tr>
 * <td>outputPath</td>
 * <td>Path where the aperture will store the temporary files</td>
 * <td><b>Yes</b></td>
 * <td>${java.io.tmpdir}/${user.name}/ice2-output</td>
 * </tr>
 * 
 * <tr>
 * <td>excludeRenditionExt</td>
 * <td>The list of renditions to be excluded</td>
 * <td>No</td>
 * <td>txt,mp3,m4a,mov,mp4,wav,wma,wmv,mpg,flv</td>
 * </tr>
 * 
 * <tr>
 * <td>priority</td>
 * <td>TPriority of ICE transformer</td>
 * <td>No</td>
 * <td>true</td>
 * </tr>
 * 
 * </table>
 * 
 * <br/>
 * <p>
 * Customise configuration for resizing the image for both thumbnail and preview
 * </p>
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
 * <td>resize/thumbnail/option</td>
 * <td>Resizing option, possible mode: fixedWidth and ratio</td>
 * <td><b>Yes</b></td>
 * <td>fixedWidth</td>
 * </tr>
 * 
 * <tr>
 * <td>resize/thumbnail/ratio</td>
 * <td>Image will be resized based on provided ratio if ratio mode is selected</td>
 * <td><b>Yes</b></td>
 * <td>-90</td>
 * </tr>
 * 
 * <tr>
 * <td>resize/thumbnail/fixedWidth</td>
 * <td>Image will be resized based on the width if fixedWidth mode is selected</td>
 * <td><b>Yes</b></td>
 * <td>160</td>
 * </tr>
 * 
 * <tr>
 * <td>resize/thumbnail/enlarge</td>
 * <td>keep image size if image width is less than the fixedWidth value</td>
 * <td><b>Yes</b></td>
 * <td>false</td>
 * </tr>
 * 
 * <tr>
 * <td>resize/preview/option</td>
 * <td>Resizing option, possible mode: fixedWidth and ratio</td>
 * <td><b>Yes</b></td>
 * <td>fixedWidth</td>
 * </tr>
 * 
 * <tr>
 * <td>resize/preview/ratio</td>
 * <td>Image will be resized based on provided ratio if ratio mode is selected</td>
 * <td><b>Yes</b></td>
 * <td>-90</td>
 * </tr>
 * 
 * <tr>
 * <td>resize/preview/fixedWidth</td>
 * <td>Image will be resized based on the width if fixedWidth mode is selected</td>
 * <td><b>600</b></td>
 * <td>160</td>
 * </tr>
 * 
 * <tr>
 * <td>resize/preview/enlarge</td>
 * <td>keep image size if image width is less than the fixedWidth value</td>
 * <td><b>Yes</b></td>
 * <td>false</td>
 * </tr>
 * 
 * </table>
 * 
 * <p>
 * For further information about Image resizing service provided by ICE, please
 * refer to <a
 * href="https://ice.usq.edu.au/trac/wiki/ICEService/ResizeImage">https
 * ://ice.usq.edu.au/trac/wiki/ICEService/ResizeImage</a>
 * </p>
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * ICE2 transformer connected to ice service for rendition with the specified
 * image resize options
 * 
 * <pre>
 * "ice2": {
 *             "id": "ice2",
 *             "url": "http://ice-service.usq.edu.au/api/convert/",
 *             "outputPath": "${java.io.tmpdir}/${user.name}/ice2-output",
 *             "excludeRenditionExt": "txt,mp3,m4a,mov,mp4,wav,wma,wmv,mpg,flv",
 *             "priority": "true",
 *             "resize": {
 *                 "thumbnail": {
 *                     "option": "fixedWidth",
 *                     "ratio": "-90",
 *                     "fixedWidth": "160",
 *                     "enlarge": "false"
 *                 },
 *                 "preview": {
 *                     "option": "fixedWidth",
 *                     "ratio": "-90",
 *                     "fixedWidth": "600",
 *                     "enlarge": "false"
 *                 }
 *             }
 *         }
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * <a href=
 * "https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Plugins/Transformer/ICE2"
 * >https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Plugins/
 * Transformer/ICE2</a>
 * </p>
 * 
 * @author Linda Octalina, Oliver Lucido
 * 
 */
public class Ice2Transformer implements Transformer {

    /** Logging **/
    private Logger log = LoggerFactory.getLogger(Ice2Transformer.class);

    /** System config file **/
    private JsonSimpleConfig config;

    /** Item config file */
    private JsonSimple itemConfig;

    /** ICE rendition output directory **/
    private File outputDir;

    /** ICE service url **/
    private String convertUrl;

    /** For html parsing **/
    private SafeSAXReader reader;

    /** For making sure the ICE thumbnail/preview is used **/
    private Boolean priority;

    /** Default zip mime type **/
    private static final String ZIP_MIME_TYPE = "application/zip";

    private static final String HTML_MIME_TYPE = "text/html";

    private static final String IMG_MIME_TYPE = "image/";

    /** Flag for first execution */
    private boolean firstRun = true;

    /** Exclude these file from renditions */
    private List<String> excludeList;

    /** A list of thumbnails in the object */
    private List<String> thumbnails;

    /** A list of previews in the object */
    private List<String> previews;

    /**
     * ICE transformer constructor
     */
    public Ice2Transformer() {
    }

    /**
     * Init method to initialise ICE transformer
     * 
     * @param jsonFile
     * @throws IOException
     * @throws PluginException
     */
    @Override
    public void init(File jsonFile) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonFile);
            itemConfig = new JsonSimple();
            reset();
        } catch (IOException ioe) {
            throw new PluginException(ioe);
        }
    }

    /**
     * Init method to initialise ICE transformer
     * 
     * @param jsonString
     * @throws IOException
     * @throws PluginException
     */
    @Override
    public void init(String jsonString) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonString);
            itemConfig = new JsonSimple();
            reset();
        } catch (IOException ioe) {
            throw new PluginException(ioe);
        }
    }

    /**
     * Reset the transformer in preparation for a new object
     */
    private void reset() throws TransformerException {
        if (firstRun) {
            firstRun = false;
            // Output directory
            String outputPath = config.getString(null, "outputPath");
            if (outputPath == null) {
                throw new TransformerException("Output path not specified!");
            }
            outputDir = new File(outputPath);
            outputDir.mkdirs();

            // Rendition exclusions
            excludeList = Arrays.asList(StringUtils.split(
                    config.getString(null, "excludeRenditionExt"), ','));

            // Conversion Service URL
            convertUrl = config.getString(null, "url");
            if (convertUrl == null) {
                throw new TransformerException("No ICE URL provided!");
            }
        }

        // Priority
        Boolean testResponse = itemConfig.getBoolean(null, "priority");
        if (testResponse != null) {
            // We found it in item config
            priority = testResponse;
        } else {
            // Try system config
            priority = config.getBoolean(true, "priority");
        }

        // Clear the old SAX reader
        reader = new SafeSAXReader();

        // Remove the last object
        thumbnails = null;
        previews = null;
    }

    /**
     * Transform method
     * 
     * @param object : DigitalObject to be transformed
     * @return transformed DigitalObject
     * @throws TransformerException
     * @throws StorageException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    @Override
    public DigitalObject transform(DigitalObject object, String jsonConfig)
            throws TransformerException {
        try {
            itemConfig = new JsonSimple(jsonConfig);
        } catch (IOException ex) {
            throw new TransformerException("Invalid configuration! '{}'", ex);
        }

        // Purge old data - after itemConfig is set
        reset();

        String sourceId = object.getSourceId();
        String ext = FilenameUtils.getExtension(sourceId);
        String fileName = FilenameUtils.getBaseName(sourceId);

        // Cache the file out of storage
        File file;
        try {
            file = new File(outputDir, sourceId);
            FileOutputStream out = new FileOutputStream(file);
            // Payload from storage
            Payload payload = object.getPayload(sourceId);
            // Copy and close
            IOUtils.copy(payload.open(), out);
            payload.close();
            out.close();
        } catch (IOException ex) {
            log.error("Error writing temp file : ", ex);
            return object;
        } catch (StorageException ex) {
            log.error("Error accessing storage data : ", ex);
            return object;
        }

        // Render the file if supported
        if (file.exists() && !excludeList.contains(ext.toLowerCase())) {
            try {
                if (isSupported(file)) {
                    File outputFile = render(file);
                    outputFile.deleteOnExit();
                    object = createIcePayload(object, outputFile);
                    outputFile.delete();
                }
            } catch (Exception e) {
                log.debug("Adding error payload to {}", object.getId());
                try {
                    object = createErrorPayload(object, fileName, e);
                } catch (Exception e1) {
                    log.error("Error creating error payload", e1);
                }
            }
        }

        // Cleanup an finish
        try {
            object.close();
        } catch (StorageException ex) {
            log.error("Failed writing object metadata", ex);
        }
        if (file.exists()) {
            file.delete();
        }
        return object;
    }

    /**
     * Create Payload method for ICE Error
     * 
     * @param object : DigitalObject that store the payload
     * @param file : File to be stored as payload
     * @param message : Error message
     * @return transformed DigitalObject
     * @throws StorageException
     * @throws UnsupportedEncodingException
     */
    public DigitalObject createErrorPayload(DigitalObject object, String file,
            Exception ex) throws StorageException, UnsupportedEncodingException {
        String name = file + "_ice_error.htm";
        String message = ex.getMessage();
        if (message == null) {
            message = ex.toString();
        }
        Payload errorPayload = StorageUtils.createOrUpdatePayload(object, name,
                new ByteArrayInputStream(message.getBytes("UTF-8")));
        errorPayload.setType(PayloadType.Error);
        errorPayload.setLabel("ICE conversion errors");
        errorPayload.setContentType("text/html");
        return object;
    }

    /**
     * Create Payload method for ICE rendition files
     * 
     * @param object : DigitalObject that store the payload
     * @param file : File to be stored as payload
     * @return transformed DigitalObject
     * @throws StorageException
     * @throws IOException
     */
    public DigitalObject createIcePayload(DigitalObject object, File file)
            throws StorageException, IOException, Exception {
        if (ZIP_MIME_TYPE.equals(MimeTypeUtil.getMimeType(file))) {
            ZipFile zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    String mimeType = MimeTypeUtil.getMimeType(name);
                    // log.info("(ZIP) Name : '" + name + "', MimeType : '" +
                    // mimeType + "'");
                    InputStream in = zipFile.getInputStream(entry);

                    // If this is a HTML document we need to strip it down
                    // to the 'body' tag and replace with a 'div'
                    if (mimeType.equals(HTML_MIME_TYPE)) {
                        try {
                            log.debug("Stripping unnecessary HTML");
                            // Parse the document
                            Document doc = reader.loadDocumentFromStream(in);
                            // Alter the body node
                            Node node = doc
                                    .selectSingleNode("//*[local-name()='body']");
                            node.setName("div");
                            // Write out the new 'document'
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            reader.docToStream(node, out);
                            // Prep our inputstream again
                            in = new ByteArrayInputStream(out.toByteArray());
                        } catch (DocumentException ex) {
                            createErrorPayload(object, name, ex);
                            continue;
                        } catch (Exception ex) {
                            log.error("Error : ", ex);
                            continue;
                        }
                    }

                    // Determing the payload type to use in storage
                    PayloadType pt = null;
                    try {
                        pt = assignType(object, name, mimeType);
                    } catch (TransformerException ex) {
                        throw new Exception(
                                "Error examining object to assign type: ", ex);
                    }
                    if (pt == null) {
                        // We're done, this file is not being stored
                        return object;
                    }

                    Payload icePayload = StorageUtils.createOrUpdatePayload(
                            object, name, in);
                    icePayload.setLabel(name);
                    icePayload.setContentType(mimeType);
                    icePayload.setType(pt);
                    icePayload.close();
                }
            }
            zipFile.close();
        } else {
            String name = file.getName();
            String mimeType = MimeTypeUtil.getMimeType(name);
            // Determing the payload type to use in storage
            PayloadType pt = null;
            try {
                pt = assignType(object, name, mimeType);
            } catch (TransformerException ex) {
                throw new Exception("Error examining object to assign type: ",
                        ex);
            }
            if (pt == null) {
                // We're done, this file is not being stored
                return object;
            }

            Payload icePayload = StorageUtils.createOrUpdatePayload(object,
                    name, new FileInputStream(file));
            icePayload.setLabel(name);
            icePayload.setContentType(mimeType);
            icePayload.setType(pt);
            icePayload.close();
        }

        return object;
    }

    /**
     * After assessing the existing object and what needs to be added, return a
     * PayloadType to use for new payloads.
     * 
     * @param object: The object to add a payload to
     * @param pid: The new payload ID that will be used
     * @param mimeType: The MIME type of the content being added
     * @return PayloadType: The type to allocate to the new payload
     */
    private PayloadType assignType(DigitalObject object, String pid,
            String mimeType) throws TransformerException {
        // First run through for the object
        if (thumbnails == null) {
            getThumbAndPreviews(object);
            cleanObject(object);
        }

        // Have we seen it before?
        if (thumbnails.contains(pid)) {
            return PayloadType.Thumbnail;
        }
        if (previews.contains(pid)) {
            return PayloadType.Preview;
        }

        // Previews
        if (mimeType.equals(HTML_MIME_TYPE)
                || ((mimeType.contains(IMG_MIME_TYPE) && pid
                        .contains("_preview")))) {
            // Existing previews?
            if (!previews.isEmpty()) {
                // Do we have priority?
                if (priority) {
                    // Yep, bump the old payload
                    String oldPid = previews.get(0);
                    changeType(object, oldPid, PayloadType.AltPreview);
                    previews.remove(oldPid);
                    // And add the new
                    previews.add(pid);
                    return PayloadType.Preview;
                } else {
                    // No, just and a new alt
                    return PayloadType.AltPreview;
                }
            } else {
                // Simple, the first preview
                previews.add(pid);
                return PayloadType.Preview;
            }
        }

        // Thumbnails
        if ((mimeType.contains(IMG_MIME_TYPE) && pid.contains("_thumbnail.jpg"))) {
            // Existing previews?
            if (!thumbnails.isEmpty()) {
                // Do we have priority?
                if (priority) {
                    // Yep, bump the old payload
                    String oldPid = thumbnails.get(0);
                    changeType(object, oldPid, PayloadType.Enrichment);
                    thumbnails.remove(oldPid);
                    // And add the new
                    thumbnails.add(pid);
                    return PayloadType.Thumbnail;
                } else {
                    // No, we are going to ignore this one then
                    return null;
                }
            } else {
                // Simple, the first thumbnail
                thumbnails.add(pid);
                return PayloadType.Thumbnail;
            }
        }

        // Not sure what it is, so just use Enrichment
        return PayloadType.Enrichment;
    }

    /**
     * Remove extraneous thumbnails and previews from the object if found
     * 
     * @param object: The object to clean
     */
    private void cleanObject(DigitalObject object) throws TransformerException {
        boolean success;

        // Validate thumbnails
        if (thumbnails.size() > 1) {
            // TODO: We could could some complicated logic here guessing where
            // things came from... or we could just keep the first one.
            String keeper = thumbnails.get(0);
            // Avoid concurrent modification
            String[] loop = thumbnails.toArray(new String[0]);
            for (String pid : loop) {
                if (!pid.equals(keeper)) {
                    success = changeType(object, pid, PayloadType.Enrichment);
                    if (!success) {
                        throw new TransformerException(
                                "Object has multiple "
                                        + "thumbnails, error accessing payloads to correct");
                    }
                    thumbnails.remove(pid);
                }
            }
        }

        // Validate previews
        if (previews.size() > 1) {
            // TODO: We could could some complicated logic here guessing where
            // things came from... or we could just keep the first one.
            String keeper = previews.get(0);
            String[] loop = previews.toArray(new String[0]);
            for (String pid : loop) {
                if (!pid.equals(keeper)) {
                    success = changeType(object, pid, PayloadType.AltPreview);
                    if (!success) {
                        throw new TransformerException(
                                "Object has multiple "
                                        + "previews, error accessing payloads to correct");
                    }
                    previews.remove(pid);
                }
            }
        }
    }

    /**
     * Change the type of an existing payload in the object.
     * 
     * @param object: The object containing the payload
     * @param pid: The payload ID of the payload to change
     * @param newType: The new type to allocate
     * @return boolean: True if the change was successful, False if not
     */
    private boolean changeType(DigitalObject object, String pid,
            PayloadType newType) {
        try {
            Payload payload = object.getPayload(pid);
            payload.setType(newType);
            payload.close();
            return true;
        } catch (StorageException ex) {
            log.error("Error accessing payload: '{}'", pid, ex);
            return false;
        }
    }

    /**
     * Main render method to send the file to ICE service
     * 
     * @param sourceFile : File to be rendered
     * @return file returned by ICE service
     * @throws TransformerException
     */
    private File render(File sourceFile) throws TransformerException {
        log.info("Converting {}...", sourceFile);
        String filename = sourceFile.getName();
        String basename = FilenameUtils.getBaseName(filename);
        String ext = FilenameUtils.getExtension(filename);
        int status = HttpStatus.SC_OK;

        // Grab our config
        JsonObject object = itemConfig.getObject("resize");
        Map<String, JsonSimple> resizeConfig = JsonSimple.toJavaMap(object);

        if (resizeConfig == null || resizeConfig.isEmpty()) {
            // Try system config instead
            object = config.getObject("resize");
            resizeConfig = JsonSimple.toJavaMap(object);
            if (resizeConfig == null || resizeConfig.isEmpty()) {
                throw new TransformerException(
                        "No resizing configuration found.");
            }
        }

        String resizeJson = "";
        for (String key : resizeConfig.keySet()) {
            JsonSimple j = resizeConfig.get(key);
            resizeJson += "\"" + key + "\":" + j.toString() + ",";
        }

        PostMethod post = new PostMethod(convertUrl);
        try {
            Part[] parts = {
                    new StringPart("zip", "on"),
                    new StringPart("dc", "on"),
                    new StringPart("toc", "on"),
                    new StringPart("pdflink", "on"),
                    new StringPart("addThumbnail", "on"),
                    new StringPart("pathext", ext),
                    new StringPart("template", getTemplate()),
                    new StringPart("multipleImageOptions", "{"
                            + StringUtils.substringBeforeLast(resizeJson, ",")
                            + "}"), new StringPart("mode", "download"),
                    new FilePart("file", sourceFile) };
            post.setRequestEntity(new MultipartRequestEntity(parts, post
                    .getParams()));
            BasicHttpClient client = new BasicHttpClient(convertUrl);
            log.debug("Using conversion URL: {}", convertUrl);
            status = client.executeMethod(post);
            log.debug("HTTP status: {} {}", status,
                    HttpStatus.getStatusText(status));
        } catch (IOException ioe) {
            throw new TransformerException(
                    "Failed to send ICE conversion request", ioe);
        }
        try {
            if (status != HttpStatus.SC_OK) {
                String xmlError = post.getResponseBodyAsString();
                log.debug("Error: {}", xmlError);
                throw new TransformerException(xmlError);
            }
            String type = post.getResponseHeader("Content-Type").getValue();
            if ("application/zip".equals(type)) {
                filename = basename + ".zip";
            } else if (type.startsWith("image/")) {
                filename = basename + "_thumbnail.jpg";
            } else if ("video/x-flv".equals(type)) {
                filename = basename + ".flv";
            } else if ("audio/mpeg".equals(type)) {
                filename = basename + ".mp3";
            }
            File outputFile = new File(outputDir, filename);
            if (outputFile.exists()) {
                outputFile.delete();
            }
            InputStream in = post.getResponseBodyAsStream();
            FileOutputStream out = new FileOutputStream(outputFile);
            IOUtils.copy(in, out);
            in.close();
            out.close();
            log.debug("ICE output file: {}", outputFile);
            return outputFile;
        } catch (IOException ioe) {
            throw new TransformerException("Failed to process ICE output", ioe);
        }
    }

    /**
     * Check if the file extension is supported
     * 
     * @param sourceFile : File to be checked
     * @return True if it's supported, false otherwise
     * @throws TransformerException
     */
    private boolean isSupported(File sourceFile) throws TransformerException {
        String ext = FilenameUtils.getExtension(sourceFile.getName());
        String url = convertUrl + "/query?pathext=" + ext.toLowerCase();
        try {
            GetMethod getMethod = new GetMethod(url);
            BasicHttpClient extClient = new BasicHttpClient(url);
            extClient.executeMethod(getMethod);
            String response = getMethod.getResponseBodyAsString().trim();
            return "OK".equals(response);
        } catch (IOException ioe) {
            throw new TransformerException(
                    "Failed to query if file type is supported", ioe);
        }
    }

    /**
     * Get ICE template
     * 
     * @return ice template
     * @throws IOException
     */
    private String getTemplate() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(getClass().getResourceAsStream("/template.xhtml"), out);
        return out.toString("UTF-8");
    }

    /**
     * Get Transformer ID
     * 
     * @return id
     */
    @Override
    public String getId() {
        return "ice2";
    }

    /**
     * Get Transformer Name
     * 
     * @return name;
     */
    @Override
    public String getName() {
        return "ICE Transformer";
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
     * Shut down the transformer plugin
     */
    @Override
    public void shutdown() throws PluginException {
    }

    /**
     * Retrieve a list of payloads that have the type 'Thumbnail' or 'Preview'.
     * In theory this should only ever be zero or one of each, but we are going
     * to also validate 'broken' objects.
     * 
     * @param object: The object to retrieve thumbnails for
     */
    private void getThumbAndPreviews(DigitalObject object) {
        thumbnails = new ArrayList<String>();
        previews = new ArrayList<String>();
        // Loop through all payloads
        for (String pid : object.getPayloadIdList()) {
            try {
                Payload p = object.getPayload(pid);
                // Compare their type
                if (p.getType().compareTo(PayloadType.Thumbnail) == 0) {
                    thumbnails.add(pid);
                }
                // Compare their type
                if (p.getType().compareTo(PayloadType.Preview) == 0) {
                    previews.add(pid);
                }
            } catch (StorageException ex) {
                log.error("Error looking at payload: '{}'", pid);
            }
        }
    }
}
