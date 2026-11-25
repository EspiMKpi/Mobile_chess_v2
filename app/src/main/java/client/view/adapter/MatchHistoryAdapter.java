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
import ptit.e23cnpm01.nguyentiendung.covua.R;

/** Chủ: N04. Adapter lịch sử các ván đã chơi. */
public class MatchHistoryAdapter extends RecyclerView.Adapter<MatchHistoryAdapter.ViewHolder> {

    private final List<Match> matches = new ArrayList<>();
    private final int myPlayerId;
    private final SimpleDateFormat fullFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

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
        Match m = matches.get(position);
        holder.txtMatchIndex.setText("#" + (position + 1));

        String startStr = m.getStartTime() != null ? fullFmt.format(m.getStartTime()) : "-";
        String endStr = m.getEndTime() != null ? timeFmt.format(m.getEndTime()) : "-";
        holder.txtMatchTime.setText(startStr + " - " + endStr);

        MatchResult mine = null;
        MatchResult opp = null;
        if (m.getResults() != null) {
            for (MatchResult r : m.getResults()) {
                if (r.getPlayer() != null && r.getPlayer().getId() == myPlayerId) {
                    mine = r;
                } else {
                    opp = r;
                }
            }
        }

        String oppName = (opp != null && opp.getPlayer() != null) ? opp.getPlayer().getUsername() : "Không rõ";
        holder.txtOpponentInfo.setText("Đối thủ: " + oppName);

        String colorLabel = (mine != null && MatchResult.WHITE.equals(mine.getColor())) ? "TRẮNG" : "ĐEN";
        holder.txtColorInfo.setText("Quân: " + colorLabel);

        String result = mine != null ? mine.getResult() : MatchResult.LOSE;
        if (MatchResult.WIN.equals(result)) {
            holder.txtResultInfo.setText("Kết quả: THẮNG");
            holder.txtResultInfo.setTextColor(Color.parseColor("#2E7D32"));
            holder.txtPointInfo.setText("Điểm: 1.0");
        } else if (MatchResult.DRAW.equals(result)) {
            holder.txtResultInfo.setText("Kết quả: HÒA");
            holder.txtResultInfo.setTextColor(Color.parseColor("#F57C00"));
            holder.txtPointInfo.setText("Điểm: 0.5");
        } else {
            holder.txtResultInfo.setText("Kết quả: THUA");
            holder.txtResultInfo.setTextColor(Color.parseColor("#C62828"));
            holder.txtPointInfo.setText("Điểm: 0.0");
        }
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
