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
package au.edu.usq.fascinator.portal.services.impl;

import au.edu.usq.fascinator.common.MessagingServices;
import au.edu.usq.fascinator.api.PluginDescription;
import au.edu.usq.fascinator.api.PluginException;
import au.edu.usq.fascinator.api.PluginManager;
import au.edu.usq.fascinator.api.indexer.Indexer;
import au.edu.usq.fascinator.api.indexer.IndexerException;
import au.edu.usq.fascinator.api.indexer.SearchRequest;
import au.edu.usq.fascinator.common.JsonSimpleConfig;
import au.edu.usq.fascinator.portal.JsonSessionState;
import au.edu.usq.fascinator.portal.services.IndexerService;

import java.io.File;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.jms.JMSException;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexerServiceImpl implements IndexerService {

    private static final String DEFAULT_INDEXER_TYPE = "solr";

    @Inject
    private JsonSessionState sessionState;

    private String username;

    /** Messaging services */
    private MessagingServices messaging;

    /** Logging */
    private Logger log = LoggerFactory.getLogger(IndexerServiceImpl.class);

    private Indexer indexer;

    public IndexerServiceImpl(Indexer indexer, ApplicationStateManager asm) {

        if (sessionState == null) {
            sessionState = asm.get(JsonSessionState.class);
        }
        username = (String) sessionState.get("username");

        try {
            messaging = MessagingServices.getInstance();
        } catch (JMSException jmse) {
            log.error("Failed to start connection: {}", jmse.getMessage());
        }

        if (indexer == null) {
            try {
                JsonSimpleConfig config = new JsonSimpleConfig();
                indexer = PluginManager.getIndexer(config.getString(
                        DEFAULT_INDEXER_TYPE, "indexer", "type"));
                indexer.init(JsonSimpleConfig.getSystemFile());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        this.indexer = indexer;
    }

    @Override
    public void index(String oid) throws IndexerException {
        sentMessage(oid, "modify");
        indexer.index(oid);
    }

    @Override
    public void index(String oid, String pid) throws IndexerException {
        sentMessage(oid, "modify");
        indexer.index(oid, pid);
    }

    @Override
    public void commit() {
        indexer.commit();
    }

    @Override
    public void annotate(String oid, String pid) throws IndexerException {
        sentMessage(oid, "modify-anotar");
        indexer.annotate(oid, pid);
    }

    @Override
    public void annotateSearch(SearchRequest request, OutputStream result)
            throws IndexerException {
        indexer.annotateSearch(request, result);
    }

    @Override
    public void annotateRemove(String oid) throws IndexerException {
        sentMessage(oid, "delete-anotar");
        indexer.annotateRemove(oid);
    }

    @Override
    public void annotateRemove(String oid, String pid) throws IndexerException {
        sentMessage(oid, "delete-anotar");
        indexer.annotateRemove(oid, pid);
    }

    @Override
    public void remove(String oid) throws IndexerException {
        sentMessage(oid, "delete");
        indexer.remove(oid);
    }

    @Override
    public void remove(String oid, String pid) throws IndexerException {
        sentMessage(oid, "delete");
        indexer.remove(oid, pid);
    }

    @Override
    public void search(SearchRequest request, OutputStream result)
            throws IndexerException {
        indexer.search(request, result);
    }

    @Override
    public String getId() {
        return indexer.getId();
    }

    @Override
    public String getName() {
        return indexer.getName();
    }

    /**
     * Gets a PluginDescription object relating to this plugin.
     * 
     * @return a PluginDescription
     */
    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    @Override
    public void init(File jsonFile) throws PluginException {
        indexer.init(jsonFile);
    }

    @Override
    public void shutdown() throws PluginException {
        indexer.shutdown();
    }

    @Override
    public void init(String jsonString) throws PluginException {
        indexer.init(jsonString);
    }

    private void sentMessage(String oid, String eventType) {
        log.info(" * Sending message: {} with event {}", oid, eventType);
        Map<String, String> param = new LinkedHashMap<String, String>();
        param.put("oid", oid);
        param.put("eventType", eventType);
        param.put("username", username);
        param.put("context", indexer.getName());
        messaging.onEvent(param);
    }
}
