# GHI CHÚ SỬA BÁO CÁO N11 (Nguyễn Tiến Dũng)

> Đề bài môn *Phát triển ứng dụng cho các thiết bị di động*: **client phải là ứng dụng Android**; server vẫn là ứng dụng Java chạy trên PC (kèm MySQL).
> Báo cáo N11 hiện thiết kế phía client theo giao diện máy tính (Swing: `...Frm`, `SwingWorker`, "tương ứng Table/GameSetup"). Dưới đây là danh sách chỗ cần sửa, theo thứ tự mục trong báo cáo, kèm các chỗ cần khớp hợp đồng chung với N04 (`docs/HOP_DONG_CHUNG.md`).
> Báo cáo N04 đã được sửa theo cùng quy ước (Activity/DialogFragment, khung điện thoại dọc) – nên tham khảo `BAO_CAO_THIET_KE_GAME_CO_VUA.md` để hai báo cáo đồng nhất.

## 1. Quy ước đổi tên (áp dụng cho toàn bộ báo cáo, kể cả hình vẽ)

| Hiện tại | Đổi thành | Ghi chú |
|---|---|---|
| `MatchPlayFrm` | `MatchPlayActivity` (của N04) + **`ChessBoardView`** (của N11) | phần bàn cờ, chọn quân, đồng hồ, "Chiếu!", biên bản nằm trong `ChessBoardView` – một View đặt trong layout của MatchPlayActivity |
| `AIMatchFrm` | `AIMatchActivity` | |
| `AISetupDialog` | `AISetupDialog extends DialogFragment` | mở bằng `AISetupDialog.open(activity)` |
| `PromotionDialog` | `PromotionDialog` (AlertDialog 4 lựa chọn) | |
| `AIThinkTank` kế thừa `SwingWorker` | `AIThinkTank` dùng `ExecutorService` + `Handler(Looper.getMainLooper())` | Android không có Swing; `AsyncTask` đã bị loại bỏ |
| `OnlineListFrm` | `OnlineListActivity` | |
| Đồng hồ đếm ngược phía client | `CountDownTimer` | đồng hồ phía server vẫn là `java.util.Timer` + `MoveTimeoutTask` |
| "bấm" quân / ô | "chạm" quân / ô | `onTouchEvent` của `BoardCanvasView` |
| (mới) | `BoardCanvasView extends View` | vẽ bàn 8x8, dùng chung cho ván online và ván với máy |
| (mới) | `ChessReferee` (server) | phần luật/lượt/đồng hồ của GameRoom tách ra lớp này; `GameRoom` giữ `referee` và chỉ chuyển tiếp `handleMove` |

## 2. Phần I – Thiết kế chung

**I.1.a Tổng quan kiến trúc**
- Bỏ câu "Mã nguồn: grassisme/btl-java" – thư viện luật cờ được viết lại; ghi: "thư viện luật cờ `com.chess.engine` là Java thuần, đặt trong module dùng chung `:common`, được dùng bởi cả server (PC) và ứng dụng Android".
- "Phía Client: MatchPlayFrm giữ một bản sao Board…" → "Phía Client (ứng dụng Android): `ChessBoardView` trong `MatchPlayActivity` giữ một bản sao Board chỉ để hiển thị và gợi ý ô đích…".
- "Chế độ chơi với máy chạy hoàn toàn tại Client (AIMatchFrm…)" → "(`AIMatchActivity`, gói `com.chess.engine.ai`) – chạy trên điện thoại, không cần mạng".
- Thêm 1 gạch đầu dòng: mọi thao tác Socket trên Android chạy ở luồng nền; gói tin nhận được chuyển sang main thread trước khi cập nhật bàn cờ.
- Mã chỉ lệnh: ghi rõ số `SEND_MOVE = 30`, `REPLY_MOVE_ILLEGAL = 31`, `SERVER_INFORM_MOVE = 32`.

**I.1.b Chức năng 1**
- "người chơi bấm vào một quân của mình trên MatchPlayFrm" → "chạm vào một quân của mình trên bàn cờ (`ChessBoardView`)".
- "MoveFactory.createMove()" → "`MoveFactory.createMove()`, hoặc `MoveFactory.createPromotionMove()` khi có quân phong cấp".
- "MatchPlayFrm.receivedDataProcessing()" → "`MatchPlayActivity.receivedDataProcessing()` chuyển gói tin cho `ChessBoardView.onServerMessage()`".
- "GameRoom.handleMove()" → "`GameRoom.handleMove()` chuyển cho `ChessReferee.handleMove()`".

**I.1.c Chức năng 2**
- `GameRoom.checkGameOver()`, `startMoveTimer()`, `onMoveTimeout()` → đặt trong `ChessReferee`.
- "gọi GameRoom.endGame(winner, reason) … MatchDAO.addMatch()" → "**`MatchDAO.saveMatch()`** (của N04), trong đó gọi `MatchMoveDAO.insertMoves()` để lưu tblMatchMove trong cùng giao dịch".
- "mỗi Client hiện lý do … rồi mở ContinueDialog" → giữ nguyên, ghi `ContinueDialog.newInstance(GameResult)`.

**I.1.d Chức năng 3**
- "Bấm [Bắt đầu] mở AIMatchFrm" → "mở `AIMatchActivity`".
- "AIMatchFrm khởi động AIThinkTank (SwingWorker) để tính trên luồng nền, giao diện hiện 'Máy đang suy nghĩ…' và không bị treo" → "`AIMatchActivity` giao `AIThinkTank` tính trên luồng nền (`ExecutorService`), kết quả gửi về main thread bằng `Handler`; màn hình hiện 'Máy đang suy nghĩ…' kèm ProgressBar và không bị đơ (tránh lỗi ANR của Android)".
- Thêm lưu ý: độ sâu 5–6 có thể mất vài giây trên điện thoại.

**I.2, I.3 (CSDL, lớp thực thể)** – không đổi.

## 3. Phần II – Thiết kế chi tiết

**II.1.a Giao diện** – vẽ lại theo khung điện thoại **dọc** (như hình 4.3 báo cáo N04):
- Giao diện 1.1 "Phòng thi đấu (MatchPlayFrm)" → "(MatchPlayActivity + ChessBoardView)": trên là thông tin đối thủ + đồng hồ, giữa là bàn cờ vuông rộng bằng màn hình, dưới là biên bản (cuộn) + thông tin của mình + nút [Thoát ván (xin thua)]. Bỏ bố cục "bàn cờ bên trái – biên bản bên phải".
- Giao diện 1.2 PromotionDialog, 1.3 thông báo → hộp thoại Android (nền mờ, nút đặt dưới).

**II.1.b Biểu đồ lớp CN1** (hình 2.1)
- Phân vùng Client: `MatchPlayActivity` (N04, chỉ chuyển tiếp mã 31/32) → `ChessBoardView` (thuộc tính `myColor`, `locked`, `board`; phương thức `reset(myColor)`, `onServerMessage(d)`, `setLocked(b)`) → `BoardCanvasView`; `PromotionDialog`; `ClientCtr` (`getInstance()`, `sendData()`).
- Phân vùng Server: thêm `ChessReferee` (`board`, `moves`, `moveTimer`; `reset()`, `handleMove(sp, req)`, `stopTimer()`, `getMoves()`); `GameRoom` có `referee: ChessReferee`, `whitePlayer`, `blackPlayer` (không dùng `white`/`black`).
- Phân vùng Model/engine: thêm `MoveFactory.createPromotionMove`; engine đặt trong `:common`.

**II.1.c Biểu đồ tuần tự CN1** (hình 2.2, 2.3)
- Đối tượng `MatchPlayFrm` → `ChessBoardView` (các bước chọn quân, gửi nước, vẽ lại) và `MatchPlayActivity` (bước nhận gói tin từ ClientListening rồi gọi `boardView.onServerMessage`).
- Thêm bước `ClientListening → Handler (main thread)` trước `receivedDataProcessing`.
- `GameRoom.handleMove → ChessReferee.handleMove`; các bước kiểm tra lượt/makeMove/gửi INFORM do `ChessReferee` thực hiện.

**II.2.a Giao diện CN2** – Giao diện 2.1, 2.2 vẽ lại theo khung điện thoại dọc; 2.2 là `ContinueDialog` của N04.

**II.2.b Biểu đồ lớp CN2** (hình 3.1) – `checkGameOver()`, `startMoveTimer()`, `onMoveTimeout()` chuyển từ `GameRoom` sang `ChessReferee`; `MoveTimeoutTask` tham chiếu `ChessReferee`; `MatchDAO.saveMatch` + `MatchMoveDAO.insertMoves`; client: `MatchPlayActivity` + `ChessBoardView` (đồng hồ `CountDownTimer`).

**II.2.c Biểu đồ tuần tự CN2** (hình 3.2, 3.3) – `GameRoom` → `ChessReferee` ở các bước kiểm tra chiếu bí/hẹn giờ; `addMatch` → `saveMatch`; phía client `MatchPlayFrm` → `MatchPlayActivity` gọi `boardView.setLocked(true)` rồi `ContinueDialog.newInstance(result).show()`.

**II.3.a Giao diện CN3** – Giao diện 3.1 AISetupDialog, 3.2 "Ván cờ với máy (AIMatchFrm)" → "(AIMatchActivity)", vẽ theo khung điện thoại dọc (bàn cờ trên, nút [Ván mới] [Đi lại] [Thoát] dưới).

**II.3.b Biểu đồ lớp CN3** (hình 4.1)
- Bỏ "(tương ứng GameSetup trong mã nguồn)", "(tương ứng Table)".
- `OnlineListFrm` → `OnlineListActivity`; `AISetupDialog extends DialogFragment` (`open(activity)`); `AIMatchActivity extends AppCompatActivity`; `AIThinkTank` (thuộc tính `ExecutorService`, `Handler`, `Future`; phương thức `think(board, depth)`, `cancel()`) thay cho "kế thừa SwingWorker"; `BoardCanvasView`.

**II.3.c Biểu đồ tuần tự CN3** (hình 4.2, 4.3) – `AIMatchFrm` → `AIMatchActivity`; bước `SwingWorker.execute()/done()` → `executor.submit(...)` / `mainHandler.post(...)`.

## 4. Sau khi sửa
- Dựng lại các hình (lớp, tuần tự, giao diện) theo tên mới rồi xuất lại PDF.
- Đối chiếu tên lớp/hàm với `docs/HOP_DONG_CHUNG.md` – code và báo cáo phải dùng cùng tên.
