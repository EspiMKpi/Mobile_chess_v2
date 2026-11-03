package client.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.IPAddress;
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
    private final ExecutorService connectExecutor = Executors.newSingleThreadExecutor();

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
        String host = txtServerHost.getText().toString().trim();
        String portStr = txtServerPort.getText().toString().trim();

        if (host.isEmpty()) {
            txtServerHost.setError("Vui lòng nhập địa chỉ server");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) {
                txtServerPort.setError("Cổng không hợp lệ");
                return;
            }
        } catch (NumberFormatException e) {
            txtServerPort.setError("Cổng phải là số nguyên");
            return;
        }

        btnConnect.setEnabled(false);
        myControl.setServerAddress(new IPAddress(host, port));

        connectExecutor.execute(() -> {
            boolean ok = myControl.openConnection();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                btnConnect.setEnabled(true);
                if (ok) {
                    startActivity(new Intent(this, LoginActivity.class));
                } else {
                    toast("Không kết nối được server");
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        connectExecutor.shutdownNow();
        super.onDestroy();
    }

    /** Đưa về trạng thái chưa kết nối (sau đăng xuất / mất kết nối). */
    public void resetClient() {
        myControl.closeConnection();
        if (btnConnect != null) {
            btnConnect.setEnabled(true);
        }
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper data) {
        // Màn hình này không nhận gói tin.
    }
}
