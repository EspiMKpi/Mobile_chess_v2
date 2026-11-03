# BLUEPRINT CODE – PHẦN N04 (Nguyễn Đại Dũng)

> Phạm vi: **CN1** Quản lý tài khoản & thông tin · **CN2** Danh sách online · **CN3** Thách đấu & ghép cặp · **BXH** (bổ sung theo đề) · toàn bộ **tầng mạng** Client/Server.
> Nguồn spec: `BAO_CAO_THIET_KE_GAME_CO_VUA.md` (hình 2.x, 3.x, 4.x). Mockup: `images/ui_templates/*.html`.
> Hợp đồng với N11 (mã nguồn các lớp nối): `docs/HOP_DONG_CHUNG.md` – tạo các lớp đó trước. Hướng dẫn cho N11: `docs/HUONG_DAN_N11.md`. Chỗ nào blueprint khác báo cáo đều ghi **[LỆCH BC]** kèm lý do.

---

## 0. Môi trường & quy ước

| Mục | Giá trị |
|---|---|
| JDK | 25 – `C:\Program Files\Android\Android Studio\jbr\bin` |
| CSDL | MySQL 8, DB `chess`; chạy `db/01_core.sql` rồi `db/02_match_move.sql` |
| Driver | `lib/mysql-connector-j-*.jar` |
| Cấu hình DB | `config/db.properties` (`url`, `user`, `password`) – **không** commit mật khẩu thật, commit `config/db.properties.example` |
| Cổng server | `9999` (mặc định trong `ServerMainFrm`, sửa được trên giao diện) |
| Biên dịch | `javac -encoding UTF-8 -cp "lib/*" -d bin $(find src -name "*.java")` |
| Chạy | `java -cp "bin;lib/*" server.ServerRun` · `java -cp "bin;lib/*" client.ClientRun` |
| Swing | Viết tay, không `.form`. Mọi cập nhật giao diện từ luồng mạng đi qua `SwingUtilities.invokeLater` |
| Nhánh git | `feature/n04-account-lobby-challenge` |
| Không được sửa | file của N11 (xem `.github/CODEOWNERS`) – cần đổi thì mở PR hợp đồng |

---

## 1. Danh sách file phải làm

```
src/server/ServerRun.java                    main: new ServerMainFrm().setVisible(true)
src/server/view/ServerMainFrm.java           hình 2.6
src/server/control/ServerCtr.java            (khung ở HOP_DONG_CHUNG) + ServerListening, ServerProcessing là inner class
src/server/control/GameRoom.java             (khung ở HOP_DONG_CHUNG)
src/server/dao/DAO.java                      (khung ở HOP_DONG_CHUNG – có sẵn hashPassword)
src/server/dao/PlayerDAO.java                MỚI
src/server/dao/MatchDAO.java                 (khung ở HOP_DONG_CHUNG)
src/client/ClientRun.java                    main: new ClientMainFrm().setVisible(true)
src/client/control/ClientCtr.java            (khung ở HOP_DONG_CHUNG) + ClientListening inner class
src/client/view/DataReceiver.java            MỚI – interface { void receivedDataProcessing(ObjectWrapper d); }
src/client/view/ClientMainFrm.java
src/client/view/LoginFrm.java, RegisterFrm.java, OnlineListFrm.java,
                EditPlayerFrm.java, ChangePasswordFrm.java, PlayerHistoryFrm.java, RankingFrm.java
src/client/view/ChallengeDialog.java, InviteDialog.java, MatchPlayFrm.java, ContinueDialog.java
config/db.properties.example
run_server.bat, run_client.bat
```
Model (`src/model/*`): tạo đúng như `docs/HOP_DONG_CHUNG.md` mục 3 (N04 tạo cả lớp model của N11 ở bước đầu để 2 bên cùng biên dịch được) – sau đó **chỉ được thêm**, không đổi chữ ký.

---

## 2. Bảng mã lệnh & dữ liệu kèm theo (`ObjectWrapper`)

`→S` = client gửi lên server · `→C` = server gửi xuống client.

| Mã | Tên | Chiều | `data` | Ai đăng ký nhận ở client |
|---|---|---|---|---|
| 1 | LOGIN_USER | →S | `Player(username, password thô)` | |
| 2 | REPLY_LOGIN_USER | →C | `Player` đầy đủ (password = null) **hoặc** `"false"` / `"logged_in"` | LoginFrm |
| 3 | REGISTER_USER | →S | `Player` | |
| 4 | REPLY_REGISTER_USER | →C | `"ok"` / `"false"` | RegisterFrm |
| 5 | EDIT_PLAYER | →S | `Player` (name, email, tel mới) | |
| 6 | REPLY_EDIT_PLAYER | →C | `"ok"` / `"false"` | EditPlayerFrm |
| 7 | CHANGE_PASSWORD | →S | `String[]{oldPass, newPass}` | |
| 8 | REPLY_CHANGE_PASSWORD | →C | `"ok"` / `"false"` | ChangePasswordFrm |
| 9 | GET_PLAYER_HISTORY | →S | `null` | |
| 10 | REPLY_PLAYER_HISTORY | →C | `ArrayList<Match>` (mỗi Match 2 MatchResult) | PlayerHistoryFrm |
| 11 | GET_ONLINE_LIST | →S | `null` | |
| 12 | REPLY_ONLINE_LIST | →C | `ArrayList<PlayerStat>` | OnlineListFrm |
| 13 | SERVER_INFORM_ONLINE_LIST | →C | `ArrayList<PlayerStat>` | OnlineListFrm |
| 14 | SEND_CHALLENGE | →S | `PlayerStat` đối thủ B | |
| 15 | REPLY_CHALLENGE_BUSY | →C | `PlayerStat` B | ChallengeDialog |
| 16 | SERVER_INFORM_CHALLENGE | →C | `PlayerStat` A (có totalPoint) | OnlineListFrm |
| 17 | REPLY_CHALLENGE_DECISION | →S | `Boolean` | |
| 18 | REPLY_CHALLENGE_REJECT | →C | `PlayerStat` B | ChallengeDialog |
| 19 | SERVER_START_MATCH | →C | `MatchStartInfo{myColor, opponent}` | ChallengeDialog (A), InviteDialog (B) |
| 20 | QUIT_GAME | →S | `null` | |
| 21 | SERVER_END_GAME | →C | `GameResult` **[LỆCH BC: báo cáo ghi String]** | MatchPlayFrm |
| 22 | REQUEST_CONTINUE_MATCH | →S | `Boolean` | |
| 23 | SERVER_RESTART_MATCH_SWAP_COLOR | →C | `String` màu mới | MatchPlayFrm |
| 24 | SERVER_END_SESSION_MATCH | →C | `"STOP"` / `"OPPONENT_STOP"` | MatchPlayFrm |
| 25 | GET_RANKING | →S | `null` | |
| 26 | REPLY_RANKING | →C | `ArrayList<PlayerRanking>` đã sắp | RankingFrm |
| 30 | SEND_MOVE | →S | `MoveRequest` (N11 gửi) | |
| 31 | REPLY_MOVE_ILLEGAL | →C | `String` | **MatchPlayFrm** → chuyển cho `ChessBoardPanel` |
| 32 | SERVER_INFORM_MOVE | →C | `MoveInfo` | **MatchPlayFrm** → chuyển cho `ChessBoardPanel` |

`"logged_in"` trong REPLY_LOGIN_USER là giá trị bổ sung (báo cáo gộp chung vào `"false"`) để LoginFrm báo đúng lỗi "đang đăng nhập ở máy khác".

---

## 3. Server

### 3.1 `ServerMainFrm` (hình 2.6)
`txtServerHost` (chỉ đọc, `InetAddress.getLocalHost()`), `txtServerPort` (mặc định 9999), `btnStartServer`, `btnStopServer`, `mainText` (JTextArea log). Start → `myServer = new ServerCtr(this); myServer.openServer()`. `showMessage(s)` append log qua `invokeLater`.

### 3.2 `ServerCtr`
| Thành phần | Việc |
|---|---|
| `ServerCtr(ServerMainFrm v)` | lưu view, `myAddress = new IPAddress(host, port)` |
| `openServer()` | `new ServerSocket(port)`, `myListening = new ServerListening(); start()` |
| `stopServer()` | đóng mọi socket client, đóng ServerSocket |
| `ServerListening.run()` | vòng `accept()` → `ServerProcessing sp = new ServerProcessing(s); synchronized(myProcess){add}; sp.start()` |
| `findProcess(int playerId)` | duyệt `myProcess`, trả sp có `loggedInPlayer.id == playerId` hoặc null |
| `removeProcess(sp)` | xóa khỏi `myProcess` |
| `createRoom(a, b)` | `new GameRoom(this, a, b)`; `a,b.setRoom(room)`, `setStatus(BUSY)`; add `myRooms`; trả room |
| `removeRoom(r)` | xóa khỏi `myRooms` |
| `getOnlinePlayers()` | với mỗi sp đã login: `new PlayerStat(sp.loggedInPlayer, playerDAO.getTotalPoint(id), sp.getStatus())` |
| `broadcastOnlineList()` | `list = getOnlinePlayers()`; gửi `SERVER_INFORM_ONLINE_LIST` cho mọi sp đã login |

Mọi thao tác trên `myProcess`/`myRooms` đặt trong `synchronized (myProcess)` / `synchronized (myRooms)`.

**Gọi `broadcastOnlineList()` đúng 4 thời điểm:** login thành công · sp kết thúc (logout/mất mạng) · `GameRoom.startGame()` · `GameRoom.closeRoom()`.
Không gọi khi đang chờ trả lời thách đấu (theo báo cáo, A/B đã BUSY nhưng chỉ broadcast khi vào ván).

### 3.3 `ServerProcessing` (inner class)
Trường: `mySocket`, `ObjectInputStream ois`, `ObjectOutputStream oos`, `loggedInPlayer`, `status = IDLE`, `room`.
Tạo `oos` **trước** `ois` (tránh treo header stream).

`run()`:
```
try {
  while (true) {
    ObjectWrapper w = (ObjectWrapper) ois.readObject();
    switch (w.getPerformative()) { ... bảng dưới ... }
  }
} catch (IOException | ClassNotFoundException e) { /* client đóng */ }
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
Trạng thái: `whitePlayer`, `blackPlayer`, `currentMatch`, `continueCount`, `playing`, `referee`, thêm `boolean decided` (đã có ai chọn Dừng).

| Phương thức | Làm gì (theo thứ tự) |
|---|---|
| `receiveChallengeDecision(accept)` | false → `closeRoom()`; gửi `REPLY_CHALLENGE_REJECT(B)` cho **người thách đấu** (= blackPlayer ván 1). true → `startGame()` |
| `startGame()` | `beginNewMatch()`; `myServer.broadcastOnlineList()`; gửi `SERVER_START_MATCH(new MatchStartInfo("BLACK", statOf(white)))` cho black, `("WHITE", statOf(black))` cho white |
| `beginNewMatch()` (private) | `currentMatch = new Match(new Date()); continueCount = 0; playing = true; referee.reset();` |
| `endGame(winner, reason)` | `if (!playing) return; playing = false; referee.stopTimer();` → `currentMatch.setEndTime(now)`; tạo 2 `MatchResult` (WIN/LOSE hoặc DRAW/DRAW nếu winner null, kèm màu); `currentMatch.getMoves().addAll(referee.getMoves())`; `matchDAO.saveMatch(currentMatch)`; gửi mỗi bên `SERVER_END_GAME(new GameResult(result, reason, color, point))` |
| `receiveContinueDecision(sp, accept)` | false → `notify` cả 2: người bấm nhận `"STOP"`, người kia `"OPPONENT_STOP"`; `closeRoom()`. true → `continueCount++`; `==2` → `swapColor(); beginNewMatch();` gửi `SERVER_RESTART_MATCH_SWAP_COLOR("WHITE")` cho whitePlayer mới, `("BLACK")` cho blackPlayer mới |
| `closeRoom()` | `playing = false; referee.stopTimer();` 2 bên `setStatus(IDLE)`, `setRoom(null)`; `myServer.removeRoom(this)`; `myServer.broadcastOnlineList()` |
| `playerLeft(sp)` (MỚI, public, chỉ N04 dùng) | đang chờ trả lời thách đấu (`currentMatch == null`): nếu người rời là B → gửi `REPLY_CHALLENGE_REJECT` cho A. Đã vào ván: `if (playing) endGame(opponent, GameResult.DISCONNECT)`; gửi `SERVER_END_SESSION_MATCH("OPPONENT_STOP")` cho đối thủ. Cuối cùng `closeRoom()` |
| `isPlaying()` | trả `playing` – N11 dùng |

**Khóa:** tất cả phương thức trên `synchronized` (trên room). Không gọi `sendData` của client khác bên ngoài khóa room cũng được, nhưng **không** được lấy khóa `myProcess` rồi gọi vào room (tránh deadlock ngược) – `broadcastOnlineList()` chỉ lấy snapshot `myProcess` rồi gửi.

### 3.5 DAO
`DAO()` – nếu `con == null`: đọc `config/db.properties`, `DriverManager.getConnection`. Dùng `PreparedStatement` cho mọi câu SQL.

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

Cách làm đơn giản, dễ đọc: 1 câu SQL lấy (playerId, matchId, result, opponentId, moveCount), tính trong Java rồi sắp bằng `Comparator`:
```
totalPoint DESC → avgOpponentPoint DESC → avgMovesWin ASC (NULL xếp cuối) → avgMovesLose DESC (NULL xếp cuối)
```
Nếu N11 chưa ghi nước đi thì moveCount = 0 – vẫn chạy được.

---

## 4. Client

### 4.1 `ClientCtr` + `ClientListening`
| Thành phần | Việc |
|---|---|
| `ClientCtr(ClientMainFrm v)` | lưu view, `serverAddress` từ form |
| `openConnection()` | `new Socket(host, port)`, tạo `oos` trước `ois`, `myListening.start()` |
| `closeConnection()` | dừng listening, đóng socket |
| `sendData(Object o)` | `synchronized`; `oos.writeObject(o); flush(); reset();` trả true/false |
| `getActiveFunction()` | trả `myFunction` (dùng `CopyOnWriteArrayList`) |
| `ClientListening.run()` | đọc `ObjectWrapper d`; với mỗi `fto` trong `myFunction` có `fto.getPerformative() == d.getPerformative()`: `SwingUtilities.invokeLater(() -> ((DataReceiver) fto.getData()).receivedDataProcessing(d))` |

Quy ước đăng ký: form đăng ký trong constructor (hoặc ngay trước khi gửi yêu cầu) và **gỡ** khi `dispose()` (`myFunction.removeIf(f -> f.getData() == this)`), tránh 1 gói tin gọi vào form đã đóng.

Mất kết nối (`IOException` ở listening) → `invokeLater`: thông báo "Mất kết nối server", đóng mọi form, `view.resetClient()`.

### 4.2 Các form CN1 (hình 2.1 – 2.5; mockup `images/ui_templates/fig_2_*.html`)
| Form | Hành vi chính |
|---|---|
| `ClientMainFrm` | host/port + [Kết nối] → `openConnection()` → mở `LoginFrm`. `resetClient()` đưa về trạng thái chưa kết nối |
| `LoginFrm` | đăng ký REPLY_LOGIN_USER; nhận `Player` → `new OnlineListFrm(myControl, player)`, dispose; `"false"` → "Sai tên đăng nhập hoặc mật khẩu"; `"logged_in"` → "Tài khoản đang đăng nhập ở máy khác" |
| `RegisterFrm` | `validateInput()`: không trống, MK khớp, email có `@` (nếu nhập); `"ok"` → báo thành công, đóng; `"false"` → "Tên đăng nhập đã tồn tại" |
| `EditPlayerFrm` | username chỉ đọc; `"ok"` → cập nhật `myPlayer` + nhãn chào ở OnlineListFrm |
| `ChangePasswordFrm` | MK mới khớp xác nhận, khác MK cũ; `"false"` → "Mật khẩu hiện tại không đúng" |
| `PlayerHistoryFrm` | JTable 7 cột STT/Bắt đầu/Kết thúc/Đối thủ/Quân cầm/Kết quả/Điểm (dd/MM/yyyy HH:mm), tự lọc MatchResult của mình và của đối thủ |

### 4.3 `OnlineListFrm` (hình 3.1) và `RankingFrm`
- Đăng ký trong constructor: REPLY_ONLINE_LIST, SERVER_INFORM_ONLINE_LIST, SERVER_INFORM_CHALLENGE; rồi gửi GET_ONLINE_LIST.
- `updateOnlineTable(list)`: bỏ chính mình; cột STT/Tên/Tổng điểm/Trạng thái ("RẢNH"/"ĐANG BẬN"); giữ dòng đang chọn sau khi làm mới.
- Nút: [Sửa thông tin] [Đổi mật khẩu] [Lịch sử đấu] **[Bảng xếp hạng]** **[Chơi với máy]** [Đăng xuất] [Thách đấu].
  - [Thách đấu]: phải chọn 1 dòng RẢNH → `new ChallengeDialog(myControl, target)`.
  - [Chơi với máy] → `new client.view.ai.AISetupDialog(this).setVisible(true);` (lớp của N11, chỉ gọi).
  - [Đăng xuất] → `closeConnection()`, `resetClient()`, dispose.
- Nhận SERVER_INFORM_CHALLENGE → `new InviteDialog(myControl, inviter)`.
- `RankingFrm`: đăng ký REPLY_RANKING, gửi GET_RANKING; JTable: Hạng/Tên/Tổng điểm/TB điểm đối thủ/TB nước thắng/TB nước thua (NULL hiện "-").
- Khi đang trong ván, ẩn OnlineListFrm (`setVisible(false)`) và hiện lại khi về sảnh – **không** dispose để vẫn nhận cập nhật online.

### 4.4 Các form CN3 (hình 4.1 – 4.4)
| Form | Đăng ký | Hành vi |
|---|---|---|
| `ChallengeDialog(c, target)` | REPLY_CHALLENGE_BUSY, REPLY_CHALLENGE_REJECT, SERVER_START_MATCH | gửi SEND_CHALLENGE(target); BUSY → "Đối thủ đang bận"; REJECT → "Đối thủ đã từ chối"; START → `new MatchPlayFrm(c, info.getMyColor(), info.getOpponent())`, dispose |
| `InviteDialog(c, inviter)` | SERVER_START_MATCH | [Đồng ý] → `REPLY_CHALLENGE_DECISION(true)`; [Từ chối] → `false` rồi dispose; START → mở MatchPlayFrm, dispose |
| `MatchPlayFrm(c, myColor, opponent)` | SERVER_END_GAME, SERVER_RESTART_MATCH_SWAP_COLOR, SERVER_END_SESSION_MATCH, **SERVER_INFORM_MOVE, REPLY_MOVE_ILLEGAL** | xem 4.5 |
| `ContinueDialog(c, GameResult r)` **[LỆCH BC: tham số String → GameResult]** | – | hiển thị lý do + "BẠN THẮNG (+1.0 điểm)" / "HÒA (+0.5)" / "BẠN THUA (+0)"; [Tiếp tục] → `REQUEST_CONTINUE_MATCH(true)` rồi hiện "Đang chờ đối thủ..."; [Dừng chơi] → `false` |

Câu chữ lý do: CHECKMATE "Chiếu bí!", STALEMATE "Hết nước đi – Hòa", TIMEOUT "Hết giờ 30 giây", RESIGN "Xin thua", DISCONNECT "Đối thủ mất kết nối".

### 4.5 `MatchPlayFrm` – khung phòng đấu **[LỆCH BC: tách bàn cờ sang ChessBoardPanel của N11]**
```
NORTH : lblOpponentInfo ("Đối thủ: tranvanb (Điểm: 18.0) | Màu quân đối thủ: TRẮNG")
CENTER: boardPanel = new ChessBoardPanel(myControl)      ← N11 (bàn cờ, biên bản, đồng hồ 30s)
SOUTH : lblMyColor ("Bạn: ... | Màu quân của bạn: ĐEN (Đi sau)") + btnQuit [Thoát ván (xin thua)]
```
`lblTimer` trong hình 4.5 **chuyển vào ChessBoardPanel** (đồng hồ do N11 quản lý).

| Sự kiện | Xử lý |
|---|---|
| constructor | dựng giao diện, đăng ký 5 mã, `boardPanel.reset(myColor)`, ẩn OnlineListFrm |
| btnQuit | xác nhận → `sendData(QUIT_GAME, null)` |
| SERVER_INFORM_MOVE, REPLY_MOVE_ILLEGAL | `boardPanel.onServerMessage(d)` – không xử lý gì thêm |
| SERVER_END_GAME | `boardPanel.setLocked(true)`; `new ContinueDialog(myControl, (GameResult) d.getData())` |
| SERVER_RESTART_MATCH_SWAP_COLOR | `swapColor()`: `myColor = (String) data`; cập nhật 2 nhãn; `boardPanel.reset(myColor)` |
| SERVER_END_SESSION_MATCH | `"OPPONENT_STOP"` → thông báo "Đối thủ đã dừng chơi"; cả 2 trường hợp: đóng ContinueDialog nếu đang mở, gỡ đăng ký, dispose, hiện lại OnlineListFrm |
| đóng cửa sổ (X) | coi như btnQuit (nếu đang chơi) |

---

## 5. Thứ tự làm (mỗi mốc chạy được)

| Mốc | Nội dung | Kiểm tra |
|---|---|---|
| M0 | Tạo toàn bộ lớp trong `docs/HOP_DONG_CHUNG.md` (model + khung), commit lên `main` | biên dịch được |
| M1 | `DAO` + config, `ServerMainFrm/ServerCtr/ServerListening/ServerProcessing`, `ClientMainFrm/ClientCtr/ClientListening`, `DataReceiver` | 2 client kết nối được, log server hiện kết nối |
| M2 | CN1: Login/Register/Logout/Edit/ChangePassword | tạo 2 tài khoản; đăng nhập trùng bị chặn; DB có password hash 64 ký tự |
| M3 | CN2: OnlineListFrm + broadcast | login/logout ở client này → client kia cập nhật ngay |
| M4 | CN3: Challenge/Invite/MatchPlayFrm (ChessBoardPanel tạm là khung rỗng)/ContinueDialog/GameRoom/`saveMatch` | thách đấu bận/từ chối/đồng ý; Thoát ván → DB có 1 tblMatch + 2 tblMatchResult; Tiếp tục đổi màu; Dừng chơi báo đối thủ; tắt hẳn 1 client giữa ván → đối thủ thắng (DISCONNECT) |
| M5 | Lịch sử đấu + Bảng xếp hạng | số liệu khớp DB, thứ tự đúng 4 tiêu chí |
| M6 | Ghép với N11 (merge nhánh N11) | chạy checklist tích hợp trong `docs/HUONG_DAN_N11.md` mục 7 |

---

## 6. Sửa báo cáo N04 cho khớp code (nên làm trước khi nộp)
1. Hình 4.5: `GameRoom.endGame(winner: ServerProcessing, reason: String)`, thêm `isPlaying()`, thêm thuộc tính `referee: ChessReferee`.
2. Hình 4.5: `ContinueDialog(c: ClientCtr, result: GameResult)`; `MatchPlayFrm` thêm `boardPanel: ChessBoardPanel`, bỏ `lblTimer`.
3. Hình 4.8: bước 12/17 gửi `SERVER_END_GAME, GameResult`; bước 6 `saveMatch` lưu thêm tblMatchMove.
4. Mục I.1.d: kết thúc ván theo luật/hết giờ → "do chức năng của N11 thực hiện, gọi `GameRoom.endGame`".
5. Thêm mục Bảng xếp hạng (nếu giảng viên chấm theo đề) + xử lý mất kết nối giữa ván.
