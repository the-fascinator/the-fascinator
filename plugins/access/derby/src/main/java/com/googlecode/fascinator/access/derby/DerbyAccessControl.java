/*
 * The Fascinator - Derby Access Control plugin
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
package com.googlecode.fascinator.access.derby;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.access.AccessControl;
import com.googlecode.fascinator.api.access.AccessControlException;
import com.googlecode.fascinator.api.access.AccessControlSchema;
import com.googlecode.fascinator.api.authentication.AuthenticationException;
import com.googlecode.fascinator.common.JsonSimple;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This plugin is a Fascinator access control plugin using Derby database
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
 * <td>derbyHome</td>
 * <td>Path in which derby database will be created and used</td>
 * <td><b>Yes</b></td>
 * <td>${fascinator.home}/database</td>
 * </tr>
 * 
 * </table>
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * Using Derby access control as the plugin in the Fascinator
 * 
 * <pre>
 *      "accesscontrol": {
 *             "type" : "derby",
 *             "derby" : {
 *                 "derbyHome" : "${fascinator.home}/database"
 *             }
 *         }
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * <h3>Wiki Link</h3>
 * <p>
 * None
 * </p>
 * 
 * @author Greg Pendlebury
 */
public class DerbyAccessControl implements AccessControl {
	/** Logging */
	private final Logger log = LoggerFactory
			.getLogger(DerbyAccessControl.class);

	/** JDBC Driver */
	private static String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

	/** Connection string prefix */
	private static String DERBY_PROTOCOL = "jdbc:derby:";

	/** Security database name */
	private static String SECURITY_DATABASE = "tfsecurity";

	/** Records table */
	private static String RECORD_TABLE = "records";

	/** Roles table */
	private static String ROLE_TABLE = "roles";

	/** Users table */
	private static String USER_TABLE = "users";

	/** Database home directory */
	private String derbyHome;

	/** Database connection */
	private Connection connection;

	/**
	 * Gets an identifier for this type of plugin. This should be a simple name
	 * such as "file-system" for a storage plugin, for example.
	 * 
	 * @return the plugin type id
	 */
	@Override
	public String getId() {
		return "derby";
	}

	/**
	 * Gets a name for this plugin. This should be a descriptive name.
	 * 
	 * @return the plugin name
	 */
	@Override
	public String getName() {
		return "Derby Access Control";
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
	 * Initializes the plugin using the specified JSON String
	 * 
	 * @param jsonString
	 *            JSON configuration string
	 * @throws PluginException
	 *             if there was an error in initialization
	 */
	@Override
	public void init(String jsonString) throws AccessControlException {
		try {
			setConfig(new JsonSimple(jsonString));
		} catch (IOException e) {
			throw new AccessControlException(e);
		}
	}

	/**
	 * Initializes the plugin using the specified JSON configuration
	 * 
	 * @param jsonFile
	 *            JSON configuration file
	 * @throws AccessControlException
	 *             if there was an error in initialization
	 */
	@Override
	public void init(File jsonFile) throws AccessControlException {
		try {
			setConfig(new JsonSimple(jsonFile));
		} catch (IOException ioe) {
			throw new AccessControlException(ioe);
		}
	}

	/**
	 * Initialization of Solr Access Control plugin
	 * 
	 * @param config
	 *            The configuration to use
	 * @throws AuthenticationException
	 *             if fails to initialize
	 */
	private void setConfig(JsonSimple config) throws AccessControlException {
		// Find data directory
		derbyHome = config.getString(null, "database-service", "derbyHome");
		String oldHome = System.getProperty("derby.system.home");

		// Derby's data directory has already been configured
		if (oldHome != null) {
			if (derbyHome != null) {
				// Use the existing one, but throw a warning
				log.warn("Using previously specified data directory:"
						+ " '{}', provided value has been ignored: '{}'",
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
						throw new AccessControlException("Database home '"
								+ derbyHome + "' is not a directory!");
					}
				} else {
					file.mkdirs();
					if (!file.exists()) {
						throw new AccessControlException("Database home '"
								+ derbyHome
								+ "' does not exist and could not be created!");
					}
				}
				System.setProperty("derby.system.home", derbyHome);
			}
		}

		// Database prep work
		try {
			checkTable(RECORD_TABLE);
			checkTable(ROLE_TABLE);
			checkTable(USER_TABLE);
		} catch (SQLException ex) {
			log.error("Error during database preparation:", ex);
			throw new AccessControlException(
					"Error during database preparation:", ex);
		}
		log.debug("Derby security database online!");
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
			connection = DriverManager.getConnection(DERBY_PROTOCOL
					+ SECURITY_DATABASE + ";create=true", props);
		}
		return connection;
	}

	/**
	 * Shuts down the plugin
	 * 
	 * @throws AccessControlException
	 *             if there was an error during shutdown
	 */
	@Override
	public void shutdown() throws AccessControlException {
		// Derby can only be shutdown from one thread,
		// we'll catch errors from the rest.
		String threadedShutdownMessage = DERBY_DRIVER
				+ " is not registered with the JDBC driver manager";
		try {
			// Tell the database to close
			DriverManager.getConnection(DERBY_PROTOCOL + ";shutdown=true");
			// Shutdown just this database (but not the engine)
			// DriverManager.getConnection(DERBY_PROTOCOL + SECURITY_DATABASE +
			// ";shutdown=true");
		} catch (SQLException ex) {
			// These test values are used if the engine is NOT shutdown
			// if (ex.getErrorCode() == 45000 &&
			// ex.getSQLState().equals("08006")) {

			// Valid response
			if (ex.getErrorCode() == 50000 && ex.getSQLState().equals("XJ015")) {
				// Error response
			} else {
				// Make sure we ignore simple thread issues
				if (!ex.getMessage().equals(threadedShutdownMessage)) {
					log.error("Error during database shutdown:", ex);
					throw new AccessControlException(
							"Error during database shutdown:", ex);
				}
			}
		} finally {
			try {
				// Close our connection
				if (connection != null) {
					connection.close();
					connection = null;
				}
			} catch (SQLException ex) {
				log.error("Error closing connection:", ex);
			}
		}
	}

	/**
	 * Return an empty security schema for the portal to investigate and/or
	 * populate.
	 * 
	 * @return An empty security schema
	 */
	@Override
	public AccessControlSchema getEmptySchema() {
		return new DerbySchema();
	}

	/**
	 * Get a list of schemas that have been applied to a record.
	 * 
	 * @param recordId
	 *            The record to retrieve information about.
	 * @return A list of access control schemas, possibly zero length.
	 * @throws AccessControlException
	 *             if there was an error during retrieval.
	 */
	@Override
	public List<AccessControlSchema> getSchemas(String recordId)
			throws AccessControlException {
		try {
			List<String> roles = searchRoles(recordId);
			if (roles == null || roles.isEmpty()) {
				return new ArrayList<AccessControlSchema>();
			}

			List<AccessControlSchema> schemas = new ArrayList<AccessControlSchema>();
			DerbySchema schema;
			for (String role : roles) {
				schema = new DerbySchema();
				schema.init(recordId);
				schema.set("role", role);
				schemas.add(schema);
			}

			List<String> users = searchUsers(recordId);
			if (users == null || users.isEmpty()) {
				return new ArrayList<AccessControlSchema>();
			}

			for (String user : users) {
				schema = new DerbySchema();
				schema.init(recordId);
				schema.set("user", user);
				schemas.add(schema);
			}

			return schemas;
		} catch (Exception ex) {
			log.error("Error searching security database: ", ex);
			throw new AccessControlException(
					"Error searching security database");
		}
	}

	/**
	 * Apply/store a new security implementation. The schema will already have a
	 * recordId as a property.
	 * 
	 * @param newSecurity
	 *            The new schema to apply.
	 * @throws AccessControlException
	 *             if storage of the schema fails.
	 */
	@Override
	public void applySchema(AccessControlSchema newSecurity)
			throws AccessControlException {
		// Find the record
		String recordId = newSecurity.getRecordId();
		if (recordId == null || recordId.equals("")) {
			throw new AccessControlException("No record provided by schema.");
		}

		// Find the new role
		String role = newSecurity.get("role");
		if (role != null && !role.equals("")) {
			processRoleSchema(recordId, role);
			return;
		}

		String user = newSecurity.get("user");
		if (user != null && !user.equals("")) {
			processUserSchema(recordId, user);
			return;
		}

		log.error("Should have returned from applySchema:", newSecurity);
		throw new AccessControlException(
				"No security role or user provided by schema.");

	}

	private void processUserSchema(String recordId, String user)
			throws AccessControlException {
		List<String> user_list;
		try {
			user_list = searchUsers(recordId);
		} catch (Exception ex) {
			log.error("Error searching security database: ", ex);
			throw new AccessControlException(
					"Error searching security database");
		}

		// Check current data
		if (user_list != null && user_list.contains(user)) {
			throw new AccessControlException("Duplicate! That user has "
					+ "already been applied to this record.");
		}

		// Add the new relationship to the database
		try {
			if (user_list == null) {
				newRecord(recordId);
			}
			grantUserAccess(recordId, user);
		} catch (Exception ex) {
			log.error("Error updating security database: ", ex);
			throw new AccessControlException("Error updating security database");
		}

	}

	private void processRoleSchema(String recordId, String role)
			throws AccessControlException {
		List<String> role_list;
		try {
			role_list = searchRoles(recordId);
		} catch (Exception ex) {
			log.error("Error searching security database: ", ex);
			throw new AccessControlException(
					"Error searching security database");
		}

		// Check current data
		if (role_list != null && role_list.contains(role)) {
			throw new AccessControlException("Duplicate! That role has "
					+ "already been applied to this record.");
		}

		// Add the new relationship to the database
		try {
			if (role_list == null) {
				newRecord(recordId);
			}
			grantRoleAccess(recordId, role);
		} catch (Exception ex) {
			log.error("Error updating security database: ", ex);
			throw new AccessControlException("Error updating security database");
		}
	}

	/**
	 * Remove a security implementation. The schema will already have a recordId
	 * as a property.
	 * 
	 * @param oldSecurity
	 *            The schema to remove.
	 * @throws AccessControlException
	 *             if removal of the schema fails.
	 */
	@Override
	public void removeSchema(AccessControlSchema oldSecurity)
			throws AccessControlException {
		// Find the record
		String recordId = oldSecurity.getRecordId();
		if (StringUtils.isBlank(recordId)) {
			throw new AccessControlException("No record provided by schema.");
		}

		String role = oldSecurity.get("role");
		if (!StringUtils.isBlank(role)) {
			removeRole(recordId, role);
			return;
		}

		String user = oldSecurity.get("user");
		
		if (!StringUtils.isBlank(user)) {
			removeUser(recordId, user);
			return;
		}

		throw new AccessControlException("No security role/user provided by schema.");

	}

	private void removeUser(String recordId, String user)
			throws AccessControlException {

		// Retrieve current data
		List<String> user_list;
		try {
			user_list = searchUsers(recordId);
		} catch (Exception ex) {
			log.error("Error searching security database: ", ex);
			throw new AccessControlException(
					"Error searching security database");
		}

		// Check current data
		if (user_list == null || !user_list.contains(user)) {
			throw new AccessControlException(
					"That user does not have access to this record.");
		}

		// Remove from security database
		try {
			revokeRoleAccess(recordId, user);
		} catch (Exception ex) {
			log.error("Error updating security database: ", ex);
			throw new AccessControlException("Error updating security database");
		}
	}

	private void removeRole(String recordId, String role)
			throws AccessControlException {
	// Retrieve current data
		List<String> role_list;
		try {
			role_list = searchRoles(recordId);
		} catch (Exception ex) {
			log.error("Error searching security database: ", ex);
			throw new AccessControlException(
					"Error searching security database");
		}

		// Check current data
		if (role_list == null || !role_list.contains(role)) {
			throw new AccessControlException(
					"That role does not have access to this record.");
		}

		// Remove from security database
		try {
			revokeRoleAccess(recordId, role);
		} catch (Exception ex) {
			log.error("Error updating security database: ", ex);
			throw new AccessControlException("Error updating security database");
		}
	}

	@Override
	public List<String> getUsers(String recordId) throws AccessControlException {
		try {
			return searchUsers(recordId);
		} catch (SQLException ex) {
			log.error("Error searching security database: ", ex);
			throw new AccessControlException(
					"Error searching security database");
		}
	}

	/**
	 * A basic wrapper for getSchemas() to return just the roles of the schemas.
	 * Useful during index and/or audit when this is the only data required.
	 * 
	 * @param recordId
	 *            The record to retrieve roles for.
	 * @return A list of Strings containing role names.
	 * @throws AccessControlException
	 *             if there was an error during retrieval.
	 */
	@Override
	public List<String> getRoles(String recordId) throws AccessControlException {
		try {
			return searchRoles(recordId);
		} catch (SQLException ex) {
			log.error("Error searching security database: ", ex);
			throw new AccessControlException(
					"Error searching security database");
		}
	}

	/**
	 * Retrieve a list of possible field values for a given field if the plugin
	 * supports this feature.
	 * 
	 * @param field
	 *            The field name.
	 * @return A list of String containing possible values
	 * @throws AccessControlException
	 *             if the field doesn't exist or there was an error during
	 *             retrieval
	 */
	@Override
	public List<String> getPossibilities(String field)
			throws AccessControlException {
		throw new AccessControlException(
				"Not supported by this plugin. Use any freetext role name.");
	}

	/**
	 * Check for the existence of a table and arrange for its creation if not
	 * found.
	 * 
	 * @param table
	 *            The table to look for and create.
	 * @throws SQLException
	 *             if there was an error.
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
				throw new SQLException("Could not find or create table '"
						+ table + "'");
			}
		}
	}

	/**
	 * Check if the given table exists in the database.
	 * 
	 * @param table
	 *            The table to look for
	 * @return boolean flag if the table was found or not
	 * @throws SQLException
	 *             if there was an error accessing the database
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
	 * @param table
	 *            The table to create
	 * @throws SQLException
	 *             if there was an error during creation, or an unknown table
	 *             was specified.
	 */
	private void createTable(String table) throws SQLException {
		if (table.equals(RECORD_TABLE)) {
			Statement sql = connection().createStatement();
			sql.execute("CREATE TABLE " + RECORD_TABLE
					+ "(recordId VARCHAR(255) NOT NULL, "
					+ "PRIMARY KEY (recordId))");
			close(sql);
			return;
		}
		if (table.equals(ROLE_TABLE)) {
			Statement sql = connection().createStatement();
			sql.execute("CREATE TABLE " + ROLE_TABLE
					+ "(recordId VARCHAR(255) NOT NULL, "
					+ "role VARCHAR(255) NOT NULL, "
					+ "PRIMARY KEY (recordId, role))");
			close(sql);
			return;
		}
		if (table.equals(USER_TABLE)) {
			Statement sql = connection().createStatement();
			sql.execute("CREATE TABLE " + USER_TABLE
					+ "(recordId VARCHAR(255) NOT NULL, "
					+ "username VARCHAR(255) NOT NULL, "
					+ "PRIMARY KEY (recordId, username))");
			close(sql);
			return;
		}

		throw new SQLException("Unknown table '" + table + "' requested!");
	}

	/**
	 * Revoke access to a record for a given role.
	 * 
	 * @param recordId
	 *            The record to revoke access from.
	 * @param role
	 *            The role whose access is being revoked.
	 * @throws SQLException
	 *             if there were database errors making the change
	 */
	private void revokeRoleAccess(String recordId, String role)
			throws SQLException {
		PreparedStatement sql = connection().prepareStatement(
				"DELETE FROM " + ROLE_TABLE
						+ " WHERE recordId = ? AND role = ?");

		// Prepare and execute
		sql.setString(1, recordId);
		sql.setString(2, role);
		sql.executeUpdate();
		close(sql);
	}

	/**
	 * Revoke access to a record for a given user.
	 * 
	 * @param recordId
	 *            The record to revoke access from.
	 * @param user
	 *            The user whose access is being revoked.
	 * @throws SQLException
	 *             if there were database errors making the change
	 */
	private void revokeUserAccess(String recordId, String user)
			throws SQLException {
		PreparedStatement sql = connection().prepareStatement(
				"DELETE FROM " + USER_TABLE
						+ " WHERE recordId = ? AND username = ?");

		// Prepare and execute
		sql.setString(1, recordId);
		sql.setString(2, user);
		sql.executeUpdate();
		close(sql);
	}

	/**
	 * Grant access to a record for a given role.
	 * 
	 * @param recordId
	 *            The record to grant access to.
	 * @param role
	 *            The role who has been granted access.
	 * @throws SQLException
	 *             if there were database errors making the change
	 */
	private void grantRoleAccess(String recordId, String role)
			throws SQLException {
		PreparedStatement sql = connection().prepareStatement(
				"INSERT INTO " + ROLE_TABLE + " VALUES (?, ?)");

		// Prepare and execute
		sql.setString(1, recordId);
		sql.setString(2, role);
		sql.executeUpdate();
		close(sql);
	}

	/**
	 * Grant access to a record for a given user.
	 * 
	 * @param recordId
	 *            The record to grant access to.
	 * @param user
	 *            The user who has been granted access.
	 * @throws SQLException
	 *             if there were database errors making the change
	 */
	private void grantUserAccess(String recordId, String user)
			throws SQLException {
		PreparedStatement sql = connection().prepareStatement(
				"INSERT INTO " + USER_TABLE + " VALUES (?, ?)");

		// Prepare and execute
		sql.setString(1, recordId);
		sql.setString(2, user);
		sql.executeUpdate();
		close(sql);
	}

	/**
	 * Add a new record to the record table.
	 * 
	 * @param recordId
	 *            The new record
	 * @throws SQLException
	 *             if there were database errors making the change
	 */
	private void newRecord(String recordId) throws SQLException {
		PreparedStatement sql = connection().prepareStatement(
				"INSERT INTO " + RECORD_TABLE + " VALUES (?)");

		// Prepare and execute
		sql.setString(1, recordId);
		sql.executeUpdate();
		close(sql);
	}

	/**
	 * Search for a record and return the roles that have access to it
	 * 
	 * @param recordId
	 *            The record ID to search for
	 * @return List<String> of roles with access, will be NULL if the record is
	 *         not on file
	 * @throws SQLException
	 *             if there were database errors during the search
	 */
	private List<String> searchRoles(String recordId) throws SQLException {
		List<String> roles = new ArrayList<String>();

		PreparedStatement sql = connection().prepareStatement(
				"SELECT * FROM " + ROLE_TABLE + " WHERE recordId = ?");

		// Prepare and execute
		sql.setString(1, recordId);
		ResultSet result = sql.executeQuery();

		// Build response
		while (result.next()) {
			roles.add(result.getString("role"));
		}
		close(result);
		close(sql);

		// Do we even have this record on file?
		if (roles.isEmpty()) {
			if (checkRecord(recordId)) {
				return new ArrayList<String>();
			} else {
				return null;
			}
		}
		return roles;
	}

	private List<String> searchUsers(String recordId) throws SQLException {
		List<String> roles = new ArrayList<String>();

		PreparedStatement sql = connection().prepareStatement(
				"SELECT * FROM " + USER_TABLE + " WHERE recordId = ?");

		// Prepare and execute
		sql.setString(1, recordId);
		ResultSet result = sql.executeQuery();

		// Build response
		while (result.next()) {
			roles.add(result.getString("username"));
		}
		close(result);
		close(sql);

		// Do we even have this record on file?
		if (roles.isEmpty()) {
			if (checkRecord(recordId)) {
				return new ArrayList<String>();
			} else {
				return null;
			}
		}
		return roles;
	}

	/**
	 * Check if the given record has an entry in the record table.
	 * 
	 * @param field
	 *            The field name.
	 * @return boolean flag for if the record exists
	 * @throws SQLException
	 *             if there were database errors during the search
	 */
	private boolean checkRecord(String recordId) throws SQLException {
		PreparedStatement sql = connection().prepareStatement(
				"SELECT count(*) as total FROM " + RECORD_TABLE
						+ " WHERE recordId = ?");

		// Prepare and execute
		sql.setString(1, recordId);
		ResultSet result = sql.executeQuery();

		// Build response
		boolean response = false;
		if (result.next()) {
			if (result.getInt("total") == 1) {
				response = true;
			}
		}
		close(result);
		close(sql);

		return response;
	}

	/**
	 * Attempt to close a ResultSet. Basic wrapper for exception catching and
	 * logging
	 * 
	 * @param resultSet
	 *            The ResultSet to try and close.
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
	 * Attempt to close a Statement. Basic wrapper for exception catching and
	 * logging
	 * 
	 * @param statement
	 *            The Statement to try and close.
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
