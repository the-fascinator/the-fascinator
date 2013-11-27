/* 
 * The Fascinator - Portal - Security
 * Copyright (C) 2013 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
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
package com.googlecode.fascinator.common.solr;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.common.JsonSimpleConfig;

/**
 * Solr wrapper used in Spring.
 * 
 * @author Andrew Brazzatti
 * @author Jianfeng Li
 * 
 */
@Component(value = "fascinatorIndexer")
public class SpringIndexerWrapper implements Indexer {
    private Indexer indexerPlugin;
    private static final String DEFAULT_STORAGE_TYPE = "solr";
    private Logger log = LoggerFactory.getLogger(SpringIndexerWrapper.class);

    public SpringIndexerWrapper() {
        // initialise storage system
        JsonSimpleConfig systemConfiguration;
        try {
            systemConfiguration = new JsonSimpleConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String indexerType = systemConfiguration.getString(
                DEFAULT_STORAGE_TYPE, "indexer", "type");
        indexerPlugin = PluginManager.getIndexer(indexerType);
        if (indexerPlugin == null) {
            throw new RuntimeException("Indexer plugin '" + indexerType
                    + "' not found. Ensure it is in the classpath.");
        }
        try {
            indexerPlugin.init(systemConfiguration.toString());
            log.debug("Indexer service has been initialiased: {}",
                    indexerPlugin.getName());
        } catch (PluginException pe) {
            throw new RuntimeException("Failed to initialise index", pe);
        }
    }

    @Override
    public String getId() {
        return indexerPlugin.getId();
    }

    @Override
    public String getName() {
        return indexerPlugin.getName();
    }

    @Override
    public PluginDescription getPluginDetails() {
        return indexerPlugin.getPluginDetails();
    }

    @Override
    public void init(File jsonFile) throws PluginException {
        indexerPlugin.init(jsonFile);
    }

    @Override
    public void init(String jsonString) throws PluginException {
        indexerPlugin.init(jsonString);

    }

    @Override
    public void shutdown() throws PluginException {
        indexerPlugin.shutdown();
    }

    @Override
    public void annotate(String arg0, String arg1) throws IndexerException {
        indexerPlugin.annotate(arg0, arg1);
    }

    @Override
    public void annotateRemove(String arg0) throws IndexerException {
        indexerPlugin.annotateRemove(arg0);
    }

    @Override
    public void annotateRemove(String arg0, String arg1)
            throws IndexerException {
        indexerPlugin.annotate(arg0, arg1);

    }

    @Override
    public void annotateSearch(SearchRequest arg0, OutputStream arg1)
            throws IndexerException {
        indexerPlugin.annotateSearch(arg0, arg1);

    }

    @Override
    public void commit() {
        indexerPlugin.commit();

    }

    @Override
    public void index(String arg0) throws IndexerException {
        indexerPlugin.index(arg0);

    }

    @Override
    public void index(String arg0, String arg1) throws IndexerException {
        indexerPlugin.index(arg0, arg1);

    }

    @Override
    public void remove(String arg0) throws IndexerException {
        indexerPlugin.remove(arg0);
    }

    @Override
    public void remove(String arg0, String arg1) throws IndexerException {
        indexerPlugin.remove(arg0, arg1);
    }

    @Override
    public void search(SearchRequest arg0, OutputStream arg1)
            throws IndexerException {
        indexerPlugin.search(arg0, arg1);
    }

    @Override
    public void search(SearchRequest arg0, OutputStream arg1, String arg2)
            throws IndexerException {
        indexerPlugin.search(arg0, arg1, arg2);
    }

    @Override
    public void searchByIndex(SearchRequest arg0, OutputStream arg1, String arg2)
            throws IndexerException {
        indexerPlugin.searchByIndex(arg0, arg1, arg2);
    }

    @Override
    public List<Object> getJsonObjectWithField(String fieldName,
            String fieldValue) throws IndexerException {
        return indexerPlugin.getJsonObjectWithField(fieldName, fieldValue);
    }

}
