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
 * Chủ: N04. Hình 4.1 - chờ đối thủ trả lời. Gửi SEND_CHALLENGE(target) khi mở.
 * Nhận REPLY_CHALLENGE_BUSY, REPLY_CHALLENGE_REJECT, SERVER_START_MATCH.
 * Dữ liệu truyền qua Bundle (Android tạo lại fragment bằng constructor rỗng).
 */
public class ChallengeDialog extends DialogFragment implements DataReceiver {

    public static final String TAG = "ChallengeDialog";
    private static final String ARG_TARGET = "target";

    private final ClientCtr myControl = ClientCtr.getInstance();
    private PlayerStat target;

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
        // TODO(N04, M4): if (savedInstanceState == null) myControl.sendData(new ObjectWrapper(SEND_CHALLENGE, target));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_challenge)
                .setMessage(getString(R.string.msg_challenge_waiting, target.getUsername()))
                .create();
    }

    @Override
    public void onDestroy() {
        myControl.unregister(this);
        super.onDestroy();
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M4): BUSY -> "Đối thủ đang bận" · REJECT -> "Đối thủ đã từ chối" -> dismiss()
        //   SERVER_START_MATCH (MatchStartInfo) -> startActivity(MatchPlayActivity + EXTRA_MY_COLOR, EXTRA_OPPONENT), dismiss()
    }
}
