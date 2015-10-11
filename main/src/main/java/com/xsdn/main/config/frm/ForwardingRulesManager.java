/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package com.xsdn.main.config.frm;

/* ForwardingRules manager is only used to proxy user flow request to our switch actor.
   BY ZDY @ 20151011. */
public interface ForwardingRulesManager extends AutoCloseable {

    public void start();
}

