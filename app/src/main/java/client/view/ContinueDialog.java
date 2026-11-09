package client.view;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import client.control.ClientCtr;
import model.GameResult;

/**
 * Chủ: N04. Hộp thoại sau mỗi ván (hình 4.13 bước 16-17): kết quả, điểm nhận được, "Tiếp tục ván tiếp theo?",
 * [Tiếp tục] / [Dừng chơi], đếm ngược 30 giây (server quyết định khi hết giờ).
 * MatchPlayActivity đóng dialog khi nhận SERVER_RESTART_MATCH_SWAP_COLOR / SERVER_END_SESSION_MATCH.
 */
public class ContinueDialog extends DialogFragment {

    public static final String TAG = "ContinueDialog";
    private static final String ARG_RESULT = "result";
    private static final long TIMEOUT_MS = 30_000;

    private final ClientCtr myControl = ClientCtr.getInstance();
    private GameResult result;
    private CountDownTimer countdown;

    /** Đóng gói result vào Bundle (ARG_RESULT). */
    public static ContinueDialog newInstance(GameResult result) {
        // TODO
        return new ContinueDialog();
    }

    /** setCancelable(false), đọc result từ Bundle. */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO
    }

    /**
     * Câu chữ lý do: CHECKMATE "Chiếu bí!", STALEMATE "Hết nước đi – Hòa", TIMEOUT "Hết giờ 30 giây",
     * RESIGN "Xin thua", DISCONNECT "Đối thủ mất kết nối"; kèm kết quả + điểm (thắng 1, hòa 0.5, thua 0).
     */
    private String describe(GameResult r) {
        // TODO
        return "";
    }

    /** AlertDialog describe(result) + [Tiếp tục] -> continueMatch(), [Dừng chơi] -> stopMatch(); startCountdown(). */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // TODO
        return super.onCreateDialog(savedInstanceState);
    }

    /** Đếm ngược 30 giây trên hộp thoại. */
    private void startCountdown() {
        // TODO
    }

    /** Hình 4.14 bước 1-2: khóa 2 nút, "Đang chờ đối thủ phản hồi...", sendData(REQUEST_CONTINUE_MATCH, true). */
    private void continueMatch() {
        // TODO
    }

    /** sendData(REQUEST_CONTINUE_MATCH, false), đóng hộp thoại. */
    private void stopMatch() {
        // TODO
    }

    @Override
    public void onDestroy() {
        // TODO: hủy countdown
        super.onDestroy();
    }
}
