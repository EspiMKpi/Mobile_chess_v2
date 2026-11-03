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
        String oldPass = txtOldPassword.getText().toString();
        String newPass = txtNewPassword.getText().toString();
        String confirmPass = txtConfirmPassword.getText().toString();

        if (oldPass.isEmpty()) {
            txtOldPassword.setError("Vui lòng nhập mật khẩu hiện tại");
            return;
        }
        if (newPass.isEmpty()) {
            txtNewPassword.setError("Vui lòng nhập mật khẩu mới");
            return;
        }
        if (newPass.length() < 6) {
            txtNewPassword.setError("Mật khẩu mới tối thiểu 6 ký tự");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            txtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }
        if (newPass.equals(oldPass)) {
            txtNewPassword.setError("Mật khẩu mới không được trùng mật khẩu cũ");
            return;
        }

        myControl.sendData(new ObjectWrapper(ObjectWrapper.CHANGE_PASSWORD, new String[]{oldPass, newPass}));
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        if (data.getPerformative() == ObjectWrapper.REPLY_CHANGE_PASSWORD) {
            if ("ok".equals(data.getData())) {
                toast("Đổi mật khẩu thành công!");
                finish();
            } else {
                txtOldPassword.setError("Mật khẩu hiện tại không đúng");
                toast("Mật khẩu hiện tại không đúng");
            }
        }
    }
}
