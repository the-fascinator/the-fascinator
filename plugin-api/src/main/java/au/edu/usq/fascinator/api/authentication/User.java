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

/**
 * A User object that authentication plugins will pass around.
 *
 * Should encapsulate the metadata schema of an user for
 * any implementing plugin.
 *
 * @author Greg Pendlebury
 */
public interface User {

    /**
     * Will return a JSON string description of an extending classes'
     * fields.
     *
     * @param property The class field to retrieve
     * @return The value of the property
     */
    public String describeMetadata();

    /**
     * Retrieves a given property for this user object.
     *
     * @param property The class field to retrieve
     * @return The value of the property
     */
    public String get(String property);

    /**
     * Sets a given property for this user object.
     *
     * @param property The class field to set
     * @param value The value to place in the field
     */
    public void set(String property, String value);

    /**
     * Retrieves how the user should be shown on-screen.
     *
     * @return The value of the property
     */
    public String realName();

    /**
     * Used by the authentication manager to track the user's origin
     *
     * @param plugin The id of the authentication plugin
     */
    public void setSource(String plugin);

    /**
     * Used by the authentication manager to track the user's origin
     *
     * @return The id of the authentication plugin
     */
    public String getSource();
}
