package com.xsdn.main.rpc;

import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.ForceFailoverSwitchInput;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.ForceFailoverSwitchOutput;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.ForceFailoverSwitchOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.com.xsdn.xos.params.xml.ns.yang.xos.rev150820.XosService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.util.concurrent.Future;

/**
 * Created by fortitude on 15-8-23.
 */
public class XosRpcImpl implements XosService {

    @Override
    public Future<RpcResult<ForceFailoverSwitchOutput>> forceFailoverSwitch(ForceFailoverSwitchInput input) {
        ForceFailoverSwitchOutputBuilder rstBuilder = new ForceFailoverSwitchOutputBuilder();
        return RpcResultBuilder.success(rstBuilder.build()).buildFuture();
    }
}
