package client.view.adapter;

import android.graphics.Color;
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

/**
 * Chủ: N04. Adapter hiển thị Bảng xếp hạng.
 * Hạng / Tên / Tổng điểm / TB điểm đối thủ / TB nước thắng / TB nước thua (giá trị -1 hiện "-").
 */
public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.ViewHolder> {

    private final List<PlayerRanking> rankings = new ArrayList<>();

    public void setRankings(List<PlayerRanking> newRankings) {
        this.rankings.clear();
        if (newRankings != null) {
            this.rankings.addAll(newRankings);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ranking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlayerRanking p = rankings.get(position);
        int rank = position + 1;
        holder.txtRank.setText(String.valueOf(rank));

        // Màu huy hiệu cho top 3
        if (rank == 1) {
            holder.txtRank.setBackgroundColor(Color.parseColor("#FBC02D")); // Vàng
        } else if (rank == 2) {
            holder.txtRank.setBackgroundColor(Color.parseColor("#90A4AE")); // Bạc
        } else if (rank == 3) {
            holder.txtRank.setBackgroundColor(Color.parseColor("#BCAAA4")); // Đồng
        } else {
            holder.txtRank.setBackgroundColor(Color.parseColor("#1976D2")); // Xanh
        }

        holder.txtName.setText(p.getName() != null ? p.getName() : p.getUsername());
        holder.txtUsername.setText("@" + p.getUsername());
        holder.txtTotalPoint.setText(String.format(Locale.getDefault(), "%.1f đ", p.getTotalPoint()));
        holder.txtAvgOpponentPoint.setText(String.format(Locale.getDefault(), "TB đối thủ: %.1f", p.getAvgOpponentPoint()));

        if (p.getAvgMovesWin() >= 0) {
            holder.txtAvgMovesWin.setText(String.format(Locale.getDefault(), "Nước thắng: %.1f", p.getAvgMovesWin()));
        } else {
            holder.txtAvgMovesWin.setText("Nước thắng: -");
        }

        if (p.getAvgMovesLose() >= 0) {
            holder.txtAvgMovesLose.setText(String.format(Locale.getDefault(), "Nước thua: %.1f", p.getAvgMovesLose()));
        } else {
            holder.txtAvgMovesLose.setText("Nước thua: -");
        }
    }

    @Override
    public int getItemCount() {
        return rankings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtRank;
        TextView txtName;
        TextView txtUsername;
        TextView txtTotalPoint;
        TextView txtAvgOpponentPoint;
        TextView txtAvgMovesWin;
        TextView txtAvgMovesLose;

        ViewHolder(View itemView) {
            super(itemView);
            txtRank = itemView.findViewById(R.id.txtRank);
            txtName = itemView.findViewById(R.id.txtName);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtTotalPoint = itemView.findViewById(R.id.txtTotalPoint);
            txtAvgOpponentPoint = itemView.findViewById(R.id.txtAvgOpponentPoint);
            txtAvgMovesWin = itemView.findViewById(R.id.txtAvgMovesWin);
            txtAvgMovesLose = itemView.findViewById(R.id.txtAvgMovesLose);
        }
    }
}
