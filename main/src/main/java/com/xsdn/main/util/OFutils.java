package com.xsdn.main.util;

/**
 * Created by fortitude on 15-10-2.
 */
public class OFutils {
    private static final String OPENFLOW_DOMAIN = "openflow:";
    public static short FLOW_ADD = 1;
    public static short FLOW_DELETE = 2;
    public static short FLOW_UPDATE= 3;

    public static String BuildNodeIdUriByDpid(String dpid) {
        return (OPENFLOW_DOMAIN + Long.parseLong(dpid, 16));
    }
}
