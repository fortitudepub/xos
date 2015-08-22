package com.xsdn.main;

import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.XosService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.ForceFailoverSwitchInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.ForceFailoverSwitchOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xos.rev150820.ForceFailoverSwitchOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Created by fortitude on 15-8-21.
 */
public class XosMain implements XosService {

    @Override
    public Future<RpcResult<ForceFailoverSwitchOutput>> forceFailoverSwitch(ForceFailoverSwitchInput input) {
        ForceFailoverSwitchOutputBuilder rstBuilder = new ForceFailoverSwitchOutputBuilder();
        return RpcResultBuilder.success(rstBuilder.build()).buildFuture();
    }
}