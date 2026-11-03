package client.view;

import javax.swing.JDialog;

import client.control.ClientCtr;
import model.PlayerStat;
import model.ObjectWrapper;

/**
 * Chủ: N04. Hình 4.1. Nhận REPLY_CHALLENGE_BUSY, REPLY_CHALLENGE_REJECT, SERVER_START_MATCH.
 */
public class ChallengeDialog extends JDialog implements DataReceiver {

    private final ClientCtr myControl;
    private final PlayerStat target;

    public ChallengeDialog(ClientCtr myControl, PlayerStat target) {
        super((java.awt.Frame) null, "Thách đấu");
        this.myControl = myControl;
        this.target = target;
        // TODO(N04, M4): dựng giao diện, đăng ký mã lệnh vào myControl.getActiveFunction()
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M4)
    }
}
