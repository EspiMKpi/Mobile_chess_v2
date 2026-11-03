package client.view;

import javax.swing.JFrame;

import client.control.ClientCtr;
import model.Player;
import model.ObjectWrapper;

/**
 * Chủ: N04. Hình 2.3. Nhận REPLY_EDIT_PLAYER.
 */
public class EditPlayerFrm extends JFrame implements DataReceiver {

    private final ClientCtr myControl;
    private final Player myPlayer;

    public EditPlayerFrm(ClientCtr myControl, Player myPlayer) {
        super("Sửa thông tin");
        this.myControl = myControl;
        this.myPlayer = myPlayer;
        // TODO(N04, M2): dựng giao diện, đăng ký mã lệnh vào myControl.getActiveFunction()
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M2)
    }
}
