package client.view;

import androidx.appcompat.app.AppCompatActivity;

import client.control.ClientCtr;

/** Chủ: N04. Lớp cha của các Activity (hình 4.2a). */
public abstract class BaseActivity extends AppCompatActivity implements DataReceiver {

    protected ClientCtr myControl = ClientCtr.getInstance();

    /** Báo ClientCtr màn hình nào đang hiển thị. */
    @Override
    protected void onResume() {
        super.onResume();
        myControl.setCurrentActivity(this);
    }

    /** Gỡ mọi đăng ký nhận gói tin của màn hình này. */
    @Override
    protected void onDestroy() {
        myControl.unregister(this);
        super.onDestroy();
    }

    // [HƯỚNG DẪN BỔ SUNG] Có thể thêm các hàm tiện ích dùng chung cho mọi Activity:
    // protected void listen(int... codes) {
    //     for (int c : codes) myControl.register(c, this);
    // }
    // protected void toast(String msg) {
    //     android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    // }
}
