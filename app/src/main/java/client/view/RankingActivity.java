package client.view;

import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import client.view.adapter.RankingAdapter;
import model.ObjectWrapper;
import model.PlayerRanking;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Xem bảng xếp hạng (hình 4.8). Nhận REPLY_RANKING (ArrayList<PlayerRanking>, server đã sắp).
 * Cột: Hạng / Tên / Tổng điểm / TB điểm đối thủ / TB nước thắng / TB nước thua (-1 hiện "-").
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
        // TODO: bước 3 - listen(REPLY_RANKING)
        // TODO: findViewById (recyclerList, txtEmpty, btnClose), adapter = new RankingAdapter()
        // TODO: bước 4 - sendData(GET_RANKING, null)
    }

    /** Bước 14: danh sách rỗng -> hiện txtEmpty; ngược lại adapter.setRankings(list). */
    private void showRanking(ArrayList<PlayerRanking> list) {
        // TODO
    }

    /** Bước 13: nhận REPLY_RANKING -> showRanking(list). */
    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO
    }
}
