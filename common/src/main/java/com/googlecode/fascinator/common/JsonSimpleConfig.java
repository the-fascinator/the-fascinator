/*
 * The Fascinator - JSON Simple Config
 * Copyright (C) 2011 University of Southern Queensland
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An extension of the JsonSimple class specifically to access configuration.
 * </p>
 * 
 * <p>
 * Aside from offering a constructor that takes care of finding and accessing
 * the system configuration file, this class also offers a selection of methods
 * for management of the system configuration, such as backup and version
 * testing.
 * </p>
 * 
 * <p>
 * Finally, whatever configuration is provided to this class, it will be backed
 * by the full system configuration file. Nodes not found in the provided config
 * will also be checked in the System config.
 * </p>
 * 
 * @author Greg Pendlebury
 */
public class JsonSimpleConfig extends JsonSimple {

    /** Logging */
    private static Logger log = LoggerFactory.getLogger(JsonSimpleConfig.class);

    /** Default configuration directory */
    private static final String CONFIG_DIR = FascinatorHome.getPath();

    /** Default system configuration file name */
    private static final String SYSTEM_CONFIG_FILE = "system-config.json";

    /** Fallback to system configuration file */
    private JsonSimple systemConfig;

    private static final String INCLUDE_DIR_KEY = "includeConfigDir";
    private static final String INCLUDE_DIR_KEY_EXT = "includeConfigExt";

    /**
     * Creates JSON Configuration object from the system config file
     * 
     * @throws IOException if there was an error during creation
     */
    public JsonSimpleConfig() throws IOException {
        super(JsonSimpleConfig.getSystemFile());
        systemConfig = new JsonSimple(JsonSimpleConfig.getSystemFile());
        loadIncludeDir();
    }

    /**
     * Creates JSON Configuration object from the provided config file
     * 
     * @param jsonFile : The file containing JSON
     * @throws IOException if there was an error during creation
     */
    public JsonSimpleConfig(File jsonFile) throws IOException {
        super(jsonFile);
        systemConfig = new JsonSimple(JsonSimpleConfig.getSystemFile());
        loadIncludeDir();
    }

    /**
     * Creates JSON Configuration object from the provided input stream
     * 
     * @param jsonIn : The input stream to read
     * @throws IOException if there was an error during creation
     */
    public JsonSimpleConfig(InputStream jsonIn) throws IOException {
        super(jsonIn);
        systemConfig = new JsonSimple(JsonSimpleConfig.getSystemFile());
        loadIncludeDir();
    }

    /**
     * Creates JSON Configuration object from the provided config string
     * 
     * @param jsonString : The JSON in string form
     * @throws IOException if there was an error during creation
     */
    public JsonSimpleConfig(String jsonString) throws IOException {
        super(jsonString);
        systemConfig = new JsonSimple(JsonSimpleConfig.getSystemFile());
        loadIncludeDir();
    }

    /**
     * Performs a backup on the system-wide configuration file from the default
     * config dir if it exists. Returns a reference to the backed up file.
     * 
     * @return the backed up system JSON file
     * @throws IOException if there was an error reading or writing either file
     */
    public static File backupSystemFile() throws IOException {
        File configFile = new File(CONFIG_DIR, SYSTEM_CONFIG_FILE);
        File backupFile = new File(CONFIG_DIR, SYSTEM_CONFIG_FILE + ".old");
        if (!configFile.exists()) {
            throw new IOException("System file does not exist! '"
                    + configFile.getAbsolutePath() + "'");
        } else {
            if (backupFile.exists()) {
                backupFile.delete();
            }
            OutputStream out = new FileOutputStream(backupFile);
            InputStream in = new FileInputStream(configFile);
            IOUtils.copy(in, out);
            in.close();
            out.close();
            log.info("Configuration copied to '{}'", backupFile);
        }
        return backupFile;
    }

    /**
     * Gets the system-wide configuration file from the default config dir. If
     * the file doesn't exist, a default is copied to the config dir.
     * 
     * @return the system JSON file
     * @throws IOException if there was an error reading or writing the system
     *             configuration file
     */
    public static File getSystemFile() throws IOException {
        File configFile = new File(CONFIG_DIR, SYSTEM_CONFIG_FILE);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            OutputStream out = new FileOutputStream(configFile);
            IOUtils.copy(
                    JsonSimpleConfig.class.getResourceAsStream("/"
                            + SYSTEM_CONFIG_FILE), out);
            out.close();
            log.info("Default configuration copied to '{}'", configFile);
        }
        return configFile;
    }

    /**
     * Tests whether or not the system-config has been properly configured.
     * 
     * @return <code>true</code> if configured, <code>false</code> if still
     *         using defaults
     */
    public boolean isConfigured() {
        return getBoolean(false, "configured");
    }

    /**
     * To check if configuration file is outdated
     * 
     * @return <code>true</code> if outdated, <code>false</code> otherwise
     */
    public boolean isOutdated() {
        boolean outdated = false;
        String systemVersion = getString(null, "version");
        if (systemVersion == null) {
            return true;
        }

        try {
            JsonSimple compiledConfig = new JsonSimple(getClass()
                    .getResourceAsStream("/" + SYSTEM_CONFIG_FILE));
            String compiledVersion = compiledConfig.getString(null, "version");
            outdated = !systemVersion.equals(compiledVersion);
            if (compiledVersion == null) {
                return false;
            }
            if (outdated) {
                log.debug("Configuration versions do not match! '{}' != '{}'",
                        systemVersion, compiledVersion);
            }
        } catch (IOException ioe) {
            log.error("Failed to parse compiled configuration!", ioe);
        }
        return outdated;
    }

    /**
     * Walk down the JSON nodes specified by the path and retrieve the target
     * JSONArray.
     * 
     * @param path : Variable length array of path segments
     * @return JSONArray : The target node, or NULL if path invalid or not an
     *         array
     */
    @Override
    public JSONArray getArray(Object... path) {
        JSONArray array = super.getArray(path);
        if (array == null && systemConfig != null) {
            return systemConfig.getArray(path);
        }
        return array;
    }

    /**
     * Walk down the JSON nodes specified by the path and retrieve the target
     * JsonObject.
     * 
     * @param path : Variable length array of path segments
     * @return JsonObject : The target node, or NULL if path invalid or not an
     *         object
     */
    @Override
    public JsonObject getObject(Object... path) {
        JsonObject object = super.getObject(path);
        if (object == null && systemConfig != null) {
            return systemConfig.getObject(path);
        }
        return object;
    }

    /**
     * Walk down the JSON nodes specified by the path and retrieve the target.
     * 
     * @param path : Variable length array of path segments
     * @return Object : The target node, or NULL if invalid
     */
    @Override
    public Object getPath(Object... path) {
        Object object = super.getPath(path);
        if (object == null && systemConfig != null) {
            return systemConfig.getPath(path);
        }
        return object;
    }

    /**
     * Retrieve the Boolean value on the given path.
     * 
     * <strong>IMPORTANT:</strong> The default value only applies if the path is
     * not found. If a string on the path is found it will be considered
     * <b>false</b> unless the value is 'true' (ignoring case). This is the
     * default behaviour of the Boolean.parseBoolean() method.
     * 
     * @param defaultValue : The fallback value to use if the path is invalid or
     *            not found
     * @param path : An array of indeterminate length to use as the path
     * @return Boolean : The Boolean value found on the given path, or null if
     *         no default provided
     */
    @Override
    public Boolean getBoolean(Boolean defaultValue, Object... path) {
        Boolean bool = super.getBoolean(null, path);
        if (bool == null) {
            if (systemConfig != null) {
                return systemConfig.getBoolean(defaultValue, path);
            }
            return defaultValue;
        }
        return bool;
    }

    /**
     * Retrieve the Integer value on the given path.
     * 
     * @param defaultValue : The fallback value to use if the path is invalid or
     *            not found
     * @param path : An array of indeterminate length to use as the path
     * @return Integer : The Integer value found on the given path, or null if
     *         no default provided
     */
    @Override
    public Integer getInteger(Integer defaultValue, Object... path) {
        Integer integer = super.getInteger(null, path);
        if (integer == null) {
            if (systemConfig != null) {
                return systemConfig.getInteger(defaultValue, path);
            }
            return defaultValue;
        }
        return integer;
    }

    /**
     * Retrieve the String value on the given path.
     * 
     * @param defaultValue : The fallback value to use if the path is invalid or
     *            not found
     * @param path : An array of indeterminate length to use as the path
     * @return String : The String value found on the given path, or null if no
     *         default provided
     */
    @Override
    public String getString(String defaultValue, Object... path) {
        String string = super.getString(null, path);
        if (string == null) {
            if (systemConfig != null) {
                return systemConfig.getString(defaultValue, path);
            }
            return defaultValue;
        }
        return string;
    }

    /**
     * <p>
     * Retrieve a list of Strings found on the given path. Note that this is a
     * utility function, and not designed for data traversal. It <b>will</b>
     * only retrieve Strings found on the provided node, and the node must be a
     * JSONArray.
     * </p>
     * 
     * @param path : An array of indeterminate length to use as the path
     * @return List<String> : A list of Strings, null if the node is not found
     */
    @Override
    public List<String> getStringList(Object... path) {
        List<String> list = super.getStringList(path);
        if (list == null && systemConfig != null) {
            return systemConfig.getStringList(path);
        }
        return list;
    }

    /**
     * <p>
     * Retrieve a list of JsonSimple objects found on the given path. Note that
     * this is a utility function, and not designed for data traversal. It
     * <b>will</b> only retrieve valid JsonObjects found on the provided node,
     * and wrap them in JsonSimple objects.
     * </p>
     * 
     * <p>
     * Other objects found on that path will be ignored, and if the path itself
     * is not a JSONArray or not found, the function will return NULL.
     * </p>
     * 
     * @param path : An array of indeterminate length to use as the path
     * @return List<JsonSimple> : A list of JSONSimple objects, or null
     */
    @Override
    public List<JsonSimple> getJsonSimpleList(Object... path) {
        List<JsonSimple> list = super.getJsonSimpleList(path);
        if (list == null && systemConfig != null) {
            return systemConfig.getJsonSimpleList(path);
        }
        return list;
    }

    /**
     * <p>
     * Retrieve a map of JsonSimple objects found on the given path. Note that
     * this is a utility function, and not designed for data traversal. It
     * <b>will</b> only retrieve valid JsonObjects found on the provided node,
     * and wrap them in JsonSimple objects.
     * </p>
     * 
     * <p>
     * Other objects found on that path will be ignored, and if the path itself
     * is not a JsonObject or not found, the function will return NULL.
     * </p>
     * 
     * @param path : An array of indeterminate length to use as the path
     * @return Map<String, JsonSimple> : A map of JSONSimple objects, or null
     */
    @Override
    public Map<String, JsonSimple> getJsonSimpleMap(Object... path) {
        Map<String, JsonSimple> map = super.getJsonSimpleMap(path);
        if (map == null && systemConfig != null) {
            return systemConfig.getJsonSimpleMap(path);
        }
        return map;
    }

    /**
     * <p>
     * Search through the JSON for any nodes (at any depth) matching the
     * requested name and return them. The returned List will be of type Object
     * and require type interrogation for detailed use, but will be implemented
     * as a LinkedList to preserve order.
     * </p>
     * 
     * @param node : The node name we are looking for
     * @return List<Object> : A list of matching Objects from the data
     */
    @Override
    public List<Object> search(String node) {
        List<Object> results = super.search(node);
        if ((results == null || results.isEmpty()) && systemConfig != null) {
            return systemConfig.search(node);
        }
        return results;
    }

    /**
     * <p>
     * Returns a reference to the underlying system configuration object. This
     * is meant to be used on conjunction with storeSystemConfig() to make
     * changes to the config file on disk.
     * </p>
     * 
     * <p>
     * Normal modifications to this objects JSON are not written to disk unless
     * they they are made via writableSystemConfig().
     * </p>
     * 
     * @return JsonObject : A reference to the system configuration JSON object
     */
    public JsonObject writableSystemConfig() {
        return systemConfig.getJsonObject();
    }

    /**
     * <p>
     * Store the underlying system configuration on disk in the appropriate
     * location.
     * </p>
     * 
     * <p>
     * Normal modifications to this objects JSON are not written to disk unless
     * they they are made via writableSystemConfig().
     * </p>
     * 
     * @return JsonObject : A reference to the system configuration JSON object
     */
    public void storeSystemConfig() throws IOException {
        FileWriter writer = new FileWriter(JsonSimpleConfig.getSystemFile());
        writer.write(systemConfig.toString(true));
        writer.close();
    }

    /**
     * Loads all the config files found in INCLUDE_DIR_KEY property entry, that
     * have extensions in INCLUDE_DIR_KEY_EXT config array. The included
     * directory may contain subdirectories, and these are searched as well.
     * 
     * The entries are merged, with the last file included overwriting all
     * previous values. These includes Map and List entries, with the exception
     * of List of Maps, which is appended by default and not examined further.
     * Therefore, the sort order is important.
     * 
     * If the base property is of different type from the included property, the
     * included property will overwrite the base property.
     * 
     * For details, please look at JsonSimpleConfigTest.
     * 
     */
    private void loadIncludeDir() {
        boolean hasIncludedDir = getJsonObject().containsKey(INCLUDE_DIR_KEY);
        boolean systemHasIncludedDir = systemConfig.getJsonObject()
                .containsKey(INCLUDE_DIR_KEY);
        if (hasIncludedDir) {
            log.debug("Loading main included dir...");
            loadIncludedDir(this);
        } else {
            log.debug("Main config has no included dir, trying system config...");
        }
        if (systemHasIncludedDir) {
            log.debug("Loading system config included dir...");
            loadIncludedDir(systemConfig);
        } else {
            log.debug("System config has no included dir, moving on...");
        }
    }

    @SuppressWarnings(value = { "unchecked" })
    private void loadIncludedDir(JsonSimple config) {
        List<String> extList = config.getStringList(INCLUDE_DIR_KEY_EXT);
        log.debug(
                "Inclusion directory found:'" + INCLUDE_DIR_KEY
                        + "', merging all files in '"
                        + config.getString(null, INCLUDE_DIR_KEY)
                        + "' ending with: {}", extList);
        List<File> configFiles = new ArrayList(FileUtils.listFiles(new File(
                config.getString(null, INCLUDE_DIR_KEY)), extList
                .toArray(new String[extList.size()]), true));

        final Comparator<File> ALPHABETICAL_ORDER = new Comparator<File>() {
            public int compare(File file1, File file2) {
                int res = String.CASE_INSENSITIVE_ORDER.compare(
                        file1.getAbsolutePath(), file2.getAbsolutePath());
                if (res == 0) {
                    res = file1.getAbsolutePath().compareTo(
                            file2.getAbsolutePath());
                }
                return res;
            }
        };

        Collections.sort(configFiles, ALPHABETICAL_ORDER);

        for (File configFile : configFiles) {
            try {
                // log.debug("Merging included config file: {}",
                // configFile);
                JsonSimple jsonConfig = new JsonSimple(configFile);
                mergeConfig(config.getJsonObject(), jsonConfig.getJsonObject());
            } catch (IOException e) {
                log.error("Failed to load file: {}", configFile);
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings(value = { "unchecked" })
    private void mergeConfig(Map targetMap, Map srcMap) {
        for (Object key : srcMap.keySet()) {
            Object src = srcMap.get(key);
            Object target = targetMap.get(key);
            if (target == null) {
                targetMap.put(key, src);
            } else {
                if (src instanceof Map && target instanceof Map) {
                    mergeConfig((Map) target, (Map) src);
                } else if (src instanceof JSONArray
                        && target instanceof JSONArray) {
                    JSONArray srcArray = (JSONArray) src;
                    JSONArray targetArray = (JSONArray) target;
                    targetArray.addAll(srcArray);
                } else {
                    targetMap.put(key, src);
                }
            }
        }
    }

}
