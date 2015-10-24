package com.xsdn.main.util;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;

/**
 * Created by fortitude on 15-10-8.
 */
public class Constants {
    public static MacAddress INVALID_MAC_ADDRESS=new MacAddress("00:00:00:00:00:00");
    public static Ipv4Address INVALID_IP_ADDRESS=new Ipv4Address("255.255.255.255");
    public static short XOS_APP_DEFAULT_TABLE_ID=(short)0;
    public static String XOS_APP_DFT_ARP_FLOW_NAME="XOS_APP_DFT_ARP_FLOW_NAME";
    // Show use int to store 65535 because java short is signed and can only have 32768 as max.
    public static int XOS_APP_DFT_ARP_FLOW_PRIORITY=65535;
    public static String XOS_APP_DFT_ROUTE_FLOW_NAME="XOS_APP_DFT_ROUTE_FLOW_NAME";
    public static int XOS_APP_DFT_ROUTE_FLOW_PRIORITY=30000;

    public static String XOS_L2_FORWARD_FLOW_NAME="XOS_L2_FORWARD_FLOW_NAME";
    public static int XOS_L2_FORWARD_FLOW_PRIORITY=20000;

    // For clustering entity owner ship detect, we use this mechanism to detect which
    // application is master.
    public static String XOS_HA_ENTITY_TYPE = "XOS_HA_ENTITY";
}
