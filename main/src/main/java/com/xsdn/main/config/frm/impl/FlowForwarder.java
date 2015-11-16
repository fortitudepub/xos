package com.xsdn.main.config.frm.impl;

import com.google.common.base.Preconditions;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.xsdn.main.config.frm.ForwardingRulesManager;
import com.xsdn.main.sw.SdnSwitchActor;
import com.xsdn.main.sw.SdnSwitchManager;
import com.xsdn.main.util.OFutils;
import com.xsdn.main.util.SimpleTaskRetryLooper;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.Xos;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.sdn._switch.UserFlow;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.AiActivePassiveSwitchset;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.AiManagedSubnet;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.East;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.West;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.east.EastSwitch;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.west.WestSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Migrated to clustered version.
public class FlowForwarder implements ClusteredDataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(FlowForwarder.class);

    //private ListenerRegistration<FlowForwarder> westFlowlistenerRegistration;
    //private ListenerRegistration<FlowForwarder> eastFlowlistenerRegistration;
    private  Registration westFlowRegistration;
    private  Registration eastFlowRegistration;
    private InstanceIdentifier<WestSwitch> westSwitchIID;
    private InstanceIdentifier<EastSwitch> eastSwitchIID;

    public FlowForwarder(final ForwardingRulesManager manager, final DataBroker db) {
        //super(manager, UserFlow.class);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");

        this.westSwitchIID = InstanceIdentifier.create(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<West>child(West.class)
                .<WestSwitch>child(WestSwitch.class);
        this.eastSwitchIID = InstanceIdentifier.create(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<East>child(East.class)
                .<EastSwitch>child(EastSwitch.class);

        registerListener(db);
    }

    private void registerListener(final DataBroker db) {
        final InstanceIdentifier<UserFlow> westSwitchFlowIID = InstanceIdentifier.create(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<West>child(West.class)
                .<WestSwitch>child(WestSwitch.class).child(UserFlow.class);
        final InstanceIdentifier<UserFlow> eastSwitchFlowIID = InstanceIdentifier.create(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<East>child(East.class)
                .<EastSwitch>child(EastSwitch.class).child(UserFlow.class);

        // Register west and east flow listener, we will forward the flow to the switch actor to handle.

        //final DataTreeIdentifier<UserFlow> westFlowTreeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, westSwitchFlowIID);
        try {
            SimpleTaskRetryLooper looper = new SimpleTaskRetryLooper(ForwardingRulesManagerImpl.STARTUP_LOOP_TICK,
                    ForwardingRulesManagerImpl.STARTUP_LOOP_MAX_RETRIES);
            westFlowRegistration = looper.loopUntilNoException(new Callable<Registration>() {
                @Override
                public Registration call() throws Exception {
                    return db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                            westSwitchFlowIID, FlowForwarder.this,  AsyncDataBroker.DataChangeScope.BASE);
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
            eastFlowRegistration = looper.loopUntilNoException(new Callable<Registration>() {
                @Override
                public Registration call() throws Exception {
                    return db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                            eastSwitchFlowIID, FlowForwarder.this, AsyncDataBroker.DataChangeScope.BASE);
                }
            });
        } catch (final Exception e) {
            LOG.error("FRM Flow DataChange listener registration for WestSwitch flow failed");
            throw new IllegalStateException("FlowForwarder startup fail! System needs restart.", e);
        }
    }

    private void processInstanceId(InstanceIdentifier instanceId, DataObject data, boolean updDelFlag) {
        if (updDelFlag) {
            LOG.info("add flow");
            if (instanceId.getTargetType().equals(UserFlow.class)) {
                if (this.westSwitchIID.contains(instanceId)) {
                    SdnSwitchManager.getSdnSwitchManager()
                            .getWestSdnSwitchActor().tell(new SdnSwitchActor.UserFlowOp(OFutils.FLOW_ADD, (UserFlow) data, instanceId), null);
                } else if (this.eastSwitchIID.contains(instanceId)) {
                    SdnSwitchManager.getSdnSwitchManager()
                            .getEastSdnSwitchActor().tell(new SdnSwitchActor.UserFlowOp(OFutils.FLOW_ADD, (UserFlow) data, instanceId), null);
                }
            }
        } else {
            LOG.info("delete user flow");
            if (instanceId.getTargetType().equals(UserFlow.class)) {
                if (this.westSwitchIID.contains(instanceId)) {
                    SdnSwitchManager.getSdnSwitchManager()
                            .getWestSdnSwitchActor().tell(new SdnSwitchActor.UserFlowOp(OFutils.FLOW_DELETE, (UserFlow) data, instanceId), null);
                } else if (this.eastSwitchIID.contains(instanceId)) {
                    SdnSwitchManager.getSdnSwitchManager()
                            .getEastSdnSwitchActor().tell(new SdnSwitchActor.UserFlowOp(OFutils.FLOW_DELETE, (UserFlow) data, instanceId), null);
                }
            }
        }
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent)
    {
        // ZDY: and also we should take leadership into consideration here, only the leader application instance
        // should manage the SDN switch, or we can delegate the work to the SDN switch, let it decide according
        // the leadership.

        if(dataChangeEvent == null)
        {
            return;
        }

        Map<InstanceIdentifier<?>, DataObject> createdData = dataChangeEvent.getCreatedData();
        Map<InstanceIdentifier<?>, DataObject> updatedData = dataChangeEvent.getUpdatedData();
        Set<InstanceIdentifier<?>> removedData = dataChangeEvent.getRemovedPaths();
        Map<InstanceIdentifier<?>, DataObject> originalData = dataChangeEvent.getOriginalData();

        if ((createdData != null) && !(createdData.isEmpty()))
        {
            Set<InstanceIdentifier<?>> createdSet = createdData.keySet();
            for (InstanceIdentifier<?> instanceId : createdSet)
            {
                processInstanceId (instanceId, createdData.get(instanceId), true);
            }
        }

        if ((updatedData != null) && !(updatedData.isEmpty()))
        {
            Set<InstanceIdentifier<?>> updatedSet = updatedData.keySet();
            for (InstanceIdentifier<?> instanceId : updatedSet)
            {
                processInstanceId (instanceId, updatedData.get(instanceId), true);
            }
        }

        if ((removedData != null) && (!removedData.isEmpty()) && (originalData != null) && (!originalData.isEmpty()))
        {
            for (InstanceIdentifier<?> instanceId : removedData)
            {
                processInstanceId (instanceId, originalData.get(instanceId), false);
            }
        }
    }
//
//
//    @Override
//    public void close() {
//        if (westFlowlistenerRegistration != null) {
//            try {
//                westFlowlistenerRegistration.close();
//            } catch (final Exception e) {
//                LOG.warn("Error by stop FRM FlowChangeListener: {}", e.getMessage());
//                LOG.debug("Error by stop FRM FlowChangeListener..", e);
//            }
//            westFlowlistenerRegistration = null;
//        }
//
//        if (eastFlowlistenerRegistration != null) {
//            try {
//                eastFlowlistenerRegistration.close();
//            } catch (final Exception e) {
//                LOG.warn("Error by stop FRM FlowChangeListener: {}", e.getMessage());
//                LOG.debug("Error by stop FRM FlowChangeListener..", e);
//            }
//            eastFlowlistenerRegistration = null;
//        }
//    }
//
//    @Override
//    public void remove(final InstanceIdentifier<UserFlow> identifier, final UserFlow removeDataObj) {
//        if (this.westSwitchIID.contains(identifier)) {
//            SdnSwitchManager.getSdnSwitchManager()
//                    .getWestSdnSwitchActor().tell(new SdnSwitchActor.UserFlowOp(OFutils.FLOW_DELETE, removeDataObj, identifier), null);
//        } else if (this.eastSwitchIID.contains(identifier)) {
//            SdnSwitchManager.getSdnSwitchManager()
//                    .getEastSdnSwitchActor().tell(new SdnSwitchActor.UserFlowOp(OFutils.FLOW_DELETE, removeDataObj, identifier), null);
//        }
//    }
//
//    @Override
//    public void update(final InstanceIdentifier<UserFlow> identifier, final UserFlow original, final UserFlow update) {
//        final UpdateFlowInputBuilder builder = new UpdateFlowInputBuilder();
//
//        /* TODO:
//        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
//        builder.setFlowRef(new FlowRef(identifier));
//        builder.setTransactionUri(new Uri(provider.getNewTransactionId()));
//
//        // This method is called only when a given flow object in datastore
//        // has been updated. So FRM always needs to set strict flag into
//        // update-flow input so that only a flow entry associated with
//        // a given flow object is updated.
//        builder.setUpdatedFlow((new UpdatedFlowBuilder(update)).setStrict(Boolean.TRUE).build());
//        builder.setOriginalFlow((new OriginalFlowBuilder(original)).setStrict(Boolean.TRUE).build());
//        */
//    }
//
//    @Override
//    public void add(final InstanceIdentifier<UserFlow> identifier, final UserFlow addDataObj) {
//        if (this.westSwitchIID.contains(identifier)) {
//            SdnSwitchManager.getSdnSwitchManager()
//                    .getWestSdnSwitchActor().tell(new SdnSwitchActor.UserFlowOp(OFutils.FLOW_ADD, addDataObj, identifier), null);
//        } else if (this.eastSwitchIID.contains(identifier)) {
//            SdnSwitchManager.getSdnSwitchManager()
//                    .getEastSdnSwitchActor().tell(new SdnSwitchActor.UserFlowOp(OFutils.FLOW_ADD, addDataObj, identifier), null);
//        }
//    }
}

