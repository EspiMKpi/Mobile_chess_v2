package client.view;

import android.os.Bundle;
import android.widget.TextView;

import model.MatchStartInfo;
import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/**
 * [PHẦN N11] Màn hình phòng thi đấu đối kháng.
 *
 * N04 mở Activity này khi nhận SERVER_START_MATCH và truyền {@link MatchStartInfo}
 * qua Intent extra {@link #EXTRA_START_INFO}.
 * Từ đây trở đi (vẽ bàn cờ, đồng hồ 30s, gửi SEND_MOVE, nhận SERVER_INFORM_MOVE,
 * xin thua QUIT_GAME, dialog kết thúc & chơi tiếp) do thành viên N11 đảm nhiệm.
 * Khi trận đấu kết thúc hẳn (Dừng chơi hoặc đối thủ dừng), N11 chỉ cần gọi finish()
 * để quay lại OnlineListActivity của N04 bên dưới.
 */
public class MatchPlayActivity extends BaseActivity {

    public static final String EXTRA_START_INFO = "start_info";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.n11_activity_match_play);
        setTitle(R.string.n11_title_match);

        TextView lblMatchInfo = findViewById(R.id.lblMatchInfo);
        MatchStartInfo info = (MatchStartInfo) getIntent().getSerializableExtra(EXTRA_START_INFO);
        if (info != null && info.getOpponent() != null) {
            lblMatchInfo.setText("Phòng thi đấu (N11)\nQuân của bạn: " + info.getMyColor()
                    + "\nĐối thủ: " + info.getOpponent().getUsername());
        } else {
            lblMatchInfo.setText("Phòng thi đấu (N11 sẽ hoàn thiện)");
        }
        // TODO(N11): đăng ký nhận các mã 21, 23, 24, 31, 32 rồi gọi myControl.flushPendingMatchMessages(this)
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N11)
    }
}
