/*
 * The Fascinator - Internal Roles plugin
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
package com.googlecode.fascinator.roles.internal;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.roles.Roles;
import com.googlecode.fascinator.api.roles.RolesException;
import com.googlecode.fascinator.common.JsonSimpleConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This plugin implements the Fascinator default internal roles. 
 * </p>
 * 
 * <h3>Configuration</h3> 
 * <p>Standard configuration table:</p>
 * <table border="1">
 * <tr>
 * <th>Option</th>
 * <th>Description</th>
 * <th>Required</th>
 * <th>Default</th>
 * </tr>
 * 
 * <tr>
 * <td>internal/path</td>
 * <td>File path in wich the roles information is stored</td>
 * <td><b>Yes</b></td>
 * <td>${user.home}/.fascinator/roles.properties</td>
 * </tr>
 * 
 * </table>
 * 
 * <h3>Examples</h3>
 * <ol>
 * <li>
 * Using Internal role plugin in The Fascinator
 * 
 * <pre>
 *      "roles": {
 *          "type": "internal",
 *          "internal": {
 *              "path": "${user.home}/.fascinator/roles.properties"
 *      }
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

public class InternalRoles implements Roles {

    private static String DEFAULT_FILE_NAME = "roles.properties";
    private final Logger log = LoggerFactory.getLogger(InternalRoles.class);
    private String file_path;
    private Properties file_store;
    private Map<String, List<String>> user_list;
    private Map<String, List<String>> role_list;

    @Override
    public String getId() {
        return "internal";
    }

    @Override
    public String getName() {
        return "Internal Roles";
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

    @Override
    public void init(String jsonString) throws RolesException {
        try {
            setConfig(new JsonSimpleConfig(jsonString));
        } catch (IOException e) {
            throw new RolesException(e);
        }
    }

    @Override
    public void init(File jsonFile) throws RolesException {
        try {
            setConfig(new JsonSimpleConfig(jsonFile));
        } catch (IOException ioe) {
            throw new RolesException(ioe);
        }
    }

    public void setConfig(JsonSimpleConfig config) throws IOException {
        // Get the basics
        file_path   = config.getString(null, "roles", "internal", "path");
        loadRoles();
    }

    private void loadRoles() throws IOException {
        file_store  = new Properties();

        // Load our userbase from disk
        try {
            // Make sure it exists first
            File user_file = new File(file_path);
            if (!user_file.exists()) {
                user_file.getParentFile().mkdirs();
                OutputStream out = new FileOutputStream(user_file);
                IOUtils.copy(getClass().getResourceAsStream("/" + DEFAULT_FILE_NAME), out);
                out.close();
            }

            // Read the file and ready output variables
            file_store.load(new FileInputStream(file_path));
            user_list = new LinkedHashMap<String, List<String>>();
            role_list = new LinkedHashMap<String, List<String>>();
            List<String> my_roles = new ArrayList();
            List<String> users_with_role = new ArrayList();

            // Loop through all users
            String[] users = file_store.keySet().toArray(new String[file_store.size()]);
            for (String user : users) {
                my_roles = new ArrayList();

                // Loop through each role the user has
                String[] roles = file_store.getProperty(user).split(",");
                for (String role : roles) {
                    users_with_role = new ArrayList();

                    // Record it for this user
                    my_roles.add(role);

                    // And look add this user to the roles list
                    if (role_list.containsKey(role))
                        users_with_role = role_list.get(role);
                    users_with_role.add(user);
                    role_list.put(role, users_with_role);
                }

                // Store this user
                user_list.put(user, my_roles);
            }

        } catch (Exception e) {
            throw new IOException (e);
        }
    }

    private void saveRoles() throws IOException {
        if (file_store != null) {
            try {
                file_store.store(new FileOutputStream(file_path), "");
            } catch (Exception e) {
                throw new IOException (e);
            }
        }
    }

    @Override
    public void shutdown() throws RolesException {
        // No action required
    }

    /**
     * Find and return all roles this user has.
     *
     * @param username The username of the user.
     * @return An array of role names (String).
     */
    @Override
    public String[] getRoles(String username) {
        if (user_list.containsKey(username)) {
            return user_list.get(username).toArray(new String[0]);
        } else {
            return new String[0];
        }
    }

    /**
     * Returns a list of users who have a particular role.
     *
     * @param role The role to search for.
     * @return An array of usernames (String) that have that role.
     */
    @Override
    public String[] getUsersInRole(String role) {
        if (role_list.containsKey(role)) {
            return role_list.get(role).toArray(new String[0]);
        } else {
            return new String[0];
        }
    }

    /**
     * Method for testing if the implementing plugin allows
     * the creation, deletion and modification of roles.
     *
     * @return true/false reponse.
     */
    @Override
    public boolean supportsRoleManagement() {
        return true;
    }

    /**
     * Assign a role to a user.
     *
     * @param username The username of the user.
     * @param newrole The new role to assign the user.
     * @throws RolesException if there was an error during assignment.
     */
    @Override
    public void setRole(String username, String newrole)
            throws RolesException {
        List<String> users_with_role = new ArrayList();

        if (user_list.containsKey(username)) {
            List<String> roles_of_user = user_list.get(username);
            if (!roles_of_user.contains(newrole)) {
                // Update our user list
                roles_of_user.add(newrole);
                user_list.put(username, roles_of_user);
                // Update our roles list
                if (role_list.containsKey(newrole))
                    users_with_role = role_list.get(newrole);
                users_with_role.add(username);
                role_list.put(newrole, users_with_role);

                // Don't forget to update our file_store
                String roles = StringUtils.join(roles_of_user.toArray(new String[0]), ",");
                file_store.setProperty(username, roles);

                // And commit the file_store to disk
                try {
                    saveRoles();
                } catch (IOException e) {
                    throw new RolesException(e);
                }
            } else {
                throw new RolesException("User '" + username + "' already has role '" + newrole + "'!");
            }
        } else {
            // Add the user
            List<String> empty = new ArrayList();
            user_list.put(username, empty);
            // Try again
            this.setRole(username, newrole);
        }
    }

    /**
     * Remove a role from a user.
     *
     * @param username The username of the user.
     * @param oldrole The role to remove from the user.
     * @throws RolesException if there was an error during removal.
     */
    @Override
    public void removeRole(String username, String oldrole)
            throws RolesException {
        List<String> users_with_role = new ArrayList();

        if (user_list.containsKey(username)) {
            List<String> roles_of_user = user_list.get(username);
            if (roles_of_user.contains(oldrole)) {
                // Update our user list
                roles_of_user.remove(oldrole);
                user_list.put(username, roles_of_user);
                // Update our roles list
                if (role_list.containsKey(oldrole))
                    users_with_role = role_list.get(oldrole);
                users_with_role.remove(username);
                if (users_with_role.size() < 1) {
                    role_list.remove(oldrole);
                } else {
                    role_list.put(oldrole, users_with_role);
                }

                // Don't forget to update our file_store
                String roles = StringUtils.join(roles_of_user.toArray(new String[0]), ",");
                file_store.setProperty(username, roles);

                // And commit the file_store to disk
                try {
                    saveRoles();
                } catch (IOException e) {
                    throw new RolesException(e);
                }
            } else {
                throw new RolesException("User '" + username + "' does not have the role '" + oldrole + "'!");
            }
        } else {
            throw new RolesException("User '" + username + "' does not exist!");
        }
    }

    /**
     * Create a role.
     *
     * @param rolename The name of the new role.
     * @throws RolesException if there was an error creating the role.
     */
    @Override
    public void createRole(String rolename)
            throws RolesException {
        throw new RolesException(
                "Role creation is not support by this plugin as a " +
                "stand-alone function. Call setRole() with a new " +
                "role and it will be created automatically.");
    }

    /**
     * Delete a role.
     *
     * @param rolename The name of the role to delete.
     * @throws RolesException if there was an error during deletion.
     */
    @Override
    public void deleteRole(String rolename) throws RolesException {
        if (role_list.containsKey(rolename)) {
            List<String> users_with_role = role_list.get(rolename);
            for (String user : users_with_role) {
                // Remove the role from this user
                List<String> roles_of_user = user_list.get(user);
                roles_of_user.remove(rolename);
                user_list.put(user, roles_of_user);

                // Update our file_store
                String roles = StringUtils.join(roles_of_user.toArray(new String[0]), ",");
                file_store.setProperty(user, roles);
            }
            // Remove the role entry
            role_list.remove(rolename);

            // And commit the file_store to disk
            try {
                saveRoles();
            } catch (IOException e) {
                throw new RolesException(e);
            }
        } else {
            throw new RolesException("Cannot find role '" + rolename + "'!");
        }
    }

    /**
     * Rename a role.
     *
     * @param oldrole The name role currently has.
     * @param newrole The name role is changing to.
     * @throws RolesException if there was an error during rename.
     */
    @Override
    public void renameRole(String oldrole, String newrole)
            throws RolesException {
        if (role_list.containsKey(oldrole)) {
            List<String> users_with_role = role_list.get(oldrole);
            for (String user : users_with_role) {
                // Remove the role from this user
                List<String> roles_of_user = user_list.get(user);
                roles_of_user.remove(oldrole);
                roles_of_user.add(newrole);
                user_list.put(user, roles_of_user);

                // Update our file_store
                String roles = StringUtils.join(roles_of_user.toArray(new String[0]), ",");
                file_store.setProperty(user, roles);
            }
            // Replace the role entry
            role_list.remove(oldrole);
            role_list.put(newrole, users_with_role);

            // And commit the file_store to disk
            try {
                saveRoles();
            } catch (IOException e) {
                throw new RolesException(e);
            }
        } else {
            throw new RolesException("Cannot find role '" + oldrole + "'!");
        }
    }

    /**
     * Returns a list of roles matching the search.
     *
     * @param search The search string to execute.
     * @return An array of role names that match the search.
     * @throws RolesException if there was an error searching.
     */
    @Override
    public String[] searchRoles(String search) throws RolesException {
         // Complete list of users
        String[] roles = role_list.keySet().toArray(new String[role_list.size()]);
        List<String> found = new ArrayList();

        // Look through the list for anyone who matches
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].toLowerCase().contains(search.toLowerCase())) {
                found.add(roles[i]);
            }
        }

        // Return the list
        return found.toArray(new String[found.size()]);
   }
}
