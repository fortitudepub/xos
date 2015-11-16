package com.xsdn.main.packet;

import akka.actor.ActorRef;
import com.xsdn.main.sw.SdnSwitchActor;
import com.xsdn.main.sw.SdnSwitchManager;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.packet.arp.rev140528.ArpPacketListener;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fortitude on 15-8-25.
 */
public class ArpPacketHandler implements ArpPacketListener {
    private static final Logger LOG = LoggerFactory.getLogger(ArpPacketHandler.class);
    private SdnSwitchManager sdnSwitchManager;

    public ArpPacketHandler(SdnSwitchManager sdnSwitchManager) {
        this.sdnSwitchManager = sdnSwitchManager;
    }

    @Override
    public void onArpPacketReceived(ArpPacketReceived packetReceived)
    {
        LOG.debug("Received an arp packet in arp packet handler");

        NodeId nodeId;
        ActorRef switchRef;

        if(packetReceived == null || packetReceived.getPacketChain() == null) {
            return;
        }

        RawPacket rawPacket = null;
        EthernetPacket ethernetPacket = null;
        ArpPacket arpPacket = null;

        for(PacketChain packetChain : packetReceived.getPacketChain()) {
            if(packetChain.getPacket() instanceof RawPacket) {
                rawPacket = (RawPacket) packetChain.getPacket();
            } else if(packetChain.getPacket() instanceof EthernetPacket) {
                ethernetPacket = (EthernetPacket) packetChain.getPacket();
            } else if(packetChain.getPacket() instanceof ArpPacket) {
                arpPacket = (ArpPacket) packetChain.getPacket();
            }
        }

        if(rawPacket == null || ethernetPacket == null || arpPacket == null) {
            return;
        }

        nodeId = rawPacket.getIngress().getValue().firstIdentifierOf(Node.class)
                .firstKeyOf(Node.class, NodeKey.class).getId();;
        switchRef = sdnSwitchManager.getSdnSwitchByNodeId(nodeId);
        if (switchRef != null) {
            switchRef.tell(new SdnSwitchActor.ArpPacketIn(nodeId, rawPacket, arpPacket), null);
        } else {
            LOG.debug("ARP packet received from a unconfigured SDN switch, received node id is " + nodeId);
        }
    }
}
