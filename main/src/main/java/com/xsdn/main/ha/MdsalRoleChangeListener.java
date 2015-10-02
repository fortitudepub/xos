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
import org.opendaylight.controller.cluster.raft.RaftState;
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

    // Defined in DistributedDataStoreFactory.
    //private static final String NOTIFIER_AKKA_URL = "akka://opendaylight-cluster-data/user";
    private static final String NOTIFIER_AKKA_URL = "akka.tcp://opendaylight-cluster-data@127.0.0.1:2550/user";

    private Cancellable registrationSchedule = null;
    private static final FiniteDuration duration = new FiniteDuration(100, TimeUnit.MILLISECONDS);
    private static final FiniteDuration schedulerDuration = new FiniteDuration(1, TimeUnit.SECONDS);
    // TODO: how we can solve this hard-coded member-1, shard, xos, -notifier .etc?
    // see createLocalShards in ShardManager to know where member-1-shard-xos-config come from.
    // basically it is constructed by "role-name"-shard-"shardname"-"datastore type(config/operational)".
    // Since we use it's leader election, we must access the information, however, this is actually a workaround,
    // if later ODL OF plugin support master/slave election and will not push flows in slave controller node,
    // we can remove our logic here.
    // NOTE: ShardIdentifier is not available because of namespace collision with sal-clustering-commons.
    private String shardName = "member-1" + "-shard-" + "xos" + "-config";
    // TODO: use string builder if neccesary.
    private String notifierActorName = NOTIFIER_AKKA_URL + "/shardmanager-config/" + shardName + "/" + shardName + "-notifier";

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

            if (notification.getNewRole().equals(RaftState.Leader.name())) {
                XosAppStatusMgr.getXosAppStatus().setStatus(XosAppStatusMgr.APP_STATUS_ACTIVE);
            } else {
                // App will only switch between active and backup after the initial invalid state.
                XosAppStatusMgr.getXosAppStatus().setStatus(XosAppStatusMgr.APP_STATUS_BACKUP);
            }
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
            LOG.info("{} registering with {}", getSelf().path().toString(), notifierActorName);
            ActorRef notifier = Await.result(
                    getContext().actorSelection(notifierActorName).resolveOne(duration), duration);

            notifier.tell(new RegisterRoleChangeListener(), getSelf());
        } catch (Exception e) {
            LOG.error("ERROR!! Unable to send registration request to notifier {}", notifierActorName);
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
