package client.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import client.view.adapter.MatchHistoryAdapter;
import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Xem lịch sử các ván đấu đã chơi (Hình 4.1, 4.2a, 4.7).
 */
public class PlayerHistoryActivity extends BaseActivity implements View.OnClickListener {

    private RecyclerView tblHistory;
    private TextView lblEmpty;
    private Button btnClose;
    private MatchHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_history);
        setTitle(R.string.title_history);

        tblHistory = findViewById(R.id.tblHistory);
        lblEmpty = findViewById(R.id.lblEmpty);
        btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(this);

        tblHistory.setLayoutManager(new LinearLayoutManager(this));
        // [HƯỚNG DẪN - Hình 4.7, bước 3-4]
        // 1. Tạo adapter = new MatchHistoryAdapter(myControl.getMyPlayer().getId()); tblHistory.setAdapter(adapter);
        // 2. myControl.register(ObjectWrapper.REPLY_PLAYER_HISTORY, this);
        // 3. myControl.sendData(new ObjectWrapper(ObjectWrapper.GET_PLAYER_HISTORY, null));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnClose) {
            finish();
        }
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        // [HƯỚNG DẪN - Hình 4.7, bước 14-15]
        // Nếu REPLY_PLAYER_HISTORY:
        //  - Đổ ArrayList<Match> vào adapter.setMatches(...).
        //  - Nếu danh sách rỗng -> hiện lblEmpty, ẩn tblHistory; ngược lại ẩn lblEmpty, hiện tblHistory.
    }
}
