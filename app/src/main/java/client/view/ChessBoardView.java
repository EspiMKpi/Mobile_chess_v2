package client.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import model.ObjectWrapper;

/**
 * Chủ: N11. View bàn cờ online, được đặt trong layout của MatchPlayActivity (N04):
 *     <client.view.ChessBoardView android:id="@+id/boardView" ... />
 * Gồm: bàn cờ 8x8 vẽ từ FEN (chạm chọn quân + ô đích), PromotionDialog, gửi SEND_MOVE,
 * biên bản nước đi, đồng hồ 30 giây, nhãn "Chiếu!".
 * Gửi nước đi: ClientCtr.getInstance().sendData(new ObjectWrapper(ObjectWrapper.SEND_MOVE, new MoveRequest(from, to, promo)));
 * Mọi phương thức public đều được MatchPlayActivity gọi trên main thread.
 */
public class ChessBoardView extends LinearLayout {

    private String myColor;
    private boolean locked = true;

    public ChessBoardView(Context context) {
        this(context, null);
    }

    public ChessBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        // TODO(N11): inflate R.layout.n11_view_chess_board vào this. Tạm hiện 1 dòng chữ giữ chỗ:
        TextView placeholder = new TextView(context);
        placeholder.setText("BÀN CỜ VUA 8x8 (N11)");
        addView(placeholder);
    }

    /** Gọi khi nhận SERVER_START_MATCH và SERVER_RESTART_MATCH_SWAP_COLOR. myColor = "WHITE" | "BLACK". */
    public void reset(String myColor) {
        this.myColor = myColor;
        // TODO(N11): bàn cờ chuẩn, xoay khi cầm Đen, xóa biên bản, đồng hồ 30s, mở khóa nếu cầm Trắng
    }

    /** MatchPlayActivity chuyển tiếp SERVER_INFORM_MOVE và REPLY_MOVE_ILLEGAL vào đây. */
    public void onServerMessage(ObjectWrapper data) {
        // TODO(N11)
    }

    /** true khi nhận SERVER_END_GAME: khóa bàn VÀ dừng đồng hồ đếm ngược. */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    protected void onDetachedFromWindow() {
        setLocked(true);            // Activity đóng -> dừng đồng hồ, tránh rò rỉ
        super.onDetachedFromWindow();
    }
}
