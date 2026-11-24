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
 * Chủ: N04. Hộp thoại nhận lời mời thách đấu (Hình 4.9, 4.10a, 4.11, 4.12).
 * Hiện ở bất kỳ màn hình nào khi nhận SERVER_INFORM_CHALLENGE.
 */
public class InviteDialog extends DialogFragment implements DataReceiver, View.OnClickListener {

    public static final String TAG = "InviteDialog";
    private static final String ARG_INVITER = "inviter";

    private TextView lblInviterInfo;
    private TextView lblCountdown;
    private Button btnAccept;
    private Button btnReject;
    private final ClientCtr myControl = ClientCtr.getInstance();
    private PlayerStat inviter;

    public static InviteDialog newInstance(PlayerStat inviter) {
        InviteDialog dialog = new InviteDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_INVITER, inviter);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        setCancelable(false);
        if (getArguments() != null) {
            inviter = (PlayerStat) getArguments().getSerializable(ARG_INVITER);
        }

        View view = getLayoutInflater().inflate(R.layout.dialog_invite, null);
        lblInviterInfo = view.findViewById(R.id.lblInviterInfo);
        lblCountdown = view.findViewById(R.id.lblCountdown);
        btnAccept = view.findViewById(R.id.btnAccept);
        btnReject = view.findViewById(R.id.btnReject);
        btnAccept.setOnClickListener(this);
        btnReject.setOnClickListener(this);

        // [HƯỚNG DẪN - Hình 4.11, bước 25-27]
        // 1. Hiển thị inviter (username + totalPoint) lên lblInviterInfo.
        // 2. Đăng ký nhận SERVER_START_MATCH và SERVER_CANCEL_CHALLENGE với myControl.
        // 3. Bật CountDownTimer 30 giây cập nhật lblCountdown ("Tự động từ chối sau: XX giây").

        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_invite)
                .setView(view)
                .create();
    }

    @Override
    public void onClick(View v) {
        // [HƯỚNG DẪN - Hình 4.12, bước 1-2]
        // - btnAccept: gửi REPLY_CHALLENGE_DECISION(true), khóa 2 nút, chờ SERVER_START_MATCH (hoặc SERVER_CANCEL_CHALLENGE).
        // - btnReject: gửi REPLY_CHALLENGE_DECISION(false), dismissAllowingStateLoss().
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        // [HƯỚNG DẪN - Hình 4.11 & 4.12]
        // Kiểm tra if (!isAdded()) return;
        // - SERVER_CANCEL_CHALLENGE: Toast lý do ("CANCEL" / "TIMEOUT" / "DISCONNECT") rồi dismissAllowingStateLoss().
        // - SERVER_START_MATCH: mở MatchPlayActivity của N11 với Intent extra MatchPlayActivity.EXTRA_START_INFO
        //   chứa (MatchStartInfo) d.getData(), rồi dismissAllowingStateLoss().
    }

    @Override
    public void onDestroy() {
        myControl.unregister(this);
        super.onDestroy();
    }
}
