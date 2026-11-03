package client.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import model.ObjectWrapper;
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
        // TODO(N04, M2): sendData(new ObjectWrapper(LOGIN_USER, new Player(username, password)))
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO(N04, M2): Player -> setMyPlayer, mở OnlineListActivity, finish()
        //   "false" -> "Sai tên đăng nhập hoặc mật khẩu" · "logged_in" -> "Tài khoản đang đăng nhập ở thiết bị khác"
    }
}
