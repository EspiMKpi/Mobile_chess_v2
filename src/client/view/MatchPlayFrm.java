package client.view;

import javax.swing.JFrame;

import client.control.ClientCtr;
import model.ObjectWrapper;
import model.PlayerStat;

/**
 * Chủ: N04. Khung phòng đấu (hình 4.5) [LỆCH BC: bàn cờ tách sang ChessBoardPanel của N11].
 * NORTH: thông tin đối thủ · CENTER: ChessBoardPanel · SOUTH: màu quân của mình + [Thoát ván].
 * Nhận SERVER_END_GAME, SERVER_RESTART_MATCH_SWAP_COLOR, SERVER_END_SESSION_MATCH
 * và chuyển tiếp SERVER_INFORM_MOVE, REPLY_MOVE_ILLEGAL cho boardPanel.
 */
public class MatchPlayFrm extends JFrame implements DataReceiver {

    private final ClientCtr myControl;
    private final ChessBoardPanel boardPanel;
    private final PlayerStat opponent;
    private String myColor;

    public MatchPlayFrm(ClientCtr myControl, String myColor, PlayerStat opponent) {
        super("Phòng đấu");
        this.myControl = myControl;
        this.myColor = myColor;
        this.opponent = opponent;
        this.boardPanel = new ChessBoardPanel(myControl);
        // TODO(N04, M4): dựng giao diện, đăng ký 5 mã, boardPanel.reset(myColor), ẩn OnlineListFrm
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        switch (data.getPerformative()) {
            case ObjectWrapper.SERVER_INFORM_MOVE:
            case ObjectWrapper.REPLY_MOVE_ILLEGAL:
                boardPanel.onServerMessage(data);
                break;
            default:
                // TODO(N04, M4): SERVER_END_GAME, SERVER_RESTART_MATCH_SWAP_COLOR, SERVER_END_SESSION_MATCH
                break;
        }
    }
}
