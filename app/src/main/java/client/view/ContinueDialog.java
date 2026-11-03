package client.view;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import client.control.ClientCtr;
import model.GameResult;
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
        // TODO(N04, M4): lý do + "BẠN THẮNG (+1.0 điểm)" / "HÒA (+0.5)" / "BẠN THUA (+0)"
        return r == null ? "" : r.getReason();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_end_game)
                .setMessage(describe(result))
                .setPositiveButton(R.string.btn_continue, null)   // TODO(N04, M4)
                .setNegativeButton(R.string.btn_stop, null)       // TODO(N04, M4)
                .create();
    }
}
