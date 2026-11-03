package client.view;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Hình 2.5. Nhận REPLY_PLAYER_HISTORY (ArrayList<Match>). */
public class PlayerHistoryActivity extends BaseActivity {

    private RecyclerView tblHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_screen);
        setTitle(R.string.title_history);
        listen(ObjectWrapper.REPLY_PLAYER_HISTORY);

        tblHistory = findViewById(R.id.recyclerList);
        tblHistory.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        myControl.sendData(new ObjectWrapper(ObjectWrapper.GET_PLAYER_HISTORY, null));
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M5): MatchHistoryAdapter (item_match_history.xml - thẻ 3 dòng như hình 2.5);
        //   rỗng -> hiện txtEmpty "Chưa có trận đấu nào"
    }
}
