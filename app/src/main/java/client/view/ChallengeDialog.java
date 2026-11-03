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
 * Chủ: N04. Hình 4.1 - chờ đối thủ trả lời. Gửi SEND_CHALLENGE(target) khi mở.
 * Nhận REPLY_CHALLENGE_BUSY, REPLY_CHALLENGE_REJECT, SERVER_START_MATCH.
 * Dữ liệu truyền qua Bundle (Android tạo lại fragment bằng constructor rỗng).
 */
public class ChallengeDialog extends DialogFragment implements DataReceiver {

    public static final String TAG = "ChallengeDialog";
    private static final String ARG_TARGET = "target";

    private final ClientCtr myControl = ClientCtr.getInstance();
    private PlayerStat target;
    private boolean cancelling;

    public static ChallengeDialog newInstance(PlayerStat target) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_TARGET, target);
        ChallengeDialog d = new ChallengeDialog();
        d.setArguments(args);
        return d;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        target = (PlayerStat) requireArguments().getSerializable(ARG_TARGET);
        myControl.register(ObjectWrapper.REPLY_CHALLENGE_BUSY, this);
        myControl.register(ObjectWrapper.REPLY_CHALLENGE_REJECT, this);
        myControl.register(ObjectWrapper.SERVER_START_MATCH, this);

        if (savedInstanceState == null && target != null) {
            myControl.sendData(new ObjectWrapper(ObjectWrapper.SEND_CHALLENGE, target));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String targetName = (target != null)
                ? (target.getName() != null && !target.getName().isEmpty() ? target.getName() : target.getUsername())
                : "";
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_challenge)
                .setMessage(getString(R.string.msg_challenge_waiting, targetName))
                .setNegativeButton(R.string.btn_cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            btnCancel.setOnClickListener(v -> {
                cancelling = true;
                btnCancel.setEnabled(false);
                dialog.setMessage("Đang hủy lời mời...");
                myControl.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_DECISION, false));
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
            case ObjectWrapper.REPLY_CHALLENGE_BUSY:
                toast("Đối thủ đang bận");
                dismissAllowingStateLoss();
                break;

            case ObjectWrapper.REPLY_CHALLENGE_REJECT:
                toast(cancelling ? "Đã hủy lời mời" : "Đối thủ đã từ chối");
                dismissAllowingStateLoss();
                break;

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

            default:
                break;
        }
    }
}
