package client.view;

import javax.swing.JDialog;

import client.control.ClientCtr;
import model.GameResult;
import model.ObjectWrapper;

/**
 * Chủ: N04. Hình 4.4 [LỆCH BC: tham số String -> GameResult]. Gửi REQUEST_CONTINUE_MATCH.
 */
public class ContinueDialog extends JDialog implements DataReceiver {

    private final ClientCtr myControl;
    private final GameResult result;

    public ContinueDialog(ClientCtr myControl, GameResult result) {
        super((java.awt.Frame) null, "Kết thúc ván");
        this.myControl = myControl;
        this.result = result;
        // TODO(N04, M4): dựng giao diện, đăng ký mã lệnh vào myControl.getActiveFunction()
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M4)
    }
}
