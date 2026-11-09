package client.view;

import android.os.Bundle;
import android.widget.EditText;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Đăng ký tài khoản (hình 4.3). Nhận REPLY_REGISTER_USER. */
public class RegisterActivity extends BaseActivity {

    private EditText txtUsername, txtPassword, txtConfirmPass, txtName, txtEmail, txtTel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setTitle(R.string.title_register);
        // TODO: bước 3 - listen(REPLY_REGISTER_USER)
        // TODO: findViewById 6 ô nhập; btnSubmit -> submit(); btnCancel -> finish()
    }

    /** Bước 6: không trống, mật khẩu khớp xác nhận, email hợp lệ (nếu nhập) - báo lỗi bằng EditText.setError. */
    private boolean validateInput() {
        // TODO
        return false;
    }

    /** Bước 5-10: validateInput(), đóng gói Player (username, password, name, email, tel), sendData(REGISTER_USER). */
    private void submit() {
        // TODO
    }

    /** Bước 17-18: "ok" -> báo thành công, finish() về LoginActivity; "false" -> "Tên đăng nhập đã tồn tại". */
    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO
    }
}
