/*
 * The Fascinator - Plugin - Transformer - FFMPEG
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
package com.googlecode.fascinator.transformer.ffmpeg;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.PayloadType;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.api.transformer.Transformer;
import com.googlecode.fascinator.api.transformer.TransformerException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.MimeTypeUtil;
import com.googlecode.fascinator.common.storage.StorageUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This plugin is used for converting audio and video media to web-friendly
 * versions using FFmpeg library.
 * </p>
 * 
 * <h3>Configuration</h3> Please refer to below wiki link for more information
 * about the configuration options
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * <a href=
 * "https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Plugins/Transformer/Ffmpeg"
 * >https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Plugins/
 * Transformer/Ffmpeg</a>
 * </p>
 * 
 * @author Oliver Lucido, Linda Octalina, Greg Pendlebury
 */

public class FfmpegTransformer implements Transformer {

    /** Error Payload */
    private static String ERROR_PAYLOAD = "ffmpegErrors.json";

    /** Merged Payload */
    private static String MERGED_PAYLOAD = "ffmpegMerged.";

    /** Metadata Payload */
    private static String METADATA_PAYLOAD = "ffmpeg.info";

    /** Logger */
    private Logger log = LoggerFactory.getLogger(FfmpegTransformer.class);

    /** Statistics database */
    private FfmpegDatabase stats;

    /** System config file */
    private JsonSimpleConfig config;

    /** Item config file */
    private JsonSimple itemConfig;

    /** FFMPEG output directory */
    private File outputDir;

    /** FFMPEG output directory */
    private File outputRoot;

    /** Ffmpeg class for conversion */
    private Ffmpeg ffmpeg;

    /** Flag for first execution */
    private boolean firstRun = true;

    /** Object format */
    private String format;

    /** Parsed media info */
    private FfmpegInfo info;

    /** Metadata storage */
    private Map<String, JsonObject> metadata;

    /** Old Metadata */
    private Map<String, JsonSimple> oldMetadata;

    /** Error messages */
    private Map<String, JsonObject> errors;

    /** Object ID */
    private String oid;

    /** Frame rate to use during merge */
    private String mergeRate;

    /** Frame rate to use AFTER merge */
    private String finalRate;

    /** Format to use AFTER merge */
    private String finalFormat;

    /**
     * Basic constructor
     * 
     */
    public FfmpegTransformer() {
        // Need a default constructor for ServiceLoader
    }

    /**
     * Instantiate the transformer with an existing instantiation of Ffmpeg
     * 
     * @param ffmpeg already instaniated ffmpeg installation
     */
    public FfmpegTransformer(Ffmpeg ffmpeg) {
        this.ffmpeg = ffmpeg;
    }

    /**
     * Init method to initialise Ffmpeg transformer
     * 
     * @param jsonFile
     * @throws IOException
     * @throws PluginException
     */
    @Override
    public void init(File jsonFile) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonFile);
            reset();
        } catch (IOException ioe) {
            throw new PluginException(ioe);
        }
    }

    /**
     * Init method to initialise Ffmpeg transformer
     * 
     * @param jsonString
     * @throws IOException
     * @throws PluginException
     */
    @Override
    public void init(String jsonString) throws PluginException {
        try {
            config = new JsonSimpleConfig(jsonString);
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
            testExecLevel();

            // Prep output area
            String outputPath = config.getString(null, "outputPath");
            if (outputPath == null) {
                log.error("No output path provided!");
                return;
            }
            outputRoot = new File(outputPath);
            outputRoot.mkdirs();

            // Database
            boolean useDB = config.getBoolean(false, "database", "enabled");
            if (useDB) {
                try {
                    stats = new FfmpegDatabase(config);
                } catch (Exception ex) {
                    log.error("Statistics database failed to initialise!");
                }
            }

            // Set system variable for presets location
            String presetsPath = config.getString(null, "presetsPath");
            if (presetsPath != null) {
                File presetDir = new File(presetsPath);
                // Make sure it's valid
                if (presetDir.exists() && presetDir.isDirectory()) {
                    // And let FFmpeg know about it
                    ffmpeg.setEnvironmentVariable("FFMPEG_DATADIR",
                            presetDir.getAbsolutePath());
                } else {
                    log.error("Invalid FFmpeg presets path provided: '{}'",
                            presetsPath);
                }
            }
        }

        itemConfig = null;
        info = null;
        format = null;
        errors = new LinkedHashMap();
        metadata = new LinkedHashMap();
        oldMetadata = new LinkedHashMap();
    }

    /**
     * Add an error to the list of errors for this pass through the transformer
     * 
     * @param index: The index to use in the Map
     * @param message: The error message to record
     */
    private void addError(String index, String message) {
        // Sanity check
        if (message == null || index == null) {
            return;
        }
        // Drop to the log files too
        log.error(message);
        // Create JSON version of the message
        JsonObject msg = new JsonObject();
        msg.put("message", message);
        addError(index, msg);
    }

    /**
     * Add an error to the list of errors for this pass through the transformer.
     * This method also accepts an exception object.
     * 
     * @param index: The index to use in the Map
     * @param message: The error message to record
     * @param ex: The
     */
    private void addError(String index, String message, Exception ex) {
        // Sanity check
        if (message == null || index == null) {
            return;
        }
        // Drop to the log files too
        log.error(message, ex);
        // Create JSON version of the message
        JsonObject msg = new JsonObject();
        msg.put("message", message);
        if (ex != null) {
            msg.put("exception", ex.getMessage());
            // Turn the stacktrace into a string
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            msg.put("stacktrace", sw.toString());
        }
        addError(index, msg);
    }

    /**
     * Add an error to the list of errors for this pass through the transformer
     * 
     * @param index: The index to use in the Map
     * @param message: The error message to record
     */
    private void addError(String index, JsonObject message) {
        // Sanity check
        if (message == null || index == null) {
            return;
        }
        // Avoid overwriting the index if re-used
        if (errors.containsKey(index)) {
            int inc = 2;
            while (errors.containsKey(index + "_" + inc)) {
                inc++;
            }
            index = index + "_" + inc;
        }
        // Store the error
        errors.put(index, message);
    }

    /**
     * Test the level of functionality available on this system
     * 
     * @return String indicating the level of available functionality
     */
    private String testExecLevel() {
        // Make sure we can start
        if (ffmpeg == null) {
            ffmpeg = new FfmpegImpl(
                    get(null, null, "binaries", "transcoding"),
                    get(null, null, "binaries", "metadata"));
        }
        return ffmpeg.testAvailability();
    }

    /**
     * Transforming digital object method
     * 
     * @params object: DigitalObject to be transformed
     * @return transformed DigitalObject after transformation
     * @throws TransformerException if the transformation fails
     */
    @Override
    public DigitalObject transform(DigitalObject object, String jsonConfig)
            throws TransformerException {
        if (testExecLevel() == null) {
            log.error("FFmpeg is either not installed, or not executing!");
            return object;
        }
        // Purge old data
        reset();
        oid = object.getId();
        outputDir = new File(outputRoot, oid);
        outputDir.mkdirs();

        try {
            itemConfig = new JsonSimple(jsonConfig);
        } catch (IOException ex) {
            throw new TransformerException("Invalid configuration! '{}'", ex);
        }

        // Resolve multi-segment files first
        mergeRate = get(itemConfig, "25", "merging", "mpegFrameRate");
        finalRate = get(itemConfig, "10", "merging", "finalFrameRate");
        finalFormat = get(itemConfig, "avi", "merging", "finalFormat");
        mergeSegments(object);

        // Find the format 'group' this file is in
        String sourceId = object.getSourceId();
        String ext = FilenameUtils.getExtension(sourceId);
        format = getFormat(ext);

        // Return now if this isn't a format we care about
        if (format == null) {
            return object;
        }
        // log.debug("Supported format found: '{}' => '{}'", ext, format);

        // Cache the file from storage
        File file;
        try {
            file = cacheFile(object, sourceId);
        } catch (IOException ex) {
            addError(sourceId, "Error writing temp file", ex);
            errorAndClose(object);
            return object;
        } catch (StorageException ex) {
            addError(sourceId, "Error accessing storage data", ex);
            errorAndClose(object);
            return object;
        }
        if (!file.exists()) {
            addError(sourceId, "Unknown error writing cache: does not exist");
            errorAndClose(object);
            return object;
        }

        // **************************************************************
        // From here on we know (assume) that we SHOULD be able to support
        // this object, so errors can't just throw exceptions. We should
        // only return under certain circumstances (ie. not just because
        // one rendition fails), and the object must get closed.
        // **************************************************************

        // Read any pre-existing rendition metadata from previous tranformations
        // ++++++++++++++++++++++++++++
        // TODO: This is useless until the last modified date can be retrieved
        // against the source file. Storage API does not currently support this,
        // it just returns a data stream.
        //
        // Once this feature exists the basic algorithm should be:
        // 1) Retrieve old metadata
        // 2) Loop through each rendition preparation as normal
        // 3) When the transcoding is ready to start, use the parameters to
        // query the database for the last time the exact transcoding was
        // and comparing against last modifed.
        // 4) If the transcoding is newer than the source, skip running FFmpeg
        // and just use the same metadata as last time.
        // ++++++++++++++++++++++++++++
        // readMetadata(object);

        // Check for a custom display type
        String display = get(itemConfig, null, "displayTypes", format);
        if (display != null) {
            try {
                Properties prop = object.getMetadata();
                prop.setProperty("displayType", display);
                prop.setProperty("previewType", display);
            } catch (StorageException ex) {
                addError("display", "Could not access object metadata", ex);
            }
        }

        // Gather metadata
        try {
            info = ffmpeg.getInfo(file);
        } catch (IOException ex) {
            addError("metadata", "Error accessing metadata", ex);
            errorAndClose(object);
            return object;
        }

        // Can we even process this file?
        if (!info.isSupported()) {
            closeObject(object);
            return object;
        }

        // What conversions are required for this format?
        List<JsonSimple> conversions = getJsonList(itemConfig,
                "transcodings", format);
        for (JsonSimple conversion : conversions) {
            String name = conversion.getString(null, "alias");
            // And what/how many renditions does it have?
            List<JsonSimple> renditions = getJsonList(conversion, "renditions");
            if (renditions == null || renditions.isEmpty()) {
                addError("transcodings", "Invalid or missing transcoding data:"
                        + " '/transcodings/" + format + "'");
            } else {
                // Config look valid, lets give it a try
                // log.debug("Starting renditions for '{}'", name);
                for (JsonSimple render : renditions) {
                    File converted = null;
                    // Render the output
                    try {
                        converted = convert(file, render, info);
                    } catch (Exception ex) {
                        String outputFile = render.getString(null, "name");
                        if (outputFile != null) {
                            addError(jsonKey(outputFile),
                                    "Error converting file", ex);
                        } else {
                            // Couldn't read the config for a name
                            addError("unknown", "Error converting file", ex);
                        }
                    }

                    // Now store the output if valid
                    if (converted != null) {
                        try {
                            Payload payload = createFfmpegPayload(object,
                                    converted);
                            // TODO: Type checking needs more work
                            // Indexing fails silently if you add two thumbnails
                            // or two previews
                            payload.setType(resolveType(
                                    render.getString(null, "type")));
                            payload.close();
                        } catch (Exception ex) {
                            addError(jsonKey(converted.getName()),
                                    "Error storing output", ex);
                        } finally {
                            converted.delete();
                        }
                    }

                }
            }
        }

        // Write metadata to storage
        if (compileMetadata(object)) {
            // Close normally
            closeObject(object);
        } else {
            // Close with some errors
            errorAndClose(object);
        }
        // Cleanup
        if (file.exists()) {
            file.delete();
        }
        return object;
    }

    /**
     * Stream data from specified payload into a file in our temp cache.
     * 
     * @param object: The digital object to use
     * @param pid: The payload ID to extract
     * @return File: The cached File
     * @throws FileNotFoundException: If accessing the cache fails
     * @throws StorageException: If accessing the object in storage fails
     * @throws IOException: If the data copy fails
     */
    private File cacheFile(DigitalObject object, String pid)
            throws FileNotFoundException, StorageException, IOException {
        // Get our cache location
        File file = new File(outputDir, pid);
        FileOutputStream tempFileOut = new FileOutputStream(file);
        // Get payload from storage
        Payload payload = object.getPayload(pid);
        try {
            // Copy to cache
            IOUtils.copy(payload.open(), tempFileOut);
        } catch (IOException ex) {
            payload.close();
            throw ex;
        }
        // Close and return
        payload.close();
        tempFileOut.close();
        return file;
    }

    /**
     * Check the object for a multi-segment source and merge them. Such sources
     * must come from a harvester specifically designed to match this
     * transformer. As such we can make certain assumptions, and if they are not
     * met we just fail silently with a log entry.
     * 
     * @param object: The digital object to modify
     */
    private void mergeSegments(DigitalObject object) {
        try {
            // Retrieve (optional) segment information from metadata
            Properties props = object.getMetadata();
            String segs = props.getProperty("mediaSegments");
            if (segs == null) {
                return;
            }
            int segments = Integer.parseInt(segs);
            if (segments <= 1) {
                return;
            }

            // We need to do some merging, lets validate IDs first
            log.info("Found {} source segments! Merging...", segments);
            List<String> segmentIds = new ArrayList();
            Set<String> payloadIds = object.getPayloadIdList();
            // The first segment
            String sourceId = object.getSourceId();
            if (sourceId == null || !payloadIds.contains(sourceId)) {
                log.error("Cannot find source payload.");
                return;
            }
            segmentIds.add(sourceId);
            // Find the other segments
            for (int i = 1; i < segments; i++) {
                // We won't know the extension though
                String segmentId = "segment" + i + ".";
                for (String pid : payloadIds) {
                    if (pid.startsWith(segmentId)) {
                        segmentIds.add(pid);
                    }
                }
            }

            // Did we find every segment?
            if (segmentIds.size() != segments) {
                log.error("Unable to find all segments in payload list.");
                return;
            }

            // Transcode all the files to neutral MPEGs first
            Map<String, File> files = new HashMap();
            for (String segment : segmentIds) {
                try {
                    File file = basicMpeg(object, segment);
                    if (file != null) {
                        files.put(segment, file);
                    }
                } catch (Exception ex) {
                    log.error("Error transcoding segment to MPEG: ", ex);
                    // Cleanup
                    for (File f : files.values()) {
                        if (f.exists()) {
                            f.delete();
                        }
                    }
                    return;
                }
            }

            // Did every transcoding succeed?
            if (files.size() != segments) {
                log.error("At least one segment transcoding failed.");
                // Cleanup
                for (File f : files.values()) {
                    if (f.exists()) {
                        f.delete();
                    }
                }
                return;
            }

            // Now to try merging all the segments. In MPEG format
            // they can just be concatenated.
            try {
                // Create our output file
                String filename = "temp_" + MERGED_PAYLOAD + "mpg";
                File merged = new File(outputDir, filename);
                if (merged.exists()) {
                    merged.delete();
                }
                FileOutputStream out = new FileOutputStream(merged);

                // Merge each segment in order
                for (String sId : segmentIds) {
                    try {
                        mergeSegment(out, files.get(sId));
                    } catch (IOException ex) {
                        log.error("Failed to stream to merged file: ", ex);
                        out.close();
                        // Cleanup
                        for (File f : files.values()) {
                            if (f.exists()) {
                                f.delete();
                            }
                        }
                        merged.delete();
                        return;
                    }
                }
                out.close();

                // Final step, run the output file through a transcoding to
                // write the correct metadata (eg. duration)
                filename = MERGED_PAYLOAD + finalFormat;
                File transcoded = new File(outputDir, filename);
                if (transcoded.exists()) {
                    transcoded.delete();
                }

                // Render
                String stderr = mergeRender(merged, transcoded);
                log.debug("=====\n{}", stderr);
                if (transcoded.exists()) {
                    // Now we need to 'fix' the object, add the new source
                    FileInputStream fis = new FileInputStream(transcoded);
                    String pid = transcoded.getName();
                    Payload p = StorageUtils.createOrUpdatePayload(object, pid,
                            fis);
                    fis.close();
                    p.setType(PayloadType.Source);
                    object.setSourceId(pid);

                    // Remove all the old segments
                    for (String sId : segmentIds) {
                        object.removePayload(sId);
                    }
                    props.remove("mediaSegments");
                    object.close();

                    // Cleanup segments
                    for (File f : files.values()) {
                        if (f.exists()) {
                            f.delete();
                        }
                    }
                    merged.delete();
                    transcoded.delete();
                }
            } catch (IOException ex) {
                log.error("Error merging segments: ", ex);
            }

        } catch (StorageException ex) {
            log.error("Error accessing object metadata: ", ex);
        }
    }

    /**
     * Stream the contents of a file into an outputstream.
     * 
     * @param out: The outputstream to send to
     * @param file: The file to stream
     * @throws IOException if the stream fails
     */
    private void mergeSegment(OutputStream out, File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            IOUtils.copy(in, out);
        } catch (IOException ex) {
            // We're just catching this so the finally
            // statement will close the stream
            throw ex;
        } finally {
            in.close();
        }
    }

    /**
     * Convert the payload specified to a neutral MPEG for later concatenation.
     * 
     * @param type The input string to resolve to a proper type
     * @return PayloadType The properly enumerated type to use
     */
    private File basicMpeg(DigitalObject object, String pid) throws Exception {
        // Prepare files
        File sourceFile = cacheFile(object, pid);
        String ext = FilenameUtils.getExtension(pid);
        String output = pid.substring(0, pid.length() - ext.length()) + "mpg";
        File outputFile = new File(outputDir, "output_" + output);
        if (outputFile.exists()) {
            FileUtils.deleteQuietly(outputFile);
        }
        log.info("Converting '{}': '{}'", sourceFile.getName(),
                outputFile.getName());

        // Render
        String stderr = mergeRender(sourceFile, outputFile);

        log.debug("=====\n{}", stderr);
        if (outputFile.exists()) {
            return outputFile;
        }
        return null;
    }

    /**
     * Wrap a basic conversion used repeatedly during file merges
     * 
     * @param in: The source file
     * @param out: The outout file
     * @return String: FFmpeg's console output
     * @thorws IOException: if there are file access errors
     */
    private String mergeRender(File in, File out) throws IOException {
        // Render config
        List<String> params = new ArrayList();
        params.add("-i");
        params.add(in.getAbsolutePath());
        params.add("-sameq");
        params.add("-r");
        if (out.getName().equals(MERGED_PAYLOAD + finalFormat)) {
            params.add(finalRate);
        } else {
            params.add(mergeRate);
        }
        params.add(out.getAbsolutePath());
        // Render
        return ffmpeg.transform(params, outputDir);
    }

    /**
     * Find and return the payload type to use from the input string
     * 
     * @param type The input string to resolve to a proper type
     * @return PayloadType The properly enumerated type to use
     */
    private PayloadType resolveType(String type) {
        // Invalid data
        if (type == null) {
            return PayloadType.Enrichment;
        }

        try {
            PayloadType pt = PayloadType.valueOf(type);
            // Valid data
            return pt;
        } catch (Exception ex) {
            // Unmatched data
            return PayloadType.Enrichment;
        }
    }

    /**
     * Determine the format group to use in transcoding
     * 
     * @param extension The file extension
     * @param String The format group, NULL if not found
     */
    private String getFormat(String extension) {
        List<JsonSimple> formatList = getJsonList(null, "supportedFormats");
        for (JsonSimple json : formatList) {
            String group = json.getString(null, "group");
            List<String> extensions = getList(json, ",", "extensions");
            for (String ext : extensions) {
                if (extension.equalsIgnoreCase(ext)) {
                    return group;
                }
            }
        }
        return null;
    }

    /**
     * Try to create an error payload on the object and close it
     * 
     * @param object to close
     */
    private void errorAndClose(DigitalObject object) {
        try {
            createFfmpegErrorPayload(object);
        } catch (Exception ex) {
            JsonObject content = new JsonObject();
            for (String key : errors.keySet()) {
                content.put(key, errors.get(key));
            }
            log.error("Unable to write error payload, {}", ex);
            log.error("Errors:\n{}", content.toString());
        } finally {
            closeObject(object);
        }
    }

    /**
     * Try to close the object
     * 
     * @param object to close
     */
    private void closeObject(DigitalObject object) {
        try {
            object.close();
        } catch (StorageException ex) {
            log.error("Failed writing object metadata", ex);
        }
    }

    /**
     * Compile all available metadata and add to object.
     * 
     * Note that a False response indicates data has been written into the
     * 'errors' map and should be followed by an 'errorAndClose()' call.
     * 
     * @return: boolean: False if any errors occurred, True otherwise
     */
    private boolean compileMetadata(DigitalObject object) {
        // Nothing to do
        if (info == null) {
            return true;
        }
        if (!info.isSupported()) {
            return true;
        }

        // Get base metadata of source
        JsonObject fullMetadata = null;
        try {
            fullMetadata = new JsonSimple(info.toString()).getJsonObject();
        } catch (IOException ex) {
            addError("metadata", "Error parsing metadata output", ex);
            return false;
        }

        // Add individual conversion(s) metadata
        if (!metadata.isEmpty()) {
            JsonObject metadataObject = new JsonObject();
            for (String key : metadata.keySet()) {
                metadataObject.put(key, metadata.get(key));
            }
            fullMetadata.put("outputs", metadataObject);
        }

        // Write the file to disk
        File metaFile;
        try {
            // log.debug("\nMetadata:\n{}", fullMetadata.toString());
            metaFile = writeMetadata(fullMetadata.toString());
            if (metaFile == null) {
                addError("metadata", "Unknown error extracting metadata");
                return false;
            }
        } catch (TransformerException ex) {
            addError("metadata", "Error writing metadata to disk", ex);
            return false;
        }

        // Store metadata
        try {
            Payload payload = createFfmpegPayload(object, metaFile);
            payload.setType(PayloadType.Enrichment);
            payload.close();
        } catch (Exception ex) {
            addError("metadata", "Error storing metadata payload", ex);
            return false;
        } finally {
            metaFile.delete();
        }

        // Everything should be fine if we got here
        return true;
    }

    /**
     * Create ffmpeg error payload
     * 
     * @param object : DigitalObject to store the payload
     * @return Payload the error payload
     * @throws FileNotFoundException if the file provided does not exist
     * @throws UnsupportedEncodingException for encoding errors in the message
     */
    public Payload createFfmpegErrorPayload(DigitalObject object)
            throws StorageException, FileNotFoundException,
            UnsupportedEncodingException {
        // Compile our error data
        JsonObject content = new JsonObject();
        for (String key : errors.keySet()) {
            content.put(key, errors.get(key));
        }
        log.debug("\nErrors:\n{}", content.toString());
        InputStream data = new ByteArrayInputStream(content.toString()
                .getBytes("UTF-8"));
        // Write to the object
        Payload payload = StorageUtils.createOrUpdatePayload(object,
                ERROR_PAYLOAD, data);
        payload.setType(PayloadType.Error);
        payload.setContentType("application/json");
        payload.setLabel("FFMPEG conversion errors");
        return payload;
    }

    /**
     * Create converted ffmpeg payload
     * 
     * @param object DigitalObject to store the payload
     * @param file File to be stored as payload
     * @return Payload new payload
     * @throws StorageException if there is a problem trying to store
     * @throws FileNotFoundException if the file provided does not exist
     */
    public Payload createFfmpegPayload(DigitalObject object, File file)
            throws StorageException, FileNotFoundException {
        String name = file.getName();
        Payload payload = StorageUtils.createOrUpdatePayload(object, name,
                new FileInputStream(file));
        payload.setContentType(MimeTypeUtil.getMimeType(name));
        payload.setLabel(name);
        return payload;
    }

    /**
     * Read FFMPEG metadata from the object if it exists
     * 
     * @param object: The object to extract data from
     */
    private void readMetadata(DigitalObject object) {
        Set<String> pids = object.getPayloadIdList();
        if (pids.contains(METADATA_PAYLOAD)) {
            try {
                Payload payload = object.getPayload(METADATA_PAYLOAD);
                JsonSimple data = new JsonSimple(payload.open());
                payload.close();
                oldMetadata = JsonSimple.toJavaMap(data.getObject("outputs"));
                // for (String k : oldMetadata.keySet()) {
                // log.debug("\n====\n{}\n===\n{}", k, oldMetadata.get(k));
                // }
            } catch (IOException ex) {
                log.error("Error parsing metadata JSON: ", ex);
            } catch (StorageException ex) {
                log.error("Error accessing metadata payload: ", ex);
            }
        }
    }

    /**
     * Write FFMPEG metadata to disk
     * 
     * @param data : Extracted metadata
     * @return File : Containing metadata
     * @throws TransformerException if the write failed
     */
    private File writeMetadata(String data) throws TransformerException {
        File outputFile = new File(outputDir, METADATA_PAYLOAD);
        if (outputFile.exists()) {
            FileUtils.deleteQuietly(outputFile);
        }
        try {
            outputFile.createNewFile();
            FileUtils.writeStringToFile(outputFile, data, "utf-8");
        } catch (IOException ioe) {
            throw new TransformerException(ioe);
        }
        return outputFile;
    }

    /**
     * Convert audio/video to required output(s)
     * 
     * @param sourceFile : The file to be converted
     * @param render : Configuration to use during the render
     * @param info : Parsed metadata about the source
     * @return File containing converted media
     * @throws TransformerException if the conversion failed
     */
    private File convert(File sourceFile, JsonSimple render, FfmpegInfo info)
            throws TransformerException {
        // Statistics variables
        long startTime, timeSpent;
        String resolution;
        // One list for all settings, the other is a subset for statistics
        List<String> statParams = new ArrayList<String>();
        List<String> params = new ArrayList<String>();

        // Prepare the output location
        String outputName = render.getString(null, "name");
        if (outputName == null) {
            return null;
        }
        File outputFile = new File(outputDir, outputName);
        if (outputFile.exists()) {
            FileUtils.deleteQuietly(outputFile);
        }
        log.info("Converting '{}': '{}'", sourceFile.getName(),
                outputFile.getName());

        // Get metadata ready
        JsonObject renderMetadata = new JsonObject();
        String key = jsonKey(outputName);
        String formatString = render.getString(null, "formatMetadata");
        if (formatString != null) {
            renderMetadata.put("format", formatString);
        }
        String codecString = render.getString(null, "codecMetadata");
        if (codecString != null) {
            renderMetadata.put("codec", codecString);
        }

        try {
            // *************
            // 1) Input file
            // *************
            params.add("-i");
            params.add(sourceFile.getAbsolutePath());
            // Overwrite output file if it exists
            params.add("-y");

            // *************
            // 2) Configurable options
            // *************
            String optionStr = render.getString("", "options");
            List<String> options = split(optionStr, " ");
            // Replace the offset placeholder now that we know the duration
            long start = 0;
            for (int i = 0; i < options.size(); i++) {
                String option = options.get(i);
                // For stats, use placeholder.. random data messes with hashing
                statParams.add(option);
                // If it even exists that is...
                if (option.equalsIgnoreCase("[[OFFSET]]")) {
                    start = (long) (Math.random() * info.getDuration() * 0.25);
                    option = Long.toString(start);
                }
                // Store the parameter for usage
                params.add(option);
            }

            // *************
            // 3) Video resolution / padding
            // *************
            String audioStr = render.getString(null, "audioOnly");
            boolean audio = Boolean.parseBoolean(audioStr);

            // Non-audio files need some resolution work
            if (!audio) {
                List<String> dimensions = getPaddedParams(render, info,
                        renderMetadata, statParams);
                if (dimensions == null || dimensions.isEmpty()) {
                    addError(key, "Error calculating dimensions");
                    return null;
                }
                // Merge resultion parameters into standard parameters
                params.addAll(dimensions);
            }
            // Statistics
            String width = (String) renderMetadata.get("width");
            String height = (String) renderMetadata.get("height");
            if (width == null || height == null) {
                // Audio... or an error
                resolution = "0x0";
            } else {
                resolution = width + "x" + height;
            }

            // *************
            // 4) Output options
            // *************
            optionStr = render.getString("", "output");
            options = split(optionStr, " ");
            // Merge option parameters into standard parameters
            if (!options.isEmpty()) {
                params.addAll(options);
                statParams.addAll(options);
            }
            params.add(outputFile.getAbsolutePath());

            // *************
            // 5) All done. Perform the transcoding
            // *************
            startTime = new Date().getTime();
            String stderr = ffmpeg.transform(params, outputDir);
            timeSpent = (new Date().getTime()) - startTime;

            renderMetadata.put("timeSpent", String.valueOf(timeSpent));
            renderMetadata.put("debugOutput", stderr);
            if (outputFile.exists()) {
                long fileSize = outputFile.length();
                if (fileSize == 0) {
                    throw new TransformerException(
                            "File conversion failed!\n=====\n" + stderr);
                } else {
                    renderMetadata.put("size", String.valueOf(fileSize));
                }
            } else {
                throw new TransformerException(
                        "File conversion failed!\n=====\n" + stderr);
            }

            // log.debug("FFMPEG Output:\n=====\n\\/\\/\\/\\/\n{}/\\/\\/\\/\\\n=====\n",
            // stderr);
        } catch (IOException ioe) {
            addError(key, "Failed to convert!", ioe);
            throw new TransformerException(ioe);
        }

        // On a multi-pass encoding we may be asked to
        // throw away the video from some passes.
        if (outputFile.getName().contains("nullFile")) {
            return null;
        } else {
            // For anything else, record metadata
            metadata.put(key, renderMetadata);
            // And statistics
            if (stats != null) {
                Map<String, String> data = new HashMap();
                data.put("oid", oid);
                data.put("datetime", String.valueOf(startTime));
                data.put("timespent", String.valueOf(timeSpent));
                data.put("renderString", StringUtils.join(statParams, " "));
                data.put("mediaduration", String.valueOf(info.getDuration()));
                data.put("inresolution",
                        info.getWidth() + "x" + info.getHeight());
                data.put("outresolution", resolution);
                data.put("insize", String.valueOf(sourceFile.length()));
                data.put("outsize", String.valueOf(outputFile.length()));
                data.put("infile", sourceFile.getName());
                data.put("outfile", outputFile.getName());
                try {
                    stats.storeTranscoding(data);
                } catch (Exception ex) {
                    log.error("Error storing statistics: ", ex);
                }
            }
        }
        return outputFile;
    }

    /**
     * Build the list of configuration strings to use for the resolution and
     * padding required to match the desired output whilst maintaining the
     * aspect ratio.
     * 
     * @param renderConfig : Configuration to use during the render
     * @param info : Parsed metadata about the source
     * @param renderMetadata : extracted metadata about the source
     * @param stats : The list of parameters to keep for statistics
     * @return List<String> : A list of parameters
     */
    private List<String> getPaddedParams(JsonSimple renderConfig,
            FfmpegInfo info, JsonObject renderMetadata, List<String> stats) {
        List<String> response = new ArrayList();

        // Get the output dimensions to use for the actual video
        int maxX = renderConfig.getInteger(-1, "maxWidth");
        int maxY = renderConfig.getInteger(-1, "maxHeight");
        String size = getSize(info.getWidth(), info.getHeight(), maxX, maxY);
        if (size == null) {
            return null;
        }

        // Validate the response before we calculate padding
        int i = size.indexOf("x");
        int x = makeEven(Integer.parseInt(size.substring(0, i)));
        int y = makeEven(Integer.parseInt(size.substring(i + 1)));
        String paddingConfig = renderConfig.getString("none", "padding");
        String paddingColor = renderConfig.getString("black", "paddingColor");

        // No padding is requested or we don't have both X and Y constraints...
        if (paddingConfig.equalsIgnoreCase("none") || maxX == -1 || maxY == -1) {
            // We're done
            response.add("-s");
            response.add(size);
            // Don't forget metadata
            renderMetadata.put("width", String.valueOf(x));
            renderMetadata.put("height", String.valueOf(y));
            // or stats
            stats.add("padding");
            stats.add("{none}");
            return response;
        }

        // Anything else, we need to modify the response
        int padXleft = makeEven((maxX - x) / 2);
        int padXright = makeEven(maxX - x - padXleft);
        int padYtop = makeEven((maxY - y) / 2);
        int padYbottom = makeEven(maxY - y - padYtop);

        // Record overall dimensions
        String width = String.valueOf(padXleft + x + padXright);
        String height = String.valueOf(padYtop + y + padYbottom);
        renderMetadata.put("width", width);
        renderMetadata.put("height", height);
        // Debugging
        // log.debug("WIDTH: " + padXleft + " + " + x + " + " + padXright +
        // " = " + width);
        // log.debug("HEIGHT: " + padYtop + " + " + y + " + " + padYbottom +
        // " = " + height);

        // Older 'deprecated' builds use individual padding
        if (paddingConfig.equalsIgnoreCase("individual")) {
            response.add("-s");
            response.add(size);
            response.add("-padtop");
            response.add(String.valueOf(padYtop));
            response.add("-padbottom");
            response.add(String.valueOf(padYbottom));
            response.add("-padleft");
            response.add(String.valueOf(padXleft));
            response.add("-padright");
            response.add(String.valueOf(padXright));
            response.add("-padcolor");
            response.add(paddingColor);
            // Collect stats
            stats.add("padding");
            stats.add("{individual}");
            return response;
        }

        // Newer builds use a filter
        if (paddingConfig.equalsIgnoreCase("filter")) {
            String filter = "pad=" + // Type of filter
                    width + ":" + // WIDTH
                    height + ":" + // HEIGHT
                    padXleft + ":" + // X PAD : Right hand is calculated
                    padYtop + ":" + // Y PAD : Bottom is calculated
                    paddingColor; // Color
            response.add("-s");
            response.add(size);
            response.add("-vf");
            response.add(filter);
            // Collect stats
            stats.add("padding");
            stats.add("{filter}");
            return response;
        }

        // Fallback, assume no padding
        log.error("Invalid padding config found: '{}'", paddingConfig);
        response.add("-s");
        response.add(size);
        // Collect stats
        stats.add("padding");
        stats.add("{invalid}");
        return response;
    }

    /**
     * Make sure the provided number is even, reducing if required. FFmpeg only
     * allows even numbers for frame sizes and padding. This is a simple
     * function to make that easier, since it is called a few times above.
     * 
     * @param input : The input integer to verify
     * @return int : The verified integer
     */
    private int makeEven(int input) {
        // An odd number
        if (input % 2 == 1) {
            return input - 1;
        }
        return input;
    }

    /**
     * Compute and return the size string that allows the provided image to fit
     * within dimension constraints whilst respecting the aspect ratio.
     * 
     * @param width : The width of the original
     * @param height : The height of the original
     * @param maxWidth : Width constraint of the output
     * @param maxHeight : Height constraint of the output
     * @return String : The computed size string
     */
    private String getSize(int width, int height, int maxWidth, int maxHeight) {
        // Calculate scaling for dimensions independently
        float scale = 0;
        float dX = maxWidth != -1 ? (float) width / (float) maxWidth : -1;
        float dY = maxWidth != -1 ? (float) height / (float) maxHeight : -1;

        // A) Scaling is constrained by height
        if (dY > dX) {
            scale = dY;
        } else {
            // B) Scaling is constrained by Width (or equal)
            if (dX != 0 && dX != -1) {
                scale = dX;
                // C) No scaling required
            } else {
                scale = 1;
            }
        }

        // Scale and round the numbers to return
        int newWidth = makeEven(Math.round(width / scale));
        int newHeight = makeEven(Math.round(height / scale));
        return newWidth + "x" + newHeight;
    }

    /**
     * Make a string safe to use as a JSON key
     * 
     * @param input: The string to make safe
     * @return String: The modified string, safe to use in JSON
     */
    private String jsonKey(String input) {
        return input.replace(" ", "_");
    }

    /**
     * Get Transformer id
     * 
     * @return id
     */
    @Override
    public String getId() {
        return "ffmpeg";
    }

    /**
     * Get Transformer name
     * 
     * @return name
     */
    @Override
    public String getName() {
        return "FFMPEG Transformer";
    }

    /**
     * Gets a PluginDescription object relating to this plugin.
     * 
     * @return a PluginDescription
     */
    @Override
    public PluginDescription getPluginDetails() {
        PluginDescription pd = new PluginDescription(this);
        JsonSimple details = null;
        String availability = "Unknown";
        if (config == null) {
            try {
                details = new JsonSimple(pd.getMetadata());
            } catch (IOException ioe) {
                log.error("Error parsing plugin description JSON");
            }

        } else {
            details = new JsonSimple();
            availability = testExecLevel();
        }

        details.writeObject("debug").put("availability", availability);
        pd.setMetadata(details.toString());
        return pd;
    }

    /**
     * Get a list of JSON configs from item JSON, falling back to system JSON if
     * not found
     * 
     * @param json Config object containing the json data
     * @param key path to the data in the config file
     * @return List<JsonConfigHelper> containing the config data
     */
    private List<JsonSimple> getJsonList(JsonSimple json, Object... path) {
        List<JsonSimple> response = null;

        // Try item config if provided
        if (json != null) {
            response = json.getJsonSimpleList(path);
        }
        // Then try system config if not found
        if (response == null) {
            response = config.getJsonSimpleList(path);
        }
        // Return an empty list otherwise
        if (response == null) {
            response = new ArrayList();
        }
        return response;
    }

    /**
     * Get a list from item JSON, falling back to system JSON if not found
     * 
     * @param json Config object containing the json data
     * @param key path to the data in the config file
     * @param separator The separator to use in splitting the string to a list
     * @return List<String> containing the config data
     */
    private List<String> getList(JsonSimple json, String separator,
            Object... path) {
        String configEntry = get(json, null, path);
        if (configEntry == null) {
            return new ArrayList();
        } else {
            return split(configEntry, separator);
        }
    }

    /**
     * Get data from item JSON, falling back to system JSON, then to provided
     * default value if not found
     * 
     * @param json Config object containing the json data
     * @param value default to use if not found
     * @param path to the data in the config file
     * @return String containing the config data
     */
    private String get(JsonSimple json, String value, Object... path) {
        String configEntry = null;
        // Try item config if provided
        if (json != null) {
            configEntry = json.getString(null, path);
        }
        // Fallback to system config
        if (configEntry == null) {
            // This time specify our default value
            configEntry = config.getString(value, path);
        }
        return configEntry;
    }

    /**
     * Simple wrapper for commonly used function, use StringUtils to split a
     * string in an array and then transform it into a list.
     * 
     * @param original : The original string to split
     * @param separator : The separator to split on
     * @return List<String> : The resulting list of split strings
     */
    private List<String> split(String original, String separator) {
        return Arrays.asList(StringUtils.split(original, separator));
    }

    /**
     * Shut down the transformer plugin
     */
    @Override
    public void shutdown() throws PluginException {
        // Shutdown the database
        if (stats != null) {
            try {
                stats.shutdown();
            } catch (Exception ex) {
                log.error("Error shutting down database: ", ex);
                throw new PluginException(ex);
            }
        }
    }

}
