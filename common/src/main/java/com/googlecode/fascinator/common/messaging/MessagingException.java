/* 
 * The Fascinator - Core - Messaging Exception
 * Copyright (C) 2011 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
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
package com.googlecode.fascinator.common.messaging;

/**
 * Generic exception for messaging errors
 * 
 * @author Greg Pendlebury
 */
@SuppressWarnings("serial")
public class MessagingException extends Exception {

    /**
     * Messaging Exception Constructor
     * 
     * @param message Exception message
     */
    public MessagingException(String message) {
        super(message);
    }

    /**
     * Messaging Exception Constructor
     * 
     * @param cause Throwable cause
     */
    public MessagingException(Throwable cause) {
        super(cause);
    }

    /**
     * Messaging Exception Constructor
     * 
     * @param message Exception message
     * @param cause Throwable cause
     */
    public MessagingException(String message, Throwable cause) {
        super(message, cause);
    }

}
