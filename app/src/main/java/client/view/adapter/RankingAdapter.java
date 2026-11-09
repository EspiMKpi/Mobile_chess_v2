package client.view.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import model.PlayerRanking;

/**
 * Chủ: N04. Adapter bảng xếp hạng (layout item_ranking):
 * Hạng / Tên / @username / Tổng điểm / TB điểm đối thủ / TB nước thắng / TB nước thua (giá trị -1 hiện "-").
 */
public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.ViewHolder> {

    private final List<PlayerRanking> rankings = new ArrayList<>();

    /** Thay toàn bộ danh sách rồi notifyDataSetChanged(). */
    public void setRankings(List<PlayerRanking> newRankings) {
        // TODO
    }

    /** Inflate R.layout.item_ranking. */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // TODO
        return null;
    }

    /** Đổ dữ liệu 1 dòng; hạng = position + 1; avgMovesWin / avgMovesLose < 0 -> "-". */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // TODO
    }

    @Override
    public int getItemCount() {
        // TODO
        return 0;
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
            // TODO: findViewById các TextView
        }
    }
}
