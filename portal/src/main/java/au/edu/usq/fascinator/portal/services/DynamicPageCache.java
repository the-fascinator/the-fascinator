/*
 * The Fascinator - Portal
 * Copyright (C) 2011 University of Southern Queensland
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
package au.edu.usq.fascinator.portal.services;

import org.apache.tapestry5.ioc.services.RegistryShutdownListener;
import org.python.core.PyObject;

/**
 * Caching support for DynamicPageServices.
 *
 * @author Oliver Lucido
 */
public interface DynamicPageCache extends RegistryShutdownListener {

    /**
     * Gets the script object with the specified path.
     *
     * @param path script path
     * @return a script object or null if an error occurred
     */
    public PyObject getScriptObject(String path);

    /**
     * Gets the fully resolved path for the specified path.
     *
     * @param pathId a path to resolve
     * @return resolved path
     */
    public String getPath(String pathId);

    /**
     * Puts an entry into the path lookup cache.
     *
     * @param pathId path to resolve
     * @param path resolved path
     */
    public void putPath(String pathId, String path);
}
