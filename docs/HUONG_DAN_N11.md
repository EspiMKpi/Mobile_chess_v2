# HƯỚNG DẪN DÀNH CHO THÀNH VIÊN N11 (MODULE ĐẤU ĐỐI KHÁNG & CHƠI VỚI MÁY)

> Tài liệu này hướng dẫn phần việc của **N11** theo **cấu trúc phân chia mới** (đọc kèm [HOP_DONG_CHUNG.md](HOP_DONG_CHUNG.md)).
> Phần **N04** đã lo toàn bộ: tự tìm server UDP, đăng ký/đăng nhập, sửa thông tin, đổi mật khẩu, lịch sử đấu, bảng xếp hạng, danh sách online realtime, và gửi/nhận/hủy/timeout 30s lời mời thách đấu.

---

## 1. Bạn (N11) cần làm những gì?

Bạn phụ trách **trọn gói 2 phần**:
1. **Module Đấu đối kháng trực tuyến (từ lúc bắt đầu trận trở đi):**
   - Server: hoàn thiện `server/src/main/java/server/control/GameRoom.java` (cài đặt `MatchSession`), thêm `ChessReferee`, `MoveTimeoutTask`, `MatchMoveDAO`.
   - Common: thêm gói engine luật cờ (`common/src/main/java/com/chess/engine/**`) – Java thuần, không dùng Swing/AWT/JDBC/`android.*`.
   - Client Android: hoàn thiện `app/src/main/java/client/view/MatchPlayActivity.java`, tạo `ContinueDialog`, `ChessBoardView` / `BoardCanvasView`, `PromotionDialog`, layout `n11_*`, chuỗi `strings_n11.xml`.
   - DB: chạy `db/02_match_move.sql` sau `db/01_core.sql`.
2. **Module Chơi với máy (AI) offline:**
   - Hoàn thiện `client/view/ai/AISetupDialog.java` và `client/view/ai/AIMatchActivity.java` (N04 đã gắn nút *Chơi với máy* trên `OnlineListActivity` gọi `AISetupDialog.open(this)`).

> **Các file KHÔNG nên tự ý sửa:** `ServerCtr.java`, `PendingChallenge.java`, `DAO.java`, `PlayerDAO.java`, `ClientCtr.java`, `BaseActivity.java`, `OnlineListActivity.java`, `ChallengeDialog.java`, `InviteDialog.java`, `ObjectWrapper.java` (nếu cần thêm mã lệnh thì báo N04 cùng thống nhất).

---

## 2. Luồng bàn giao lúc bắt đầu trận

1. Khi người được mời B bấm **Đồng ý** trên `InviteDialog`:
   - `PendingChallenge` của N04 tạo `MatchSession session = new GameRoom(myServer, challenger, challenged)`, gán vào 2 `ServerProcessing` và gọi `session.start()`.
2. Trong `GameRoom.start()` của bạn:
   - Khởi tạo ván mới (`new Match(new Date())`, `referee.reset()`, `playing = true`, bật đồng hồ 30s cho quân Trắng).
   - Lấy `PlayerStat` của 2 người bằng `myServer.getPlayerStat(challenger)` và `myServer.getPlayerStat(challenged)`.
   - Gửi `ObjectWrapper(ObjectWrapper.SERVER_START_MATCH, new MatchStartInfo(...))` cho cả 2 bên:
     - `challenger` (người mời A) cầm **`MatchResult.BLACK`** (đi sau).
     - `challenged` (người được mời B) cầm **`MatchResult.WHITE`** (đi trước).
3. Phía Android: `ChallengeDialog` / `InviteDialog` của N04 nhận `SERVER_START_MATCH`, mở `MatchPlayActivity` và truyền `MatchStartInfo` qua extra `MatchPlayActivity.EXTRA_START_INFO`.

---

## 3. Giao tiếp trong trận đấu (dùng chung Socket của N04)

### Phía Server (`GameRoom implements MatchSession`)
- `ServerProcessing` tự động chuyển tiếp mọi gói tin có mã **`20..24` và `30..39`** vào hàm `onPacket(ServerCtr.ServerProcessing sender, ObjectWrapper packet)` của bạn:
  - `ObjectWrapper.SEND_MOVE` (`30`): `data` là `MoveRequest(fromSquare, toSquare, promotion)`.
    - Sai lượt / sai luật → `sender.sendData(new ObjectWrapper(ObjectWrapper.REPLY_MOVE_ILLEGAL, "lý do"))`.
    - Hợp lệ → cập nhật bàn cờ, ghi `MatchMove`, đặt lại đồng hồ 30s, gửi `SERVER_INFORM_MOVE` (`MoveInfo`) cho **cả 2 người chơi**.
    - Nếu chiếu bí / hết nước / hết 30s → kết thúc ván, lưu DB, gửi `SERVER_END_GAME` (`GameResult`) riêng cho từng người chơi.
  - `ObjectWrapper.QUIT_GAME` (`20`): `sender` xin thua → xử đối thủ thắng với lý do `GameResult.RESIGN`, lưu DB, gửi `SERVER_END_GAME` cho cả 2.
  - `ObjectWrapper.REQUEST_CONTINUE_MATCH` (`22`): `data` là `Boolean` (`true` = Tiếp tục, `false` = Dừng chơi).
    - Nếu cả 2 đồng ý tiếp tục (và nên có timeout 30s chờ chọn): hoán đổi màu `WHITE <-> BLACK`, tạo ván mới, gửi `SERVER_RESTART_MATCH_SWAP_COLOR` kèm màu mới cho từng người.
    - Nếu 1 người chọn Dừng (hoặc hết giờ chọn): gửi `SERVER_END_SESSION_MATCH` (`"STOP"` cho người bấm, `"OPPONENT_STOP"` cho người kia), rồi gọi **`myServer.onMatchSessionEnded(this, challenger, challenged)`** để N04 giải phóng 2 người về `IDLE` và cập nhật danh sách online.
- Khi 1 người rớt mạng / tắt app giữa chừng, `ServerProcessing` gọi `onDisconnect(ServerCtr.ServerProcessing player)`:
  - Nếu ván đang diễn ra (`playing`), xử đối thủ thắng với lý do `GameResult.DISCONNECT`, lưu DB.
  - Gửi `SERVER_END_SESSION_MATCH("OPPONENT_STOP")` cho người còn lại (nếu vẫn online).
  - Gọi **`myServer.onMatchSessionEnded(this, challenger, challenged)`**.

> **Quy tắc đồng bộ luồng trên Server:** Mọi hàm trong `GameRoom` và `ChessReferee` (kể cả `MoveTimeoutTask` chạy trên `Timer`) nên đồng bộ trên **cùng một khóa là đối tượng `GameRoom`** để tránh deadlock và tránh lưu trùng 2 lần (đảm bảo `endGame` chỉ chạy 1 lần cho mỗi ván). `ServerProcessing.sendData(...)` đã `synchronized` sẵn.

### Phía Client Android (`MatchPlayActivity`)
- `MatchPlayActivity` kế thừa `BaseActivity` (đã có `myControl = ClientCtr.getInstance()`).
- Trong `onCreate`:
  1. Đọc `(MatchStartInfo) getIntent().getSerializableExtra(EXTRA_START_INFO)`.
  2. Đăng ký nhận các mã `SERVER_END_GAME`, `SERVER_RESTART_MATCH_SWAP_COLOR`, `SERVER_END_SESSION_MATCH`, `REPLY_MOVE_ILLEGAL`, `SERVER_INFORM_MOVE` bằng `myControl.register(...)`.
  3. Gọi **`myControl.flushPendingMatchMessages(this)`** để nhận lại các gói tin (ví dụ nước đi đầu tiên của Trắng) nếu có gửi đến trước khi Activity kịp mở.
- Khi hết phiên (`SERVER_END_SESSION_MATCH`) → đóng dialog đang mở (nếu có) và gọi `finish()` để quay về `OnlineListActivity`.
- Lưu ý lifecycle Android: nếu app bị đẩy xuống nền (`getSupportFragmentManager().isStateSaved()`), hoãn việc mở `ContinueDialog` sang `onPostResume()` để tránh crash `IllegalStateException`.

---

## 4. Lưu CSDL để Lịch sử đấu & Bảng xếp hạng (N04) hoạt động đúng

Khi mỗi ván kết thúc, bạn có thể tạo đối tượng `Match` (gồm `startTime`, `endTime`, 2 `MatchResult` với `WIN`/`DRAW`/`LOSE` và `WHITE`/`BLACK`, cùng danh sách `ArrayList<MatchMove>`), rồi:
- Gọi `new MatchDAO().saveMatch(match)` (nếu bạn tự chèn `tblMatchMove` trong cùng transaction hoặc bổ sung `MatchMoveDAO.insertMoves(connection, matchId, match.getMoves())`).
- **Rất quan trọng cho Bảng xếp hạng:** trong `tblMatchMove`, `moveIndex` phải đánh số từ `1` theo đúng lượt:
  - `moveIndex` **lẻ (1, 3, 5...)** = nước do quân **TRẮNG** đi.
  - `moveIndex` **chẵn (2, 4, 6...)** = nước do quân **ĐEN** đi.
  *(Vì `PlayerDAO.getRanking()` của N04 dựa vào `moveIndex % 2` và màu quân trong `tblMatchResult` để tính trung bình số nước đi của chính người chơi trong các trận thắng / trận thua.)*
