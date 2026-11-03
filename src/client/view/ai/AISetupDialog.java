package client.view.ai;

import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 * Chủ: N11. Hộp thoại thiết lập ván với máy (hình 3.1 báo cáo N11).
 * OnlineListFrm (N04) chỉ gọi: new AISetupDialog(this).setVisible(true);
 */
public class AISetupDialog extends JDialog {

    public AISetupDialog(JFrame owner) {
        super(owner, "Thiết lập ván với máy", true);
        // TODO(N11): chọn Người/Máy cho Trắng/Đen, độ sâu 1-6 -> mở AIMatchFrm
    }
}
