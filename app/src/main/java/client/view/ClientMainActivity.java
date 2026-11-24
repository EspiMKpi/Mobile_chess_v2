package client.view;

// [HƯỚNG DẪN BƯỚC 5 - Dọn file cũ] Sau khi bạn làm xong Bước 2, Bước 3 (ClientCtr hết import ClientMainActivity)
// và Bước 4, hãy XÓA file ClientMainActivity.java này (vì đã bỏ màn hình kết nối thủ công trong báo cáo .docx mới).

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import model.IPAddress;
import model.ObjectWrapper;
import vn.edu.ptit.mobilechess.R;

/** Chủ: N04. Màn hình kết nối server (hình 4.2a). Máy ảo dùng 10.0.2.2, điện thoại thật dùng IP LAN của PC. */
public class ClientMainActivity extends BaseActivity implements View.OnClickListener {

    private EditText txtHost;
    private EditText txtPort;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);
        setTitle(R.string.title_connect);

        txtHost = findViewById(R.id.txtServerHost);
        txtPort = findViewById(R.id.txtServerPort);
        btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() != R.id.btnConnect) {
            return;
        }
        String host = txtHost.getText().toString().trim();
        if (host.isEmpty()) {
            txtHost.setError("Nhập địa chỉ server");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().toString().trim());
        } catch (NumberFormatException e) {
            txtPort.setError("Cổng không hợp lệ");
            return;
        }

        btnConnect.setEnabled(false);
        myControl.closeConnection();                          // bỏ kết nối cũ (nếu có)
        myControl.setServerAddress(new IPAddress(host, port));
        new Thread(() -> {
            boolean ok = myControl.openConnection();           // mạng -> luồng nền
            runOnUiThread(() -> {
                btnConnect.setEnabled(true);
                if (ok) {
                    startActivity(new Intent(ClientMainActivity.this, LoginActivity.class));
                } else {
                    Toast.makeText(ClientMainActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        // Màn hình này không nhận gói tin nào.
    }
}
