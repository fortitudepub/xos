package com.xsdn.main.config.frm.impl;

import com.google.common.base.Preconditions;
import com.xsdn.main.config.frm.ForwardingRulesCommiter;
import com.xsdn.main.config.frm.ForwardingRulesManager;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import java.util.Collection;

public abstract class AbstractListeningCommiter <T extends DataObject> implements ForwardingRulesCommiter<T> {

    protected ForwardingRulesManager provider;

    protected final Class<T> clazz;

    public AbstractListeningCommiter (ForwardingRulesManager provider, Class<T> clazz) {
        this.provider = Preconditions.checkNotNull(provider, "ForwardingRulesManager can not be null!");
        this.clazz = Preconditions.checkNotNull(clazz, "Class can not be null!");
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<T>> changes) {
        Preconditions.checkNotNull(changes, "Changes may not be null!");

        for (DataTreeModification<T> change : changes) {
            final InstanceIdentifier<T> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<T> mod = change.getRootNode();

            switch (mod.getModificationType()) {
                case DELETE:
                    remove(key, mod.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                    update(key, mod.getDataBefore(), mod.getDataAfter());
                    break;
                case WRITE:
                    if (mod.getDataBefore() == null) {
                        add(key, mod.getDataAfter());
                    } else {
                        update(key, mod.getDataBefore(), mod.getDataAfter());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }
}

