package client.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import client.view.adapter.RankingAdapter;
import model.ObjectWrapper;
import model.PlayerRanking;
import ptit.e23cnpm01.nguyentiendung.covua.R;

/** Chủ: N04. Xem bảng xếp hạng toàn bộ người chơi (Hình 4.1, 4.8). */
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

        listen(ObjectWrapper.REPLY_RANKING);
        myControl.sendData(new ObjectWrapper(ObjectWrapper.GET_RANKING, null));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnClose) {
            finish();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void receivedDataProcessing(ObjectWrapper d) {
        if (d.getPerformative() != ObjectWrapper.REPLY_RANKING) {
            return;
        }
        ArrayList<PlayerRanking> list = d.getData() instanceof ArrayList<?>
                ? (ArrayList<PlayerRanking>) d.getData() : new ArrayList<>();
        adapter.setRankings(list);
        boolean empty = list.isEmpty();
        lblEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        tblRanking.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
