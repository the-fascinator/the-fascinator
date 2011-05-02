/*
 * The Fascinator - Plugin - Harvester - ICE2
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
package au.edu.usq.fascinator.harvester.ice2;

import au.edu.usq.fascinator.api.harvester.HarvesterException;
import au.edu.usq.fascinator.api.storage.DigitalObject;
import au.edu.usq.fascinator.api.storage.Payload;
import au.edu.usq.fascinator.api.storage.PayloadType;
import au.edu.usq.fascinator.api.storage.StorageException;
import au.edu.usq.fascinator.common.FascinatorHome;
import au.edu.usq.fascinator.common.JsonObject;
import au.edu.usq.fascinator.common.JsonSimple;
import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.common.harvester.impl.GenericHarvester;
import au.edu.usq.fascinator.common.storage.StorageUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import net.htmlparser.jericho.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This plugin ingests ICE2 courses directly from ICE. This harvester
 * understand the ICE specific content such as packaging and media objects.
 * </p>
 * 
 * <p>
 * For accessing the ICE rendering API see the ICE2 Transformer instead.
 * </p>
 * 
 * <h3>Configuration</h3>
 * <p>
 * Sample configuration file for ICE2 harvester: <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/trunk/plugins/harvester/ice2/src/main/resources/harvest/ice2.json"
 * >ice2.json</a>
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
 * <td>baseDir</td>
 * <td>Path of directory or file to be harvested</td>
 * <td><b>Yes</b></td>
 * <td>${user.home}/Documents/public/</td>
 * </tr>
 * 
 * <tr>
 * <td>ignoreFilter</td>
 * <td>Pipe-separated ('|') list of filename patterns to ignore</td>
 * <td>No</td>
 * <td>.svn|.ice|.*|~*|Thumbs.db|.DS_Store</td>
 * </tr>
 * 
 * <tr>
 * <td>targetCourses</td>
 * <td>Courses list to be harvested</td>
 * <td><b>Yes</b></td>
 * <td></td>
 * </tr>
 * 
 * <tr>
 * <td>ignoreCourses</td>
 * <td>Courses list to be ignored (comma separated)</td>
 * <td>No</td>
 * <td></td>
 * </tr>
 * 
 * <tr>
 * <td>link</td>
 * <td>Store the digital object as a link in the storage and point to the
 * original file in the file system</td>
 * <td>No</td>
 * <td>false</td>
 * </tr>
 * 
 * <tr>
 * <td>testRun</td>
 * <td>Do not harvest the course directory if it's a test run</td>
 * <td>No</td>
 * <td>false</td>
 * </tr>
 * </table>
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * Harvesting NSC1500 course and ignore those courses listen in ignoreCourses
 * list. Also ignore files that match the pattern found in ignoreFilter list.
 * 
 * <pre>
 *   "harvester": {
 *         "type": "ice2-harvester",
 *         "ice2-harvester": {
 *             "baseDir": "${user.home}/Documents/2010cw/",
 *             "ignoreFilter": ".svn|.DS_Store|.site|skin",
 *             "targetCourses": "NSC1500",
 *             "ignoreCourses": "CIV5704,ACC3101,ACC5218,CSC2402,CDS2001",
 *             "link": false,
 *             "testRun": false
 *         }
 *     }
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Rule file</h3>
 * <p>
 * Sample rule file for the ICE2 harvester: <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/trunk/plugins/harvester/ice2/src/main/resources/harvest/ice2.py"
 * >ice2.py</a>
 * </p>
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * <b>None</b>
 * </p>
 * 
 * @author Greg Pendlebury
 */
public class Ice2Harvester extends GenericHarvester {

    /** logging */
    private Logger log = LoggerFactory.getLogger(Ice2Harvester.class);

    /** What we do/don't look for */
    private static final String DEFAULT_IGNORE_PATTERNS = ".svn";
    private static String[] acceptedMedia = { "audio", "flash", "images",
            "presentations", "readings", "video", "breeze" };

    /** directories */
    private File tempDir;
    private File baseDir;
    private File currentDir;
    private File courseRoot;
    private Stack<File> subDirs;
    private Stack<File> iceDirs;

    /** stats */
    private int objectsCreated;
    private int filesProcessed;

    /** stack of ICE manifest files found */
    private Stack<File> iceMetadata;

    /** Flags */
    private boolean testRun;
    private boolean hasMore;
    private boolean link;

    /** filter used to ignore files matching specified patterns */
    private IgnoreFilter ignoreFilter;

    /** Course Data */
    private String semester;
    private String course;
    private List<String> targetCourses = null;
    private List<String> ignoreCourses = null;

    /** our python rendering engine */
    private PythonInterpreter python;

    /** A copy of the ICE manifest parsing code */
    private File iceManifestLib;
    private String iceManifestPath, iceManifestName, jsonName;

    /** Packager variables */
    private String username = "ICE";
    private File packageDir, workflowsDir;
    private DigitalObject pkgConfig, pkgRules;

    /**
     * File filter used to ignore specified files
     */
    private class IgnoreFilter implements FileFilter {

        /** wildcard patterns of files to ignore */
        private String[] patterns;

        public IgnoreFilter(String[] patterns) {
            this.patterns = patterns;
        }

        @Override
        public boolean accept(File path) {
            for (String pattern : patterns) {
                if (FilenameUtils.wildcardMatch(path.getName(), pattern)) {
                    return false;
                }
            }
            return true;
        }
    }

    public Ice2Harvester() {
        super("ice2-harvester", "ICE2 Harvester");
    }

    @Override
    public void init() throws HarvesterException {
        // Init stats
        objectsCreated = 0;
        filesProcessed = 0;

        // Caching area for html marshalling
        String tempPath = System.getProperty("java.io.tmpdir");
        tempDir = new File(tempPath, "ice2Harvest");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }

        // Base directory of harvested content
        baseDir = new File(getJsonConfig().getString(".",
                "harvester", "ice2-harvester", "baseDir"));
        // File to ignore inside directory
        ignoreFilter = new IgnoreFilter(getJsonConfig().getString(
                DEFAULT_IGNORE_PATTERNS, "harvester", "ice2-harvester",
                "ignoreFilter").split("\\|"));

        // Course to specifically look for
        String courseList = getJsonConfig().getString(null,
                "harvester", "ice2-harvester", "targetCourses");
        if (courseList != null && !courseList.isEmpty()) {
            targetCourses = Arrays.asList(StringUtils.split(courseList, ','));
        }

        // Courses we are ignoring
        courseList = getJsonConfig().getString(null,
                "harvester", "ice2-harvester", "ignoreCourses");
        if (courseList != null && !courseList.isEmpty()) {
            ignoreCourses = Arrays.asList(StringUtils.split(courseList, ','));
        }

        // Harvest completely into storage or harvest a link back to disk?
        link = getJsonConfig().getBoolean(false,
                "harvester", "ice2-harvester", "link");
        // If the testRun flag is on, we're not really harvesting
        testRun = getJsonConfig().getBoolean(false,
                "harvester", "ice2-harvester", "testRun");

        // Directory traversal variables
        currentDir = baseDir;
        subDirs = new Stack<File>();
        iceDirs = new Stack<File>();
        iceMetadata = new Stack<File>();
        hasMore = true;

        // Files, directories and objects for preparing packages
        packageDir = FascinatorHome.getPathFile("packages");
        if (!packageDir.exists()) {
            packageDir.mkdirs();
        }
        workflowsDir = FascinatorHome.getPathFile("harvest/workflows");
        if (!workflowsDir.exists()) {
            workflowsDir.mkdirs();
        }
        try {
            File configFile = getFile(workflowsDir, "packaging-config.json");
            File rulesFile = getFile(workflowsDir, "packaging-rules.py");
            pkgConfig = StorageUtils.storeFile(getStorage(), configFile);
            pkgRules = StorageUtils.storeFile(getStorage(), rulesFile);
        } catch (Exception ex) {
            throw new HarvesterException(ex);
        }

        // Python scripting
        python = new PythonInterpreter();
        iceManifestLib = null;
    }

    /**
     * Interface method the HarvestClient uses to iteratively retrieve harvested
     * objects.
     */
    @Override
    public Set<String> getObjectIdList() throws HarvesterException {
        if (currentDir.isDirectory()) {
            // Don't try traversing through media directories
            if (!currentDir.getName().equals("media")) {
                // Traverse our directory
                for (File file : currentDir.listFiles(ignoreFilter)) {
                    if (file.isDirectory()) {
                        // Store it for further traversal
                        subDirs.push(file);
                        // Have we found an ICE directory?
                        if (file.getName().equals(".ice")) {
                            iceDirs.push(file);
                        }
                    }
                }
            }
            // Test for more sub-directories
            hasMore = !subDirs.isEmpty();
            if (hasMore) {
                currentDir = subDirs.pop();
            }
        } else {
            hasMore = false;
        }

        // Proceed to next step if we found an .ice directory
        if (iceDirs.size() > 0) {
            return findIceMetadata();
        } else {
            return new HashSet<String>();
        }
    }

    /**
     * Traverse an '.ice' directory and find an ICE 'meta' file.
     */
    private Set<String> findIceMetadata() throws HarvesterException {
        File file, fileDir, metaFile = null;
        // For each .ice directory we've found
        while (!iceDirs.empty()) {
            file = iceDirs.pop();
            fileDir = new File(file, "__dir__");
            if (fileDir.exists() && fileDir.isDirectory()) {
                metaFile = new File(fileDir, "meta");
                if (metaFile.exists()) {
                    iceMetadata.push(metaFile);
                } else {
                    log.error("Expected ICE manifest not found : '" + metaFile
                            + "'");
                }
            } else {
                log.error("Expected ICE directory not found : '" + fileDir
                        + "'");
            }

        }
        return parseIceMetadata();
    }

    /**
     * Unserialize and parse the ICE 'meta' file looking for a manifest.
     */
    private Set<String> parseIceMetadata() throws HarvesterException {
        // Some basic initialisation
        File file = null;
        InputStream iceParser = null;
        String responseJson, responseGuid = null;

        // Cache our Python libraries for parsing manifests
        if (iceManifestLib == null) {
            // For ICE manifest - copy of the manifest code direct from ICE
            iceParser = getClass().getResourceAsStream("/plugin_manifest.py");
            try {
                iceManifestLib = File.createTempFile("iceParser", ".py");
                iceManifestLib.deleteOnExit();
                FileOutputStream out = new FileOutputStream(iceManifestLib);
                IOUtils.copy(iceParser, out);
                out.close();
                iceParser.close();
                iceManifestPath = iceManifestLib.getParent();
                iceManifestName = FilenameUtils.getBaseName(iceManifestLib
                        .getName());
            } catch (IOException ex) {
                log.error("Error caching ICE parser : ", ex);
                return new HashSet<String>();
            }

            // JSON generation - Jython 2.5 doesn't have Python 2.6's JSON
            //   libraries, so we need to get one.
            iceParser = getClass().getResourceAsStream("/json.py");
            try {
                File json = File.createTempFile("json", ".py");
                json.deleteOnExit();
                FileOutputStream out = new FileOutputStream(json);
                IOUtils.copy(iceParser, out);
                out.close();
                iceParser.close();
                jsonName = FilenameUtils.getBaseName(json.getName());
            } catch (IOException ex) {
                log.error("Error caching JSON library : ", ex);
                return new HashSet<String>();
            }
        }

        // Now on to the parsing
        // Loop through all the ICE metadata files we found
        while (!iceMetadata.empty()) {
            // Prepare the data
            file = iceMetadata.pop();
            iceParser = getClass().getResourceAsStream("/ice_item.py");
            responseJson = "";
            responseGuid = "";

            // Run the ICE parser
            python.set("filePath", file.getAbsoluteFile());
            python.set("responseJson", responseJson);
            python.set("responseGuid", responseGuid);
            python.set("parsePath", iceManifestPath);
            python.set("parseLib", iceManifestName);
            python.set("jsonLib", jsonName);
            python.execfile(iceParser);

            // Grab the JSON response and cleanup
            responseGuid = python.get("responseGuid", String.class);
            responseJson = python.get("responseJson", String.class);
            python.cleanup();

            // Check response is valid
            // The parser won't return a GUID unless it found a manifest
            if (responseGuid != null) {
                // Step back up through '/.ice/__dir__/'
                courseRoot = file.getParentFile().getParentFile()
                        .getParentFile();
                // Some course metadata
                semester = courseRoot.getName().toUpperCase();
                course = courseRoot.getParentFile().getParentFile().getName();
                course = (course + courseRoot.getParentFile().getName())
                        .toUpperCase();
                // Process if we are looking for this one
                if (ignoreCourses == null || !ignoreCourses.contains(course)) {
                    if (targetCourses == null || targetCourses.contains(course)) {
                        log.debug("PROCESS : " + course + " : " + semester);
                        return processIceManifest(responseJson);
                    } else {
                        log.debug("IGNORE : " + course + " : " + semester);
                    }
                }
            }
        }
        return new HashSet<String>();
    }

    /**
     * Process the data in an ICE manifest, looking for the
     */
    private Set<String> processIceManifest(String responseJson)
            throws HarvesterException {
        Set<String> fileObjectIdList = new HashSet<String>();

        // Get the 'manifest' node of the metadata and
        //   parse it into a useful object.
        JsonSimple jsonManifest;
        try {
            jsonManifest = new JsonSimple(responseJson);
        } catch (IOException ex) {
            log.error("Error in manifest JSON : ", ex);
            return new HashSet<String>();
        }

        // Top level metadata
        String title = jsonManifest.getString(null, "title");
        String home = jsonManifest.getString(null, "homePage");
        List<JsonSimple> children = new ArrayList<JsonSimple>();
        List<JsonSimple> toc = JsonSimple.toJavaList(
                jsonManifest.getArray("toc"));

        // We only want 'visible' children. A (potentially) enormous number
        //  of non-visible media objects can be listed as top-level children.
        for (JsonSimple entry : toc) {
            boolean visible = entry.getBoolean(false, "visible");
            if (visible) {
                children.add(entry);
            }
        }

        // *** Harvesting
        // Convert responses from the functions below into a simple list of IDs
        Map<String, String> responseMap = new HashMap<String, String>();
        JsonSimple icePackage = prepareObject(title, home, children,
                responseMap);
        //log.debug("\n *** ICE2 : Package -\n{}", icePackage.toString());

        // *** Packaging
        // Make sure it's not empty
        if (!icePackage.toString().equals("{}")) {
            if (title == null) {
                title = "Untitled";
            }
            // Create the manifest file
            String packageId = DigestUtils.md5Hex(title);
            File manifestFile = new File(packageDir, packageId + ".tfpackage");
            try {
                FileUtils.writeStringToFile(manifestFile,
                        icePackage.toString(), "utf-8");
                // Harvest the manifest file
                try {
                    if (!testRun) {
                        DigitalObject object = StorageUtils.storeFile(
                                getStorage(), manifestFile);
                        manifestFile.delete();
                        try {
                            Properties props = object.getMetadata();
                            props.setProperty("rulesOid", pkgRules.getId());
                            props.setProperty("rulesPid",
                                    pkgRules.getSourceId());
                            props.setProperty("jsonConfigOid",
                                    pkgConfig.getId());
                            props.setProperty("jsonConfigPid",
                                    pkgConfig.getSourceId());
                            props.setProperty("usq-course", course);
                            props.setProperty("usq-semester", semester);
                            props.setProperty("owner", username);
                            props.setProperty("item-type", "Course Manifest");
                        } catch (StorageException stEx) {
                            log.error("Error accessing manifest metadata : ",
                                    stEx);
                        }

                        responseMap.put(title, object.getId());
                        object.close();
                    }
                } catch (Exception ex) {
                    log.error("Error storing manifest : ", ex);
                }
            } catch (IOException ioEx) {
                log.error("Error writing manifest file to disk : ", ioEx);
            }
        }

        // *** Returning
        if (!testRun) {
            for (String key : responseMap.keySet()) {
                if (responseMap.get(key) != null) {
                    fileObjectIdList.add(responseMap.get(key));
                }
            }
        }
        return fileObjectIdList;
    }

    private JsonSimple prepareObject(String title, String rootDoc,
            List<JsonSimple> children, Map<String, String> objectIdMap)
            throws HarvesterException {
        return prepareObject(title, rootDoc, children, objectIdMap, 0);
    }

    /**
     * A recursively used function to prepare objects for harvest.
     * 
     * @param title The ICE title of this object, used to index objects in the
     * global Map
     * @param rootDoc The top-level document for this object
     * @param children The manifest entries for any children of this object, its
     * presence triggers the creation of a package.
     * @param objectIdMap A global Map of all objects harvested so far, used to
     * construct internal links and avoid duplication
     * @param level The depth in the manifest of the current object
     * @return a DigitalObject, the object that was just harvested, useful
     * during recursion of children
     * @throws HarvesterException for any errors
     */
    private JsonSimple prepareObject(String title, String rootDoc,
            List<JsonSimple> children, Map<String, String> objectIdMap,
            int level) throws HarvesterException {
        // This object
        DigitalObject object = null;
        JsonObject objectData = new JsonObject();
        Map<String, JsonSimple> allChildren = new LinkedHashMap();
        if (title == null) {
            title = "Untitled";
        }
        //log.debug(" *** ICE2 : Title (" + level + ") '" + title + "' => '" + rootDoc + "'");

        // Child variables - Harvesting
        String childTitle, childHome = null;
        List<JsonSimple> grandChildren = null;

        // Child variables - Packaging
        JsonSimple childData;
        Map<String, JsonSimple> childManifest;

        // Process the manifest children first
        for (JsonSimple child : children) {
            // Prepare metadata
            childTitle = child.getString(null, "title");
            childHome = child.getString(null, "relPath");
            grandChildren = JsonSimple.toJavaList(child.getArray("children"));

            // Process the childen
            childData = prepareObject(childTitle, childHome, grandChildren,
                    objectIdMap, level + 1);
            childManifest = JsonSimple.toJavaMap(
                    childData.getObject("manifest"));

            // Remember them for later
            for (String key : childManifest.keySet()) {
                allChildren.put(key, childManifest.get(key));
            }
        }

        // Now find the html rendition of this file
        File rootFile = getOriginalDoc(courseRoot, rootDoc);
        File htmlDir = null;
        try {
            htmlDir = getHtmlRendition(rootFile);
        } catch (IOException ex) {
            // Nothing, leave it to the test below
        }
        if (htmlDir == null || !htmlDir.exists()) {
            //log.warn(" *** ICE2 : Root document not found, skipping");

        } else {
            // Is this an object we've previsouly harvested?
            if (!objectIdMap.keySet().contains(title)) {
                object = harvestHtml(rootFile, htmlDir, title, objectIdMap);
            } else {
                // Do nothing, we'll get the oid from the global Map later
            }
        }

        // Top level package
        if (level == 0) {
            // Pointless if we didn't harvest anything
            if (allChildren.size() > 0) {
                objectData.put("title", title);
                objectData.put("manifest", JsonSimple.fromJavaMap(allChildren));
            }
            //log.debug(objectData.toString());

            // We are only building 'part' packages
            //    to be collected recursively.
        } else {
            if (object != null || testRun) {
                String md5, oid = null;
                objectData.put("title", title);
                if (testRun) {
                    oid = "testRunObject:" + title;
                } else {
                    oid = object.getId();
                }
                md5 = DigestUtils.md5Hex(oid);
                JsonObject t2Data = new JsonObject();
                t2Data.put("id", oid);
                t2Data.put("title", title);
                if (allChildren.size() > 0) {
                    t2Data.put("children", JsonSimple.fromJavaMap(allChildren));
                }
                JsonObject t1Data = new JsonObject();
                t1Data.put("node-" + md5, t2Data);
                objectData.put("manifest", t1Data);
            }
        }
        return new JsonSimple(objectData);
    }

    private DigitalObject harvestHtml(File rootFile, File htmlDir,
            String title, Map<String, String> objectIdMap)
            throws HarvesterException {
        //log.debug("harvestHtml() start : {}", title);
        File htmlFile = new File(htmlDir, htmlDir.getName() + ".html");
        List<Element> images, links, params = null;
        DigitalObject object = null;

        try {
            String content = FileUtils.readFileToString(htmlFile);
            Source source = new Source(content);
            OutputDocument htmlOut = new OutputDocument(source);
            source.setLogger(null);

            images = source.getAllElements(HTMLElementName.IMG);
            for (Element image : images) {
                // TODO
                //These are NOT images from a rendition, those are handled later.
                //These images will be need to added as payloads, but we need to
                //  test whether we can even access each image.
                // Following that we need to link to change the link to the image.
            }

            // Links, replace with object references if required
            links = source.getAllElements(HTMLElementName.A);
            for (Element aLink : links) {
                Attributes attr = aLink.getAttributes();
                String replacement = "<a ";
                boolean target = false;
                for (Iterator i = attr.iterator(); i.hasNext();) {
                    Attribute a = (Attribute) i.next();
                    if (a.getName().equals("href")) {
                        // Ignore legitimate web links
                        if (!a.getValue().startsWith("http")
                                && !a.getValue().startsWith("mailto")
                                && !a.getValue().isEmpty()) {
                            String href = a.getValue().replace("%20", " ");
                            String newLink = harvestLink(htmlFile, href,
                                    objectIdMap);
                            if (!newLink.equals(href)) {
                                target = true;
                                //log.debug(" *** ICE2 : Link : '" + href + "'");
                                replacement += "href=\"" + newLink + "\" ";
                            } else {
                                replacement += "href=\"" + a.getValue() + "\" ";
                            }
                        } else {
                            replacement += "href=\"" + a.getValue() + "\" ";
                        }
                    }
                }
                replacement += ">" + aLink.getContent().toString() + "</a>";
                if (target) {
                    htmlOut.replace(aLink, replacement);
                }
            }

            params = source.getAllElements(HTMLElementName.PARAM);
            for (Element param : params) {
                // TODO - Rip out the video 'object' element
                //        and replace with an oEmbed tag
                //log.debug(" *** ICE2 : Param : '"
                //        + param.getAttributeValue("name") + "' => '"
                //        + param.getAttributeValue("value") + "'");
                if (param.getAttributeValue("name").equals("movie")) {
                    String newMovie = harvestVideo(htmlFile,
                            param.getAttributeValue("value"), objectIdMap);
                }
            }

            // Create digital object
            try {
                // Create the object of the original
                object = createObject(rootFile, "Document");
                // Stream our custom html back to disk
                FileUtils.writeStringToFile(htmlFile, htmlOut.toString());
                // Add the html render of the file
                Payload payload = addPayload(object, htmlFile, "");
                if (!testRun) {
                    payload.setType(PayloadType.Preview);
                    payload.close();
                }
                File dcXml = new File(htmlDir, "dc.xml");
                if (dcXml.exists()) {
                    addPayload(object, dcXml, "");
                }
                File imgDir = new File(htmlDir, htmlDir.getName() + "_files");
                if (imgDir.exists()) {
                    addPayload(object, imgDir, "");
                }
                // Log the object creation
                if (testRun) {
                    objectIdMap.put(title, "testRunObject:" + title);
                } else {
                    objectIdMap.put(title, object.getId());
                }
            } catch (StorageException ex) {
                log.error("Error storing html : '" + title + "' : ", ex);
            }

        } catch (IOException ex) {
            log.error("Error reading file : ", ex);
        }

        return object;
    }

    private String harvestVideo(File htmlFile, String oldLink,
            Map<String, String> objectIdMap) throws HarvesterException {
        //log.debug("harvestVideo() start : {}", oldLink);
        // Sometimes we need to separate the video from the player
        if (oldLink.contains("/player_")) {
            // Find the base of the video path
            int i = oldLink.lastIndexOf("/");
            String baseLink = oldLink.substring(0, i);
            // Then the actual video file
            String ending = oldLink.substring(i + 1);
            int j = ending.lastIndexOf("=");
            String video = baseLink + "/" + ending.substring(j + 1);
            // Now go harvest the video
            String returnValue = harvestLink(htmlFile, video, objectIdMap);
            if (returnValue.equals(video)) {
                return oldLink;
            } else {
                return returnValue;
            }
        }
        return oldLink;
    }

    private String harvestLink(File htmlFile, String oldLink,
            Map<String, String> objectIdMap) throws HarvesterException {
        //log.debug("harvestLink() start : {}", oldLink);
        // Normalise relative links
        String index = oldLink;
        if (oldLink.startsWith("../")) {
            index = index.substring(3);
        }
        // Is this an object we've previsouly harvested?
        if (objectIdMap.keySet().contains(index)) {
            return objectIdMap.get(index);
        }

        // Media, handle separately
        if (index.startsWith("media/")) {
            String mediaOid = harvestMedia(htmlFile, oldLink, objectIdMap);
            if (mediaOid == null) {
                return oldLink;
            } else {
                return "tfObject:" + mediaOid;
            }

            // A link to another document
        } else {
            String DocOid = harvestDocument(htmlFile, oldLink, objectIdMap);
            if (DocOid == null) {
                return oldLink;
            } else {
                return "tfObject:" + DocOid;
            }

        }
    }

    private String harvestDocument(File htmlFile, String oldLink,
            Map<String, String> objectIdMap) throws HarvesterException {
        //log.debug("harvestDocument() start : {}", oldLink);
        // Normalise relative links
        String index = oldLink;
        if (oldLink.startsWith("../")) {
            index = index.substring(3);
        }
        if (index.contains("#")) {
            index = index.substring(0, index.indexOf("#"));
        }
        if (index.contains("?")) {
            index = index.substring(0, index.indexOf("?"));
        }
        // TODO - Suffixes like anchors and parameters need to be retained and
        //       handled on the detail screen when the object link is resolved.

        //log.debug(" *** ICE2 : Link = '" + index + "'");
        DigitalObject object = null;

        // First, it could be a direct link to a document in the package
        File file = new File(courseRoot, oldLink);
        if (!file.exists()) {
            // Secondly, it could be relative link to a document
            file = new File(htmlFile.getParentFile(), oldLink);
        }

        // Did we find either?
        if (file.exists()) {
            File htmlDir = null;
            // Can we find a rendition of this document?
            try {
                htmlDir = getHtmlRendition(file);
            } catch (IOException ex) {
                // Nothing, leave it to the test below
            }

            // Can we find a rendition of this document?
            if (htmlDir != null && htmlDir.exists()) {
                object = harvestHtml(file, htmlDir, oldLink, objectIdMap);
                if (testRun) {
                    return "tfObject:testRunObject";
                } else {
                    return "tfObject:" + object.getId();
                }

                // No, time to harvest the original
            } else {
                // TODO - Determine whether we're really harvesting html
                //  or need to render a document.
                object = harvestHtml(file, file.getParentFile(), index,
                        objectIdMap);
                if (testRun) {
                    return "tfObject:" + oldLink;
                } else {
                    return "tfObject:" + object.getId();
                }
            }
        }

        if (object == null && !testRun) {
            return null;
        } else {
            if (testRun) {
                return "tfObject:" + oldLink;
            } else {
                return "tfObject:" + object.getId();
            }
        }
    }

    private String harvestMedia(File htmlFile, String oldLink,
            Map<String, String> objectIdMap) throws HarvesterException {
        //log.debug("harvestMedia() start : {}", oldLink);
        // Normalise relative links
        String filePath = oldLink;
        if (oldLink.startsWith("../")) {
            filePath = filePath.substring(3);
        }
        if (filePath.contains("#")) {
            filePath = filePath.substring(0, filePath.indexOf("#"));
        }
        if (filePath.contains("?")) {
            filePath = filePath.substring(0, filePath.indexOf("?"));
        }
        // TODO - Suffixes like anchors and parameters need to be retained and
        //        andled on the detail screen when the object link is resolved.

        File media = new File(courseRoot, filePath);
        DigitalObject object = null;
        if (media.exists()) {
            String fileType = FilenameUtils.getExtension(media.getName());
            String subFilePath = filePath.substring(6);
            int firstSlash = subFilePath.indexOf("/");

            // Unfiled media
            if (firstSlash == -1) {
                try {
                    log.warn("Harvesting unfiled media object: '{}'",
                            media.getAbsolutePath());
                    object = createObject(media, "Unknown Media");
                } catch (HarvesterException ex) {
                    log.error("Error storing file : ", ex);
                } catch (StorageException ex) {
                    log.error("Error storing file : ", ex);
                }
            } else {
                String mediaType = subFilePath.substring(0, firstSlash);
                subFilePath = subFilePath.substring(firstSlash + 1);

                // Treat as a document if we don't support it
                if (!Arrays.asList(acceptedMedia).contains(mediaType)) {
                    return harvestDocument(htmlFile, oldLink, objectIdMap);
                } else {
                    try {
                        if (mediaType.equals("audio")) {
                            //log.debug(" *** ICE2 : Audio => '"+ subFilePath + "'");
                            object = createObject(media, "Audio", true);
                        }
                        if (mediaType.equals("flash")) {
                            // TODO - Need test data
                        }
                        if (mediaType.equals("images")) {
                            //log.debug(" *** ICE2 : Image => '"+ subFilePath + "'");
                            object = createObject(media, "Image", true);
                        }
                        if (mediaType.equals("presentations")
                                || mediaType.equals("breeze")) {
                            // Likely a package - simple (v1) answer is to 'gulch'
                            //  everything in the same directory and all sub-dirs
                            // TODO - Prepare in such a way as to be handled
                            //        by the IMS transformer.
                            if (fileType.contains("htm")) {
                                //log.debug(" *** ICE2 : HTML Presentation => '"
                                //        + subFilePath + "'");
                                object = createObject(media, "Presentation");
                                File mediaRoot = media.getParentFile();
                                File[] files = mediaRoot
                                        .listFiles(ignoreFilter);
                                for (File f : files) {
                                    if (!f.getName().equals(".ice")) {
                                        addPayload(object, f, "");
                                    }
                                }

                                // Single Files
                            } else {
                                // TODO = Renditions required on PPT files (at least)
                                //log.debug(" *** ICE2 : Presentation => '"
                                //        + subFilePath + "'");
                                object = createObject(media, "Presentation",
                                        true);
                            }
                        }
                        if (mediaType.equals("readings")) {
                            // TODO - Need data, and what to do with documents? render?
                        }
                        if (mediaType.equals("video")) {
                            //log.debug(" *** ICE2 : Video => '"+ subFilePath + "'");
                            object = createObject(media, "Video", true);
                        }
                    } catch (HarvesterException ex) {
                        log.error("Error storing file : ", ex);
                    } catch (StorageException ex) {
                        log.error("Error storing file : ", ex);
                    }
                }
            }
        }

        if (object == null && !testRun) {
            log.error("Media object not found : '" + media.getAbsolutePath()
                    + "'");
            return null;

        } else {
            if (testRun) {
                objectIdMap.put(filePath, filePath);
                return filePath;
            } else {
                objectIdMap.put(filePath, object.getId());
                return object.getId();
            }
        }
    }

    private File getOriginalDoc(File origDir, String manifestFileName) {
        File file = new File(origDir, manifestFileName);
        //log.debug("getOriginalDoc() start : {}", manifestFileName);
        String fileName = file.getName();

        String simpleName = FilenameUtils.getBaseName(fileName);
        File simpleDir = file.getParentFile();
        // If we can't find it now, it doesn't exist
        if (!simpleDir.exists()) {
            return null;
        }
        // Find the original name of our file
        File[] files = simpleDir.listFiles();
        for (File f : files) {
            if (f.isFile() && f.getName().startsWith(simpleName)) {
                return f;
            }
        }
        return null;
    }

    private File getHtmlRendition(File srcFile) throws IOException {
        //log.debug("getHtmlRendition() start : {}", srcFile);
        if (srcFile == null) {
            return null;
        }
        // Prepate our temp space
        String simpleName = FilenameUtils.getBaseName(srcFile.getName());
        File htmlDir = new File(tempDir, simpleName);
        if (!htmlDir.exists()) {
            htmlDir.mkdir();
        }
        htmlDir.deleteOnExit();
        File imgDir = new File(htmlDir, simpleName + "_files");
        if (!imgDir.exists()) {
            imgDir.mkdir();
        } else {
            // !! important - so many files use the same basename
            //  eg. 'module2.doc' and we don't want to mix them
            purgeDirectory(imgDir);
        }
        imgDir.deleteOnExit();

        // Go check for the renditions directory
        boolean found = false;
        File renditionDir = new File(srcFile.getParentFile(), ".ice/"
                + srcFile.getName());
        if (renditionDir.exists() && renditionDir.isDirectory()) {
            // Loop through all available renditions
            File[] files = renditionDir.listFiles();
            for (File f : files) {
                // Html rendition
                if (f.getName().endsWith("xhtml.body")) {
                    found = true;
                    File htmlFile = new File(htmlDir, simpleName + ".html");
                    if (!htmlFile.exists()) {
                        htmlFile.createNewFile();
                    }
                    htmlFile.deleteOnExit();
                    FileOutputStream htmlFileOut = new FileOutputStream(
                            htmlFile);
                    FileInputStream htmlFileIn = new FileInputStream(f);
                    IOUtils.copy(htmlFileIn, htmlFileOut);
                    htmlFileIn.close();
                    htmlFileOut.close();
                }
                // Dublin Core metadata
                if (f.getName().endsWith(".dc")) {
                    File dcFile = new File(htmlDir, "dc.xml");
                    if (!dcFile.exists()) {
                        dcFile.createNewFile();
                    }
                    dcFile.deleteOnExit();
                    FileOutputStream dcFileOut = new FileOutputStream(dcFile);
                    FileInputStream dcFileIn = new FileInputStream(f);
                    IOUtils.copy(dcFileIn, dcFileOut);
                    dcFileIn.close();
                    dcFileOut.close();
                }
                // Images
                if (f.getName().startsWith("image-")) {
                    File imgFile = new File(imgDir, f.getName().substring(6));
                    if (!imgFile.exists()) {
                        imgFile.createNewFile();
                    }
                    imgFile.deleteOnExit();
                    FileOutputStream imgFileOut = new FileOutputStream(imgFile);
                    FileInputStream imgFileIn = new FileInputStream(f);
                    IOUtils.copy(imgFileIn, imgFileOut);
                    imgFileIn.close();
                    imgFileOut.close();
                }
            }
        }

        if (found) {
            return htmlDir;
        } else {
            return null;
        }
    }

    private void purgeDirectory(File dir) {
        if (!dir.isDirectory())
            return;

        File[] files = dir.listFiles();
        for (File f : files) {
            f.delete();
        }
    }

    @Override
    public boolean hasMoreObjects() {
        if (!hasMore) {
            log.info("COMPLETE: {} Files => {} Objects.", filesProcessed,
                    objectsCreated);
        }
        return hasMore;
    }

    private DigitalObject createObject(File file, String itemType)
            throws HarvesterException, StorageException {
        return createObject(file, itemType, false);
    }

    private DigitalObject createObject(File file, String itemType,
            boolean render) throws HarvesterException, StorageException {
        objectsCreated++;
        filesProcessed++;

        if (testRun) {
            return null;
        }

        DigitalObject object = StorageUtils.storeFile(getStorage(), file, link);

        // update object metadata
        Properties props = object.getMetadata();
        props.setProperty("usq-course", course);
        props.setProperty("usq-semester", semester);
        props.setProperty("render-pending", "true");
        props.setProperty("file.path",
                FilenameUtils.separatorsToUnix(file.getAbsolutePath()));
        props.setProperty("item-type", itemType);

        if (render) {
            props.setProperty("harvestQueue", "aperture");
            props.setProperty("indexOnHarvest", "true");
            props.setProperty("renderQueue", "ffmpeg,ice2");
        } else {
            props.setProperty("harvestQueue", "");
            props.setProperty("indexOnHarvest", "false");
            props.setProperty("renderQueue", "aperture");
        }

        object.close();
        return object;
    }

    private Payload addPayload(DigitalObject object, File file, String prefix)
            throws HarvesterException, StorageException {
        filesProcessed++;

        if (testRun) {
            return null;
        }

        String pid = StorageUtils.generatePid(file);
        // Make sure we don't add the source again
        if (pid.equals(object.getSourceId())) {
            return null;
        }
        if (!prefix.equals("")) {
            prefix += "/";
        }
        pid = prefix + pid;

        //log.debug("Adding payload to object : '" + file.getAbsolutePath() + "'");
        if (file.isDirectory()) {
            File[] files = file.listFiles(ignoreFilter);
            for (File f : files) {
                if (!f.getName().equals(".ice")) {
                    addPayload(object, f, prefix + file.getName());
                }
            }
        } else {
            try {
                InputStream in = new FileInputStream(file);
                if (link) {
                    return StorageUtils.createOrUpdatePayload(object, pid, in,
                            file.getAbsolutePath());
                } else {
                    return StorageUtils.createOrUpdatePayload(object, pid, in);
                }
            } catch (FileNotFoundException ex) {
                log.error("Error accessing file : ", ex);
            }
        }
        return null;
    }

    private File getFile(File location, String fileName)
            throws FileNotFoundException, IOException {
        File file = new File(location, fileName);
        if (!file.exists()) {
            FileOutputStream out = new FileOutputStream(file);
            IOUtils.copy(
                    this.getClass().getResourceAsStream(
                            "/workflows/" + fileName), out);
            out.close();
        }
        return file;
    }
}