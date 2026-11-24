package client.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import model.ObjectWrapper;
import model.Player;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Đăng ký tài khoản (hình 4.2a, 4.3). */
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

        myControl.register(ObjectWrapper.REPLY_REGISTER_USER, this);   // bước 3

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
        if (v.getId() == R.id.btnSubmit) {                             // bước 5
            if (!validateInput()) {                                    // bước 6
                return;
            }
            Player player = new Player();                              // bước 7-8
            player.setUsername(txtUsername.getText().toString().trim());
            player.setPassword(txtPassword.getText().toString());
            player.setName(txtName.getText().toString().trim());
            player.setEmail(txtEmail.getText().toString().trim());
            player.setTel(txtTel.getText().toString().trim());

            // [HƯỚNG DẪN BƯỚC 4b - Hình 4.3, bước 9-16] Thay đoạn sendData bên dưới bằng đoạn sau để tự tìm
            // server qua openConnection() trên luồng nền (vì người dùng mở RegisterActivity ngay từ LoginActivity
            // khi chưa kết nối TCP) rồi mới gửi gói tin REGISTER_USER:
            //
            // btnRegister.setEnabled(false);
            // new Thread(() -> {
            //     boolean connected = myControl.openConnection();
            //     if (!connected) {
            //         runOnUiThread(() -> {
            //             if (isFinishing() || isDestroyed()) {
            //                 return;
            //             }
            //             btnRegister.setEnabled(true);
            //             Toast.makeText(RegisterActivity.this,
            //                     "Không tìm thấy hoặc không kết nối được server", Toast.LENGTH_SHORT).show();
            //         });
            //         return;
            //     }
            //     if (!myControl.sendData(new ObjectWrapper(ObjectWrapper.REGISTER_USER, player))) {
            //         runOnUiThread(() -> {
            //             if (isFinishing() || isDestroyed()) {
            //                 return;
            //             }
            //             btnRegister.setEnabled(true);
            //             Toast.makeText(RegisterActivity.this, "Chưa kết nối server", Toast.LENGTH_SHORT).show();
            //         });
            //     }
            // }).start();

            if (!myControl.sendData(new ObjectWrapper(ObjectWrapper.REGISTER_USER, player))) {   // bước 9
                Toast.makeText(this, "Chưa kết nối server", Toast.LENGTH_SHORT).show();
            }
        } else if (v.getId() == R.id.btnCancel) {
            finish();
        }
    }

    /** Mục 1.3: tên đăng nhập, mật khẩu, họ tên không trống; mật khẩu khớp xác nhận. */
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
        return true;
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {              // bước 17
        if (d.getPerformative() != ObjectWrapper.REPLY_REGISTER_USER) {
            return;
        }
        // [HƯỚNG DẪN BƯỚC 4b] Mở lại nút Đăng ký khi nhận phản hồi (nếu trùng username để người dùng sửa lại):
        // btnRegister.setEnabled(true);
        if ("ok".equals(d.getData())) {                                // bước 18
            Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
            finish();                                                  // về LoginActivity
        } else {
            Toast.makeText(this, "Tên đăng nhập đã tồn tại", Toast.LENGTH_SHORT).show();
        }
    }
}
