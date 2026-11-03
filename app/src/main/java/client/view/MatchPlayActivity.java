package client.view;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import model.ObjectWrapper;
import model.PlayerStat;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Khung phòng đấu (hình 4.3) [LỆCH BC: bàn cờ tách sang ChessBoardView của N11].
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_play);
        setTitle(R.string.title_match);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        myColor = getIntent().getStringExtra(EXTRA_MY_COLOR);
        opponent = (PlayerStat) getIntent().getSerializableExtra(EXTRA_OPPONENT);

        lblOpponentInfo = findViewById(R.id.lblOpponentInfo);
        lblMyColor = findViewById(R.id.lblMyColor);
        btnQuit = findViewById(R.id.btnQuit);
        boardView = findViewById(R.id.boardView);

        listen(ObjectWrapper.SERVER_END_GAME, ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR,
                ObjectWrapper.SERVER_END_SESSION_MATCH,
                ObjectWrapper.SERVER_INFORM_MOVE, ObjectWrapper.REPLY_MOVE_ILLEGAL);

        btnQuit.setOnClickListener(v -> quit());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                quit();
            }
        });

        // TODO(N04, M4): cập nhật lblOpponentInfo, lblMyColor theo opponent, myColor
        boardView.reset(myColor);
    }

    /** Đang chơi -> xác nhận -> sendData(QUIT_GAME, null); ván đã kết thúc -> bỏ qua (chờ ContinueDialog). */
    private void quit() {
        // TODO(N04, M4)
    }

    /** SERVER_RESTART_MATCH_SWAP_COLOR: đổi màu, cập nhật nhãn, đóng ContinueDialog, boardView.reset(myColor). */
    public void swapColor(String newColor) {
        // TODO(N04, M4)
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        switch (data.getPerformative()) {
            case ObjectWrapper.SERVER_INFORM_MOVE:
            case ObjectWrapper.REPLY_MOVE_ILLEGAL:
                boardView.onServerMessage(data);
                break;
            default:
                // TODO(N04, M4): SERVER_END_GAME -> boardView.setLocked(true); ContinueDialog.newInstance(GameResult).show(...)
                //   SERVER_RESTART_MATCH_SWAP_COLOR -> swapColor((String) data.getData())
                //   SERVER_END_SESSION_MATCH -> "OPPONENT_STOP" thì toast; đóng ContinueDialog; finish()
                break;
        }
    }
}
