/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.xsdn.main.config.frm.impl;

import com.google.common.base.Preconditions;
import java.util.concurrent.Callable;

import com.xsdn.main.config.frm.ForwardingRulesManager;
import com.xsdn.main.util.SimpleTaskRetryLooper;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.Xos;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.sdn._switch.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.AiActivePassiveSwitchset;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.West;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.west.WestSwitch;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * GroupForwarder
 * It implements {@link org.opendaylight.controller.md.sal.binding.api.DataChangeListener}}
 * for WildCardedPath to {@link Flow} and ForwardingRulesCommiter interface for methods:
 *  add, update and remove {@link Flow} processing for
 *  {@link org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class WestFlowForwarder extends AbstractListeningCommiter<Flow> {

    private static final Logger LOG = LoggerFactory.getLogger(WestFlowForwarder.class);

    private ListenerRegistration<WestFlowForwarder> listenerRegistration;

    public WestFlowForwarder(final ForwardingRulesManager manager, final DataBroker db) {
        super(manager, Flow.class);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");
        registrationListener(db);
    }

    private void registrationListener(final DataBroker db) {
        final DataTreeIdentifier<Flow> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getWildCardPath());
        try {
            SimpleTaskRetryLooper looper = new SimpleTaskRetryLooper(ForwardingRulesManagerImpl.STARTUP_LOOP_TICK,
                    ForwardingRulesManagerImpl.STARTUP_LOOP_MAX_RETRIES);
            listenerRegistration = looper.loopUntilNoException(new Callable<ListenerRegistration<WestFlowForwarder>>() {
                @Override
                public ListenerRegistration<WestFlowForwarder> call() throws Exception {
                    return db.registerDataTreeChangeListener(treeId, WestFlowForwarder.this);
                }
            });
        } catch (final Exception e) {
            LOG.warn("FRM Flow DataChange listener registration fail!");
            LOG.debug("FRM Flow DataChange listener registration fail ..", e);
            throw new IllegalStateException("WestFlowForwarder startup fail! System needs restart.", e);
        }
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception e) {
                LOG.warn("Error by stop FRM FlowChangeListener: {}", e.getMessage());
                LOG.debug("Error by stop FRM FlowChangeListener..", e);
            }
            listenerRegistration = null;
        }
    }

    @Override
    public void remove(final InstanceIdentifier<Flow> identifier, final Flow removeDataObj) {
        final RemoveFlowInputBuilder builder = new RemoveFlowInputBuilder(removeDataObj);

        /* TODO:
        builder.setFlowRef(new FlowRef(identifier));
        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
        builder.setFlowTable(new FlowTableRef(nodeIdent.child(Table.class, tableKey)));

        // This method is called only when a given flow object has been
        // removed from datastore. So FRM always needs to set strict flag
        // into remove-flow input so that only a flow entry associated with
        // a given flow object is removed.
        builder.setTransactionUri(new Uri(provider.getNewTransactionId())).
                setStrict(Boolean.TRUE);
        */
        provider.getSalFlowService().removeFlow(builder.build());
    }

    @Override
    public void update(final InstanceIdentifier<Flow> identifier, final Flow original, final Flow update) {
        final UpdateFlowInputBuilder builder = new UpdateFlowInputBuilder();

        /* TODO:
        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
        builder.setFlowRef(new FlowRef(identifier));
        builder.setTransactionUri(new Uri(provider.getNewTransactionId()));

        // This method is called only when a given flow object in datastore
        // has been updated. So FRM always needs to set strict flag into
        // update-flow input so that only a flow entry associated with
        // a given flow object is updated.
        builder.setUpdatedFlow((new UpdatedFlowBuilder(update)).setStrict(Boolean.TRUE).build());
        builder.setOriginalFlow((new OriginalFlowBuilder(original)).setStrict(Boolean.TRUE).build());
        */
        provider.getSalFlowService().updateFlow(builder.build());
    }

    @Override
    public void add(final InstanceIdentifier<Flow> identifier, final Flow addDataObj) {
        final AddFlowInputBuilder builder = new AddFlowInputBuilder(addDataObj);

        /* TODO:
        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
        builder.setFlowRef(new FlowRef(identifier));
        builder.setFlowTable(new FlowTableRef(nodeIdent.child(Table.class, tableKey)));
        builder.setTransactionUri(new Uri(provider.getNewTransactionId()));
        */
        provider.getSalFlowService().addFlow(builder.build());
    }

    @Override
    protected InstanceIdentifier<Flow> getWildCardPath() {
        // ZDY: we monitor the flow defined in our own models, we do not use the default openflowplugin model.
        return InstanceIdentifier.create(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<West>child(West.class)
                .<WestSwitch>child(WestSwitch.class).child(Flow.class);
    }
}

