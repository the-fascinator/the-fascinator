/*
 * The Fascinator - Portal - Database Services
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
package au.edu.usq.fascinator.portal.services.impl;

import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.portal.services.DatabaseServices;
import java.io.File;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instantiates a database used for persistence of generic data in the
 * scripting layer, and offers utility functions for same.
 *
 * @author Greg Pendlebury
 */
public class DatabaseServicesImpl implements DatabaseServices {

    /** JDBC Driver */
    private static String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    /** Connection string prefix */
    private static String DERBY_PROTOCOL = "jdbc:derby:";

    /** Logging */
    private Logger log = LoggerFactory.getLogger(DatabaseServicesImpl.class);

    /** System Configuration */
    private JsonSimpleConfig sysConfig;

    /** Database data directory */
    private String derbyHome;

    /** Flag to halt usage later */
    private boolean hasErrors;

    /** List of databases opened */
    private Map<String, Connection> dbConnections;

    /** List of statements opened */
    private Map<String, PreparedStatement> statements;

    /**
     * Basic constructor, run by Tapestry through injection.
     *
     */
    public DatabaseServicesImpl() {
        log.info("Database services starting...");
        hasErrors = false;

        try {
            sysConfig = new JsonSimpleConfig();
            // Find data directory
            derbyHome = sysConfig.getString(null,
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
                    hasErrors = true;
                    return;
                } else {
                    // Establish its validity and existance, create if necessary
                    File file = new File(derbyHome);
                    if (file.exists()) {
                        if (!file.isDirectory()) {
                            log.error("Database home '" +
                                    derbyHome + "' is not a directory!");
                            hasErrors = true;
                            return;
                        }
                    } else {
                        file.mkdirs();
                        if (!file.exists()) {
                            log.error("Database home '" + derbyHome +
                                 "' does not exist and could not be created!");
                            hasErrors = true;
                            return;
                        }
                    }
                    System.setProperty("derby.system.home", derbyHome);
                }
            }

            // Load the JDBC driver
            try {
                Class.forName(DERBY_DRIVER).newInstance();
            } catch (Exception ex) {
                log.error("JDBC Driver load failed: ", ex);
                hasErrors = true;
                return;
            }

            // Instantiate holding maps
            dbConnections = new HashMap();
            statements = new HashMap();

        } catch (IOException ex) {
            log.error("Failed to access system config", ex);
        }
    }

    private java.sql.Connection connection(String database, boolean create)
            throws SQLException {

        // Has it already been instantiated this session?
        Connection thisDatabase = null;
        if (dbConnections.containsKey(database)) {
            thisDatabase = dbConnections.get(database);
        }

        if (thisDatabase == null || !thisDatabase.isValid(1)) {
            // At least try to close if not null... even though its not valid
            if (thisDatabase != null) {
                log.error("Database connection '{}' has failed, recreating.",
                        database);
                try {
                    thisDatabase.close();
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

            // Connection string
            String connection = DERBY_PROTOCOL + database;
            if (create) {
                //log.debug("connect() '{}' SETTING CREATION FLAG", database);
                connection += ";create=true";
            }
            // Try to connect
            //log.debug("connect() '{}' ATTEMPTING CONNECTION", connection);
            thisDatabase = DriverManager.getConnection(
                    connection, new Properties());
            dbConnections.put(database, thisDatabase);
        }

        return thisDatabase;
    }

    /**
     * Tapestry notification that server is shutting down
     *
     */
    @Override
    public void registryDidShutdown() {
        log.info("Database services shutting down...");

        // Release all our queries
        for (String key : statements.keySet()) {
            close(statements.get(key));
        }

        // Shutdown database connections
        for (String key : dbConnections.keySet()) {
            try {
                dbConnections.get(key).close();
            } catch (SQLException ex) {
                log.error("Error closing database: ", ex);
            }
        }

        // Shutdown database engine
        // Derby can only be shutdown from one thread,
        //    we'll catch errors from the rest.
        String threadedShutdownMessage = DERBY_DRIVER
                + " is not registered with the JDBC driver manager";
        try {
            // Tell the database to close
            DriverManager.getConnection(DERBY_PROTOCOL + ";shutdown=true");
        } catch (SQLException ex) {
            // Valid response
            if (ex.getErrorCode() == 50000 &&
                    ex.getSQLState().equals("XJ015")) {
            // Error response
            } else {
                // Make sure we ignore simple thread issues
                if (!ex.getMessage().equals(threadedShutdownMessage)) {
                    log.warn("Error during database shutdown:", ex);
                }
            }
        }
    }

    /**
     * Return a connection to the specified database, failing if it does not
     * exist.
     *
     * @param database The name of the database to connect to.
     * @return Connection The instantiated database connection or NULL.
     * @throws Exception if there is a connection error.
     */
    @Override
    public Connection checkConnection(String database) throws Exception {
        try {
            //log.debug("checkConnection() '{}'", database);
            return connection(database, false);
        } catch (Exception ex) {
            //log.debug("checkConnection() '{}' FAIL", database);
            throw new Exception("Database does not exist", ex);
        }
    }

    /**
     * Return a connection to the specified database. The database will be
     * created if it does not exist.
     *
     * @param database The name of the database to connect to.
     * @return Connection The instantiated database connection, NULL if an error occurs.
     * @throws Exception if there is a connection error.
     */
    @Override
    public Connection getConnection(String database) throws Exception {
        try {
            //log.debug("getConnection() '{}'", database);
            return connection(database, true);
        } catch (Exception ex) {
            //log.debug("getConnection() '{}' FAIL", database);
            log.error("Error during database creation:", ex);
            throw ex;
        }
    }

    /**
     * Prepare and return an SQL statement, filing it under the provided index.
     * Subsequent calls to this function using the same index will return the
     * previously prepared statement.
     *
     * @param db The database connection to use.
     * @param index The index to store the statement under.
     * @param sql The SQL statement to prepare.
     * @return PreparedStatement The prepared statement.
     * @throws Exception if there is an error.
     */
    @Override
    public PreparedStatement prepare(Connection db, String index, String sql)
            throws Exception {
        //log.debug("prepare() '{}' SQL: \n===\n{}\n===", index, sql);
        PreparedStatement statement = statements.get(index);
        if (statement == null) {
            try {
                statement = db.prepareStatement(sql);
                statements.put(index, statement);
                //log.debug("prepare() '{}' SUCCESS", index);
            } catch (SQLException ex) {
                log.error("Error preparing statement:", ex);
                throw new Exception("Error preparing statement:", ex);
            }
        }
        return statement;
    }

    /**
     * Bind a parameter to a SQL statement. All Java types should be acceptable,
     * except NULL. Use 'IS NULL' in your SQL for this.
     *
     * @param sql The prepared statement to bind to.
     * @param index Specifies which placeholder to bind to (starts at 1).
     * @param data The data to bind to that placeholder.
     * @throws Exception if there is an error.
     */
    @Override
    public void bindParam(PreparedStatement sql, int index, Object data)
            throws Exception {
        try {
            //log.debug("bindParam() ({}) => '{}'", index, data);
            if (data == null) {
                throw new Exception(
                    "NULL values are not accepted. Use 'IS NULL' or similar!");
            } else {
                sql.setObject(index, data);
                //log.debug("bindParam() ({}) SUCCESS", index);
            }
        } catch (SQLException ex) {
            log.error("Error binding parameter:", ex);
            throw new Exception("Error binding parameter:", ex);
        }
    }

    /**
     * Free the resources for a prepared statement. For very commonly occurring
     * statements this is not necessarily advised since DatabaseServices tracks
     * all statements and will free them at server shutdown. The performance
     * gains are useful from this approach IF you use the same query routinely.
     *
     * @param sql The prepared statement to release.
     * @throws Exception if there is an error.
     */
    @Override
    public void free(PreparedStatement sql) throws Exception {
        close(sql);
    }

    /**
     * Parse the results of the query into a basic Java data structure. Users
     * wanting the original result set should call getResultSet() directly
     * against the prepared statement.
     *
     * @param sql The prepared statement to get the results from.
     * @return List<Map<String, String>> A list of result rows as key/value pairs in HashMaps
     * @throws Exception if there is an error.
     */
    @Override
    public List<Map<String, String>> getResults(PreparedStatement sql)
            throws Exception {
        // Prepare variables
        List<Map<String, String>> response = new ArrayList();
        ResultSet results = null;
        ResultSetMetaData columns = null;

        try {
            // Run the search
            results = sql.executeQuery();
            // Process the results
            columns = results.getMetaData();
            if (results.isClosed()) {
                log.error("!!! ResultSet is closed");
                return response;
            }
            while (results.next()) {
                Map<String, String> row = new HashMap();
                for (int i = 1; i <= columns.getColumnCount(); i++) {
                    //log.debug("getResults(): Storing '{}' ({}) => " +
                    //        results.getString(i), columns.getColumnName(i),
                    //        columns.getColumnLabel(i));
                    row.put(columns.getColumnName(i), results.getString(i));
                }
                response.add(row);
            }
            // Finish up
            results.close();
            return response;

        } catch (SQLException ex) {
            throw new Exception("Error executing query:", ex);
        }
    }

    /**
     * Top level wrapper for a select statement.
     *
     * @param db The database connection to use.
     * @param index The index to file this statement under for caching.
     * @param sql The sql string to execute.
     * @param fields The data to bind against placeholders. NULL is valid.
     * @return List<Map<String, String>> A list of result rows as key/value pairs in HashMaps
     * @throws Exception if there is an error.
     */
    @Override
    public List<Map<String, String>> select(String db, String index, String sql,
            List<Object> fields) throws Exception {
        // Sanity checks
        if (db == null) {
            throw new Exception("Database cannot be NULL!");
        }
        if (sql == null) {
            throw new Exception("SQL statement cannot be NULL!");
        }

        // Establish a database connection
        Connection database = checkConnection(db);
        if (database == null) {
            throw new Exception("Database '" + db + "' does not exist!");
        }

        // Build our query
        //PreparedStatement statement = prepare(database, index, sql);
        PreparedStatement statement = null;
        //*********************
        try {
            statement = database.prepareStatement(sql);
        } catch (SQLException ex) {
            log.error("Error preparing statement:", ex);
            throw new Exception("Error preparing statement:", ex);
        }
        //*********************
        if (fields != null) {
            for (int i = 1; i <= fields.size(); i++) {
                bindParam(statement, i, fields.get(i-1));
            }
        }

        // Done
        List<Map<String, String>> response = getResults(statement);
        close(statement);
        return response;
        //return getResults(statement);
    }

    /**
     * Top level wrapper for an insert statement.
     *
     * @param db The database connection to use.
     * @param index The index to file this statement under for caching.
     * @param table The name of the table to insert into.
     * @param fields The data to insert, a map of <Column, Data>.
     * @throws Exception if there is an error.
     */
    @Override
    public void insert(String db, String index, String table,
            Map<String, Object> fields) throws Exception {
        // Sanity checks
        if (db == null) {
            throw new Exception("Database cannot be NULL!");
        }
        if (table == null) {
            throw new Exception("Table name cannot be NULL!");
        }
        if (fields == null) {
            throw new Exception("No field data provided!");
        }

        // Establish a database connection
        Connection database = checkConnection(db);
        if (database == null) {
            throw new Exception("Database '" + db + "' does not exist!");
        }

        // Build our query string
        List<String> columns = new ArrayList();
        List<String> placeHolders = new ArrayList();
        List<Object> data = new ArrayList();
        for (String key : fields.keySet()) {
            columns.add(key);
            placeHolders.add("?");
            data.add(fields.get(key));
        }
        String sql = "INSERT INTO " + table + " (" +
                StringUtils.join(columns, ",") + ") VALUES (" +
                StringUtils.join(placeHolders, ",") + ")";

        // Build our query
        PreparedStatement statement = prepare(database, index, sql);
        for (int i = 1; i <= data.size(); i++) {
            bindParam(statement, i, data.get(i-1));
        }

        // Run query
        try {
            statement.executeUpdate();
        } catch (SQLException ex) {
            // These are reasonably expected, let the caller handle them
            if (ex.getMessage().contains("duplicate key value")) {
                throw new Exception("Duplicate record!");

            // Log anything else
            } else {
                log.error("Error during insert:", ex);
                throw new Exception("Error during insert:", ex);
            }
        }
    }

    /**
     * Top level wrapper for a delete statement. Simple equality tests are
     * possible for the where clause.
     *
     * @param db The database connection to use.
     * @param index The index to file this statement under for caching.
     * @param table The name of the table to delete.
     * @param fields The data to use in a where clause. key/value pairs
     * @throws Exception if there is an error.
     */
    @Override
    public void delete(String db, String index, String table,
            Map<String, Object> where) throws Exception {
        // Sanity checks
        if (db == null) {
            throw new Exception("Database cannot be NULL!");
        }
        if (table == null) {
            throw new Exception("Table name cannot be NULL!");
        }

        // Establish a database connection
        Connection database = checkConnection(db);
        if (database == null) {
            throw new Exception("Database '" + db + "' does not exist!");
        }

        // Build our query string
        List<String> columns = new ArrayList();
        List<Object> data = new ArrayList();
        if (where != null) {
            for (String key : where.keySet()) {
                columns.add(key + " = ?");
                data.add(where.get(key));
            }
        }
        String sql;
        if (columns.isEmpty()) {
            sql = "DELETE FROM " + table;
        } else {
            sql = "DELETE FROM " + table + " WHERE " +
                    StringUtils.join(columns, " AND ");
        }

        // Build our query
        PreparedStatement statement = prepare(database, index, sql);
        if (!data.isEmpty()) {
            for (int i = 1; i <= data.size(); i++) {
                bindParam(statement, i, data.get(i-1));
            }
        }

        // Run query
        try {
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new Exception("Error during insert:", ex);
        }
    }

    /**
     * Top level wrapper to execute simple non-returning SQL, such as create
     * or update statements.
     *
     * @param db The database connection to use.
     * @param index The index to file this statement under for caching.
     * @param sql The sql string to execute.
     * @param fields The data to bind against placeholders. NULL is valid.
     * @throws Exception if there is an error.
     */
    @Override
    public void execute(String db, String index, String sql,
            List<Object> fields) throws Exception {
        // Sanity checks
        if (db == null) {
            throw new Exception("Database cannot be NULL!");
        }
        if (sql == null) {
            throw new Exception("SQL statement cannot be NULL!");
        }

        // Establish a database connection
        Connection database = checkConnection(db);
        if (database == null) {
            throw new Exception("Database '" + db + "' does not exist!");
        }

        // Build our query
        PreparedStatement statement = prepare(database, index, sql);
        if (fields != null) {
            for (int i = 1; i <= fields.size(); i++) {
                bindParam(statement, i, fields.get(i-1));
            }
        }

        // Done
        statement.execute();
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
}
