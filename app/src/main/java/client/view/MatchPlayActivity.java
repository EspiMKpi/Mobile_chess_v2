package client.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import model.GameResult;
import model.MatchStartInfo;
import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/**
 * [PHẦN N11 THAY THẾ ĐỂ TỰ KIỂM THỬ TRÊN NHÁNH FULL]
 *
 * Khi N11 làm module đấu đối kháng, N11 sẽ thay layout và phần xử lý bàn cờ/đồng hồ vào đây.
 * Hiện tại màn hình này hiển thị thông tin bàn giao từ N04 ({@link #EXTRA_START_INFO}) và cho phép
 * giả lập xin thua / tiếp tục đổi màu / dừng chơi về OnlineListActivity.
 */
public class MatchPlayActivity extends BaseActivity implements View.OnClickListener {

    public static final String EXTRA_START_INFO = "start_info";

    private TextView lblMatchInfo;
    private Button btnSimulateResign;
    private LinearLayout layoutContinue;
    private TextView lblEndResult;
    private Button btnContinueMatch;
    private Button btnStopMatch;

    private String myColor = "";
    private String oppName = "";
    private float oppPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.n11_activity_match_play);
        setTitle(R.string.n11_title_match);

        lblMatchInfo = findViewById(R.id.lblMatchInfo);
        btnSimulateResign = findViewById(R.id.btnSimulateResign);
        layoutContinue = findViewById(R.id.layoutContinue);
        lblEndResult = findViewById(R.id.lblEndResult);
        btnContinueMatch = findViewById(R.id.btnContinueMatch);
        btnStopMatch = findViewById(R.id.btnStopMatch);

        btnSimulateResign.setOnClickListener(this);
        btnContinueMatch.setOnClickListener(this);
        btnStopMatch.setOnClickListener(this);

        MatchStartInfo info = (MatchStartInfo) getIntent().getSerializableExtra(EXTRA_START_INFO);
        if (info != null) {
            myColor = info.getMyColor() != null ? info.getMyColor() : "";
            if (info.getOpponent() != null) {
                oppName = info.getOpponent().getUsername();
                oppPoint = info.getOpponent().getTotalPoint();
            }
        }
        renderMatchHeader();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (layoutContinue.getVisibility() != View.VISIBLE) {
                    myControl.sendData(new ObjectWrapper(ObjectWrapper.QUIT_GAME, null));
                }
            }
        });

        listen(
                ObjectWrapper.SERVER_END_GAME,
                ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR,
                ObjectWrapper.SERVER_END_SESSION_MATCH,
                ObjectWrapper.REPLY_MOVE_ILLEGAL,
                ObjectWrapper.SERVER_INFORM_MOVE);
        myControl.flushPendingMatchMessages(this);
    }

    private void renderMatchHeader() {
        lblMatchInfo.setText("Phòng thi đấu (N11 sẽ vẽ bàn cờ tại đây)\n"
                + "Đối thủ: " + oppName + " (Điểm: " + oppPoint + ")\n"
                + "Quân của bạn: " + myColor);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSimulateResign) {
            btnSimulateResign.setEnabled(false);
            myControl.sendData(new ObjectWrapper(ObjectWrapper.QUIT_GAME, null));
        } else if (id == R.id.btnContinueMatch) {
            btnContinueMatch.setEnabled(false);
            btnStopMatch.setEnabled(false);
            lblEndResult.setText("Đang chờ đối thủ chọn...");
            myControl.sendData(new ObjectWrapper(ObjectWrapper.REQUEST_CONTINUE_MATCH, true));
        } else if (id == R.id.btnStopMatch) {
            btnContinueMatch.setEnabled(false);
            btnStopMatch.setEnabled(false);
            myControl.sendData(new ObjectWrapper(ObjectWrapper.REQUEST_CONTINUE_MATCH, false));
        }
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        int code = d.getPerformative();
        if (code == ObjectWrapper.SERVER_END_GAME && d.getData() instanceof GameResult) {
            GameResult gr = (GameResult) d.getData();
            btnSimulateResign.setVisibility(View.GONE);
            layoutContinue.setVisibility(View.VISIBLE);
            btnContinueMatch.setEnabled(true);
            btnStopMatch.setEnabled(true);
            lblEndResult.setText("Kết quả: " + gr.getResult() + " (" + gr.getReason()
                    + ") | Điểm: +" + gr.getPoint());
        } else if (code == ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR && d.getData() instanceof String) {
            myColor = (String) d.getData();
            renderMatchHeader();
            layoutContinue.setVisibility(View.GONE);
            btnSimulateResign.setVisibility(View.VISIBLE);
            btnSimulateResign.setEnabled(true);
        } else if (code == ObjectWrapper.SERVER_END_SESSION_MATCH) {
            if ("OPPONENT_STOP".equals(d.getData())) {
                toast("Đối thủ đã dừng chơi");
            }
            finish();
        }
    }
}
