/*
 * The Fascinator - Portal - Database Services
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
package au.edu.usq.fascinator.portal.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

import org.apache.tapestry5.ioc.services.RegistryShutdownListener;

/**
 * Instantiates a database used for persistence of generic data in the
 * scripting layer, and offers utility functions for same.
 *
 * Can be used on multiple levels.
 *  - Access the database directly and handle all executions yourself.
 *  - Use minimal wrapping methods to handle errors trapping and minor details.
 *  - Use top level wrappers to hide all details.
 *
 * @author Greg Pendlebury
 */
public interface DatabaseServices extends RegistryShutdownListener {

    /**
     * Return a connection to the specified database, failing if it does not
     * exist.
     *
     * @param database The name of the database to connect to.
     * @return Connection The instantiated database connection or NULL.
     * @throws Exception if there is a connection error.
     */
    public Connection checkConnection(String database) throws Exception;

    /**
     * Return a connection to the specified database. The database will be
     * created if it does not exist.
     *
     * @param database The name of the database to connect to.
     * @return Connection The instantiated database connection, NULL if an error occurs.
     * @throws Exception if there is a connection error.
     */
    public Connection getConnection(String database) throws Exception;

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
    public PreparedStatement prepare(Connection db, String index, String sql)
            throws Exception;

    /**
     * Bind a parameter to a SQL statement. All Java types should be acceptable,
     * except NULL. Use 'IS NULL' in your SQL for this.
     *
     * @param sql The prepared statement to bind to.
     * @param index Specifies which placeholder to bind to (starts at 1).
     * @param data The data to bind to that placeholder.
     * @throws Exception if there is an error.
     */
    public void bindParam(PreparedStatement sql, int index, Object data)
            throws Exception;

    /**
     * Free the resources for a prepared statement. For very commonly occurring
     * statements this is not necessarily advised since DatabaseServices tracks
     * all statements and will free them at server shutdown. The performance
     * gains are useful from this approach IF you use the same query routinely.
     *
     * @param sql The prepared statement to release.
     * @throws Exception if there is an error.
     */
    public void free(PreparedStatement sql) throws Exception;

    /**
     * Parse the results of the query into a basic Java data structure. Users
     * wanting the original result set should call getResultSet() directly
     * against the prepared statement.
     *
     * @param sql The prepared statement to get the results from.
     * @return List<Map<String, String>> A list of result rows as key/value pairs in HashMaps
     * @throws Exception if there is an error.
     */
    public List<Map<String, String>> getResults(PreparedStatement sql)
            throws Exception;

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
    public List<Map<String, String>> select(String db, String index, String sql,
            List<Object> fields) throws Exception;

    /**
     * Top level wrapper for an insert statement.
     *
     * @param db The database connection to use.
     * @param index The index to file this statement under for caching.
     * @param table The name of the table to insert into.
     * @param fields The data to insert, a map of <Column, Data>.
     * @throws Exception if there is an error.
     */
    public void insert(String db, String index, String table,
            Map<String, Object> fields) throws Exception;

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
    public void delete(String db, String index, String table,
            Map<String, Object> where) throws Exception;

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
    public void execute(String db, String index, String sql,
            List<Object> fields) throws Exception;
}
