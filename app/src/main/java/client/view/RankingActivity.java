package client.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import client.view.adapter.RankingAdapter;
import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Xem bảng xếp hạng toàn bộ người chơi (Hình 4.1, 4.2a, 4.8).
 */
public class RankingActivity extends BaseActivity implements View.OnClickListener {

    private RecyclerView tblRanking;
    private TextView lblEmpty;
    private Button btnClose;
    private RankingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);
        setTitle(R.string.title_ranking);

        tblRanking = findViewById(R.id.tblRanking);
        lblEmpty = findViewById(R.id.lblEmpty);
        btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(this);

        tblRanking.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RankingAdapter();
        tblRanking.setAdapter(adapter);

        // [HƯỚNG DẪN - Hình 4.8, bước 3-4]
        // 1. myControl.register(ObjectWrapper.REPLY_RANKING, this);
        // 2. myControl.sendData(new ObjectWrapper(ObjectWrapper.GET_RANKING, null));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnClose) {
            finish();
        }
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        // [HƯỚNG DẪN - Hình 4.8, bước 13-14]
        // Nếu REPLY_RANKING -> cập nhật adapter.setRankings((ArrayList<PlayerRanking>) d.getData());
        // và bật/tắt lblEmpty tương ứng.
    }
}
