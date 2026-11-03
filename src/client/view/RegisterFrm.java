package client.view;

import javax.swing.JFrame;

import client.control.ClientCtr;
import model.ObjectWrapper;

/**
 * Chủ: N04. Hình 2.2. Nhận REPLY_REGISTER_USER.
 */
public class RegisterFrm extends JFrame implements DataReceiver {

    private final ClientCtr myControl;

    public RegisterFrm(ClientCtr myControl) {
        super("Đăng ký");
        this.myControl = myControl;
        // TODO(N04, M2): dựng giao diện, đăng ký mã lệnh vào myControl.getActiveFunction()
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M2)
    }
}
