package server;

import javax.swing.SwingUtilities;

import server.view.ServerMainFrm;
public class ServerRun {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerMainFrm().setVisible(true));
    }
}
