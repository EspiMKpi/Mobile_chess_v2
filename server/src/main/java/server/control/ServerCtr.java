package server.control;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import model.GameResult;
import model.IPAddress;
import model.MoveRequest;
import model.ObjectWrapper;
import model.Player;
import model.PlayerStat;
import server.dao.MatchDAO;
import server.dao.PlayerDAO;
import server.view.ServerMainFrm;

/**
 * Chủ: N04. Bộ điều khiển phía Server (ServerSocket, ServerListening, ServerProcessing).
 * Phase 0 chỉ chốt các chữ ký mà phần của N11 (ChessReferee) cần gọi.
 */
public class ServerCtr {

    private ServerMainFrm view;
    private IPAddress myAddress;
    private ServerSocket myServerSocket;
    private ServerListening myListening;

    private final ArrayList<ServerProcessing> myProcess = new ArrayList<>();
    private final ArrayList<GameRoom> myRooms = new ArrayList<>();

    /** Khóa riêng cho ghép cặp: chỉ bao quanh kiểm tra trạng thái + createRoom. KHÔNG lấy khóa GameRoom bên trong. */
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

    public boolean openServer() {
        try {
            myServerSocket = new ServerSocket(myAddress.getPort());
            myListening = new ServerListening();
            myListening.start();
            return true;
        } catch (IOException e) {
            if (view != null) {
                view.showMessage("Lỗi mở ServerSocket tại cổng " + myAddress.getPort() + ": " + e.getMessage());
            }
            return false;
        }
    }

    public void stopServer() {
        try {
            if (myServerSocket != null && !myServerSocket.isClosed()) {
                myServerSocket.close();
            }
        } catch (IOException ignored) {
        }

        synchronized (myRooms) {
            for (GameRoom room : new ArrayList<>(myRooms)) {
                try {
                    room.closeRoom();
                } catch (Exception ignored) {
                }
            }
            myRooms.clear();
        }

        synchronized (myProcess) {
            for (ServerProcessing sp : new ArrayList<>(myProcess)) {
                try {
                    if (sp.mySocket != null && !sp.mySocket.isClosed()) {
                        sp.mySocket.close();
                    }
                } catch (IOException ignored) {
                }
            }
            myProcess.clear();
        }
    }

    public ServerProcessing findProcess(int playerId) {
        synchronized (myProcess) {
            for (ServerProcessing sp : myProcess) {
                if (sp.getLoggedInPlayer() != null && sp.getLoggedInPlayer().getId() == playerId) {
                    return sp;
                }
            }
            return null;
        }
    }

    public void removeProcess(ServerProcessing sp) {
        synchronized (myProcess) {
            myProcess.remove(sp);
        }
    }

    /** Kiểm tra cả hai rảnh rồi tạo phòng trong cùng một khóa. Trả null nếu không ghép được. */
    public GameRoom tryCreateRoom(ServerProcessing a, ServerProcessing b) {
        synchronized (matchmakingLock) {
            if (a == null || b == null || a == b) return null;
            if (a.getRoom() != null || b.getRoom() != null) return null;
            if (PlayerStat.BUSY.equals(a.getStatus()) || PlayerStat.BUSY.equals(b.getStatus())) return null;
            return createRoom(a, b);
        }
    }

    public GameRoom createRoom(ServerProcessing a, ServerProcessing b) {
        GameRoom room = new GameRoom(this, a, b);
        a.setRoom(room);
        a.setStatus(PlayerStat.BUSY);
        b.setRoom(room);
        b.setStatus(PlayerStat.BUSY);
        synchronized (myRooms) {
            myRooms.add(room);
        }
        return room;
    }

    public void removeRoom(GameRoom r) {
        synchronized (myRooms) {
            myRooms.remove(r);
        }
    }

    public ArrayList<PlayerStat> getOnlinePlayers() {
        ArrayList<PlayerStat> list = new ArrayList<>();
        synchronized (myProcess) {
            for (ServerProcessing sp : myProcess) {
                Player p = sp.getLoggedInPlayer();
                if (p != null) {
                    float totalPoint = playerDAO.getTotalPoint(p.getId());
                    list.add(new PlayerStat(p, totalPoint, sp.getStatus()));
                }
            }
        }
        return list;
    }

    public void broadcastOnlineList() {
        ArrayList<PlayerStat> list = getOnlinePlayers();
        ObjectWrapper wrapper = new ObjectWrapper(ObjectWrapper.SERVER_INFORM_ONLINE_LIST, list);
        ArrayList<ServerProcessing> recipients;
        synchronized (myProcess) {
            recipients = new ArrayList<>(myProcess);
        }
        for (ServerProcessing sp : recipients) {
            if (sp.getLoggedInPlayer() != null) {
                sp.sendData(wrapper);
            }
        }
    }

    public ArrayList<ServerProcessing> getMyProcess() {
        return myProcess;
    }

    public ArrayList<GameRoom> getMyRooms() {
        return myRooms;
    }

    /** Luồng lắng nghe kết nối từ client. */
    public class ServerListening extends Thread {
        @Override
        public void run() {
            try {
                while (myServerSocket != null && !myServerSocket.isClosed()) {
                    Socket socket = myServerSocket.accept();
                    socket.setKeepAlive(true);
                    ServerProcessing sp = new ServerProcessing(socket);
                    synchronized (myProcess) {
                        myProcess.add(sp);
                    }
                    sp.start();
                    if (view != null) {
                        view.showMessage("Client mới kết nối từ: " + socket.getRemoteSocketAddress());
                    }
                }
            } catch (IOException e) {
                if (view != null && myServerSocket != null && !myServerSocket.isClosed()) {
                    view.showMessage("Lỗi ServerListening: " + e.getMessage());
                }
            }
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

        @Override
        public void run() {
            try {
                // Tạo OOS trước OIS để tránh nghẽn bắt tay stream
                oos = new ObjectOutputStream(mySocket.getOutputStream());
                oos.flush();
                ois = new ObjectInputStream(mySocket.getInputStream());

                while (true) {
                    ObjectWrapper w = (ObjectWrapper) ois.readObject();
                    if (w == null) continue;
                    handleMessage(w);
                }
            } catch (IOException | ClassNotFoundException e) {
                // Client ngắt kết nối hoặc lỗi mạng
            } finally {
                onDisconnect();
            }
        }

        private void handleMessage(ObjectWrapper w) {
            switch (w.getPerformative()) {
                case ObjectWrapper.LOGIN_USER: {
                    Player p = (Player) w.getData();
                    boolean ok = playerDAO.checkLogin(p);
                    if (!ok) {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_LOGIN_USER, "false"));
                    } else if (findProcess(p.getId()) != null) {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_LOGIN_USER, "logged_in"));
                    } else {
                        loggedInPlayer = p;
                        status = PlayerStat.IDLE;
                        p.setPassword(null);
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_LOGIN_USER, p));
                        if (view != null) {
                            view.showMessage("Đăng nhập thành công: " + p.getUsername() + " (ID: " + p.getId() + ")");
                        }
                        broadcastOnlineList();
                    }
                    break;
                }

                case ObjectWrapper.REGISTER_USER: {
                    Player p = (Player) w.getData();
                    boolean ok = playerDAO.registerPlayer(p);
                    sendData(new ObjectWrapper(ObjectWrapper.REPLY_REGISTER_USER, ok ? "ok" : "false"));
                    if (ok && view != null) {
                        view.showMessage("Đăng ký tài khoản mới: " + p.getUsername());
                    }
                    break;
                }

                case ObjectWrapper.EDIT_PLAYER: {
                    if (loggedInPlayer != null) {
                        Player p = (Player) w.getData();
                        p.setId(loggedInPlayer.getId());
                        boolean ok = playerDAO.editPlayer(p);
                        if (ok) {
                            loggedInPlayer.setName(p.getName());
                            loggedInPlayer.setEmail(p.getEmail());
                            loggedInPlayer.setTel(p.getTel());
                        }
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_EDIT_PLAYER, ok ? "ok" : "false"));
                    } else {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_EDIT_PLAYER, "false"));
                    }
                    break;
                }

                case ObjectWrapper.CHANGE_PASSWORD: {
                    if (loggedInPlayer != null && w.getData() instanceof String[]) {
                        String[] arr = (String[]) w.getData();
                        boolean ok = playerDAO.changePassword(loggedInPlayer.getId(), arr[0], arr[1]);
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHANGE_PASSWORD, ok ? "ok" : "false"));
                    } else {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHANGE_PASSWORD, "false"));
                    }
                    break;
                }

                case ObjectWrapper.GET_PLAYER_HISTORY: {
                    if (loggedInPlayer != null) {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_PLAYER_HISTORY,
                                matchDAO.getMatchesByPlayer(loggedInPlayer.getId())));
                    }
                    break;
                }

                case ObjectWrapper.GET_ONLINE_LIST: {
                    sendData(new ObjectWrapper(ObjectWrapper.REPLY_ONLINE_LIST, getOnlinePlayers()));
                    break;
                }

                case ObjectWrapper.GET_RANKING: {
                    sendData(new ObjectWrapper(ObjectWrapper.REPLY_RANKING, playerDAO.getRanking()));
                    break;
                }

                case ObjectWrapper.SEND_CHALLENGE: {
                    if (loggedInPlayer == null || !(w.getData() instanceof PlayerStat)) {
                        break;
                    }
                    PlayerStat target = (PlayerStat) w.getData();
                    ServerProcessing b = findProcess(target.getId());
                    GameRoom newRoom = tryCreateRoom(this, b);
                    if (newRoom == null) {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_BUSY, target));
                    } else {
                        float pt = playerDAO.getTotalPoint(loggedInPlayer.getId());
                        PlayerStat inviterStat = new PlayerStat(loggedInPlayer, pt, PlayerStat.BUSY);
                        b.sendData(new ObjectWrapper(ObjectWrapper.SERVER_INFORM_CHALLENGE, inviterStat));
                        broadcastOnlineList();
                    }
                    break;
                }

                case ObjectWrapper.REPLY_CHALLENGE_DECISION: {
                    if (room != null && w.getData() instanceof Boolean) {
                        room.receiveChallengeDecision(this, (Boolean) w.getData());
                    }
                    break;
                }

                case ObjectWrapper.QUIT_GAME: {
                    if (room != null) {
                        ServerProcessing opponent = (room.getWhitePlayer() == this)
                                ? room.getBlackPlayer() : room.getWhitePlayer();
                        room.endGame(opponent, GameResult.RESIGN);
                    }
                    break;
                }

                case ObjectWrapper.REQUEST_CONTINUE_MATCH: {
                    if (room != null && w.getData() instanceof Boolean) {
                        room.receiveContinueDecision(this, (Boolean) w.getData());
                    }
                    break;
                }

                case ObjectWrapper.SEND_MOVE: {
                    // Chuyển cho trọng tài của N11
                    if (room != null && w.getData() instanceof MoveRequest) {
                        room.handleMove(this, (MoveRequest) w.getData());
                    }
                    break;
                }

                default:
                    break;
            }
        }

        private void onDisconnect() {
            if (room != null) {
                room.playerLeft(this);
            }
            removeProcess(this);
            try {
                if (mySocket != null && !mySocket.isClosed()) {
                    mySocket.close();
                }
            } catch (IOException ignored) {
            }
            if (view != null) {
                String name = (loggedInPlayer != null) ? loggedInPlayer.getUsername() : String.valueOf(mySocket.getRemoteSocketAddress());
                view.showMessage("Client đã ngắt kết nối: " + name);
            }
            if (loggedInPlayer != null) {
                broadcastOnlineList();
            }
        }

        /** Gửi gói tin về client này (được ChessReferee của N11 gọi). */
        public synchronized void sendData(Object obj) {
            try {
                if (oos != null) {
                    oos.writeObject(obj);
                    oos.flush();
                    oos.reset();
                }
            } catch (IOException e) {
                // Bỏ qua lỗi gửi khi socket bị đóng
            }
        }

        public Player getLoggedInPlayer() { return loggedInPlayer; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public GameRoom getRoom() { return room; }
        public void setRoom(GameRoom room) { this.room = room; }
        public Socket getMySocket() { return mySocket; }
    }
}
