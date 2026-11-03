package client.view;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import client.control.ClientCtr;
import model.MatchStartInfo;
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
        String name = (inviter != null)
                ? (inviter.getName() != null && !inviter.getName().isEmpty() ? inviter.getName() : inviter.getUsername())
                : "";
        float pt = (inviter != null) ? inviter.getTotalPoint() : 0f;

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_invite)
                .setMessage(getString(R.string.msg_invite, name, pt))
                .setPositiveButton(R.string.btn_accept, null)
                .setNegativeButton(R.string.btn_reject, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnAccept = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnReject = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            btnAccept.setOnClickListener(v -> {
                btnAccept.setEnabled(false);
                btnReject.setEnabled(false);
                dialog.setMessage("Đang vào phòng thi đấu...");
                myControl.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_DECISION, true));
            });

            btnReject.setOnClickListener(v -> {
                myControl.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_DECISION, false));
                dismissAllowingStateLoss();
            });
        });

        return dialog;
    }

    @Override
    public void onDestroy() {
        myControl.unregister(this);
        super.onDestroy();
    }

    private void toast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        if (!isAdded()) return;

        switch (data.getPerformative()) {
            case ObjectWrapper.SERVER_START_MATCH:
                if (data.getData() instanceof MatchStartInfo) {
                    MatchStartInfo info = (MatchStartInfo) data.getData();
                    Intent intent = new Intent(requireContext(), MatchPlayActivity.class);
                    intent.putExtra(MatchPlayActivity.EXTRA_MY_COLOR, info.getMyColor());
                    intent.putExtra(MatchPlayActivity.EXTRA_OPPONENT, info.getOpponent());
                    startActivity(intent);
                }
                dismissAllowingStateLoss();
                break;

            case ObjectWrapper.REPLY_CHALLENGE_REJECT:
                toast("Người mời đã rời đi");
                dismissAllowingStateLoss();
                break;

            default:
                break;
        }
    }
}
