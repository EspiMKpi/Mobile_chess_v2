package client.view;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Bảng xếp hạng [LỆCH BC: bổ sung theo đề]. Nhận REPLY_RANKING (ArrayList<PlayerRanking>, đã sắp).
 * Cột: Hạng / Tên / Tổng điểm / TB điểm đối thủ / TB nước thắng / TB nước thua (giá trị -1 hiện "-").
 */
public class RankingActivity extends BaseActivity {

    private RecyclerView tblRanking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_screen);
        setTitle(R.string.title_ranking);
        listen(ObjectWrapper.REPLY_RANKING);

        tblRanking = findViewById(R.id.recyclerList);
        tblRanking.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        myControl.sendData(new ObjectWrapper(ObjectWrapper.GET_RANKING, null));
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M5): RankingAdapter (item_ranking.xml)
    }
}
