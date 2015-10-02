package com.xsdn.main.ha;

import com.xsdn.main.sw.SdnSwitchManager;

/**
 * Created by fortitude on 15-8-23.
 */
public class XosAppStatusMgr {
    private int appStatus = 0;
    public static int APP_STATUS_INVALID = 0;
    public static int APP_STATUS_ACTIVE = 1;
    public static int APP_STATUS_BACKUP = 2;

    private static XosAppStatusMgr xosAppStatus;

    public static XosAppStatusMgr getXosAppStatus() {
        if (xosAppStatus == null) {
            xosAppStatus = new XosAppStatusMgr();
            xosAppStatus.appStatus = XosAppStatusMgr.APP_STATUS_INVALID;
            return xosAppStatus;
        }

        return xosAppStatus;
    }

    public void setStatus(int status) {
        this.appStatus = status;

        // Notify actor the role of this controller instance.
        SdnSwitchManager.getSdnSwitchManager().notifyAppStatus(this.appStatus);
    }

    public int getStatus() {
        return this.appStatus;
    }
}
