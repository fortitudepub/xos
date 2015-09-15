package com.xsdn.main.sw;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import com.xsdn.main.sw.SdnSwitchActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * Created by fortitude on 15-9-6.
 */
public class SdnSwitchManager {
    private static final Logger LOG = LoggerFactory.getLogger(SdnSwitchManager.class);

    private static final String OPENFLOW_DOMAIN = "openflow:";

    NodeId westSdnSwitchNodeId = null;
    NodeId eastSdnSwitchNodeId = null;
    final ActorRef westActorRef;
    final ActorRef eastActorRef;

    public SdnSwitchManager(ActorSystem system, PacketProcessingService packetProcessingService) {
        westActorRef = system.actorOf(SdnSwitchActor.props(packetProcessingService));
        eastActorRef = system.actorOf(SdnSwitchActor.props(packetProcessingService));

        /* Send periodical arp probe message to trigger arp probe and mac learning.
         * TODO: let 50ms to be configurable. */
        SdnSwitchActor.ProbeArpOnce probeArpOnce = new SdnSwitchActor.ProbeArpOnce();
        Cancellable _cl1 =  system.scheduler().schedule(Duration.Zero(),
                Duration.create(5000, TimeUnit.MILLISECONDS), westActorRef, probeArpOnce, system.dispatcher(), null);
        Cancellable _cl2 =  system.scheduler().schedule(Duration.Zero(),
                Duration.create(5000, TimeUnit.MILLISECONDS), eastActorRef, probeArpOnce, system.dispatcher(), null);
    }

    public ActorRef getWestSdnSwitchActor() {
        return westActorRef;
    }

    public ActorRef getEastSdnSwitchActor() {
        return eastActorRef;
    }

    // Set dpid to null clear the dpid
    public void setWestSdnSwitchDpid(String dpid) {
        if (!dpid.equals("")) {
            try {
                String nodeIdUri = OPENFLOW_DOMAIN + Long.parseLong(dpid, 16);
                westSdnSwitchNodeId = new NodeId(nodeIdUri);
            } catch (NumberFormatException e) {
                LOG.error("Configured dpid " + dpid + " is invalid");
                westSdnSwitchNodeId = null;
            }
        }
        else {
            westSdnSwitchNodeId = null;
        }
    }

    public void setEastSdnSwitchDpid(String dpid) {
        if (!dpid.equals("")) {
            try {
                String nodeIdUri = OPENFLOW_DOMAIN + Long.parseLong(dpid, 16);
                westSdnSwitchNodeId = new NodeId(nodeIdUri);
            } catch (NumberFormatException e) {
                LOG.error("Configured dpid " + dpid + " is invalid");
                westSdnSwitchNodeId = null;
            }
        }
        else {
            westSdnSwitchNodeId = null;
        }
    }

    public ActorRef getSdnSwitchByNodeId(NodeId nodeId) {
        if (nodeId.equals(westSdnSwitchNodeId)) {
            return westActorRef;
        }

        if (nodeId.equals(eastSdnSwitchNodeId)) {
            return eastActorRef;
        }

        return null;
    }
}
