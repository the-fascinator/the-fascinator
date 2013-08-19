/* 
 * The Fascinator - File System Harvester Plugin
 * Copyright (C) 2009-2011 University of Southern Queensland
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
package com.googlecode.fascinator.harvester.filesystem;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.fascinator.api.harvester.HarvesterException;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.harvester.impl.GenericHarvester;
import com.googlecode.fascinator.common.storage.StorageUtils;

/**
 * <p>
 * This plugin harvests files in a specified directory or a specified file on
 * the local file system. it can use a cache to do incremental harvests, which
 * only harvests files that have changed since the last time it was run. system.
 * </p>
 * 
 * <h3>Configuration</h3>
 * <p>
 * Sample configuration file for file system harvester: <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/trunk/plugins/harvester/filesystem/src/main/resources/harvest/local-files.json"
 * >local-files.json</a>
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
 * <td>facetDir</td>
 * <td>Used to specify the top level directory for the file_path facet</td>
 * <td>No</td>
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
 * <td>recursive</td>
 * <td>Set true to harvest files recursively</td>
 * <td>No</td>
 * <td>true</td>
 * </tr>
 * 
 * <tr>
 * <td>force</td>
 * <td>Force harvest the specified directory or file again even when it's not
 * modified (ignore cache)</td>
 * <td>No</td>
 * <td>false</td>
 * </tr>
 * 
 * <tr>
 * <td>link</td>
 * <td>Store the digital object as a link in the storage and point to the
 * original file in the file system</td>
 * <td>No</td>
 * <td>true</td>
 * </tr>
 * 
 * <tr>
 * <td>caching</td>
 * <td>Caching method to use. Valid entries are 'basic' and 'hashed'</td>
 * <td>No</td>
 * <td>null</td>
 * </tr>
 * 
 * <tr>
 * <td>cacheId</td>
 * <td>The cache ID to use in the database if caching is in use.</td>
 * <td>Yes (if valid 'caching' value is provided)</td>
 * <td>null</td>
 * </tr>
 * 
 * <tr>
 * <td>derbyHome</td>
 * <td>Path to use for the file store of the database. Should match other Derby
 * paths provided in the configuration file for the application.</td>
 * <td>Yes (if valid 'caching' value is provided)</td>
 * <td>null</td>
 * </tr>
 * </table>
 * 
 * <h3>Caching</h3> With regards to the underlying cache you have three options
 * for configuration:
 * <ol>
 * <li>No caching: All files will always be be harvested. Be aware that without
 * caching there is no support for deletion.</li>
 * <li><b>Basic</b> caching: The file is considered 'cached' if the last
 * modified date matches the database entry. On some operating systems (like
 * linux) this can provide a minimum of around 2 seconds of granularity. For
 * most purposes this is sufficient, and this cache is the most efficient.</li>
 * <li><b>Hashed</b> caching: The entire contents of the file are SHA hashed and
 * the hash is stored in the database. The file is considered cached if the old
 * hash matches the new hash. This approach will only trigger a harvest if the
 * contents of the file really change, but it is quite slow across large data
 * sets and large files.</li>
 * </ol>
 * Deletion support is provided by any configured cache. After the standard
 * harvest is performed any 'stale' cache entries are considered to targets for
 * deletion. This is why the 'cacheId' is particularly important, because you
 * don't want cache entries from a different harvest configuration getting
 * deleted.
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * Harvesting ${user.home}/Documents/public/ directory recursively. Ignore files
 * with the filename match the pattern specified in the ignoreFilter. The
 * harvest includes the files in the subdirectory, and do not re-harvest
 * unmodified file if the file exist in the cache database under the 'default'
 * cache.
 * 
 * <pre>
 *   "harvester": {
 *      "type": "file-system",
 *      "file-system": {
 *          "targets": [
 *              {
 *                  "baseDir": "${user.home}/Documents/public/",
 *                  "facetDir": "${user.home}/Documents/public/",
 *                  "ignoreFilter": ".svn|.ice|.*|~*|Thumbs.db|.DS_Store",
 *                  "recursive": true,
 *                  "force": false,
 *                  "link": true
 *              }
 *          ],
 *          "caching": "basic",
 *          "cacheId": "default",
 *          "derbyHome" : "${fascinator.home}/database"
 *      }
 *  }
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Rule file</h3>
 * <p>
 * Sample rule file for the file system harvester: <a href=
 * "https://fascinator.usq.edu.au/trac/browser/code/the-fascinator2/trunk/plugins/harvester/filesystem/src/main/resources/harvest/local-files.py"
 * >local-files.py</a>
 * </p>
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * <b>None</b>
 * </p>
 * 
 * @author Oliver Lucido
 */
public class FileSystemHarvester extends GenericHarvester {

    /** default ignore list */
    private static final String DEFAULT_IGNORE_PATTERNS = ".svn";

    /** logging */
    private Logger log = LoggerFactory.getLogger(FileSystemHarvester.class);

    /** Harvesting targets */
    private List<JsonSimple> targets;

    /** Target index */
    private Integer targetIndex;

    /** Target index */
    private File nextFile;

    /** Stack of queued files to harvest */
    private Stack<File> fileStack;

    /** Path data for facet */
    private String facetBase;

    /** whether or not there are more files to harvest */
    private boolean hasMore;

    /** filter used to ignore files matching specified patterns */
    private IgnoreFilter ignoreFilter;

    /** whether or not to recursively harvest */
    private boolean recursive;

    /** force harvesting all files */
    private boolean force;

    /** use links instead of copying */
    private boolean link;

    /** Render chains */
    private Map<String, Map<String, List<String>>> renderChains;

    /** Caching */
    private DerbyCache cache;

    /** Delete Support? */
    private boolean supportDeletes;

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

    /**
     * File System Harvester Constructor
     */
    public FileSystemHarvester() {
        super("file-system", "File System Harvester");
    }

    /**
     * Initialisation of File system harvester plugin
     * 
     * @throws HarvesterException if fails to initialise
     */
    @Override
    public void init() throws HarvesterException {
        // Check for valid targests
        targets = getJsonConfig().getJsonSimpleList("harvester", "file-system",
                "targets");
        if (targets.isEmpty()) {
            throw new HarvesterException("No targets specified");
        }

        // Loop processing variables
        fileStack = new Stack<File>();
        targetIndex = null;
        hasMore = true;

        // Caching
        try {
            cache = new DerbyCache(getJsonConfig());
            // Reset flags for deletion support
            cache.resetFlags();
            // But don't support deletes until we've seen 'add' traffic
            // otherwise the flags will all be unset and everything will
            // be flagged for deletion
            supportDeletes = false;
        } catch (Exception ex) {
            log.error("Error instantiating cache: ", ex);
            throw new HarvesterException(ex);
        }

        // Rendering: Order is significant
        renderChains = new LinkedHashMap<String, Map<String, List<String>>>();
        Map<String, JsonSimple> renderTypes = getJsonConfig().getJsonSimpleMap(
                "renderTypes");
        if (renderTypes != null) {
            for (Entry<String, JsonSimple> entry : renderTypes.entrySet()) {
                Map<String, List<String>> details = new HashMap<String, List<String>>();
                details.put("fileTypes",
                        entry.getValue().getStringList("fileTypes"));
                details.put("harvestQueue",
                        entry.getValue().getStringList("harvestQueue"));
                details.put("indexOnHarvest",
                        entry.getValue().getStringList("indexOnHarvest"));
                details.put("renderQueue",
                        entry.getValue().getStringList("renderQueue"));
                renderChains.put(entry.getKey(), details);
            }
        }

        // Prep the first file
        nextFile = getNextFile();
    }

    /**
     * Get the next file due to be harvested
     * 
     * @return The next file to harvest, null if none
     */
    private File getNextFile() {
        File next = null;
        if (fileStack.empty()) {
            next = getNextTarget();
        } else {
            next = fileStack.pop();
        }
        if (next == null) {
            hasMore = false;
        }
        return next;
    }

    /**
     * Retrieve the next file specified as a target in configuration
     * 
     * @return The next target file, null if none
     */
    private File getNextTarget() {
        // First execution
        if (targetIndex == null) {
            targetIndex = Integer.valueOf(0);
        } else {
            targetIndex++;
        }

        // We're finished
        if (targetIndex >= targets.size()) {
            return null;
        }

        // Get the next target
        JsonSimple target = targets.get(targetIndex);
        String path = target.getString(null, "baseDir");
        if (path == null) {
            log.warn("No path provided for target, skipping!");
            return getNextTarget();

        } else {
            File file = new File(path);
            if (!file.exists()) {
                log.warn("Path '{}' does not exist, skipping!", path);
                return getNextTarget();

            } else {
                log.info("Target file/directory found: '{}'", path);
                updateConfig(target, path);
                return file;
            }
        }
    }

    /**
     * Update harvest configuration when switching target path
     * 
     * @param tConfig The target configuration
     * @param path The path to the target (used as default facet)
     */
    private void updateConfig(JsonSimple tConfig, String path) {
        recursive = tConfig.getBoolean(false, "recursive");
        ignoreFilter = new IgnoreFilter(tConfig.getString(
                DEFAULT_IGNORE_PATTERNS, "ignoreFilter").split("\\|"));
        force = tConfig.getBoolean(false, "force");
        link = tConfig.getBoolean(false, "link");
        facetBase = tConfig.getString(path, "facetDir");
    }

    /**
     * Shutdown the plugin
     * 
     * @throws HarvesterException is there are errors
     */
    @Override
    public void shutdown() throws HarvesterException {
        if (cache != null) {
            try {
                cache.shutdown();
            } catch (Exception ex) {
                log.error("Error shutting down cache: ", ex);
                throw new HarvesterException(ex);
            }
        }
    }

    /**
     * Harvest the next set of files, and return their Object IDs
     * 
     * @return Set<String> The set of object IDs just harvested
     * @throws HarvesterException is there are errors
     */
    @Override
    public Set<String> getObjectIdList() throws HarvesterException {
        Set<String> fileObjectIdList = new HashSet<String>();

        // We had no valid targets
        if (nextFile == null) {
            hasMore = false;
            return fileObjectIdList;
        }

        // Normal logic
        if (nextFile.isDirectory()) {
            File[] children = nextFile.listFiles(ignoreFilter);
            for (File child : children) {
                if (child.isDirectory()) {
                    if (recursive) {
                        fileStack.push(child);
                    }
                } else {
                    harvestFile(fileObjectIdList, child);
                }
            }

        } else {
            harvestFile(fileObjectIdList, nextFile);
        }

        // Progess the stack and return
        nextFile = getNextFile();
        return fileObjectIdList;
    }

    /**
     * Harvest a file based on configuration
     * 
     * @param list The set of harvested IDs to add to
     * @param file The file to harvest
     * @throws HarvesterException is there are errors
     */
    private void harvestFile(Set<String> list, File file)
            throws HarvesterException {
        // What OID will be used ID we did store this?
        String oid = StorageUtils.generateOid(file);
        // Check if it is in the cache, make sure the cache call come before
        // 'force' in the boolean OR so that the cache entry is 'touched'
        if (cache.hasChanged(oid, file) || force) {
            try {
                list.add(createDigitalObject(file));
            } catch (StorageException se) {
                log.warn("File not harvested {}: {}", file, se.getMessage());
            }
        }
    }

    /**
     * Check if there are more objects to harvest
     * 
     * @return <code>true</code> if there are more, <code>false</code> otherwise
     */
    @Override
    public boolean hasMoreObjects() {
        if (!hasMore) {
            // 'Add' harvesting must be run through to completeion before we
            // support deletes.
            supportDeletes = true;
        }
        return hasMore;
    }

    /**
     * Delete cached references to files which no longer exist and return the
     * set of IDs to delete from the system.
     * 
     * @return Set<String> The set of object IDs deleted
     * @throws HarvesterException is there are errors
     */
    @Override
    public Set<String> getDeletedObjectIdList() throws HarvesterException {
        if (!supportDeletes) {
            String msg = "This plugin only supports deletion if caching is"
                    + " enabled and all 'add' and 'update' harvesting has been"
                    + " processed first. Please ensure caching is configured"
                    + " correctly and that harvesting has continued until"
                    + " hasMoreObjects() returns false. ";
            throw new HarvesterException(msg);
        }

        // Make sure we don't get called twice
        supportDeletes = false;
        // Get our response data
        Set<String> response = cache.getUnsetFlags();
        // Clean up the cache
        cache.purgeUnsetFlags();

        return response;
    }

    /**
     * Check if there are more objects to delete
     * 
     * @return <code>true</code> if there are more, <code>false</code> otherwise
     */
    @Override
    public boolean hasMoreDeletedObjects() {
        return supportDeletes;
    }

    /**
     * Create digital object
     * 
     * @param file File to be transformed to be digital object
     * @return object id of created digital object
     * @throws HarvesterException if fail to create the object
     * @throws StorageException if fail to save the file to the storage
     */
    private String createDigitalObject(File file) throws HarvesterException,
            StorageException {
        DigitalObject object = StorageUtils.storeFile(getStorage(), file, link);

        // update object metadata
        Properties props = object.getMetadata();
        props.setProperty("render-pending", "true");
        props.setProperty("file.path",
                FilenameUtils.separatorsToUnix(file.getAbsolutePath()));
        props.setProperty("base.file.path",
                FilenameUtils.separatorsToUnix(facetBase));

        // Store rendition information if we have it
        String ext = FilenameUtils.getExtension(file.getName());
        for (String chain : renderChains.keySet()) {
            Map<String, List<String>> details = renderChains.get(chain);
            if (details.get("fileTypes").contains(ext)) {
                storeList(props, details, "harvestQueue");
                storeList(props, details, "indexOnHarvest");
                storeList(props, details, "renderQueue");
            }
        }

        object.close();
        return object.getId();
    }

    /**
     * Take a list of strings from a Java Map, concatenate the values together
     * and store them in a Properties object using the Map's original key.
     * 
     * @param props Properties object to store into
     * @param details The full Java Map
     * @param field The key to use in both objects
     */
    private void storeList(Properties props, Map<String, List<String>> details,
            String field) {
        Set<String> valueSet = new LinkedHashSet<String>();
        // merge with original property value if exists
        String currentValue = props.getProperty(field, "");
        if (!"".equals(currentValue)) {
            String[] currentList = currentValue.split(",");
            valueSet.addAll(Arrays.asList(currentList));
        }
        valueSet.addAll(details.get(field));
        String joinedList = StringUtils.join(valueSet, ",");
        props.setProperty(field, joinedList);
    }
}
