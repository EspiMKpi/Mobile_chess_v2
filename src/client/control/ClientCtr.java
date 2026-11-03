package client.control;

import java.util.ArrayList;

import model.ObjectWrapper;

/**
 * Chủ: N04. Bộ điều khiển phía Client (Socket, ClientListening, danh sách myFunction).
 * Phase 0 chỉ chốt sendData() mà ChessBoardPanel của N11 cần gọi.
 */
public class ClientCtr {

    private final ArrayList<ObjectWrapper> myFunction = new ArrayList<>();

    // TODO(N04): openConnection(), closeConnection(), ClientListening (inner class) ...

    /** Gửi gói tin lên Server. */
    public boolean sendData(Object obj) {
        // TODO(N04)
        return false;
    }

    public ArrayList<ObjectWrapper> getActiveFunction() {
        return myFunction;
    }
}
