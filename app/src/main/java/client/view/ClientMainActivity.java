package client.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/**
 * Chủ: N04. Màn hình khởi động (LAUNCHER): nhập host/port server rồi [Kết nối].
 * Máy ảo: 10.0.2.2 · điện thoại thật: IP LAN của PC chạy server.
 */
public class ClientMainActivity extends BaseActivity {

    private EditText txtServerHost;
    private EditText txtServerPort;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);
        setTitle(R.string.title_connect);
        myControl.init(this);

        txtServerHost = findViewById(R.id.txtServerHost);
        txtServerPort = findViewById(R.id.txtServerPort);
        btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(v -> connect());
    }

    @Override
    protected boolean requiresConnection() {
        return false;
    }

    private void connect() {
        // TODO(N04, M1): myControl.setServerAddress(new IPAddress(host, port));
        //   chạy myControl.openConnection() trên luồng nền; xong runOnUiThread:
        //   true -> startActivity(LoginActivity) · false -> toast("Không kết nối được server")
    }

    /** Đưa về trạng thái chưa kết nối (sau đăng xuất / mất kết nối). */
    public void resetClient() {
        // TODO(N04, M1)
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // Màn hình này không nhận gói tin.
    }
}
