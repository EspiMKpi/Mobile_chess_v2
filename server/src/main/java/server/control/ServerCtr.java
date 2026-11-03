package server.control;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import model.ObjectWrapper;
import model.Player;
import model.PlayerStat;

/**
 * Chủ: N04. Bộ điều khiển phía Server (ServerSocket, ServerListening, ServerProcessing).
 * Phase 0 chỉ chốt các chữ ký mà phần của N11 (ChessReferee) cần gọi.
 */
public class ServerCtr {

    private final ArrayList<ServerProcessing> myProcess = new ArrayList<>();
    private final ArrayList<GameRoom> myRooms = new ArrayList<>();

    // TODO(N04): xem docs/BLUEPRINT_N04.md mục "ServerCtr" - openServer, stopServer, findProcess,
    //            removeProcess, createRoom, removeRoom, getOnlinePlayers, broadcastOnlineList ...

    /** Luồng xử lý 1 client. Chủ: N04. */
    public class ServerProcessing extends Thread {
        private Socket mySocket;
        private ObjectOutputStream oos;
        private Player loggedInPlayer;
        private String status = PlayerStat.IDLE;
        private GameRoom room;

        public ServerProcessing(Socket s) {
            this.mySocket = s;
        }

        @Override
        public void run() {
            // TODO(N04): đọc ObjectWrapper, switch performative.
            //   case ObjectWrapper.SEND_MOVE:
            //       if (room != null) room.handleMove(this, (MoveRequest) data);   // giao cho N11
            //   case ObjectWrapper.QUIT_GAME: room.endGame(đối thủ, GameResult.RESIGN)
            //   khi mất kết nối & room != null: room.endGame(đối thủ, GameResult.DISCONNECT)
        }

        /** Gửi gói tin về client này (được ChessReferee của N11 gọi). */
        public synchronized void sendData(Object obj) {
            // TODO(N04): oos.writeObject(obj); oos.flush();
        }

        public Player getLoggedInPlayer() { return loggedInPlayer; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public GameRoom getRoom() { return room; }
        public void setRoom(GameRoom room) { this.room = room; }
    }
}
