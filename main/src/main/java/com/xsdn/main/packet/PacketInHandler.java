package com.xsdn.main.packet;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fortitude on 15-8-25.
 */
public class PacketInHandler implements PacketProcessingListener {
    private static final Logger LOG = LoggerFactory.getLogger(PacketInHandler.class);

    @Override
    public void onPacketReceived (PacketReceived packetReceived)
    {
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
            /*
            MacAddress destMac = new MacAddress(HexEncode.bytesToHexStringFormat(BitBufferHelper.getBits(data, 0, 48)));
            int ethType = BitBufferHelper.getInt(BitBufferHelper.getBits(data, 96, 16));
            */
            LOG.info("TO BE IMPLEMENTED PKTIN SERVICE");
        }
        catch(Exception e)
        {
            LOG.warn("Failed to decode packet: {}", e.getMessage());
            return;
        }
    }
}
