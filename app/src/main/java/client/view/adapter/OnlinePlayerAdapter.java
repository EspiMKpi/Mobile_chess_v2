package client.view.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import model.PlayerStat;

/**
 * Chủ: N04. Adapter danh sách người chơi online (layout item_online_player): STT, tên, @username, tổng điểm,
 * trạng thái RẢNH / ĐANG BẬN. Chạm để chọn 1 dòng (tô nền), giữ lựa chọn theo id khi danh sách cập nhật.
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

    /** Thay toàn bộ danh sách rồi notifyDataSetChanged(). */
    public void setPlayers(List<PlayerStat> newPlayers) {
        // TODO
    }

    /** Người chơi đang được chọn (theo selectedId), null nếu chưa chọn / đã rời đi. */
    public PlayerStat getSelected() {
        // TODO
        return null;
    }

    public void setSelectedId(int id) {
        // TODO
    }

    /** Inflate R.layout.item_online_player. */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // TODO
        return null;
    }

    /** Đổ dữ liệu 1 dòng, tô nền dòng được chọn, gắn click -> chọn + listener.onPlayerSelected(p). */
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
        TextView txtIndex;
        TextView txtName;
        TextView txtUsername;
        TextView txtPoints;
        TextView txtStatus;

        ViewHolder(View itemView) {
            super(itemView);
            // TODO: findViewById các TextView
        }
    }
}
