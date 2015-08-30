package com.xsdn.main.config;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.Xos;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.SdnSwitch;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by fortitude on 15-8-27.
 */
public class ConfigDataListener implements DataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigDataListener.class);
    private final DataBroker dataService;
    private Registration confListener;

    public ConfigDataListener(DataBroker dataBroker)
    {
        this.dataService = dataBroker;

        // ZDY: the syntax here is called type inference, refer to java programmer guide to learn it.
        // A good tutorial is in: https://docs.oracle.com/javase/tutorial/java/generics/index.html
        InstanceIdentifier<SdnSwitch> sdnSwInstances = InstanceIdentifier.<Xos>builder(Xos.class)
                .<SdnSwitch>child(SdnSwitch.class).build();
        confListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, sdnSwInstances,
                this, AsyncDataBroker.DataChangeScope.BASE);

        LOG.info("XOS finished registered data change listeners");

        return;
    }

    public void closeListeners() throws Exception
    {
        if (confListener != null)
        {
            confListener.close();
        }

        return;
    }


    private void processInstanceId(InstanceIdentifier instanceId, DataObject data, boolean updDelFlag)
    {
        LOG.debug ("entering processInstanceId");
        if (instanceId.getTargetType().equals(SdnSwitch.class))
        {
            SdnSwitch sdnSw = (SdnSwitch) data;

            LOG.info("New sdn switch added " + sdnSw.getSdnSwitchDpid());
        }

        // TODO: dispatch other instance data changes...
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent)
    {
        // ZDY: we should create SDN switch object (may be as an Akka actor) here to abstract the logic.


        // ZDY: and also we should take leadership into consideration here, only the leader application instance
        // should manage the SDN switch, or we can delegate the work to the SDN switch, let it decide according
        // the leadership.

        if(dataChangeEvent == null)
        {
            return;
        }

        Map<InstanceIdentifier<?>, DataObject> createdData = dataChangeEvent.getCreatedData();
        if ((createdData != null) && !(createdData.isEmpty()))
        {
            Set<InstanceIdentifier<?>> createdSet = createdData.keySet();
            for (InstanceIdentifier<?> instanceId : createdSet)
            {
                processInstanceId(instanceId, createdData.get(instanceId), true);
            }
        }
    }
}
