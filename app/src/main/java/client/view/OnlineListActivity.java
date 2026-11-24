package client.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import client.view.adapter.OnlinePlayerAdapter;
import client.view.ai.AISetupDialog;
import model.ObjectWrapper;
import model.Player;
import model.PlayerStat;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Danh sách người chơi trực tuyến & điều hướng (Hình 4.1, 4.9, 4.11). */
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmLogout();
            }
        });

        listen(ObjectWrapper.REPLY_ONLINE_LIST, ObjectWrapper.SERVER_INFORM_ONLINE_LIST);
        myControl.sendData(new ObjectWrapper(ObjectWrapper.GET_ONLINE_LIST, null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Player me = myControl.getMyPlayer();
        if (me != null) {
            String display = me.getName() != null && !me.getName().isEmpty() ? me.getName() : me.getUsername();
            lblUserInfo.setText(getString(R.string.greeting, display));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnEditProfile) {
            startActivity(new Intent(this, EditPlayerActivity.class));
        } else if (id == R.id.btnChangePass) {
            startActivity(new Intent(this, ChangePasswordActivity.class));
        } else if (id == R.id.btnHistory) {
            startActivity(new Intent(this, PlayerHistoryActivity.class));
        } else if (id == R.id.btnRanking) {
            startActivity(new Intent(this, RankingActivity.class));
        } else if (id == R.id.btnPlayAI) {
            AISetupDialog.open(this);
        } else if (id == R.id.btnLogout) {
            confirmLogout();
        } else if (id == R.id.btnChallenge) {
            PlayerStat target = adapter.getSelectedPlayer();
            if (target == null) {
                toast("Vui lòng chọn một người chơi RẢNH");
                return;
            }
            if (!PlayerStat.IDLE.equals(target.getStatus())) {
                toast("Người chơi này đang bận");
                return;
            }
            if (!getSupportFragmentManager().isStateSaved()
                    && getSupportFragmentManager().findFragmentByTag(ChallengeDialog.TAG) == null) {
                ChallengeDialog.newInstance(target).show(getSupportFragmentManager(), ChallengeDialog.TAG);
            }
        }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_logout)
                .setMessage("Bạn muốn đăng xuất khỏi hệ thống?")
                .setPositiveButton("Đồng ý", (d, w) -> {
                    myControl.closeConnection();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @SuppressWarnings("unchecked")
    public void updateOnlineTable(ArrayList<PlayerStat> list) {
        Player me = myControl.getMyPlayer();
        int myId = me != null ? me.getId() : -1;
        ArrayList<PlayerStat> others = new ArrayList<>();
        if (list != null) {
            for (PlayerStat p : list) {
                if (p.getId() != myId) {
                    others.add(p);
                }
            }
        }
        adapter.updateList(others);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void receivedDataProcessing(ObjectWrapper d) {
        int code = d.getPerformative();
        if (code == ObjectWrapper.REPLY_ONLINE_LIST || code == ObjectWrapper.SERVER_INFORM_ONLINE_LIST) {
            if (d.getData() instanceof ArrayList<?>) {
                updateOnlineTable((ArrayList<PlayerStat>) d.getData());
            }
        }
    }
}
