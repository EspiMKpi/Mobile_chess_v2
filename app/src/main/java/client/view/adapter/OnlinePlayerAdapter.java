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

import model.PlayerStat;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Adapter hiển thị danh sách người chơi trực tuyến (hình 3.1).
 * Hỗ trợ chọn 1 dòng bằng cách chạm (tô nền) và giữ lựa chọn theo id khi danh sách cập nhật.
 */
public class OnlinePlayerAdapter extends RecyclerView.Adapter<OnlinePlayerAdapter.ViewHolder> {

    public interface OnPlayerSelectedListener {
        void onPlayerSelected(PlayerStat player);
    }

    private final List<PlayerStat> players = new ArrayList<>();
    private int selectedId = -1;
    private OnPlayerSelectedListener listener;

    public OnlinePlayerAdapter(OnPlayerSelectedListener listener) {
        this.listener = listener;
    }

    public void setPlayers(List<PlayerStat> newPlayers) {
        this.players.clear();
        if (newPlayers != null) {
            this.players.addAll(newPlayers);
        }
        notifyDataSetChanged();
    }

    public PlayerStat getSelected() {
        if (selectedId == -1) return null;
        for (PlayerStat p : players) {
            if (p.getId() == selectedId) {
                return p;
            }
        }
        return null;
    }

    public void setSelectedId(int id) {
        this.selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_online_player, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlayerStat p = players.get(position);
        holder.txtIndex.setText(String.valueOf(position + 1));
        holder.txtName.setText(p.getName() != null ? p.getName() : p.getUsername());
        holder.txtUsername.setText("@" + p.getUsername());
        holder.txtPoints.setText(String.format(Locale.getDefault(), "%.1f đ", p.getTotalPoint()));

        boolean isIdle = PlayerStat.IDLE.equalsIgnoreCase(p.getStatus());
        if (isIdle) {
            holder.txtStatus.setText("RẢNH");
            holder.txtStatus.setTextColor(Color.parseColor("#2E7D32")); // Green
        } else {
            holder.txtStatus.setText("ĐANG BẬN");
            holder.txtStatus.setTextColor(Color.parseColor("#E65100")); // Orange
        }

        // Tô nền dòng được chọn
        if (p.getId() == selectedId) {
            holder.itemView.setBackgroundColor(Color.parseColor("#BBDEFB")); // Light blue highlight
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(v -> {
            selectedId = p.getId();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onPlayerSelected(p);
            }
        });
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtIndex;
        TextView txtName;
        TextView txtUsername;
        TextView txtPoints;
        TextView txtStatus;

        ViewHolder(View itemView) {
            super(itemView);
            txtIndex = itemView.findViewById(R.id.txtIndex);
            txtName = itemView.findViewById(R.id.txtName);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtPoints = itemView.findViewById(R.id.txtPoints);
            txtStatus = itemView.findViewById(R.id.txtStatus);
        }
    }
}
