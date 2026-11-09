package client.view;

import android.os.Bundle;
import android.widget.EditText;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Đăng nhập (hình 4.4). Nhận REPLY_LOGIN_USER. */
public class LoginActivity extends BaseActivity {

    private EditText txtUsername;
    private EditText txtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle(R.string.title_login);
        // TODO: bước 1 - listen(REPLY_LOGIN_USER)
        // TODO: findViewById; btnLogin -> login(); btnRegister -> mở RegisterActivity (hình 4.3, bước 2)
    }

    /** Bước 3-7: kiểm tra không trống, đóng gói new Player(username, password), sendData(LOGIN_USER). */
    private void login() {
        // TODO
    }

    /**
     * Bước 18-21: nhận Player -> myControl.setMyPlayer(p), mở OnlineListActivity, finish().
     * "false" -> "Sai tên đăng nhập hoặc mật khẩu".
     */
    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // TODO
    }
}
