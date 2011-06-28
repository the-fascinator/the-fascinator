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
package com.googlecode.fascinator.portal.services.impl;

import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.portal.services.ByteRangeRequestCache;
import com.googlecode.fascinator.portal.services.DatabaseServices;
import com.googlecode.fascinator.portal.services.DynamicPageService;
import com.googlecode.fascinator.portal.services.HarvestManager;
import com.googlecode.fascinator.portal.services.HouseKeepingManager;
import com.googlecode.fascinator.portal.services.PortalManager;
import com.googlecode.fascinator.portal.services.ScriptingServices;
import com.googlecode.fascinator.portal.services.VelocityService;

import org.apache.tapestry5.ioc.annotations.Inject;

public class ScriptingServicesImpl implements ScriptingServices {

    @Inject
    private DatabaseServices database;

    @Inject
    private DynamicPageService pageService;

    @Inject
    private Indexer indexerService;

    @Inject
    private Storage storageService;

    @Inject
    private HarvestManager harvestManager;

    @Inject
    private HouseKeepingManager houseKeeping;

    @Inject
    private PortalManager portalManager;

    @Inject
    private ByteRangeRequestCache byteRangeCache;

    @Inject
    private VelocityService velocityService;

    @Override
    public DatabaseServices getDatabase() {
        return database;
    }

    @Override
    public DynamicPageService getPageService() {
        return pageService;
    }

    @Override
    public Indexer getIndexer() {
        return indexerService;
    }

    @Override
    public Storage getStorage() {
        return storageService;
    }

    @Override
    public HarvestManager getHarvestManager() {
        return harvestManager;
    }

    @Override
    public HouseKeepingManager getHouseKeepingManager() {
        return houseKeeping;
    }

    @Override
    public PortalManager getPortalManager() {
        return portalManager;
    }

    @Override
    public ByteRangeRequestCache getByteRangeCache() {
        return byteRangeCache;
    }

    @Override
    public VelocityService getVelocityService() {
        return velocityService;
    }
}
