package client.view;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import client.control.ClientCtr;
import model.ObjectWrapper;
import model.PlayerStat;

/**
 * Chủ: N04. Hộp thoại phía người mời A: "Đang chờ đối thủ phản hồi..." + đếm ngược 30 giây + [Hủy lời mời].
 * Nhận REPLY_CHALLENGE_BUSY, REPLY_CHALLENGE_REJECT, SERVER_START_MATCH.
 * Dữ liệu truyền qua Bundle (Android tạo lại fragment bằng constructor rỗng).
 */
public class ChallengeDialog extends DialogFragment implements DataReceiver {

    public static final String TAG = "ChallengeDialog";
    private static final String ARG_TARGET = "target";
    private static final long TIMEOUT_MS = 30_000;

    private final ClientCtr myControl = ClientCtr.getInstance();
    private PlayerStat target;
    private boolean cancelling;
    private CountDownTimer countdown;

    /** Hình 4.11 bước 14: đóng gói target vào Bundle (ARG_TARGET). */
    public static ChallengeDialog newInstance(PlayerStat target) {
        // TODO
        return new ChallengeDialog();
    }

    /**
     * setCancelable(false), đọc target từ Bundle.
     * Bước 15: register(REPLY_CHALLENGE_BUSY, REPLY_CHALLENGE_REJECT, SERVER_START_MATCH).
     * Bước 16: lần đầu tạo (savedInstanceState == null) -> sendData(SEND_CHALLENGE, target).
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO
    }

    /** Bước 17: AlertDialog "Đang chờ <tên> phản hồi..." + nút [Hủy lời mời] -> cancelChallenge(); startCountdown(). */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // TODO
        return super.onCreateDialog(savedInstanceState);
    }

    /** Đếm ngược 30 giây, cập nhật số giây còn lại lên hộp thoại. */
    private void startCountdown() {
        // TODO
    }

    /** A hủy lời mời: cancelling = true, sendData(REPLY_CHALLENGE_DECISION, false). */
    private void cancelChallenge() {
        // TODO
    }

    @Override
    public void onDestroy() {
        // TODO: hủy countdown, myControl.unregister(this)
        super.onDestroy();
    }

    /**
     * - REPLY_CHALLENGE_BUSY -> "Đối thủ đang bận", đóng.
     * - REPLY_CHALLENGE_REJECT -> "Đã hủy lời mời" / "Đối thủ đã từ chối" / hết giờ, đóng.
     * - SERVER_START_MATCH (hình 4.12 bước 13-14) -> mở MatchPlayActivity với màu quân + đối thủ, đóng.
     */
    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO
    }
}
