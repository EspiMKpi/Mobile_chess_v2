package client.view;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;

import model.GameResult;
import model.MatchResult;
import model.ObjectWrapper;
import model.Player;
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
    private boolean inGame = true;
    private GameResult pendingResult;   // kết quả nhận được lúc Activity đang ở nền

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_play);
        setTitle(R.string.title_match);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        myColor = getIntent().getStringExtra(EXTRA_MY_COLOR);
        if (myColor == null) {
            myColor = MatchResult.WHITE;
        }
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

        updateLabels();
        boardView.reset(myColor);
        myControl.flushPendingBoardMessages(this);
    }

    private void updateLabels() {
        String oppName = (opponent != null)
                ? (opponent.getName() != null && !opponent.getName().isEmpty() ? opponent.getName() : opponent.getUsername())
                : "Đối thủ";
        float oppPt = (opponent != null) ? opponent.getTotalPoint() : 0f;
        String oppColor = MatchResult.WHITE.equalsIgnoreCase(myColor) ? "ĐEN" : "TRẮNG";
        lblOpponentInfo.setText(String.format("Đối thủ: %s (Điểm: %.1f) | Quân đối thủ: %s", oppName, oppPt, oppColor));

        Player me = myControl.getMyPlayer();
        String myName = (me != null) ? me.getName() : "Bạn";
        String myColorDetail = MatchResult.WHITE.equalsIgnoreCase(myColor) ? "TRẮNG (Đi trước)" : "ĐEN (Đi sau)";
        lblMyColor.setText(String.format("Bạn: %s | Quân của bạn: %s", myName, myColorDetail));
    }

    /** Đang chơi -> xác nhận -> sendData(QUIT_GAME, null); ván đã kết thúc -> bỏ qua (chờ ContinueDialog). */
    private void quit() {
        if (inGame) {
            confirm("Bạn có chắc chắn muốn xin thua và thoát ván đấu?", () -> {
                myControl.sendData(new ObjectWrapper(ObjectWrapper.QUIT_GAME, null));
            });
        }
    }

    /** SERVER_RESTART_MATCH_SWAP_COLOR: đổi màu, cập nhật nhãn, đóng ContinueDialog, boardView.reset(myColor). */
    public void swapColor(String newColor) {
        this.myColor = newColor;
        this.inGame = true;
        this.pendingResult = null;
        dismissContinueDialog();
        updateLabels();
        boardView.reset(myColor);
        myControl.flushPendingBoardMessages(this);
    }

    private void showContinueDialog(GameResult r) {
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) {        // đang ở nền -> để dành, hiện khi quay lại
            pendingResult = r;
            return;
        }
        if (fm.findFragmentByTag(ContinueDialog.TAG) == null) {
            ContinueDialog.newInstance(r).show(fm, ContinueDialog.TAG);
        }
    }

    @Override
    protected void onPostResume() {     // FragmentManager đã sẵn sàng nhận transaction ở đây
        super.onPostResume();
        if (pendingResult != null && !isFinishing()) {
            GameResult r = pendingResult;
            pendingResult = null;
            showContinueDialog(r);
        }
    }

    private void dismissContinueDialog() {
        Fragment dialog = getSupportFragmentManager().findFragmentByTag(ContinueDialog.TAG);
        if (dialog instanceof ContinueDialog) {
            ((ContinueDialog) dialog).dismissAllowingStateLoss();
        }
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        switch (data.getPerformative()) {
            case ObjectWrapper.SERVER_INFORM_MOVE:
            case ObjectWrapper.REPLY_MOVE_ILLEGAL:
                boardView.onServerMessage(data);
                break;

            case ObjectWrapper.SERVER_END_GAME:
                inGame = false;
                boardView.setLocked(true);
                if (data.getData() instanceof GameResult) {
                    showContinueDialog((GameResult) data.getData());
                }
                break;

            case ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR:
                if (data.getData() instanceof String) {
                    swapColor((String) data.getData());
                }
                break;

            case ObjectWrapper.SERVER_END_SESSION_MATCH:
                pendingResult = null;
                dismissContinueDialog();
                if ("OPPONENT_STOP".equals(data.getData())) {
                    toast("Đối thủ đã dừng chơi");
                }
                finish();
                break;

            default:
                break;
        }
    }
}
