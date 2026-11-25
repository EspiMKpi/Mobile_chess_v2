package client.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import client.view.adapter.MatchHistoryAdapter;
import model.Match;
import model.ObjectWrapper;
import model.Player;
import ptit.e23cnpm01.nguyentiendung.covua.R;

/** Chủ: N04. Xem lịch sử các trận đấu (Hình 4.1, 4.7). */
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

        Player me = myControl.getMyPlayer();
        int myId = me != null ? me.getId() : -1;
        tblHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MatchHistoryAdapter(myId);
        tblHistory.setAdapter(adapter);

        listen(ObjectWrapper.REPLY_PLAYER_HISTORY);
        myControl.sendData(new ObjectWrapper(ObjectWrapper.GET_PLAYER_HISTORY, null));
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
        if (d.getPerformative() != ObjectWrapper.REPLY_PLAYER_HISTORY) {
            return;
        }
        ArrayList<Match> matches = d.getData() instanceof ArrayList<?> ? (ArrayList<Match>) d.getData() : new ArrayList<>();
        adapter.setMatches(matches);
        boolean empty = matches.isEmpty();
        lblEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        tblHistory.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
