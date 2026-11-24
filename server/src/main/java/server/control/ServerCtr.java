package server.control;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import model.IPAddress;
import model.ObjectWrapper;
import model.Player;
import model.PlayerStat;
import server.dao.PlayerDAO;
import server.view.ServerMainFrm;
public class ServerCtr {

    private ServerMainFrm view;
    private IPAddress myAddress;
    private ServerSocket myServer;
    private ServerListening myListening;
    private ArrayList<ServerProcessing> myProcess = new ArrayList<>();

    public static final int DISCOVERY_PORT = 9999;
    private ServerDiscovery myDiscovery;

    // [HƯỚNG DẪN THÁCH ĐẤU - CẤU TRÚC MỚI]
    // private final Object matchmakingLock = new Object();
    // private final ArrayList<MatchSession> mySessions = new ArrayList<>();

    public ServerCtr(ServerMainFrm v, int port) {
        this.view = v;
        this.myAddress = new IPAddress("0.0.0.0", port);
    }

    public void openServer() {
        try {
            myServer = new ServerSocket(myAddress.getPort());
            myListening = new ServerListening();
            myListening.start();
            myDiscovery = new ServerDiscovery();
            myDiscovery.start();
            view.showMessage("Server đang chạy ở cổng " + myAddress.getPort());
        } catch (IOException e) {
            view.showMessage("Không mở được server: " + e.getMessage());
        }
    }

    public void stopServer() {
        try {
            if (myServer != null) {
                myServer.close();
            }
        } catch (IOException ignored) {
        }
        // [HƯỚNG DẪN BƯỚC 2] Đóng UDP socket khi dừng server để luồng ServerDiscovery thoát gọn:
        if (myDiscovery != null) {
             myDiscovery.close();
             myDiscovery = null;
        }
        ArrayList<ServerProcessing> copy;
        synchronized (myProcess) {
            copy = new ArrayList<>(myProcess);
            myProcess.clear();
        }
        for (ServerProcessing sp : copy) {
            try {
                sp.mySocket.close();
            } catch (IOException ignored) {
            }
        }
        view.showMessage("Server đã dừng");
    }


    public ServerProcessing findProcess(int playerId) {
        synchronized (myProcess) {
            for (ServerProcessing sp : myProcess) {
                if (sp.loggedInPlayer != null && sp.loggedInPlayer.getId() == playerId) {
                    return sp;
                }
            }
        }
        return null;
    }

    public void removeProcess(ServerProcessing p) {
        synchronized (myProcess) {
            myProcess.remove(p);
        }
    }


    public synchronized void login(ServerProcessing p, Player player) {
        ServerProcessing old = findProcess(player.getId());
        if (old != null && old != p) {
            removeProcess(old);
            try {
                old.mySocket.close();
            } catch (IOException ignored) {
            }
        }
        p.setLoggedInPlayer(player);                 // bước 15
        p.setStatus(PlayerStat.IDLE);
        broadcastOnlineList();                       // bước 16
        view.showMessage(player.getUsername() + " đã đăng nhập");
    }


    public void broadcastOnlineList() {
        ArrayList<PlayerStat> list = getOnlinePlayers();
        ArrayList<ServerProcessing> copy;
        synchronized (myProcess) {
            copy = new ArrayList<>(myProcess);
        }
        for (ServerProcessing sp : copy) {
            if (sp.loggedInPlayer != null) {
                sp.sendData(new ObjectWrapper(ObjectWrapper.SERVER_INFORM_ONLINE_LIST, list));
            }
        }
    }


    public ArrayList<PlayerStat> getOnlinePlayers() {
        ArrayList<ServerProcessing> copy;
        synchronized (myProcess) {
            copy = new ArrayList<>(myProcess);
        }
        PlayerDAO playerDAO = new PlayerDAO();
        ArrayList<PlayerStat> list = new ArrayList<>();
        for (ServerProcessing sp : copy) {
            Player p = sp.loggedInPlayer;
            if (p != null) {
                list.add(new PlayerStat(p, playerDAO.getTotalPoint(p.getId()), sp.status));
            }
        }
        return list;
    }

    /** Tạo PlayerStat mới nhất cho 1 phiên đang đăng nhập (dùng cho cả N04 và GameRoom của N11). */
    public PlayerStat getPlayerStat(ServerProcessing sp) {
        if (sp == null || sp.loggedInPlayer == null) {
            return null;
        }
        PlayerDAO playerDAO = new PlayerDAO();
        return new PlayerStat(sp.loggedInPlayer, playerDAO.getTotalPoint(sp.loggedInPlayer.getId()), sp.status);
    }

    /**
     * N11 gọi hàm này khi kết thúc hẳn phòng đấu (chọn Dừng chơi hoặc mất kết nối giữa ván)
     * để trả 2 người chơi về trạng thái IDLE và cập nhật lại danh sách online.
     */
    public void onMatchSessionEnded(MatchSession session, ServerProcessing p1, ServerProcessing p2) {
        // TODO(N04): xóa session khỏi mySessions (nếu có), đặt matchSession = null và status = IDLE cho p1, p2,
        // rồi gọi broadcastOnlineList().
        if (p1 != null) {
            p1.setStatus(PlayerStat.IDLE);
        }
        if (p2 != null) {
            p2.setStatus(PlayerStat.IDLE);
        }
        broadcastOnlineList();
    }

    // [HƯỚNG DẪN THÁCH ĐẤU - Ghép cặp nguyên tử]
    // Thêm hàm tryCreateChallenge(ServerProcessing a, ServerProcessing b) dùng khóa matchmakingLock:
    //  - Kiểm tra a != null && b != null && a != b && a.loggedInPlayer != null && b.loggedInPlayer != null
    //  - Kiểm tra cả 2 đều chưa có pendingChallenge, chưa có matchSession và status == IDLE
    //  - Đổi cả 2 sang PlayerStat.BUSY, tạo PendingChallenge, gắn vào a và b, gọi startTimeout(),
    //    gọi broadcastOnlineList() để người khác thấy cả hai đang BẬN, rồi trả về PendingChallenge.


    public class ServerListening extends Thread {

        public ServerListening() {
            super("ServerListening");
        }

        @Override
        public void run() {
            while (!myServer.isClosed()) {
                try {
                    Socket s = myServer.accept();
                    ServerProcessing sp = new ServerProcessing(s);
                    synchronized (myProcess) {
                        myProcess.add(sp);
                    }
                    sp.start();
                    view.showMessage("Client kết nối: " + s.getRemoteSocketAddress());
                } catch (IOException e) {
                    break;                           // myServer đã đóng
                }
            }
        }
    }


    public class ServerDiscovery extends Thread {

         private DatagramSocket socket;

         public ServerDiscovery() {
             super("ServerDiscovery");
             try {
                 socket = new DatagramSocket(DISCOVERY_PORT);
                 socket.setBroadcast(true);
             } catch (IOException e) {
                 view.showMessage("Không mở được UDP discovery: " + e.getMessage());
             }
         }

         @Override
         public void run() {
             if (socket == null) {
                 return;
             }
             byte[] buf = new byte[256];
             while (!socket.isClosed()) {
                 try {
                     DatagramPacket packet = new DatagramPacket(buf, buf.length);
                     socket.receive(packet);
                     String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                     if ("DISCOVER_SERVER".equals(msg)) {
                         byte[] replyBytes = String.valueOf(myAddress.getPort()).getBytes(StandardCharsets.UTF_8);
                         DatagramPacket reply = new DatagramPacket(
                                 replyBytes, replyBytes.length, packet.getAddress(), packet.getPort());
                         socket.send(reply);
                     }
                 } catch (IOException e) {
                     if (socket.isClosed()) {
                         break;
                     }
                 }
             }
         }

         public void close() {
             if (socket != null && !socket.isClosed()) {
                 socket.close();
             }
         }
     }


    public class ServerProcessing extends Thread {

        private Socket mySocket;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        private Player loggedInPlayer;
        private volatile String status = PlayerStat.IDLE;
        // [HƯỚNG DẪN CẤU TRÚC MỚI] Thêm 2 trường quản lý lời mời đang chờ (N04) và trận đang đấu (N11):
        // private volatile PendingChallenge pendingChallenge;
        // private volatile MatchSession matchSession;

        public ServerProcessing(Socket s) {
            this.mySocket = s;
        }

        @Override
        public void run() {
            PlayerDAO playerDAO = new PlayerDAO();
            try {
                oos = new ObjectOutputStream(mySocket.getOutputStream());   // tạo oos TRƯỚC ois
                oos.flush();
                ois = new ObjectInputStream(mySocket.getInputStream());

                while (true) {
                    Object o = ois.readObject();
                    if (!(o instanceof ObjectWrapper)) {
                        continue;
                    }
                    ObjectWrapper data = (ObjectWrapper) o;
                    switch (data.getPerformative()) {
                        case ObjectWrapper.REGISTER_USER: {                    // hình 4.3, bước 10-16
                            Player player = (Player) data.getData();
                            boolean ok = playerDAO.registerPlayer(player);
                            sendData(new ObjectWrapper(ObjectWrapper.REPLY_REGISTER_USER, ok ? "ok" : "false"));
                            break;
                        }
                        case ObjectWrapper.LOGIN_USER: {                       // hình 4.4, bước 7-17
                            Player player = (Player) data.getData();
                            if (playerDAO.checkLogin(player)) {
                                login(this, player);
                                sendData(new ObjectWrapper(ObjectWrapper.REPLY_LOGIN_USER, player));
                            } else {
                                sendData(new ObjectWrapper(ObjectWrapper.REPLY_LOGIN_USER, "false"));
                            }
                            break;
                        }
                        // [HƯỚNG DẪN CÁC CHỨC NĂNG CÒN LẠI CỦA N04 + CHUYỂN TIẾP SANG N11]
                        // 1) EDIT_PLAYER:
                        //    Player edited = (Player) data.getData();
                        //    boolean ok = loggedInPlayer != null && playerDAO.editPlayer(loggedInPlayer.getId(), edited);
                        //    if (ok) { cập nhật name, email, tel của loggedInPlayer; }
                        //    sendData(new ObjectWrapper(ObjectWrapper.REPLY_EDIT_PLAYER, ok ? "ok" : "false"));
                        //
                        // 2) CHANGE_PASSWORD:
                        //    String[] arr = (String[]) data.getData(); // {oldPass, newPass}
                        //    boolean ok = loggedInPlayer != null && playerDAO.changePassword(loggedInPlayer.getId(), arr[0], arr[1]);
                        //    sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHANGE_PASSWORD, ok ? "ok" : "false"));
                        //
                        // 3) GET_PLAYER_HISTORY:
                        //    MatchDAO matchDAO = new MatchDAO();
                        //    sendData(new ObjectWrapper(ObjectWrapper.REPLY_PLAYER_HISTORY,
                        //            matchDAO.getMatchesByPlayer(loggedInPlayer.getId())));
                        //
                        // 4) GET_RANKING:
                        //    sendData(new ObjectWrapper(ObjectWrapper.REPLY_RANKING, playerDAO.getRanking()));
                        //
                        // 5) GET_ONLINE_LIST:
                        //    sendData(new ObjectWrapper(ObjectWrapper.REPLY_ONLINE_LIST, getOnlinePlayers()));
                        //
                        // 6) SEND_CHALLENGE:
                        //    PlayerStat target = (PlayerStat) data.getData();
                        //    ServerProcessing b = findProcess(target.getId());
                        //    PendingChallenge ch = tryCreateChallenge(this, b);
                        //    if (ch == null) { sendData(new ObjectWrapper(REPLY_CHALLENGE_BUSY, target)); }
                        //    else { b.sendData(new ObjectWrapper(SERVER_INFORM_CHALLENGE, getPlayerStat(this))); }
                        //
                        // 7) CANCEL_CHALLENGE:
                        //    if (pendingChallenge != null) pendingChallenge.onCancel(this);
                        //
                        // 8) REPLY_CHALLENGE_DECISION:
                        //    if (pendingChallenge != null) pendingChallenge.onDecision(this, Boolean.TRUE.equals(data.getData()));
                        //
                        // 9) CHUYỂN TIẾP GÓI TIN TRONG TRẬN SANG N11 (mã 20..24, 30..39):
                        //    if (matchSession != null) matchSession.onPacket(this, data);
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                // Client đóng kết nối hoặc mạng lỗi
            } finally {
                // [HƯỚNG DẪN MẤT KẾT NỐI]
                // Nếu đang chờ lời mời -> if (pendingChallenge != null) pendingChallenge.onDisconnect(this);
                // Nếu đang trong trận -> if (matchSession != null) matchSession.onDisconnect(this);
                removeProcess(this);
                try {
                    mySocket.close();
                } catch (IOException ignored) {
                }
                if (loggedInPlayer != null) {
                    view.showMessage(loggedInPlayer.getUsername() + " đã ngắt kết nối");
                    broadcastOnlineList();
                }
            }
        }

        public synchronized void sendData(Object o) {
            if (oos == null) {
                return;
            }
            try {
                oos.writeObject(o);
                oos.flush();
                oos.reset();
            } catch (IOException ignored) {

            }
        }

        public Player getLoggedInPlayer() { return loggedInPlayer; }
        public void setLoggedInPlayer(Player p) { this.loggedInPlayer = p; }
        public String getStatus() { return status; }
        public void setStatus(String s) { this.status = s; }
        // [HƯỚNG DẪN CẤU TRÚC MỚI] Khi mở pendingChallenge & matchSession thì thêm getter/setter tương ứng:
        // public PendingChallenge getPendingChallenge() { return pendingChallenge; }
        // public void setPendingChallenge(PendingChallenge c) { this.pendingChallenge = c; }
        // public MatchSession getMatchSession() { return matchSession; }
        // public void setMatchSession(MatchSession m) { this.matchSession = m; }
    }
}
