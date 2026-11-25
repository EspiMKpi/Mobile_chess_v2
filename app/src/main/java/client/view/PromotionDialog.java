package client.view;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import java.util.function.Consumer;

import ptit.e23cnpm01.nguyentiendung.covua.R;

/** Chủ: N11. Hộp thoại chọn quân phong cấp khi Tốt tới hàng cuối. */
public final class PromotionDialog {

    private static final String[] CODES = {"Q", "R", "B", "N"};

    private PromotionDialog() {
    }

    /** onChosen nhận "Q" | "R" | "B" | "N". Không cho hủy: người chơi bắt buộc chọn một quân. */
    public static void show(Context context, Consumer<String> onChosen) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.n11_promotion_title)
                .setItems(R.array.n11_promotion_labels, (dialog, which) -> onChosen.accept(CODES[which]))
                .setCancelable(false)
                .show();
    }
}
