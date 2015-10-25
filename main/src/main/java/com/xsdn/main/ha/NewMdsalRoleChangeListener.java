package com.xsdn.main.ha;

import com.google.common.base.Optional;
import com.xsdn.main.util.Constants;
import org.opendaylight.controller.md.sal.common.api.clustering.*;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fortitude on 15-10-24.
 */
public class NewMdsalRoleChangeListener implements EntityOwnershipListener {
    private final static Logger LOG = LoggerFactory.getLogger(NewMdsalRoleChangeListener.class);

    EntityOwnershipService entityOwnershipService;
    String entityId;
    Entity entity;

    public NewMdsalRoleChangeListener(EntityOwnershipService entityOwnershipService) {
        this.entityOwnershipService = entityOwnershipService;
        this.entity = new Entity(Constants.XOS_HA_ENTITY_TYPE, Constants.XOS_HA_ENTITY_ID);
        this.startup();
    }

    private void startup() {
        this. entityOwnershipService.registerListener(Constants.XOS_HA_ENTITY_TYPE, this);

        try {
            this.entityOwnershipService.registerCandidate(entity);
        } catch (CandidateAlreadyRegisteredException e) {
            // The case when we restart this node, skip it.
        }

        LOG.info("XOS entity owner ship registration finished");

        Optional<EntityOwnershipState> initialState = entityOwnershipService.getOwnershipState(entity);
        if (initialState.isPresent() && initialState.get().isOwner()) {
            XosAppStatusMgr.getXosAppStatus().setStatus(XosAppStatusMgr.APP_STATUS_ACTIVE);
        }
    }
    @Override
    public void ownershipChanged(EntityOwnershipChange entityOwnershipChange) {
        LOG.info("XOS Entity owner ship change entity: {}, isOwner: {}, hasOwner: {}",
                entityOwnershipChange.getEntity(),
                entityOwnershipChange.isOwner(),
                entityOwnershipChange.hasOwner());
        
        if (entityOwnershipChange.isOwner()) {
            XosAppStatusMgr.getXosAppStatus().setStatus(XosAppStatusMgr.APP_STATUS_ACTIVE);
        } else {
            XosAppStatusMgr.getXosAppStatus().setStatus(XosAppStatusMgr.APP_STATUS_BACKUP);
        }
    }
}
