package com.xsdn.main.packet;

import akka.actor.ActorRef;
import com.xsdn.main.sw.SdnSwitchActor;
import com.xsdn.main.sw.SdnSwitchManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fortitude on 15-8-25.
 */
public class PacketInListener implements PacketProcessingListener {
    private static final Logger LOG = LoggerFactory.getLogger(PacketInListener.class);
    private SdnSwitchManager sdnSwitchManager;

    public PacketInListener(SdnSwitchManager sdnSwitchManager) {
        this.sdnSwitchManager = sdnSwitchManager;
    }

    @Override
    public void onPacketReceived (PacketReceived packetReceived)
    {
        NodeConnectorRef nc;
        NodeId nodeId;
        ActorRef switchRef;

        boolean result = false;
        if (packetReceived == null)
        {
            LOG.debug("receiving null packet. returning without any processing");
            return;
        }
        byte[] data = packetReceived.getPayload();
        if (data.length <= 0)
        {
            LOG.debug ("received packet with invalid length {}", data.length);
            return;
        }
        try
        {
            nc = packetReceived.getIngress();
            nodeId = nc.getValue().firstKeyOf(Node.class, NodeKey.class).getId();
            switchRef = sdnSwitchManager.getSdnSwitchByNodeId(nodeId);
            if (switchRef != null) {
                switchRef.tell(new SdnSwitchActor.OFPacketIn(nodeId, packetReceived), null);
            }
            else {
                LOG.debug("packet received from a unconfigured sdn switch, received node id is " + nodeId);
            }
        }
        catch(Exception e)
        {
            LOG.warn("Failed to decode packet: {}", e.getMessage());
            return;
        }
    }
}
