package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.main.rev150820;

import akka.actor.ActorSystem;
import akka.osgi.BundleDelegatingClassLoader;
import com.typesafe.config.ConfigFactory;
import com.xsdn.main.config.ConfigDataListener;
import com.xsdn.main.packet.ArpPacketHandler;
import com.xsdn.main.rpc.XosRpcProvider;
import com.xsdn.main.sw.SdnSwitchManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class XosModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.main.rev150820.AbstractXosModule {
    private final static Logger LOG = LoggerFactory.getLogger(XosModule.class);
    // Thread poll which will be used to process pkt in message.
    private final ExecutorService pktInExecutor = Executors.newCachedThreadPool();
    private ArpPacketHandler arpPacketHandler;
    private Registration arpPacketInListener = null;
    private ConfigDataListener configDataListener = null;

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

        // Initialize actor system.
        BundleContext bundleContext =
                BundleReference.class.cast(XosModule.class.getClassLoader())
                        .getBundle()
                        .getBundleContext();
        BundleDelegatingClassLoader classLoader = new BundleDelegatingClassLoader(bundleContext.getBundle(),
                Thread.currentThread().getContextClassLoader());
        final ActorSystem system = ActorSystem.create("XosActorSystem", ConfigFactory.load(classLoader), classLoader);

        // Create west and east sdn switch actor.
        final SdnSwitchManager sdnSwitchManager = new SdnSwitchManager(system, packetProcessingService);

        // Register xos rpc.
        getBindingAwareBrokerDependency().registerProvider(new XosRpcProvider());

        // TODO: install a to controller flow.


        // Register packet dispatcher.
        arpPacketHandler = new ArpPacketHandler(sdnSwitchManager);
        arpPacketInListener = notificationService.registerNotificationListener(arpPacketHandler);

        // Register config handler.
        configDataListener = new ConfigDataListener(sdnSwitchManager, dataService);

        final class CloseXosResource implements AutoCloseable {
            @Override
            public void close() throws Exception {

                // pktInExecutor.shutdown();

                if (arpPacketInListener != null) {
                    arpPacketInListener.close();
                }

                return;
            }
        }

        AutoCloseable ret = new CloseXosResource();
        LOG.info("XOS(instance {}) initialized.", ret);
        return ret;
    }

}
