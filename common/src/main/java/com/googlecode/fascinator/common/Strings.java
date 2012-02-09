/*
 * The Fascinator - Common Strings
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

package com.googlecode.fascinator.common;

/**
 * <p>
 * This file contains a number of commonly occurring String Literals that are
 * often used in the system across a number of Classes. Including them here
 * avoid duplication and the potential for typographical errors in usage.
 * </p>
 * 
 * <p>
 * Classes with commonly occurring Strings, local to that class, should consider
 * extending this class as a private inner class rather then clutter this list
 * with very niche references. For example:
 * </p>
 * 
 * <pre>
 * private static class Strings extends com.googlecode.fascinator.common.Strings {
 *     // Default email values
 *     public static String DEFAULT_EMAIL_SUBJECT = &quot;An Error has occurred&quot;;
 *     public static String DEFAULT_EMAIL_TEMPLATE = &quot;ERROR: [[MESSAGE]]\n\n====\n\n[[ERROR]]&quot;;
 * }
 * </pre>
 * 
 * @author Greg Pendlebury
 */
public class Strings {
    /** Packaging */
    public static String PACKAGE_FILE_EXTENSION = ".tfpackage";

    /** Workflows */
    public static String WORKFLOW_PAYLOAD_ID = "workflow.metadata";
}