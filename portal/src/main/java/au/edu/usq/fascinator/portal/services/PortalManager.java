/* 
 * The Fascinator - Portal
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
package au.edu.usq.fascinator.portal.services;

import java.io.File;
import java.util.Map;
import java.util.Set;

import au.edu.usq.fascinator.portal.Portal;
import java.util.List;

public interface PortalManager {

    public static final String DEFAULT_PORTAL_NAME = "default";

    public static final String DEFAULT_SKIN = "default";

    public static final String DEFAULT_DISPLAY = "default";

    public static final String DEFAULT_PORTAL_HOME = "portal";

    public static final String DEFAULT_PORTAL_HOME_DEV = "src/main/config/portal";

    public Map<String, Portal> getPortals();

    public Portal getDefault();

    public File getHomeDir();

    public Portal get(String name);

    public boolean exists(String name);

    public void add(Portal portal);

    public void remove(String name);

    public void save(Portal portal);

    public void reharvest(String objectId);

    public void reharvest(Set<String> objectIds);

    public String getDefaultPortal();

    public String getDefaultDisplay();

    public List<String> getSkinPriority();
}
