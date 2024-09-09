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
package org.apache.sling.discovery.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.discovery.base.connectors.BaseConfig;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteConfig;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.discovery.impl.DiscoveryServiceConfig.DEFAULT_BACKOFF_STABLE_FACTOR;
import static org.apache.sling.discovery.impl.DiscoveryServiceConfig.DEFAULT_BACKOFF_STANDBY_FACTOR;
import static org.apache.sling.discovery.impl.DiscoveryServiceConfig.DEFAULT_CONNECTION_TIMEOUT;
import static org.apache.sling.discovery.impl.DiscoveryServiceConfig.DEFAULT_DISCOVERY_RESOURCE_PATH;
import static org.apache.sling.discovery.impl.DiscoveryServiceConfig.DEFAULT_HEARTBEAT_INTERVAL;
import static org.apache.sling.discovery.impl.DiscoveryServiceConfig.DEFAULT_HEARTBEAT_TIMEOUT;
import static org.apache.sling.discovery.impl.DiscoveryServiceConfig.DEFAULT_MIN_EVENT_DELAY;
import static org.apache.sling.discovery.impl.DiscoveryServiceConfig.DEFAULT_SO_TIMEOUT;
import static org.apache.sling.discovery.impl.DiscoveryServiceConfig.DEFAULT_TOPOLOGY_CONNECTOR_WHITELIST;

/**
 * Configuration object used as a central config point for the discovery service
 * implementation
 * <p>
 * The properties are described below under.
 */
@Component(immediate = true, service = { Config.class, BaseConfig.class, DiscoveryLiteConfig.class })
@Designate(ocd = DiscoveryServiceConfig.class)
public class Config implements BaseConfig, DiscoveryLiteConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** resource used to keep instance information such as last heartbeat, properties, incoming announcements **/
    private static final String CLUSTERINSTANCES_RESOURCE = "clusterInstances";

    /** resource used to store the sync tokens as part of a topology change **/
    private static final String SYNC_TOKEN_RESOURCE = "syncTokens";

    /** resource used to store the clusterNodeIds to slingIds map **/
    private static final String ID_MAP_RESOURCE = "idMap";

    /** resource used to keep the currently established view **/
    private static final String ESTABLISHED_VIEW_RESOURCE = "establishedView";

    /** resource used to keep the previously established view **/
    private static final String PREVIOUS_VIEW_RESOURCE = "previousView";

    /** resource used to keep ongoing votings **/
    private static final String ONGOING_VOTING_RESOURCE = "ongoingVotings";

    protected long heartbeatTimeout = DEFAULT_HEARTBEAT_TIMEOUT;
    protected long heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
    protected int minEventDelay = DEFAULT_MIN_EVENT_DELAY;
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private int soTimeout = DEFAULT_SO_TIMEOUT;
    private URL[] topologyConnectorUrls = {null};
    private String[] topologyConnectorWhitelist = DEFAULT_TOPOLOGY_CONNECTOR_WHITELIST;
    private String discoveryResourcePath = DEFAULT_DISCOVERY_RESOURCE_PATH;
    private String leaderElectionRepositoryDescriptor ;
    private boolean invertRepositoryDescriptor = false; /* default: false */
    
    /** True when auto-stop of a local-loop is enabled. Default is false. **/
    private boolean autoStopLocalLoopEnabled;
    
    /**
     * True when the hmac is enabled and signing is disabled.
     */
    private boolean hmacEnabled;

    /**
     * the shared key.
     */
    private String sharedKey;

    /**
     * The key interval.
     */
    private long keyInterval;

    /**
     * true when encryption is enabled.
     */
    private boolean encryptionEnabled;
    
    /**
     * true when topology connector requests should be gzipped
     */
    private boolean gzipConnectorRequestsEnabled;

    /** the backoff factor to be used for standby (loop) connectors **/
    private int backoffStandbyFactor = DEFAULT_BACKOFF_STANDBY_FACTOR;
    
    /** the maximum backoff factor to be used for stable connectors **/
    private int backoffStableFactor = DEFAULT_BACKOFF_STABLE_FACTOR;
    
    /**
     * when set to true and the syncTokenService (of discovery.commons) is available,
     * then it is used.
     */
    private boolean useSyncTokenService = true;

    /**
     * true when discovery.impl is enabled
     */
    private boolean enabled = true;

    @Activate
    protected void activate(final DiscoveryServiceConfig config,
                            final ComponentContext context) {
		logger.debug("activate: config activated.");
        configure(config, context);
    }

    protected void configure(final DiscoveryServiceConfig config,
                             final ComponentContext context) {
        this.enabled = config.enabled();
        logger.debug("configure: enabled='{}'", this.enabled);
        if (this.enabled) {
            // enable all components in this bundle
            context.enableComponent(null);
        } else {
            logger.info("configure: discovery.impl service is disabled.");
        }

        this.heartbeatTimeout = config.heartbeatTimeout();
        logger.debug("configure: heartbeatTimeout='{}'", this.heartbeatTimeout);

        this.heartbeatInterval = config.heartbeatInterval();
        logger.debug("configure: heartbeatInterval='{}'",
                this.heartbeatInterval);

        this.minEventDelay = config.minEventDelay();
        logger.debug("configure: minEventDelay='{}'",
                this.minEventDelay);
        
        this.connectionTimeout = config.connectionTimeout();
        logger.debug("configure: connectionTimeout='{}'",
                this.connectionTimeout);
        
        this.soTimeout = config.soTimeout();
        logger.debug("configure: soTimeout='{}'",
                this.soTimeout);
        
        
        String[] topologyConnectorUrlsStr = PropertiesUtil.toStringArray(
                config.topologyConnectorUrls(), null);
        if (topologyConnectorUrlsStr!=null && topologyConnectorUrlsStr.length > 0) {
            List<URL> urls = new LinkedList<>();
            for (int i = 0; i < topologyConnectorUrlsStr.length; i++) {
                String anUrlStr = topologyConnectorUrlsStr[i];
                try {
                	if (anUrlStr!=null && !anUrlStr.isEmpty()) {
	                    URL url = new URL(anUrlStr);
	                    logger.debug("configure: a topologyConnectorbUrl='{}'",
	                            url);
	                    urls.add(url);
                	}
                } catch (MalformedURLException e) {
                    logger.error("configure: could not set a topologyConnectorUrl: " + e,
                            e);
                }
            }
            if (!urls.isEmpty()) {
                this.topologyConnectorUrls = urls.toArray(new URL[urls.size()]);
                logger.debug("configure: number of topologyConnectorUrls='{}''",
                        urls.size());
            } else {
                this.topologyConnectorUrls = null;
                logger.debug("configure: no (valid) topologyConnectorUrls configured");
            }
        } else {
            this.topologyConnectorUrls = null;
            logger.debug("configure: no (valid) topologyConnectorUrls configured");
        }
        this.topologyConnectorWhitelist = PropertiesUtil.toStringArray(
                config.topologyConnectorWhitelist(),
                DEFAULT_TOPOLOGY_CONNECTOR_WHITELIST);
        logger.debug("configure: topologyConnectorWhitelist='{}'",
                this.topologyConnectorWhitelist);

        this.discoveryResourcePath = PropertiesUtil.toString(
                config.discoveryResourcePath(),
                "");
        while(this.discoveryResourcePath.endsWith("/")) {
            this.discoveryResourcePath = this.discoveryResourcePath.substring(0,
                    this.discoveryResourcePath.length()-1);
        }
        this.discoveryResourcePath = this.discoveryResourcePath + "/";
        if (this.discoveryResourcePath.length()<=1) {
            // if the path is empty, or /, then use the default
            this.discoveryResourcePath = DEFAULT_DISCOVERY_RESOURCE_PATH;
        }
        logger.debug("configure: discoveryResourcePath='{}'",
                this.discoveryResourcePath);

        this.leaderElectionRepositoryDescriptor = config.leaderElectionRepositoryDescriptor();
        logger.debug("configure: leaderElectionRepositoryDescriptor='{}'",
                this.leaderElectionRepositoryDescriptor);
        
        this.invertRepositoryDescriptor = config.invertRepositoryDescriptor();
        logger.debug("configure: invertRepositoryDescriptor='{}'",
                this.invertRepositoryDescriptor);

        autoStopLocalLoopEnabled = config.autoStopLocalLoopEnabled();
        gzipConnectorRequestsEnabled = config.gzipConnectorRequestsEnabled();
        
        hmacEnabled = config.hmacEnabled();
        encryptionEnabled = config.enableEncryption();
        sharedKey = config.sharedKey();
        keyInterval = config.hmacSharedKeyTTL();
        
        backoffStandbyFactor = config.backoffStandbyFactor();
        backoffStableFactor = config.backoffStableFactor();
        
        useSyncTokenService = config.useSyncTokenServiceEnabled();
    }

    /**
     * Returns the timeout (in seconds) after which an instance or voting is considered invalid/timed out
     * @return the timeout (in seconds) after which an instance or voting is considered invalid/timed out
     */
    public long getHeartbeatTimeout() {
        return heartbeatTimeout;
    }
    
    /**
     * Returns the timeout (in milliseconds) after which an instance or voting is considered invalid/timed out
     * @return the timeout (in milliseconds) after which an instance or voting is considered invalid/timed out
     */
    public long getHeartbeatTimeoutMillis() {
        return getHeartbeatTimeout() * 1000;
    }
    
    /**
     * Returns the socket connect() timeout used by the topology connector, 0 disables the timeout
     * @return the socket connect() timeout used by the topology connector, 0 disables the timeout
     */
    public int getSocketConnectTimeout() {
        return connectionTimeout;
    }

    /**
     * Returns the socket read timeout (SO_TIMEOUT) used by the topology connector, 0 disables the timeout
     * @return the socket read timeout (SO_TIMEOUT) used by the topology connector, 0 disables the timeout
     */
    public int getSoTimeout() {
        return soTimeout;
    }
    
    /**
     * Returns the interval (in seconds) in which heartbeats are sent
     * @return the interval (in seconds) in which heartbeats are sent
     */
    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    /**
     * Returns the minimum time (in seconds) between sending TOPOLOGY_CHANGING/_CHANGED events - to avoid flooding
     * @return the minimum time (in seconds) between sending TOPOLOGY_CHANGING/_CHANGED events - to avoid flooding
     */
    public int getMinEventDelay() {
        return minEventDelay;
    }

    /**
     * Returns the URLs to which to open a topology connector - or null/empty if no topology connector
     * is configured (default is null)
     * @return the URLs to which to open a topology connector - or null/empty if no topology connector
     * is configured
     */
    public URL[] getTopologyConnectorURLs() {
        return topologyConnectorUrls;
    }

    /**
     * Returns a comma separated list of hostnames and/or ip addresses which are allowed as
     * remote hosts to open connections to the topology connector servlet
     * @return a comma separated list of hostnames and/or ip addresses which are allowed as
     * remote hosts to open connections to the topology connector servlet
     */
    public String[] getTopologyConnectorWhitelist() {
        return topologyConnectorWhitelist;
    }

    public String getDiscoveryResourcePath() {
        return discoveryResourcePath;
    }
    
    /**
     * Returns the resource path where cluster instance informations are stored.
     * @return the resource path where cluster instance informations are stored
     */
    public String getClusterInstancesPath() {
        return getDiscoveryResourcePath() + CLUSTERINSTANCES_RESOURCE;
    }

    /**
     * Returns the resource path where the established view is stored.
     * @return the resource path where the established view is stored
     */
    public String getEstablishedViewPath() {
        return getDiscoveryResourcePath() + ESTABLISHED_VIEW_RESOURCE;
    }

    /**
     * Returns the resource path where ongoing votings are stored.
     * @return the resource path where ongoing votings are stored
     */
    public String getOngoingVotingsPath() {
        return getDiscoveryResourcePath() + ONGOING_VOTING_RESOURCE;
    }

    /**
     * Returns the resource path where the previous view is stored.
     * @return the resource path where the previous view is stored
     */
    public String getPreviousViewPath() {
        return getDiscoveryResourcePath() + PREVIOUS_VIEW_RESOURCE;
    }

    /**
     * Returns the repository descriptor key which is to be included in the
     * cluster leader election - or null.
     * <p>
     * When set, the value (treated as a boolean) of the repository descriptor
     * is prepended to the leader election id.
     * @return the repository descriptor key which is to be included in the
     * cluster leader election - or null
     */
    public String getLeaderElectionRepositoryDescriptor() {
        return leaderElectionRepositoryDescriptor;
    }
    
    /**
     * Returns true when the value of the repository descriptor identified
     * via the property 'leaderElectionRepositoryDescriptor' should be 
     * inverted - only applies when 'leaderElectionRepositoryDescriptor' 
     * is configured of course.
     * @return true when property resulting from 'leaderElectionRepositoryDescriptor'
     * should be inverted, false if it should remain unchanged.
     */
    public boolean shouldInvertRepositoryDescriptor() {
        return invertRepositoryDescriptor;
    }

    /**
     * @return true if hmac is enabled.
     */
    public boolean isHmacEnabled() {
        return hmacEnabled;
    }

    /**
     * @return the shared key
     */
    public String getSharedKey() {
        return sharedKey;
    }

    /**
     * @return the interval of the shared key for hmac.
     */
    public long getKeyInterval() {
        return keyInterval;
    }

    /**
     * @return true if encryption is enabled.
     */
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }
    
    /**
     * @return true if requests on the topology connector should be gzipped
     * (which only works if the server accepts that.. ie discovery.impl 1.0.4+)
     */
    public boolean isGzipConnectorRequestsEnabled() {
        return gzipConnectorRequestsEnabled;
    }
    
    /**
     * @return true if the auto-stopping of local-loop topology connectors is enabled.
     */
    public boolean isAutoStopLocalLoopEnabled() {
        return autoStopLocalLoopEnabled;
    }

    /**
     * Returns the backoff factor to be used for standby (loop) connectors
     * @return the backoff factor to be used for standby (loop) connectors
     */
    public int getBackoffStandbyFactor() {
        return backoffStandbyFactor;
    }

    /**
     * Returns the (maximum) backoff factor to be used for stable connectors
     * @return the (maximum) backoff factor to be used for stable connectors
     */
    public int getBackoffStableFactor() {
        return backoffStableFactor;
    }

    /**
     * Returns the backoff interval for standby (loop) connectors in seconds
     * @return the backoff interval for standby (loop) connectors in seconds
     */
    public long getBackoffStandbyInterval() {
        final int factor = getBackoffStandbyFactor();
        if (factor<=1) {
            return -1;
        } else {
            return factor * getHeartbeatInterval();
        }
    }

    @Override
    public long getConnectorPingInterval() {
        return getHeartbeatInterval();
    }

    @Override
    public long getConnectorPingTimeout() {
        return getHeartbeatTimeout();
    }

    @Override
    public String getSyncTokenPath() {
        return getDiscoveryResourcePath() + SYNC_TOKEN_RESOURCE;
    }

    @Override
    public String getIdMapPath() {
        return getDiscoveryResourcePath() + ID_MAP_RESOURCE;
    }

    @Override
    public long getClusterSyncServiceTimeoutMillis() {
        return -1;
    }

    @Override
    public long getClusterSyncServiceIntervalMillis() {
        return 1000;
    }

    public boolean useSyncTokenService() {
        return useSyncTokenService;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
