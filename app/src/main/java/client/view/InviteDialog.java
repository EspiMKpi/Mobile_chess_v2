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
 * Chủ: N04. Hộp thoại lời mời phía B (hiện ở bất kỳ màn hình nào): tên + điểm người mời,
 * đếm ngược 30 giây, [Đồng ý] / [Từ chối] gửi REPLY_CHALLENGE_DECISION (Boolean).
 * Nhận SERVER_START_MATCH (vào ván) và SERVER_CANCEL_CHALLENGE (A hủy / hết giờ / A mất kết nối).
 */
public class InviteDialog extends DialogFragment implements DataReceiver {

    public static final String TAG = "InviteDialog";
    private static final String ARG_INVITER = "inviter";
    private static final long TIMEOUT_MS = 30_000;

    private final ClientCtr myControl = ClientCtr.getInstance();
    private PlayerStat inviter;
    private CountDownTimer countdown;

    /** Hình 4.11 bước 25: đóng gói inviter vào Bundle (ARG_INVITER). */
    public static InviteDialog newInstance(PlayerStat inviter) {
        // TODO
        return new InviteDialog();
    }

    /** setCancelable(false), đọc inviter. Bước 26: register(SERVER_START_MATCH, SERVER_CANCEL_CHALLENGE). */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO
    }

    /** Bước 27: AlertDialog lời mời + [Đồng ý] -> accept(), [Từ chối] -> reject(); startCountdown(). */
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

    /** Hình 4.12 bước 1-2: khóa 2 nút, "Đang vào phòng thi đấu...", sendData(REPLY_CHALLENGE_DECISION, true). */
    private void accept() {
        // TODO
    }

    /** sendData(REPLY_CHALLENGE_DECISION, false), đóng hộp thoại. */
    private void reject() {
        // TODO
    }

    @Override
    public void onDestroy() {
        // TODO: hủy countdown, myControl.unregister(this)
        super.onDestroy();
    }

    /**
     * - SERVER_START_MATCH (hình 4.12 bước 19-20) -> mở MatchPlayActivity với màu quân + đối thủ, đóng.
     * - SERVER_CANCEL_CHALLENGE -> báo lý do (người mời đã hủy / hết giờ), đóng.
     */
    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO
    }
}
