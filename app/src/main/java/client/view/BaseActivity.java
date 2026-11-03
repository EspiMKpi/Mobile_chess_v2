package client.view;

import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import client.control.ClientCtr;

/**
 * Chủ: N04. Lớp cha của mọi Activity phía N04 [LỆCH BC: bổ sung để xử lý vòng đời Android].
 * - onResume: báo ClientCtr màn hình nào đang hiện; mất kết nối (app bị dọn khi ở nền) -> về màn hình kết nối.
 * - onDestroy: gỡ mọi đăng ký nhận gói tin của màn hình này.
 */
public abstract class BaseActivity extends AppCompatActivity implements DataReceiver {

    protected final ClientCtr myControl = ClientCtr.getInstance();

    /** Màn hình kết nối không cần kiểm tra kết nối. */
    protected boolean requiresConnection() {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        myControl.setCurrentActivity(this);
        if (requiresConnection() && !myControl.isConnected()) {
            Intent i = new Intent(this, ClientMainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }
    }

    @Override
    protected void onPause() {
        if (myControl.getCurrentActivity() == this) {
            myControl.setCurrentActivity(null);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        myControl.unregister(this);
        super.onDestroy();
    }

    /** Đăng ký nhận nhiều mã lệnh một lúc - gọi trong onCreate, TRƯỚC khi gửi yêu cầu. */
    protected void listen(int... codes) {
        for (int code : codes) {
            myControl.register(code, this);
        }
    }

    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    protected void confirm(String msg, Runnable onYes) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, (d, w) -> onYes.run())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
