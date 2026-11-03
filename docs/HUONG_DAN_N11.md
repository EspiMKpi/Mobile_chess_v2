# HƯỚNG DẪN CODE PHẦN N11 (Nguyễn Tiến Dũng) – KHỚP VỚI CODE CỦA N04

> Phạm vi của bạn: **CN1** Thực hiện nước đi & đồng bộ bàn cờ · **CN2** Kết thúc ván theo luật & hết 30 giây · **CN3** Chơi với máy (AI).
> N04 (Đại Dũng) làm tầng mạng, tài khoản, danh sách online, thách đấu, vòng đời phòng đấu, BXH – xem `docs/BLUEPRINT_N04.md`.
> Mã nguồn các lớp nối giữa 2 người: `docs/HOP_DONG_CHUNG.md` (N04 tạo khung + model ở bước M0; bạn điền phần `TODO(N11)`).
> Nguyên tắc: **mỗi file chỉ một chủ** (`.github/CODEOWNERS`). Bạn chỉ sửa file của mình; muốn đổi bất cứ thứ gì ở mục 2 thì báo N04 và mở PR riêng.

---

## 1. File của bạn

| File | Trạng thái | Việc |
|---|---|---|
| `src/server/control/ChessReferee.java` | khung (HOP_DONG_CHUNG), chữ ký cố định | trọng tài: luật, lượt, phát nước đi, chiếu bí/hết nước, đồng hồ |
| `src/server/control/MoveTimeoutTask.java` | khung (HOP_DONG_CHUNG) | báo hết 30s |
| `src/server/dao/MatchMoveDAO.java` | khung (HOP_DONG_CHUNG), chữ ký cố định | ghi tblMatchMove |
| `src/client/view/ChessBoardPanel.java` | khung (HOP_DONG_CHUNG), chữ ký cố định | bàn cờ online nhúng trong MatchPlayFrm |
| `src/client/view/PromotionDialog.java` | tạo mới | chọn quân phong cấp |
| `src/client/view/ai/AISetupDialog.java` | khung (HOP_DONG_CHUNG), constructor cố định | thiết lập ván với máy (hình 3.1 báo cáo N11) |
| `src/client/view/ai/AIMatchFrm.java`, `AIThinkTank.java` | tạo mới | ván với máy (hình 3.2), `AIThinkTank extends SwingWorker` |
| `src/model/MatchMove.java`, `MoveRequest.java`, `MoveInfo.java`, `GameResult.java` | theo HOP_DONG_CHUNG | **đóng băng** – chỉ thêm getter/tiện ích, không đổi field/constructor |
| `db/02_match_move.sql` | đã có trong repo | |
| `src/com/chess/engine/**` (luật cờ + AI) | tạo mới | Board, Piece + 6 quân, Move/MoveFactory, MoveTransition, Player (White/Black), FenUtilities, MiniMax, MoveSorter, StandardBoardEvaluator, PieceSquareTable – theo báo cáo N11. Tên hàm dùng trong mục 3–4 là **yêu cầu tối thiểu** engine phải có |

**Không** sửa: `GameRoom`, `ServerCtr` (gồm `ServerProcessing`), `MatchDAO`, `ClientCtr`, `MatchPlayFrm`, `ContinueDialog`, `ObjectWrapper`, model của N04.

Nhánh git: `feature/n11-gameplay-ai`.

---

## 2. Hợp đồng đã chốt (đóng băng)

### 2.1 Mã lệnh bạn dùng (`model.ObjectWrapper`)
| Mã | Tên | Chiều | `data` | Ai gửi / ai nhận |
|---|---|---|---|---|
| 30 | `SEND_MOVE` | client → server | `MoveRequest(fromSquare, toSquare, promotion)` | ChessBoardPanel gửi qua `myControl.sendData(...)`; ServerProcessing (N04) chuyển vào `room.handleMove` → `referee.handleMove` |
| 31 | `REPLY_MOVE_ILLEGAL` | server → **người gửi** | `String` lý do | ChessReferee gửi; MatchPlayFrm (N04) nhận rồi gọi `boardPanel.onServerMessage(d)` |
| 32 | `SERVER_INFORM_MOVE` | server → **cả 2** | `MoveInfo(notation, fen, sideToMove, inCheck)` | như trên |
| 21 | `SERVER_END_GAME` | server → mỗi bên | `GameResult` | **N04 gửi** trong `GameRoom.endGame` – bạn **không** tự gửi mã này |

- Ô cờ: `0 = a8 … 63 = h1` (giống `BoardUtils`). `promotion`: `"Q" | "R" | "B" | "N" | null` (khớp ký hiệu `Piece.PieceType`).
- Màu: chuỗi `"WHITE"` / `"BLACK"` (= `Alliance.toString()` và `MatchResult.WHITE/BLACK`).
- Lý do kết thúc của bạn: `GameResult.CHECKMATE`, `GameResult.STALEMATE`, `GameResult.TIMEOUT`. (`RESIGN`, `DISCONNECT` là của N04.)

### 2.2 Những gì N04 cung cấp cho bạn gọi (server)
| Gọi | Ý nghĩa |
|---|---|
| `room.getWhitePlayer()` / `room.getBlackPlayer()` | `ServerCtr.ServerProcessing` đang cầm Trắng / Đen ở ván hiện tại (đã tính đổi màu) |
| `room.isPlaying()` | true từ lúc ván bắt đầu đến khi `endGame` chạy |
| `room.endGame(ServerProcessing winner, String reason)` | kết thúc + lưu + gửi `SERVER_END_GAME`. `winner = null` khi hòa. **Idempotent** – gọi thừa không sao |
| `sp.sendData(Object)` | gửi gói tin tới 1 client; `synchronized`, an toàn khi gọi từ luồng Timer |
| `sp.getLoggedInPlayer()` | Player của luồng đó (nếu cần hiển thị) |

### 2.3 Những gì N04 sẽ gọi vào code của bạn
| N04 gọi | Khi nào | Luồng | Bạn phải làm |
|---|---|---|---|
| `new ChessReferee(room)` | tạo phòng | – | chỉ lưu `room` |
| `referee.reset()` | **mỗi ván mới** (ván 1 và sau mỗi lần cả 2 Tiếp tục, màu đã đổi xong) | luồng client, **đang giữ khóa room** | `board = Board.createStandardBoard(); moves.clear(); startMoveTimer();` |
| `referee.handleMove(sp, req)` | nhận SEND_MOVE | luồng ServerProcessing của người gửi | thuật toán mục 3.1 |
| `referee.stopTimer()` | đầu `endGame`, trong `closeRoom` | đang giữ khóa room | hủy Timer (đã viết sẵn) |
| `referee.getMoves()` | trong `endGame` trước khi lưu | đang giữ khóa room | trả bản sao biên bản (đã viết sẵn) |
| `new MatchMoveDAO().insertMoves(con, matchId, moves)` | trong transaction `MatchDAO.saveMatch` | – | INSERT, **không** commit/rollback/close |
| `new ChessBoardPanel(myControl)` | mở MatchPlayFrm | EDT | dựng giao diện |
| `boardPanel.reset(myColor)` | SERVER_START_MATCH và SERVER_RESTART_MATCH_SWAP_COLOR | EDT | mục 4.1 |
| `boardPanel.onServerMessage(d)` | nhận mã 31, 32 | EDT | mục 4.1 |
| `boardPanel.setLocked(true)` | nhận SERVER_END_GAME | EDT | khóa bàn **và dừng đồng hồ** |
| `new AISetupDialog(onlineListFrm).setVisible(true)` | bấm [Chơi với máy] | EDT | mục 5 |

### 2.4 Luồng & khóa (quan trọng – tránh deadlock)
- Có 3 loại luồng chạm vào phòng đấu: luồng ServerProcessing của 2 người chơi và luồng `java.util.Timer` của bạn.
- **Chỉ dùng một khóa: chính đối tượng `room`.** N04 khai báo các method của `GameRoom` là `synchronized`; bạn bọc mọi đoạn đọc/ghi `board`, `moves`, `moveTimer` trong `synchronized (room) { ... }` (khung trong HOP_DONG_CHUNG đã làm mẫu). **Không** dùng `synchronized` method trên ChessReferee, không tạo lock riêng.
- Trong khối `synchronized (room)` được phép gọi `room.endGame(...)` (khóa reentrant).
- Đầu `handleMove` và `onMoveTimeout`: `if (!room.isPlaying()) return;` – ván có thể vừa kết thúc do bên kia xin thua/mất kết nối.
- Phía client: ClientListening (N04) chuyển mọi gói tin lên **EDT** trước khi gọi MatchPlayFrm, nên `onServerMessage` luôn chạy trên EDT – không cần `invokeLater` nữa. Đồng hồ client dùng `javax.swing.Timer`.

---

## 3. Server – `ChessReferee`

### 3.1 `handleMove(sender, req)`
```
synchronized (room) {
  if (!room.isPlaying()) return;
  String senderColor = (sender == room.getWhitePlayer()) ? "WHITE" : "BLACK";
  if (!board.currentPlayer().getAlliance().toString().equals(senderColor))
      -> sender.sendData(new ObjectWrapper(REPLY_MOVE_ILLEGAL, "Chưa tới lượt của bạn")); return;

  Move move = (req.getPromotion() == null)
      ? MoveFactory.createMove(board, from, to)
      : MoveFactory.createPromotionMove(board, from, to, toPieceType(req.getPromotion()));
  // LƯU Ý: createMove duyệt board.getAllLegalMoves() (cả 2 bên) -> bắt buộc kiểm tra lượt ở trên
  // Tốt đi tới hàng cuối mà promotion == null -> coi là ILLEGAL (client phải hỏi PromotionDialog)

  MoveTransition t = board.currentPlayer().makeMove(move);
  switch (t.getMoveStatus()):
     ILLEGAL_MOVE           -> REPLY_MOVE_ILLEGAL "Nước đi không hợp lệ"; return;
     LEAVES_PLAYER_IN_CHECK -> REPLY_MOVE_ILLEGAL "Nước đi khiến Vua bị chiếu"; return;
     DONE:
        String notation = move.toString();          // Move.toString() trả ký hiệu đại số (e4, Nf3, O-O, exd5, e8=Q)
        board = t.getToBoard();
        moves.add(new MatchMove(moves.size() + 1, from, to, notation, req.getPromotion()));
        startMoveTimer();                            // hủy hẹn giờ cũ, hẹn 30s cho bên tới lượt
        MoveInfo info = new MoveInfo(notation, FenUtilities.createFENFromGame(board),
                                     board.currentPlayer().getAlliance().toString(),
                                     board.currentPlayer().isInCheck());
        room.getWhitePlayer().sendData(new ObjectWrapper(SERVER_INFORM_MOVE, info));
        room.getBlackPlayer().sendData(new ObjectWrapper(SERVER_INFORM_MOVE, info));
        checkGameOver(sender);
}
```
Lưu ý `move.toString()` phải lấy **trước** khi thay `board` (ký hiệu dựa vào bàn cũ).

### 3.2 `checkGameOver(ServerProcessing justMoved)`
```
Player p = board.currentPlayer();               // bên vừa tới lượt
if (p.isInCheckMate())  room.endGame(justMoved, GameResult.CHECKMATE);
else if (p.isInStaleMate()) room.endGame(null, GameResult.STALEMATE);
```
`endGame` sẽ tự gọi `stopTimer()` và `getMoves()` – bạn không gửi gì thêm.
Gửi `SERVER_INFORM_MOVE` **trước** `checkGameOver` để 2 client thấy nước chiếu bí rồi mới nhận kết quả.

### 3.3 Đồng hồ 30 giây
```
startMoveTimer(): synchronized (room) { stopTimer(); moveTimer = new Timer(true);
                   moveTimer.schedule(new MoveTimeoutTask(this), MOVE_TIME_LIMIT_MS); }
onMoveTimeout():  synchronized (room) { if (!room.isPlaying()) return;
                   ServerProcessing loser = isWhiteToMove ? room.getWhitePlayer() : room.getBlackPlayer();
                   room.endGame(opponentOf(loser), GameResult.TIMEOUT); }
```
Gọi `moveTimer.cancel()` từ bên trong TimerTask là hợp lệ.

---

## 4. Client – `ChessBoardPanel` (+ `PromotionDialog`)

Bố cục gợi ý (nằm ở CENTER của MatchPlayFrm; N04 lo header đối thủ/màu quân và nút Thoát ván):
```
[ Đồng hồ: 30s | "Chiếu!" ]                    ← nhãn đồng hồ thuộc panel của bạn
[ Bàn cờ 8x8 (BoardPanel + 64 TilePanel, ảnh quân cờ) ][ Biên bản ]
```

### 4.1 Hành vi
| Sự kiện | Xử lý |
|---|---|
| `reset(myColor)` | `board = Board.createStandardBoard()`; xóa biên bản; xoay bàn nếu `"BLACK"`; đồng hồ về 30 và chạy; `locked = !myColor.equals("WHITE")` |
| bấm quân của mình (khi không `locked`) | tô ô chọn, chấm các ô đích từ `piece.calculateLegalMoves(board)`; bấm lại/ô khác bỏ chọn |
| bấm ô đích | tốt tới hàng cuối → `PromotionDialog` trả `"Q"/"R"/"B"/"N"`; gửi `myControl.sendData(new ObjectWrapper(SEND_MOVE, new MoveRequest(from, to, promo)))`; tạm `locked = true` chờ server |
| `onServerMessage` mã 32 | `board = FenUtilities.createGameFromFEN(info.getFen())`; vẽ lại; thêm `notation` vào biên bản; đồng hồ về 30; `locked = !info.getSideToMove().equals(myColor)`; hiện "Chiếu!" nếu `info.isInCheck()` |
| `onServerMessage` mã 31 | thông báo lý do (hình 1.3 báo cáo N11), bỏ chọn, `locked = false` |
| `setLocked(true)` | khóa bàn, **dừng** `javax.swing.Timer` |

- **Không** đăng ký `myFunction` trong ClientCtr – MatchPlayFrm đã đăng ký mã 31/32 và chuyển tiếp cho bạn.
- Bàn cờ client chỉ để hiển thị và gợi ý; server mới là nơi quyết định.
- Đồng hồ client chỉ hiển thị; hết giờ do server phán (không gửi gì khi về 0).

---

## 5. Chơi với máy (offline, không qua server)
- `AISetupDialog(JFrame owner)` – **giữ đúng constructor này** (OnlineListFrm của N04 gọi). Modal; chọn Người/Máy cho mỗi bên, độ sâu 1–6 (mặc định Trắng = Người, Đen = Máy, độ sâu 3) → mở `AIMatchFrm`.
- `AIMatchFrm`: JFrame `DISPOSE_ON_CLOSE` (đóng ván máy không tắt cả client); có thể dùng chung lớp vẽ bàn cờ với `ChessBoardPanel`.
- `AIThinkTank extends SwingWorker<Move, String>` gọi `MiniMax` (`com.chess.engine.ai`) với `MoveSorter`, `StandardBoardEvaluator`, `PieceSquareTable` như báo cáo N11.
- Không lưu DB, không tính điểm.

---

## 6. `MatchMoveDAO.insertMoves(con, matchId, moves)`
```
PreparedStatement ps = con.prepareStatement(
  "INSERT INTO tblMatchMove(matchId, moveIndex, fromSquare, toSquare, notation, promotion) VALUES (?,?,?,?,?,?)");
for each m: set...; promotion null -> ps.setNull(6, Types.CHAR); ps.addBatch();
ps.executeBatch(); ps.close();
```
Ném `SQLException` ra ngoài để `MatchDAO.saveMatch` rollback. Danh sách rỗng → không làm gì.

---

## 7. Kiểm thử

**Làm được ngay (không cần phần N04):**
- AI: chạy AIMatchFrm qua `AISetupDialog` từ một `main` tạm; Người–Máy, Máy–Máy, độ sâu 1–6, giao diện không treo.
- Luật: viết test (một lớp `main` hoặc JUnit) cho các ca của `handleMove` (sai lượt, sai luật, tự để Vua bị chiếu, phong cấp, nhập thành, bắt tốt qua đường, chiếu bí "Fool's mate" 4 nước, hết nước) – gọi trực tiếp engine như thuật toán 3.1.

**Sau khi N04 xong mốc M4 (thách đấu + phòng đấu):**
1. 2 client vào ván: Trắng đi trước, Đen bị khóa.
2. Đi sai lượt/sai luật → chỉ người gửi nhận thông báo, bàn 2 bên không đổi.
3. Nước hợp lệ → 2 bàn giống nhau, biên bản giống nhau, đồng hồ reset.
4. Fool's mate → cả 2 nhận ContinueDialog lý do "Chiếu bí!", DB có 1 tblMatch + 2 tblMatchResult + 4 tblMatchMove.
5. Để hết 30s → bên tới lượt thua, lý do TIMEOUT.
6. Hết giờ trùng lúc bấm Thoát ván → chỉ lưu **1** ván.
7. Cả 2 Tiếp tục → màu đổi, bàn và đồng hồ reset, biên bản trống.
8. Phong cấp Hậu/Mã, nhập thành 2 phía, bắt tốt qua đường hiển thị đúng ở cả 2 client.

---

## 8. Nên sửa báo cáo N11 cho khớp code
1. `GameRoom.endGame(winner, reason)` – đúng như bạn viết; thêm `isPlaying()`.
2. `MatchDAO.addMatch()` → **`MatchDAO.saveMatch()`** (tên của N04) gọi `MatchMoveDAO.insertMoves()`.
3. Biểu đồ lớp CN1/CN2: phần bàn cờ/đồng hồ nằm ở **`ChessBoardPanel`** (nhúng trong MatchPlayFrm), phần luật/đồng hồ server nằm ở **`ChessReferee`** (GameRoom giữ tham chiếu `referee` và chỉ chuyển tiếp `handleMove`).
4. Tạo nước phong cấp bằng `MoveFactory.createPromotionMove()`.
5. Tên thuộc tính `GameRoom.whitePlayer / blackPlayer` (thay cho `white / black`).
6. Mã lệnh: `SEND_MOVE = 30`, `REPLY_MOVE_ILLEGAL = 31`, `SERVER_INFORM_MOVE = 32`.
