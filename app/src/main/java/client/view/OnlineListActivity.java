package client.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import client.view.adapter.OnlinePlayerAdapter;
import model.ObjectWrapper;
import model.PlayerStat;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Màn hình chính sau khi đăng nhập: danh sách người chơi trực tuyến & điều hướng (Hình 4.1, 4.9, 4.11).
 */
public class OnlineListActivity extends BaseActivity implements View.OnClickListener {

    private TextView lblUserInfo;
    private RecyclerView tblOnlineUsers;
    private Button btnEditProfile;
    private Button btnChangePass;
    private Button btnHistory;
    private Button btnRanking;
    private Button btnPlayAI;
    private Button btnLogout;
    private Button btnChallenge;
    private OnlinePlayerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_list);
        setTitle(R.string.title_online);

        lblUserInfo = findViewById(R.id.lblUserInfo);
        tblOnlineUsers = findViewById(R.id.tblOnlineUsers);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePass = findViewById(R.id.btnChangePass);
        btnHistory = findViewById(R.id.btnHistory);
        btnRanking = findViewById(R.id.btnRanking);
        btnPlayAI = findViewById(R.id.btnPlayAI);
        btnLogout = findViewById(R.id.btnLogout);
        btnChallenge = findViewById(R.id.btnChallenge);

        tblOnlineUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OnlinePlayerAdapter();
        tblOnlineUsers.setAdapter(adapter);

        btnEditProfile.setOnClickListener(this);
        btnChangePass.setOnClickListener(this);
        btnHistory.setOnClickListener(this);
        btnRanking.setOnClickListener(this);
        btnPlayAI.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnChallenge.setOnClickListener(this);

        // [HƯỚNG DẪN - Hình 4.11, bước 1-2]
        // 1. Đăng ký lắng nghe REPLY_ONLINE_LIST và SERVER_INFORM_ONLINE_LIST với myControl.register(...).
        //    (Lưu ý: SERVER_INFORM_CHALLENGE nên xử lý toàn cục ở ClientCtr để hiện InviteDialog trên mọi màn hình.)
        // 2. Gửi yêu cầu lấy danh sách ban đầu:
        //    myControl.sendData(new ObjectWrapper(ObjectWrapper.GET_ONLINE_LIST, null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // [HƯỚNG DẪN] Cập nhật lời chào "Xin chào: <tên>" từ myControl.getMyPlayer()
        // để khi sửa thông tin ở EditPlayerActivity quay về sẽ thấy tên mới ngay.
    }

    @Override
    public void onClick(View v) {
        // [HƯỚNG DẪN] Xử lý từng nút:
        // - btnEditProfile -> startActivity(EditPlayerActivity.class)
        // - btnChangePass  -> startActivity(ChangePasswordActivity.class)
        // - btnHistory     -> startActivity(PlayerHistoryActivity.class)
        // - btnRanking     -> startActivity(RankingActivity.class)
        // - btnPlayAI      -> client.view.ai.AISetupDialog.open(this);  // gọi sang phần N11
        // - btnLogout      -> hỏi xác nhận -> myControl.closeConnection() -> mở LoginActivity + finish()
        // - btnChallenge   -> lấy adapter.getSelectedPlayer(); kiểm tra != null và IDLE
        //                     -> ChallengeDialog.newInstance(target).show(getSupportFragmentManager(), ChallengeDialog.TAG)
    }

    /** Lọc bỏ chính mình rồi cập nhật bảng online (Hình 4.10a). */
    public void updateOnlineTable(ArrayList<PlayerStat> list) {
        // TODO(N04): lọc bỏ player có id == myControl.getMyPlayer().getId() rồi gọi adapter.updateList(...)
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        // TODO(N04 - Hình 4.11, bước 11-12):
        // Nếu performative là REPLY_ONLINE_LIST hoặc SERVER_INFORM_ONLINE_LIST:
        //   updateOnlineTable((ArrayList<PlayerStat>) d.getData());
    }
}
