package org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.packet.packet.handler.impl.rev140528;

import com.google.common.collect.ImmutableSet;
import com.xsdn.xos.packethandler.decoders.*;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PacketHandlerModule extends org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.packet.packet.handler.impl.rev140528.AbstractPacketHandlerModule {
    private static final Logger _logger = LoggerFactory.getLogger(PacketHandlerModule.class);
    ImmutableSet<AbstractPacketDecoder> decoders;

    public PacketHandlerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PacketHandlerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.packet.packet.handler.impl.rev140528.PacketHandlerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        NotificationProviderService notificationService = getNotificationServiceDependency();
        initiateDecoders(notificationService);

        final class CloseResources implements AutoCloseable {
            @Override
            public void close() throws Exception {
                closeDecoders();
                _logger.info("PacketHandler (instance {}) torn down.", this);
            }
        }

        AutoCloseable ret = new CloseResources();
        _logger.info("PacketHandler (instance {}) initialized.", ret);
        return ret;
    }

    private void initiateDecoders(NotificationProviderService notificationProviderService) {
        decoders = new ImmutableSet.Builder<AbstractPacketDecoder>()
                .add(new EthernetDecoder(notificationProviderService))
                .add(new ArpDecoder(notificationProviderService))
                .add(new Ipv4Decoder(notificationProviderService))
                .add(new Ipv6Decoder(notificationProviderService))
                .build();
    }

    private void closeDecoders() throws Exception {
        if(decoders != null && !decoders.isEmpty()) {
            for(AbstractPacketDecoder decoder : decoders) {
                decoder.close();
            }
        }
    }
}
