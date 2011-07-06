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
package com.googlecode.fascinator.api.authentication;

import com.googlecode.fascinator.api.PluginDescription;
import java.util.List;

/**
 * A simple extension of Authentication defining
 * some methods that general plugins won't need
 * to concern themselves with.
 *
 * @author Greg Pendlebury
 */
public interface AuthManager extends Authentication {

    /**
     * Specifies which plugin the authentication manager should use
     * when managing users. This won't effect reading of data, just
     * writing.
     *
     * @param pluginId The id of the plugin.
     */
    public void setActivePlugin(String pluginId);

    /**
     * Return the current active plugin.
     *
     * @return The currently active plugin.
     */
    public String getActivePlugin();

    /**
     * Return the list of plugins being managed.
     *
     * @return A list of plugins.
     */
    public List<PluginDescription> getPluginList();
}
