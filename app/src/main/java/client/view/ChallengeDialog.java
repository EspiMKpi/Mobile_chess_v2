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

/** Chủ: N04. Hộp thoại chờ đối thủ trả lời lời mời thách đấu (Hình 4.9, 4.11, 4.12). */
public class ChallengeDialog extends DialogFragment implements DataReceiver, View.OnClickListener {

    public static final String TAG = "ChallengeDialog";
    private static final String ARG_TARGET = "target";
    private static final long INVITE_TIMEOUT_MS = 30_000L;

    private TextView lblStatus;
    private TextView lblCountdown;
    private Button btnCancel;
    private final ClientCtr myControl = ClientCtr.getInstance();
    private PlayerStat target;
    private CountDownTimer countDownTimer;
    private boolean cancelling;

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

        String targetName = target != null ? target.getUsername() : "";
        lblStatus.setText(getString(R.string.msg_challenge_waiting, targetName));

        myControl.register(ObjectWrapper.REPLY_CHALLENGE_BUSY, this);
        myControl.register(ObjectWrapper.REPLY_CHALLENGE_REJECT, this);
        myControl.register(ObjectWrapper.SERVER_START_MATCH, this);

        if (target == null || !myControl.sendData(new ObjectWrapper(ObjectWrapper.SEND_CHALLENGE, target))) {
            lblStatus.setText("Không gửi được lời mời");
        } else {
            startCountdown();
        }

        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_challenge)
                .setView(view)
                .create();
    }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(INVITE_TIMEOUT_MS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (lblCountdown != null) {
                    long sec = (millisUntilFinished + 999L) / 1000L;
                    lblCountdown.setText("Còn lại: " + sec + " giây");
                }
            }

            @Override
            public void onFinish() {
                if (lblCountdown != null) {
                    lblCountdown.setText("Hết thời gian chờ");
                }
            }
        }.start();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnCancel) {
            cancelling = true;
            btnCancel.setEnabled(false);
            lblStatus.setText("Đang hủy lời mời...");
            myControl.sendData(new ObjectWrapper(ObjectWrapper.CANCEL_CHALLENGE, null));
        }
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        if (!isAdded()) {
            return;
        }
        int code = d.getPerformative();
        if (code == ObjectWrapper.REPLY_CHALLENGE_BUSY) {
            Toast.makeText(requireContext(), "Đối thủ đang bận hoặc không còn online", Toast.LENGTH_SHORT).show();
            dismissAllowingStateLoss();
        } else if (code == ObjectWrapper.REPLY_CHALLENGE_REJECT) {
            String reason = d.getData() instanceof String ? (String) d.getData() : "REJECT";
            String msg;
            if (cancelling || "CANCEL".equals(reason)) {
                msg = "Đã hủy lời mời";
            } else if ("TIMEOUT".equals(reason)) {
                msg = "Đối thủ không trả lời sau 30 giây";
            } else if ("DISCONNECT".equals(reason)) {
                msg = "Đối thủ đã mất kết nối";
            } else {
                msg = "Đối thủ đã từ chối lời mời";
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
