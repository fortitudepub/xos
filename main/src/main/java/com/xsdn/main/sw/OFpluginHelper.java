package com.xsdn.main.sw;

import com.google.common.collect.ImmutableList;
import com.xsdn.main.util.Constants;
import com.xsdn.main.util.OFutils;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.sdn._switch.UserGroup;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.Groups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fortitude on 15-10-11.
 * This class is used for some class naming conflict between our user/app flow with of sal flow.
 */
public class OFpluginHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SdnSwitchActor.class);

    SalFlowService salFlowService;
    SalGroupService salGroupService;

    public OFpluginHelper(SalFlowService salFlowService, SalGroupService salGroupService) {
        this.salFlowService = salFlowService;
        this.salGroupService = salGroupService;
    }

    public void addFlow(String dpid, String flowName, int priority, Match match, Instructions instructions) {
        // Staring building flow.
        FlowBuilder flowBuilder = new FlowBuilder().setTableId(Constants.XOS_APP_DEFAULT_TABLE_ID).setFlowName(flowName);
        // use its own hash code for id.
        // TODO: copied from openflow plugin, we need to provide a persistent api for flow id generation
        // better use priroity+flow_id_in_that priority as we defined in our design document.
        flowBuilder.setId(new FlowId(Long.toString(flowBuilder.hashCode())));
        // Put our Instruction in a list of Instructions
        flowBuilder.setMatch(match) //
                .setInstructions(instructions) //
                .setPriority(priority) //
                .setBufferId(OFConstants.OFP_NO_BUFFER) //
                        //.setHardTimeout(flowHardTimeout)
                        //.setIdleTimeout(flowIdleTimeout)
                        //.setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                .setFlags(new FlowModFlags(false, false, false, false, false));

        // Send the request to OF switch through SalFlowService.
        InstanceIdentifier<Node> nodeIID =
                InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, new NodeKey(new NodeId(OFutils.BuildNodeIdUriByDpid(dpid)))).build();
        TableKey flowTableKey = new TableKey(Constants.XOS_APP_DEFAULT_TABLE_ID);
        InstanceIdentifier<Table> tableIID = nodeIID.builder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, flowTableKey)
                .build();
        FlowId flowId = new FlowId(flowName);
        FlowKey flowKey = new FlowKey(flowId);
        InstanceIdentifier<Flow> flowIID = tableIID.child(Flow.class, flowKey);
        AddFlowInputBuilder builder = new AddFlowInputBuilder(flowBuilder.build());
        builder.setNode(new NodeRef(nodeIID));
        builder.setFlowRef(new FlowRef(flowIID));
        builder.setFlowTable(new FlowTableRef(tableIID));
        builder.setTransactionUri(new Uri(flowBuilder.getId().getValue()));
        // TODO: by reading openflowplugin code, seems the returned future of salflowservice only make sure the
        // flow mod message is write to the of channel, it does not ensure the flow entries is installed in the
        // switch, we must implement a logic to check this. either by periodically timer or some other means.
        salFlowService.addFlow(builder.build());
    }

    public void deleteFlow(String dpid, String flowName, int priority, Match match) {
        // Staring building flow.
        FlowBuilder flowBuilder = new FlowBuilder().setTableId(Constants.XOS_APP_DEFAULT_TABLE_ID).setFlowName(flowName);
        // use its own hash code for id.
        // TODO: copied from openflow plugin, we need to provide a persistent api for flow id generation
        // better use priroity+flow_id_in_that priority as we defined in our design document.
        flowBuilder.setId(new FlowId(Long.toString(flowBuilder.hashCode())));
        // Put our Instruction in a list of Instructions
        flowBuilder.setMatch(match) //
                .setPriority(priority) //
                .setBufferId(OFConstants.OFP_NO_BUFFER) //
                        //.setHardTimeout(flowHardTimeout)
                        //.setIdleTimeout(flowIdleTimeout)
                        //.setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                .setFlags(new FlowModFlags(false, false, false, false, false));

        // Send the request to OF switch through SalFlowService.
        InstanceIdentifier<Node> nodeIID =
                InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, new NodeKey(new NodeId(OFutils.BuildNodeIdUriByDpid(dpid)))).build();
        TableKey flowTableKey = new TableKey(Constants.XOS_APP_DEFAULT_TABLE_ID);
        InstanceIdentifier<Table> tableIID = nodeIID.builder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, flowTableKey)
                .build();
        FlowId flowId = new FlowId(flowName);
        FlowKey flowKey = new FlowKey(flowId);
        InstanceIdentifier<Flow> flowIID = tableIID.child(Flow.class, flowKey);
        RemoveFlowInputBuilder builder = new RemoveFlowInputBuilder(flowBuilder.build());
        builder.setNode(new NodeRef(nodeIID));
        builder.setFlowRef(new FlowRef(flowIID));
        builder.setFlowTable(new FlowTableRef(tableIID));
        builder.setTransactionUri(new Uri(flowBuilder.getId().getValue()));
        // TODO: by reading openflowplugin code, seems the returned future of salflowservice only make sure the
        // flow mod message is write to the of channel, it does not ensure the flow entries is installed in the
        // switch, we must implement a logic to check this. either by periodically timer or some other means.
        salFlowService.removeFlow(builder.build());
    }

    public void addGroup(String dpid, UserGroup userGroup) {
        final AddGroupInputBuilder groupBuilder = new AddGroupInputBuilder(userGroup);

        InstanceIdentifier<Node> nodeIID =
                InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, new NodeKey(new NodeId(OFutils.BuildNodeIdUriByDpid(dpid)))).build();
        groupBuilder.setNode(new NodeRef(nodeIID));
        //builder.setGroupRef(new GroupRef(groupIID));
        groupBuilder.setTransactionUri(new Uri(groupBuilder.getGroupId().getValue().toString()));
        salGroupService.addGroup(groupBuilder.build());
    }

    public void deleteGroup(String dpid, UserGroup userGroup) {
        final RemoveGroupInputBuilder groupBuilder = new RemoveGroupInputBuilder(userGroup);

        InstanceIdentifier<Node> nodeIID =
                InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, new NodeKey(new NodeId(OFutils.BuildNodeIdUriByDpid(dpid)))).build();
        groupBuilder.setNode(new NodeRef(nodeIID));
        //builder.setGroupRef(new GroupRef(groupIID));
        groupBuilder.setTransactionUri(new Uri(groupBuilder.getGroupId().getValue().toString()));
        salGroupService.removeGroup(groupBuilder.build());
    }
}
