package server.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import server.control.ServerCtr;

/**
 * Chủ: N04. Giao diện Server (hình 2.6): txtServerHost (chỉ đọc), txtServerPort (9999),
 * btnStartServer, btnStopServer, mainText (log).
 */
public class ServerMainFrm extends JFrame {

    public static final int DEFAULT_PORT = 9999;

    private final JTextField txtServerHost;
    private final JTextField txtServerPort;
    private final JButton btnStartServer;
    private final JButton btnStopServer;
    private final JTextArea mainText;

    private ServerCtr myServer;

    public ServerMainFrm() {
        super("Chess Server Control Panel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        // Top control panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        String hostStr = resolveServerIps();
        topPanel.add(new JLabel("Server Host:"));
        txtServerHost = new JTextField(hostStr, 18);
        txtServerHost.setEditable(false);
        txtServerHost.setToolTipText("IP dùng để kết nối từ thiết bị client");
        topPanel.add(txtServerHost);

        topPanel.add(new JLabel("Port:"));
        txtServerPort = new JTextField(String.valueOf(DEFAULT_PORT), 5);
        topPanel.add(txtServerPort);

        btnStartServer = new JButton("Khởi động Server");
        btnStopServer = new JButton("Dừng Server");
        btnStopServer.setEnabled(false);

        topPanel.add(btnStartServer);
        topPanel.add(btnStopServer);
        add(topPanel, BorderLayout.NORTH);

        // Center log area
        mainText = new JTextArea();
        mainText.setEditable(false);
        mainText.setLineWrap(true);
        mainText.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(mainText);
        add(scrollPane, BorderLayout.CENTER);

        // Actions
        btnStartServer.addActionListener(e -> {
            int port = DEFAULT_PORT;
            try {
                port = Integer.parseInt(txtServerPort.getText().trim());
            } catch (NumberFormatException ex) {
                showMessage("Cổng không hợp lệ, dùng cổng mặc định " + DEFAULT_PORT);
                txtServerPort.setText(String.valueOf(DEFAULT_PORT));
            }
            myServer = new ServerCtr(this, port);
            if (myServer.openServer()) {
                btnStartServer.setEnabled(false);
                btnStopServer.setEnabled(true);
                txtServerPort.setEditable(false);
                showMessage("Server đã khởi động lắng nghe tại cổng " + port);
                showMessage("Địa chỉ kết nối máy ảo Android: 10.0.2.2:" + port);
                showMessage("Địa chỉ kết nối điện thoại thật: " + hostStr + ":" + port);
            } else {
                showMessage("Lỗi: Không thể mở ServerSocket tại cổng " + port);
            }
        });

        btnStopServer.addActionListener(e -> {
            if (myServer != null) {
                myServer.stopServer();
                myServer = null;
            }
            btnStartServer.setEnabled(true);
            btnStopServer.setEnabled(false);
            txtServerPort.setEditable(true);
            showMessage("Server đã dừng.");
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (myServer != null) {
                    myServer.stopServer();
                }
            }
        });

        showMessage("Sẵn sàng khởi động Server. Bấm [Khởi động Server] để bắt đầu.");
    }

    private String resolveServerIps() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (ips.isEmpty()) {
            try {
                ips.add(InetAddress.getLocalHost().getHostAddress());
            } catch (Exception ignored) {
                ips.add("127.0.0.1");
            }
        }

        return String.join(", ", ips);
    }

    /** Ghi một dòng log (an toàn từ mọi luồng). */
    public void showMessage(String s) {
        SwingUtilities.invokeLater(() -> {
            mainText.append(s + "\n");
            mainText.setCaretPosition(mainText.getDocument().getLength());
        });
    }

    public int getPort() {
        try {
            return Integer.parseInt(txtServerPort.getText().trim());
        } catch (Exception e) {
            return DEFAULT_PORT;
        }
    }
}
