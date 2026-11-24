package client.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import model.ObjectWrapper;
import model.Player;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Đăng nhập (hình 4.2a, 4.4). */
public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private EditText txtUsername;
    private EditText txtPassword;
    private Button btnLogin;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle(R.string.title_login);

        myControl.register(ObjectWrapper.REPLY_LOGIN_USER, this);    // bước 1

        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin.setOnClickListener(this);
        btnRegister.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {                                    // bước 3
        if (v.getId() == R.id.btnLogin) {
            String username = txtUsername.getText().toString().trim();
            String password = txtPassword.getText().toString();
            if (username.isEmpty()) {
                txtUsername.setError("Nhập tên đăng nhập");
                return;
            }
            if (password.isEmpty()) {
                txtPassword.setError("Nhập mật khẩu");
                return;
            }
            Player player = new Player();                            // bước 4-5
            player.setUsername(username);
            player.setPassword(password);

            // [HƯỚNG DẪN BƯỚC 4a - Hình 4.4, bước 6-13] Thay đoạn sendData bên dưới bằng đoạn sau để tự tìm
            // server qua openConnection() trên luồng nền rồi mới gửi gói tin LOGIN_USER:
            //
            // btnLogin.setEnabled(false);
            // new Thread(() -> {
            //     boolean connected = myControl.openConnection();
            //     if (!connected) {
            //         runOnUiThread(() -> {
            //             if (isFinishing() || isDestroyed()) {
            //                 return;
            //             }
            //             btnLogin.setEnabled(true);
            //             Toast.makeText(LoginActivity.this,
            //                     "Không tìm thấy hoặc không kết nối được server", Toast.LENGTH_SHORT).show();
            //         });
            //         return;
            //     }
            //     if (!myControl.sendData(new ObjectWrapper(ObjectWrapper.LOGIN_USER, player))) {
            //         runOnUiThread(() -> {
            //             if (isFinishing() || isDestroyed()) {
            //                 return;
            //             }
            //             btnLogin.setEnabled(true);
            //             Toast.makeText(LoginActivity.this, "Chưa kết nối server", Toast.LENGTH_SHORT).show();
            //         });
            //     }
            // }).start();

            if (!myControl.sendData(new ObjectWrapper(ObjectWrapper.LOGIN_USER, player))) {   // bước 6
                Toast.makeText(this, "Chưa kết nối server", Toast.LENGTH_SHORT).show();
            }
        } else if (v.getId() == R.id.btnRegister) {                  // hình 4.3, bước 2
            startActivity(new Intent(this, RegisterActivity.class));
        }
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {            // bước 18
        if (d.getPerformative() != ObjectWrapper.REPLY_LOGIN_USER) {
            return;
        }
        // [HƯỚNG DẪN BƯỚC 4a] Mở lại nút Đăng nhập khi nhận phản hồi (nhất là lúc sai tài khoản/mật khẩu):
        // btnLogin.setEnabled(true);
        if (d.getData() instanceof Player) {
            myControl.setMyPlayer((Player) d.getData());             // bước 19
            startActivity(new Intent(this, OnlineListActivity.class));   // bước 20
            finish();
        } else {
            Toast.makeText(this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show();
        }
    }
}
