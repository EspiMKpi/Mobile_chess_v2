package client.view;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import client.control.ClientCtr;
import model.ObjectWrapper;
import model.PlayerStat;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Hộp thoại chờ đối thủ trả lời lời mời thách đấu (Hình 4.9, 4.10a, 4.11).
 */
public class ChallengeDialog extends DialogFragment implements DataReceiver, View.OnClickListener {

    public static final String TAG = "ChallengeDialog";
    private static final String ARG_TARGET = "target";

    private TextView lblStatus;
    private TextView lblCountdown;
    private Button btnCancel;
    private final ClientCtr myControl = ClientCtr.getInstance();
    private PlayerStat target;

    public static ChallengeDialog newInstance(PlayerStat target) {
        ChallengeDialog dialog = new ChallengeDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TARGET, target);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        setCancelable(false);
        if (getArguments() != null) {
            target = (PlayerStat) getArguments().getSerializable(ARG_TARGET);
        }

        View view = getLayoutInflater().inflate(R.layout.dialog_challenge, null);
        lblStatus = view.findViewById(R.id.lblStatus);
        lblCountdown = view.findViewById(R.id.lblCountdown);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(this);

        // [HƯỚNG DẪN - Hình 4.11, bước 15-17]
        // 1. Hiển thị lblStatus = getString(R.string.msg_challenge_waiting, target.getUsername()).
        // 2. Đăng ký nhận 3 mã: REPLY_CHALLENGE_BUSY, REPLY_CHALLENGE_REJECT, SERVER_START_MATCH.
        // 3. Gửi lời mời: myControl.sendData(new ObjectWrapper(ObjectWrapper.SEND_CHALLENGE, target));
        // 4. Bật CountDownTimer 30 giây cập nhật lblCountdown ("Còn lại: XX giây") và nhớ hủy trong onDestroyView().

        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_challenge)
                .setView(view)
                .create();
    }

    @Override
    public void onClick(View v) {
        // [HƯỚNG DẪN] Khi bấm btnCancel ("Hủy lời mời"):
        //  - Khóa btnCancel, đổi chữ trạng thái thành "Đang hủy lời mời...",
        //  - Gửi myControl.sendData(new ObjectWrapper(ObjectWrapper.CANCEL_CHALLENGE, null));
        //  - Chờ REPLY_CHALLENGE_REJECT hoặc SERVER_START_MATCH (trường hợp đối thủ vừa bấm Đồng ý trước đó).
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        // [HƯỚNG DẪN - Hình 4.11 & 4.12]
        // Kiểm tra if (!isAdded()) return;
        // - REPLY_CHALLENGE_BUSY: Toast "Đối thủ đang bận hoặc không còn online", dismissAllowingStateLoss().
        // - REPLY_CHALLENGE_REJECT: xem lý do ("REJECT" / "CANCEL" / "TIMEOUT" / "DISCONNECT"), Toast rồi dismissAllowingStateLoss().
        // - SERVER_START_MATCH: mở MatchPlayActivity của N11 với Intent extra MatchPlayActivity.EXTRA_START_INFO
        //   chứa (MatchStartInfo) d.getData(), rồi dismissAllowingStateLoss().
    }

    @Override
    public void onDestroy() {
        myControl.unregister(this);
        super.onDestroy();
    }
}
