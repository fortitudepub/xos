/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * <p/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.xsdn.main.config.frm.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.xsdn.main.config.frm.FlowNodeObserver;
import com.xsdn.main.config.frm.ForwardingRulesCommiter;
import com.xsdn.main.config.frm.ForwardingRulesManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.sdn._switch.UserFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* ForwardingRules manager is only used to proxy user flow request to our switch actor.
   BY ZDY @ 20151011. */
public class ForwardingRulesManagerImpl implements ForwardingRulesManager {

    private static final Logger LOG = LoggerFactory.getLogger(ForwardingRulesManagerImpl.class);
    public static final int STARTUP_LOOP_TICK = 500;
    public static final int STARTUP_LOOP_MAX_RETRIES = 8;

    private final DataBroker dataService;

    private ForwardingRulesCommiter<UserFlow> flowListener;
    private ForwardingRulesCommiter<Group> groupListener;
    private FlowNodeObserver nodeListener;

    public ForwardingRulesManagerImpl(final DataBroker dataBroker) {
        this.dataService = Preconditions.checkNotNull(dataBroker, "DataBroker can not be null!");
    }

    @Override
    public void start() {

        this.flowListener = new FlowForwarder(this, dataService);

        this.groupListener = new GroupForwarder(this, dataService);

        this.nodeListener = new FlowNodeObserverImpl(this, dataService);

        LOG.info("ForwardingRulesManager has started successfully.");

    }

    @Override
    public void close() throws Exception {
        if (this.flowListener != null) {
            this.flowListener.close();
            this.flowListener = null;
        }
        if (this.groupListener != null) {
            this.groupListener.close();
            this.groupListener = null;
        }
        if (this.nodeListener != null) {
            this.nodeListener.close();
            this.nodeListener = null;
        }
    }
}

