package com.xsdn.main.sw;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * Created by fortitude on 15-9-6.
 */
public class SdnSwitchManager {
    final ActorRef westActorRef;
    final ActorRef eastActorRef;

    public SdnSwitchManager(ActorSystem system) {
        westActorRef = system.actorOf(Props.create(SdnSwitchActor.class), "westActor");
        eastActorRef = system.actorOf(Props.create(SdnSwitchActor.class), "eastActor");

        /* Send periodical arp probe message to trigger arp probe and mac learning.
         * TODO: let 50ms to be configurable. */
        SdnSwitchActor.ProbeArpOnce probeArpOnce = new SdnSwitchActor.ProbeArpOnce();
        Cancellable _cl1 =  system.scheduler().schedule(Duration.Zero(),
                Duration.create(5000, TimeUnit.MILLISECONDS), westActorRef, probeArpOnce, system.dispatcher(), null);
        Cancellable _cl2 =  system.scheduler().schedule(Duration.Zero(),
                Duration.create(5000, TimeUnit.MILLISECONDS), eastActorRef, probeArpOnce, system.dispatcher(), null);
    }

    public ActorRef getWestSdnSwitchActor() {
        return westActorRef;
    }

    public ActorRef getEastSdnSwitchActor() {
        return eastActorRef;
    }
}
