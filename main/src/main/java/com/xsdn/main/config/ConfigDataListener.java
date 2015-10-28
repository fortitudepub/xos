package com.xsdn.main.config;

import com.xsdn.main.sw.SdnSwitchActor;
import com.xsdn.main.sw.SdnSwitchManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.Xos;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.AiActivePassiveSwitchset;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.AiManagedSubnet;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.East;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.West;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.east.EastSwitch;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.west.WestSwitch;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Created by fortitude on 15-8-27.
 */
public class ConfigDataListener implements DataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigDataListener.class);
    private final DataBroker dataService;
    private Registration westSwitchConfListener;
    private Registration managedSubnetsConfListener;
    private Registration eastSwitchConfListener;
    private SdnSwitchManager sdnSwitchManager;

    public ConfigDataListener(SdnSwitchManager sdnSwitchManager, DataBroker dataBroker)
    {
        this.sdnSwitchManager = sdnSwitchManager;
        this.dataService = dataBroker;

        // ZDY: the syntax here is called type inference, refer to java programmer guide to learn it.
        // A good tutorial is in: https://docs.oracle.com/javase/tutorial/java/generics/index.html
        InstanceIdentifier<WestSwitch> westSwitchIID = InstanceIdentifier.<Xos>builder(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<West>child(West.class)
                .<WestSwitch>child(WestSwitch.class).build();
        InstanceIdentifier<EastSwitch> eastSwitchIID = InstanceIdentifier.<Xos>builder(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<East>child(East.class)
                .<EastSwitch>child(EastSwitch.class).build();
        InstanceIdentifier<AiManagedSubnet> managedSubnetIID = InstanceIdentifier.<Xos>builder(Xos.class)
                .<AiActivePassiveSwitchset>child(AiActivePassiveSwitchset.class)
                .<AiManagedSubnet>child(AiManagedSubnet.class).build();


        westSwitchConfListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                westSwitchIID, this, AsyncDataBroker.DataChangeScope.BASE);
        eastSwitchConfListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                eastSwitchIID, this, AsyncDataBroker.DataChangeScope.BASE);
        managedSubnetsConfListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                managedSubnetIID, this, AsyncDataBroker.DataChangeScope.BASE);


        LOG.info("XOS finished registered data change listeners");

        return;
    }

    public void closeListeners() throws Exception
    {
        if (westSwitchConfListener != null)
        {
            westSwitchConfListener.close();
        }

        if (eastSwitchConfListener != null)
        {
            eastSwitchConfListener.close();
        }

        if (managedSubnetsConfListener != null)
        {
            managedSubnetsConfListener.close();
        }

        return;
    }


    private void processInstanceId(InstanceIdentifier instanceId, DataObject data, boolean updDelFlag)
    {
        if (updDelFlag) {
            LOG.debug("entering processInstanceId");
            if (instanceId.getTargetType().equals(WestSwitch.class)) {
                this.sdnSwitchManager.updateWestSdnSwitch((WestSwitch)data);
            } else if (instanceId.getTargetType().equals(EastSwitch.class)) {
                this.sdnSwitchManager.updateEastSdnSwitch((EastSwitch)data);
            } else if (instanceId.getTargetType().equals(AiManagedSubnet.class)) {
                LOG.info("New subnet added");

                AiManagedSubnet managedSubnet = (AiManagedSubnet)data;
                this.sdnSwitchManager.getEastSdnSwitchActor()
                        .tell(new SdnSwitchActor.ManagedSubnetUpdate(managedSubnet, false), null);
                this.sdnSwitchManager.getWestSdnSwitchActor()
                        .tell(new SdnSwitchActor.ManagedSubnetUpdate(managedSubnet, false), null);
            }
        }
        else {
            // TODO: process delete.
        }
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent)
    {
        // ZDY: and also we should take leadership into consideration here, only the leader application instance
        // should manage the SDN switch, or we can delegate the work to the SDN switch, let it decide according
        // the leadership.

        if(dataChangeEvent == null)
        {
            return;
        }

        Map<InstanceIdentifier<?>, DataObject> createdData = dataChangeEvent.getCreatedData();
        Map<InstanceIdentifier<?>, DataObject> updatedData = dataChangeEvent.getUpdatedData();
        Set<InstanceIdentifier<?>> removedData = dataChangeEvent.getRemovedPaths();
        Map<InstanceIdentifier<?>, DataObject> originalData = dataChangeEvent.getOriginalData();

        if ((createdData != null) && !(createdData.isEmpty()))
        {
            Set<InstanceIdentifier<?>> createdSet = createdData.keySet();
            for (InstanceIdentifier<?> instanceId : createdSet)
            {
                processInstanceId (instanceId, createdData.get(instanceId), true);
            }
        }

        if ((updatedData != null) && !(updatedData.isEmpty()))
        {
            Set<InstanceIdentifier<?>> updatedSet = updatedData.keySet();
            for (InstanceIdentifier<?> instanceId : updatedSet)
            {
                processInstanceId (instanceId, updatedData.get(instanceId), true);
            }
        }

        if ((removedData != null) && (!removedData.isEmpty()) && (originalData != null) && (!originalData.isEmpty()))
        {
            for (InstanceIdentifier<?> instanceId : removedData)
            {
                processInstanceId (instanceId, originalData.get(instanceId), false);
            }
        }
    }
}
