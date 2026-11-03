package client;

import javax.swing.SwingUtilities;

import client.view.ClientMainFrm;

/** Chủ: N04. Điểm vào phía Client. */
public class ClientRun {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientMainFrm().setVisible(true));
    }
}
