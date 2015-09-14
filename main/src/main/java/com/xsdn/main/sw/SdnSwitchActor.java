package com.xsdn.main.sw;

import akka.actor.UntypedActor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.packet.received.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.AiManagedSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fortitude on 15-8-23.
 */
public class SdnSwitchActor extends UntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(SdnSwitchActor.class);

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

        public ManagedSubnetUpdate(AiManagedSubnet subnet, boolean delete) {
            this.subnet = subnet;
        }
    }

    static public class ArpPacketIn {
        private NodeId nodeId;
        private ArpPacket pkt;

        public ArpPacketIn(NodeId nodeId, ArpPacket pkt) {
            // Record node id for possible usage later.
            this.nodeId = nodeId;
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

    private void processArp(ArpPacketIn pktIn)  {
        String dip = pktIn.pkt.getDestinationProtocolAddress();
        LOG.info("XXX: process dip " + dip + " arp request");
    }

    public void onReceive(Object message) throws Exception {
        if (message instanceof DpIdCreated) {
            LOG.info("SdnSwitch actor received dpid created, dpid is " + ((DpIdCreated)(message)).getDpId());
        } else if (message instanceof ProbeArpOnce) {
            LOG.info("TO BE IMPLEMENTED: ARP PROBE");
        } else if (message instanceof  ManagedSubnetUpdate) {
            LOG.info("Process managed subnet update, I will develop GW ARP proxy soon.");
        } else if (message instanceof  ArpPacketIn) {
            processArp((ArpPacketIn)message);
        } else {
            unhandled(message);
        }
    }
}
