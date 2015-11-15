/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.xsdn.xos.packethandler.decoders;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

/**
 * A base class for all decoders. Each extended decoder should also implement a notification listener
 * that it can consume. And make use of
 */
public abstract class AbstractPacketDecoder<ConsumedPacketNotification, ProducedPacketNotification extends Notification>  {


  private Class<ProducedPacketNotification> producedPacketNotificationType;
  private NotificationService notificationService;
  private NotificationPublishService notificationPublishService;


  protected Registration listenerRegistration;

  /**
   * Constructor to
   * @param producedPacketNotificationType
   * @param notificationService
   */
  public  AbstractPacketDecoder(Class<ProducedPacketNotification> producedPacketNotificationType,
                                NotificationService notificationService,
                                NotificationPublishService notificationPublishService) {
    this.producedPacketNotificationType = producedPacketNotificationType;
    this.notificationService = notificationService;
    this.notificationPublishService = notificationPublishService;
  }

  /**
   * Every extended decoder should call this method on a receipt of a input packet notification.
   * This method would make sure it decodes only when necessary and publishes corresponding event
   * on successful decoding.
   */
  public void decodeAndPublish(ConsumedPacketNotification consumedPacketNotification) {
    ProducedPacketNotification packetNotification=null;
    if(consumedPacketNotification!= null && canDecode(consumedPacketNotification)) {
      packetNotification = decode(consumedPacketNotification);
    }
    if(packetNotification != null) {
      notificationPublishService.offerNotification(packetNotification);
    }
  }
  /**
   * Decodes the payload in given Packet further and returns a extension of Packet.
   * e.g. ARP, IPV4, LLDP etc.
   *
   * @return
   */
  public abstract ProducedPacketNotification decode(ConsumedPacketNotification consumedPacketNotification);


  public abstract NotificationListener getConsumedNotificationListener();

  public abstract boolean canDecode(ConsumedPacketNotification consumedPacketNotification);
}
