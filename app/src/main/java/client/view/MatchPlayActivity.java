package client.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import model.GameResult;
import model.ObjectWrapper;
import model.PlayerStat;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Phòng thi đấu đối kháng (hình 4.9). Bàn cờ là ChessBoardView của N11.
 * Trên: thông tin đối thủ · giữa: ChessBoardView · dưới: màu quân của mình + [Thoát ván].
 * Nhận SERVER_END_GAME, SERVER_RESTART_MATCH_SWAP_COLOR, SERVER_END_SESSION_MATCH
 * và chuyển tiếp SERVER_INFORM_MOVE, REPLY_MOVE_ILLEGAL cho boardView.
 * Extras: EXTRA_MY_COLOR ("WHITE" | "BLACK"), EXTRA_OPPONENT (PlayerStat).
 */
public class MatchPlayActivity extends BaseActivity {

    public static final String EXTRA_MY_COLOR = "myColor";
    public static final String EXTRA_OPPONENT = "opponent";

    private TextView lblOpponentInfo;
    private TextView lblMyColor;
    private Button btnQuit;
    private ChessBoardView boardView;

    private String myColor;
    private PlayerStat opponent;
    private boolean inGame = true;
    private GameResult pendingResult;   // kết quả nhận được lúc Activity đang ở nền

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_play);
        setTitle(R.string.title_match);
        // TODO: đọc EXTRA_MY_COLOR, EXTRA_OPPONENT; findViewById
        // TODO: hình 4.12 bước 15 / 21 - listen(SERVER_END_GAME, SERVER_RESTART_MATCH_SWAP_COLOR,
        //       SERVER_END_SESSION_MATCH, SERVER_INFORM_MOVE, REPLY_MOVE_ILLEGAL)
        // TODO: btnQuit và nút Back -> quit()
        // TODO: bước 16 / 22 - updateLabels(); boardView.reset(myColor); myControl.flushPendingBoardMessages(this)
    }

    /** Hiện tên + điểm + màu quân của đối thủ và màu quân của mình. */
    private void updateLabels() {
        // TODO
    }

    /** Hình 4.13 bước 1-2: đang chơi -> confirm(...) -> sendData(QUIT_GAME, null). Ván đã kết thúc -> bỏ qua. */
    private void quit() {
        // TODO
    }

    /** Hình 4.14 bước 16-17 / 21-22: đổi myColor, inGame = true, đóng ContinueDialog, updateLabels(), boardView.reset(myColor). */
    public void swapColor(String newColor) {
        // TODO
    }

    /** Hình 4.13 bước 16: ContinueDialog.newInstance(result).show(...). Activity đang ở nền -> cất vào pendingResult. */
    private void showContinueDialog(GameResult r) {
        // TODO
    }

    /** Hiện ContinueDialog còn treo (pendingResult) khi quay lại màn hình. */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        // TODO
    }

    private void dismissContinueDialog() {
        // TODO
    }

    /**
     * - SERVER_INFORM_MOVE / REPLY_MOVE_ILLEGAL -> boardView.onServerMessage(data)
     * - SERVER_END_GAME -> inGame = false, boardView.setLocked(true), showContinueDialog(result)
     * - SERVER_RESTART_MATCH_SWAP_COLOR -> swapColor(màu mới)
     * - SERVER_END_SESSION_MATCH -> đóng dialog; "OPPONENT_STOP" -> "Đối thủ đã dừng chơi"; finish() về danh sách online
     */
    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO
    }
}
