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
package com.googlecode.fascinator.portal.services;

import com.googlecode.fascinator.AccessManager;
import com.googlecode.fascinator.AuthenticationManager;
import com.googlecode.fascinator.RoleManager;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.access.AccessControlManager;
import com.googlecode.fascinator.api.authentication.AuthManager;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.roles.RolesManager;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.portal.JsonSessionState;
import com.googlecode.fascinator.portal.services.impl.ByteRangeRequestCacheImpl;
import com.googlecode.fascinator.portal.services.impl.CachingDynamicPageServiceImpl;
import com.googlecode.fascinator.portal.services.impl.DatabaseServicesImpl;
import com.googlecode.fascinator.portal.services.impl.EhcacheDynamicPageCacheImpl;
import com.googlecode.fascinator.portal.services.impl.HarvestManagerImpl;
import com.googlecode.fascinator.portal.services.impl.HouseKeepingManagerImpl;
import com.googlecode.fascinator.portal.services.impl.PortalManagerImpl;
import com.googlecode.fascinator.portal.services.impl.PortalSecurityManagerImpl;
import com.googlecode.fascinator.portal.services.impl.ScriptingServicesImpl;
import com.googlecode.fascinator.portal.services.impl.VelocityServiceImpl;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.OrderedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.services.RegistryShutdownHub;
import org.apache.tapestry5.services.AliasContribution;
import org.apache.tapestry5.services.ApplicationStateContribution;
import org.apache.tapestry5.services.ApplicationStateCreator;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.URLEncoder;
import org.apache.tapestry5.urlrewriter.RewriteRuleApplicability;
import org.apache.tapestry5.urlrewriter.SimpleRequestWrapper;
import org.apache.tapestry5.urlrewriter.URLRewriteContext;
import org.apache.tapestry5.urlrewriter.URLRewriterRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * <p>
 * A Tapestry Module which controls service instantiation and configuration.
 * </p>
 *
 * <h3>Wiki Link</h3>
 * <p>
 * <b>https://fascinator.usq.edu.au/trac/wiki/Fascinator/Documents/Portal/JavaCore#TapestryServices</b>
 * </p>
 *
 * @author Oliver Lucido
 */
public class PortalModule {

    private static final String DEFAULT_INDEXER_TYPE = "solr";

    private static final String DEFAULT_STORAGE_TYPE = "file-system";

    private static Logger log = LoggerFactory.getLogger(PortalModule.class);

    static {
        MDC.put("name", "main");
    }

    /**
     * Use the ServiceBinder to bind Tapestry Service implementations
     * to their interfaces.
     *
     * @param binder : Tapestry service binder
     */
    public static void bind(ServiceBinder binder) {
        binder.bind(ByteRangeRequestCache.class,
                ByteRangeRequestCacheImpl.class);
        binder.bind(DynamicPageService.class,
                CachingDynamicPageServiceImpl.class);
        binder.bind(HarvestManager.class, HarvestManagerImpl.class);
        binder.bind(PortalManager.class, PortalManagerImpl.class);
        binder.bind(PortalSecurityManager.class,
                PortalSecurityManagerImpl.class);
        binder.bind(ScriptingServices.class, ScriptingServicesImpl.class);
        binder.bind(VelocityService.class, VelocityServiceImpl.class);
    }

    public static DynamicPageCache buildDynamicPageCache(
            PortalManager portalManager, VelocityService velocityService,
            ScriptingServices scriptingServices, RegistryShutdownHub hub) {
        DynamicPageCache cache = new EhcacheDynamicPageCacheImpl(portalManager,
                velocityService, scriptingServices);
        hub.addRegistryShutdownListener(cache);
        return cache;
    }

    /**
     * Instantiate and return the DatabaseService, making sure Tapestry notifies
     * the Service at system shutdown.
     *
     * @param hub : Tapestry shutdown hub
     * @return DatabaseServices : The Database Tapestry Service
     */
    public static DatabaseServices buildDatabaseServices(
            RegistryShutdownHub hub) {
        DatabaseServices database = new DatabaseServicesImpl();
        hub.addRegistryShutdownListener(database);
        return database;
    }

    /**
     * Instantiate and return the House Keeper, making sure Tapestry notifies
     * the Service at system shutdown.
     *
     * @param hub : Tapestry shutdown hub
     * @return HouseKeepingManager : The House Keeping Service
     */
    public static HouseKeepingManager buildHouseKeepingManager(
            RegistryShutdownHub hub) {
        HouseKeepingManager houseKeeping = new HouseKeepingManagerImpl();
        hub.addRegistryShutdownListener(houseKeeping);
        return houseKeeping;
    }

    /**
     * Instantiate and return the Manager object for Access Control plugins.
     *
     * @return AccessControlManager : The Access Control Manager
     */
    public static AccessControlManager buildAccessManager() {
        try {
            AccessManager access = new AccessManager();
            access.init(JsonSimpleConfig.getSystemFile());
            return access;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Instantiate and return the Manager object for Authentication plugins.
     *
     * @return AuthManager : The Authentication Manager
     */
    public static AuthManager buildAuthManager() {
        try {
            AuthenticationManager auth = new AuthenticationManager();
            auth.init(JsonSimpleConfig.getSystemFile());
            return auth;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Instantiate and return an Indexer plugin.
     *
     * @return Indexer : An Indexer plugin
     */
    public static Indexer buildIndexer() {
        try {
            JsonSimpleConfig config = new JsonSimpleConfig();
            Indexer indexer = PluginManager.getIndexer(
                    config.getString(DEFAULT_INDEXER_TYPE, "indexer", "type"));
            indexer.init(JsonSimpleConfig.getSystemFile());
            return indexer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Instantiate and return the Manager object for security Roles plugins.
     *
     * @return RolesManager : The Roles Manager
     */
    public static RolesManager buildRoleManager() {
        try {
            RoleManager roles = new RoleManager();
            roles.init(JsonSimpleConfig.getSystemFile());
            return roles;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Instantiate and return a Storage plugin.
     *
     * @return Storage : A Storage plugin
     */
    public static Storage buildStorage() {
        try {
            JsonSimpleConfig config = new JsonSimpleConfig();
            Storage storage = PluginManager.getStorage(
                    config.getString(DEFAULT_STORAGE_TYPE, "storage", "type"));
            storage.init(JsonSimpleConfig.getSystemFile());
            return storage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Modify the configuration of the ResponseCompressionAnalyzer Tapestry
     * service. The service is responsible for deciding which MIME type
     * should be automatically GZIP'd on the way back to the client.
     *
     * Adding formats to this configuration will exclude them from compression
     *
     * @param configuration: Unordered configuration from Tapestry
     */
    public static void contributeResponseCompressionAnalyzer(
            Configuration<String> configuration) {
        try {
            JsonSimpleConfig config = new JsonSimpleConfig();
            List<String> ignoreList =
                    config.getStringList("portal", "compression", "ignore");
            if (ignoreList != null && !ignoreList.isEmpty()) {
                for (String format : ignoreList) {
                    log.info("Exclude from GZIP '{}'", format);
                    configuration.add(format);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Modify the configuration of the ApplicationStateManager Tapestry
     * service. We want it to use our JsonSessionState object for session data.
     *
     * @param configuration: Configuration from Tapestry
     */
    public static void contributeApplicationStateManager(
            MappedConfiguration<Class<?>, ApplicationStateContribution> configuration) {
        ApplicationStateCreator<JsonSessionState> creator =
                new ApplicationStateCreator<JsonSessionState>() {

                    @Override
                    public JsonSessionState create() {
                        return new JsonSessionState();
                    }
                };
        ApplicationStateContribution contribution =
                new ApplicationStateContribution("session", creator);
        configuration.add(JsonSessionState.class, contribution);
    }

    /**
     * Modify the Tapestry URL encoding/decoding to ensure URLs are left
     * exactly as received before they reach our code.
     *
     * @param configuration: Configuration from Tapestry
     */
    public static void contributeAlias(
            Configuration<AliasContribution<URLEncoder>> configuration) {
        configuration.add(AliasContribution.create(URLEncoder.class,
                new NullURLEncoderImpl()));
    }

    /**
     * Ensure Tapestry routes all URLs to our Dispatch object.
     *
     * The sole except is 'asset*' URLs which Tapestry will handle, although
     * we don't use at this time
     *
     * @param configuration: Configuration from Tapestry
     * @param requestGlobals: Request information
     * @param urlEncoder: The URL encoder
     */
    public static void contributeURLRewriter(
            OrderedConfiguration<URLRewriterRule> configuration,
            @Inject final RequestGlobals requestGlobals,
            @Inject final URLEncoder urlEncoder) {
        URLRewriterRule rule = new URLRewriterRule() {

            @Override
            public Request process(Request request, URLRewriteContext context) {
                // set the original request uri - without context
                HttpServletRequest req = requestGlobals.getHTTPServletRequest();
                String ctxPath = request.getContextPath();
                String uri = req.getRequestURI();
                request.setAttribute("RequestURI",
                        uri.substring(ctxPath.length() + 1));
                // forward all requests to the main dispatcher
                String path = request.getPath();
                String[] parts = path.substring(1).split("/");
                if (parts.length > 0) {
                    String start = parts[0];
                    if (!"assets".equals(start) && !"dispatch".equals(start)) {
                        path = "/dispatch" + path;
                    }
                } else {
                    path = "/dispatch";
                }
                return new SimpleRequestWrapper(request, path);
            }

            @Override
            public RewriteRuleApplicability applicability() {
                return RewriteRuleApplicability.INBOUND;
            }
        };
        configuration.add("dispatch", rule);
    }
}
