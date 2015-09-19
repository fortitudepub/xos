package com.xsdn.main.sw;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.google.common.base.Preconditions;
import com.xsdn.main.packet.ArpPacketBuilder;
import com.xsdn.main.util.EtherAddress;
import com.xsdn.main.util.Ip4Network;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.packet.received.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.AiManagedSubnet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Created by fortitude on 15-8-23.
 */
public class SdnSwitchActor extends UntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(SdnSwitchActor.class);
    private static final HashMap<Short, AiManagedSubnet> subnetMap = new HashMap(50); // TODO: 50 is a arbitrary number now.
    private PacketProcessingService packetProcessingService = null;

    private SdnSwitchActor(final PacketProcessingService packetProcessingService) {
        this.packetProcessingService = Preconditions.checkNotNull(packetProcessingService);
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
            LOG.info("SdnSwitch actor received dpid created, dpid is " + ((DpIdCreated)(message)).getDpId());
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

    public static Props props(final PacketProcessingService packetProcessingService) {
        return Props.create(new SdnSwitchActorCreator(packetProcessingService));
    }

    private static final class SdnSwitchActorCreator implements Creator<SdnSwitchActor> {
        private final PacketProcessingService packetProcessingService;

        SdnSwitchActorCreator(final PacketProcessingService packetProcessingService) {
            this.packetProcessingService = Preconditions.checkNotNull(packetProcessingService);
        }

        @Override
        public SdnSwitchActor create() {
            return new SdnSwitchActor(packetProcessingService);
        }
    }
}
