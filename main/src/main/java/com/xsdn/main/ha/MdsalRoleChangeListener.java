package com.xsdn.main.ha;

/**
 * Created by fortitude on 15-9-28.
 */

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

//ZDY_NOTE: we have two place that we need to decide active and backup
// 1st: active switch and backup switch
// 2nd: active controller and backup controller
// for the later, we listen to the mdsal role change event and act accordingly.

/**
 * This is a implementation of a Role Change Listener which is an actor, which registers itself to the ClusterRoleChangeNotifier
 * The Role Change listener receives a SetNotifiers message with the notifiers to register itself with.
 * It kicks of a scheduler which sents registration messages to the notifiers, till it gets a RegisterRoleChangeListenerReply
 * If registered, then it cancels the scheduler, otherwise it starts the scheduler again until registration finished.
 *
 */
public class MdsalRoleChangeListener extends AbstractUntypedActor implements AutoCloseable{

    private static final String NOTIFIER_AKKA_URL = "akka.tcp://opendaylight-cluster-data@127.0.0.1:2550/user";

    private Cancellable registrationSchedule = null;
    private static final FiniteDuration duration = new FiniteDuration(100, TimeUnit.MILLISECONDS);
    private static final FiniteDuration schedulerDuration = new FiniteDuration(1, TimeUnit.SECONDS);
    private static final String shardName = "xos";
    //private String notifierActorName = NOTIFIER_AKKA_URL + "/" + shardName + "-notifier";
    // TODO: this is hard coded path for test only, later we need to find a way to locate the xos config actor.
    private String notifierActorName =
            "akka.tcp://opendaylight-cluster-data@127.0.0.1:2550/user/shard manager-config/member-1-shard-xos-config/member-1-shard-xos-config-notifier";

    public MdsalRoleChangeListener(String memberName) {
        super();
        scheduleRegistrationListener(schedulerDuration);
    }

    public static Props getProps(final String memberName) {
        return Props.create(MdsalRoleChangeListener.class, memberName);
    }

    static public class DoRegisterListener {
    }


    @Override
    protected void handleReceive(Object message) throws Exception {
        if (message instanceof DoRegisterListener) {
            // called by the scheduler at intervals to register any unregistered notifiers
            sendRegistrationRequests();
        } else if (message instanceof RegisterRoleChangeListenerReply) {
            // called by the Notifier
            handleRegisterRoleChangeListenerReply(getSender().path().toString());
        } else if (message instanceof RoleChangeNotification) {
            // called by the Notifier
            RoleChangeNotification notification = (RoleChangeNotification) message;

            LOG.info("Role Change Notification received for member:{}, old role:{}, new role:{}",
                    notification.getMemberId(), notification.getOldRole(), notification.getNewRole());

            // the apps dependent on such notifications can be called here
            //TODO: add implementation here
        }
    }

    private void scheduleRegistrationListener(FiniteDuration interval) {
        LOG.debug("--->scheduleRegistrationListener called.");
        registrationSchedule = getContext().system().scheduler().schedule(
                interval, interval, getSelf(), new DoRegisterListener(),
                getContext().system().dispatcher(), getSelf());

    }

    private void sendRegistrationRequests() {
        try {
            LOG.debug("{} registering with {}", getSelf().path().toString(), notifierActorName);
            ActorRef notifier = Await.result(
                    getContext().actorSelection(notifierActorName).resolveOne(duration), duration);

            notifier.tell(new RegisterRoleChangeListener(), getSelf());
        } catch (Exception e) {
            LOG.error("ERROR!! Unable to send registration request to notifier {}", shardName);
        }
    }

    private void handleRegisterRoleChangeListenerReply(String senderId) {
        if (senderId.contains("xos")) {
            LOG.info("Succesfully register to xos config role change notifier:{}", senderId);
            //cancel the schedule when listener is registered with all notifiers
            if (!registrationSchedule.isCancelled()) {
                registrationSchedule.cancel();
            }
        } else {
            LOG.info("Unexpected, RegisterRoleChangeListenerReply received from notifier which is not known to Listener:{}",
                    senderId);
        }
    }


    @Override
    public void close() throws Exception {
        registrationSchedule.cancel();
    }
}
