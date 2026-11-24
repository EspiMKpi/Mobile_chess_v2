package server.control;

import java.util.Timer;
import java.util.TimerTask;

import model.ObjectWrapper;
import model.PlayerStat;

/**
 * Chủ: N04. Quản lý vòng đời một lời mời thách đấu đang chờ phản hồi (tối đa 30 giây).
 */
public class PendingChallenge {

    public static final long INVITE_TIMEOUT_MS = 30_000L;

    private final ServerCtr myServer;
    private final ServerCtr.ServerProcessing challenger;
    private final ServerCtr.ServerProcessing challenged;
    private Timer timeoutTimer;
    private boolean resolved;

    public PendingChallenge(ServerCtr myServer,
                            ServerCtr.ServerProcessing challenger,
                            ServerCtr.ServerProcessing challenged) {
        this.myServer = myServer;
        this.challenger = challenger;
        this.challenged = challenged;
    }

    public synchronized void startTimeout() {
        if (resolved) {
            return;
        }
        timeoutTimer = new Timer(true);
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                onTimeout();
            }
        }, INVITE_TIMEOUT_MS);
    }

    public synchronized void onDecision(ServerCtr.ServerProcessing sender, boolean accept) {
        if (resolved || sender != challenged) {
            return;
        }
        resolved = true;
        cancelTimer();

        if (!accept) {
            releaseToIdle();
            challenger.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_REJECT, "REJECT"));
            return;
        }

        // B đồng ý -> bàn giao sang MatchSession (GameRoom của N11)
        challenger.setPendingChallenge(null);
        challenged.setPendingChallenge(null);
        MatchSession session = new GameRoom(myServer, challenger, challenged);
        myServer.registerMatchSession(session);
        challenger.setMatchSession(session);
        challenged.setMatchSession(session);
        session.start();
    }

    public synchronized void onCancel(ServerCtr.ServerProcessing sender) {
        if (resolved || sender != challenger) {
            return;
        }
        resolved = true;
        cancelTimer();
        releaseToIdle();
        challenged.sendData(new ObjectWrapper(ObjectWrapper.SERVER_CANCEL_CHALLENGE, "CANCEL"));
        challenger.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_REJECT, "CANCEL"));
    }

    public synchronized void onTimeout() {
        if (resolved) {
            return;
        }
        resolved = true;
        cancelTimer();
        releaseToIdle();
        challenger.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_REJECT, "TIMEOUT"));
        challenged.sendData(new ObjectWrapper(ObjectWrapper.SERVER_CANCEL_CHALLENGE, "TIMEOUT"));
    }

    public synchronized void onDisconnect(ServerCtr.ServerProcessing disconnected) {
        if (resolved) {
            return;
        }
        resolved = true;
        cancelTimer();

        if (disconnected == challenger) {
            challenged.setPendingChallenge(null);
            challenged.setStatus(PlayerStat.IDLE);
            challenged.sendData(new ObjectWrapper(ObjectWrapper.SERVER_CANCEL_CHALLENGE, "DISCONNECT"));
        } else if (disconnected == challenged) {
            challenger.setPendingChallenge(null);
            challenger.setStatus(PlayerStat.IDLE);
            challenger.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_REJECT, "DISCONNECT"));
        }
        myServer.broadcastOnlineList();
    }

    private void releaseToIdle() {
        challenger.setPendingChallenge(null);
        challenged.setPendingChallenge(null);
        challenger.setStatus(PlayerStat.IDLE);
        challenged.setStatus(PlayerStat.IDLE);
        myServer.broadcastOnlineList();
    }

    private void cancelTimer() {
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
            timeoutTimer = null;
        }
    }
}
