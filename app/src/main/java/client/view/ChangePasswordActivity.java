package client.view;

import android.os.Bundle;
import android.widget.EditText;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Hình 2.4. Nhận REPLY_CHANGE_PASSWORD. data gửi lên: String[]{oldPass, newPass}. */
public class ChangePasswordActivity extends BaseActivity {

    private EditText txtOldPassword, txtNewPassword, txtConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        setTitle(R.string.title_change_password);
        listen(ObjectWrapper.REPLY_CHANGE_PASSWORD);

        txtOldPassword = findViewById(R.id.txtOldPassword);
        txtNewPassword = findViewById(R.id.txtNewPassword);
        txtConfirmPassword = findViewById(R.id.txtConfirmPassword);
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    private void save() {
        // TODO(N04, M2): MK mới khớp xác nhận, khác MK cũ -> sendData(CHANGE_PASSWORD, new String[]{old, new})
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M2): "ok" -> toast, finish() · "false" -> "Mật khẩu hiện tại không đúng"
    }
}
