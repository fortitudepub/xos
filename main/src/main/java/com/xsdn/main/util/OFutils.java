package com.xsdn.main.util;

/**
 * Created by fortitude on 15-10-2.
 */
public class OFutils {
    private static final String OPENFLOW_DOMAIN = "openflow:";

    public static String BuildNodeIdUriByDpid(String dpid) {
        return (OPENFLOW_DOMAIN + Long.parseLong(dpid, 16));
    }
}
