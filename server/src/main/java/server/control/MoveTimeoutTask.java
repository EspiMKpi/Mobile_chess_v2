package server.control;

import java.util.TimerTask;

/** Chủ: N11. Báo hết 30 giây về trọng tài. */
public class MoveTimeoutTask extends TimerTask {

    private final ChessReferee referee;

    public MoveTimeoutTask(ChessReferee referee) {
        this.referee = referee;
    }

    @Override
    public void run() {
        // TODO(N11): referee.onMoveTimeout();
    }
}
