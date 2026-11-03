package client.view;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;

import model.ObjectWrapper;
import model.Player;
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
        String username = txtUsername.getText().toString().trim();
        String password = txtPassword.getText().toString();
        String confirmPass = txtConfirmPass.getText().toString();
        String name = txtName.getText().toString().trim();
        String email = txtEmail.getText().toString().trim();
        String tel = txtTel.getText().toString().trim();

        if (username.isEmpty()) {
            txtUsername.setError("Vui lòng nhập tên đăng nhập");
            return false;
        }
        if (username.length() < 3) {
            txtUsername.setError("Tên đăng nhập tối thiểu 3 ký tự");
            return false;
        }
        if (password.isEmpty()) {
            txtPassword.setError("Vui lòng nhập mật khẩu");
            return false;
        }
        if (password.length() < 6) {
            txtPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            return false;
        }
        if (!password.equals(confirmPass)) {
            txtConfirmPass.setError("Mật khẩu xác nhận không khớp");
            return false;
        }
        if (name.isEmpty()) {
            txtName.setError("Vui lòng nhập họ và tên");
            return false;
        }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            txtEmail.setError("Email không đúng định dạng");
            return false;
        }
        if (!tel.isEmpty() && tel.length() < 8) {
            txtTel.setError("Số điện thoại không hợp lệ");
            return false;
        }

        return true;
    }

    private void submit() {
        if (!validateInput()) {
            return;
        }

        String username = txtUsername.getText().toString().trim();
        String password = txtPassword.getText().toString();
        String name = txtName.getText().toString().trim();
        String email = txtEmail.getText().toString().trim();
        String tel = txtTel.getText().toString().trim();

        Player player = new Player(username, password);
        player.setName(name);
        player.setEmail(email.isEmpty() ? null : email);
        player.setTel(tel.isEmpty() ? null : tel);

        myControl.sendData(new ObjectWrapper(ObjectWrapper.REGISTER_USER, player));
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        if (data.getPerformative() == ObjectWrapper.REPLY_REGISTER_USER) {
            if ("ok".equals(data.getData())) {
                toast("Đăng ký tài khoản thành công!");
                finish();
            } else {
                txtUsername.setError("Tên đăng nhập đã tồn tại");
                toast("Tên đăng nhập đã tồn tại");
            }
        }
    }
}
