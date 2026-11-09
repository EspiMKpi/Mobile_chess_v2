package client.view;

import android.os.Bundle;
import android.widget.EditText;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Đổi mật khẩu (hình 4.6). Nhận REPLY_CHANGE_PASSWORD. Dữ liệu gửi lên: String[]{oldPass, newPass}. */
public class ChangePasswordActivity extends BaseActivity {

    private EditText txtOldPassword, txtNewPassword, txtConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        setTitle(R.string.title_change_password);
        // TODO: bước 3 - listen(REPLY_CHANGE_PASSWORD)
        // TODO: findViewById; btnSave -> save(); btnCancel -> finish()
    }

    /** Bước 5: không trống, mật khẩu mới khớp xác nhận. */
    private boolean validateInput() {
        // TODO
        return false;
    }

    /** Bước 4-6: validateInput(), sendData(CHANGE_PASSWORD, new String[]{oldPass, newPass}). */
    private void save() {
        // TODO
    }

    /** Bước 13-14: "ok" -> báo thành công, finish(); "false" -> "Mật khẩu hiện tại không đúng". */
    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO
    }
}
