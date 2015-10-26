package com.xsdn.main.sw;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.xsdn.main.ha.XosAppStatusMgr;
import com.xsdn.main.packet.ArpPacketBuilder;
import com.xsdn.main.util.EtherAddress;
import com.xsdn.main.util.Ip4Network;
import com.xsdn.main.util.OFutils;
import com.xsdn.main.util.Constants;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.action.rev150203.action.grouping.action.choice.set.field._case.SetFieldActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.sdn._switch.UserFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.sdn._switch.UserGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.AiManagedSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.managed.subnet.SubnetHost;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
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
    private SalGroupService salGroupService = null;
    private SalFlowService salFlowService = null;
    private DataBroker dataService = null;
    private String dpid;
    private NodeId nodeId;
    private int appStatus = XosAppStatusMgr.APP_STATUS_INVALID;
    private boolean deviceConnected = false;
    private MacAddress vGMAC = new MacAddress(Constants.INVALID_MAC_ADDRESS);
    private Ipv4Address edgeRouterInterfaceIp = new Ipv4Address(Constants.INVALID_IP_ADDRESS);
    private Ipv4Address quaggaInterfaceIp = new Ipv4Address(Constants.INVALID_IP_ADDRESS);

    private OFpluginHelper ofpluginHelper = null;
    private MdsalHelper mdsalHelper = null;

    // Note: WZJ, for store host ip/mac mapping
    private ConcurrentMap<Short, List> subnetTracer = new ConcurrentHashMap<>();
    private ConcurrentMap<String, HostInfo> hostTracer = new ConcurrentHashMap<>();

    private SdnSwitchActor(final PacketProcessingService packetProcessingService,
                           final SalFlowService salFlowService,
                           final SalGroupService salGroupService,
                           final DataBroker dataService) {
        this.packetProcessingService = Preconditions.checkNotNull(packetProcessingService);
        this.salGroupService = salGroupService;
        this.salFlowService = salFlowService;
        this.dataService = dataService;
        this.ofpluginHelper = new OFpluginHelper(salFlowService, salGroupService);
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

    static public class NotifyDefaultRoute {
        public NotifyDefaultRoute() {

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
        // iid is not used currently since we will construct it to inventory iid.
        private InstanceIdentifier<UserFlow> iid;

        public UserFlowOp(short op, UserFlow userFlow, InstanceIdentifier<UserFlow> iid) {
            this.op = op;
            this.userFlow = userFlow;
            this.iid = iid;
        }
    }

    static public class UserGroupOp {
        private short op;
        private UserGroup userGroup;
        // iid is not used currently since we will construct it to inventory iid.
        private InstanceIdentifier<UserGroup> iid;

        public UserGroupOp(short op, UserGroup userGroup, InstanceIdentifier<UserGroup> iid) {
            this.op = op;
            this.userGroup = userGroup;
            this.iid = iid;
        }
    }

    static public class VirtualGatewayMacUpdate {
        private MacAddress address;

        public VirtualGatewayMacUpdate(MacAddress address) {
            this.address = address;
        }
    }


    static public class QuaggaInterfaceIpUpdate {
        private Ipv4Address address;

        public QuaggaInterfaceIpUpdate(Ipv4Address address) {
            this.address = address;
        }
    }


    static public class EdgeRouterInterfaceIpUpdate {
        private Ipv4Address address;

        public EdgeRouterInterfaceIpUpdate(Ipv4Address address) {
            this.address = address;
        }
    }

    public SdnSwitchActor() {
        // TODO:
        // 1. initialize runtime database
        // 2. start arp prober thread
        // 3. provide callback for extern events like pkt in
        // 4. implement master-slave decide logic
    }

    /**
     * Note: WZJ, Store the host mac and ingress
     */
    public class HostInfo {
        private MacAddress mac;
        private NodeConnectorRef ingress;

        public HostInfo(MacAddress mac, NodeConnectorRef ingress) {
            this.mac = mac;
            this.ingress = ingress;
        }

        public NodeConnectorRef getIngress() {
            return this.ingress;
        }

        public MacAddress getMac() {
            return this.mac;
        }
    }

    /**
     * Note: WZJ, add l2 forward flows
     */
    private void addL2ForwardFlows(String ipAddr, MacAddress destMac, NodeConnectorRef destPort) {
        if (destMac == null || destPort == null) {
            LOG.info("Add l2 forward flow error.");
            return;
        }

        // Match.
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder()
                .setEthernetDestination(new EthernetDestinationBuilder().setAddress(destMac).build());
        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatchBuilder.build());
        Match match = matchBuilder.build();

        // Actions.
        Uri destPortUri = destPort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
        ActionBuilder actionBuilder = new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new OutputActionCaseBuilder()
                        .setOutputAction(new OutputActionBuilder().setMaxLength(0xffff)
                                .setOutputNodeConnector(destPortUri)
                                .build()).build());

        List<Action> actions = new ArrayList<>();
        actions.add(actionBuilder.build());

        ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();

        InstructionBuilder applyActionsInstructionBuilder = new InstructionBuilder()
                .setOrder(0)
                .setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions).build());

        InstructionsBuilder instructionsBuilder = new InstructionsBuilder()
                .setInstruction(ImmutableList.of(applyActionsInstructionBuilder.build()));

        this.ofpluginHelper.addFlow(this.dpid, Constants.XOS_L2_FORWARD_FLOW_NAME,
                Constants.XOS_L2_FORWARD_FLOW_PRIORITY,
                match , instructionsBuilder.build());

        // Action 2: store to our md sal datastore.
        this.mdsalHelper.storeAppFlow(this.nodeId, Constants.XOS_L2_FORWARD_FLOW_NAME + ipAddr,
                match, instructionsBuilder.build());

        LOG.info("Pushed l2 flow {} to the switch {}", "_XOS_L2_FORWARD", this.dpid);
    }

    private void addDftArpFlows() {
        // Match.
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder()
                .setEthernetType(new EthernetTypeBuilder()
                        .setType(new EtherType(Long.valueOf(KnownEtherType.Arp.getIntValue()))).build());
        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatchBuilder.build());


        // Actions.
        ActionBuilder actionBuilder = new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new OutputActionCaseBuilder()
                        .setOutputAction(new OutputActionBuilder()
                                .setMaxLength(0xffff)
                                .setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
                                .build())
                        .build());
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

        LOG.info("Pushed init flow {} to the switch {}", Constants.XOS_APP_DFT_ARP_FLOW_NAME, this.dpid);
    }

    private void addInitFlows() {
        if (!deviceConnected) {
            LOG.info("Device is not connected, skip init flow provisioning");
            return;
        }

        this.addDftArpFlows();
    }

    private void rebuildActorStateWhenActive() {
        // ZDY_NOTE: if we become active again, we need at least do the following stuffs
        // 1. check predefined app flow have been write to the md-sal data store, if not
        //    we should reinstall it.
        // 2. clear local configuration caches like hostinfo and etc. reload the data from
        //    md-sal configuration store and build cache again.
        // 3. after the above event have been finished, we switch into the fully functional status
        //    and before these stuffs have been finished, we should not react exterior event like
        //    packet in and etc.
        // if 1/2 need asynchronous processing, we need add a hold mechanism to this actor to
        // hold all the things we do not want to occur during this time.

        // @2015.10.26

        // STEP1:

        // STEP2:

        // FINISHED...

        if (this.appStatus == XosAppStatusMgr.APP_STATUS_INVALID) {
            this.addInitFlows();
        }
    }

    private void processAppStatusUpdate(int status) {
        // Dpid is not set yet, just record app status.
        if (null == this.dpid) {
            this.appStatus = status;
            return;
        }

        if (this.appStatus != status) {
            if (status == XosAppStatusMgr.APP_STATUS_ACTIVE) {
                this.rebuildActorStateWhenActive();
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

        // ZDY_NOTE:
        // for HA, after the switch have been connected, we should do a synchronous check to ensure
        // all the app flows/user flows have been successfully downloaded to the switch, if not,
        // we should download it again.
        // This can be done using flow stats multipart message.
        // 2015.10.26

        // TODO: do the reconciliation logic, the code here is just a test to do the event driven logic.
        if ((null != this.dpid) && (this.appStatus == XosAppStatusMgr.APP_STATUS_ACTIVE)) {
            this.addInitFlows();
        }
    }

    private void processSwitchDisonnected() {
        LOG.info("SdnSwitch actor received switch disconnected event, dpid is " + dpid);

        this.deviceConnected = false;
    }

    private void processSubnetUpdate(ManagedSubnetUpdate subnetUpdate) {
        if (this.appStatus != XosAppStatusMgr.APP_STATUS_ACTIVE) {
            return;
        }

        // TODO: this code need to be refactored because I only want to extract the subnet information
        // more santity check need to be done.
        // And also, we should build a auxiliary map that use the virtual gateway ip as index to help
        // do the arp proxy.
        // We should not try to read data from the data store directly because the transaction read is slow.
        if (!subnetUpdate.delete) {
            this.subnetMap.put(subnetUpdate.subnet.getKey().getSubnetId(), subnetUpdate.subnet);
            /* Note: WZJ, add or update host info */
            handleHostUpdate(subnetUpdate.subnet.getKey().getSubnetId(), subnetUpdate.subnet, false);
        } else {
            this.subnetMap.remove(subnetUpdate.subnet.getKey().getSubnetId());
            this.subnetMap.put(subnetUpdate.subnet.getKey().getSubnetId(), subnetUpdate.subnet);
            /* Note: WZJ, delete host info */
            handleHostUpdate(subnetUpdate.subnet.getKey().getSubnetId(), subnetUpdate.subnet, true);
        }
    }

    private boolean processArpReqForVGW(ArpPacketIn pktIn) {
        String dip = pktIn.pkt.getDestinationProtocolAddress();
        Ipv4Address dIPv4 = new Ipv4Address(dip);
        boolean isVGWARP = false;

        // Locate whether this arp request is for
        Iterator<Entry<Short, AiManagedSubnet>> it = this.subnetMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Short, AiManagedSubnet> entry = it.next();
            AiManagedSubnet subnet = entry.getValue();
            if ((subnet.getVirtualGateway() != null) && (subnet.getVirtualGateway().getVirtualGatewayIp() != null)
                    && (subnet.getVirtualGateway().getVirtualGatewayIp().equals(dIPv4))) {
                isVGWARP = true;
                break;
            }
        }

        if (false == isVGWARP) {
            return false;
        }

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
                    .build(new EtherAddress(this.vGMAC.getValue()),
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

        return true;
    }

    /**
     * Note: WZJ, process arp proxy for hosts
     */
    private void processArpReqForHosts(ArpPacketIn pktIn) {
        String dip = pktIn.pkt.getDestinationProtocolAddress();
        Ipv4Address dIPv4 = new Ipv4Address(dip);

        if (!this.hostTracer.containsKey(dip)) {
            LOG.info("Receive host arp request, but destination host is unknow.");
            return;
        }

        LOG.info("Receive host arp request, destination host is found.");

        TransmitPacketInput arpReply;
        MacAddress destMac = this.hostTracer.get(dip).getMac();

        try {
            /* Construct ARP Reply for host */
            /* Virtual GW IP will be used as SPA, Virtual GW MAC will be ethernet source and SHA. */
            Ip4Network spa = new Ip4Network(dIPv4.getValue());
            Ip4Network tpa = new Ip4Network(pktIn.pkt.getSourceProtocolAddress());
            /* No VLAN in ai deployment. */
            Ethernet ether = new ArpPacketBuilder().setAsReply()
                    .setSenderProtocolAddress(spa)
                    .build(new EtherAddress(destMac.getValue()),
                           new EtherAddress(pktIn.pkt.getSourceHardwareAddress()),
                           tpa);

            InstanceIdentifier<Node> node = pktIn.rawPkt.getIngress().getValue().firstIdentifierOf(Node.class);

            arpReply = new TransmitPacketInputBuilder()
                    .setPayload(ether.serialize())
                    .setNode(new NodeRef(node))
                    .setEgress(pktIn.rawPkt.getIngress())
                    .build();
        } catch (Exception e) {
            LOG.error("Failed to build arp reply for host " + dIPv4.getValue() +
                    "with request from " + pktIn.pkt.getSourceProtocolAddress());
            return;
        }

        packetProcessingService.transmitPacket(arpReply);

        return;
    }

    private void processArp(ArpPacketIn pktIn) {
        if (this.appStatus != XosAppStatusMgr.APP_STATUS_ACTIVE) {
            return;
        }

        if (vGMAC.equals(Constants.INVALID_MAC_ADDRESS)) {
            LOG.error("Virtual Gateway MAC address is not configured, no need to handle arp packet");
            return;
        }

        if (pktIn.pkt.getOperation() == KnownOperation.Request) {
            // Handle arp request for vmac
            boolean vgwHandled = false;
            vgwHandled = processArpReqForVGW(pktIn);
            if (vgwHandled) {
                return;
            }

            // Handle arp request for hosts
            processArpReqForHosts(pktIn);

            return;
        }

        /* Note: WZJ, if arp replay, should handle by us */
        if (pktIn.pkt.getOperation() == KnownOperation.Reply) {
            if (!this.vGMAC.getValue().equals(pktIn.pkt.getDestinationHardwareAddress())) {
                /* Not handle arp reply which macda is not vGMAC */
                return;
            }

            Ipv4Address dIPv4 = new Ipv4Address(pktIn.pkt.getDestinationProtocolAddress());
            Short subnetId = null;

            Iterator<Entry<Short, AiManagedSubnet>> it = this.subnetMap.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Short, AiManagedSubnet> entry = it.next();
                AiManagedSubnet subnet = entry.getValue();
                if (subnet.getVirtualGateway() != null &&
                    subnet.getVirtualGateway().getVirtualGatewayIp().equals(dIPv4)) {
                    subnetId = entry.getKey();
                    break;
                }
            }

            if (subnetId == null) {
                /* Not handle arp reply which destip is not vGIP */
                return;
            }

            notifyHostTracerToAddHost(subnetId, pktIn);
        }

        return;
    }

    /**
     * Note: WZJ, process arp probe
     */
    private void processArpProbe() {
        if (this.appStatus != XosAppStatusMgr.APP_STATUS_ACTIVE) {
            return;
        }

        for (Short key : this.subnetTracer.keySet()) {
            sendHostsArpProbe(key, this.subnetTracer.get(key));
        }

        return;
    }

    private void processUserFlowOp(UserFlowOp userFlowOp) {
        if (this.appStatus != XosAppStatusMgr.APP_STATUS_ACTIVE) {
            return;
        }

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

    private void processUserGroupOp(UserGroupOp userGroupOp) {
        if (this.appStatus != XosAppStatusMgr.APP_STATUS_ACTIVE) {
            return;
        }

        if (userGroupOp.op == OFutils.GROUP_ADD) {
            UserGroup userGroup = userGroupOp.userGroup;

            this.ofpluginHelper.addGroup(this.dpid, userGroup);
        } else if (userGroupOp.op == OFutils.GROUP_DELETE) {
            UserGroup userGroup = userGroupOp.userGroup;

            this.ofpluginHelper.deleteGroup(this.dpid, userGroup);
        }
    }

    /**
     * Note: WZJ, handle add/update/delete host ip address
     */
    private void handleHostUpdate(Short subnetId, AiManagedSubnet subnetInfo, boolean isDel) {
        if (subnetId == null || subnetInfo == null) {
            LOG.error("Handle host update error.");
            return;
        }

        List<String> addHostsIp = new ArrayList<>();

        if (!this.subnetTracer.containsKey(subnetId)) {
            /* Handle add new subnet */
            subnetTracer.put(subnetId, addHostsIp);

            /* Get all of hosts in this subnet */
            List<SubnetHost> listHosts = subnetInfo.getSubnetHost();
            if (listHosts == null || listHosts.isEmpty()) {
                LOG.info("Subnet: " + subnetId + " no need to add hosts.");
                return;
            }

            /* If hosts exist, add to database */
            Iterator itHost = listHosts.iterator();
            while(itHost.hasNext()) {
                SubnetHost hostInfo = (SubnetHost)itHost.next();
                Ipv4Address hostIp = hostInfo.getManagedHostIp();
                addHostsIp.add(hostIp.getValue());
                LOG.info("Subnet: " + subnetId + " add host :" + hostIp.getValue());
            }
        }
        else {
            if (isDel) {
                /* Delete host info from this subnet */
                if (!this.subnetTracer.containsKey(subnetId)) {
                    LOG.info("Subnet: " + subnetId + " delete host error.");
                    return;
                }

                /* Get old host list from this subnet */
                List<String> listIp = this.subnetTracer.get(subnetId);
                /* Get delete info in this subnet */
                List<SubnetHost> listHosts = subnetInfo.getSubnetHost();
                if (listHosts == null || listHosts.isEmpty()) {
                    LOG.info("Subnet: " + subnetId + " no need to delete hosts.");
                    return;
                }

                Iterator itHost = listHosts.iterator();
                while(itHost.hasNext()) {
                    SubnetHost hostInfo = (SubnetHost)itHost.next();
                    Ipv4Address hostIp = hostInfo.getManagedHostIp();
                    if (listIp.contains(hostIp.getValue())) {
                        listIp.remove(hostIp.getValue());
                        notifyHostTracerToDelHost(hostIp.getValue());
                        LOG.info("Subnet: " + subnetId + " delete host :" + hostIp.getValue());
                    }
                }
            }
            else {
                /* Add/update host info from this subnet */
                if (!this.subnetTracer.containsKey(subnetId)) {
                    LOG.info("Subnet: " + subnetId + " add host error.");
                    return;
                }

                /* Get old host list from this subnet */
                List<String> listIp = this.subnetTracer.get(subnetId);
                /* Get add info from this subnet */
                List<SubnetHost> listHosts = subnetInfo.getSubnetHost();
                if (listHosts == null || listHosts.isEmpty()) {
                    LOG.info("Subnet: " + subnetId + " no need to update hosts.");
                    return;
                }

                Iterator itHost = listHosts.iterator();
                while(itHost.hasNext()) {
                    SubnetHost hostInfo = (SubnetHost)itHost.next();
                    Ipv4Address hostIp = hostInfo.getManagedHostIp();
                    listIp.add(hostIp.getValue());
                    addHostsIp.add(hostIp.getValue());
                    LOG.info("Subnet: " + subnetId + " add host :" + hostIp.getValue());
                }
            }
        }

        if (!addHostsIp.isEmpty()) {
            /* Send arp probe for new hosts */
            sendHostsArpProbe(subnetId, addHostsIp);
        }

        return;
    }

    /**
     * Note: WZJ, handle add host info
     */
    private void notifyHostTracerToAddHost(Short subnetId, ArpPacketIn pktIn) {
        if (pktIn == null) {
            return;
        }

        if (this.subnetTracer.containsKey(subnetId)) {
            List<String> listHosts = this.subnetTracer.get(subnetId);

            if (!listHosts.contains(pktIn.pkt.getSourceProtocolAddress())) {
                LOG.info("host:" + pktIn.pkt.getSourceProtocolAddress() + " not int subnet:" + subnetId);
                return;
            }

            MacAddress sMac = new MacAddress(pktIn.pkt.getSourceHardwareAddress());
            String sIp =  pktIn.pkt.getSourceProtocolAddress();
            NodeConnectorRef ingress = pktIn.rawPkt.getIngress();

            /* If this host exist before, update it */
            if (this.hostTracer.containsKey(sIp)) {
                HostInfo oldInfo = this.hostTracer.get(sIp);
                if (oldInfo.getMac().equals(sMac)) {
                    return;
                }

                this.hostTracer.remove(sIp);
            }

            HostInfo hostInfo = new HostInfo(sMac, ingress);
            this.hostTracer.put(sIp, hostInfo);
            LOG.info("host: " + sIp + " add host information");

            addL2ForwardFlows(sIp, sMac, ingress);

            /* If this host is edgeRouter or quagga, notify to add default route flows */
            if (sIp.equals(this.edgeRouterInterfaceIp.getValue()) ||
                sIp.equals(this.quaggaInterfaceIp.getValue()) ) {
                LOG.info("Tell route module add default route");
                this.self().tell(new NotifyDefaultRoute(), null);
            }
        }

        return;
    }

    /**
     * Note: WZJ, handle delete host info
     */
    private void notifyHostTracerToDelHost(String ipAddress) {
        if (ipAddress == null) {
            return;
        }

        if (this.hostTracer.containsKey(ipAddress)) {
            this.hostTracer.remove(ipAddress);
            LOG.info("host: " + ipAddress + " delete host information");
        }
    }

    /**
     * Note: WZJ, get host mac and ingress port by ip address
     */
    private HostInfo getHostInfoByIpAddr(Ipv4Address ipAddress) {
        if (ipAddress == null) {
            return null;
        }

        if (this.hostTracer.containsKey(ipAddress.getValue())) {
            return this.hostTracer.get(ipAddress.getValue());
        }

        return null;
    }

    /**
     * Note: WZJ, send arp request for arp probe
     */
    private void sendHostsArpProbe(Short subnetId, List<String> listIp) {
        if (subnetId == null || listIp == null) {
            LOG.error("Send hosts arp probe error.");
            return;
        }

        if (listIp.isEmpty() || !this.subnetMap.containsKey(subnetId)) {
            LOG.info("Subnet: " + subnetId + " no need to send arp probe.");
            return;
        }

        if (this.subnetMap.get(subnetId).getVirtualGateway() == null){
            LOG.info("Subnet: " + subnetId + " virtual gateway configuration is not created.");
            return;
        }

        Ipv4Address vIP = this.subnetMap.get(subnetId).getVirtualGateway().getVirtualGatewayIp();
        if (vIP == null) {
            LOG.info("Subnet: " + subnetId + " virtual gateway ip is not configured.");
            return;
        }

        if (this.vGMAC.equals(Constants.INVALID_MAC_ADDRESS)) {
            LOG.error("Virtual gateway mac is not configured.");
            return;
        }

        /* Construct arp request packet and send arp probe */
        for(int i = 0; i < listIp.size(); i++)
        {
            String hostIp = listIp.get(i);
            TransmitPacketInput arpRequest;

            try {
                /* Subnet VIP will be used as source SPA,
                   VGMAC will be used as ethernet source and SHA. */
                Ip4Network spa = new Ip4Network(vIP.getValue());
                Ip4Network tpa = new Ip4Network(hostIp);

                /* ARP packet build */
                Ethernet ether = new ArpPacketBuilder().setAsRequest()
                    .setSenderProtocolAddress(spa)
                        .build(new EtherAddress(this.vGMAC.getValue()), EtherAddress.BROADCAST, tpa);

                String swId = OFutils.BuildNodeIdUriByDpid(this.dpid);
                NodeKey swNodeKey = new NodeKey(new NodeId(swId));
                InstanceIdentifier<Node> swNode = InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, swNodeKey).build();

                String outPutAll = OFutils.BuildNodeIdUriForOutPutAll(this.dpid);
                NodeKey portNodeKey = new NodeKey(new NodeId(outPutAll));
                InstanceIdentifier<NodeConnector> portNode = InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, portNodeKey)
                        .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(outPutAll))).build();

                arpRequest = new TransmitPacketInputBuilder()
                             .setPayload(ether.serialize())
                             .setNode(new NodeRef(swNode))
                             .setEgress(new NodeConnectorRef(portNode))
                             .build();
            } catch (Exception e) {
                LOG.error("Failed to build arp request for host: " + hostIp);
                return;
            }

            packetProcessingService.transmitPacket(arpRequest);
        }

        return;
    }

    // TODO: pending zhijun's mac spoofing impl
    // the flow should be:
    // match: ethertype(ipv4)+dst_mac(the virtual gw mac, if the mac is not present, should not install the flow)
    // action: mod_eth_src(quagga interface mac),mod_eth_dst(edge router interface mac),output to uplink interface \
    // (edge router interface ip port learned by mac snooping)
    // in the current topology, the uplink can only be the link between the SDN switch and the L2 switch.
    // So the input of this function is:
    // 1. src_mac (quagga interface mac)
    // 2. dst_mac (edge router interface mac)
    // 3. output_of_port (port where edge router interface mac learned)
    // 4. vmac of this sdn switch.
    private void addDftRouteFlow(MacAddress vGMAC, MacAddress srcMAC, MacAddress dstMAC, NodeConnectorRef output) {
        // Match: IPV4+VGMAC.
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder()
                .setEthernetType(new EthernetTypeBuilder()
                        .setType(new EtherType(Long.valueOf(KnownEtherType.Ipv4.getIntValue()))).build())
                .setEthernetDestination(new EthernetDestinationBuilder().setAddress(vGMAC).build());
        MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatchBuilder.build());


        // Actions:mod_dl_src,mod_dl_dst,output
        // NOTE: we only support OPENFLOW 1.3, so we use set_field action.
        // and we should not use dec_ip_ttl because most of hw switch does not support it.

        /* We do not need to create setfield aciotion explicitly, flowconverter in openflowplugin
           will do that for me, leave these code for future reference.

           See flowconverter class in openflowplugin.
        List<MatchEntry> setFieldMatchEntries = new ArrayList<MatchEntry>();
        MatchEntryBuilder srcMatchEntryBuilder = new MatchEntryBuilder();
        srcMatchEntryBuilder.setOxmClass(OpenflowBasicClass.class)
                            .setOxmMatchField(EthSrc.class)
                            .setHasMask(false)
                            .setMatchEntryValue(new EthSrcCaseBuilder()
                                    .setEthSrc(new EthSrcBuilder()
                                            .setMacAddress(srcMAC)
                                            .build())
                                    .build());
        MatchEntryBuilder dstMatchEntryBuilder = new MatchEntryBuilder();
        dstMatchEntryBuilder.setOxmClass(OpenflowBasicClass.class)
                .setOxmMatchField(EthDst.class)
                .setHasMask(false)
                .setMatchEntryValue(new EthDstCaseBuilder()
                        .setEthDst(new EthDstBuilder()
                                .setMacAddress(dstMAC)
                                .build())
                        .build());
        setFieldMatchEntries.add(srcMatchEntryBuilder.build());
        setFieldMatchEntries.add(dstMatchEntryBuilder.build());
        */

        ActionBuilder modDlSrcActionBuilder = new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new SetDlSrcActionCaseBuilder()
                        .setSetDlSrcAction(new SetDlSrcActionBuilder()
                                .setAddress(srcMAC)
                                .build())
                        .build());
        ActionBuilder modDlDstActionBuilder = new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new SetDlDstActionCaseBuilder()
                        .setSetDlDstAction(new SetDlDstActionBuilder()
                                .setAddress(dstMAC)
                                .build())
                        .build());
        Uri destPortUri = output.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
        ActionBuilder outputActionBuilder = new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new OutputActionCaseBuilder()
                        .setOutputAction(new OutputActionBuilder()
                                .setMaxLength(0xffff)
                                .setOutputNodeConnector(destPortUri)
                                .build())
                        .build());

        List<Action> actions = new ArrayList<Action>();
        actions.add(modDlSrcActionBuilder.build());
        actions.add(modDlDstActionBuilder.build());
        actions.add(outputActionBuilder.build());
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();
        InstructionBuilder applyActionsInstructionBuilder = new InstructionBuilder()
                .setOrder(0)
                .setInstruction(new ApplyActionsCaseBuilder()
                        .setApplyActions(applyActions)
                        .build());
        InstructionsBuilder instructionsBuilder = new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(applyActionsInstructionBuilder.build()));

        this.ofpluginHelper.addFlow(this.dpid, Constants.XOS_APP_DFT_ROUTE_FLOW_NAME,
                Constants.XOS_APP_DFT_ROUTE_FLOW_PRIORITY,
                matchBuilder.build(), instructionsBuilder.build());

        // Note: we need install default arp flow for both active and backup switch.

        // Action 2: store to our md sal datastore.

        this.mdsalHelper.storeAppFlow(this.nodeId, Constants.XOS_APP_DFT_ROUTE_FLOW_NAME,
                matchBuilder.build(), instructionsBuilder.build());

        LOG.info("Pushed init flow {} to the switch {}", Constants.XOS_APP_DFT_ROUTE_FLOW_NAME, this.dpid);
    }

    // TODO: need handle information update.
    private void tryAddDftRouteFlow() {
        // Check vgmac is configured.
        if (this.vGMAC.equals(Constants.INVALID_MAC_ADDRESS)) {
            return;
        }

        // Check quagga interface ip is configured and it's mac (default route source mac) is probed.
        if (this.quaggaInterfaceIp.equals(Constants.INVALID_IP_ADDRESS) ||
                (null == this.getHostInfoByIpAddr(this.quaggaInterfaceIp))) {
            return;
        }

        // Check edge router interface ip is configured and it's mac (default route dst mac) & port is probed.
        if (this.edgeRouterInterfaceIp.equals(Constants.INVALID_IP_ADDRESS) ||
                (null == this.getHostInfoByIpAddr(this.edgeRouterInterfaceIp))) {
            return;
        }

        addDftRouteFlow(this.vGMAC,
                this.getHostInfoByIpAddr(this.quaggaInterfaceIp).getMac(),
                this.getHostInfoByIpAddr(this.edgeRouterInterfaceIp).getMac(),
                this.getHostInfoByIpAddr(this.edgeRouterInterfaceIp).getIngress());
    }

    private void processVirtualGatewayMacUpdate(VirtualGatewayMacUpdate update) {
        if (this.appStatus != XosAppStatusMgr.APP_STATUS_ACTIVE) {
            return;
        }

        // Only handle changes.
        if (!update.address.equals(this.vGMAC)) {
            this.vGMAC = update.address;
            LOG.info("Update VGMac to {}", this.vGMAC);
            tryAddDftRouteFlow();
        }
    }

    private void processEdgeRouterInterfaceIpUpdate(EdgeRouterInterfaceIpUpdate update) {
        if (this.appStatus != XosAppStatusMgr.APP_STATUS_ACTIVE) {
            return;
        }

        // Only handle changes.
        if (!update.address.equals(this.edgeRouterInterfaceIp)) {
            this.edgeRouterInterfaceIp = update.address;
            tryAddDftRouteFlow();
        }
    }

    private void processQuaggaInterfaceIpUpdate(QuaggaInterfaceIpUpdate update) {
        if (this.appStatus != XosAppStatusMgr.APP_STATUS_ACTIVE) {
            return;
        }

        // Only handle changes.
        if (!update.address.equals(this.quaggaInterfaceIp)) {
            this.quaggaInterfaceIp = update.address;
            tryAddDftRouteFlow();
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
            processArpProbe();
        } else if (message instanceof NotifyDefaultRoute) {
            // The message is a signal to us that we can try to re-add the default route.
            tryAddDftRouteFlow();
        } else if (message instanceof ManagedSubnetUpdate) {
            processSubnetUpdate((ManagedSubnetUpdate) message);
        } else if (message instanceof ArpPacketIn) {
            processArp((ArpPacketIn) message);
        } else if (message instanceof UserFlowOp) {
            processUserFlowOp((UserFlowOp) message);
        } else if (message instanceof UserGroupOp) {
            processUserGroupOp((UserGroupOp) message);
        } else if (message instanceof VirtualGatewayMacUpdate) {
            processVirtualGatewayMacUpdate((VirtualGatewayMacUpdate) message);
        } else if (message instanceof EdgeRouterInterfaceIpUpdate) {
            processEdgeRouterInterfaceIpUpdate((EdgeRouterInterfaceIpUpdate) message);
        } else if (message instanceof QuaggaInterfaceIpUpdate) {
            processQuaggaInterfaceIpUpdate((QuaggaInterfaceIpUpdate) message);
        } else {
            unhandled(message);
        }
    }

    public static Props props(final PacketProcessingService packetProcessingService,
                              final SalFlowService salFlowService,
                              final SalGroupService salGroupService,
                              final DataBroker dataService) {
        return Props.create(new SdnSwitchActorCreator(packetProcessingService, salFlowService, salGroupService, dataService));
    }

    private static final class SdnSwitchActorCreator implements Creator<SdnSwitchActor> {
        private final PacketProcessingService packetProcessingService;
        private final SalFlowService salFlowService;
        private final SalGroupService salGroupService;
        private final DataBroker dataService;

        SdnSwitchActorCreator(final PacketProcessingService packetProcessingService,
                              final SalFlowService salFlowService,
                              final SalGroupService salGroupService,
                              final DataBroker dataService) {
            this.packetProcessingService = Preconditions.checkNotNull(packetProcessingService);
            this.salFlowService = Preconditions.checkNotNull(salFlowService);
            this.salGroupService = Preconditions.checkNotNull(salGroupService);
            this.dataService = Preconditions.checkNotNull(dataService);
        }

        @Override
        public SdnSwitchActor create() {
            return new SdnSwitchActor(packetProcessingService, salFlowService, salGroupService, dataService);
        }
    }
}
