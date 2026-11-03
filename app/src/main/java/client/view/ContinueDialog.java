package client.view;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import client.control.ClientCtr;
import model.GameResult;
import model.MatchResult;
import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Hình 4.4 - hỏi chơi tiếp sau mỗi ván [LỆCH BC: tham số String -> GameResult].
 * [Tiếp tục] -> REQUEST_CONTINUE_MATCH(true), đổi nội dung thành "Đang chờ đối thủ..."
 * [Dừng chơi] -> REQUEST_CONTINUE_MATCH(false).
 * MatchPlayActivity đóng dialog khi nhận RESTART_MATCH_SWAP_COLOR / END_SESSION_MATCH.
 */
public class ContinueDialog extends DialogFragment {

    public static final String TAG = "ContinueDialog";
    private static final String ARG_RESULT = "result";

    private final ClientCtr myControl = ClientCtr.getInstance();
    private GameResult result;

    public static ContinueDialog newInstance(GameResult result) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_RESULT, result);
        ContinueDialog d = new ContinueDialog();
        d.setArguments(args);
        return d;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        result = (GameResult) requireArguments().getSerializable(ARG_RESULT);
    }

    /** CHECKMATE "Chiếu bí!" · STALEMATE "Hết nước đi – Hòa" · TIMEOUT "Hết giờ 30 giây" · RESIGN "Xin thua" · DISCONNECT "Đối thủ mất kết nối". */
    private String describe(GameResult r) {
        if (r == null) return "";

        String reasonStr;
        String reason = r.getReason();
        if (GameResult.CHECKMATE.equalsIgnoreCase(reason)) {
            reasonStr = "Chiếu bí!";
        } else if (GameResult.STALEMATE.equalsIgnoreCase(reason)) {
            reasonStr = "Hết nước đi – Hòa";
        } else if (GameResult.TIMEOUT.equalsIgnoreCase(reason)) {
            reasonStr = "Hết giờ 30 giây";
        } else if (GameResult.RESIGN.equalsIgnoreCase(reason)) {
            reasonStr = "Xin thua";
        } else if (GameResult.DISCONNECT.equalsIgnoreCase(reason)) {
            reasonStr = "Đối thủ mất kết nối";
        } else {
            reasonStr = (reason != null ? reason : "");
        }

        String outcomeStr;
        String res = r.getResult();
        if (MatchResult.WIN.equalsIgnoreCase(res)) {
            outcomeStr = "BẠN THẮNG (+1.0 điểm)";
        } else if (MatchResult.DRAW.equalsIgnoreCase(res)) {
            outcomeStr = "HÒA (+0.5 điểm)";
        } else {
            outcomeStr = "BẠN THUA (+0 điểm)";
        }

        return reasonStr + "\n" + outcomeStr + "\n\nBạn có muốn tiếp tục ván mới và đổi bên không?";
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_end_game)
                .setMessage(describe(result))
                .setPositiveButton(R.string.btn_continue, null)
                .setNegativeButton(R.string.btn_stop, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnContinue = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnStop = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            btnContinue.setOnClickListener(v -> {
                btnContinue.setEnabled(false);
                btnStop.setEnabled(false);
                dialog.setMessage("Đang chờ đối thủ phản hồi...");
                myControl.sendData(new ObjectWrapper(ObjectWrapper.REQUEST_CONTINUE_MATCH, true));
            });

            btnStop.setOnClickListener(v -> {
                myControl.sendData(new ObjectWrapper(ObjectWrapper.REQUEST_CONTINUE_MATCH, false));
                dismissAllowingStateLoss();
            });
        });

        return dialog;
    }
}
