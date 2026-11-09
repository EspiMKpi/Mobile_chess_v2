package client.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Màn hình khởi động (LAUNCHER): nhập địa chỉ IP, cổng server rồi [Kết nối].
 * Máy ảo: 10.0.2.2 · điện thoại thật: IP LAN của PC chạy server.
 */
public class ClientMainActivity extends BaseActivity {

    private EditText txtServerHost;
    private EditText txtServerPort;
    private Button btnConnect;
    private final ExecutorService connectExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);
        setTitle(R.string.title_connect);
        // TODO: myControl.init(this); findViewById các ô nhập + nút; btnConnect -> connect()
    }

    @Override
    protected boolean requiresConnection() {
        return false;
    }

    /**
     * Kiểm tra host không trống, port là số 1..65535; setServerAddress();
     * openConnection() trên connectExecutor; thành công -> mở LoginActivity, lỗi -> "Không kết nối được server".
     */
    private void connect() {
        // TODO
    }

    @Override
    protected void onDestroy() {
        // TODO: connectExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // Màn hình này không nhận gói tin.
    }
}
