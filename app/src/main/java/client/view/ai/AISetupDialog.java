package client.view.ai;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

/**
 * Chủ: N11. Hộp thoại thiết lập ván với máy (Giao diện 3.1 báo cáo N11).
 * OnlineListActivity (N04) chỉ gọi đúng một dòng: AISetupDialog.open(this);
 */
public class AISetupDialog extends DialogFragment {

    public static final String TAG = "AISetupDialog";

    public static void open(AppCompatActivity host) {
        new AISetupDialog().show(host.getSupportFragmentManager(), TAG);
    }

    // TODO(N11): onCreateDialog(): chọn Người/Máy cho Trắng/Đen, độ sâu 1-6 (mặc định 3)
    //            -> startActivity(new Intent(requireContext(), AIMatchActivity.class) + extras)
}
