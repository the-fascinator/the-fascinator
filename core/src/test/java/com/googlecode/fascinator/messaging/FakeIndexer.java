/* 
 * The Fascinator - Fake Indexer
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
package com.googlecode.fascinator.messaging;

import java.io.File;
import java.io.OutputStream;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.IndexerException;
import com.googlecode.fascinator.api.indexer.SearchRequest;

/**
 * A fake Indexer plugin for unit testing against. This isn't even an
 * implementation in RAM, it just fakes the interface and reports activity to
 * STDOUT. It's usage outside of unit tests is pointless, and even in unit tests
 * you cannot trust what it is *doing*, just that it is being called.
 * 
 * @author Greg Pendlebury
 */

public class FakeIndexer implements Indexer {
    public void log(String message) {
        log(null, message);
    }

    public void log(String title, String message) {
        String titleLog = "";
        if (title != null) {
            titleLog += "===\n" + title + "\n===\n";
        }
        System.err.println(titleLog + message);
    }

    @Override
    public String getId() {
        return "fake";
    }

    @Override
    public String getName() {
        return "Fake Indexer";
    }

    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    @Override
    public void init(String jsonString) throws IndexerException {
        log(" * INDEXER: init(String)");
    }

    @Override
    public void init(File jsonFile) throws IndexerException {
        log(" * INDEXER: init(File)");
    }

    @Override
    public void shutdown() throws PluginException {
        log(" * INDEXER: shutdown()");
    }

    @Override
    public void search(SearchRequest request, OutputStream response)
            throws IndexerException {
        log(" * INDEXER: search()");
    }

    @Override
    public void remove(String oid) throws IndexerException {
        log(" * INDEXER: remove(oid)");
    }

    @Override
    public void remove(String oid, String pid) throws IndexerException {
        log(" * INDEXER: remove(oid, pid)");
    }

    @Override
    public void annotateRemove(String oid) throws IndexerException {
        log(" * INDEXER: annotateRemove(oid)");
    }

    @Override
    public void annotateRemove(String oid, String annoId)
            throws IndexerException {
        log(" * INDEXER: annotateRemove(oid, pid)");
    }

    @Override
    public void index(String oid) throws IndexerException {
        log(" * INDEXER: index(oid)");
    }

    @Override
    public void index(String oid, String pid) throws IndexerException {
        log(" * INDEXER: index(oid, pid)");
    }

    @Override
    public void commit() {
        log(" * INDEXER: commit()");
    }

    @Override
    public void annotate(String oid, String pid) throws IndexerException {
        log(" * INDEXER: annotate()");
    }

    @Override
    public void annotateSearch(SearchRequest request, OutputStream response)
            throws IndexerException {
        log(" * INDEXER: annotateSearch()");
    }

    @Override
    public void searchByIndex(SearchRequest request, OutputStream response,
            String indexName) throws IndexerException {
        log(" * INDEXER: searchByIndex()");

    }
}
