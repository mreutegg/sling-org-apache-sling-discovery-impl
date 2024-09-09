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
package org.apache.sling.discovery.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        localization = "OSGI-INF/l10n/metatype",
        name = "%config.name",
        description = "%config.description"
)
public @interface DiscoveryServiceConfig {

    /** Configure the timeout (in seconds) after which an instance is considered dead/crashed. */
    long DEFAULT_HEARTBEAT_TIMEOUT = 120;
    @AttributeDefinition(name = "%heartbeatTimeout.name", description = "%heartbeatTimeout.description")
    long heartbeatTimeout() default DEFAULT_HEARTBEAT_TIMEOUT;

    /** Configure the interval (in seconds) according to which the heartbeats are exchanged in the topology. */
    long DEFAULT_HEARTBEAT_INTERVAL = 30;
    @AttributeDefinition(name = "%heartbeatInterval.name", description = "%heartbeatInterval.description")
    long heartbeatInterval() default DEFAULT_HEARTBEAT_INTERVAL;

    /** Configure the time (in seconds) which must be passed at minimum between sending TOPOLOGY_CHANGING/_CHANGED (avoid flooding). */
    int DEFAULT_MIN_EVENT_DELAY = 3;
    @AttributeDefinition(name = "%minEventDelay.name", description = "%minEventDelay.description")
    int minEventDelay() default DEFAULT_MIN_EVENT_DELAY;

    /** Configure the socket connect timeout for topology connectors. */
    int DEFAULT_CONNECTION_TIMEOUT = 10;
    @AttributeDefinition(name = "%connectionTimeout.name", description = "%connectionTimeout.description")
    int connectionTimeout() default DEFAULT_CONNECTION_TIMEOUT;

    /** Configure the socket read timeout (SO_TIMEOUT) for topology connectors. */
    int DEFAULT_SO_TIMEOUT = 10;
    @AttributeDefinition(name = "%soTimeout.name", description = "%soTimeout.description")
    int soTimeout() default DEFAULT_SO_TIMEOUT;

    /** Name of the repository descriptor to be taken into account for leader election:
     those instances have preference to become leader which have the corresponding descriptor value of 'false' */
    @AttributeDefinition(name = "%leaderElectionRepositoryDescriptor.name", description = "%leaderElectionRepositoryDescriptor.description")
    String leaderElectionRepositoryDescriptor();

    /**
     * Whether or not (default false) the leaderElectionRepositoryDescriptor should be inverted (if that one
     * is configured at all).
     */
    @AttributeDefinition(name = "%invertRepositoryDescriptor.name", description = "%invertRepositoryDescriptor.description")
    boolean invertRepositoryDescriptor() default false;

    /** URLs where to join a topology, eg http://localhost:4502/libs/sling/topology/connector */
    @AttributeDefinition(name = "%topologyConnectorUrls.name", description = "%topologyConnectorUrls.description", cardinality=1024)
    String[] topologyConnectorUrls() default {};

    /** list of ips and/or hostnames which are allowed to connect to /libs/sling/topology/connector */
    @AttributeDefinition(name = "%topologyConnectorWhitelist.name", description = "%topologyConnectorWhitelist.description")
    String[] topologyConnectorWhitelist() default {"localhost", "127.0.0.1"};
    String[] DEFAULT_TOPOLOGY_CONNECTOR_WHITELIST = {"localhost", "127.0.0.1"};

    /** Path of resource where to keep discovery information, e.g /var/discovery/impl/ */
    @AttributeDefinition(name = "%discoveryResourcePath.name", description = "%discoveryResourcePath.description")
    String discoveryResourcePath() default DEFAULT_DISCOVERY_RESOURCE_PATH;
    String DEFAULT_DISCOVERY_RESOURCE_PATH = "/var/discovery/impl/";

    /**
     * If set to true, local-loops of topology connectors are automatically stopped when detected so.
     */
    @AttributeDefinition(name = "%autoStopLocalLoopEnabled.name", description = "%autoStopLocalLoopEnabled.description")
    boolean autoStopLocalLoopEnabled() default false;

    /**
     * If set to true, request body will be gzipped - only works if counter-part accepts gzip-requests!
     */
    @AttributeDefinition(name = "%gzipConnectorRequestsEnabled.name", description = "%gzipConnectorRequestsEnabled.description")
    boolean gzipConnectorRequestsEnabled() default false;

    /**
     * If set to true, hmac is enabled and the white list is disabled.
     */
    @AttributeDefinition(name = "%hmacEnabled.name", description = "%hmacEnabled.description")
    boolean hmacEnabled() default true;

    /**
     * If set to true, and the whitelist is disabled, messages will be encrypted.
     */
    @AttributeDefinition()
    boolean enableEncryption() default false;

    /**
     * The value for the shared key, shared amongst all instances in the same cluster.
     */
    @AttributeDefinition(name = "%sharedKey.name", description = "%sharedKey.description")
    String sharedKey();

    /**
     * The default lifetime of a HMAC shared key in ms. (4h)
     */
    long DEFAULT_SHARED_KEY_INTERVAL = 3600L*1000*4;
    @AttributeDefinition(name = "%hmacSharedKeyTTL.name", description = "%hmacSharedKeyTTL.description")
    long hmacSharedKeyTTL() default DEFAULT_SHARED_KEY_INTERVAL;

    /**
     * The property for defining the backoff factor for standby (loop) connectors
     */
    @AttributeDefinition(name = "%backoffStandbyFactor.name", description = "%backoffStandbyFactor.description")
    int backoffStandbyFactor() default DEFAULT_BACKOFF_STANDBY_FACTOR;
    int DEFAULT_BACKOFF_STANDBY_FACTOR = 5;

    /**
     * The property for defining the maximum backoff factor for stable connectors
     */
    @AttributeDefinition(name = "%backoffStableFactor.name", description = "%backoffStableFactor.description")
    int backoffStableFactor() default DEFAULT_BACKOFF_STABLE_FACTOR;
    int DEFAULT_BACKOFF_STABLE_FACTOR = 5;

    /**
     * when set to true and the syncTokenService (of discovery.commons) is available,
     * then it is used
     */
    @AttributeDefinition(name = "%useSyncTokenServiceEnabled.name", description = "%useSyncTokenServiceEnabled.description")
    boolean useSyncTokenServiceEnabled() default true;

}
