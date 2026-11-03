package client.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import model.ObjectWrapper;
import model.Player;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Hình 2.1. Nhận REPLY_LOGIN_USER. */
public class LoginActivity extends BaseActivity {

    private EditText txtUsername;
    private EditText txtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle(R.string.title_login);
        listen(ObjectWrapper.REPLY_LOGIN_USER);

        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);
        findViewById(R.id.btnLogin).setOnClickListener(v -> login());
        findViewById(R.id.btnRegister).setOnClickListener(
                v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String username = txtUsername.getText().toString().trim();
        String password = txtPassword.getText().toString();

        if (username.isEmpty()) {
            txtUsername.setError("Vui lòng nhập tên đăng nhập");
            return;
        }
        if (password.isEmpty()) {
            txtPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }

        myControl.sendData(new ObjectWrapper(ObjectWrapper.LOGIN_USER, new Player(username, password)));
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        if (data.getPerformative() == ObjectWrapper.REPLY_LOGIN_USER) {
            Object obj = data.getData();
            if (obj instanceof Player) {
                myControl.setMyPlayer((Player) obj);
                startActivity(new Intent(this, OnlineListActivity.class));
                finish();
            } else if ("logged_in".equals(obj)) {
                toast("Tài khoản đang đăng nhập ở thiết bị khác");
            } else {
                toast("Sai tên đăng nhập hoặc mật khẩu");
            }
        }
    }
}
