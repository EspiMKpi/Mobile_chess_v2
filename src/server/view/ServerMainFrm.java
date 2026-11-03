package server.view;

import javax.swing.JFrame;

/**
 * Chủ: N04. Giao diện Server (hình 2.6): txtServerHost (chỉ đọc), txtServerPort (9999),
 * btnStartServer, btnStopServer, mainText (log).
 */
public class ServerMainFrm extends JFrame {

    public static final int DEFAULT_PORT = 9999;

    public ServerMainFrm() {
        super("Chess Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        // TODO(N04, M1): dựng giao diện; Start -> myServer = new ServerCtr(this); myServer.openServer()
    }

    /** Ghi một dòng log (an toàn từ mọi luồng). */
    public void showMessage(String s) {
        // TODO(N04, M1): SwingUtilities.invokeLater(() -> mainText.append(s + "\n"))
    }
}
