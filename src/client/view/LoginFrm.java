package client.view;

import javax.swing.JFrame;

import client.control.ClientCtr;
import model.ObjectWrapper;

/**
 * Chủ: N04. Hình 2.1. Nhận REPLY_LOGIN_USER.
 */
public class LoginFrm extends JFrame implements DataReceiver {

    private final ClientCtr myControl;

    public LoginFrm(ClientCtr myControl) {
        super("Đăng nhập");
        this.myControl = myControl;
        // TODO(N04, M2): dựng giao diện, đăng ký mã lệnh vào myControl.getActiveFunction()
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M2)
    }
}
