package client.view;

import javax.swing.JFrame;

import client.control.ClientCtr;
import model.ObjectWrapper;

/**
 * Chủ: N04. Hình 2.4. Nhận REPLY_CHANGE_PASSWORD.
 */
public class ChangePasswordFrm extends JFrame implements DataReceiver {

    private final ClientCtr myControl;

    public ChangePasswordFrm(ClientCtr myControl) {
        super("Đổi mật khẩu");
        this.myControl = myControl;
        // TODO(N04, M2): dựng giao diện, đăng ký mã lệnh vào myControl.getActiveFunction()
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M2)
    }
}
