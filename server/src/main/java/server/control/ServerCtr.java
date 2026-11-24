package server.control;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import model.IPAddress;
import model.ObjectWrapper;
import model.Player;
import model.PlayerStat;
import server.dao.MatchDAO;
import server.dao.PlayerDAO;
import server.view.ServerMainFrm;

/** Chủ: N04. Bộ điều khiển phía Server (TCP, UDP Discovery, quản lý phiên, ghép cặp). */
public class ServerCtr {

    public static final int DISCOVERY_PORT = 9999;

    private final ServerMainFrm view;
    private final IPAddress myAddress;
    private final int discoveryPort;
    private ServerSocket myServer;
    private ServerListening myListening;
    private ServerDiscovery myDiscovery;
    private final ArrayList<ServerProcessing> myProcess = new ArrayList<>();
    private final ArrayList<MatchSession> mySessions = new ArrayList<>();
    private final Object matchmakingLock = new Object();

    public ServerCtr(ServerMainFrm v, int port) {
        this(v, port, DISCOVERY_PORT);
    }

    /** Constructor phụ (truyền discoveryPort = 0 khi chạy unit test để tránh xung đột cổng UDP). */
    public ServerCtr(ServerMainFrm v, int port, int discoveryPort) {
        this.view = v;
        this.myAddress = new IPAddress("0.0.0.0", port);
        this.discoveryPort = discoveryPort;
    }

    public void openServer() {
        try {
            myServer = new ServerSocket(myAddress.getPort());
            myAddress.setPort(myServer.getLocalPort());
            myListening = new ServerListening();
            myListening.start();
            myDiscovery = new ServerDiscovery(discoveryPort);
            myDiscovery.start();
            log("Server đang chạy ở cổng " + myAddress.getPort());
        } catch (IOException e) {
            log("Không mở được server: " + e.getMessage());
        }
    }

    public void stopServer() {
        try {
            if (myServer != null) {
                myServer.close();
            }
        } catch (IOException ignored) {
        }
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
        synchronized (mySessions) {
            mySessions.clear();
        }
        log("Server đã dừng");
    }

    public int getTcpPort() {
        return myAddress.getPort();
    }

    public int getDiscoveryPort() {
        return myDiscovery != null ? myDiscovery.getLocalPort() : -1;
    }

    public ArrayList<ServerProcessing> getMyProcess() {
        synchronized (myProcess) {
            return new ArrayList<>(myProcess);
        }
    }

    public ArrayList<MatchSession> getMySessions() {
        synchronized (mySessions) {
            return new ArrayList<>(mySessions);
        }
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

    /** Đăng nhập thành công; nếu tài khoản đang mở ở máy khác thì đóng phiên cũ. */
    public synchronized void login(ServerProcessing p, Player player) {
        ServerProcessing old = findProcess(player.getId());
        if (old != null && old != p) {
            removeProcess(old);
            try {
                old.mySocket.close();
            } catch (IOException ignored) {
            }
        }
        p.setLoggedInPlayer(player);
        p.setStatus(PlayerStat.IDLE);
        broadcastOnlineList();
        log(player.getUsername() + " đã đăng nhập");
    }

    /** Kiểm tra cả 2 người chơi đều rảnh và tạo lời mời thách đấu nguyên tử. */
    public PendingChallenge tryCreateChallenge(ServerProcessing a, ServerProcessing b) {
        synchronized (matchmakingLock) {
            if (a == null || b == null || a == b) {
                return null;
            }
            if (a.loggedInPlayer == null || b.loggedInPlayer == null) {
                return null;
            }
            if (a.getPendingChallenge() != null || b.getPendingChallenge() != null) {
                return null;
            }
            if (a.getMatchSession() != null || b.getMatchSession() != null) {
                return null;
            }
            if (PlayerStat.BUSY.equals(a.getStatus()) || PlayerStat.BUSY.equals(b.getStatus())) {
                return null;
            }

            a.setStatus(PlayerStat.BUSY);
            b.setStatus(PlayerStat.BUSY);
            PendingChallenge challenge = new PendingChallenge(this, a, b);
            a.setPendingChallenge(challenge);
            b.setPendingChallenge(challenge);
            challenge.startTimeout();
            broadcastOnlineList();
            return challenge;
        }
    }

    public void registerMatchSession(MatchSession session) {
        if (session == null) {
            return;
        }
        synchronized (mySessions) {
            mySessions.add(session);
        }
    }

    /** N11 gọi hàm này khi kết thúc hẳn phòng đấu để trả 2 người chơi về IDLE. */
    public void onMatchSessionEnded(MatchSession session, ServerProcessing p1, ServerProcessing p2) {
        if (session != null) {
            synchronized (mySessions) {
                mySessions.remove(session);
            }
        }
        if (p1 != null) {
            if (p1.getMatchSession() == session) {
                p1.setMatchSession(null);
            }
            p1.setStatus(PlayerStat.IDLE);
        }
        if (p2 != null) {
            if (p2.getMatchSession() == session) {
                p2.setMatchSession(null);
            }
            p2.setStatus(PlayerStat.IDLE);
        }
        broadcastOnlineList();
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

    public PlayerStat getPlayerStat(ServerProcessing sp) {
        if (sp == null || sp.loggedInPlayer == null) {
            return null;
        }
        PlayerDAO playerDAO = new PlayerDAO();
        return new PlayerStat(sp.loggedInPlayer, playerDAO.getTotalPoint(sp.loggedInPlayer.getId()), sp.status);
    }

    private void log(String msg) {
        if (view != null) {
            view.showMessage(msg);
        }
    }

    public class ServerListening extends Thread {
        public ServerListening() {
            super("ServerListening");
        }

        @Override
        public void run() {
            while (!myServer.isClosed()) {
                try {
                    Socket s = myServer.accept();
                    s.setKeepAlive(true);
                    ServerProcessing sp = new ServerProcessing(s);
                    synchronized (myProcess) {
                        myProcess.add(sp);
                    }
                    sp.start();
                    log("Client kết nối: " + s.getRemoteSocketAddress());
                } catch (IOException e) {
                    break;
                }
            }
        }
    }

    public class ServerDiscovery extends Thread {
        private DatagramSocket socket;

        public ServerDiscovery(int udpPort) {
            super("ServerDiscovery");
            try {
                socket = new DatagramSocket(udpPort);
                socket.setBroadcast(true);
            } catch (IOException e) {
                log("Không mở được UDP discovery: " + e.getMessage());
            }
        }

        public int getLocalPort() {
            return socket != null ? socket.getLocalPort() : -1;
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

        private final Socket mySocket;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        private volatile Player loggedInPlayer;
        private volatile String status = PlayerStat.IDLE;
        private volatile PendingChallenge pendingChallenge;
        private volatile MatchSession matchSession;

        public ServerProcessing(Socket s) {
            this.mySocket = s;
        }

        @Override
        public void run() {
            PlayerDAO playerDAO = new PlayerDAO();
            MatchDAO matchDAO = new MatchDAO();
            try {
                oos = new ObjectOutputStream(mySocket.getOutputStream());
                oos.flush();
                ois = new ObjectInputStream(mySocket.getInputStream());

                while (true) {
                    Object o = ois.readObject();
                    if (!(o instanceof ObjectWrapper)) {
                        continue;
                    }
                    try {
                        handleMessage((ObjectWrapper) o, playerDAO, matchDAO);
                    } catch (RuntimeException ex) {
                        log("Lỗi xử lý gói tin: " + ex.getMessage());
                    }
                }
            } catch (Exception e) {
                // Client đóng kết nối hoặc mất mạng
            } finally {
                if (pendingChallenge != null) {
                    pendingChallenge.onDisconnect(this);
                }
                if (matchSession != null) {
                    matchSession.onDisconnect(this);
                }
                removeProcess(this);
                try {
                    mySocket.close();
                } catch (IOException ignored) {
                }
                if (loggedInPlayer != null) {
                    log(loggedInPlayer.getUsername() + " đã ngắt kết nối");
                    broadcastOnlineList();
                }
            }
        }

        private void handleMessage(ObjectWrapper data, PlayerDAO playerDAO, MatchDAO matchDAO) {
            int code = data.getPerformative();
            switch (code) {
                case ObjectWrapper.REGISTER_USER: {
                    if (!(data.getData() instanceof Player)) {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_REGISTER_USER, "false"));
                        break;
                    }
                    boolean ok = playerDAO.registerPlayer((Player) data.getData());
                    sendData(new ObjectWrapper(ObjectWrapper.REPLY_REGISTER_USER, ok ? "ok" : "false"));
                    break;
                }
                case ObjectWrapper.LOGIN_USER: {
                    if (!(data.getData() instanceof Player)) {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_LOGIN_USER, "false"));
                        break;
                    }
                    Player p = (Player) data.getData();
                    if (playerDAO.checkLogin(p)) {
                        login(this, p);
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_LOGIN_USER, p));
                    } else {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_LOGIN_USER, "false"));
                    }
                    break;
                }
                case ObjectWrapper.EDIT_PLAYER: {
                    if (loggedInPlayer == null || !(data.getData() instanceof Player)) {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_EDIT_PLAYER, "false"));
                        break;
                    }
                    Player edited = (Player) data.getData();
                    boolean ok = playerDAO.editPlayer(loggedInPlayer.getId(), edited);
                    if (ok) {
                        loggedInPlayer.setName(edited.getName());
                        loggedInPlayer.setEmail(edited.getEmail());
                        loggedInPlayer.setTel(edited.getTel());
                        broadcastOnlineList();
                    }
                    sendData(new ObjectWrapper(ObjectWrapper.REPLY_EDIT_PLAYER, ok ? "ok" : "false"));
                    break;
                }
                case ObjectWrapper.CHANGE_PASSWORD: {
                    if (loggedInPlayer == null || !(data.getData() instanceof String[])) {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHANGE_PASSWORD, "false"));
                        break;
                    }
                    String[] arr = (String[]) data.getData();
                    boolean ok = arr.length == 2
                            && playerDAO.changePassword(loggedInPlayer.getId(), arr[0], arr[1]);
                    sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHANGE_PASSWORD, ok ? "ok" : "false"));
                    break;
                }
                case ObjectWrapper.GET_PLAYER_HISTORY: {
                    if (loggedInPlayer == null) {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_PLAYER_HISTORY, new ArrayList<>()));
                        break;
                    }
                    sendData(new ObjectWrapper(ObjectWrapper.REPLY_PLAYER_HISTORY,
                            matchDAO.getMatchesByPlayer(loggedInPlayer.getId())));
                    break;
                }
                case ObjectWrapper.GET_RANKING: {
                    sendData(new ObjectWrapper(ObjectWrapper.REPLY_RANKING, playerDAO.getRanking()));
                    break;
                }
                case ObjectWrapper.GET_ONLINE_LIST: {
                    sendData(new ObjectWrapper(ObjectWrapper.REPLY_ONLINE_LIST, getOnlinePlayers()));
                    break;
                }
                case ObjectWrapper.SEND_CHALLENGE: {
                    if (loggedInPlayer == null || !(data.getData() instanceof PlayerStat)) {
                        break;
                    }
                    PlayerStat target = (PlayerStat) data.getData();
                    ServerProcessing b = findProcess(target.getId());
                    PendingChallenge challenge = tryCreateChallenge(this, b);
                    if (challenge == null) {
                        sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_BUSY, target));
                    } else {
                        b.sendData(new ObjectWrapper(ObjectWrapper.SERVER_INFORM_CHALLENGE, getPlayerStat(this)));
                    }
                    break;
                }
                case ObjectWrapper.CANCEL_CHALLENGE: {
                    if (pendingChallenge != null) {
                        pendingChallenge.onCancel(this);
                    }
                    break;
                }
                case ObjectWrapper.REPLY_CHALLENGE_DECISION: {
                    if (pendingChallenge != null && data.getData() instanceof Boolean) {
                        pendingChallenge.onDecision(this, (Boolean) data.getData());
                    }
                    break;
                }
                default: {
                    // Chuyển tiếp mọi gói tin trong trận (20..24, 30..39) sang MatchSession của N11
                    if ((code >= 20 && code <= 24) || (code >= 30 && code <= 39)) {
                        MatchSession session = matchSession;
                        if (session != null) {
                            session.onPacket(this, data);
                        }
                    }
                    break;
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
        public PendingChallenge getPendingChallenge() { return pendingChallenge; }
        public void setPendingChallenge(PendingChallenge c) { this.pendingChallenge = c; }
        public MatchSession getMatchSession() { return matchSession; }
        public void setMatchSession(MatchSession m) { this.matchSession = m; }
    }
}
