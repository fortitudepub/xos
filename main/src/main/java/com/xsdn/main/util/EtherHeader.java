package com.xsdn.main.util;

/**
 * Created by fortitude on 15-9-17.
 */

import com.xsdn.main.util.EtherAddress;

/**
 * {@code EtherHeader} describes the contents of ethernet header and
 * IEEE 802.1Q VLAN tag.
 */
public interface EtherHeader {
    /**
     * A pseudo VLAN ID that indicates untagged ethernet frame.
     */
    int VLAN_NONE = 0;

    /**
     * Return the source MAC address.
     *
     * @return  An {@link EtherAddress} instance which represents the source
     *          MAC address.
     */
    EtherAddress getSourceAddress();

    /**
     * Set the source MAC address.
     *
     * @param mac  An {@link EtherAddress} instance which represents the source
     *             MAC address.
     */
    void setSourceAddress(EtherAddress mac);

    /**
     * Return the destination MAC address.
     *
     * @return  An {@link EtherAddress} instance which represents the
     *          destination MAC address.
     */
    EtherAddress getDestinationAddress();

    /**
     * Set the destination MAC address.
     *
     * @param mac  An {@link EtherAddress} instance which represents the
     *             destination MAC address.
     */
    void setDestinationAddress(EtherAddress mac);

    /**
     * Return the ethernet type.
     *
     * @return  An integer value which represents the ethernet type.
     */
    int getEtherType();

    /**
     * Return the VLAN ID in the IEEE 802.1Q VLAN tag.
     *
     * @return  A VLAN ID in the VLAN tag.
     *          {@link #VLAN_NONE} is returned if this frame does not contain
     *          a VLAN tag.
     */
    int getVlanId();

    /**
     * Set the VLAN ID to the IEEE 802.1Q VLAN tag.
     *
     * @param vid  A VLAN ID in the VLAN tag.
     *             If {@link #VLAN_NONE} is specified, the VLAN tag will be
     *             removed.
     */
    void setVlanId(int vid);

    /**
     * Return the VLAN priority value in the IEEE 802.1Q VLAN tag.
     *
     * @return  A byte value which represents the VLAN priority.
     *          A negative value is returned if the VLAN priority is not
     *          configured in this instance.
     */
    short getVlanPriority();

    /**
     * Set the VLAN priority value to the IEEE 802.1Q VLAN tag.
     *
     * @param pcp  A byte value which represents the VLAN priority.
     */
    void setVlanPriority(short pcp);
}