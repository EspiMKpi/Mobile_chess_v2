# HƯỚNG DẪN CODE PHẦN N11 (Nguyễn Tiến Dũng) – KHỚP VỚI CODE CỦA N04

> Hệ thống: **server Java chạy trên PC (kèm MySQL)** + **client là ứng dụng Android viết bằng Java** (client chỉ chạy trên điện thoại / máy ảo Android).
> Phạm vi của bạn: **CN1** Thực hiện nước đi & đồng bộ bàn cờ · **CN2** Kết thúc ván theo luật & hết 30 giây · **CN3** Chơi với máy (AI) · thư viện luật cờ.
> N04 (Đại Dũng) làm tầng mạng, tài khoản, danh sách online, thách đấu, vòng đời phòng đấu, BXH – xem `docs/BLUEPRINT_N04.md`.
> Mã nguồn các lớp nối, cấu trúc Gradle, manifest: `docs/HOP_DONG_CHUNG.md` (N04 tạo khung ở bước M0; bạn điền phần `TODO(N11)`).
> Ghi chú sửa báo cáo N11 cho đúng nền tảng Android: `docs/GHI_CHU_SUA_BAO_CAO_N11.md`.
> Nguyên tắc: **mỗi file chỉ một chủ** (`.github/CODEOWNERS`). Bạn chỉ sửa file của mình; muốn đổi bất cứ thứ gì ở mục 2 thì báo N04 và mở PR riêng.

---

## 1. File của bạn

| File | Trạng thái | Việc |
|---|---|---|
| `common/src/main/java/com/chess/engine/**` | tạo mới | Board, Piece + 6 quân, Move/MoveFactory, MoveTransition, Player (White/Black), FenUtilities, MiniMax, MoveSorter, StandardBoardEvaluator, PieceSquareTable – theo báo cáo N11. **Java thuần** (không AWT/Swing/android.*) vì dùng chung cho server và app. Tên hàm ở mục 3–4 là yêu cầu tối thiểu |
| `common/src/main/java/model/MatchMove.java`, `MoveRequest.java`, `MoveInfo.java`, `GameResult.java` | theo HOP_DONG_CHUNG | **đóng băng** – chỉ thêm getter/tiện ích |
| `server/src/main/java/server/control/ChessReferee.java` | khung, chữ ký cố định | trọng tài: luật, lượt, phát nước đi, chiếu bí/hết nước, đồng hồ |
| `server/src/main/java/server/control/MoveTimeoutTask.java` | khung | báo hết 30s |
| `server/src/main/java/server/dao/MatchMoveDAO.java` | khung, chữ ký cố định | ghi tblMatchMove |
| `app/src/main/java/client/view/ChessBoardView.java` | khung, chữ ký cố định | View bàn cờ online nhúng trong MatchPlayActivity |
| `app/src/main/java/client/view/BoardCanvasView.java` | tạo mới | View vẽ 8x8 + xử lý chạm – dùng chung cho ván online và ván với máy |
| `app/src/main/java/client/view/PromotionDialog.java` | tạo mới | chọn quân phong cấp |
| `app/src/main/java/client/view/ai/AISetupDialog.java` | khung, `open(AppCompatActivity)` cố định | thiết lập ván với máy (Giao diện 3.1) |
| `app/src/main/java/client/view/ai/AIMatchActivity.java`, `AIThinkTank.java` | tạo mới | ván với máy (Giao diện 3.2) |
| `app/src/main/res/layout/n11_*.xml`, `res/drawable/n11_*`, `res/values/strings_n11.xml` | tạo mới | tài nguyên riêng của bạn (tiền tố `n11_`) |
| `db/02_match_move.sql` | đã có trong repo | |

**Không** sửa: `GameRoom`, `ServerCtr` (gồm `ServerProcessing`), `MatchDAO`, `ClientCtr`, `MatchPlayActivity`, `ContinueDialog`, `OnlineListActivity`, `AndroidManifest.xml` (đã khai báo sẵn `client.view.ai.AIMatchActivity`), `ObjectWrapper`, model của N04, tài nguyên không có tiền tố `n11_`.

Nhánh git: `feature/n11-gameplay-ai`.

---

## 2. Hợp đồng đã chốt (đóng băng)

### 2.1 Mã lệnh bạn dùng (`model.ObjectWrapper`)
| Mã | Tên | Chiều | `data` | Ai gửi / ai nhận |
|---|---|---|---|---|
| 30 | `SEND_MOVE` | app → server | `MoveRequest(fromSquare, toSquare, promotion)` | ChessBoardView gửi qua `ClientCtr.getInstance().sendData(...)`; ServerProcessing (N04) chuyển vào `room.handleMove` → `referee.handleMove` |
| 31 | `REPLY_MOVE_ILLEGAL` | server → **người gửi** | `String` lý do | ChessReferee gửi; MatchPlayActivity (N04) nhận rồi gọi `boardView.onServerMessage(d)` |
| 32 | `SERVER_INFORM_MOVE` | server → **cả 2** | `MoveInfo(notation, fen, sideToMove, inCheck)` | như trên |
| 21 | `SERVER_END_GAME` | server → mỗi bên | `GameResult` | **N04 gửi** trong `GameRoom.endGame` – bạn **không** tự gửi mã này |

- Ô cờ: `0 = a8 … 63 = h1`. `promotion`: `"Q" | "R" | "B" | "N" | null`.
- Màu: chuỗi `"WHITE"` / `"BLACK"` (= `MatchResult.WHITE/BLACK`).
- Lý do kết thúc của bạn: `GameResult.CHECKMATE`, `GameResult.STALEMATE`, `GameResult.TIMEOUT`. (`RESIGN`, `DISCONNECT` là của N04.)

### 2.2 Những gì N04 cung cấp cho bạn gọi
| Gọi | Ý nghĩa |
|---|---|
| `room.getWhitePlayer()` / `room.getBlackPlayer()` | `ServerCtr.ServerProcessing` đang cầm Trắng / Đen ở ván hiện tại (đã tính đổi màu) |
| `room.isPlaying()` | true từ lúc ván bắt đầu đến khi `endGame` chạy |
| `room.endGame(ServerProcessing winner, String reason)` | kết thúc + lưu + gửi `SERVER_END_GAME`. `winner = null` khi hòa. **Idempotent** |
| `sp.sendData(Object)` | gửi gói tin tới 1 app; `synchronized`, an toàn khi gọi từ luồng Timer |
| `ClientCtr.getInstance().sendData(Object)` (app) | gọi được từ main thread – ClientCtr tự đẩy việc ghi Socket sang luồng nền |

### 2.3 Những gì N04 sẽ gọi vào code của bạn
| N04 gọi | Khi nào | Luồng | Bạn phải làm |
|---|---|---|---|
| `new ChessReferee(room)` | tạo phòng | – | chỉ lưu `room` |
| `referee.reset()` | **mỗi ván mới** (ván 1 và sau mỗi lần cả 2 Tiếp tục, màu đã đổi xong) | luồng client, **đang giữ khóa room** | `board = Board.createStandardBoard(); moves.clear(); startMoveTimer();` |
| `referee.handleMove(sp, req)` | nhận SEND_MOVE | luồng ServerProcessing của người gửi | thuật toán mục 3.1 |
| `referee.stopTimer()` | đầu `endGame`, trong `closeRoom` | đang giữ khóa room | hủy Timer (đã có trong khung) |
| `referee.getMoves()` | trong `endGame` trước khi lưu | đang giữ khóa room | trả bản sao biên bản (đã có trong khung) |
| `new MatchMoveDAO().insertMoves(con, matchId, moves)` | trong transaction `MatchDAO.saveMatch` | – | INSERT, **không** commit/rollback/close |
| `<client.view.ChessBoardView>` trong `activity_match_play.xml` | MatchPlayActivity `setContentView` | main thread | constructor `(Context, AttributeSet)` inflate `n11_view_chess_board.xml` |
| `boardView.reset(myColor)` | SERVER_START_MATCH và SERVER_RESTART_MATCH_SWAP_COLOR | main thread | mục 4.1 |
| `boardView.onServerMessage(d)` | nhận mã 31, 32 | main thread | mục 4.1 |
| `boardView.setLocked(true)` | nhận SERVER_END_GAME | main thread | khóa bàn **và dừng đồng hồ** |
| `AISetupDialog.open(this)` | bấm [Chơi với máy] ở OnlineListActivity | main thread | mục 5 |

### 2.4 Luồng & khóa
**Server** (3 loại luồng chạm vào phòng đấu: ServerProcessing của 2 người + `java.util.Timer` của bạn):
- **Chỉ dùng một khóa: chính đối tượng `room`.** GameRoom dùng method `synchronized`; bạn bọc mọi đoạn đọc/ghi `board`, `moves`, `moveTimer` trong `synchronized (room) { ... }` (khung đã làm mẫu). Không dùng `synchronized` method trên ChessReferee, không tạo lock riêng.
- Trong khối `synchronized (room)` được phép gọi `room.endGame(...)` (khóa reentrant).
- Đầu `handleMove` và `onMoveTimeout`: `if (!room.isPlaying()) return;`.

**Android**:
- ClientListening (N04) đã chuyển gói tin lên **main thread** trước khi gọi MatchPlayActivity ⇒ `reset/onServerMessage/setLocked` luôn chạy trên main thread, cập nhật View trực tiếp được.
- Không I/O mạng trên main thread – bạn chỉ gọi `ClientCtr.sendData`, không tự mở Socket.
- Đồng hồ: `android.os.CountDownTimer` (hoặc `Handler.postDelayed`), **hủy** trong `setLocked(true)` và `onDetachedFromWindow()` (khung đã gọi) để không rò rỉ khi Activity đóng.
- AI: tính trên luồng nền (ExecutorService), trả kết quả về main thread bằng `Handler(Looper.getMainLooper())`; **không** dùng `AsyncTask` (đã bị loại bỏ) hay SwingWorker (không có trên Android).

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
  // Nếu createMove tìm trong nước đi của cả 2 bên thì bắt buộc kiểm tra lượt như trên.
  // Tốt tới hàng cuối mà promotion == null -> coi là ILLEGAL (app phải hỏi PromotionDialog)

  MoveTransition t = board.currentPlayer().makeMove(move);
  switch (t.getMoveStatus()):
     ILLEGAL_MOVE           -> REPLY_MOVE_ILLEGAL "Nước đi không hợp lệ"; return;
     LEAVES_PLAYER_IN_CHECK -> REPLY_MOVE_ILLEGAL "Nước đi khiến Vua bị chiếu"; return;
     DONE:
        String notation = move.toString();          // ký hiệu đại số (e4, Nf3, O-O, exd5, e8=Q) – lấy TRƯỚC khi thay board
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

### 3.2 `checkGameOver(ServerProcessing justMoved)`
```
Player p = board.currentPlayer();               // bên vừa tới lượt
if (p.isInCheckMate())      room.endGame(justMoved, GameResult.CHECKMATE);
else if (p.isInStaleMate()) room.endGame(null, GameResult.STALEMATE);
```
`endGame` tự gọi `stopTimer()` và `getMoves()` – bạn không gửi gì thêm. Gửi `SERVER_INFORM_MOVE` **trước** `checkGameOver` để 2 app thấy nước chiếu bí rồi mới nhận kết quả.

### 3.3 Đồng hồ 30 giây
```
startMoveTimer(): synchronized (room) { stopTimer(); moveTimer = new Timer(true);
                   moveTimer.schedule(new MoveTimeoutTask(this), MOVE_TIME_LIMIT_MS); }
onMoveTimeout():  synchronized (room) { if (!room.isPlaying()) return;
                   ServerProcessing loser = whiteToMove ? room.getWhitePlayer() : room.getBlackPlayer();
                   room.endGame(opponentOf(loser), GameResult.TIMEOUT); }
```
Server là nơi duy nhất phán hết giờ (đồng hồ trên điện thoại chỉ để hiển thị, có thể lệch vài trăm ms do mạng).

---

## 4. Android – `ChessBoardView` (+ `BoardCanvasView`, `PromotionDialog`)

`n11_view_chess_board.xml` (inflate vào `ChessBoardView`, dọc):
```
[ TextView n11_txtClock "30s" ]  [ TextView n11_txtCheck "Chiếu!" (ẩn) ]
[ BoardCanvasView – vuông, cạnh = chiều rộng màn hình ]
[ TextView/RecyclerView n11_moveLog – biên bản, cuộn ngang hoặc dọc ]
```
`BoardCanvasView extends View`: `onMeasure` ép vuông; `onDraw` vẽ 64 ô + quân (ảnh `res/drawable/n11_piece_wk.png`… chuyển từ bộ ảnh cũ trong `art/`, hoặc vẽ ký tự Unicode ♔♕♖♗♘♙ bằng `Paint`); `onTouchEvent(ACTION_UP)` đổi tọa độ chạm → chỉ số ô 0–63 (đảo khi cầm Đen); có `setBoard(Board)`, `setFlipped(boolean)`, `setHighlights(...)`, listener `onSquareTapped(int)`.

### 4.1 Hành vi
| Sự kiện | Xử lý |
|---|---|
| `reset(myColor)` | `board = Board.createStandardBoard()`; xóa biên bản; lật bàn nếu `"BLACK"`; đồng hồ về 30 và chạy; `locked = !myColor.equals("WHITE")` |
| chạm quân của mình (khi không `locked`) | tô ô chọn, chấm các ô đích từ `piece.calculateLegalMoves(board)`; chạm lại/ô khác bỏ chọn |
| chạm ô đích | tốt tới hàng cuối → `PromotionDialog` trả `"Q"/"R"/"B"/"N"`; `ClientCtr.getInstance().sendData(new ObjectWrapper(SEND_MOVE, new MoveRequest(from, to, promo)))`; tạm `locked = true` chờ server |
| `onServerMessage` mã 32 | `board = FenUtilities.createGameFromFEN(info.getFen())`; vẽ lại (`invalidate()`); thêm `notation` vào biên bản; đồng hồ về 30; `locked = !info.getSideToMove().equals(myColor)`; hiện "Chiếu!" nếu `info.isInCheck()` |
| `onServerMessage` mã 31 | Toast/AlertDialog lý do (Giao diện 1.3), bỏ chọn, `locked = false` |
| `setLocked(true)` | khóa bàn, **hủy** CountDownTimer |

- `PromotionDialog`: `AlertDialog.Builder(getContext()).setItems({"Hậu","Xe","Tượng","Mã"})`, `setCancelable(false)`; trả kết quả qua callback `java.util.function.Consumer<String>` (minSdk 24).
- **Không** đăng ký `myFunction` – MatchPlayActivity đã đăng ký mã 31/32 và chuyển tiếp cho bạn.
- Bàn cờ trên điện thoại chỉ để hiển thị và gợi ý; server mới là nơi quyết định.
- Ô chạm tối thiểu ~40dp: trên màn hình hẹp (360dp) mỗi ô 45dp – đủ dùng, không cần zoom.

---

## 5. Chơi với máy (offline, không qua server)
- `AISetupDialog extends DialogFragment` – **giữ đúng `public static void open(AppCompatActivity host)`** (OnlineListActivity gọi). `onCreateDialog`: RadioGroup Người/Máy cho mỗi bên, Spinner/SeekBar độ sâu 1–6 (mặc định Trắng = Người, Đen = Máy, độ sâu 3), [Bắt đầu] → `startActivity(new Intent(requireContext(), AIMatchActivity.class).putExtra("whiteHuman", …).putExtra("blackHuman", …).putExtra("depth", …))`.
- `AIMatchActivity` (đã khai báo trong manifest): `n11_activity_ai_match.xml` gồm `BoardCanvasView`, biên bản, nhãn "Máy đang suy nghĩ…" + ProgressBar, nút [Ván mới] [Đi lại] [Thoát]. Nút Back = Thoát.
- `AIThinkTank`: `ExecutorService` 1 luồng chạy `MiniMax.execute(board, depth)` rồi `mainHandler.post(...)` thực hiện nước và vẽ lại; giữ `Future` để `cancel(true)` khi [Ván mới]/[Thoát]/`onDestroy`. Máy–Máy: sau mỗi nước tự gọi lượt kế tiếp (có `postDelayed` ~300ms để người xem kịp).
- Độ sâu 5–6 có thể mất nhiều giây trên điện thoại – luôn chạy nền, hiện ProgressBar, khóa bàn trong lúc máy nghĩ.
- Không lưu DB, không tính điểm, không cần mạng.

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
- Luật: JUnit trong `common/src/test/java` (thêm `testImplementation("junit:junit:4.13.2")` vào `common/build.gradle.kts` qua PR hợp đồng) – các ca của `handleMove`: sai lượt, sai luật, tự để Vua bị chiếu, phong cấp, nhập thành 2 phía, bắt tốt qua đường, chiếu bí "Fool's mate" 4 nước, hết nước; FEN → Board → FEN giữ nguyên. Chạy `gradlew :common:test`.
- AI: mở `AIMatchActivity` trực tiếp bằng một nút tạm/`adb shell am start` trên máy ảo; Người–Máy, Máy–Máy, độ sâu 1–6, giao diện không đơ, thoát giữa lúc máy nghĩ không crash.

**Sau khi N04 xong mốc M4 (thách đấu + phòng đấu), 2 máy ảo cùng trỏ `10.0.2.2`:**
1. Vào ván: Trắng đi trước, Đen bị khóa.
2. Đi sai lượt/sai luật → chỉ người gửi nhận thông báo, bàn 2 bên không đổi.
3. Nước hợp lệ → 2 bàn giống nhau, biên bản giống nhau, đồng hồ reset.
4. Fool's mate → cả 2 nhận ContinueDialog lý do "Chiếu bí!", DB có 1 tblMatch + 2 tblMatchResult + 4 tblMatchMove.
5. Để hết 30s → bên tới lượt thua, lý do TIMEOUT.
6. Hết giờ trùng lúc bấm Thoát ván → chỉ lưu **1** ván.
7. Cả 2 Tiếp tục → màu đổi (bàn lật lại), đồng hồ reset, biên bản trống.
8. Phong cấp Hậu/Mã, nhập thành 2 phía, bắt tốt qua đường hiển thị đúng ở cả 2 máy.
9. Đưa app ra nền (nút Home) rồi mở lại giữa ván → bàn cờ và đồng hồ vẫn đúng; vuốt tắt hẳn app → đối thủ thắng DISCONNECT (phần N04) và không crash.
10. Thử thêm 1 điện thoại thật qua Wi-Fi (IP LAN của PC).

---

## 8. Sửa báo cáo N11
Xem `docs/GHI_CHU_SUA_BAO_CAO_N11.md` (chuyển sang Android + khớp hợp đồng với N04).
