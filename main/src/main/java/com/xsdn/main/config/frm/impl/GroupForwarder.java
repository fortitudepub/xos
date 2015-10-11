package com.xsdn.main.config.frm.impl;

import com.google.common.base.Preconditions;
import java.util.concurrent.Callable;

import com.xsdn.main.config.frm.ForwardingRulesManager;
import com.xsdn.main.util.SimpleTaskRetryLooper;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
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
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupForwarder extends AbstractListeningCommiter<Group> {

    private static final Logger LOG = LoggerFactory.getLogger(GroupForwarder.class);

    private ListenerRegistration<GroupForwarder> listenerRegistration;

    public GroupForwarder (final ForwardingRulesManager manager, final DataBroker db) {
        super(manager, Group.class);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");
//        final DataTreeIdentifier<Group> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getWildCardPath());
//
//        try {
//            SimpleTaskRetryLooper looper = new SimpleTaskRetryLooper(ForwardingRulesManagerImpl.STARTUP_LOOP_TICK,
//                    ForwardingRulesManagerImpl.STARTUP_LOOP_MAX_RETRIES);
//            listenerRegistration = looper.loopUntilNoException(new Callable<ListenerRegistration<GroupForwarder>>() {
//                @Override
//                public ListenerRegistration<GroupForwarder> call() throws Exception {
//                    return db.registerDataTreeChangeListener(treeId, GroupForwarder.this);
//                }
//            });
//        } catch (final Exception e) {
//            LOG.warn("FRM Group DataChange listener registration fail!");
//            LOG.debug("FRM Group DataChange listener registration fail ..", e);
//            throw new IllegalStateException("FlowForwarder startup fail! System needs restart.", e);
//        }
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (Exception e) {
                LOG.warn("Error by stop FRM GroupChangeListener: {}", e.getMessage());
                LOG.debug("Error by stop FRM GroupChangeListener..", e);
            }
            listenerRegistration = null;
        }
    }

    @Override
    public void remove(final InstanceIdentifier<Group> identifier, final Group removeDataObj) {

        final Group group = (removeDataObj);
        final RemoveGroupInputBuilder builder = new RemoveGroupInputBuilder(group);

        /* TODO:
        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
        builder.setGroupRef(new GroupRef(identifier));
        builder.setTransactionUri(new Uri(provider.getNewTransactionId()));
        */
    }

    @Override
    public void update(final InstanceIdentifier<Group> identifier, final Group original, final Group update) {

        final Group originalGroup = (original);
        final Group updatedGroup = (update);
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
    public void add(final InstanceIdentifier<Group> identifier, final Group addDataObj) {

        final Group group = (addDataObj);
        final AddGroupInputBuilder builder = new AddGroupInputBuilder(group);
        /* TODO:
        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
        builder.setGroupRef(new GroupRef(identifier));
        builder.setTransactionUri(new Uri(provider.getNewTransactionId()));
        */
    }
}

