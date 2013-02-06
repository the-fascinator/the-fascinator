/* 
 * The Fascinator - Portal
 * Copyright (C) 2008-2009 University of Southern Queensland
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
package com.googlecode.fascinator.portal.services;

import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.storage.Storage;

public interface ScriptingServices {

    public DatabaseServices getDatabase();

    public DynamicPageService getPageService();

    public Indexer getIndexer();

    public Storage getStorage();

    public HarvestManager getHarvestManager();

    public HouseKeepingManager getHouseKeepingManager();

    public PortalManager getPortalManager();

    public ByteRangeRequestCache getByteRangeCache();

    public VelocityService getVelocityService();

    public FascinatorService getService(String serviceName);
}
