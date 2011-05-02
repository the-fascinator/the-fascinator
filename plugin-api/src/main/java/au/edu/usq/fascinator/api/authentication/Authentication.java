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
package au.edu.usq.fascinator.api.authentication;

import au.edu.usq.fascinator.api.Plugin;

import java.util.List;

/**
 * Authentication and management of users.
 * This is the top-level interface defining
 * available methods.
 *
 * @author Greg Pendlebury
 */
public interface Authentication extends Plugin {

    /**
     * Tests the user's username/password validity.
     *
     * @param username The username of the user logging in.
     * @param password The password of the user logging in.
     * @return A user object for the newly logged in user.
     * @throws AuthenticationException if there was an error logging in.
     */
    public User logIn(String username, String password)
            throws AuthenticationException;

    /**
     * Optional logout method if the implementing class wants
     * to do any post-processing.
     *
     * @param username The username of the logging out user.
     * @throws AuthenticationException if there was an error logging out.
     */
    public void logOut(User user) throws AuthenticationException;

    /**
     * Method for testing if the implementing plugin allows
     * the creation, deletion and modification of users.
     *
     * @return true/false reponse.
     */
    public boolean supportsUserManagement();

    /**
     * Describe the metadata the implementing class
     * needs/allows for a user.
     *
     * TODO: This is a placeholder of possible later SQUIRE integration.
     *
     * @return TODO: possibly a JSON string.
     */
    public String describeUser();

    /**
     * Create a user.
     *
     * @param username The username of the new user.
     * @param password The password of the new user.
     * @return A user object for the newly created in user.
     * @throws AuthenticationException if there was an error creating the user.
     */
    public User createUser(String username, String password)
            throws AuthenticationException;

    /**
     * Delete a user.
     *
     * @param username The username of the user to delete.
     * @throws AuthenticationException if there was an error during deletion.
     */
    public void deleteUser(String username) throws AuthenticationException;

    /**
     * A simplified method alternative to modifyUser() if the implementing
     * class wants to just allow password changes.
     *
     * @param username The user changing their password.
     * @param password The new password for the user.
     * @throws AuthenticationException if there was an error changing the password.
     */
    public void changePassword(String username, String password)
            throws AuthenticationException;

    /**
     * Modify one of the user's properties. Available properties should match
     * up with the return value of describeUser().
     *
     * @param username The user being modified.
     * @param property The user property being modified.
     * @param newValue The new value to be assigned to the property.
     * @return An updated user object for the modifed user.
     * @throws AuthenticationException if there was an error during modification.
     */
    public User modifyUser(String username, String property, String newValue)
            throws AuthenticationException;
    public User modifyUser(String username, String property, int newValue)
            throws AuthenticationException;
    public User modifyUser(String username, String property, boolean newValue)
            throws AuthenticationException;

    /**
     * Returns a User object if the implementing class supports
     * user queries without authentication.
     *
     * @param username The username of the user required.
     * @return An user object of the requested user.
     * @throws AuthenticationException if there was an error retrieving the object.
     */
    public User getUser(String username) throws AuthenticationException;

    /**
     * Returns a list of users matching the search.
     *
     * @param search The search string to execute.
     * @return A list of usernames (String) that match the search.
     * @throws AuthenticationException if there was an error searching.
     */
    public List<User> searchUsers(String search) throws AuthenticationException;

}
