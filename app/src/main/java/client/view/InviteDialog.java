package client.view;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import client.control.ClientCtr;
import model.ObjectWrapper;
import model.PlayerStat;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Hình 4.2 - lời mời từ người khác. [Đồng ý] / [Từ chối] gửi REPLY_CHALLENGE_DECISION (Boolean).
 * Nhận SERVER_START_MATCH (vào ván) và REPLY_CHALLENGE_REJECT (người mời đã rời đi).
 */
public class InviteDialog extends DialogFragment implements DataReceiver {

    public static final String TAG = "InviteDialog";
    private static final String ARG_INVITER = "inviter";

    private final ClientCtr myControl = ClientCtr.getInstance();
    private PlayerStat inviter;

    public static InviteDialog newInstance(PlayerStat inviter) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_INVITER, inviter);
        InviteDialog d = new InviteDialog();
        d.setArguments(args);
        return d;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        inviter = (PlayerStat) requireArguments().getSerializable(ARG_INVITER);
        myControl.register(ObjectWrapper.SERVER_START_MATCH, this);
        myControl.register(ObjectWrapper.REPLY_CHALLENGE_REJECT, this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_invite)
                .setMessage(getString(R.string.msg_invite, inviter.getName(), inviter.getTotalPoint()))
                .setPositiveButton(R.string.btn_accept, null)   // TODO(N04, M4): gửi true, giữ dialog chờ START
                .setNegativeButton(R.string.btn_reject, null)   // TODO(N04, M4): gửi false rồi dismiss()
                .create();
    }

    @Override
    public void onDestroy() {
        myControl.unregister(this);
        super.onDestroy();
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M4): SERVER_START_MATCH -> mở MatchPlayActivity, dismiss()
        //   REPLY_CHALLENGE_REJECT -> "Người mời đã rời đi", dismiss()
    }
}
