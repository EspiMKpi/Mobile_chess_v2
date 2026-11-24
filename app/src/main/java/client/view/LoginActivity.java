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

/** Chủ: N04. Màn hình đầu tiên của ứng dụng: Đăng nhập (Hình 4.1, 4.4). */
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

        listen(ObjectWrapper.REPLY_LOGIN_USER);

        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin.setOnClickListener(this);
        btnRegister.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
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
            Player player = new Player(username, password);
            btnLogin.setEnabled(false);
            new Thread(() -> {
                boolean connected = myControl.openConnection();
                if (!connected) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        btnLogin.setEnabled(true);
                        Toast.makeText(this, "Không tìm thấy hoặc không kết nối được server", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                if (!myControl.sendData(new ObjectWrapper(ObjectWrapper.LOGIN_USER, player))) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        btnLogin.setEnabled(true);
                        toast("Chưa kết nối server");
                    });
                }
            }).start();
        } else if (v.getId() == R.id.btnRegister) {
            startActivity(new Intent(this, RegisterActivity.class));
        }
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        if (d.getPerformative() != ObjectWrapper.REPLY_LOGIN_USER) {
            return;
        }
        btnLogin.setEnabled(true);
        if (d.getData() instanceof Player) {
            myControl.setMyPlayer((Player) d.getData());
            startActivity(new Intent(this, OnlineListActivity.class));
            finish();
        } else {
            toast("Sai tên đăng nhập hoặc mật khẩu");
        }
    }
}
