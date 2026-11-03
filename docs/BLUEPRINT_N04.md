# BLUEPRINT CODE – PHẦN N04 (Nguyễn Đại Dũng)

> Hệ thống: **server Java chạy trên PC (kèm MySQL)** + **client là ứng dụng Android viết bằng Java** (client chỉ chạy trên điện thoại / máy ảo Android).
> Phạm vi N04: **CN1** Quản lý tài khoản & thông tin · **CN2** Danh sách online · **CN3** Thách đấu & ghép cặp · **BXH** (bổ sung theo đề) · toàn bộ **tầng mạng** (server + `ClientCtr` Android).
> Nguồn spec: `BAO_CAO_THIET_KE_GAME_CO_VUA.md` (hình 2.x, 3.x, 4.x – đã chuyển sang Activity/Android).
> Hợp đồng với N11 (mã nguồn các lớp nối, cấu trúc Gradle, manifest): `docs/HOP_DONG_CHUNG.md` – làm trước tiên (M0). Hướng dẫn cho N11: `docs/HUONG_DAN_N11.md`.
> Chỗ nào blueprint khác báo cáo đều ghi **[LỆCH BC]** kèm lý do.

---

## 0. Môi trường & quy ước

| Mục | Giá trị |
|---|---|
| IDE | Android Studio (kèm JDK – dùng luôn cho server) |
| Project | 1 project, 3 module: `:common` (Java thuần), `:server` (Java PC), `:app` (Android) – xem HOP_DONG_CHUNG mục 1, 4 |
| Android | Java, minSdk 24, `namespace = vn.edu.ptit.mobilechess`, Activity ở package `client.view` |
| Thiết bị thử | 2 máy ảo (AVD) hoặc 1 máy ảo + 1 điện thoại thật |
| CSDL | MySQL 8 trên PC, DB `chess`; chạy `db/01_core.sql` rồi `db/02_match_move.sql` |
| Cấu hình DB | `server/config/db.properties` (`url`, `user`, `password`) – **không** commit mật khẩu thật, commit `server/config/db.properties.example` |
| Cổng server | `9999`; mở cổng trên Windows Firewall khi dùng điện thoại thật |
| Địa chỉ server trên app | máy ảo: `10.0.2.2` · điện thoại thật: IP LAN của PC (cùng Wi-Fi) |
| Chạy | server: `gradlew :server:run` · app: Run cấu hình `app` |
| Luồng (Android) | **không** I/O mạng trên main thread; cập nhật giao diện **chỉ** trên main thread |
| Nhánh git | `feature/n04-account-lobby-challenge` |
| Không được sửa | file của N11 (xem `.github/CODEOWNERS`): `ChessReferee`, `MoveTimeoutTask`, `MatchMoveDAO`, `ChessBoardView`, `PromotionDialog`, `client/view/ai/**`, `com/chess/**`, tài nguyên `n11_*`, `strings_n11.xml` |

---

## 1. Danh sách file phải làm

```
common/src/main/java/model/*                     tạo đủ như HOP_DONG_CHUNG ở M0 (cả lớp model của N11) – sau đó chỉ thêm, không đổi chữ ký
server/src/main/java/server/ServerRun.java       main: new ServerMainFrm().setVisible(true)
server/src/main/java/server/view/ServerMainFrm.java   Swing trên PC (hình 2.6)
server/src/main/java/server/control/ServerCtr.java    + ServerListening, ServerProcessing (inner class)
server/src/main/java/server/control/GameRoom.java
server/src/main/java/server/dao/DAO.java, PlayerDAO.java, MatchDAO.java
server/config/db.properties.example
app/src/main/AndroidManifest.xml                 khai báo sẵn mọi Activity (cả AIMatchActivity của N11)
app/src/main/java/client/control/ClientCtr.java  singleton + ClientListening (inner class)
app/src/main/java/client/view/DataReceiver.java
app/src/main/java/client/view/BaseActivity.java  MỚI – lớp cha của mọi Activity N04 (mục 4.2)
app/src/main/java/client/view/ClientMainActivity.java, LoginActivity.java, RegisterActivity.java,
      OnlineListActivity.java, EditPlayerActivity.java, ChangePasswordActivity.java,
      PlayerHistoryActivity.java, RankingActivity.java, MatchPlayActivity.java
app/src/main/java/client/view/ChallengeDialog.java, InviteDialog.java, ContinueDialog.java   (DialogFragment)
app/src/main/java/client/view/adapter/OnlinePlayerAdapter.java, MatchHistoryAdapter.java, RankingAdapter.java
app/src/main/res/layout/activity_*.xml, dialog_*.xml, item_*.xml   (trừ file n11_*)
app/src/main/res/values/strings.xml, colors.xml, themes.xml
```

---

## 2. Bảng mã lệnh & dữ liệu kèm theo (`ObjectWrapper`)

`→S` = app gửi lên server · `→C` = server gửi xuống app.

| Mã | Tên | Chiều | `data` | Màn hình đăng ký nhận |
|---|---|---|---|---|
| 1 | LOGIN_USER | →S | `Player(username, password thô)` | |
| 2 | REPLY_LOGIN_USER | →C | `Player` đầy đủ (password = null) **hoặc** `"false"` / `"logged_in"` | LoginActivity |
| 3 | REGISTER_USER | →S | `Player` | |
| 4 | REPLY_REGISTER_USER | →C | `"ok"` / `"false"` | RegisterActivity |
| 5 | EDIT_PLAYER | →S | `Player` (name, email, tel mới) | |
| 6 | REPLY_EDIT_PLAYER | →C | `"ok"` / `"false"` | EditPlayerActivity |
| 7 | CHANGE_PASSWORD | →S | `String[]{oldPass, newPass}` | |
| 8 | REPLY_CHANGE_PASSWORD | →C | `"ok"` / `"false"` | ChangePasswordActivity |
| 9 | GET_PLAYER_HISTORY | →S | `null` | |
| 10 | REPLY_PLAYER_HISTORY | →C | `ArrayList<Match>` (mỗi Match 2 MatchResult) | PlayerHistoryActivity |
| 11 | GET_ONLINE_LIST | →S | `null` | |
| 12 | REPLY_ONLINE_LIST | →C | `ArrayList<PlayerStat>` | OnlineListActivity |
| 13 | SERVER_INFORM_ONLINE_LIST | →C | `ArrayList<PlayerStat>` | OnlineListActivity |
| 14 | SEND_CHALLENGE | →S | `PlayerStat` đối thủ B | |
| 15 | REPLY_CHALLENGE_BUSY | →C | `PlayerStat` B | ChallengeDialog |
| 16 | SERVER_INFORM_CHALLENGE | →C | `PlayerStat` A (có totalPoint) | OnlineListActivity |
| 17 | REPLY_CHALLENGE_DECISION | →S | `Boolean` | |
| 18 | REPLY_CHALLENGE_REJECT | →C | `PlayerStat` B | ChallengeDialog |
| 19 | SERVER_START_MATCH | →C | `MatchStartInfo{myColor, opponent}` | ChallengeDialog (A), InviteDialog (B) |
| 20 | QUIT_GAME | →S | `null` | |
| 21 | SERVER_END_GAME | →C | `GameResult` **[LỆCH BC: báo cáo ghi String]** | MatchPlayActivity |
| 22 | REQUEST_CONTINUE_MATCH | →S | `Boolean` | |
| 23 | SERVER_RESTART_MATCH_SWAP_COLOR | →C | `String` màu mới | MatchPlayActivity |
| 24 | SERVER_END_SESSION_MATCH | →C | `"STOP"` / `"OPPONENT_STOP"` | MatchPlayActivity |
| 25 | GET_RANKING | →S | `null` | |
| 26 | REPLY_RANKING | →C | `ArrayList<PlayerRanking>` đã sắp | RankingActivity |
| 30 | SEND_MOVE | →S | `MoveRequest` (N11 gửi) | |
| 31 | REPLY_MOVE_ILLEGAL | →C | `String` | **MatchPlayActivity** → chuyển cho `ChessBoardView` |
| 32 | SERVER_INFORM_MOVE | →C | `MoveInfo` | **MatchPlayActivity** → chuyển cho `ChessBoardView` |

`"logged_in"` trong REPLY_LOGIN_USER là giá trị bổ sung (báo cáo gộp chung vào `"false"`) để LoginActivity báo đúng lỗi "đang đăng nhập ở thiết bị khác".

---

## 3. Server (module `:server`, chạy trên PC)

### 3.1 `ServerMainFrm` (hình 2.6 – vẫn là Swing vì server chạy trên máy tính)
`txtServerHost` (chỉ đọc, hiện **tất cả IP LAN** của PC để nhập vào app trên điện thoại), `txtServerPort` (mặc định 9999), `btnStartServer`, `btnStopServer`, `mainText` (JTextArea log). Start → `myServer = new ServerCtr(this); myServer.openServer()`. `showMessage(s)` append log qua `SwingUtilities.invokeLater`.

### 3.2 `ServerCtr`
| Thành phần | Việc |
|---|---|
| `ServerCtr(ServerMainFrm v)` | lưu view, `myAddress = new IPAddress(host, port)` |
| `openServer()` | `new ServerSocket(port)` (bind mọi địa chỉ – không bind `localhost` kẻo điện thoại không vào được), `myListening = new ServerListening(); start()` |
| `stopServer()` | đóng mọi socket client, đóng ServerSocket |
| `ServerListening.run()` | vòng `accept()` → `s.setKeepAlive(true)`; `ServerProcessing sp = new ServerProcessing(s); synchronized(myProcess){add}; sp.start()` |
| `findProcess(int playerId)` | duyệt `myProcess`, trả sp có `loggedInPlayer.id == playerId` hoặc null |
| `removeProcess(sp)` | xóa khỏi `myProcess` |
| `createRoom(a, b)` | `new GameRoom(this, a, b)`; `a,b.setRoom(room)`, `setStatus(BUSY)`; add `myRooms`; trả room |
| `removeRoom(r)` | xóa khỏi `myRooms` |
| `getOnlinePlayers()` | với mỗi sp đã login: `new PlayerStat(sp.loggedInPlayer, playerDAO.getTotalPoint(id), sp.getStatus())` |
| `broadcastOnlineList()` | `list = getOnlinePlayers()`; gửi `SERVER_INFORM_ONLINE_LIST` cho mọi sp đã login |

Mọi thao tác trên `myProcess`/`myRooms` đặt trong `synchronized (myProcess)` / `synchronized (myRooms)`.

**Gọi `broadcastOnlineList()` đúng 4 thời điểm:** login thành công · sp kết thúc (logout/mất mạng/tắt app) · `GameRoom.startGame()` · `GameRoom.closeRoom()`.
Không gọi khi đang chờ trả lời thách đấu (theo báo cáo, A/B đã BUSY nhưng chỉ broadcast khi vào ván).

### 3.3 `ServerProcessing` (inner class)
Trường: `mySocket`, `ObjectInputStream ois`, `ObjectOutputStream oos`, `loggedInPlayer`, `status = IDLE`, `room`.
Tạo `oos` **trước** `ois` (tránh treo header stream) – phía app cũng làm đúng thứ tự này.

`run()`:
```
try {
  while (true) {
    ObjectWrapper w = (ObjectWrapper) ois.readObject();
    switch (w.getPerformative()) { ... bảng dưới ... }
  }
} catch (IOException | ClassNotFoundException e) { /* app đóng / mất mạng */ }
finally { onDisconnect(); }
```

| Mã nhận | Xử lý |
|---|---|
| LOGIN_USER | `playerDAO.checkLogin(p)`; sai → `"false"`; `findProcess(p.id) != null` → `"logged_in"`; đúng → `loggedInPlayer = p; status = IDLE`; gửi `REPLY_LOGIN_USER(p)` (xóa password trước khi gửi); `broadcastOnlineList()` |
| REGISTER_USER | `registerPlayer(p)` → `"ok"`/`"false"` |
| EDIT_PLAYER | `p.setId(loggedInPlayer.id)`; `editPlayer(p)`; ok → cập nhật name/email/tel của `loggedInPlayer` |
| CHANGE_PASSWORD | `changePassword(loggedInPlayer.id, arr[0], arr[1])` |
| GET_PLAYER_HISTORY | `matchDAO.getMatchesByPlayer(loggedInPlayer.id)` |
| GET_ONLINE_LIST | `REPLY_ONLINE_LIST(getOnlinePlayers())` |
| GET_RANKING | `REPLY_RANKING(playerDAO.getRanking())` |
| SEND_CHALLENGE | `b = findProcess(target.id)`; `b == null` hoặc `b.status == BUSY` hoặc `this.status == BUSY` → `REPLY_CHALLENGE_BUSY(target)`; ngược lại `createRoom(this, b)` rồi `b.sendData(SERVER_INFORM_CHALLENGE, new PlayerStat(loggedInPlayer, getTotalPoint, BUSY))` |
| REPLY_CHALLENGE_DECISION | `if (room != null) room.receiveChallengeDecision((Boolean) data)` |
| QUIT_GAME | `if (room != null) room.endGame(opponentOf(this), GameResult.RESIGN)` |
| REQUEST_CONTINUE_MATCH | `if (room != null) room.receiveContinueDecision(this, (Boolean) data)` |
| **SEND_MOVE** | `if (room != null) room.handleMove(this, (MoveRequest) data);` – **chỉ 1 dòng, phần còn lại của N11** |

`onDisconnect()`:
```
if (room != null) room.playerLeft(this);      // xem GameRoom.playerLeft
removeProcess(this); đóng socket; if (loggedInPlayer != null) broadcastOnlineList();
```
`sendData(Object)` là `synchronized` (luồng Timer của N11 cũng gọi): `oos.writeObject(o); oos.flush(); oos.reset();` – bắt `IOException` và bỏ qua.

### 3.4 `GameRoom` (giữ nguyên chữ ký public trong HOP_DONG_CHUNG)
Trạng thái: `whitePlayer`, `blackPlayer`, `currentMatch`, `continueCount`, `playing`, `referee`.

| Phương thức | Làm gì (theo thứ tự) |
|---|---|
| `receiveChallengeDecision(accept)` | false → `closeRoom()`; gửi `REPLY_CHALLENGE_REJECT(B)` cho **người thách đấu** (= blackPlayer ván 1). true → `startGame()` |
| `startGame()` | `beginNewMatch()`; `myServer.broadcastOnlineList()`; gửi `SERVER_START_MATCH(new MatchStartInfo("BLACK", statOf(white)))` cho black, `("WHITE", statOf(black))` cho white |
| `beginNewMatch()` (private) | `currentMatch = new Match(new Date()); continueCount = 0; playing = true; referee.reset();` |
| `endGame(winner, reason)` | `if (!playing) return; playing = false; referee.stopTimer();` → `currentMatch.setEndTime(now)`; tạo 2 `MatchResult` (WIN/LOSE hoặc DRAW/DRAW nếu winner null, kèm màu); `currentMatch.getMoves().addAll(referee.getMoves())`; `matchDAO.saveMatch(currentMatch)`; gửi mỗi bên `SERVER_END_GAME(new GameResult(result, reason, color, point))` |
| `receiveContinueDecision(sp, accept)` | false → người bấm nhận `SERVER_END_SESSION_MATCH("STOP")`, người kia `"OPPONENT_STOP"`; `closeRoom()`. true → `continueCount++`; `==2` → `swapColor(); beginNewMatch();` gửi `SERVER_RESTART_MATCH_SWAP_COLOR("WHITE")` cho whitePlayer mới, `("BLACK")` cho blackPlayer mới |
| `closeRoom()` | `playing = false; referee.stopTimer();` 2 bên `setStatus(IDLE)`, `setRoom(null)`; `myServer.removeRoom(this)`; `myServer.broadcastOnlineList()` |
| `playerLeft(sp)` (MỚI, public, chỉ N04 dùng) | đang chờ trả lời thách đấu (`currentMatch == null`): gửi `REPLY_CHALLENGE_REJECT` cho người còn lại (B rời → ChallengeDialog của A báo "Đối thủ đã từ chối"; A rời → InviteDialog của B báo "Người mời đã rời đi" và tự đóng). Đã vào ván: `if (playing) endGame(opponent, GameResult.DISCONNECT)`; gửi `SERVER_END_SESSION_MATCH("OPPONENT_STOP")` cho đối thủ. Cuối cùng `closeRoom()` |
| `isPlaying()` | trả `playing` – N11 dùng |

**Khóa:** tất cả phương thức trên `synchronized` (trên room). **Không** được lấy khóa `myProcess` rồi gọi vào room (tránh deadlock ngược) – `broadcastOnlineList()` chỉ lấy snapshot `myProcess` rồi gửi.

### 3.5 DAO
`DAO()` – nếu `con == null`: đọc `config/db.properties` (thư mục làm việc của `:server:run` là `server/`), `DriverManager.getConnection`. Dùng `PreparedStatement` cho mọi câu SQL.

**`PlayerDAO`**
| Hàm | SQL |
|---|---|
| `checkLogin(Player p)` | `SELECT id,name,email,tel FROM tblPlayer WHERE username=? AND password=?` (hash) → điền vào `p`, trả true |
| `registerPlayer(Player p)` | `SELECT id ... WHERE username=?` có → false; không → `INSERT` (hash) |
| `editPlayer(Player p)` | `UPDATE tblPlayer SET name=?,email=?,tel=? WHERE id=?` |
| `changePassword(id, old, new)` | `UPDATE tblPlayer SET password=? WHERE id=? AND password=?` → `executeUpdate()==1` |
| `getTotalPoint(playerId)` | `SELECT COALESCE(SUM(CASE result WHEN 'WIN' THEN 1.0 WHEN 'DRAW' THEN 0.5 ELSE 0 END),0) FROM tblMatchResult WHERE playerId=?` |
| `getRanking()` | xem 3.6 |

**`MatchDAO`**
| Hàm | Ghi chú |
|---|---|
| `getMatchesByPlayer(int playerId)` | `SELECT m.id,m.startTime,m.endTime,r.result,r.color,p.id,p.username,p.name FROM tblMatch m JOIN tblMatchResult r ON r.matchId=m.id JOIN tblPlayer p ON p.id=r.playerId WHERE m.id IN (SELECT matchId FROM tblMatchResult WHERE playerId=?) ORDER BY m.startTime DESC` → gom theo `m.id`, mỗi Match 2 MatchResult |
| `saveMatch(Match m)` | `con.setAutoCommit(false)` → INSERT tblMatch (lấy generated key) → INSERT 2 tblMatchResult → `new MatchMoveDAO().insertMoves(con, matchId, m.getMoves())` (**N11**) → `commit`; lỗi → `rollback`; `finally setAutoCommit(true)`. Hàm `synchronized` (dùng chung 1 Connection) |

### 3.6 Bảng xếp hạng **[LỆCH BC: bổ sung, báo cáo chưa có]**
`PlayerDAO.getRanking()` trả `ArrayList<PlayerRanking>` của **toàn bộ** người chơi trong tblPlayer:
- `totalPoint` = như `getTotalPoint`.
- `avgOpponentPoint` = trung bình `totalPoint` của các **đối thủ khác nhau** đã gặp (0 nếu chưa đấu).
- `avgMovesWin` = trung bình số nước của các ván **thắng**; số nước 1 ván = `COUNT(*)` tblMatchMove của ván đó (đếm nửa nước). Chưa thắng ván nào → `NULL`.
- `avgMovesLose` = tương tự cho ván **thua**. Chưa thua → `NULL`.

1 câu SQL lấy (playerId, matchId, result, opponentId, moveCount), tính trong Java rồi sắp bằng `Comparator`:
```
totalPoint DESC → avgOpponentPoint DESC → avgMovesWin ASC (NULL xếp cuối) → avgMovesLose DESC (NULL xếp cuối)
```
`PlayerRanking` dùng `float`, nên quy ước `NULL` = `-1` khi gửi về app (app hiển thị "-"). Nếu N11 chưa ghi nước đi thì moveCount = 0 – vẫn chạy được.

---

## 4. Client Android (module `:app`)

### 4.1 `ClientCtr` (singleton) + `ClientListening`
| Thành phần | Việc |
|---|---|
| `getInstance()` | có sẵn trong hợp đồng |
| `setServerAddress(IPAddress a)` | lưu host/port nhập ở ClientMainActivity |
| `openConnection()` | **gọi trên luồng nền** (ClientMainActivity dùng `sendExecutor`/`Executors`): `new Socket()`, `connect(new InetSocketAddress(host, port), 5000)`, `setKeepAlive(true)`, tạo `oos` trước `ois`, `myListening.start()`; trả true/false |
| `closeConnection()` | đặt cờ `closing = true`, đóng socket (listening tự thoát), `myFunction.clear()` |
| `isConnected()` | socket != null && đang mở |
| `sendData(Object o)` | có sẵn khung: đẩy việc ghi vào `sendExecutor` (1 luồng ⇒ giữ thứ tự), `oos.writeObject(o); flush(); reset();` |
| `getActiveFunction()` | có sẵn – `CopyOnWriteArrayList` an toàn khi vừa duyệt vừa thêm/gỡ |
| `register(int code, DataReceiver r)` / `unregister(DataReceiver r)` | tiện ích: `myFunction.add(new ObjectWrapper(code, r))` / `removeIf(f -> f.getData() == r)` |
| `setMyPlayer / getMyPlayer` | Player đang đăng nhập – dùng chung cho mọi màn hình (không cần truyền qua Intent mỗi lần) |
| `setCurrentActivity / getCurrentActivity` | Activity đang hiển thị (BaseActivity gán trong `onResume`, xóa trong `onPause`) – dùng để mở InviteDialog ở bất kỳ màn hình nào |
| `ClientListening.run()` (Thread nền) | vòng `readObject()`; với mỗi `fto` có cùng performative: `mainHandler.post(() -> ((DataReceiver) fto.getData()).receivedDataProcessing(d))` |
| mất kết nối | `IOException` mà `!closing` → `mainHandler.post`: Toast "Mất kết nối server", mở `ClientMainActivity` với `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` (dùng `applicationContext` lưu khi khởi tạo) |

### 4.2 `BaseActivity` (lớp cha chung – **[LỆCH BC: bổ sung để xử lý vòng đời Android]**)
`public abstract class BaseActivity extends AppCompatActivity implements DataReceiver`
- `onResume`: `ClientCtr.getInstance().setCurrentActivity(this)`; nếu không phải ClientMainActivity mà `!isConnected()` (app bị hệ điều hành dọn khi ở nền) → về ClientMainActivity.
- `onPause`: nếu current == this thì đặt null.
- `onDestroy`: `ClientCtr.getInstance().unregister(this)`.
- `protected void listen(int... codes)` – đăng ký nhiều mã một lúc.
- `protected void toast(String)`, `confirm(String, Runnable)` tiện ích (AlertDialog).

Quy ước chung: màn hình gọi `listen(...)` trong `onCreate` **trước** khi gửi yêu cầu; truyền dữ liệu giữa màn hình bằng `Intent.putExtra(key, (Serializable) obj)`; chuỗi tiếng Việt để trong `res/values/strings.xml`.

### 4.3 Màn hình CN1 (hình 2.1 – 2.5, khung điện thoại dọc)
| Màn hình | Layout | Hành vi chính |
|---|---|---|
| `ClientMainActivity` (LAUNCHER) | `activity_client_main.xml`: EditText host (mặc định `10.0.2.2`), port (`9999`), Button [Kết nối] | kết nối trên luồng nền → thành công: mở LoginActivity; lỗi: Toast "Không kết nối được server". `resetClient()` = đóng kết nối, hiện lại form |
| `LoginActivity` | EditText username, password (`inputType=textPassword`), [Đăng nhập], [Đăng ký tài khoản] | `listen(REPLY_LOGIN_USER)`; nhận `Player` → `setMyPlayer`, mở OnlineListActivity, `finish()`; `"false"` → "Sai tên đăng nhập hoặc mật khẩu"; `"logged_in"` → "Tài khoản đang đăng nhập ở thiết bị khác" |
| `RegisterActivity` | 6 EditText (`textPassword`, `textEmailAddress`, `phone`), [Đăng ký ngay], [Hủy bỏ] | `validateInput()`: không trống, MK khớp, email hợp lệ (nếu nhập) – báo lỗi bằng `EditText.setError`; `"ok"` → Toast thành công, `finish()`; `"false"` → "Tên đăng nhập đã tồn tại" |
| `EditPlayerActivity` | username là TextView (cố định), 3 EditText, [Cập nhật], [Đóng] | `"ok"` → cập nhật `getMyPlayer()`, Toast, `finish()` (OnlineListActivity cập nhật lời chào ở `onResume`) |
| `ChangePasswordActivity` | 3 EditText `textPassword`, [Lưu mật khẩu], [Hủy bỏ] | MK mới khớp xác nhận, khác MK cũ; `"false"` → "Mật khẩu hiện tại không đúng" |
| `PlayerHistoryActivity` | RecyclerView + `item_match_history.xml` (thẻ 3 dòng như hình 2.5) | thời gian `dd/MM/yyyy HH:mm`; tự lọc MatchResult của mình / của đối thủ; danh sách rỗng → "Chưa có trận đấu nào" |

### 4.4 `OnlineListActivity` (hình 3.1) và `RankingActivity`
- `listen(REPLY_ONLINE_LIST, SERVER_INFORM_ONLINE_LIST, SERVER_INFORM_CHALLENGE)`; rồi gửi GET_ONLINE_LIST.
- RecyclerView + `OnlinePlayerAdapter` (`item_online_player.xml`: STT/Tên/Điểm/Trạng thái "RẢNH"/"ĐANG BẬN"); chạm 1 dòng để chọn (tô nền), bỏ qua chính mình; `updateOnlineTable(list)` gọi `submitList`/`notifyDataSetChanged` và giữ lựa chọn theo `id`.
- Nút: [Sửa thông tin] [Đổi mật khẩu] [Lịch sử đấu] **[Bảng xếp hạng]** **[Chơi với máy]** [Đăng xuất] [Thách đấu] (có thể gom 4 nút đầu vào menu `⋮` trên thanh tiêu đề).
  - [Thách đấu]: phải chọn 1 dòng RẢNH → `ChallengeDialog.newInstance(target).show(getSupportFragmentManager(), ...)`.
  - [Chơi với máy] → `AISetupDialog.open(this);` (lớp của N11, chỉ gọi).
  - [Đăng xuất] (và nút Back – hỏi xác nhận) → `closeConnection()`, về ClientMainActivity, `finish()`.
- Nhận SERVER_INFORM_CHALLENGE → hiển thị `InviteDialog.newInstance(inviter)` trên **`ClientCtr.getCurrentActivity()`** (người chơi có thể đang ở màn hình Lịch sử/Sửa thông tin). Không có Activity nào đang hiện (app ở nền) → trả `REPLY_CHALLENGE_DECISION(false)` ngay.
- `RankingActivity`: `listen(REPLY_RANKING)`, gửi GET_RANKING; RecyclerView `item_ranking.xml`: Hạng/Tên/Tổng điểm/TB điểm đối thủ/TB nước thắng/TB nước thua (giá trị `-1` hiện "-").

### 4.5 Hộp thoại CN3 (hình 4.1, 4.2, 4.4) – `DialogFragment`, `setCancelable(false)`
| Hộp thoại | Đăng ký (trong `onCreate`, gỡ trong `onDestroy`) | Hành vi |
|---|---|---|
| `ChallengeDialog.newInstance(target)` | REPLY_CHALLENGE_BUSY, REPLY_CHALLENGE_REJECT, SERVER_START_MATCH | gửi SEND_CHALLENGE(target); BUSY → "Đối thủ đang bận"; REJECT → "Đối thủ đã từ chối"; START → mở `MatchPlayActivity` (extras `myColor`, `opponent`), `dismiss()` |
| `InviteDialog.newInstance(inviter)` | SERVER_START_MATCH, REPLY_CHALLENGE_REJECT | [Đồng ý] → `REPLY_CHALLENGE_DECISION(true)`, hiện "Đang vào phòng..."; [Từ chối] → `false` rồi `dismiss()`; START → mở MatchPlayActivity, `dismiss()` |
| `ContinueDialog.newInstance(GameResult r)` **[LỆCH BC: tham số String → GameResult]** | – | hiển thị lý do + "BẠN THẮNG (+1.0 điểm)" / "HÒA (+0.5)" / "BẠN THUA (+0)"; [Tiếp tục] → `REQUEST_CONTINUE_MATCH(true)`, đổi nội dung thành "Đang chờ đối thủ..."; [Dừng chơi] → `false` |

Dữ liệu truyền vào DialogFragment qua `Bundle` (`putSerializable`) trong `newInstance` – **không** dùng constructor có tham số (Android tạo lại fragment bằng constructor rỗng).
Câu chữ lý do: CHECKMATE "Chiếu bí!", STALEMATE "Hết nước đi – Hòa", TIMEOUT "Hết giờ 30 giây", RESIGN "Xin thua", DISCONNECT "Đối thủ mất kết nối".

### 4.6 `MatchPlayActivity` – khung phòng đấu **[LỆCH BC: bàn cờ tách thành ChessBoardView của N11]**
`activity_match_play.xml` (N04), dọc từ trên xuống:
```
TextView lblOpponentInfo   "Đối thủ: tranvanb (Điểm: 18.0)" / "Quân đối thủ: TRẮNG"
client.view.ChessBoardView @+id/boardView  (layout_height=0dp, layout_weight=1)  ← N11: bàn cờ, biên bản, đồng hồ 30s, "Chiếu!"
TextView lblMyColor        "Bạn: ... | Quân của bạn: ĐEN (Đi sau)"
Button   btnQuit           [Thoát ván (xin thua)]
```
`lblTimer` trong hình 4.5 **chuyển vào ChessBoardView** (đồng hồ do N11 quản lý).

| Sự kiện | Xử lý |
|---|---|
| `onCreate` | đọc extras `myColor`, `opponent`; `listen(SERVER_END_GAME, SERVER_RESTART_MATCH_SWAP_COLOR, SERVER_END_SESSION_MATCH, SERVER_INFORM_MOVE, REPLY_MOVE_ILLEGAL)`; `boardView.reset(myColor)`; giữ màn hình sáng (`FLAG_KEEP_SCREEN_ON`) |
| btnQuit / nút Back (`OnBackPressedCallback`) | đang chơi → xác nhận → `sendData(QUIT_GAME, null)`; ván đã kết thúc → bỏ qua (chờ ContinueDialog) |
| SERVER_INFORM_MOVE, REPLY_MOVE_ILLEGAL | `boardView.onServerMessage(d)` – không xử lý gì thêm |
| SERVER_END_GAME | `boardView.setLocked(true)`; `ContinueDialog.newInstance((GameResult) d.getData()).show(...)` |
| SERVER_RESTART_MATCH_SWAP_COLOR | `swapColor()`: `myColor = (String) data`; cập nhật 2 nhãn; đóng ContinueDialog; `boardView.reset(myColor)` |
| SERVER_END_SESSION_MATCH | `"OPPONENT_STOP"` → Toast "Đối thủ đã dừng chơi"; cả 2 trường hợp: đóng ContinueDialog nếu đang mở, `finish()` (quay lại OnlineListActivity bên dưới) |

---

## 5. Thứ tự làm (mỗi mốc chạy được)

| Mốc | Nội dung | Kiểm tra |
|---|---|---|
| M0 ✅ | **Đã xong** trong `Mobile_chess_v2`: project 3 module, model, khung server, khung toàn bộ Activity/Dialog + layout, manifest, `BaseActivity`, các file khung của N11 | `gradlew :server:build :app:assembleDebug` thành công (đã chạy); còn cần mở app trên máy ảo phát triển |
| M1 | `DAO` + config, `ServerMainFrm/ServerCtr/ServerListening/ServerProcessing`; `ClientCtr/ClientListening`, `DataReceiver`, `BaseActivity`, `ClientMainActivity` | 2 máy ảo kết nối `10.0.2.2:9999`, log server hiện 2 kết nối |
| M2 | CN1: Login/Register/Logout/Edit/ChangePassword | tạo 2 tài khoản; đăng nhập trùng bị chặn; DB có password hash 64 ký tự |
| M3 | CN2: OnlineListActivity + broadcast | đăng nhập/đăng xuất/tắt app ở máy này → máy kia cập nhật ngay |
| M4 | CN3: Challenge/Invite/MatchPlayActivity (ChessBoardView tạm là khung rỗng)/ContinueDialog/GameRoom/`saveMatch` | thách đấu bận/từ chối/đồng ý; lời mời hiện cả khi đối thủ đang ở màn Lịch sử; Thoát ván → DB có 1 tblMatch + 2 tblMatchResult; Tiếp tục đổi màu; Dừng chơi báo đối thủ; vuốt tắt app giữa ván → đối thủ thắng (DISCONNECT) |
| M5 | Lịch sử đấu + Bảng xếp hạng | số liệu khớp DB, thứ tự đúng 4 tiêu chí |
| M6 | Ghép với N11 (merge nhánh N11), thử thêm trên điện thoại thật qua Wi-Fi | chạy checklist tích hợp trong `docs/HUONG_DAN_N11.md` mục 7 |

---

## 6. Báo cáo N04
Đã chuyển `BAO_CAO_THIET_KE_GAME_CO_VUA.md` sang Android: các `...Frm` phía client → `...Activity`, widget Swing → `EditText/Button/TextView/RecyclerView`, `actionPerformed` → `onClick`, constructor → `onCreate`/`newInstance`, các bước `new ...Frm(...)` trong biểu đồ tuần tự → `startActivity(...)`/`show()`, 10 mockup vẽ lại theo khung điện thoại dọc, mục kiến trúc bổ sung nền tảng Android. `ServerMainFrm` giữ nguyên (server chạy trên PC).
Còn cần sửa thêm cho khớp code:
1. Hình 4.5: `GameRoom.endGame(winner: ServerProcessing, reason: String)`, thêm `isPlaying()`, `playerLeft()`, thuộc tính `referee: ChessReferee`.
2. Hình 4.5: `ContinueDialog.newInstance(result: GameResult)`; `MatchPlayActivity` thêm `boardView: ChessBoardView`, bỏ `lblTimer`; thêm `BaseActivity`.
3. Hình 4.8: bước 12/17 gửi `SERVER_END_GAME, GameResult`; bước 6 `saveMatch` lưu thêm tblMatchMove.
4. Mục I.1.d: kết thúc ván theo luật/hết giờ → "do chức năng của N11 thực hiện, gọi `GameRoom.endGame`".
5. Thêm mục Bảng xếp hạng + xử lý mất kết nối giữa ván.
6. Dựng lại ảnh sơ đồ (`images/fig_*.png`) và file DOCX/PDF từ file `.md` mới (`build_doc.py`).
