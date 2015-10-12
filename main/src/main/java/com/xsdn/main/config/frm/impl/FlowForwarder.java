package com.xsdn.main.config.frm.impl;

import com.google.common.base.Preconditions;
import java.util.concurrent.Callable;

import com.xsdn.main.config.frm.ForwardingRulesManager;
import com.xsdn.main.sw.SdnSwitchActor;
import com.xsdn.main.sw.SdnSwitchManager;
import com.xsdn.main.util.OFutils;
import com.xsdn.main.util.SimpleTaskRetryLooper;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.Xos;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.sdn._switch.UserFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.AiActivePassiveSwitchset;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.East;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.West;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.east.EastSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.west.WestSwitch;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FlowForwarder extends AbstractListeningCommiter<UserFlow> {

    private static final Logger LOG = LoggerFactory.getLogger(FlowForwarder.class);

    private ListenerRegistration<FlowForwarder> westFlowlistenerRegistration;
    private ListenerRegistration<FlowForwarder> eastFlowlistenerRegistration;
    private InstanceIdentifier<WestSwitch> westSwitchIID;
    private InstanceIdentifier<EastSwitch> eastSwitchIID;

    public FlowForwarder(final ForwardingRulesManager manager, final DataBroker db) {
        super(manager, UserFlow.class);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");

        this.westSwitchIID = InstanceIdentifier.create(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<West>child(West.class)
                .<WestSwitch>child(WestSwitch.class);
        this.eastSwitchIID = InstanceIdentifier.create(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<East>child(East.class)
                .<EastSwitch>child(EastSwitch.class);

        registrationListener(db);
    }

    private void registrationListener(final DataBroker db) {
        InstanceIdentifier<UserFlow> westSwitchFlowIID = InstanceIdentifier.create(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<West>child(West.class)
                .<WestSwitch>child(WestSwitch.class).child(UserFlow.class);
        InstanceIdentifier<UserFlow> eastSwitchFlowIID = InstanceIdentifier.create(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<East>child(East.class)
                .<EastSwitch>child(EastSwitch.class).child(UserFlow.class);

        // Register west and east flow listener, we will forward the flow to the switch actor to handle.

        final DataTreeIdentifier<UserFlow> westFlowTreeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, westSwitchFlowIID);
        try {
            SimpleTaskRetryLooper looper = new SimpleTaskRetryLooper(ForwardingRulesManagerImpl.STARTUP_LOOP_TICK,
                    ForwardingRulesManagerImpl.STARTUP_LOOP_MAX_RETRIES);
            westFlowlistenerRegistration = looper.loopUntilNoException(new Callable<ListenerRegistration<FlowForwarder>>() {
                @Override
                public ListenerRegistration<FlowForwarder> call() throws Exception {
                    return db.registerDataTreeChangeListener(westFlowTreeId, FlowForwarder.this);
                }
            });
        } catch (final Exception e) {
            LOG.error("FRM Flow DataChange listener registration for WestSwitch flow failed");
            throw new IllegalStateException("FlowForwarder startup fail! System needs restart.", e);
        }

        final DataTreeIdentifier<UserFlow> eastFlowTreeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, eastSwitchFlowIID);
        try {
            SimpleTaskRetryLooper looper = new SimpleTaskRetryLooper(ForwardingRulesManagerImpl.STARTUP_LOOP_TICK,
                    ForwardingRulesManagerImpl.STARTUP_LOOP_MAX_RETRIES);
            eastFlowlistenerRegistration = looper.loopUntilNoException(new Callable<ListenerRegistration<FlowForwarder>>() {
                @Override
                public ListenerRegistration<FlowForwarder> call() throws Exception {
                    return db.registerDataTreeChangeListener(eastFlowTreeId, FlowForwarder.this);
                }
            });
        } catch (final Exception e) {
            LOG.error("FRM Flow DataChange listener registration for WestSwitch flow failed");
            throw new IllegalStateException("FlowForwarder startup fail! System needs restart.", e);
        }
    }

    @Override
    public void close() {
        if (westFlowlistenerRegistration != null) {
            try {
                westFlowlistenerRegistration.close();
            } catch (final Exception e) {
                LOG.warn("Error by stop FRM FlowChangeListener: {}", e.getMessage());
                LOG.debug("Error by stop FRM FlowChangeListener..", e);
            }
            westFlowlistenerRegistration = null;
        }

        if (eastFlowlistenerRegistration != null) {
            try {
                eastFlowlistenerRegistration.close();
            } catch (final Exception e) {
                LOG.warn("Error by stop FRM FlowChangeListener: {}", e.getMessage());
                LOG.debug("Error by stop FRM FlowChangeListener..", e);
            }
            eastFlowlistenerRegistration = null;
        }
    }

    @Override
    public void remove(final InstanceIdentifier<UserFlow> identifier, final UserFlow removeDataObj) {
        if (this.westSwitchIID.contains(identifier)) {
            SdnSwitchManager.getSdnSwitchManager()
                    .getWestSdnSwitchActor().tell(new SdnSwitchActor.UserFlowOp(OFutils.FLOW_DELETE, removeDataObj), null);
        } else if (this.eastSwitchIID.contains(identifier)) {
            SdnSwitchManager.getSdnSwitchManager()
                    .getEastSdnSwitchActor().tell(new SdnSwitchActor.UserFlowOp(OFutils.FLOW_DELETE, removeDataObj), null);
        }
    }

    @Override
    public void update(final InstanceIdentifier<UserFlow> identifier, final UserFlow original, final UserFlow update) {
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
    }

    @Override
    public void add(final InstanceIdentifier<UserFlow> identifier, final UserFlow addDataObj) {
        if (this.westSwitchIID.contains(identifier)) {
            SdnSwitchManager.getSdnSwitchManager()
                    .getWestSdnSwitchActor().tell(new SdnSwitchActor.UserFlowOp(OFutils.FLOW_ADD, addDataObj), null);
        } else if (this.eastSwitchIID.contains(identifier)) {
            SdnSwitchManager.getSdnSwitchManager()
                    .getEastSdnSwitchActor().tell(new SdnSwitchActor.UserFlowOp(OFutils.FLOW_ADD, addDataObj), null);
        }
    }
}

