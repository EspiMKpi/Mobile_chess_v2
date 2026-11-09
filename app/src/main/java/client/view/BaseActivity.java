package client.view;

import androidx.appcompat.app.AppCompatActivity;

import client.control.ClientCtr;

/**
 * Chủ: N04. Lớp cha của mọi Activity phía N04 (xử lý vòng đời Android).
 * - onResume: báo ClientCtr màn hình nào đang hiện; mất kết nối -> về màn hình kết nối.
 * - onPause: bỏ đánh dấu màn hình hiện tại.
 * - onDestroy: gỡ mọi đăng ký nhận gói tin của màn hình này.
 */
public abstract class BaseActivity extends AppCompatActivity implements DataReceiver {

    protected final ClientCtr myControl = ClientCtr.getInstance();

    /** Màn hình kết nối override trả false (không cần kiểm tra kết nối). */
    protected boolean requiresConnection() {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: myControl.setCurrentActivity(this);
        //       requiresConnection() && !isConnected() -> mở ClientMainActivity (NEW_TASK | CLEAR_TASK), finish()
    }

    @Override
    protected void onPause() {
        // TODO: nếu currentActivity == this thì setCurrentActivity(null)
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // TODO: myControl.unregister(this);
        super.onDestroy();
    }

    /** Đăng ký nhận nhiều mã lệnh một lúc - gọi trong onCreate, TRƯỚC khi gửi yêu cầu. */
    protected void listen(int... codes) {
        // TODO
    }

    protected void toast(String msg) {
        // TODO
    }

    /** Hộp thoại xác nhận OK / Hủy; OK -> onYes.run(). */
    protected void confirm(String msg, Runnable onYes) {
        // TODO
    }
}
