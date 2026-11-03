# HỢP ĐỒNG CHUNG N04 ↔ N11 (Phase 0)

> Cả hai bắt đầu code lại từ đầu. Các lớp dưới đây là **điểm nối giữa hai người** – tạo đúng package, tên lớp, chữ ký public như sau rồi mới làm phần riêng.
> Ai là chủ file nào: `.github/CODEOWNERS`. Đổi bất cứ thứ gì ở đây → báo người kia + PR riêng.
> Chi tiết phần N04: `docs/BLUEPRINT_N04.md` · phần N11: `docs/HUONG_DAN_N11.md`.

## 1. Cấu trúc package

```
src/
  model/                      dùng chung client + server (Serializable)
  server/ServerRun, server/view, server/control, server/dao
  client/ClientRun, client/control, client/view, client/view/ai
  com/chess/engine/**         thư viện luật cờ + AI (N11)
db/01_core.sql (N04), db/02_match_move.sql (N11)
```

## 2. Quy tắc bắt buộc
1. Mã lệnh: N04 dùng 1–29, N11 dùng 30–39. `SERVER_END_GAME` (21) dùng chung, data = `GameResult`.
2. Ô cờ `0 = a8 … 63 = h1`; màu `"WHITE"`/`"BLACK"`; phong cấp `"Q"|"R"|"B"|"N"|null`.
3. Khóa: mọi trạng thái phòng đấu + trọng tài đồng bộ trên **một** khóa là đối tượng `GameRoom` (method `synchronized` ở GameRoom, `synchronized (room)` ở ChessReferee).
4. `GameRoom.endGame` idempotent; ChessReferee kiểm tra `room.isPlaying()` trước khi xử lý.
5. Client: `ClientListening` chuyển gói tin lên EDT trước khi gọi form; `ChessBoardPanel` không tự đăng ký `myFunction`, MatchPlayFrm chuyển tiếp mã 31/32.
6. `MatchMoveDAO.insertMoves` dùng chung Connection của `MatchDAO.saveMatch`, không commit/rollback.

## 3. Mã nguồn hợp đồng

Phần `// TODO(...)` là việc của chủ file; mọi chữ ký public phải giữ nguyên.

### `src/model/ObjectWrapper.java`

```java
package model;

import java.io.Serializable;

/**
 * Gói tin trao đổi Client <-> Server. HỢP ĐỒNG CHUNG (Phase 0) - không tự ý đổi số.
 * Dải mã: N04 = 1..29, N11 = 30..39.
 */
public class ObjectWrapper implements Serializable {
    private static final long serialVersionUID = 1L;

    // Số 1..24 khớp BAO_CAO_THIET_KE_GAME_CO_VUA.md (sơ đồ lớp CN1, CN2, CN3 của N04).

    // ===== N04 - Chức năng 1: Tài khoản & thông tin người chơi =====
    public static final int LOGIN_USER = 1;
    public static final int REPLY_LOGIN_USER = 2;
    public static final int REGISTER_USER = 3;
    public static final int REPLY_REGISTER_USER = 4;
    public static final int EDIT_PLAYER = 5;
    public static final int REPLY_EDIT_PLAYER = 6;
    public static final int CHANGE_PASSWORD = 7;
    public static final int REPLY_CHANGE_PASSWORD = 8;
    public static final int GET_PLAYER_HISTORY = 9;
    public static final int REPLY_PLAYER_HISTORY = 10;
    // ===== N04 - Chức năng 2: Danh sách online =====
    public static final int GET_ONLINE_LIST = 11;
    public static final int REPLY_ONLINE_LIST = 12;
    public static final int SERVER_INFORM_ONLINE_LIST = 13;
    // ===== N04 - Chức năng 3: Thách đấu & ghép cặp (theo hình 4.5) =====
    public static final int SEND_CHALLENGE = 14;
    public static final int REPLY_CHALLENGE_BUSY = 15;
    public static final int SERVER_INFORM_CHALLENGE = 16;
    public static final int REPLY_CHALLENGE_DECISION = 17;        // data: Boolean (true = OK, false = Reject)
    public static final int REPLY_CHALLENGE_REJECT = 18;
    public static final int SERVER_START_MATCH = 19;              // data: MatchStartInfo {màu quân, đối thủ}
    public static final int QUIT_GAME = 20;                       // data: null
    public static final int SERVER_END_GAME = 21;                 // data: GameResult (dùng chung N04 + N11)
                                                                  //   (báo cáo N04 ghi String "WIN"/"LOSE" -> đã thống nhất đổi sang GameResult)
    public static final int REQUEST_CONTINUE_MATCH = 22;          // data: Boolean (true = Tiếp tục, false = Dừng)
    public static final int SERVER_RESTART_MATCH_SWAP_COLOR = 23; // data: String màu mới "WHITE" | "BLACK"
    public static final int SERVER_END_SESSION_MATCH = 24;
    // ===== N04 - Bảng xếp hạng (bổ sung) =====
    public static final int GET_RANKING = 25;
    public static final int REPLY_RANKING = 26;                   // data: ArrayList<PlayerRanking>

    // ===== N11 - Nước đi & đồng bộ bàn cờ =====
    public static final int SEND_MOVE = 30;                       // data: MoveRequest
    public static final int REPLY_MOVE_ILLEGAL = 31;              // data: String (lý do)
    public static final int SERVER_INFORM_MOVE = 32;              // data: MoveInfo

    private int performative;
    private Object data;

    public ObjectWrapper() {
    }

    public ObjectWrapper(int performative, Object data) {
        this.performative = performative;
        this.data = data;
    }

    public int getPerformative() { return performative; }
    public void setPerformative(int performative) { this.performative = performative; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
```

### `src/model/Player.java`

```java
package model;

import java.io.Serializable;

/** Bảng tblPlayer. Chủ: N04. */
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String username;
    private String password;
    private String name;
    private String email;
    private String tel;

    public Player() {
    }

    public Player(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }
}
```

### `src/model/PlayerStat.java`

```java
package model;

/** Dòng trong danh sách online (không ánh xạ bảng). Chủ: N04. */
public class PlayerStat extends Player {
    private static final long serialVersionUID = 1L;
    public static final String IDLE = "IDLE";
    public static final String BUSY = "BUSY";

    private float totalPoint;
    private String status;

    public PlayerStat() {
    }

    /** Theo hình 3.2: sao chép id, username, name, email, tel từ p (KHÔNG sao chép password). */
    public PlayerStat(Player p, float totalPoint, String status) {
        setId(p.getId());
        setUsername(p.getUsername());
        setName(p.getName());
        setEmail(p.getEmail());
        setTel(p.getTel());
        this.totalPoint = totalPoint;
        this.status = status;
    }

    public float getTotalPoint() { return totalPoint; }
    public void setTotalPoint(float totalPoint) { this.totalPoint = totalPoint; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

### `src/model/PlayerRanking.java`

```java
package model;

/**
 * Dòng bảng xếp hạng (không ánh xạ bảng). Chủ: N04.
 * Thứ tự sắp: totalPoint DESC, avgOpponentPoint DESC, avgMovesWin ASC, avgMovesLose DESC.
 */
public class PlayerRanking extends Player {
    private static final long serialVersionUID = 1L;
    private float totalPoint;
    private float avgOpponentPoint;
    private float avgMovesWin;
    private float avgMovesLose;

    public float getTotalPoint() { return totalPoint; }
    public void setTotalPoint(float totalPoint) { this.totalPoint = totalPoint; }
    public float getAvgOpponentPoint() { return avgOpponentPoint; }
    public void setAvgOpponentPoint(float avgOpponentPoint) { this.avgOpponentPoint = avgOpponentPoint; }
    public float getAvgMovesWin() { return avgMovesWin; }
    public void setAvgMovesWin(float avgMovesWin) { this.avgMovesWin = avgMovesWin; }
    public float getAvgMovesLose() { return avgMovesLose; }
    public void setAvgMovesLose(float avgMovesLose) { this.avgMovesLose = avgMovesLose; }
}
```

### `src/model/Match.java`

```java
package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/** Bảng tblMatch. Chủ: N04 (danh sách moves do N11 điền). */
public class Match implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private Date startTime;
    private Date endTime;
    private ArrayList<MatchResult> results = new ArrayList<>();
    private ArrayList<MatchMove> moves = new ArrayList<>();

    public Match() {
    }

    public Match(Date startTime) {
        this.startTime = startTime;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public ArrayList<MatchResult> getResults() { return results; }
    public void addResult(MatchResult r) { results.add(r); }
    public ArrayList<MatchMove> getMoves() { return moves; }
    public void addMove(MatchMove m) { moves.add(m); }
}
```

### `src/model/MatchResult.java`

```java
package model;

import java.io.Serializable;

/** Bảng tblMatchResult. Chủ: N04. */
public class MatchResult implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String WIN = "WIN";
    public static final String DRAW = "DRAW";
    public static final String LOSE = "LOSE";
    public static final String WHITE = "WHITE";
    public static final String BLACK = "BLACK";

    private int id;
    private Player player;
    private String result;
    private String color;

    public MatchResult() {
    }

    public MatchResult(Player player, String result, String color) {
        this.player = player;
        this.result = result;
        this.color = color;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
```

### `src/model/MatchStartInfo.java`

```java
package model;

import java.io.Serializable;

/** Payload SERVER_START_MATCH (ván đầu tiên). Chủ: N04. */
public class MatchStartInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String myColor;        // MatchResult.WHITE | MatchResult.BLACK
    private PlayerStat opponent;

    public MatchStartInfo(String myColor, PlayerStat opponent) {
        this.myColor = myColor;
        this.opponent = opponent;
    }

    public String getMyColor() { return myColor; }
    public PlayerStat getOpponent() { return opponent; }
}
```

### `src/model/IPAddress.java`

```java
package model;

import java.io.Serializable;

/** Chủ: N04. */
public class IPAddress implements Serializable {
    private static final long serialVersionUID = 1L;
    private String host;
    private int port;

    public IPAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
}
```

### `src/model/MatchMove.java`

```java
package model;

import java.io.Serializable;

/** Bảng tblMatchMove. Ô: 0 = a8 ... 63 = h1 (giống BoardUtils). Chủ: N11. */
public class MatchMove implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private int moveIndex;
    private int fromSquare;
    private int toSquare;
    private String notation;
    private String promotion;      // "Q" | "R" | "B" | "N" | null

    public MatchMove() {
    }

    public MatchMove(int moveIndex, int fromSquare, int toSquare, String notation, String promotion) {
        this.moveIndex = moveIndex;
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.notation = notation;
        this.promotion = promotion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMoveIndex() { return moveIndex; }
    public int getFromSquare() { return fromSquare; }
    public int getToSquare() { return toSquare; }
    public String getNotation() { return notation; }
    public String getPromotion() { return promotion; }
}
```

### `src/model/MoveRequest.java`

```java
package model;

import java.io.Serializable;

/** Payload SEND_MOVE. Chủ: N11. */
public class MoveRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private int fromSquare;
    private int toSquare;
    private String promotion;      // "Q" | "R" | "B" | "N" | null nếu không phong cấp

    public MoveRequest(int fromSquare, int toSquare, String promotion) {
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.promotion = promotion;
    }

    public int getFromSquare() { return fromSquare; }
    public int getToSquare() { return toSquare; }
    public String getPromotion() { return promotion; }
}
```

### `src/model/MoveInfo.java`

```java
package model;

import java.io.Serializable;

/** Payload SERVER_INFORM_MOVE. Chủ: N11. */
public class MoveInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String notation;
    private String fen;
    private String sideToMove;     // "WHITE" | "BLACK"
    private boolean inCheck;

    public MoveInfo(String notation, String fen, String sideToMove, boolean inCheck) {
        this.notation = notation;
        this.fen = fen;
        this.sideToMove = sideToMove;
        this.inCheck = inCheck;
    }

    public String getNotation() { return notation; }
    public String getFen() { return fen; }
    public String getSideToMove() { return sideToMove; }
    public boolean isInCheck() { return inCheck; }
}
```

### `src/model/GameResult.java`

```java
package model;

import java.io.Serializable;

/** Payload SERVER_END_GAME (gửi riêng cho từng người chơi). Chủ: N11, dùng chung với N04. */
public class GameResult implements Serializable {
    private static final long serialVersionUID = 1L;
    // Lý do kết thúc ván
    public static final String CHECKMATE = "CHECKMATE";   // N11
    public static final String STALEMATE = "STALEMATE";   // N11
    public static final String TIMEOUT = "TIMEOUT";       // N11
    public static final String RESIGN = "RESIGN";         // N04 (QUIT_GAME)
    public static final String DISCONNECT = "DISCONNECT"; // N04

    private String result;         // MatchResult.WIN | DRAW | LOSE
    private String reason;
    private String color;          // màu quân của người nhận
    private float point;           // 1 | 0.5 | 0

    public GameResult(String result, String reason, String color, float point) {
        this.result = result;
        this.reason = reason;
        this.color = color;
        this.point = point;
    }

    public String getResult() { return result; }
    public String getReason() { return reason; }
    public String getColor() { return color; }
    public float getPoint() { return point; }
}
```

### `src/server/control/ServerCtr.java`

```java
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
```

### `src/server/control/GameRoom.java`

```java
package server.control;

import model.Match;
import model.MoveRequest;

/**
 * Chủ: N04. Vòng đời một cặp đấu: chờ trả lời thách đấu, phân màu, bắt đầu ván, kết thúc ván,
 * tiếp tục/đổi màu, đóng phòng. Phần luật cờ / nước đi / đồng hồ 30s nằm ở {@link ChessReferee} (N11).
 *
 * QUY TẮC KHÓA (hợp đồng chung): mọi thay đổi trạng thái phòng + trọng tài đều đồng bộ trên
 * CHÍNH ĐỐI TƯỢNG GameRoom này (synchronized method ở đây, synchronized (room) bên ChessReferee).
 * Không dùng khóa thứ hai -> không thể deadlock giữa luồng client và luồng Timer.
 */
public class GameRoom {

    private final ServerCtr myServer;
    private ServerCtr.ServerProcessing whitePlayer;
    private ServerCtr.ServerProcessing blackPlayer;
    private Match currentMatch;
    private int continueCount;
    private boolean playing;               // true từ startGame/ván mới tới endGame
    private final ChessReferee referee;

    /** Tạo khi A thách đấu B đang rảnh. Ván 1: người thách đấu (A) cầm Đen, người được thách (B) cầm Trắng. */
    public GameRoom(ServerCtr myServer, ServerCtr.ServerProcessing challenger,
                    ServerCtr.ServerProcessing challenged) {
        this.myServer = myServer;
        this.blackPlayer = challenger;
        this.whitePlayer = challenged;
        this.referee = new ChessReferee(this);
    }

    /** B trả lời lời mời (REPLY_CHALLENGE_DECISION). true -> startGame(); false -> closeRoom() + REPLY_CHALLENGE_REJECT cho A. */
    public synchronized void receiveChallengeDecision(boolean accept) {
        // TODO(N04)
    }

    /** Ván 1: new Match, referee.reset(), playing = true, broadcastOnlineList, gửi SERVER_START_MATCH cho 2 bên. */
    public synchronized void startGame() {
        // TODO(N04)
    }

    /** Nhận SEND_MOVE từ ServerProcessing -> chuyển cho trọng tài (N11). */
    public void handleMove(ServerCtr.ServerProcessing sender, MoveRequest req) {
        referee.handleMove(sender, req);
    }

    /**
     * Kết thúc ván. Gọi bởi N04 (RESIGN, DISCONNECT) và N11 (CHECKMATE, STALEMATE, TIMEOUT).
     * Idempotent: nếu !playing thì return ngay (tránh hết giờ và xin thua cùng lúc bị lưu 2 lần).
     * @param winner người thắng, null nếu hòa
     * @param reason một hằng số trong GameResult
     */
    public synchronized void endGame(ServerCtr.ServerProcessing winner, String reason) {
        // TODO(N04): if (!playing) return; playing = false; referee.stopTimer(); ... (xem blueprint)
    }

    /** REQUEST_CONTINUE_MATCH. Đủ 2 "Tiếp tục" -> swapColor + ván mới; có 1 "Dừng" -> closeRoom + END_SESSION. */
    public synchronized void receiveContinueDecision(ServerCtr.ServerProcessing sp, boolean accept) {
        // TODO(N04)
    }

    public synchronized void swapColor() {
        ServerCtr.ServerProcessing tmp = whitePlayer;
        whitePlayer = blackPlayer;
        blackPlayer = tmp;
    }

    /** referee.stopTimer(); 2 bên về IDLE, setRoom(null); myServer.removeRoom(this). */
    public synchronized void closeRoom() {
        // TODO(N04)
    }

    /** ChessReferee kiểm tra trước khi xử lý nước đi / hết giờ. */
    public synchronized boolean isPlaying() { return playing; }

    public ServerCtr.ServerProcessing getWhitePlayer() { return whitePlayer; }
    public ServerCtr.ServerProcessing getBlackPlayer() { return blackPlayer; }
    public Match getCurrentMatch() { return currentMatch; }
}
```

### `src/server/control/ChessReferee.java`

```java
package server.control;

import java.util.ArrayList;
import java.util.Timer;

import com.chess.engine.board.Board;
import model.MatchMove;
import model.MoveRequest;

/**
 * Chủ: N11. Trọng tài một ván: giữ bàn cờ chuẩn, kiểm tra lượt + luật, phát nước đi,
 * kiểm tra chiếu bí / hết nước, đồng hồ 30 giây mỗi nước.
 * Khi ván kết thúc theo luật/hết giờ: gọi room.endGame(winner, GameResult.CHECKMATE | STALEMATE | TIMEOUT).
 *
 * QUY TẮC KHÓA: đồng bộ bằng synchronized (room), KHÔNG dùng synchronized method của lớp này
 * (xem docs/HUONG_DAN_N11.md mục "Luồng & khóa").
 */
public class ChessReferee {

    public static final long MOVE_TIME_LIMIT_MS = 30_000;

    private final GameRoom room;
    private Board board;
    private Timer moveTimer;
    private final ArrayList<MatchMove> moves = new ArrayList<>();

    public ChessReferee(GameRoom room) {
        this.room = room;
    }

    /** Gọi bởi GameRoom (đang giữ khóa room) mỗi khi bắt đầu ván mới: bàn chuẩn, xóa biên bản, bật đồng hồ cho Trắng. */
    public void reset() {
        synchronized (room) {
            // TODO(N11): board = Board.createStandardBoard(); moves.clear(); startMoveTimer();
        }
    }

    /**
     * Gọi bởi GameRoom.handleMove() trên luồng ServerProcessing của người gửi.
     * Bỏ qua nếu !room.isPlaying(). Sai lượt/sai luật -> REPLY_MOVE_ILLEGAL cho sender.
     * Hợp lệ -> cập nhật board, thêm MatchMove, đặt lại đồng hồ, SERVER_INFORM_MOVE cho cả 2, checkGameOver().
     * Phong cấp: dùng MoveFactory.createPromotionMove khi req.getPromotion() != null.
     */
    public void handleMove(ServerCtr.ServerProcessing sender, MoveRequest req) {
        synchronized (room) {
            // TODO(N11)
        }
    }

    /** Gọi bởi GameRoom.endGame()/closeRoom() (đang giữ khóa room). */
    public void stopTimer() {
        synchronized (room) {
            if (moveTimer != null) {
                moveTimer.cancel();
                moveTimer = null;
            }
        }
    }

    /** Biên bản nước đi của ván hiện tại (GameRoom.endGame chép vào Match trước khi lưu). */
    public ArrayList<MatchMove> getMoves() {
        synchronized (room) {
            return new ArrayList<>(moves);
        }
    }

    // TODO(N11): checkGameOver(), startMoveTimer(), onMoveTimeout() - cũng dùng synchronized (room)
}
```

### `src/server/control/MoveTimeoutTask.java`

```java
package server.control;

import java.util.TimerTask;

/** Chủ: N11. Báo hết 30 giây về trọng tài. */
public class MoveTimeoutTask extends TimerTask {

    private final ChessReferee referee;

    public MoveTimeoutTask(ChessReferee referee) {
        this.referee = referee;
    }

    @Override
    public void run() {
        // TODO(N11): referee.onMoveTimeout();
    }
}
```

### `src/server/dao/DAO.java`

```java
package server.dao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;

/** Chủ: N04. Lớp cha giữ kết nối CSDL + hàm băm mật khẩu dùng chung. */
public class DAO {

    protected static Connection con;

    public DAO() {
        // TODO(N04): nếu con == null thì DriverManager.getConnection(jdbc:mysql://..., user, pass)
    }

    /** SHA-256, trả về 64 ký tự hexa (khớp cột tblPlayer.password VARCHAR(64)). */
    protected static String hashPassword(String plain) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
```

### `src/server/dao/MatchDAO.java`

```java
package server.dao;

import model.Match;

/** Chủ: N04. */
public class MatchDAO extends DAO {

    /**
     * Lưu 1 ván trong MỘT transaction: tblMatch -> 2 tblMatchResult ->
     * new MatchMoveDAO().insertMoves(con, matchId, match.getMoves()) (phần của N11) -> commit.
     */
    public boolean saveMatch(Match match) {
        // TODO(N04)
        return false;
    }

    // TODO(N04): getMatchesByPlayer(Player) cho Lịch sử đấu
}
```

### `src/server/dao/MatchMoveDAO.java`

```java
package server.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import model.MatchMove;

/** Chủ: N11. Ghi biên bản nước đi vào tblMatchMove. */
public class MatchMoveDAO {

    /**
     * Dùng chung Connection (đang trong transaction của MatchDAO.saveMatch) - KHÔNG commit/rollback ở đây.
     */
    public void insertMoves(Connection con, int matchId, List<MatchMove> moves) throws SQLException {
        // TODO(N11): INSERT INTO tblMatchMove(matchId, moveIndex, fromSquare, toSquare, notation, promotion)
        //            dùng PreparedStatement + addBatch/executeBatch
    }
}
```

### `src/client/control/ClientCtr.java`

```java
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
```

### `src/client/view/ChessBoardPanel.java`

```java
package client.view;

import javax.swing.JPanel;

import client.control.ClientCtr;
import model.ObjectWrapper;

/**
 * Chủ: N11. Panel bàn cờ nhúng giữa MatchPlayFrm (N04): vẽ bàn từ FEN, chọn quân + ô đích,
 * PromotionDialog, gửi SEND_MOVE, biên bản, ĐỒNG HỒ 30s (nhãn đồng hồ nằm trong panel này), "Chiếu!".
 * Mọi phương thức public đều được MatchPlayFrm gọi trên luồng Swing (EDT).
 */
public class ChessBoardPanel extends JPanel {

    private final ClientCtr myControl;
    private String myColor;
    private boolean locked = true;

    public ChessBoardPanel(ClientCtr myControl) {
        this.myControl = myControl;
    }

    /** Gọi khi nhận SERVER_START_MATCH và SERVER_RESTART_MATCH_SWAP_COLOR. myColor = "WHITE" | "BLACK". */
    public void reset(String myColor) {
        this.myColor = myColor;
        // TODO(N11): bàn cờ chuẩn, xoay khi cầm Đen, xóa biên bản, đồng hồ 30s, mở khóa nếu cầm Trắng
    }

    /** MatchPlayFrm chuyển tiếp SERVER_INFORM_MOVE và REPLY_MOVE_ILLEGAL vào đây. */
    public void onServerMessage(ObjectWrapper data) {
        // TODO(N11)
    }

    /** true khi nhận SERVER_END_GAME: khóa bàn VÀ dừng đồng hồ đếm ngược. */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
```

### `src/client/view/ai/AISetupDialog.java`

```java
package client.view.ai;

import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 * Chủ: N11. Hộp thoại thiết lập ván với máy (hình 3.1 báo cáo N11).
 * OnlineListFrm (N04) chỉ gọi: new AISetupDialog(this).setVisible(true);
 */
public class AISetupDialog extends JDialog {

    public AISetupDialog(JFrame owner) {
        super(owner, "Thiết lập ván với máy", true);
        // TODO(N11): chọn Người/Máy cho Trắng/Đen, độ sâu 1-6 -> mở AIMatchFrm
    }
}
```
