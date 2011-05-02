/*
 * The Fascinator - Portal - User action
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

package au.edu.usq.fascinator.portal;

import java.util.Date;

/**
 * A simple class for describing actions that require
 * attention from the user interface.
 *
 * @author Greg Pendlebury
 */
public class UserAction {
    /** Action ID from database */
    public int id;

    /** Flag to set this action as 'blocking'. Used to ensure
     *  actions like system restarts halt user activity */
    public boolean block;

    /** Message to display */
    public String message;

    /** Message Timestamp */
    public Date date;

    // The getters below are a convenience for Velocity

    /**
     * Get the message ID
     *
     * @returns int The ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the message block property
     *
     * @returns boolean Flag if the message is blocking
     */
    public boolean getBlock() {
        return block;
    }

    /**
     * Get the message
     *
     * @returns String The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the formatted timestamp of message
     *
     * @returns String The message
     */
    public String getDate() {
        return date.toString();
    }
}
