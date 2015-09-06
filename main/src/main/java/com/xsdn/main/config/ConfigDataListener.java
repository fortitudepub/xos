package com.xsdn.main.config;

import com.xsdn.main.sw.SdnSwitchActor;
import com.xsdn.main.sw.SdnSwitchManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.Xos;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.AiActivePassiveSwitchset;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.East;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.West;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.east.EastSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.xos.ai.active.passive.switchset.west.WestSwitch;
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

        westSwitchConfListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, westSwitchIID,
                this, AsyncDataBroker.DataChangeScope.BASE);
        eastSwitchConfListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, eastSwitchIID,
                this, AsyncDataBroker.DataChangeScope.BASE);

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


        return;
    }


    private void processInstanceId(InstanceIdentifier instanceId, DataObject data, boolean updDelFlag)
    {
        LOG.debug ("entering processInstanceId");
        if (instanceId.getTargetType().equals(WestSwitch.class))
        {
            WestSwitch westSw = (WestSwitch) data;

            LOG.info("New west sdn switch added " + westSw.getDpid());

            SdnSwitchActor.DpIdCreated dpIdCreated= new SdnSwitchActor.DpIdCreated(westSw.getDpid());
            this.sdnSwitchManager.getEastSdnSwitchActor().tell(dpIdCreated, null);
        }
        else if (instanceId.getTargetType().equals(EastSwitch.class))
        {
            EastSwitch eastSw = (EastSwitch) data;

            LOG.info("New east sdn switch added " + eastSw.getDpid());

            SdnSwitchActor.DpIdCreated dpIdCreated= new SdnSwitchActor.DpIdCreated(eastSw.getDpid());
            this.sdnSwitchManager.getEastSdnSwitchActor().tell(dpIdCreated, null);
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
