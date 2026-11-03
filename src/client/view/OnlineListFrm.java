package client.view;

import javax.swing.JFrame;

import client.control.ClientCtr;
import model.Player;
import model.ObjectWrapper;

/**
 * Chủ: N04. Hình 3.1. Nhận REPLY_ONLINE_LIST, SERVER_INFORM_ONLINE_LIST, SERVER_INFORM_CHALLENGE.
 */
public class OnlineListFrm extends JFrame implements DataReceiver {

    private final ClientCtr myControl;
    private final Player myPlayer;

    public OnlineListFrm(ClientCtr myControl, Player myPlayer) {
        super("Danh sách người chơi online");
        this.myControl = myControl;
        this.myPlayer = myPlayer;
        // TODO(N04, M3): dựng giao diện, đăng ký mã lệnh vào myControl.getActiveFunction()
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M3)
    }
}
