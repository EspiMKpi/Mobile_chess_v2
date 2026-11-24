package client.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import model.PlayerStat;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Adapter hiển thị danh sách người chơi trực tuyến trong {@link client.view.OnlineListActivity}.
 */
public class OnlinePlayerAdapter extends RecyclerView.Adapter<OnlinePlayerAdapter.ViewHolder> {

    private final List<PlayerStat> items = new ArrayList<>();
    private int selectedPlayerId = -1;

    /**
     * TODO(N04): Cập nhật danh sách người chơi online, giữ nguyên dòng đang chọn theo selectedPlayerId
     * (nếu người đó không còn trong danh sách hoặc không còn rảnh thì đặt lại selectedPlayerId = -1),
     * rồi gọi notifyDataSetChanged().
     */
    public void updateList(List<PlayerStat> newList) {
        items.clear();
        if (newList != null) {
            items.addAll(newList);
        }
        notifyDataSetChanged();
    }

    /** TODO(N04): Trả về đối tượng PlayerStat đang được chọn trong danh sách (hoặc null). */
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
        // TODO(N04): Đổ dữ liệu STT, name, username, totalPoint, trạng thái (RẢNH / ĐANG BẬN),
        // tô nền phân biệt dòng đang chọn và xử lý click chọn dòng -> notifyDataSetChanged().
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
