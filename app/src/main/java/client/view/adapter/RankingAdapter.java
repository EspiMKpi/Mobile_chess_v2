package client.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import model.PlayerRanking;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Adapter hiển thị bảng xếp hạng trong {@link client.view.RankingActivity}.
 */
public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.ViewHolder> {

    private final List<PlayerRanking> rankings = new ArrayList<>();

    public void setRankings(List<PlayerRanking> list) {
        rankings.clear();
        if (list != null) {
            rankings.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ranking, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // TODO(N04 - Hình 4.1 & Mục 1.3):
        //  - Hiển thị "#<position+1> <username>", "Tổng điểm: <totalPoint>",
        //    "TB điểm đối thủ: <avgOpponentPoint>",
        //    "TB số nước thắng: <avgMovesWin < 0 ? '-' : ...>",
        //    "TB số nước thua: <avgMovesLose < 0 ? '-' : ...>".
    }

    @Override
    public int getItemCount() {
        return rankings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtRankName;
        final TextView txtTotalPoint;
        final TextView txtAvgOpponentPoint;
        final TextView txtAvgMovesWin;
        final TextView txtAvgMovesLose;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRankName = itemView.findViewById(R.id.txtRankName);
            txtTotalPoint = itemView.findViewById(R.id.txtTotalPoint);
            txtAvgOpponentPoint = itemView.findViewById(R.id.txtAvgOpponentPoint);
            txtAvgMovesWin = itemView.findViewById(R.id.txtAvgMovesWin);
            txtAvgMovesLose = itemView.findViewById(R.id.txtAvgMovesLose);
        }
    }
}
