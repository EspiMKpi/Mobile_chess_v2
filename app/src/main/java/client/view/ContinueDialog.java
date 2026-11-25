package client.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import model.GameResult;
import model.MatchResult;
import ptit.e23cnpm01.nguyentiendung.covua.R;

/**
 * Chủ: N11. Hộp thoại kết thúc ván: hiện kết quả + lý do, hỏi [Tiếp tục] / [Dừng chơi].
 * Sau khi chọn, hộp thoại vẫn mở (hiện "Đang chờ đối thủ…") cho tới khi MatchPlayActivity nhận
 * SERVER_RESTART_MATCH_SWAP_COLOR hoặc SERVER_END_SESSION_MATCH rồi tự đóng nó.
 * Dữ liệu truyền qua Bundle (Android tạo lại fragment bằng constructor rỗng).
 */
public class ContinueDialog extends DialogFragment {

    public static final String TAG = "ContinueDialog";
    private static final String ARG_RESULT = "result";

    /** Activity chứa hộp thoại (MatchPlayActivity) cài đặt interface này. */
    public interface Listener {
        void onContinueDecision(boolean accept);
    }

    public static ContinueDialog newInstance(GameResult result) {
        ContinueDialog d = new ContinueDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_RESULT, result);
        d.setArguments(args);
        d.setCancelable(false);
        return d;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        GameResult result = (GameResult) requireArguments().getSerializable(ARG_RESULT);
        Context ctx = requireContext();
        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle(R.string.n11_game_over_title)
                .setMessage(describe(ctx, result) + "\n\n" + ctx.getString(R.string.n11_continue_question))
                .setPositiveButton(R.string.n11_continue, null)     // gắn listener ở onShow để không tự đóng
                .setNegativeButton(R.string.n11_stop, null)
                .create();
        dialog.setOnShowListener(d -> {
            Button btnContinue = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnStop = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            btnContinue.setOnClickListener(v -> choose(dialog, btnContinue, btnStop, true));
            btnStop.setOnClickListener(v -> choose(dialog, btnContinue, btnStop, false));
        });
        return dialog;
    }

    private void choose(AlertDialog dialog, Button btnContinue, Button btnStop, boolean accept) {
        btnContinue.setEnabled(false);
        btnStop.setEnabled(false);
        dialog.setMessage(getString(accept ? R.string.n11_waiting_opponent : R.string.n11_leaving));
        if (getActivity() instanceof Listener) {
            ((Listener) getActivity()).onContinueDecision(accept);
        }
    }

    /** "BẠN THẮNG (+1.0 điểm)\nChiếu bí!" */
    static String describe(Context ctx, GameResult r) {
        if (r == null) {
            return "";
        }
        int resultText = MatchResult.WIN.equals(r.getResult()) ? R.string.n11_result_win
                : MatchResult.DRAW.equals(r.getResult()) ? R.string.n11_result_draw
                : R.string.n11_result_lose;
        return ctx.getString(resultText) + "\n" + ctx.getString(reasonText(r.getReason()));
    }

    private static int reasonText(String reason) {
        if (GameResult.CHECKMATE.equals(reason)) return R.string.n11_reason_checkmate;
        if (GameResult.STALEMATE.equals(reason)) return R.string.n11_reason_stalemate;
        if (GameResult.TIMEOUT.equals(reason)) return R.string.n11_reason_timeout;
        if (GameResult.RESIGN.equals(reason)) return R.string.n11_reason_resign;
        return R.string.n11_reason_disconnect;
    }
}
