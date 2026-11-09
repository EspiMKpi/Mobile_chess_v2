package server.view;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import server.control.ServerCtr;

/**
 * Chủ: N04. Giao diện Server: txtServerHost (chỉ đọc), txtServerPort (mặc định 9999),
 * btnStartServer, btnStopServer, mainText (log).
 */
public class ServerMainFrm extends JFrame {

    public static final int DEFAULT_PORT = 9999;

    private JTextField txtServerHost;
    private JTextField txtServerPort;
    private JButton btnStartServer;
    private JButton btnStopServer;
    private JTextArea mainText;

    private ServerCtr myServer;

    public ServerMainFrm() {
        super("Chess Server Control Panel");
        // TODO: dựng giao diện (BorderLayout: panel nút ở NORTH, mainText trong JScrollPane ở CENTER)
        // TODO: btnStartServer -> startServer(); btnStopServer -> stopServer(); đóng cửa sổ -> stopServer()
    }

    /** new ServerCtr(this, port).openServer(); thành công thì khóa nút Start, mở nút Stop. */
    private void startServer() {
        // TODO
    }

    /** myServer.stopServer(); mở lại nút Start. */
    private void stopServer() {
        // TODO
    }

    /** Liệt kê các IPv4 không phải loopback để client biết địa chỉ kết nối. */
    private String resolveServerIps() {
        // TODO
        return "";
    }

    /** Ghi một dòng log (an toàn từ mọi luồng: dùng SwingUtilities.invokeLater). */
    public void showMessage(String s) {
        // TODO
    }

    public int getPort() {
        // TODO: đọc txtServerPort, lỗi -> DEFAULT_PORT
        return DEFAULT_PORT;
    }
}
