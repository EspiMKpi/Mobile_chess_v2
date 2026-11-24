package client.view;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import model.ObjectWrapper;
import model.Player;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Đăng ký tài khoản (Hình 4.1, 4.3). */
public class RegisterActivity extends BaseActivity implements View.OnClickListener {

    private EditText txtUsername;
    private EditText txtPassword;
    private EditText txtConfirmPassword;
    private EditText txtName;
    private EditText txtEmail;
    private EditText txtTel;
    private Button btnRegister;
    private Button btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setTitle(R.string.title_register);

        listen(ObjectWrapper.REPLY_REGISTER_USER);

        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);
        txtConfirmPassword = findViewById(R.id.txtConfirmPass);
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        txtTel = findViewById(R.id.txtTel);
        btnRegister = findViewById(R.id.btnSubmit);
        btnCancel = findViewById(R.id.btnCancel);
        btnRegister.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSubmit) {
            if (!validateInput()) {
                return;
            }
            Player player = new Player();
            player.setUsername(txtUsername.getText().toString().trim());
            player.setPassword(txtPassword.getText().toString());
            player.setName(txtName.getText().toString().trim());
            player.setEmail(txtEmail.getText().toString().trim());
            player.setTel(txtTel.getText().toString().trim());

            btnRegister.setEnabled(false);
            new Thread(() -> {
                boolean connected = myControl.openConnection();
                if (!connected) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        btnRegister.setEnabled(true);
                        Toast.makeText(this, "Không tìm thấy hoặc không kết nối được server", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                if (!myControl.sendData(new ObjectWrapper(ObjectWrapper.REGISTER_USER, player))) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        btnRegister.setEnabled(true);
                        toast("Chưa kết nối server");
                    });
                }
            }).start();
        } else if (v.getId() == R.id.btnCancel) {
            finish();
        }
    }

    public boolean validateInput() {
        if (txtUsername.getText().toString().trim().isEmpty()) {
            txtUsername.setError("Nhập tên đăng nhập");
            return false;
        }
        if (txtPassword.getText().toString().isEmpty()) {
            txtPassword.setError("Nhập mật khẩu");
            return false;
        }
        if (!txtPassword.getText().toString().equals(txtConfirmPassword.getText().toString())) {
            txtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return false;
        }
        if (txtName.getText().toString().trim().isEmpty()) {
            txtName.setError("Nhập họ tên");
            return false;
        }
        String email = txtEmail.getText().toString().trim();
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            txtEmail.setError("Email không hợp lệ");
            return false;
        }
        return true;
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        if (d.getPerformative() != ObjectWrapper.REPLY_REGISTER_USER) {
            return;
        }
        btnRegister.setEnabled(true);
        if ("ok".equals(d.getData())) {
            toast("Đăng ký thành công");
            finish();
        } else {
            toast("Tên đăng nhập đã tồn tại");
        }
    }
}
