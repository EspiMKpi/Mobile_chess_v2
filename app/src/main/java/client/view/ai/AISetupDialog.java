package client.view.ai;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import ptit.e23cnpm01.nguyentiendung.covua.R;

/**
 * [PHẦN N11] Hộp thoại thiết lập ván với máy.
 * OnlineListActivity (N04) chỉ gọi: AISetupDialog.open(this);
 */
public class AISetupDialog extends DialogFragment {

    public static final String TAG = "AISetupDialog";

    public static void open(AppCompatActivity host) {
        if (!host.getSupportFragmentManager().isStateSaved()
                && host.getSupportFragmentManager().findFragmentByTag(TAG) == null) {
            new AISetupDialog().show(host.getSupportFragmentManager(), TAG);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.n11_title_ai_match)
                .setMessage(R.string.n11_ai_todo)
                .setPositiveButton("Mở màn hình AI", (d, w) ->
                        startActivity(new Intent(requireContext(), AIMatchActivity.class)))
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
    }
}
