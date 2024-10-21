/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.discovery.impl.cluster;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.discovery.commons.providers.spi.base.DummySlingSettingsService;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.DiscoveryServiceImpl;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ClusterViewChangeListenerTest {

    private final OsgiContext context = new OsgiContext();

    private final Config config = new Config();
    private final ResourceResolverFactory resolverFactory = new MockResourceResolverFactory();
    private final SlingSettingsService settingsService = new DummySlingSettingsService("1", "target");
    private final MockDiscoveryServiceImpl discoveryService = new MockDiscoveryServiceImpl();
    private final ClusterViewChangeListener clusterViewChangeListener = new ClusterViewChangeListener();

    @Before
    public void setup() {
        context.registerInjectActivateService(config);
        context.registerService(ResourceResolverFactory.class, resolverFactory);
        context.registerService(SlingSettingsService.class, settingsService);
        context.registerService(DiscoveryServiceImpl.class, discoveryService);
        context.registerInjectActivateService(clusterViewChangeListener);
    }

    @After
    public void tearDown() {
        clusterViewChangeListener.deactivate();
    }

    @Test
    public void resourceChangeListenerRegistered() {
        ResourceChangeListener listener = context.getService(ResourceChangeListener.class);
        assertNotNull(listener);
    }

    @Test
    public void changeUnrelatedResource() {
        change("/unrelated");
        assertEquals(0, discoveryService.getCheckForTopologyChangeCount());
    }

    @Test
    public void changeResourceUnderEstablishedView() {
        change(config.getEstablishedViewPath() + "/foo");
        assertEquals(1, discoveryService.getCheckForTopologyChangeCount());
    }

    @Test
    public void changeResourceUnderClusterInstancePath() {
        change(config.getClusterInstancesPath() + "/foo");
        assertEquals(1, discoveryService.getCheckForTopologyChangeCount());
    }

    @Test
    public void changeResourceUnderClusterInstancePathWithPropertyChange() {
        change(config.getClusterInstancesPath() + "/foo", "some-property");
        assertEquals(1, discoveryService.getCheckForTopologyChangeCount());
    }

    @Test
    public void ignoreHeartbeat() {
        change(config.getClusterInstancesPath() + "/foo", "lastHeartbeat");
        assertEquals(0, discoveryService.getCheckForTopologyChangeCount());
    }

    private void change(String path, String changedProperty) {
        deliver(changed(path, changedProperty));
    }

    private void change(String path) {
        deliver(changed(path));
    }

    private void deliver(ResourceChange change) {
        List<ResourceChange> changes = Collections.singletonList(change);
        Arrays.asList(context.getServices(ResourceChangeListener.class, null))
                .forEach(listener -> listener.onChange(changes));
    }

    public static class MockDiscoveryServiceImpl extends DiscoveryServiceImpl {

        private final AtomicInteger checkForTopologyChangeCounter = new AtomicInteger(0);

        int getCheckForTopologyChangeCount() {
            return checkForTopologyChangeCounter.get();
        }

        @Override
        public void checkForTopologyChange() {
            checkForTopologyChangeCounter.incrementAndGet();
            super.checkForTopologyChange();
        }
    }

    private static ResourceChange changed(String path) {
        return new ResourceChange(ResourceChange.ChangeType.CHANGED, path, false, null, null, null);
    }

    private static ResourceChange changed(String path, String changedProperty) {
        return new ResourceChange(ResourceChange.ChangeType.CHANGED, path, false, null, Collections.singleton(changedProperty), null);
    }
}
