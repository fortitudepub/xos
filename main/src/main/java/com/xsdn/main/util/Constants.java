package com.xsdn.main.util;

/**
 * Created by fortitude on 15-10-8.
 */
public class Constants {
    public static short XOS_APP_DEFAULT_TABLE_ID=(short)0;
    public static String XOS_APP_DFT_ARP_FLOW_NAME="XOS_APP_DFT_ARP_FLOW_NAME";
    // Show use int to store 65535 because java short is signed and can only have 32768 as max.
    public static int XOS_APP_DFT_ARP_FLOW_PRIORITY=65535;
    public static String XOS_APP_DFT_ROUTE_FLOW_NAME="XOS_APP_DFT_ROUTE_FLOW_NAME";
    public static int XOS_APP_DFT_ROUTE_FLOW_PRIORITY=30000;

}
