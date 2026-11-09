package server.control;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import model.IPAddress;
import model.ObjectWrapper;
import model.Player;
import model.PlayerStat;
import server.dao.MatchDAO;
import server.dao.PlayerDAO;
import server.view.ServerMainFrm;

/**
 * Chủ: N04. Bộ điều khiển phía Server: ServerSocket, luồng ServerListening nhận kết nối,
 * mỗi client một luồng ServerProcessing. Giữ danh sách phiên (myProcess) và phòng đấu (myRooms).
 */
public class ServerCtr {

    private ServerMainFrm view;
    private IPAddress myAddress;
    private ServerSocket myServerSocket;
    private ServerListening myListening;

    private final ArrayList<ServerProcessing> myProcess = new ArrayList<>();
    private final ArrayList<GameRoom> myRooms = new ArrayList<>();

    /** Khóa riêng cho ghép cặp: bao quanh kiểm tra trạng thái + createRoom. KHÔNG lấy khóa GameRoom bên trong. */
    private final Object matchmakingLock = new Object();

    private final PlayerDAO playerDAO = new PlayerDAO();
    private final MatchDAO matchDAO = new MatchDAO();

    public ServerCtr(ServerMainFrm v) {
        this(v, ServerMainFrm.DEFAULT_PORT);
    }

    public ServerCtr(ServerMainFrm v, int port) {
        this.view = v;
        this.myAddress = new IPAddress("0.0.0.0", port);
    }

    /** Mở ServerSocket tại cổng myAddress, khởi động myListening. Lỗi -> view.showMessage, trả false. */
    public boolean openServer() {
        // TODO
        return false;
    }

    /** Đóng ServerSocket, closeRoom() mọi phòng, đóng socket mọi client, xóa 2 danh sách. */
    public void stopServer() {
        // TODO
    }

    /** Tìm phiên đang đăng nhập bằng playerId (null nếu không online). */
    public ServerProcessing findProcess(int playerId) {
        // TODO
        return null;
    }

    public void removeProcess(ServerProcessing sp) {
        // TODO
    }

    /**
     * Quản lý phiên đăng nhập (hình 4.4, bước 14-16):
     * - Tài khoản đang có phiên ở máy khác: removeProcess + đóng socket phiên cũ (máy cũ về màn hình kết nối).
     * - sp.setLoggedInPlayer(p); sp.setStatus(PlayerStat.IDLE).
     * - broadcastOnlineList().
     */
    public void login(ServerProcessing sp, Player p) {
        // TODO
    }

    /**
     * Kiểm tra a, b khác null, khác nhau, chưa có phòng, đang IDLE rồi createRoom() - tất cả trong matchmakingLock.
     * Không ghép được (B bận / không còn online) -> null.
     */
    public GameRoom tryCreateRoom(ServerProcessing a, ServerProcessing b) {
        // TODO
        return null;
    }

    /**
     * Tạo phòng (hình 4.11, bước 20-22): new GameRoom(this, a, b), cả hai setRoom + BUSY,
     * thêm vào myRooms, room.startChallengeTimer() (hẹn giờ 30 giây chờ B trả lời), broadcastOnlineList().
     */
    public GameRoom createRoom(ServerProcessing a, ServerProcessing b) {
        // TODO
        return null;
    }

    public void removeRoom(GameRoom r) {
        // TODO
    }

    /** Danh sách online (hình 4.11, bước 4-9): mỗi phiên đã đăng nhập -> getTotalPoint() -> new PlayerStat(p, point, status). */
    public ArrayList<PlayerStat> getOnlinePlayers() {
        // TODO
        return new ArrayList<>();
    }

    /** Điểm + thông tin một người chơi để gửi kèm SERVER_START_MATCH (hình 4.12, bước 9-10). */
    public PlayerStat getPlayerStat(ServerProcessing sp) {
        // TODO
        return null;
    }

    /** Gửi SERVER_INFORM_ONLINE_LIST (getOnlinePlayers()) cho mọi phiên đã đăng nhập. */
    public void broadcastOnlineList() {
        // TODO
    }

    public ArrayList<ServerProcessing> getMyProcess() {
        return myProcess;
    }

    public ArrayList<GameRoom> getMyRooms() {
        return myRooms;
    }

    /** Luồng lắng nghe kết nối từ client. */
    public class ServerListening extends Thread {
        /** Vòng lặp accept(): mỗi socket mới -> new ServerProcessing(socket), thêm vào myProcess, start(). */
        @Override
        public void run() {
            // TODO
        }
    }

    /** Luồng xử lý 1 client. Chủ: N04. */
    public class ServerProcessing extends Thread {
        private Socket mySocket;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        private Player loggedInPlayer;
        private volatile String status = PlayerStat.IDLE;
        private volatile GameRoom room;

        public ServerProcessing(Socket s) {
            this.mySocket = s;
        }

        /**
         * Tạo oos TRƯỚC ois (tránh nghẽn bắt tay stream), rồi vòng lặp readObject() -> handleMessage().
         * Bắt RuntimeException của từng gói để luồng không chết; mất kết nối -> onDisconnect() (trong finally).
         */
        @Override
        public void run() {
            // TODO
        }

        /** switch theo performative, mỗi case gọi một hàm handleXxx bên dưới. */
        private void handleMessage(ObjectWrapper w) {
            switch (w.getPerformative()) {
                // TODO: LOGIN_USER, REGISTER_USER, EDIT_PLAYER, CHANGE_PASSWORD, GET_PLAYER_HISTORY,
                //       GET_ONLINE_LIST, GET_RANKING, SEND_CHALLENGE, REPLY_CHALLENGE_DECISION,
                //       QUIT_GAME, REQUEST_CONTINUE_MATCH, SEND_MOVE
                default:
                    break;
            }
        }

        /**
         * Hình 4.4, bước 8-17: playerDAO.checkLogin(p).
         * Sai -> REPLY_LOGIN_USER "false"; đúng -> login(this, p), REPLY_LOGIN_USER kèm p.
         */
        private void handleLogin(Player p) {
            // TODO
        }

        /** Hình 4.3, bước 11-16: playerDAO.registerPlayer(p) -> REPLY_REGISTER_USER "ok" | "false". */
        private void handleRegister(Player p) {
            // TODO
        }

        /**
         * Hình 4.5, bước 13-17: p.setId(loggedInPlayer.getId()); playerDAO.editPlayer(p);
         * thành công -> cập nhật loggedInPlayer; REPLY_EDIT_PLAYER "ok" | "false".
         */
        private void handleEditPlayer(Player p) {
            // TODO
        }

        /** Hình 4.6, bước 8-12: data = String[]{oldPass, newPass}; playerDAO.changePassword(...) -> REPLY_CHANGE_PASSWORD. */
        private void handleChangePassword(String[] passwords) {
            // TODO
        }

        /** Hình 4.7, bước 6-13: matchDAO.getMatchesByPlayer(loggedInPlayer.getId()) -> REPLY_PLAYER_HISTORY. */
        private void handleGetHistory() {
            // TODO
        }

        /** Hình 4.11, bước 4-10: getOnlinePlayers() -> REPLY_ONLINE_LIST. */
        private void handleGetOnlineList() {
            // TODO
        }

        /** Hình 4.8, bước 6-12: playerDAO.getRanking() -> REPLY_RANKING. */
        private void handleGetRanking() {
            // TODO
        }

        /**
         * Hình 4.11, bước 19-24: findProcess(target.getId()), tryCreateRoom(this, b).
         * null -> REPLY_CHALLENGE_BUSY cho A; ngược lại SERVER_INFORM_CHALLENGE (PlayerStat của A) cho B.
         */
        private void handleSendChallenge(PlayerStat target) {
            // TODO
        }

        /** Hình 4.12, bước 4: room.receiveChallengeDecision(this, accept). */
        private void handleChallengeDecision(boolean accept) {
            // TODO
        }

        /** Hình 4.13, bước 4: room.endGame(đối thủ, GameResult.RESIGN). */
        private void handleQuitGame() {
            // TODO
        }

        /** Hình 4.14, bước 4 / 8: room.receiveContinueDecision(this, accept). */
        private void handleContinueDecision(boolean accept) {
            // TODO
        }

        /** Chuyển SEND_MOVE (MoveRequest) cho room.handleMove(this, req) - trọng tài của N11. */
        private void handleSendMove(Object moveRequest) {
            // TODO
        }

        /** Mất kết nối = đăng xuất: room.playerLeft(this), removeProcess, đóng socket, broadcastOnlineList nếu đã đăng nhập. */
        private void onDisconnect() {
            // TODO
        }

        /** Gửi gói tin về client này: writeObject + flush + reset (GameRoom và ChessReferee cũng gọi). */
        public synchronized void sendData(Object obj) {
            // TODO
        }

        public Player getLoggedInPlayer() { return loggedInPlayer; }
        public void setLoggedInPlayer(Player loggedInPlayer) { this.loggedInPlayer = loggedInPlayer; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public GameRoom getRoom() { return room; }
        public void setRoom(GameRoom room) { this.room = room; }
        public Socket getMySocket() { return mySocket; }
    }
}
