# HỢP ĐỒNG CHUNG N04 ↔ N11 (CẤU TRÚC PHÂN CHIA MỚI)

> Hệ thống: **1 Server Java chạy trên PC (kèm MySQL)** + **nhiều Client Android (Java)**.
> Ranh giới phân chia mới:
> - **N04 (Nguyễn Đại Dũng)** đảm nhiệm **mọi thứ trước khi vào trận đối kháng** + hạ tầng mạng/CSDL/tài khoản/BXH/lịch sử đấu.
> - **N11 (Nguyễn Tiến Dũng)** đảm nhiệm **toàn bộ phần trong trận đấu đối kháng** (từ lúc bắt đầu trận đến khi kết thúc phiên/thoát về Lobby) + chế độ **Chơi với máy (AI)**.

---

## 1. Cấu trúc module Gradle

- `:common` (`java-library`, Java 17 thuần – không dùng `Swing/AWT`, `JDBC`, hay `android.*`)
  - `model/*`: `ObjectWrapper`, `IPAddress`, `Player`, `PlayerStat`, `PlayerRanking`, `Match`, `MatchResult`, `MatchStartInfo`, `GameResult`, `MatchMove`, `MoveRequest`, `MoveInfo`
  - `com/chess/engine/**` (N11): thư viện luật cờ & AI
- `:server` (Java 17 trên PC, Swing `ServerMainFrm`, JDBC MySQL)
  - N04: `ServerRun`, `ServerMainFrm`, `ServerCtr` (`ServerDiscovery` UDP + `ServerListening` TCP + `ServerProcessing`), `PendingChallenge`, `MatchSession` (interface), `DAO`, `PlayerDAO`, `MatchDAO`
  - N11: `GameRoom` (cài đặt `MatchSession`), `ChessReferee`, `MoveTimeoutTask`, `MatchMoveDAO`
- `:app` (Android Java, `minSdk = 24`, `compileSdk = 37`, `namespace = vn.edu.ptit.mobilechess`)
  - N04: `ClientCtr`, `BaseActivity`, `DataReceiver`, `LoginActivity`, `RegisterActivity`, `OnlineListActivity`, `EditPlayerActivity`, `ChangePasswordActivity`, `PlayerHistoryActivity`, `RankingActivity`, `ChallengeDialog`, `InviteDialog`, các Adapter & layout của N04
  - N11: `MatchPlayActivity`, `ContinueDialog`, `ChessBoardView`, `BoardCanvasView`, `PromotionDialog`, `ai/AISetupDialog`, `ai/AIMatchActivity`, tài nguyên `n11_*` & `strings_n11.xml`
- `db/`: `01_core.sql` (N04) và `02_match_move.sql` (N11)

---

## 2. Bảng mã lệnh dùng chung (`model.ObjectWrapper`)

| Mã | Tên hằng | Chiều | `data` | Chủ quản |
|---|---|---|---|---|
| 1 | `LOGIN_USER` | C → S | `Player(username, password)` | N04 |
| 2 | `REPLY_LOGIN_USER` | S → C | `Player` (`password=null`) / `"false"` / `"logged_in"` | N04 |
| 3 | `REGISTER_USER` | C → S | `Player` | N04 |
| 4 | `REPLY_REGISTER_USER` | S → C | `"ok"` / `"false"` | N04 |
| 5 | `EDIT_PLAYER` | C → S | `Player(name, email, tel)` | N04 |
| 6 | `REPLY_EDIT_PLAYER` | S → C | `"ok"` / `"false"` | N04 |
| 7 | `CHANGE_PASSWORD` | C → S | `String[]{oldPass, newPass}` | N04 |
| 8 | `REPLY_CHANGE_PASSWORD` | S → C | `"ok"` / `"false"` | N04 |
| 9 | `GET_PLAYER_HISTORY` | C → S | `null` | N04 |
| 10 | `REPLY_PLAYER_HISTORY` | S → C | `ArrayList<Match>` | N04 |
| 11 | `GET_ONLINE_LIST` | C → S | `null` | N04 |
| 12 | `REPLY_ONLINE_LIST` | S → C | `ArrayList<PlayerStat>` | N04 |
| 13 | `SERVER_INFORM_ONLINE_LIST` | S → C | `ArrayList<PlayerStat>` | N04 |
| 14 | `SEND_CHALLENGE` | C → S | `PlayerStat` (đối thủ B) | N04 |
| 15 | `REPLY_CHALLENGE_BUSY` | S → C | `PlayerStat` (đối thủ B) | N04 |
| 16 | `SERVER_INFORM_CHALLENGE` | S → C | `PlayerStat` (người mời A) | N04 |
| 17 | `REPLY_CHALLENGE_DECISION` | C → S | `Boolean` (`true` = Đồng ý, `false` = Từ chối) | N04 |
| 18 | `REPLY_CHALLENGE_REJECT` | S → C | `"REJECT"` / `"CANCEL"` / `"TIMEOUT"` / `"DISCONNECT"` | N04 |
| 19 | `SERVER_START_MATCH` | S → C | `MatchStartInfo(myColor, me, opponent)` | N04 → N11 |
| 20 | `QUIT_GAME` | C → S | `null` | **N11** |
| 21 | `SERVER_END_GAME` | S → C | `GameResult` | **N11** |
| 22 | `REQUEST_CONTINUE_MATCH` | C → S | `Boolean` (`true` = Tiếp tục, `false` = Dừng) | **N11** |
| 23 | `SERVER_RESTART_MATCH_SWAP_COLOR` | S → C | `String` (`"WHITE"` / `"BLACK"`) | **N11** |
| 24 | `SERVER_END_SESSION_MATCH` | S → C | `"STOP"` / `"OPPONENT_STOP"` | **N11** |
| 25 | `GET_RANKING` | C → S | `null` | N04 |
| 26 | `REPLY_RANKING` | S → C | `ArrayList<PlayerRanking>` | N04 |
| 27 | `CANCEL_CHALLENGE` | C → S | `null` | N04 |
| 28 | `SERVER_CANCEL_CHALLENGE` | S → C | `"CANCEL"` / `"TIMEOUT"` / `"DISCONNECT"` | N04 |
| 30 | `SEND_MOVE` | C → S | `MoveRequest(fromSquare, toSquare, promotion)` | **N11** |
| 31 | `REPLY_MOVE_ILLEGAL` | S → C | `String` (lý do) | **N11** |
| 32 | `SERVER_INFORM_MOVE` | S → C | `MoveInfo(notation, fen, sideToMove, inCheck)` | **N11** |

---

## 3. Bốn điểm bàn giao N04 ↔ N11

1. **Kết nối tự động & giữ Socket chung:**
   - App mở thẳng vào `LoginActivity`; khi bấm *Đăng nhập* hoặc *Đăng ký ngay*, `ClientCtr.openConnection()` tự dò server qua UDP broadcast cổng `9999` (`"DISCOVER_SERVER"`) rồi mở Socket TCP.
   - `ClientCtr.getInstance()` sống suốt vòng đời app; mọi Activity/Dialog của N11 dùng `ClientCtr.getInstance().sendData(...)`, `register(...)`, `unregister(...)`.
2. **Bàn giao lúc vào trận (N04 → N11):**
   - **Server:** Khi B đồng ý lời mời trong `PendingChallenge`, N04 tạo `MatchSession session = new GameRoom(myServer, challenger, challenged)`, gán vào 2 `ServerProcessing` (trạng thái `BUSY`) và gọi `session.start()`.
   - **Client:** `GameRoom.start()` gửi `SERVER_START_MATCH` kèm `MatchStartInfo(myColor, me, opponent)` (A = `BLACK`, B = `WHITE`). `ChallengeDialog` / `InviteDialog` mở `MatchPlayActivity` và truyền qua `Intent.putExtra(MatchPlayActivity.EXTRA_START_INFO, info)`.
3. **Trong trận & kết thúc phiên (N11 → N04):**
   - **Server:** `ServerProcessing` chuyển tiếp toàn bộ gói tin mã `20..24, 30..39` vào `matchSession.onPacket(this, data)`, và gọi `matchSession.onDisconnect(this)` nếu client mất kết nối khi đang ở trong phòng.
   - **Server khi đóng phòng:** N11 gọi `myServer.onMatchSessionEnded(this, challenger, challenged)` để N04 trả 2 người về `IDLE` và `broadcastOnlineList()`.
   - **Client:** `MatchPlayActivity` gọi `myControl.flushPendingMatchMessages(this)` cuối `onCreate` để không sót gói đến sớm; khi hết phiên thì gọi `finish()` để quay lại `OnlineListActivity`.
4. **CSDL dùng chung (`tblMatch`, `tblMatchResult`, `tblMatchMove`):**
   - N11 gọi `MatchDAO.saveMatch(match)` (hoặc tự lưu trong 1 transaction) khi mỗi ván kết thúc.
   - Quy ước `tblMatchMove.moveIndex`: bắt đầu từ `1`; **nước lẻ (`1, 3, 5...`) là quân TRẮNG**, **nước chẵn (`2, 4, 6...`) là quân ĐEN** — để `PlayerDAO.getRanking()` của N04 đếm chính xác số nước do từng người đi.
