/*
 * CopyRight (c) 2015 xsdn, co,.Ltd and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.xsdn.main.rpc;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.XosService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XosRpcProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(XosRpcProvider.class);
    private BindingAwareBroker.RpcRegistration<XosService> xosService;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("XosRpcProvider Session Initiated");

        LOG.info("XOS(eXtensible network Operating System) started");

        // Register the RPC service.
        xosService = session.addRpcImplementation(XosService.class, new XosRpcImpl());
    }

    @Override
    public void close() throws Exception {
        LOG.info("XosRpcProvider Closed");
    }

}
