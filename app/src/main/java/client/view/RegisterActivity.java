package client.view;

import android.os.Bundle;
import android.widget.EditText;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Hình 2.2. Nhận REPLY_REGISTER_USER. */
public class RegisterActivity extends BaseActivity {

    private EditText txtUsername, txtPassword, txtConfirmPass, txtName, txtEmail, txtTel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setTitle(R.string.title_register);
        listen(ObjectWrapper.REPLY_REGISTER_USER);

        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);
        txtConfirmPass = findViewById(R.id.txtConfirmPass);
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        txtTel = findViewById(R.id.txtTel);
        findViewById(R.id.btnSubmit).setOnClickListener(v -> submit());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    /** Không trống, mật khẩu khớp, email hợp lệ (nếu nhập) - báo lỗi bằng EditText.setError. */
    private boolean validateInput() {
        // TODO(N04, M2)
        return false;
    }

    private void submit() {
        // TODO(N04, M2): if (validateInput()) sendData(REGISTER_USER, player)
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M2): "ok" -> toast thành công, finish() · "false" -> "Tên đăng nhập đã tồn tại"
    }
}
