package client.view;

import javax.swing.JFrame;

/** Chủ: N04. Màn hình kết nối: host/port + [Kết nối] -> openConnection() -> LoginFrm. */
public class ClientMainFrm extends JFrame {

    public ClientMainFrm() {
        super("Chess Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);
        setLocationRelativeTo(null);
        // TODO(N04, M1): txtHost, txtPort, btnConnect -> new ClientCtr(this).openConnection()
    }

    /** Đưa về trạng thái chưa kết nối (sau đăng xuất / mất kết nối). */
    public void resetClient() {
        // TODO(N04, M1)
    }
}
