/*
 * The Fascinator - Plugin API
 * Copyright (C) 2008-2010 University of Southern Queensland
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
package au.edu.usq.fascinator.api.roles;

import au.edu.usq.fascinator.api.Plugin;

/**
 * Query external data source for user roles.
 * This is the top-level interface defining
 * available methods.
 *
 * @author Greg Pendlebury
 */
public interface Roles extends Plugin {

    /**
     * Find and return all roles this user has.
     *
     * @param username The username of the user.
     * @return An array of role names (String).
     */
    public String[] getRoles(String username);

    /**
     * Returns a list of users who have a particular role.
     *
     * @param role The role to search for.
     * @return An array of usernames (String) that have that role.
     */
    public String[] getUsersInRole(String role);

    /**
     * Method for testing if the implementing plugin allows
     * the creation, deletion and modification of roles.
     *
     * @return true/false reponse.
     */
    public boolean supportsRoleManagement();

    /**
     * Assign a role to a user.
     *
     * @param username The username of the user.
     * @param newrole The new role to assign the user.
     * @throws RolesException if there was an error during assignment.
     */
    public void setRole(String username, String newrole) throws RolesException;

    /**
     * Remove a role to a user.
     *
     * @param username The username of the user.
     * @param oldrole The role to remove from the user.
     * @throws RolesException if there was an error during removal.
     */
    public void removeRole(String username, String oldrole)
            throws RolesException;

    /**
     * Create a role.
     *
     * @param rolename The name of the new role.
     * @throws RolesException if there was an error creating the role.
     */
    public void createRole(String rolename)
            throws RolesException;

    /**
     * Delete a role.
     *
     * @param rolename The name of the role to delete.
     * @throws RolesException if there was an error during deletion.
     */
    public void deleteRole(String rolename) throws RolesException;

    /**
     * Rename a role.
     *
     * @param oldrole The name role currently has.
     * @param newrole The name role is changing to.
     * @throws RolesException if there was an error during rename.
     */
    public void renameRole(String oldrole, String newrole) throws RolesException;

    /**
     * Returns a list of roles matching the search.
     *
     * @param search The search string to execute.
     * @return An array of role names that match the search.
     * @throws RolesException if there was an error searching.
     */
    public String[] searchRoles(String search) throws RolesException;

}
