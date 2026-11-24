package client.view;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

/** Chủ: N04. Hộp thoại nhận lời mời thách đấu ở bất kỳ màn hình nào (Hình 4.9, 4.11, 4.12). */
public class InviteDialog extends DialogFragment implements DataReceiver, View.OnClickListener {

    public static final String TAG = "InviteDialog";
    private static final String ARG_INVITER = "inviter";
    private static final long INVITE_TIMEOUT_MS = 30_000L;

    private TextView lblInviterInfo;
    private TextView lblCountdown;
    private Button btnAccept;
    private Button btnReject;
    private final ClientCtr myControl = ClientCtr.getInstance();
    private PlayerStat inviter;
    private CountDownTimer countDownTimer;

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

        String name = inviter != null ? inviter.getUsername() : "";
        float point = inviter != null ? inviter.getTotalPoint() : 0f;
        lblInviterInfo.setText(getString(R.string.msg_invite, name, point));

        myControl.register(ObjectWrapper.SERVER_START_MATCH, this);
        myControl.register(ObjectWrapper.SERVER_CANCEL_CHALLENGE, this);
        startCountdown();

        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_invite)
                .setView(view)
                .create();
    }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(INVITE_TIMEOUT_MS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (lblCountdown != null) {
                    long sec = (millisUntilFinished + 999L) / 1000L;
                    lblCountdown.setText("Tự động từ chối sau: " + sec + " giây");
                }
            }

            @Override
            public void onFinish() {
                if (lblCountdown != null) {
                    lblCountdown.setText("Đã hết thời gian trả lời");
                }
            }
        }.start();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnAccept) {
            btnAccept.setEnabled(false);
            btnReject.setEnabled(false);
            myControl.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_DECISION, true));
        } else if (v.getId() == R.id.btnReject) {
            btnAccept.setEnabled(false);
            btnReject.setEnabled(false);
            myControl.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_DECISION, false));
            dismissAllowingStateLoss();
        }
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        if (!isAdded()) {
            return;
        }
        int code = d.getPerformative();
        if (code == ObjectWrapper.SERVER_CANCEL_CHALLENGE) {
            String reason = d.getData() instanceof String ? (String) d.getData() : "CANCEL";
            String msg;
            if ("TIMEOUT".equals(reason)) {
                msg = "Lời mời đã hết hạn sau 30 giây";
            } else if ("DISCONNECT".equals(reason)) {
                msg = "Người mời đã mất kết nối";
            } else {
                msg = "Người mời đã hủy lời mời";
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            dismissAllowingStateLoss();
        } else if (code == ObjectWrapper.SERVER_START_MATCH && d.getData() instanceof MatchStartInfo) {
            Intent i = new Intent(requireContext(), MatchPlayActivity.class);
            i.putExtra(MatchPlayActivity.EXTRA_START_INFO, (MatchStartInfo) d.getData());
            startActivity(i);
            dismissAllowingStateLoss();
        }
    }

    @Override
    public void onDestroyView() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        myControl.unregister(this);
        super.onDestroy();
    }
}
