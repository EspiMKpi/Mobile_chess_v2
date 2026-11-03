package client.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import client.view.adapter.OnlinePlayerAdapter;
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
    private OnlinePlayerAdapter adapter;

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

        adapter = new OnlinePlayerAdapter(player -> {
            // Callback khi người dùng chạm chọn 1 người chơi
        });
        tblOnlineUsers.setAdapter(adapter);

        findViewById(R.id.btnEditProfile).setOnClickListener(
                v -> startActivity(new Intent(this, EditPlayerActivity.class)));
        findViewById(R.id.btnChangePass).setOnClickListener(
                v -> startActivity(new Intent(this, ChangePasswordActivity.class)));
        findViewById(R.id.btnHistory).setOnClickListener(
                v -> startActivity(new Intent(this, PlayerHistoryActivity.class)));
        findViewById(R.id.btnRanking).setOnClickListener(
                v -> startActivity(new Intent(this, RankingActivity.class)));
        findViewById(R.id.btnPlayAI).setOnClickListener(v -> AISetupDialog.open(this));   // lớp của N11
        findViewById(R.id.btnLogout).setOnClickListener(v -> confirm("Bạn có chắc chắn muốn đăng xuất?", this::logout));
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
        if (list == null) return;
        Player me = myControl.getMyPlayer();
        int myId = (me != null) ? me.getId() : -1;

        List<PlayerStat> filtered = new ArrayList<>();
        for (PlayerStat p : list) {
            if (p != null && p.getId() != myId) {
                filtered.add(p);
            }
        }

        PlayerStat currentSelected = adapter.getSelected();
        int keepSelectedId = (currentSelected != null) ? currentSelected.getId() : -1;

        adapter.setPlayers(filtered);

        if (keepSelectedId != -1) {
            adapter.setSelectedId(keepSelectedId);
        }
    }

    private void challenge() {
        PlayerStat selected = adapter.getSelected();
        if (selected == null) {
            toast("Chạm chọn một người chơi để thách đấu");
            return;
        }
        if (PlayerStat.BUSY.equalsIgnoreCase(selected.getStatus())) {
            toast("Người chơi này đang bận");
            return;
        }

        ChallengeDialog.newInstance(selected).show(getSupportFragmentManager(), ChallengeDialog.TAG);
    }

    private void logout() {
        myControl.closeConnection();
        Intent intent = new Intent(this, ClientMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        switch (data.getPerformative()) {
            case ObjectWrapper.REPLY_ONLINE_LIST:
            case ObjectWrapper.SERVER_INFORM_ONLINE_LIST:
                if (data.getData() instanceof ArrayList) {
                    @SuppressWarnings("unchecked")
                    ArrayList<PlayerStat> list = (ArrayList<PlayerStat>) data.getData();
                    updateOnlineTable(list);
                }
                break;

            case ObjectWrapper.SERVER_INFORM_CHALLENGE:
                if (data.getData() instanceof PlayerStat) {
                    PlayerStat inviter = (PlayerStat) data.getData();
                    Activity cur = myControl.getCurrentActivity();
                    if (cur instanceof AppCompatActivity) {
                        InviteDialog.newInstance(inviter)
                                .show(((AppCompatActivity) cur).getSupportFragmentManager(), InviteDialog.TAG);
                    } else {
                        // Không có activity nào đang hiển thị -> từ chối ngay
                        myControl.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_DECISION, false));
                    }
                }
                break;

            default:
                break;
        }
    }
}
