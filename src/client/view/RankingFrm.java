package client.view;

import javax.swing.JFrame;

import client.control.ClientCtr;
import model.ObjectWrapper;

/**
 * Chủ: N04. Nhận REPLY_RANKING [LỆCH BC: bổ sung].
 */
public class RankingFrm extends JFrame implements DataReceiver {

    private final ClientCtr myControl;

    public RankingFrm(ClientCtr myControl) {
        super("Bảng xếp hạng");
        this.myControl = myControl;
        // TODO(N04, M5): dựng giao diện, đăng ký mã lệnh vào myControl.getActiveFunction()
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M5)
    }
}
