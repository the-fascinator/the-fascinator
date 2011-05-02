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

import au.edu.usq.fascinator.api.PluginException;

/**
 * Generic exception for roles plugin
 * 
 * @author Greg Pendlebury
 */
@SuppressWarnings("serial")
public class RolesException extends PluginException {

    /**
     * Roles Exception Constructor
     * 
     * @param message Error message
     */
    public RolesException(String message) {
        super(message);
    }

    /**
     * Roles Exception Constructor
     * 
     * @param cause Throwable cause
     */
    public RolesException(Throwable cause) {
        super(cause);
    }

    /**
     * Roles Exception Constructor
     * 
     * @param message Error message
     * @param cause Throwable cause
     */
    public RolesException(String message, Throwable cause) {
        super(message, cause);
    }

}
