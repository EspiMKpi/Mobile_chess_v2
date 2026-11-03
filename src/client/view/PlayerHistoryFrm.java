package client.view;

import javax.swing.JFrame;

import client.control.ClientCtr;
import model.Player;
import model.ObjectWrapper;

/**
 * Chủ: N04. Hình 2.5. Nhận REPLY_PLAYER_HISTORY.
 */
public class PlayerHistoryFrm extends JFrame implements DataReceiver {

    private final ClientCtr myControl;
    private final Player myPlayer;

    public PlayerHistoryFrm(ClientCtr myControl, Player myPlayer) {
        super("Lịch sử đấu");
        this.myControl = myControl;
        this.myPlayer = myPlayer;
        // TODO(N04, M5): dựng giao diện, đăng ký mã lệnh vào myControl.getActiveFunction()
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M5)
    }
}
