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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.OriginalGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.UpdatedGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.Xos;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.sdn._switch.UserGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.AiActivePassiveSwitchset;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.East;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.West;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.east.EastSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.west.WestSwitch;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupForwarder extends AbstractListeningCommiter<UserGroup> {

    private static final Logger LOG = LoggerFactory.getLogger(GroupForwarder.class);

    private ListenerRegistration<GroupForwarder> westGrouplistenerRegistration;
    private ListenerRegistration<GroupForwarder> eastGrouplistenerRegistration;

    private InstanceIdentifier<WestSwitch> westSwitchIID;
    private InstanceIdentifier<EastSwitch> eastSwitchIID;

    public GroupForwarder (final ForwardingRulesManager manager, final DataBroker db) {
        super(manager, UserGroup.class);
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
        InstanceIdentifier<UserGroup> westSwitchGroupIID = InstanceIdentifier.create(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<West>child(West.class)
                .<WestSwitch>child(WestSwitch.class).child(UserGroup.class);
        InstanceIdentifier<UserGroup> eastSwitchGroupIID = InstanceIdentifier.create(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<East>child(East.class)
                .<EastSwitch>child(EastSwitch.class).child(UserGroup.class);

        // Register west and east group listener, we will forward the group to the switch actor to handle.

        final DataTreeIdentifier<UserGroup> westGroupTreeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, westSwitchGroupIID);
        try {
            SimpleTaskRetryLooper looper = new SimpleTaskRetryLooper(ForwardingRulesManagerImpl.STARTUP_LOOP_TICK,
                    ForwardingRulesManagerImpl.STARTUP_LOOP_MAX_RETRIES);
            westGrouplistenerRegistration = looper.loopUntilNoException(new Callable<ListenerRegistration<GroupForwarder>>() {
                @Override
                public ListenerRegistration<GroupForwarder> call() throws Exception {
                    return db.registerDataTreeChangeListener(westGroupTreeId, GroupForwarder.this);
                }
            });
        } catch (final Exception e) {
            LOG.error("FRM group DataChange listener registration for WestSwitch group failed");
            throw new IllegalStateException("GroupForwarder startup fail! System needs restart.", e);
        }

        final DataTreeIdentifier<UserGroup> eastGroupTreeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, eastSwitchGroupIID);
        try {
            SimpleTaskRetryLooper looper = new SimpleTaskRetryLooper(ForwardingRulesManagerImpl.STARTUP_LOOP_TICK,
                    ForwardingRulesManagerImpl.STARTUP_LOOP_MAX_RETRIES);
            eastGrouplistenerRegistration = looper.loopUntilNoException(new Callable<ListenerRegistration<GroupForwarder>>() {
                @Override
                public ListenerRegistration<GroupForwarder> call() throws Exception {
                    return db.registerDataTreeChangeListener(eastGroupTreeId, GroupForwarder.this);
                }
            });
        } catch (final Exception e) {
            LOG.error("FRM Group DataChange listener registration for WestSwitch group failed");
            throw new IllegalStateException("GroupForwarder startup fail! System needs restart.", e);
        }
    }

    @Override
    public void close() {
        if (westGrouplistenerRegistration != null) {
            try {
                westGrouplistenerRegistration.close();
            } catch (final Exception e) {
                LOG.warn("Error by stop FRM GroupChangeListener: {}", e.getMessage());
                LOG.debug("Error by stop FRM GroupChangeListener..", e);
            }
            westGrouplistenerRegistration = null;
        }

        if (eastGrouplistenerRegistration != null) {
            try {
                eastGrouplistenerRegistration.close();
            } catch (final Exception e) {
                LOG.warn("Error by stop FRM GroupChangeListener: {}", e.getMessage());
                LOG.debug("Error by stop FRM GroupChangeListener..", e);
            }
            eastGrouplistenerRegistration = null;
        }
    }

    @Override
    public void remove(final InstanceIdentifier<UserGroup> identifier, final UserGroup removeDataObj) {
        if (this.westSwitchIID.contains(identifier)) {
            SdnSwitchManager.getSdnSwitchManager()
                    .getWestSdnSwitchActor().tell(new SdnSwitchActor.UserGroupOp(OFutils.GROUP_DELETE, removeDataObj, identifier), null);
        } else if (this.eastSwitchIID.contains(identifier)) {
            SdnSwitchManager.getSdnSwitchManager()
                    .getEastSdnSwitchActor().tell(new SdnSwitchActor.UserGroupOp(OFutils.GROUP_DELETE, removeDataObj, identifier), null);
        }
    }

    @Override
    public void update(final InstanceIdentifier<UserGroup> identifier, final UserGroup original, final UserGroup update) {

        final UserGroup originalGroup = (original);
        final UserGroup updatedGroup = (update);
        final UpdateGroupInputBuilder builder = new UpdateGroupInputBuilder();

        /* TODO:
        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
        builder.setGroupRef(new GroupRef(identifier));
        builder.setTransactionUri(new Uri(provider.getNewTransactionId()));
        builder.setUpdatedGroup((new UpdatedGroupBuilder(updatedGroup)).build());
        builder.setOriginalGroup((new OriginalGroupBuilder(originalGroup)).build());
        */
    }

    @Override
    public void add(final InstanceIdentifier<UserGroup> identifier, final UserGroup addDataObj) {
        if (this.westSwitchIID.contains(identifier)) {
            SdnSwitchManager.getSdnSwitchManager()
                    .getWestSdnSwitchActor().tell(new SdnSwitchActor.UserGroupOp(OFutils.GROUP_ADD, addDataObj, identifier), null);
        } else if (this.eastSwitchIID.contains(identifier)) {
            SdnSwitchManager.getSdnSwitchManager()
                    .getEastSdnSwitchActor().tell(new SdnSwitchActor.UserGroupOp(OFutils.GROUP_ADD, addDataObj, identifier), null);
        }
    }
}

