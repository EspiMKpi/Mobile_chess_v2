package client.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import client.view.ai.AISetupDialog;
import model.ObjectWrapper;
import model.Player;
import model.PlayerStat;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Hình 3.1. Nhận REPLY_ONLINE_LIST, SERVER_INFORM_ONLINE_LIST, SERVER_INFORM_CHALLENGE.
 * Lời mời thách đấu được hiển thị trên ClientCtr.getCurrentActivity() (người chơi có thể đang ở màn hình khác).
 */
public class OnlineListActivity extends BaseActivity {

    private TextView lblUserInfo;
    private RecyclerView tblOnlineUsers;
    private PlayerStat selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_list);
        setTitle(R.string.title_online);
        listen(ObjectWrapper.REPLY_ONLINE_LIST, ObjectWrapper.SERVER_INFORM_ONLINE_LIST,
                ObjectWrapper.SERVER_INFORM_CHALLENGE);

        lblUserInfo = findViewById(R.id.lblUserInfo);
        tblOnlineUsers = findViewById(R.id.tblOnlineUsers);
        tblOnlineUsers.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnEditProfile).setOnClickListener(
                v -> startActivity(new Intent(this, EditPlayerActivity.class)));
        findViewById(R.id.btnChangePass).setOnClickListener(
                v -> startActivity(new Intent(this, ChangePasswordActivity.class)));
        findViewById(R.id.btnHistory).setOnClickListener(
                v -> startActivity(new Intent(this, PlayerHistoryActivity.class)));
        findViewById(R.id.btnRanking).setOnClickListener(
                v -> startActivity(new Intent(this, RankingActivity.class)));
        findViewById(R.id.btnPlayAI).setOnClickListener(v -> AISetupDialog.open(this));   // lớp của N11
        findViewById(R.id.btnLogout).setOnClickListener(v -> confirm("Đăng xuất?", this::logout));
        findViewById(R.id.btnChallenge).setOnClickListener(v -> challenge());

        myControl.sendData(new ObjectWrapper(ObjectWrapper.GET_ONLINE_LIST, null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Player me = myControl.getMyPlayer();
        if (me != null) {
            lblUserInfo.setText(getString(R.string.greeting, me.getName()));
        }
    }

    /** Bỏ chính mình; giữ dòng đang chọn theo id sau khi làm mới. */
    public void updateOnlineTable(ArrayList<PlayerStat> list) {
        // TODO(N04, M3): OnlinePlayerAdapter (item_online_player.xml: STT/Tên/Điểm/Trạng thái)
    }

    private void challenge() {
        // TODO(N04, M4): selected phải RẢNH -> ChallengeDialog.newInstance(selected).show(getSupportFragmentManager(), ChallengeDialog.TAG)
    }

    private void logout() {
        // TODO(N04, M2): myControl.closeConnection(); về ClientMainActivity; finish()
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M3/M4): REPLY_ONLINE_LIST, SERVER_INFORM_ONLINE_LIST -> updateOnlineTable(...)
        //   SERVER_INFORM_CHALLENGE -> InviteDialog.newInstance(inviter) trên myControl.getCurrentActivity();
        //   không có Activity nào đang hiện -> sendData(REPLY_CHALLENGE_DECISION, false)
    }
}
