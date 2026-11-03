package client.view;

import javax.swing.JPanel;

import client.control.ClientCtr;
import model.ObjectWrapper;

/**
 * Chủ: N11. Panel bàn cờ nhúng giữa MatchPlayFrm (N04): vẽ bàn từ FEN, chọn quân + ô đích,
 * PromotionDialog, gửi SEND_MOVE, biên bản, ĐỒNG HỒ 30s (nhãn đồng hồ nằm trong panel này), "Chiếu!".
 * Mọi phương thức public đều được MatchPlayFrm gọi trên luồng Swing (EDT).
 */
public class ChessBoardPanel extends JPanel {

    private final ClientCtr myControl;
    private String myColor;
    private boolean locked = true;

    public ChessBoardPanel(ClientCtr myControl) {
        this.myControl = myControl;
    }

    /** Gọi khi nhận SERVER_START_MATCH và SERVER_RESTART_MATCH_SWAP_COLOR. myColor = "WHITE" | "BLACK". */
    public void reset(String myColor) {
        this.myColor = myColor;
        // TODO(N11): bàn cờ chuẩn, xoay khi cầm Đen, xóa biên bản, đồng hồ 30s, mở khóa nếu cầm Trắng
    }

    /** MatchPlayFrm chuyển tiếp SERVER_INFORM_MOVE và REPLY_MOVE_ILLEGAL vào đây. */
    public void onServerMessage(ObjectWrapper data) {
        // TODO(N11)
    }

    /** true khi nhận SERVER_END_GAME: khóa bàn VÀ dừng đồng hồ đếm ngược. */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
