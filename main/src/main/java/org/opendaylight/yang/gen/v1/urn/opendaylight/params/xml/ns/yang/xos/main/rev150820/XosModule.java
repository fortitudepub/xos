package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.main.rev150820;

import com.xsdn.main.rpc.XosRpcProvider;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.main.rev150820.modules.module.configuration.xos.BindingAwareBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XosModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.main.rev150820.AbstractXosModule {
    private final static Logger LOG = LoggerFactory.getLogger(XosModule.class);

    public XosModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public XosModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.main.rev150820.XosModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("createInstance invoked for the xos module.");
        NotificationProviderService notificationService = getNotificationServiceDependency();
        DataBroker dataService = getDataBrokerDependency();
        RpcProviderRegistry rpcRegistryDependency = getRpcRegistryDependency();
        SalFlowService salFlowService = rpcRegistryDependency.getRpcService(SalFlowService.class);
        SalGroupService salGroupService = rpcRegistryDependency.getRpcService (SalGroupService.class);
        PacketProcessingService packetProcessingService =
                rpcRegistryDependency.getRpcService(PacketProcessingService.class);

        // Register xos rpc.
        getBindingAwareBrokerDependency().registerProvider(new XosRpcProvider());

        // TODO(zdy): start packet in processor and etc.


        final class CloseXosResource implements AutoCloseable {
            @Override
            public void close() throws Exception {
                // TODO(zdy): close the instance we created here if neccesary.
                // actually we should do nothing here, because our application should run infinitely(if we made it:))
                return;
            }
        }

        AutoCloseable ret = new CloseXosResource();
        LOG.info("XOS(instance {}) initialized.", ret);
        return ret;
    }

}
