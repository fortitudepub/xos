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
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.packet.received.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.Xos;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.sdn._switch.AppFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.sdn._switch.AppFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.sdn._switch.AppFlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.AiActivePassiveSwitchset;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.AiManagedSubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.East;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.West;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.east.EastSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.west.WestSwitch;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetAddress;
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
    boolean deviceConnected = false;

    private SdnSwitchActor(final PacketProcessingService packetProcessingService,
                           final SalFlowService salFlowService,
                           final DataBroker dataService) {
        this.packetProcessingService = Preconditions.checkNotNull(packetProcessingService);
        this.salFlowService = salFlowService;
        this.dataService = dataService;
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

    public SdnSwitchActor() {
        // TODO:
        // 1. initialize runtime database
        // 2. start arp prober thread
        // 3. provide callback for extern events like pkt in
        // 4. implement master-slave decide logic
    }

    private Action getSendToControllerAction() {
        Action sendToController = new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new OutputActionCaseBuilder()
                        .setOutputAction(new OutputActionBuilder()
                                .setMaxLength(0xffff)
                                .setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
                                .build())
                        .build())
                .build();
        return sendToController;
    }

    private Flow createArpToControllerFlow() {

        // start building flow
        FlowBuilder arpFlow = new FlowBuilder() //
                .setTableId(Constants.XOS_APP_DEFAULT_TABLE_ID)
                .setFlowName(Constants.XOS_APP_DFT_ARP_FLOW_NAME);

        // use its own hash code for id.
        // TODO: copied from openflow plugin, we need to provide a persistent api for flow id generation
        // better use priroity+flow_id_in_that priority as we defined in our design document.
        arpFlow.setId(new FlowId(Long.toString(arpFlow.hashCode())));
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder()
                .setEthernetType(new EthernetTypeBuilder()
                        .setType(new EtherType(Long.valueOf(KnownEtherType.Arp.getIntValue()))).build());

        org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match match = new MatchBuilder()
                .setEthernetMatch(ethernetMatchBuilder.build())
                .build();

        List<Action> actions = new ArrayList<Action>();
        actions.add(getSendToControllerAction());

        // Create an Apply Action
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions)
                .build();

        // Wrap our Apply Action in an Instruction
        Instruction applyActionsInstruction = new InstructionBuilder() //
                .setOrder(0)
                .setInstruction(new ApplyActionsCaseBuilder()//
                        .setApplyActions(applyActions) //
                        .build()) //
                .build();

        // Put our Instruction in a list of Instructions
        arpFlow
                .setMatch(match) //
                .setInstructions(new InstructionsBuilder() //
                        .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                        .build()) //
                .setPriority(Constants.XOS_APP_DFT_ARP_FLOW_PRIORITY) //
                .setBufferId(OFConstants.OFP_NO_BUFFER) //
                //.setHardTimeout(flowHardTimeout)
                //.setIdleTimeout(flowIdleTimeout)
                //.setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                .setFlags(new FlowModFlags(false, false, false, false, false));

        return arpFlow.build();
    }

    // generate app flow builder by converting from ofpluginflow.
    private void storeAppFlowbyOfpluginFlow(Flow ofpluginFlow, final FlowId ofpluginFlowId) {
        final WriteTransaction writeTransaction = dataService.newWriteOnlyTransaction();

        // We need use our own flow id and appflow definition, though this seems quit duplicate, it's the problem
        // of yangtools.
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.FlowId appFlowId =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.FlowId(ofpluginFlowId.getValue());
        AppFlowKey appFlowKey = new AppFlowKey(appFlowId);

        InstanceIdentifier<AppFlow> appFlowIID;

        if (SdnSwitchManager.getSdnSwitchManager().isEast(this.nodeId)) {
            appFlowIID = InstanceIdentifier.<Xos>builder(Xos.class)
                    .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                    .<East>child(East.class)
                    .<EastSwitch>child(EastSwitch.class)
                    .<AppFlow, AppFlowKey>child(AppFlow.class, appFlowKey).build();
        }
        else if (SdnSwitchManager.getSdnSwitchManager().isWest(this.nodeId)) {
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
        AppFlowBuilder appFlowBuilder = new AppFlowBuilder(ofpluginFlow).setKey(appFlowKey).setId(appFlowId);

        // Do the database transaction, true is neccesary because we have to create the missing parent
        // node in the path.
        writeTransaction.merge(LogicalDatastoreType.OPERATIONAL, appFlowIID, appFlowBuilder.build(), true);
        final CheckedFuture writeTxResultFuture = writeTransaction.submit();
        Futures.addCallback(writeTxResultFuture, new FutureCallback() {
            @Override
            public void onSuccess(Object o) {
                LOG.debug("Store app flow {} to mdsal store successful for tx :{}",
                        ofpluginFlowId.getValue(), writeTransaction.getIdentifier());
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Store app flow {] to mdsal write transaction {} failed",
                        ofpluginFlowId.getValue(), writeTransaction.getIdentifier(), throwable.getCause());
            }
        });
    }

    private void addDftArpFlows() {

        // Note: we need install default arp flow for both active and backup switch.

        // Action 1: send to switch by using sal flow service.
        InstanceIdentifier<Node> nodeIID =
                InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, new NodeKey(new NodeId(OFutils.BuildNodeIdUriByDpid(this.dpid)))).build();
        TableKey flowTableKey = new TableKey(Constants.XOS_APP_DEFAULT_TABLE_ID);
        InstanceIdentifier<Table> tableIID = nodeIID.builder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, flowTableKey)
                .build();
        FlowId flowId = new FlowId(Constants.XOS_APP_DFT_ARP_FLOW_NAME);
        FlowKey flowKey = new FlowKey(flowId);
        InstanceIdentifier<Flow> flowIID = tableIID.child(Flow.class, flowKey);


        Flow flow = createArpToControllerFlow();
        AddFlowInputBuilder builder = new AddFlowInputBuilder(createArpToControllerFlow());

        builder.setNode(new NodeRef(nodeIID));
        builder.setFlowRef(new FlowRef(flowIID));
        builder.setFlowTable(new FlowTableRef(tableIID));
        builder.setTransactionUri(new Uri(flow.getId().getValue()));
        // TODO: by reading openflowplugin code, seems the returned future of salflowservice only make sure the
        // flow mod message is write to the of channel, it does not ensure the flow entries is installed in the
        // switch, we must implement a logic to check this. either by periodically timer or some other means.
        salFlowService.addFlow(builder.build());

        // Action 2: store to our md sal datastore for reconciliation.
        storeAppFlowbyOfpluginFlow(flow, flowId);
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
        } else {
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
