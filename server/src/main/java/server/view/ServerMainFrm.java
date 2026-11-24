package server.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import server.control.ServerCtr;

/** Chủ: N04. Giao diện Server (hình 4.2b). */
public class ServerMainFrm extends JFrame implements ActionListener {

    private JTextField txtServerHost;
    private JTextField txtServerPort;
    private JButton btnStartServer;
    private JButton btnStopServer;
    private JTextArea mainText;
    private ServerCtr myServer;

    public ServerMainFrm() {
        super("Chess Server");

        txtServerHost = new JTextField(14);
        txtServerHost.setEditable(false);
        try {
            txtServerHost.setText(InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            txtServerHost.setText("localhost");
        }
        txtServerPort = new JTextField("9999", 6);

        btnStartServer = new JButton("Start");
        btnStopServer = new JButton("Stop");
        btnStopServer.setEnabled(false);
        btnStartServer.addActionListener(this);
        btnStopServer.addActionListener(this);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Host:"));
        top.add(txtServerHost);
        top.add(new JLabel("Port:"));
        top.add(txtServerPort);
        top.add(btnStartServer);
        top.add(btnStopServer);

        mainText = new JTextArea(20, 60);
        mainText.setEditable(false);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(mainText), BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnStartServer) {
            int port;
            try {
                port = Integer.parseInt(txtServerPort.getText().trim());
            } catch (NumberFormatException ex) {
                showMessage("Cổng không hợp lệ");
                return;
            }
            myServer = new ServerCtr(this, port);
            myServer.openServer();
            btnStartServer.setEnabled(false);
            btnStopServer.setEnabled(true);
            txtServerPort.setEditable(false);
        } else if (e.getSource() == btnStopServer) {
            if (myServer != null) {
                myServer.stopServer();
                myServer = null;
            }
            btnStartServer.setEnabled(true);
            btnStopServer.setEnabled(false);
            txtServerPort.setEditable(true);
        }
    }

    /** Ghi một dòng log; gọi được từ mọi luồng. */
    public void showMessage(String s) {
        SwingUtilities.invokeLater(() -> {
            mainText.append(s + "\n");
            mainText.setCaretPosition(mainText.getDocument().getLength());
        });
    }
}
