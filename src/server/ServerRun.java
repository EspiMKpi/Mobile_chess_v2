package server;

import javax.swing.SwingUtilities;

import server.view.ServerMainFrm;

/** Chủ: N04. Điểm vào phía Server. */
public class ServerRun {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerMainFrm().setVisible(true));
    }
}
