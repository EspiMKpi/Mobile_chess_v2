# HỢP ĐỒNG CHUNG N04 ↔ N11 (Phase 0) – SERVER JAVA (PC) + CLIENT ANDROID

> Hệ thống: **1 server Java chạy trên máy tính (kèm MySQL)** và **nhiều client là ứng dụng Android (Java)** – client chỉ chạy trên điện thoại/máy ảo Android.
> Cả hai bắt đầu code lại từ đầu. Các lớp/tệp dưới đây là **điểm nối giữa hai người** – tạo đúng module, package, tên lớp, chữ ký public như sau rồi mới làm phần riêng.
> Ai là chủ file nào: `.github/CODEOWNERS`. Đổi bất cứ thứ gì ở đây → báo người kia + PR riêng.
> Chi tiết phần N04: `docs/BLUEPRINT_N04.md` · phần N11: `docs/HUONG_DAN_N11.md`.

## 1. Cấu trúc project (một project Android Studio, 3 module Gradle)

```
Mobile_chess/
  settings.gradle.kts, build.gradle.kts, gradle/      (Android Studio sinh ra – cả 2 cùng review)
  common/   thư viện Java thuần, dùng chung :server + :app
    src/main/java/model/**                  lớp thực thể + gói tin (Serializable)
    src/main/java/com/chess/engine/**       luật cờ + AI (N11)
  server/   ứng dụng Java chạy trên PC (Swing ServerMainFrm, ServerSocket, JDBC MySQL)
    src/main/java/server/{ServerRun, view, control, dao}
  app/      ứng dụng Android (Java)
    src/main/AndroidManifest.xml
    src/main/java/client/control/**         ClientCtr (+ ClientListening)
    src/main/java/client/view/**            Activity + DialogFragment
    src/main/java/client/view/ai/**         chơi với máy (N11)
    src/main/res/**                         layout, drawable, values
db/01_core.sql (N04), db/02_match_move.sql (N11)
```

Project đã được dựng sẵn ở repo code `Mobile_chess_v2` (bước M0) – mở thư mục gốc bằng Android Studio là dùng được; cấu hình build xem mục 4.

## 2. Quy tắc bắt buộc
1. **Mã lệnh**: N04 dùng 1–29, N11 dùng 30–39. `SERVER_END_GAME` (21) dùng chung, data = `GameResult`.
2. **Dữ liệu**: ô cờ `0 = a8 … 63 = h1`; màu `"WHITE"`/`"BLACK"`; phong cấp `"Q"|"R"|"B"|"N"|null`.
3. **`:common` chỉ dùng Java thuần** (`java.base`): không Swing/AWT, không JDBC, không `android.*` – vì chạy trên cả PC lẫn Android. Mọi lớp gửi qua Socket nằm trong `:common`, `implements Serializable`, `serialVersionUID = 1L`. Cả server và app phụ thuộc cùng module nên tuần tự hóa khớp nhau.
4. **Khóa phía server**: mọi trạng thái phòng đấu + trọng tài đồng bộ trên **một** khóa là đối tượng `GameRoom` (method `synchronized` ở GameRoom, `synchronized (room)` ở ChessReferee). `GameRoom.endGame` idempotent; ChessReferee kiểm tra `room.isPlaying()` trước khi xử lý.
5. **Luồng phía Android**: `ClientCtr` là singleton (`ClientCtr.getInstance()`); mọi I/O Socket chạy trên luồng nền; gói tin nhận được chuyển sang **main thread** (Handler + `Looper.getMainLooper()`) rồi mới gọi `receivedDataProcessing`. Không bao giờ đọc/ghi Socket trên main thread (Android ném `NetworkOnMainThreadException`).
6. **Vòng đời Activity**: màn hình đăng ký `myFunction` trong `onCreate` và gỡ trong `onDestroy`. `ChessBoardView` **không** tự đăng ký – `MatchPlayActivity` nhận mã 31/32 và chuyển tiếp. Mọi Activity khóa màn hình dọc (`screenOrientation="portrait"`) để không bị tạo lại khi xoay.
7. **Manifest & tài nguyên**: `AndroidManifest.xml` (N04) khai báo sẵn mọi Activity của cả 2 ngay ở M0 (mục 4). Tài nguyên của N11 đặt tiền tố `n11_` (layout, drawable) và chuỗi trong `res/values/strings_n11.xml`; N04 dùng các file còn lại – không ai sửa file tài nguyên của người kia.
8. **Kết nối mạng**: server lắng nghe cổng `9999` trên PC. Máy ảo Android trỏ tới PC bằng `10.0.2.2`; điện thoại thật dùng IP LAN của PC (cùng Wi-Fi, mở cổng 9999 trên Windows Firewall).
9. `MatchMoveDAO.insertMoves` dùng chung Connection của `MatchDAO.saveMatch`, không commit/rollback.

## 3. Mã nguồn hợp đồng

Phần `// TODO(...)` là việc của chủ file; mọi chữ ký public phải giữ nguyên.

### 3.1 Module `:common` và `:server` (không đổi so với bản trước, chỉ đổi đường dẫn)

#### `common/src/main/java/model/ObjectWrapper.java`

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

#### `common/src/main/java/model/Player.java`

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

#### `common/src/main/java/model/PlayerStat.java`

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

#### `common/src/main/java/model/PlayerRanking.java`

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

#### `common/src/main/java/model/Match.java`

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

#### `common/src/main/java/model/MatchResult.java`

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

#### `common/src/main/java/model/MatchStartInfo.java`

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

#### `common/src/main/java/model/IPAddress.java`

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

#### `common/src/main/java/model/MatchMove.java`

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

#### `common/src/main/java/model/MoveRequest.java`

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

#### `common/src/main/java/model/MoveInfo.java`

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

#### `common/src/main/java/model/GameResult.java`

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

### 3.2 Phía server

#### `server/src/main/java/server/control/ServerCtr.java`

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

        /** Gửi gói tin về client này (được ChessReferee của N11 gọi, kể cả từ luồng Timer). */
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

#### `server/src/main/java/server/control/GameRoom.java`

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

#### `server/src/main/java/server/control/ChessReferee.java`

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

#### `server/src/main/java/server/control/MoveTimeoutTask.java`

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

#### `server/src/main/java/server/dao/DAO.java`

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

#### `server/src/main/java/server/dao/MatchDAO.java`

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

#### `server/src/main/java/server/dao/MatchMoveDAO.java`

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

### 3.3 Phía Android (`:app`)

#### `app/src/main/java/client/control/ClientCtr.java`

```java
package client.control;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.ObjectWrapper;

/**
 * Chủ: N04. Bộ điều khiển phía Client Android - SINGLETON, sống suốt vòng đời ứng dụng
 * (Activity bị tạo/hủy liên tục nhưng kết nối Socket phải giữ nguyên).
 *  - Mọi I/O Socket chạy trên luồng nền (Android cấm mạng trên main thread).
 *  - ClientListening (inner class) đọc gói tin rồi chuyển sang main thread qua mainHandler
 *    trước khi gọi receivedDataProcessing() của màn hình đã đăng ký.
 * Phase 0 chỉ chốt các hàm mà phần của N11 (ChessBoardView) gọi.
 */
public class ClientCtr {

    private static ClientCtr instance;

    private final CopyOnWriteArrayList<ObjectWrapper> myFunction = new CopyOnWriteArrayList<>();
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ClientCtr() {
    }

    public static synchronized ClientCtr getInstance() {
        if (instance == null) {
            instance = new ClientCtr();
        }
        return instance;
    }

    // TODO(N04): setServerAddress(IPAddress), openConnection() (chạy trên luồng nền),
    //            closeConnection(), ClientListening ... - xem docs/BLUEPRINT_N04.md mục 4.1

    /**
     * Gửi gói tin lên Server. Gọi được từ main thread: việc ghi Socket được đẩy sang sendExecutor
     * (1 luồng duy nhất -> giữ đúng thứ tự gói tin). Trả về false nếu chưa kết nối.
     */
    public boolean sendData(Object obj) {
        // TODO(N04): if (oos == null) return false;
        //            sendExecutor.execute(() -> { oos.writeObject(obj); oos.flush(); oos.reset(); });
        return false;
    }

    public List<ObjectWrapper> getActiveFunction() {
        return myFunction;
    }
}
```

#### `app/src/main/java/client/view/DataReceiver.java`

```java
package client.view;

import model.ObjectWrapper;

/** Chủ: N04. Mọi Activity/Dialog nhận gói tin từ Server cài đặt interface này (luôn được gọi trên main thread). */
public interface DataReceiver {
    void receivedDataProcessing(ObjectWrapper data);
}
```

#### `app/src/main/java/client/view/ChessBoardView.java`

```java
package client.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import model.ObjectWrapper;

/**
 * Chủ: N11. View bàn cờ online, được đặt trong layout của MatchPlayActivity (N04):
 *     <client.view.ChessBoardView
 *         android:id="@+id/boardView"
 *         android:layout_width="match_parent"
 *         android:layout_height="0dp"
 *         android:layout_weight="1" />
 * Gồm: bàn cờ 8x8 vẽ từ FEN (chạm chọn quân + ô đích), PromotionDialog, gửi SEND_MOVE,
 * biên bản nước đi, đồng hồ 30 giây, nhãn "Chiếu!".
 * Gửi nước đi: ClientCtr.getInstance().sendData(new ObjectWrapper(ObjectWrapper.SEND_MOVE, new MoveRequest(from, to, promo)));
 * Mọi phương thức public đều được MatchPlayActivity gọi trên main thread.
 */
public class ChessBoardView extends LinearLayout {

    private String myColor;
    private boolean locked = true;

    public ChessBoardView(Context context) {
        this(context, null);
    }

    public ChessBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        // TODO(N11): inflate R.layout.n11_view_chess_board vào this
    }

    /** Gọi khi nhận SERVER_START_MATCH và SERVER_RESTART_MATCH_SWAP_COLOR. myColor = "WHITE" | "BLACK". */
    public void reset(String myColor) {
        this.myColor = myColor;
        // TODO(N11): bàn cờ chuẩn, xoay khi cầm Đen, xóa biên bản, đồng hồ 30s, mở khóa nếu cầm Trắng
    }

    /** MatchPlayActivity chuyển tiếp SERVER_INFORM_MOVE và REPLY_MOVE_ILLEGAL vào đây. */
    public void onServerMessage(ObjectWrapper data) {
        // TODO(N11)
    }

    /** true khi nhận SERVER_END_GAME: khóa bàn VÀ dừng đồng hồ đếm ngược. */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    protected void onDetachedFromWindow() {
        setLocked(true);            // Activity đóng -> dừng đồng hồ, tránh rò rỉ
        super.onDetachedFromWindow();
    }
}
```

#### `app/src/main/java/client/view/ai/AISetupDialog.java`

```java
package client.view.ai;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

/**
 * Chủ: N11. Hộp thoại thiết lập ván với máy (Giao diện 3.1 báo cáo N11).
 * OnlineListActivity (N04) chỉ gọi đúng một dòng: AISetupDialog.open(this);
 */
public class AISetupDialog extends DialogFragment {

    public static final String TAG = "AISetupDialog";

    public static void open(AppCompatActivity host) {
        new AISetupDialog().show(host.getSupportFragmentManager(), TAG);
    }

    // TODO(N11): onCreateDialog(): chọn Người/Máy cho Trắng/Đen, độ sâu 1-6 (mặc định 3)
    //            -> startActivity(new Intent(requireContext(), AIMatchActivity.class) + extras)
}
```

#### `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Chủ: N04. Khai báo SẴN toàn bộ Activity của cả 2 người ở M0 để N11 không phải sửa file này. -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="false"
        android:label="@string/app_name"
        android:theme="@style/Theme.MobileChess">

        <activity android:name="client.view.ClientMainActivity"
            android:exported="true" android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity android:name="client.view.LoginActivity" android:screenOrientation="portrait" />
        <activity android:name="client.view.RegisterActivity" android:screenOrientation="portrait" />
        <activity android:name="client.view.OnlineListActivity" android:screenOrientation="portrait" />
        <activity android:name="client.view.EditPlayerActivity" android:screenOrientation="portrait" />
        <activity android:name="client.view.ChangePasswordActivity" android:screenOrientation="portrait" />
        <activity android:name="client.view.PlayerHistoryActivity" android:screenOrientation="portrait" />
        <activity android:name="client.view.RankingActivity" android:screenOrientation="portrait" />
        <activity android:name="client.view.MatchPlayActivity" android:screenOrientation="portrait" />
        <!-- N11 -->
        <activity android:name="client.view.ai.AIMatchActivity" android:screenOrientation="portrait" />
    </application>
</manifest>
```

## 4. Build file (đã dựng sẵn trong repo code `Mobile_chess_v2`)

Project được dựng theo đúng cấu hình của Android Studio trên máy nhóm, build thử thành công `:common:build`, `:server:build`, `:app:assembleDebug`:

| File | Nội dung chính |
|---|---|
| `settings.gradle.kts` | `include(":common", ":server", ":app")`, kho `google()` + `mavenCentral()` |
| `gradle/libs.versions.toml` | AGP **9.4.1**, appcompat 1.6.1, material 1.10.0, activity 1.8.0, constraintlayout 2.1.4, recyclerview 1.1.0, mysql-connector-j 9.1.0, junit 4.13.2 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle **9.6.0** |
| `common/build.gradle.kts` | `java-library`, `options.release = 17`, UTF-8, `testImplementation(junit)` |
| `server/build.gradle.kts` | `application` (`mainClass = server.ServerRun`), `implementation(project(":common"))`, `runtimeOnly(mysql-connector-j)`; `run` chạy với thư mục làm việc `server/` |
| `app/build.gradle.kts` | `com.android.application`, `namespace = applicationId = vn.edu.ptit.mobilechess`, `compileSdk = release(37)`, `minSdk 24`, `targetSdk 37`, Java 17, `implementation(project(":common"))` + androidx |
| `local.properties` | `sdk.dir=C:/Users/.../Android/Sdk` (dùng `/`, **không** commit) |

Lệnh:
- Server: `gradlew :server:run` (hoặc Run `server.ServerRun` trong Android Studio). Cấu hình DB: chép `server/config/db.properties.example` thành `server/config/db.properties`.
- App: mở thư mục gốc bằng Android Studio, chọn cấu hình `app` → Run lên máy ảo / điện thoại.
- Lớp `R` của app nằm trong package `vn.edu.ptit.mobilechess` (namespace) – Activity ở package `client.view` phải `import vn.edu.ptit.mobilechess.R;`.
- Thêm thư viện mới: khai báo trong `gradle/libs.versions.toml` (file dùng chung – cả 2 cùng review).
