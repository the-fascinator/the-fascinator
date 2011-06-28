/* 
 * The Fascinator - Common Library
 * Copyright (C) 2008-2009 University of Southern Queensland
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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the Fascinator home directory.
 * 
 * Default Fascinator home directory: ${user.home}/.fascinator
 * 
 * NOTE: ${user.home} in:
 * <ul>
 * <li>Window: %USERPROFILE%</li>
 * <li>Linux/Mac OS X: ~ or $HOME</li>
 * </ul>
 * 
 * To change Fascinator home directory add FASCINATOR_HOME environment variable
 * 
 * @author Oliver Lucido
 */
public class FascinatorHome {

    /** Logging */
    public static Logger log = LoggerFactory.getLogger(FascinatorHome.class);

    /** System property key */
    public static final String SYSTEM_KEY = "fascinator.home";

    /** Default Fascinator home directory */
    public static final String DEFAULT_PATH = StrSubstitutor
            .replaceSystemProperties(FilenameUtils
                    .separatorsToSystem("${user.home}/.fascinator"));

    static {
        // cache the home path as a system property
        String home = getPath();
        System.setProperty(SYSTEM_KEY, home);
        log.info("Set Fascinator home to '{}'", home);
    }

    /**
     * Gets the Fascinator home directory, which is used for configuration,
     * storage, etc. The directory can be set through the following:
     * 
     * <ul>
     * <li>Environment variable: FASCINATOR_HOME</li>
     * <li>System property: fascinator.home</li>
     * </ul>
     * 
     * @return a directory path
     */
    public static String getPath() {
        String home = System.getProperty(SYSTEM_KEY, getenv("FASCINATOR_HOME",
                DEFAULT_PATH));
        return FilenameUtils.separatorsToSystem(home);
    }

    /**
     * Gets a relative directory inside the Fascinator home directory.
     * 
     * @param subDir the directory name
     * @return a directory path
     */
    public static String getPath(String subDir) {
        return getPath() + File.separator
                + FilenameUtils.separatorsToSystem(subDir);
    }

    /**
     * Gets the Fascinator home directory as a File object.
     * 
     * @return a File representing the Fascinator home directory
     */
    public static File getPathFile() {
        return new File(getPath());
    }

    /**
     * Gets a relative directory inside the Fascinator home directory.
     * 
     * @param subDir the directory name
     * @return a File representing the directory
     */
    public static File getPathFile(String subDir) {
        return new File(getPath(subDir));
    }

    /**
     * Gets an environment variable with default value if not set.
     * 
     * @param name environment variable name
     * @param def default value to return if environment variable not set
     * @return environment variable value
     */
    private static String getenv(String name, String def) {
        String value = System.getenv(name);
        if (value == null) {
            return def;
        }
        return value;
    }
}
