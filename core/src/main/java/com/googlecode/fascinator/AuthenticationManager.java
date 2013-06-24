/*
 * The Fascinator - Authentication Manager
 * Copyright (C) 2008-2011 University of Southern Queensland
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
package com.googlecode.fascinator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.access.AccessControlException;
import com.googlecode.fascinator.api.authentication.AuthManager;
import com.googlecode.fascinator.api.authentication.Authentication;
import com.googlecode.fascinator.api.authentication.AuthenticationException;
import com.googlecode.fascinator.api.authentication.User;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.authentication.GenericUser;
import com.googlecode.fascinator.spring.ApplicationContextProvider;

/**
 * Authentication and management of users.
 * 
 * This object manages one or more authentication plugins based on
 * configuration. The portal doesn't need to know the details of talking to each
 * data source.
 * 
 * @author Greg Pendlebury
 */
public class AuthenticationManager implements AuthManager {

    /** The internal plugin is the default if none are specified */
    private static final String INTERNAL_AUTH_PLUGIN = "internal";

    /** Logging */
    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory
            .getLogger(AuthenticationManager.class);

    /** User */
    @SuppressWarnings("unused")
    private GenericUser user_object;

    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    /** List of plugins */
    private Map<String, Authentication> plugins;

    /** Authentication object */
    private Authentication p;

    /** Current Active plugin */
    private String active = null;

    @Override
    public String getId() {
        return "authmanager";
    }

    @Override
    public String getName() {
        return "Authentication Manager";
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
    public void init(String jsonString) throws AuthenticationException {
        try {
            setConfig(new JsonSimpleConfig(jsonString));
        } catch (AuthenticationException e) {
            throw new AuthenticationException(e);
        } catch (IOException e) {
            throw new AuthenticationException(e);
        }
    }

    @Override
    public void init(File jsonFile) throws AuthenticationException {
        try {
            setConfig(new JsonSimpleConfig(jsonFile));
        } catch (AuthenticationException e) {
            throw new AuthenticationException(e);
        } catch (IOException ioe) {
            throw new AuthenticationException(ioe);
        }
    }

    /**
     * Read the config file and retrieve the plugin list
     * 
     * @param config JSON Configuration
     * @throws AccessControlException if plugin fail to initialise
     */
    public void setConfig(JsonSimpleConfig config)
            throws AuthenticationException {
        // Initialise our local properties
        user_object = new GenericUser();
        plugins = new LinkedHashMap<String, Authentication>();
        // Get and parse the config
        String plugin_string = config.getString(INTERNAL_AUTH_PLUGIN,
                "authentication", "type");
        String[] plugin_list = plugin_string.split(",");
        // Now start each required plugin
        for (String element : plugin_list) {
            // Get the plugin from the service loader
            Authentication auth = PluginManager.getAuthentication(element);
            // Pass it our config file
            try {
                auth.init(config.toString());
            } catch (PluginException e) {
                throw new AuthenticationException(e);
            }
            plugins.put(element, auth);
        }
        // Finally, we need to pick the plugin for admins
        // to create/modify users. The first non-internal
        // plugin is the default, but admins can also
        // request one.
        Iterator<Authentication> i = plugins.values().iterator();
        while (i.hasNext() && active == null) {
            p = i.next();
            if (p.getId().equals(INTERNAL_AUTH_PLUGIN)) {
                // Skip internal
            } else {
                active = p.getId();
            }
        }
        // Fall back to internal if there were no other plugins
        if (active == null) {
            active = INTERNAL_AUTH_PLUGIN;
        }

        authenticationManager = (org.springframework.security.authentication.AuthenticationManager) ApplicationContextProvider
                .getApplicationContext().getBean(
                        "fascinatorAuthenticationManager");
    }

    @Override
    public void shutdown() throws AuthenticationException {
        Iterator<Authentication> i = plugins.values().iterator();
        while (i.hasNext()) {
            p = i.next();
            try {
                p.shutdown();
            } catch (PluginException e) {
                throw new AuthenticationException(e);
            }
        }
    }

    /**
     * Tests the user's username/password validity.
     * 
     * @param username The username of the user logging in.
     * @param password The password of the user logging in.
     * @return A user object for the newly logged in user.
     * @throws AuthenticationException if there was an error logging in.
     */
    @Override
    public User logIn(String username, String password)
            throws AuthenticationException {
        Iterator<Authentication> i = plugins.values().iterator();
        while (i.hasNext()) {
            p = i.next();
            try {
                User user = p.logIn(username, password);
                user.setSource(p.getId());

                // Now authenticate user using Spring Securit
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                        username, password);
                token.setDetails(user);

                try {
                    org.springframework.security.core.Authentication auth = authenticationManager
                            .authenticate(token);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (BadCredentialsException e) {
                    // TODO: When we move completely to Spring Security we'll
                    // need to handle this.
                    // Our custom AuthManager will never throw this exception
                    // for the time being
                }
                return user;
            } catch (AuthenticationException e) {
                // Don't need to do anything here
                // just means the user wasn't in
                // this plugin
            }
        }

        // Still no luck, time to error
        throw new AuthenticationException("Invalid username or password.");
    }

    /**
     * Optional logout method if the implementing class wants to do any
     * post-processing.
     * 
     * @param username The username of the logging out user.
     * @throws AuthenticationException if there was an error logging out.
     */
    @Override
    public void logOut(User user) throws AuthenticationException {
        String source = user.getSource();
        plugins.get(source).logOut(user);
        // Log user out of Spring Security
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    /**
     * Method for testing if the implementing plugin allows the creation,
     * deletion and modification of users.
     * 
     * @return <code>true</code> if user management is supported,
     *         <code>false</code> otherwise
     */
    @Override
    public boolean supportsUserManagement() {
        Iterator<Authentication> i = plugins.values().iterator();
        while (i.hasNext()) {
            p = i.next();
            // Return true as soon as we
            // find one plugin
            if (p.supportsUserManagement()) {
                return true;
            }
        }
        // Return false if none
        return false;
    }

    /**
     * Describe the metadata the implementing class needs/allows for a user.
     * 
     * TODO: This is a placeholder of possible later SQUIRE integration.
     * 
     * @return TODO: possibly a JSON string.
     */
    @Override
    public String describeUser() {
        String response = "{";
        Iterator<Authentication> i = plugins.values().iterator();
        while (i.hasNext()) {
            p = i.next();
            response += "\"" + p.getId() + "\" : {";
            response += "\"name\": \"" + p.getName() + "\", ";
            response += "\"metadata\": " + p.describeUser();
            response += "}";
            if (i.hasNext()) {
                response += ", ";
            }
        }
        response += "}";
        return response;
    }

    /**
     * Create a user.
     * 
     * @param username The username of the new user.
     * @param password The password of the new user.
     * @return A user object for the newly created in user.
     * @throws AuthenticationException if there was an error creating the user.
     */
    @Override
    public User createUser(String username, String password)
            throws AuthenticationException {
        try {
            User user = plugins.get(active).createUser(username, password);
            user.setSource(active);
            return user;
        } catch (AuthenticationException e) {
            throw new AuthenticationException(e);
        }
    }

    /**
     * Delete a user.
     * 
     * @param username The username of the user to delete.
     * @throws AuthenticationException if there was an error during deletion.
     */
    @Override
    public void deleteUser(String username) throws AuthenticationException {
        try {
            plugins.get(active).deleteUser(username);
        } catch (AuthenticationException e) {
            throw new AuthenticationException(e);
        }
    }

    /**
     * A simplified method alternative to modifyUser() if the implementing class
     * wants to just allow password changes.
     * 
     * @param username The user changing their password.
     * @param password The new password for the user.
     * @throws AuthenticationException if there was an error changing the
     *             password.
     */
    @Override
    public void changePassword(String username, String password)
            throws AuthenticationException {
        try {
            plugins.get(active).changePassword(username, password);
        } catch (AuthenticationException e) {
            throw new AuthenticationException(e);
        }
    }

    /**
     * Modify one of the user's properties. Available properties should match up
     * with the return value of describeUser().
     * 
     * @param username The user being modified.
     * @param property The user property being modified.
     * @param newValue The new value to be assigned to the property.
     * @return An updated user object for the modifed user.
     * @throws AuthenticationException if there was an error during
     *             modification.
     */
    @Override
    public User modifyUser(String username, String property, String newValue)
            throws AuthenticationException {
        throw new AuthenticationException(
                "This class does not support user modification.");
    }

    @Override
    public User modifyUser(String username, String property, int newValue)
            throws AuthenticationException {
        throw new AuthenticationException(
                "This class does not support user modification.");
    }

    @Override
    public User modifyUser(String username, String property, boolean newValue)
            throws AuthenticationException {
        throw new AuthenticationException(
                "This class does not support user modification.");
    }

    /**
     * Returns a User object if the implementing class supports user queries
     * without authentication.
     * 
     * @param username The username of the user required.
     * @return An user object of the requested user.
     * @throws AuthenticationException if there was an error retrieving the
     *             object.
     */
    @Override
    public User getUser(String username) throws AuthenticationException {
        // Try the active plugin first
        try {
            User user = plugins.get(active).getUser(username);
            user.setSource(active);
            return user;
        } catch (AuthenticationException e) {
            // Do nothing, not found
        }

        // Now try looking everywhere
        Iterator<Authentication> i = plugins.values().iterator();
        while (i.hasNext()) {
            p = i.next();
            try {
                // Retrieve the user
                User user = p.getUser(username);
                // Make sure we set where it came from
                user.setSource(p.getId());
                // and return it
                return user;
            } catch (AuthenticationException e) {
                // Don't need to do anything here
                // just means the user wasn't in
                // this plugin
            }
        }
        // If we are still executing then the user wasn't found
        throw new AuthenticationException("User '" + username + "' not found.");
    }

    /**
     * Returns a list of users matching the search.
     * 
     * @param search The search string to execute.
     * @return A list of usernames (String) that match the search.
     * @throws AuthenticationException if there was an error searching.
     */
    @Override
    public List<User> searchUsers(String search) throws AuthenticationException {
        List<User> found = new ArrayList<User>();
        User user;

        // Try the active plugin first
        if (active != null) {
            try {
                Iterator<?> i = plugins.get(active).searchUsers(search)
                        .iterator();
                // Now loop through those
                while (i.hasNext()) {
                    user = (User) i.next();
                    // Record where they came from
                    user.setSource(active);
                    // And add to the result set
                    found.add(user);
                }
            } catch (AuthenticationException e) {
                // Do nothing, not found
            }

            return found;
        }

        // Loop through each plugin
        Iterator<Authentication> i = plugins.values().iterator();
        while (i.hasNext()) {
            p = i.next();
            try {
                // Get the mathing users in this plugin
                Iterator<?> j = p.searchUsers(search).iterator();
                // Now loop through those
                while (j.hasNext()) {
                    user = (User) j.next();
                    // Record where they came from
                    user.setSource(p.getId());
                    // And add to the result set
                    found.add(user);
                }
            } catch (AuthenticationException e) {
                throw new AuthenticationException(e);
            }
        }

        return found;
    }

    /**
     * Specifies which plugin the authentication manager should use when
     * managing users. This won't effect reading of data, just writing.
     * 
     * @param pluginId The id of the plugin.
     */
    @Override
    public void setActivePlugin(String pluginId) {
        if (pluginId == null) {
            active = null;
            return;
        }

        // Make sure it exists
        Iterator<Authentication> i = plugins.values().iterator();
        while (i.hasNext()) {
            p = i.next();
            if (pluginId.equals(p.getId())) {
                active = pluginId;
            }
        }
    }

    /**
     * Return the current active plugin.
     * 
     * @return The currently active plugin.
     */
    @Override
    public String getActivePlugin() {
        return active;
    }

    /**
     * Return the list of plugins being managed.
     * 
     * @return A list of plugins.
     */
    @Override
    public List<PluginDescription> getPluginList() {
        List<PluginDescription> found = new ArrayList<PluginDescription>();
        PluginDescription result;

        // Loop through each plugin
        Iterator<Authentication> i = plugins.values().iterator();
        while (i.hasNext()) {
            p = i.next();
            result = new PluginDescription(p);
            result.setMetadata(p.describeUser());
            found.add(result);
        }

        return found;
    }
}
