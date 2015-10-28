package com.xsdn.main.sw;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.xsdn.main.util.Constants;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.FlowId;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.Xos;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.sdn._switch.AppFlow;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.sdn._switch.AppFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.sdn._switch.AppFlowKey;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.AiActivePassiveSwitchset;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.East;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.West;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.east.EastSwitch;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.west.WestSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fortitude on 15-10-11.
 */
public class MdsalHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SdnSwitchActor.class);

    private DataBroker dataService = null;

    public MdsalHelper(DataBroker dataService) {
        this.dataService = dataService;
    }

    // generate app flow builder by converting from ofpluginflow.
    public void storeAppFlow(NodeId nodeId, String flowName, Match match, Instructions instructions) {
        final FlowId appFlowId = new FlowId(flowName);
        final WriteTransaction writeTransaction = dataService.newWriteOnlyTransaction();

        AppFlowKey appFlowKey = new AppFlowKey(appFlowId);

        InstanceIdentifier<AppFlow> appFlowIID;

        if (SdnSwitchManager.getSdnSwitchManager().isEast(nodeId))
            appFlowIID = InstanceIdentifier.<Xos>builder(Xos.class)
                    .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                    .<East>child(East.class)
                    .<EastSwitch>child(EastSwitch.class)
                    .<AppFlow, AppFlowKey>child(AppFlow.class, appFlowKey).build();
        else if (SdnSwitchManager.getSdnSwitchManager().isWest(nodeId)) {
            appFlowIID = InstanceIdentifier.<Xos>builder(Xos.class)
                    .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                    .<West>child(West.class)
                    .<WestSwitch>child(WestSwitch.class)
                    .<AppFlow, AppFlowKey>child(AppFlow.class, appFlowKey).build();
        }
        else {
            writeTransaction.cancel();
            return;
        }

        // setKey is must because instantiate AppFlowBuilder with ofpluginFlow will not set app flow key.
        AppFlowBuilder appFlowBuilder = new AppFlowBuilder().setKey(appFlowKey).setId(appFlowId);
        appFlowBuilder.setMatch(match) //
                .setInstructions(instructions) //
                .setPriority(Constants.XOS_APP_DFT_ARP_FLOW_PRIORITY) //
                .setBufferId(OFConstants.OFP_NO_BUFFER) //
                        //.setHardTimeout(flowHardTimeout)
                        //.setIdleTimeout(flowIdleTimeout)
                        //.setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                .setFlags(new FlowModFlags(false, false, false, false, false));

        // Do the database transaction, true is neccesary because we have to create the missing parent
        // node in the path.
        writeTransaction.merge(LogicalDatastoreType.OPERATIONAL, appFlowIID, appFlowBuilder.build(), true);
        final CheckedFuture writeTxResultFuture = writeTransaction.submit();
        Futures.addCallback(writeTxResultFuture, new FutureCallback() {
            @Override
            public void onSuccess(Object o) {
                LOG.debug("add app flow {} to mdsal store successful for tx :{}",
                        appFlowId.getValue(), writeTransaction.getIdentifier());
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("add app flow {] to mdsal write transaction {} failed",
                        appFlowId.getValue(), writeTransaction.getIdentifier(), throwable.getCause());
            }
        });
    }
}
