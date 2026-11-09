package client.view;

import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import client.view.adapter.MatchHistoryAdapter;
import model.Match;
import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Xem lịch sử đấu (hình 4.7). Nhận REPLY_PLAYER_HISTORY (ArrayList<Match>). */
public class PlayerHistoryActivity extends BaseActivity {

    private RecyclerView tblHistory;
    private TextView txtEmpty;
    private MatchHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_screen);
        setTitle(R.string.title_history);
        // TODO: bước 3 - listen(REPLY_PLAYER_HISTORY)
        // TODO: findViewById (recyclerList, txtEmpty, btnClose), adapter = new MatchHistoryAdapter(myId)
        // TODO: bước 4 - sendData(GET_PLAYER_HISTORY, null)
    }

    /** Bước 15: danh sách rỗng -> hiện txtEmpty; ngược lại adapter.setMatches(list). */
    private void showHistory(ArrayList<Match> list) {
        // TODO
    }

    /** Bước 14: nhận REPLY_PLAYER_HISTORY -> showHistory(list). */
    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO
    }
}
