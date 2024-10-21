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
package org.apache.sling.discovery.impl.setup;

import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.impl.cluster.voting.VotingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class VotingEventListener implements EventListener {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 
     */
    private final VirtualInstance instance;
    volatile boolean stopped = false;
    private final String slingId;
    private final ConcurrentLinkedQueue<ResourceChange> q = new ConcurrentLinkedQueue<>();
    
    public VotingEventListener(VirtualInstance instance, final VotingHandler votingHandler, final String slingId) {
        this.instance = instance;
        this.slingId = slingId;
        Thread th = new Thread(() -> {
            while(!stopped) {
                try{
                    ResourceChange c = q.poll();
                    if (c == null) {
                        Thread.sleep(10);
                        continue;
                    }
                    logger.debug("async.run: delivering event to listener: "+slingId+", stopped: "+stopped+", change: "+c);
                    votingHandler.onChange(Collections.singletonList(c));
                } catch(Exception e) {
                    logger.error("async.run: got Exception: "+e, e);
                }
            }
        });
        th.setName("VotingEventListener-"+instance.getDebugName());
        th.setDaemon(true);
        th.start();
    }
    
    public void stop() {
        logger.debug("stop: stopping listener for slingId: "+slingId);
        stopped = true;
    }

    public void onEvent(EventIterator events) {
        if (stopped) {
            logger.info("onEvent: listener: "+slingId+" getting late events even though stopped: "+events.hasNext());
            return;
        }
        try {
            while (!stopped && events.hasNext()) {
                Event event = events.nextEvent();
                ChangeType type;
                if (event.getType() == Event.NODE_ADDED) {
                    type = ChangeType.ADDED;
                } else if (event.getType() == Event.NODE_MOVED) {
                    type = ChangeType.CHANGED;
                } else if (event.getType() == Event.NODE_REMOVED) {
                    type = ChangeType.REMOVED;
                } else {
                    type = ChangeType.CHANGED;
                }
                try {
                    ResourceChange c = new ResourceChange(type, event.getPath(), false, null, null, null);
                    logger.debug("onEvent: enqueuing event to listener: "+slingId+", stopped: "+stopped+", change: "+c);
                    q.add(c);
                } catch (RepositoryException e) {
                    logger.warn("RepositoryException: " + e, e);
                }
            }
            if (stopped) {
                logger.info("onEvent: listener stopped: "+slingId+", pending events: "+events.hasNext());
            }
        } catch (Throwable th) {
            try {
                this.instance.dumpRepo();
            } catch (Exception e) {
                logger.info("onEvent: could not dump as part of catching a throwable, e="+e+", th="+th);
            }
            logger.error(
                    "Throwable occurred in onEvent: " + th, th);
        }
    }
}