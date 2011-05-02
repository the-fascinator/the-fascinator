/*
 * The Fascinator - Plugin - File System Harvester - Derby Cache
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

package au.edu.usq.fascinator.harvester.filesystem;

import au.edu.usq.fascinator.common.JsonSimpleConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class is designed to encapsulate all logic required to run a cache
 * backed into a derby database in support of the file system harvester. There
 * are two caches available.
 * <p>
 *
 * <ul>
 *   <li><b>Basic</b>: The file is considered 'cached' if the last modified date
 * matches the database entry. On some operating systems (like linux) this can
 * provide a minimum of around 2 seconds of granularity. For most purposes this
 * is sufficient, and this cache is the most efficient.</li>
 *   <li><b>Hashed</b>: The entire contents of the file are SHA hashed and the
 * hash is stored in the database. The file is considered cached if the old hash
 * matches the new hash. This approach will only trigger a harvest if the
 * contents of the file really change, but it is quite slow across large data
 * sets and large files.</li>
 * </ul>
 *
 * <p>
 * Some form of caching must be enabled to support deletion detection for this
 * plugin.
 * </p>
 * @author Greg Pendlebury
 */
public class DerbyCache {
    /** Logging */
    private final Logger log = LoggerFactory.getLogger(DerbyCache.class);

    /** JDBC Driver */
    private static String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    /** Connection string prefix */
    private static String DERBY_PROTOCOL = "jdbc:derby:";

    /** Database name */
    private static String DATABASE_NAME = "fsHarvestCache";

    /** Basic table */
    private static String BASIC_TABLE = "basic";

    /** Hash table */
    private static String HASH_TABLE = "hashed";

    /** Database home directory */
    private String derbyHome;

    /** Database connection */
    private Connection connection;

    /** Are we using the database cache */
    private boolean useCache;
    private String cacheType;
    private String cacheId;

    public DerbyCache(JsonSimpleConfig config) throws Exception {
        cacheType = config.getString(null,
                "harvester", "file-system", "caching");
        cacheId = config.getString(null,
                "harvester", "file-system", "cacheId");
        if (cacheType != null && cacheId != null &&
                (cacheType.equals("basic") || cacheType.equals("hashed"))) {
            useCache = true;
            startDatabase(config);
        } else {
            log.error("Caching is either disabled or not configured properly:");
            log.error("Cache Type: '{}'", cacheType);
            log.error("Cache ID: '{}'", cacheId);
        }
    }

    private void startDatabase(JsonSimpleConfig config) throws Exception {
        // Find data directory
        derbyHome = config.getString(null,
                "database-service", "derbyHome");
        String oldHome = System.getProperty("derby.system.home");

        // Derby's data directory has already been configured
        if (oldHome != null) {
            if (derbyHome != null) {
                // Use the existing one, but throw a warning
                log.warn("Using previously specified data directory:" +
                        " '{}', provided value has been ignored: '{}'",
                        oldHome, derbyHome);
            } else {
                // This is ok, no configuration conflicts
                log.info("Using existing data directory: '{}'", oldHome);
            }

        // We don't have one, config MUST have one
        } else {
            if (derbyHome == null) {
                log.error("No database home directory configured!");
                return;
            } else {
                // Establish its validity and existance, create if necessary
                File file = new File(derbyHome);
                if (file.exists()) {
                    if (!file.isDirectory()) {
                        throw new Exception("Database home '" +
                                derbyHome + "' is not a directory!");
                    }
                } else {
                    file.mkdirs();
                    if (!file.exists()) {
                        throw new Exception("Database home '" +
                                derbyHome +
                                "' does not exist and could not be created!");
                    }
                }
                System.setProperty("derby.system.home", derbyHome);
            }
        }

        // Database prep work
        try {
            checkTable(BASIC_TABLE);
            checkTable(HASH_TABLE);
        } catch (SQLException ex) {
            log.error("Error during database preparation:", ex);
            throw new Exception(
                    "Error during database preparation:", ex);
        }
        //log.debug("Derby caching database online!");
    }

    private Connection connection() throws SQLException {
        if (connection == null || !connection.isValid(1)) {
            // At least try to close if not null... even though its not valid
            if (connection != null) {
                log.error("!!! Database connection has failed, recreating.");
                try {
                    connection.close();
                } catch (SQLException ex) {
                    log.error("Error closing invalid connection, ignoring: {}",
                            ex.getMessage());
                }
            }

            // Open a new connection
            Properties props = new Properties();
            // Load the JDBC driver
            try {
                Class.forName(DERBY_DRIVER).newInstance();
            } catch (Exception ex) {
                log.error("Driver load failed: ", ex);
                throw new SQLException("Driver load failed: ", ex);
            }

            // Establish a database connection
            connection = DriverManager.getConnection(DERBY_PROTOCOL +
                    DATABASE_NAME + ";create=true", props);
        }
        return connection;
    }

    /**
     * <p>
     * Check whether or not the file has changed according to the configured
     * cache. The response from this method should be used as an indicator on
     * whether or not to proceed with harvesting the file. In that context:
     * </p>
     *
     * <ul>
     *   <li>If there are any errors or exceptions accessing or assessing the
     * file, the return value will be <b>false</b>.</li>
     *   <li>If no caches are configured at all, the return value will be
     * <b>true</b>.</li>
     *   <li>If any cache is turned on and the file appears to have changed,
     * the return value will be <b>true</b>.</li>
     *   <li>If any cache is turned on and the file <b>does not</b> appear to
     * have changed, the return value will be <b>false</b>.</li>
     * </ul>
     *
     * @param file : The file to test.
     * @return boolean : <b>True</b> if the harvest should proceed, otherwise
     * <b>false</b>.
     */
    public boolean hasChanged(String oid, File file) {
        //log.debug("hasChanged('{}', '{}')", oid, file.getAbsolutePath());
        // Sanity check
        if (file == null || !file.exists()) {
            return false;
        }
        // Cache check
        if (useCache) {
            try {
                if (cacheType.equals("basic")) {
                    return checkBasicCache(oid, file);
                }
                if (cacheType.equals("hashed")) {
                    return checkHashedCache(oid, file);
                }
            } catch (Exception ex) {
                log.error("Error during cache process: ", ex);
                return false;
            }
        }
        // Fallback, just approve it
        return true;
    }

    /**
     * <p>
     * Used to support deletion detection. Should be called at the beginning of
     * a harvest to reset the flags in the database.
     * </p>
     *
     */
    public void resetFlags() {
        //log.debug("resetFlags()");
        if (useCache) {
            try {
                // Run whichever update is required
                PreparedStatement sql = null;
                if (cacheType.equals("basic")) {
                    sql = connection().prepareStatement(
                            "UPDATE " + BASIC_TABLE + " SET changeFlag = 0" +
                            " WHERE cacheId = '" + cacheId + "'");
                }

                if (cacheType.equals("hashed")) {
                    sql = connection().prepareStatement(
                            "UPDATE " + HASH_TABLE + " SET changeFlag = 0" +
                            " WHERE cacheId = '" + cacheId + "'");
                }
                sql.executeUpdate();
                close(sql);

            } catch (Exception ex) {
                log.error("Error updating cache to reset flags: ", ex);
            }
        }
    }

    /**
     * <p>
     * Used to support deletion detection. This method is called after the
     * harvest, and will return a list of all file paths
     * </p>
     *
     * @return Set<String>: A list of all object IDs in the cache which have
     * not been 'touched'
     */
    public Set<String> getUnsetFlags() {
        //log.debug("getUnsetFlags()");
        Set<String> response = null;

        if (useCache) {
            try {
                // Run whichever update is required
                ResultSet result = null;
                PreparedStatement sql = null;
                if (cacheType.equals("basic")) {
                    sql = connection().prepareStatement(
                            "SELECT oid FROM " + BASIC_TABLE +
                            " WHERE changeFlag = 0" +
                            " AND cacheId = '" + cacheId + "'");
                }

                if (cacheType.equals("hashed")) {
                    sql = connection().prepareStatement(
                            "SELECT oid FROM " + HASH_TABLE +
                            " WHERE changeFlag = 0" +
                            " AND cacheId = '" + cacheId + "'");
                }

                // Build response
                response = new HashSet();
                result = sql.executeQuery();
                while (result.next()) {
                    String oid = result.getString("oid");
                    if (oid != null) {
                        response.add(oid);
                    }
                }
                close(result);
                close(sql);

            } catch (Exception ex) {
                log.error("Error updating cache to reset flags: ", ex);
            }
        }

        return response;
    }

    /**
     * <p>
     * Used to support deletion detection. This method is just for cleanup
     * after the process completes.
     * </p>
     *
     */
    public void purgeUnsetFlags() {
        //log.debug("purgeUnsetFlags()");
        if (useCache) {
            try {
                // Run whichever update is required
                PreparedStatement sql = null;
                if (cacheType.equals("basic")) {
                    sql = connection().prepareStatement(
                            "DELETE FROM " + BASIC_TABLE +
                            " WHERE changeFlag = 0" +
                            " AND cacheId = '" + cacheId + "'");
                }

                if (cacheType.equals("hashed")) {
                    sql = connection().prepareStatement(
                            "DELETE FROM " + HASH_TABLE +
                            " WHERE changeFlag = 0" +
                            " AND cacheId = '" + cacheId + "'");
                }

                sql.executeUpdate();
                close(sql);
            } catch (Exception ex) {
                log.error("Error updating cache to delete data: ", ex);
            }
        }
    }

    private boolean checkBasicCache(String oid, File file) throws Exception {
        //log.debug("checkBasicCache('{}', '{}')", oid, file.getAbsolutePath());
        // What do we know?
        long lastCached = getLastModified(oid);
        long lastModified = file.lastModified();

        // Now decide the return value
        //log.debug("BASIC : cache({}) vs. file({})", lastCached, lastModified);
        if (lastCached == -1l) {
            // First time... insert and return true
            //log.debug("BASIC : TRUE (INSERT) : ({})", oid);
            insertLastModified(oid, file.lastModified());
            return true;
        } else {
            // Force an update... even if unchanged, the flag avoids deletes
            updateLastModified(oid, file.lastModified());

            if (lastModified > lastCached) {
                // Data has changed... return true
                //log.debug("BASIC : TRUE (UPDATE) : ({})", oid);
                return true;
            }
        }

        // No luck
        //log.debug("BASIC : FALSE : ({})", oid);
        return false;
    }

    private boolean checkHashedCache(String oid, File file) throws Exception {
        //log.debug("checkHashedCache('{}', '{}')", oid, file.getAbsolutePath());
        // What do we know?
        String cachedHash = getHash(oid);
        String currentHash = hashFile(file);

        // Now decide the return value
        //log.debug("HASHED : cache({}) vs. file({})", cachedHash, currentHash);
        if (cachedHash == null) {
            // First time... insert and return true
            //log.debug("HASHED : TRUE (INSERT) : ({})", oid);
            insertHash(oid, currentHash);
            return true;
        } else {
            // Force an update... even if unchanged, the flag avoids deletes
            updateHash(oid, currentHash);

            if (!currentHash.equals(cachedHash)) {
                // Data has changed... return true
                //log.debug("HASHED : TRUE (UPDATE) : ({})", oid);
                return true;
            }
        }

        // No luck
        //log.debug("HASHED : FALSE : ({})", oid);
        return false;
    }

    /**
     * Shutdown the database connections and cleanup.
     *
     * @throws Exception if there are errors
     */
    public void shutdown() throws Exception {
        // Derby can only be shutdown from one thread,
        //    we'll catch errors from the rest.
        String threadedShutdownMessage = DERBY_DRIVER
                + " is not registered with the JDBC driver manager";
        try {
            // Tell the database to close
            //DriverManager.getConnection(DERBY_PROTOCOL + ";shutdown=true");
            // Shutdown just this database (but not the engine)
            DriverManager.getConnection(DERBY_PROTOCOL + DATABASE_NAME +
                    ";shutdown=true");
        } catch (SQLException ex) {
            // These test values are used if the engine is NOT shutdown
            if (ex.getErrorCode() == 45000 &&
                    ex.getSQLState().equals("08006")) {

            // Valid response
            //if (ex.getErrorCode() == 50000 &&
            //        ex.getSQLState().equals("XJ015")) {
            // Error response
            } else {
                // Make sure we ignore simple thread issues
                //if (!ex.getMessage().equals(threadedShutdownMessage)) {
                //    throw new Exception("Error during database shutdown:", ex);
                //}
            }
        } finally {
            try {
                // Close our connection
                if (connection != null) {
                    connection.close();
                    connection = null;
                }
            } catch (SQLException ex) {
                throw new Exception("Error closing connection:", ex);
            }
        }
    }

    private long getLastModified(String oid) {
        try {
            PreparedStatement sql = connection().prepareStatement(
                    "SELECT lastModified FROM " + BASIC_TABLE +
                    " WHERE oid = ? AND cacheId = ?");

            // Prepare and execute
            sql.setString(1, oid);
            sql.setString(2, cacheId);
            ResultSet result = sql.executeQuery();

            // Build response
            Timestamp ts = null;
            if (result.next()) {
                ts = result.getTimestamp("lastModified");
            }
            close(result);
            close(sql);

            if (ts == null) {
                return -1;
            } else {
                return ts.getTime();
            }
        } catch(SQLException ex) {
            log.error("Error querying last modified date: ", ex);
            return -1;
        }
    }

    private void insertLastModified(String oid, long lastModified)
            throws Exception {
        PreparedStatement sql = connection().prepareStatement(
                "INSERT INTO " + BASIC_TABLE +
                " (oid, cacheId, lastModified, changeFlag)" +
                " VALUES (?, ?, ?, 1)");

        // Prepare and execute
        sql.setString(1, oid);
        sql.setString(2, cacheId);
        sql.setTimestamp(3, new Timestamp(lastModified));
        sql.executeUpdate();
        close(sql);
    }

    private void updateLastModified(String oid, long lastModified)
            throws Exception {
        PreparedStatement sql = connection().prepareStatement(
                "UPDATE " + BASIC_TABLE +
                " SET lastModified = ?, changeFlag = 1" +
                " WHERE oid = ? and cacheId = ?");

        // Prepare and execute
        sql.setTimestamp(1, new Timestamp(lastModified));
        sql.setString(2, oid);
        sql.setString(3, cacheId);
        sql.executeUpdate();
        close(sql);
    }

    private String getHash(String oid) {
        try {
            PreparedStatement sql = connection().prepareStatement(
                    "SELECT hash FROM " + HASH_TABLE +
                    " WHERE oid = ? AND cacheId = ?");

            // Prepare and execute
            sql.setString(1, oid);
            sql.setString(2, cacheId);
            ResultSet result = sql.executeQuery();

            // Build response
            String response = null;
            if (result.next()) {
                response = result.getString("hash");
            }
            close(result);
            close(sql);

            return response;
        } catch(SQLException ex) {
            log.error("Error querying last hash: ", ex);
            return null;
        }
    }

    private void insertHash(String oid, String hash)
            throws Exception {
        PreparedStatement sql = connection().prepareStatement(
                "INSERT INTO " + HASH_TABLE +
                " (oid, cacheId, hash, changeFlag) VALUES (?, ?, ?, 1)");

        // Prepare and execute
        sql.setString(1, oid);
        sql.setString(2, cacheId);
        sql.setString(3, hash);
        sql.executeUpdate();
        close(sql);
    }

    private void updateHash(String oid, String hash)
            throws Exception {
        PreparedStatement sql = connection().prepareStatement(
                "UPDATE " + HASH_TABLE +
                " SET hash = ?, changeFlag = 1" +
                " WHERE oid = ? AND cacheId = ?");

        // Prepare and execute
        sql.setString(1, hash);
        sql.setString(2, oid);
        sql.setString(3, cacheId);
        sql.executeUpdate();
        close(sql);
    }

    /**
     * Check for the existence of a table and arrange for its creation if
     * not found.
     *
     * @param table The table to look for and create.
     * @throws SQLException if there was an error.
     */
    private void checkTable(String table) throws SQLException {
        boolean tableFound = findTable(table);

        // Create the table if we couldn't find it
        if (!tableFound) {
            log.debug("Table '{}' not found, creating now!", table);
            createTable(table);

            // Double check it was created
            if (!findTable(table)) {
                log.error("Unknown error creating table '{}'", table);
                throw new SQLException(
                        "Could not find or create table '" + table + "'");
            }
        }
    }

    /**
     * Check if the given table exists in the database.
     *
     * @param table The table to look for
     * @return boolean flag if the table was found or not
     * @throws SQLException if there was an error accessing the database
     */
    private boolean findTable(String table) throws SQLException {
        boolean tableFound = false;
        DatabaseMetaData meta = connection().getMetaData();
        ResultSet result = (ResultSet) meta.getTables(null, null, null, null);
        while (result.next() && !tableFound) {
            if (result.getString("TABLE_NAME").equalsIgnoreCase(table)) {
                tableFound = true;
            }
        }
        close(result);
        return tableFound;
    }

    /**
     * Create the given table in the database.
     *
     * @param table The table to create
     * @throws SQLException if there was an error during creation,
     *                      or an unknown table was specified.
     */
    private void createTable(String table) throws SQLException {
        if (table.equals(BASIC_TABLE)) {
            Statement sql = connection().createStatement();
            sql.execute(
                    "CREATE TABLE " + BASIC_TABLE +
                    "(oid VARCHAR(255) NOT NULL, " +
                    "cacheId VARCHAR(255) NOT NULL, " +
                    "lastModified TIMESTAMP NOT NULL, " +
                    "changeFlag SMALLINT NOT NULL, " +
                    "PRIMARY KEY (oid, cacheId))");
            close(sql);
            return;
        }
        if (table.equals(HASH_TABLE)) {
            Statement sql = connection().createStatement();
            sql.execute(
                    "CREATE TABLE " + HASH_TABLE +
                    "(oid VARCHAR(255) NOT NULL, " +
                    "cacheId VARCHAR(255) NOT NULL, " +
                    "hash VARCHAR(50) NOT NULL, " +
                    "changeFlag SMALLINT NOT NULL, " +
                    "PRIMARY KEY (oid, cacheId))");
            close(sql);
            return;
        }
        throw new SQLException("Unknown table '" + table + "' requested!");
    }

    /**
     * Attempt to close a ResultSet. Basic wrapper for exception
     * catching and logging
     *
     * @param resultSet The ResultSet to try and close.
     */
    private void close(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException ex) {
                log.error("Error closing result set: ", ex);
            }
        }
        resultSet = null;
    }

    /**
     * Attempt to close a Statement. Basic wrapper for exception
     * catching and logging
     *
     * @param statement The Statement to try and close.
     */
    private void close(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ex) {
                log.error("Error closing statement: ", ex);
            }
        }
        statement = null;
    }

    /*
     * Sourced (and adapted) from commons-codec-1.4, required since 1.4
     * contains bug which affects httpclient request headers
     */
    private static final int BUFFER_SIZE = 1024;
    private String hashFile(File file) throws IOException {
        InputStream data = new FileInputStream(file);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            byte[] buffer = new byte[BUFFER_SIZE];
            int read = data.read(buffer, 0, BUFFER_SIZE);
            while (read > -1) {
                digest.update(buffer, 0, read);
                read = data.read(buffer, 0, BUFFER_SIZE);
            }
            return new String(Hex.encodeHex(digest.digest()));
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae.getMessage());
        }
    }
}
