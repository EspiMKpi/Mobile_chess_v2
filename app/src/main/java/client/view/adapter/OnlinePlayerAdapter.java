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

/** Chủ: N04. Adapter danh sách người chơi trực tuyến. */
public class OnlinePlayerAdapter extends RecyclerView.Adapter<OnlinePlayerAdapter.ViewHolder> {

    private final List<PlayerStat> items = new ArrayList<>();
    private int selectedPlayerId = -1;

    public void updateList(List<PlayerStat> newList) {
        items.clear();
        if (newList != null) {
            items.addAll(newList);
        }
        boolean stillPresentAndIdle = false;
        for (PlayerStat p : items) {
            if (p.getId() == selectedPlayerId && PlayerStat.IDLE.equals(p.getStatus())) {
                stillPresentAndIdle = true;
                break;
            }
        }
        if (!stillPresentAndIdle) {
            selectedPlayerId = -1;
        }
        notifyDataSetChanged();
    }

    public PlayerStat getSelectedPlayer() {
        for (PlayerStat p : items) {
            if (p.getId() == selectedPlayerId) {
                return p;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_online_player, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlayerStat p = items.get(position);
        holder.txtIndex.setText(String.valueOf(position + 1));
        holder.txtName.setText(p.getName() != null && !p.getName().isEmpty() ? p.getName() : p.getUsername());
        holder.txtUsername.setText("@" + p.getUsername());
        holder.txtPoints.setText(String.format(Locale.US, "%.1f đ", p.getTotalPoint()));

        boolean idle = PlayerStat.IDLE.equals(p.getStatus());
        holder.txtStatus.setText(idle ? "RẢNH" : "ĐANG BẬN");
        holder.txtStatus.setTextColor(idle ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));

        boolean selected = (p.getId() == selectedPlayerId);
        holder.itemView.setBackgroundColor(selected ? Color.parseColor("#E3F2FD") : Color.TRANSPARENT);

        holder.itemView.setOnClickListener(v -> {
            selectedPlayerId = selected ? -1 : p.getId();
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtIndex;
        final TextView txtName;
        final TextView txtUsername;
        final TextView txtPoints;
        final TextView txtStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtIndex = itemView.findViewById(R.id.txtIndex);
            txtName = itemView.findViewById(R.id.txtName);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtPoints = itemView.findViewById(R.id.txtPoints);
            txtStatus = itemView.findViewById(R.id.txtStatus);
        }
    }
}
