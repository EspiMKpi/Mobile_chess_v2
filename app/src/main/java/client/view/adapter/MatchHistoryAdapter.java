package client.view.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import model.Match;

/**
 * Chủ: N04. Adapter lịch sử đấu (layout item_match_history). Mỗi dòng (báo cáo mục 1.3):
 * thời điểm bắt đầu và kết thúc, tên đối thủ, màu quân, kết quả, điểm nhận được (thắng 1, hòa 0.5, thua 0).
 */
public class MatchHistoryAdapter extends RecyclerView.Adapter<MatchHistoryAdapter.ViewHolder> {

    private final List<Match> matches = new ArrayList<>();
    private final int myPlayerId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public MatchHistoryAdapter(int myPlayerId) {
        this.myPlayerId = myPlayerId;
    }

    /** Thay toàn bộ danh sách rồi notifyDataSetChanged(). */
    public void setMatches(List<Match> newMatches) {
        // TODO
    }

    /** Inflate R.layout.item_match_history. */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // TODO
        return null;
    }

    /** Tách MatchResult của mình (theo myPlayerId) và của đối thủ trong m.getResults(), đổ lên các TextView. */
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
        TextView txtMatchId;
        TextView txtMatchTime;
        TextView txtOpponentInfo;
        TextView txtResultBadge;
        TextView txtMyColor;

        ViewHolder(View itemView) {
            super(itemView);
            // TODO: findViewById các TextView
        }
    }
}
