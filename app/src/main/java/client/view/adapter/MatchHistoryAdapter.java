package client.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import model.Match;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Adapter hiển thị lịch sử đấu trong {@link client.view.PlayerHistoryActivity}.
 */
public class MatchHistoryAdapter extends RecyclerView.Adapter<MatchHistoryAdapter.ViewHolder> {

    private final List<Match> matches = new ArrayList<>();
    private final int myPlayerId;

    public MatchHistoryAdapter(int myPlayerId) {
        this.myPlayerId = myPlayerId;
    }

    public void setMatches(List<Match> list) {
        matches.clear();
        if (list != null) {
            matches.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // TODO(N04 - Hình 4.1 & Mục 1.3):
        //  - Từ Match m, tìm MatchResult của mình (player.id == myPlayerId) và MatchResult của đối thủ.
        //  - Định dạng startTime - endTime (dd/MM/yyyy HH:mm).
        //  - Hiển thị: "#<position+1>", thời gian, "Đối thủ: <username>", "Quân: TRẮNG/ĐEN",
        //    "Kết quả: THẮNG/HÒA/THUA", "Điểm: 1.0 / 0.5 / 0.0".
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtMatchIndex;
        final TextView txtMatchTime;
        final TextView txtOpponentInfo;
        final TextView txtColorInfo;
        final TextView txtResultInfo;
        final TextView txtPointInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMatchIndex = itemView.findViewById(R.id.txtMatchIndex);
            txtMatchTime = itemView.findViewById(R.id.txtMatchTime);
            txtOpponentInfo = itemView.findViewById(R.id.txtOpponentInfo);
            txtColorInfo = itemView.findViewById(R.id.txtColorInfo);
            txtResultInfo = itemView.findViewById(R.id.txtResultInfo);
            txtPointInfo = itemView.findViewById(R.id.txtPointInfo);
        }
    }
}
