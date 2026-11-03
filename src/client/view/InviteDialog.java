package client.view;

import javax.swing.JDialog;

import client.control.ClientCtr;
import model.PlayerStat;
import model.ObjectWrapper;

/**
 * Chủ: N04. Hình 4.2. Nhận SERVER_START_MATCH.
 */
public class InviteDialog extends JDialog implements DataReceiver {

    private final ClientCtr myControl;
    private final PlayerStat inviter;

    public InviteDialog(ClientCtr myControl, PlayerStat inviter) {
        super((java.awt.Frame) null, "Lời mời thách đấu");
        this.myControl = myControl;
        this.inviter = inviter;
        // TODO(N04, M4): dựng giao diện, đăng ký mã lệnh vào myControl.getActiveFunction()
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M4)
    }
}
