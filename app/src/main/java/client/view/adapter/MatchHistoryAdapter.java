package client.view.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
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
import model.MatchResult;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Adapter hiển thị lịch sử đấu (hình 2.5).
 */
public class MatchHistoryAdapter extends RecyclerView.Adapter<MatchHistoryAdapter.ViewHolder> {

    private final List<Match> matches = new ArrayList<>();
    private final int myPlayerId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public MatchHistoryAdapter(int myPlayerId) {
        this.myPlayerId = myPlayerId;
    }

    public void setMatches(List<Match> newMatches) {
        this.matches.clear();
        if (newMatches != null) {
            this.matches.addAll(newMatches);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Match m = matches.get(position);
        holder.txtMatchId.setText(String.format(Locale.getDefault(), "Trận #%d", m.getId()));

        if (m.getStartTime() != null) {
            holder.txtMatchTime.setText(dateFormat.format(m.getStartTime()));
        } else {
            holder.txtMatchTime.setText("--/--/---- --:--");
        }

        MatchResult myRes = null;
        MatchResult oppRes = null;

        for (MatchResult r : m.getResults()) {
            if (r != null && r.getPlayer() != null) {
                if (r.getPlayer().getId() == myPlayerId) {
                    myRes = r;
                } else {
                    oppRes = r;
                }
            }
        }

        // Thông tin đối thủ
        if (oppRes != null && oppRes.getPlayer() != null) {
            String oppColorStr = MatchResult.WHITE.equalsIgnoreCase(oppRes.getColor()) ? "TRẮNG" : "ĐEN";
            String oppName = oppRes.getPlayer().getName();
            if (oppName == null || oppName.isEmpty()) {
                oppName = oppRes.getPlayer().getUsername();
            }
            holder.txtOpponentInfo.setText(String.format("Đối thủ: %s (Quân %s)", oppName, oppColorStr));
        } else {
            holder.txtOpponentInfo.setText("Đối thủ: Không xác định");
        }

        // Kết quả của mình
        if (myRes != null) {
            String resStr = myRes.getResult();
            String myColorStr = MatchResult.WHITE.equalsIgnoreCase(myRes.getColor()) ? "TRẮNG" : "ĐEN";
            holder.txtMyColor.setText(String.format("(Quân %s)", myColorStr));

            if (MatchResult.WIN.equalsIgnoreCase(resStr)) {
                holder.txtResultBadge.setText("THẮNG (+1.0 đ)");
                holder.txtResultBadge.setTextColor(Color.parseColor("#2E7D32"));
                holder.txtResultBadge.setBackgroundColor(Color.parseColor("#E8F5E9"));
            } else if (MatchResult.DRAW.equalsIgnoreCase(resStr)) {
                holder.txtResultBadge.setText("HÒA (+0.5 đ)");
                holder.txtResultBadge.setTextColor(Color.parseColor("#1565C0"));
                holder.txtResultBadge.setBackgroundColor(Color.parseColor("#E3F2FD"));
            } else {
                holder.txtResultBadge.setText("THUA (+0 đ)");
                holder.txtResultBadge.setTextColor(Color.parseColor("#C62828"));
                holder.txtResultBadge.setBackgroundColor(Color.parseColor("#FFEBEE"));
            }
        } else {
            holder.txtResultBadge.setText("---");
            holder.txtMyColor.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtMatchId;
        TextView txtMatchTime;
        TextView txtOpponentInfo;
        TextView txtResultBadge;
        TextView txtMyColor;

        ViewHolder(View itemView) {
            super(itemView);
            txtMatchId = itemView.findViewById(R.id.txtMatchId);
            txtMatchTime = itemView.findViewById(R.id.txtMatchTime);
            txtOpponentInfo = itemView.findViewById(R.id.txtOpponentInfo);
            txtResultBadge = itemView.findViewById(R.id.txtResultBadge);
            txtMyColor = itemView.findViewById(R.id.txtMyColor);
        }
    }
}
