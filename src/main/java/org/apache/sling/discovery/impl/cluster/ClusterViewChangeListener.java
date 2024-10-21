/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.discovery.impl.cluster;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.DiscoveryServiceImpl;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource change listener which takes note when the established view changes in the
 * repository - or when an announcement changed in one of the instances
 */
@Component(immediate = true)
public class ClusterViewChangeListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private SlingSettingsService slingSettingsService;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private DiscoveryServiceImpl discoveryService;
    
    @Reference
    private Config config;

    /** the sling id of the local instance **/
    private String slingId;

    private ComponentContext context;

    private ServiceRegistration<?> eventHandlerRegistration;

    @Activate
    protected void activate(final ComponentContext context) {
        this.slingId = slingSettingsService.getSlingId();
        this.context = context;
    	if (logger.isDebugEnabled()) {
	        logger.debug("activated. slingid=" + slingId + ", discoveryservice="
	                + discoveryService);
    	}
    	registerEventHandler();
    }
    
    private void registerEventHandler() {
        BundleContext bundleContext = context == null ? null : context.getBundleContext();
        if (bundleContext == null) {
            logger.info("registerEventHandler: context or bundleContext is null - cannot register");
            return;
        }
        Dictionary<String,Object> properties = new Hashtable<String,Object>();
        properties.put(Constants.SERVICE_DESCRIPTION, "Cluster View Change Listener");
        String path = config.getDiscoveryResourcePath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length()-1);
        }
        properties.put(ResourceChangeListener.PATHS, new String[] {path});
        eventHandlerRegistration = bundleContext.registerService(
                ResourceChangeListener.class.getName(), new Listener(), properties);
        logger.info("registerEventHandler: ClusterViewChangeListener registered as ResourceChangeListener");
    }

    @Deactivate
    protected void deactivate() {
        if (eventHandlerRegistration != null) {
            eventHandlerRegistration.unregister();
            logger.info("deactivate: ClusterViewChangeListener unregistered as ResourceChangeListener");
            eventHandlerRegistration = null;
        }
        logger.info("deactivate: deactivated slingId: {}, this: {}", slingId, this);
    }

    private final class Listener implements ResourceChangeListener, ExternalResourceChangeListener {

        /**
         * Handle resource change notifications and take note when
         * the established view, properties or announcements change - and
         * inform the DiscoveryServiceImpl in those cases.
         */
        @Override
        public void onChange(List<ResourceChange> changes) {
            changes.forEach(this::onChange);
        }

        private void onChange(ResourceChange change) {
            if (config==null) {
                return;
            }
            final String establishedViewPath = config.getEstablishedViewPath();
            final String clusterInstancesPath = config.getClusterInstancesPath();
            final String resourcePath = change.getPath();

            if (resourcePath.startsWith(establishedViewPath)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("onChange: establishedViewPath resourcePath={}, change={}",
                            resourcePath, change);
                }
                handleTopologyChanged();
            } else if (resourcePath.startsWith(clusterInstancesPath)) {
                final Set<String> changedProperties = change.getChangedPropertyNames();
                if (changedProperties != null
                        && changedProperties.size() == 1
                        && changedProperties.contains("lastHeartbeat")) {
                    // then ignore this one
                    return;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("onChange: clusterInstancesPath (announcement or properties) resourcePath={}, change={}",
                            resourcePath, change);
                }
                handleTopologyChanged();
            }
        }

        /** Inform the DiscoveryServiceImpl that the topology (might) have changed **/
        private void handleTopologyChanged() {
            logger.debug("handleTopologyChanged: detected a change in the established views, invoking checkForTopologyChange.");
            discoveryService.checkForTopologyChange();
        }
    }
}