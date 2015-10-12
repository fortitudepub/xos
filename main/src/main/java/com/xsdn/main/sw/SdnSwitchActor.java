package com.xsdn.main.sw;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.xsdn.main.ha.XosAppStatusMgr;
import com.xsdn.main.packet.ArpPacketBuilder;
import com.xsdn.main.util.EtherAddress;
import com.xsdn.main.util.Ip4Network;
import com.xsdn.main.util.OFutils;
import com.xsdn.main.util.Constants;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.sdn._switch.UserFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.AiManagedSubnet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * Created by fortitude on 15-8-23.
 */
public class SdnSwitchActor extends UntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(SdnSwitchActor.class);
    private static final HashMap<Short, AiManagedSubnet> subnetMap = new HashMap(50); // TODO: 50 is a arbitrary number now.
    private PacketProcessingService packetProcessingService = null;
    private SalFlowService salFlowService = null;
    private DataBroker dataService = null;
    private String dpid;
    private NodeId nodeId;
    private int appStatus = XosAppStatusMgr.APP_STATUS_INVALID;
    private boolean deviceConnected = false;
    private OFpluginHelper ofpluginHelper= null;
    private MdsalHelper mdsalHelper = null;

    private SdnSwitchActor(final PacketProcessingService packetProcessingService,
                           final SalFlowService salFlowService,
                           final DataBroker dataService) {
        this.packetProcessingService = Preconditions.checkNotNull(packetProcessingService);
        this.salFlowService = salFlowService;
        this.dataService = dataService;
        this.ofpluginHelper = new OFpluginHelper(salFlowService);
        this.mdsalHelper = new MdsalHelper(dataService);
    }

    // Define messages which will be processed by this actor.
    static public class DpIdCreated {
        private final String dpId;

        public DpIdCreated(String dpId) {
            this.dpId = dpId;
        }

        public String getDpId() {
            return dpId;
        }
    }

    // The two message will be used to implement reconciliation logic by which
    // we will re-program the flow tables if the switch have rebooted or reconnect.
    static public class SwitchConnected {
        public SwitchConnected() {
        }
    }

    static public class SwitchDisconnected {
        public SwitchDisconnected() {
        }
    }

    static public class AppStatusUpdate {
        private int appStatus = XosAppStatusMgr.APP_STATUS_INVALID;

        public AppStatusUpdate(int status) {
            this.appStatus = status;
        }
    }

    static public class ProbeArpOnce {
        public ProbeArpOnce() {

        }
    }

    static public class ManagedSubnetUpdate {
        private AiManagedSubnet subnet;
        boolean delete;

        public ManagedSubnetUpdate(AiManagedSubnet subnet, boolean delete) {
            this.subnet = subnet;
            this.delete = delete;
        }
    }

    static public class ArpPacketIn {
        private NodeId nodeId;
        private RawPacket rawPkt;
        private ArpPacket pkt;

        public ArpPacketIn(NodeId nodeId, RawPacket rawPkt, ArpPacket pkt) {
            // Record node id for possible usage later.
            this.nodeId = nodeId;
            this.rawPkt = rawPkt;
            this.pkt = pkt;
        }
    }

    static public class UserFlowOp {
        private short op;
        private UserFlow userFlow;

        public UserFlowOp(short op, UserFlow userFlow) {
            this.op = op;
            this.userFlow = userFlow;
        }
    }

    public SdnSwitchActor() {
        // TODO:
        // 1. initialize runtime database
        // 2. start arp prober thread
        // 3. provide callback for extern events like pkt in
        // 4. implement master-slave decide logic
    }

    private void addDftArpFlows() {
        // Match.
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder()
                .setEthernetType(new EthernetTypeBuilder()
                        .setType(new EtherType(Long.valueOf(KnownEtherType.Arp.getIntValue()))).build());
        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatchBuilder.build());


        // Instrutions.
        ActionBuilder actionBuilder = new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new OutputActionCaseBuilder()
                        .setOutputAction(new OutputActionBuilder()
                                .setMaxLength(0xffff)
                                .setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
                                .build())
                        .build());
        org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match match = matchBuilder.build();
        List<Action> actions = new ArrayList<Action>();
        actions.add(actionBuilder.build());
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();
        InstructionBuilder applyActionsInstructionBuilder = new InstructionBuilder()
                .setOrder(0)
                .setInstruction(new ApplyActionsCaseBuilder()
                        .setApplyActions(applyActions)
                        .build());
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(applyActionsInstructionBuilder.build()));

        this.ofpluginHelper.addFlow(this.dpid, Constants.XOS_APP_DFT_ARP_FLOW_NAME,
                                    Constants.XOS_APP_DFT_ARP_FLOW_PRIORITY,
                                    matchBuilder.build(), instructionsBuilder.build());

        // Note: we need install default arp flow for both active and backup switch.

        // Action 2: store to our md sal datastore.

        this.mdsalHelper.storeAppFlow(this.nodeId, Constants.XOS_APP_DFT_ARP_FLOW_NAME,
                                      matchBuilder.build(), instructionsBuilder.build());

        LOG.info("Pushed init flow {} to the switch {}", "_XOS_DFT_ARP_0", this.dpid);
    }

    private void addInitFlows() {
        if (!deviceConnected) {
            LOG.info("Device is not connected, skip init flow provisioning");
            return;
        }

        addDftArpFlows();
    }

    private void processAppStatusUpdate(int status) {
        // Dpid is not set yet, just record app status.
        if (null == this.dpid) {
            this.appStatus = status;
            return;
        }

        if (this.appStatus != status) {
            if (status == XosAppStatusMgr.APP_STATUS_ACTIVE)
            {
                // We are now running active controller.
                // There will be quite complicate logic, may be we should spawn a seperate actor to handle all
                // the sub task.
                // Basically we need handle the following tasks:
                // 1. INVALID->ACTIVE
                //    1.1 init state, clear all flow of the managed switch.
                //    1.2 update controller role to master instead of equal.
                //    1.2 install default flow and store the flow in to the xos operati.
                // 2. BACKUP->ACTIVE
                //    2.1
                // 3. ACTIVE->BACKUP
                //    3.1 ... TBD

                // Case 1, INVALID->ACTIVE.
                if (this.appStatus == XosAppStatusMgr.APP_STATUS_INVALID) {
                    this.addInitFlows();
                }
            }
            this.appStatus = status;

            LOG.info("Update sdn switch actor to status {}", this.appStatus);
        } else {
            LOG.error("Duplicate status recevied {} for sdnswitch actor {}", this.appStatus, this.dpid);
        }
    }

    private void processDpid(String dpid) {
        LOG.info("SdnSwitch actor received dpid created, dpid is " + dpid);
        if (null == this.dpid) {
            this.dpid = dpid;
            this.nodeId = new NodeId(OFutils.BuildNodeIdUriByDpid(this.dpid));
            if (this.appStatus == XosAppStatusMgr.APP_STATUS_ACTIVE) {
                // TODO: init the switch since we have the dpid configured now.
                this.addInitFlows();
            }
        } else {
            // TODO: handle update.
        }
    }

    private void processSwitchConnected() {
        LOG.info("SdnSwitch actor received switch connected event, dpid is " + dpid);

        this.deviceConnected = true;

        // TODO: do the reconciliation logic, the code here is just a test to do the event driven logic.
        if ((null != this.dpid) && (this.appStatus == XosAppStatusMgr.APP_STATUS_ACTIVE)) {
            this.addInitFlows();
        }
    }

    private void processSwitchDisonnected() {
        LOG.info("SdnSwitch actor received switch disconnected event, dpid is " + dpid);

        this.deviceConnected = false;
    }

    private void processSubnetUpdate(ManagedSubnetUpdate subnetUpdate)  {
        // TODO: this code need to be refactored because I only want to extract the subnet information
        // more santity check need to be done.
        // And also, we should build a auxiliary map that use the virtual gateway ip as index to help
        // do the arp proxy.
        // We should not try to read data from the data store directly because the transaction read is slow.
        if (!subnetUpdate.delete) {
            this.subnetMap.put(subnetUpdate.subnet.getKey().getSubnetId(), subnetUpdate.subnet);
        } else {
            this.subnetMap.remove(subnetUpdate.subnet.getKey().getSubnetId());
            this.subnetMap.put(subnetUpdate.subnet.getKey().getSubnetId(), subnetUpdate.subnet);
        }
    }

    private boolean processArpReqForVGW(ArpPacketIn pktIn) {
        String dip = pktIn.pkt.getDestinationProtocolAddress();
        Ipv4Address dIPv4 = new Ipv4Address(dip);
        boolean isVGWARP = false;
        MacAddress vMAC = null;

        // Locate whether this arp request is for
        Iterator<Entry<Short, AiManagedSubnet>> it = this.subnetMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Short, AiManagedSubnet> entry = it.next();
            AiManagedSubnet subnet = entry.getValue();
            if ((subnet.getVirtualGateway() != null) && (subnet.getVirtualGateway().getVirtualGatewayIp() != null)
                    && (subnet.getVirtualGateway().getVirtualGatewayIp().equals(dIPv4)))
            {
                isVGWARP = true;
                vMAC = subnet.getVirtualGateway().getVirtualGatewayMac();
                break;
            }
        }

        if (false == isVGWARP) {
            return false;
        }

        if (pktIn.pkt.getOperation() != KnownOperation.Request) {
            LOG.error("Received bad arp packet for the vGW ip " + dip);
            return true; // It should be handled by us, but it's not request.
        }

        if (vMAC != null) {
            TransmitPacketInput arpReply;

            try {
                // Construct ARP Reply for virtual GW.
                // Virtual GW IP will be used as SPA, Virtual GW MAC will be ethernet source and SHA.
                Ip4Network spa = new Ip4Network(dIPv4.getValue());
                Ip4Network tpa = new Ip4Network(pktIn.pkt.getSourceProtocolAddress());
                // No VLAN in ai deployment.
                Ethernet ether = new ArpPacketBuilder()
                        .setAsReply()
                        .setSenderProtocolAddress(spa)
                        .build(new EtherAddress(vMAC.getValue()),
                                new EtherAddress(pktIn.pkt.getSourceHardwareAddress()),
                                tpa);

                InstanceIdentifier<Node> node = pktIn.rawPkt.getIngress().getValue().firstIdentifierOf(Node.class);

                arpReply = new TransmitPacketInputBuilder()
                        .setPayload(ether.serialize())
                        .setNode(new NodeRef(node))
                        .setEgress(pktIn.rawPkt.getIngress())
                        .build();
            } catch (Exception e) {
                LOG.error("Failed to build arp reply for vgw " + dIPv4.getValue() +
                        "with request from " + pktIn.pkt.getSourceProtocolAddress());
                return true;
            }

            packetProcessingService.transmitPacket(arpReply);
        }

        return true;
    }

    private void processArp(ArpPacketIn pktIn)  {
        boolean vgwHandled = false;
        // Handle arp request for vmac
        vgwHandled = processArpReqForVGW(pktIn);
        if (vgwHandled) {
            return;
        }


        // TODO: add arp snooping logic (by WEIZJ).
    }

    private void processUserFlowOp(UserFlowOp userFlowOp) {
        if (userFlowOp.op == OFutils.FLOW_ADD) {
            UserFlow userFlow = userFlowOp.userFlow;

            this.ofpluginHelper.addFlow(this.dpid, userFlow.getFlowName(), userFlow.getPriority().intValue(),
                    userFlow.getMatch(), userFlow.getInstructions());
        } else if (userFlowOp.op == OFutils.FLOW_DELETE) {
            UserFlow userFlow = userFlowOp.userFlow;

            this.ofpluginHelper.deleteFlow(this.dpid, userFlow.getFlowName(), userFlow.getPriority().intValue(),
                    userFlow.getMatch());
        }
    }

    public void onReceive(Object message) throws Exception {
        if (message instanceof DpIdCreated) {
            processDpid(((DpIdCreated) (message)).getDpId());
        } else if (message instanceof SwitchConnected) {
            processSwitchConnected();
        } else if (message instanceof SwitchDisconnected) {
            processSwitchDisonnected();
        } else if (message instanceof AppStatusUpdate) {
            processAppStatusUpdate(((AppStatusUpdate) (message)).appStatus);
        } else if (message instanceof ProbeArpOnce) {
            LOG.info("TO BE IMPLEMENTED: ARP PROBE");
        } else if (message instanceof  ManagedSubnetUpdate) {
            processSubnetUpdate((ManagedSubnetUpdate)message);
        } else if (message instanceof  ArpPacketIn) {
            processArp((ArpPacketIn)message);
        } else if (message instanceof UserFlowOp) {
            processUserFlowOp((UserFlowOp)message);
        }
        else {
            unhandled(message);
        }
    }

    public static Props props(final PacketProcessingService packetProcessingService,
                              final SalFlowService salFlowService,
                              final DataBroker dataService) {
        return Props.create(new SdnSwitchActorCreator(packetProcessingService, salFlowService, dataService));
    }

    private static final class SdnSwitchActorCreator implements Creator<SdnSwitchActor> {
        private final PacketProcessingService packetProcessingService;
        private final SalFlowService salFlowService;
        private final DataBroker dataService;

        SdnSwitchActorCreator(final PacketProcessingService packetProcessingService,
                              final SalFlowService salFlowService,
                              final DataBroker dataService) {
            this.packetProcessingService = Preconditions.checkNotNull(packetProcessingService);
            this.salFlowService = Preconditions.checkNotNull(salFlowService);
            this.dataService = Preconditions.checkNotNull(dataService);
        }

        @Override
        public SdnSwitchActor create() {
            return new SdnSwitchActor(packetProcessingService, salFlowService, dataService);
        }
    }
}
