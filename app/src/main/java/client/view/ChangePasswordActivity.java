package client.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Đổi mật khẩu (Hình 4.1, 4.2a, 4.6).
 */
public class ChangePasswordActivity extends BaseActivity implements View.OnClickListener {

    private EditText txtOldPassword;
    private EditText txtNewPassword;
    private EditText txtConfirmPassword;
    private Button btnSave;
    private Button btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        setTitle(R.string.title_change_password);

        txtOldPassword = findViewById(R.id.txtOldPassword);
        txtNewPassword = findViewById(R.id.txtNewPassword);
        txtConfirmPassword = findViewById(R.id.txtConfirmPassword);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        btnSave.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        // [HƯỚNG DẪN - Hình 4.6, bước 3]
        // myControl.register(ObjectWrapper.REPLY_CHANGE_PASSWORD, this);
    }

    /** TODO(N04 - Hình 4.6, bước 5): kiểm tra không rỗng, mật khẩu mới khớp xác nhận, khác mật khẩu cũ. */
    public boolean validateInput() {
        return true;
    }

    @Override
    public void onClick(View v) {
        // [HƯỚNG DẪN - Hình 4.6, bước 4-6]
        // - btnSave: gọi validateInput(), gửi ObjectWrapper(CHANGE_PASSWORD, new String[]{oldPass, newPass}).
        // - btnCancel: finish().
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        // [HƯỚNG DẪN - Hình 4.6, bước 13-14]
        // Nếu REPLY_CHANGE_PASSWORD:
        // - "ok": Toast "Đổi mật khẩu thành công", finish().
        // - Ngược lại: Toast "Mật khẩu hiện tại không đúng".
    }
}
