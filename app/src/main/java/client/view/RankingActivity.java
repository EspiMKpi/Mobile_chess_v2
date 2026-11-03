package client.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import client.view.adapter.RankingAdapter;
import model.ObjectWrapper;
import model.PlayerRanking;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Bảng xếp hạng [LỆCH BC: bổ sung theo đề]. Nhận REPLY_RANKING (ArrayList<PlayerRanking>, đã sắp).
 * Cột: Hạng / Tên / Tổng điểm / TB điểm đối thủ / TB nước thắng / TB nước thua (giá trị -1 hiện "-").
 */
public class RankingActivity extends BaseActivity {

    private RecyclerView tblRanking;
    private TextView txtEmpty;
    private RankingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_screen);
        setTitle(R.string.title_ranking);
        listen(ObjectWrapper.REPLY_RANKING);

        tblRanking = findViewById(R.id.recyclerList);
        tblRanking.setLayoutManager(new LinearLayoutManager(this));
        txtEmpty = findViewById(R.id.txtEmpty);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        adapter = new RankingAdapter();
        tblRanking.setAdapter(adapter);

        myControl.sendData(new ObjectWrapper(ObjectWrapper.GET_RANKING, null));
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        if (data.getPerformative() == ObjectWrapper.REPLY_RANKING) {
            if (data.getData() instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<PlayerRanking> list = (ArrayList<PlayerRanking>) data.getData();
                if (list == null || list.isEmpty()) {
                    txtEmpty.setText("Chưa có dữ liệu bảng xếp hạng");
                    txtEmpty.setVisibility(View.VISIBLE);
                    tblRanking.setVisibility(View.GONE);
                } else {
                    txtEmpty.setVisibility(View.GONE);
                    tblRanking.setVisibility(View.VISIBLE);
                    adapter.setRankings(list);
                }
            } else {
                txtEmpty.setText("Chưa có dữ liệu bảng xếp hạng");
                txtEmpty.setVisibility(View.VISIBLE);
                tblRanking.setVisibility(View.GONE);
            }
        }
    }
}
