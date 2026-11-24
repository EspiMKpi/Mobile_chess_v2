package client.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import model.PlayerRanking;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Adapter bảng xếp hạng toàn bộ người chơi. */
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
        PlayerRanking r = rankings.get(position);
        holder.txtRankName.setText("#" + (position + 1) + " " + r.getUsername());
        holder.txtTotalPoint.setText(String.format(Locale.US, "Tổng điểm: %.1f", r.getTotalPoint()));
        holder.txtAvgOpponentPoint.setText(String.format(Locale.US, "TB điểm đối thủ: %.1f", r.getAvgOpponentPoint()));
        holder.txtAvgMovesWin.setText("TB số nước thắng: "
                + (r.getAvgMovesWin() < 0 ? "-" : String.format(Locale.US, "%.1f", r.getAvgMovesWin())));
        holder.txtAvgMovesLose.setText("TB số nước thua: "
                + (r.getAvgMovesLose() < 0 ? "-" : String.format(Locale.US, "%.1f", r.getAvgMovesLose())));
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
