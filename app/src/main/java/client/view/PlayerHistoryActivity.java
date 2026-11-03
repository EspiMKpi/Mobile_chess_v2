package client.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import client.view.adapter.MatchHistoryAdapter;
import model.Match;
import model.ObjectWrapper;
import model.Player;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Hình 2.5. Nhận REPLY_PLAYER_HISTORY (ArrayList<Match>). */
public class PlayerHistoryActivity extends BaseActivity {

    private RecyclerView tblHistory;
    private TextView txtEmpty;
    private MatchHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_screen);
        setTitle(R.string.title_history);
        listen(ObjectWrapper.REPLY_PLAYER_HISTORY);

        tblHistory = findViewById(R.id.recyclerList);
        tblHistory.setLayoutManager(new LinearLayoutManager(this));
        txtEmpty = findViewById(R.id.txtEmpty);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        Player me = myControl.getMyPlayer();
        int myId = (me != null) ? me.getId() : -1;
        adapter = new MatchHistoryAdapter(myId);
        tblHistory.setAdapter(adapter);

        myControl.sendData(new ObjectWrapper(ObjectWrapper.GET_PLAYER_HISTORY, null));
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        if (data.getPerformative() == ObjectWrapper.REPLY_PLAYER_HISTORY) {
            if (data.getData() instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<Match> list = (ArrayList<Match>) data.getData();
                if (list == null || list.isEmpty()) {
                    txtEmpty.setVisibility(View.VISIBLE);
                    tblHistory.setVisibility(View.GONE);
                } else {
                    txtEmpty.setVisibility(View.GONE);
                    tblHistory.setVisibility(View.VISIBLE);
                    adapter.setMatches(list);
                }
            } else {
                txtEmpty.setVisibility(View.VISIBLE);
                tblHistory.setVisibility(View.GONE);
            }
        }
    }
}
