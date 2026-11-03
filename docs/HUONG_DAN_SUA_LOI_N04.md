# Hướng dẫn sửa lỗi phần N04 (sau review commit `add4856`)

> Tài liệu dành cho agent/lập trình viên thực hiện sửa. Số dòng tính theo commit `add4856`
> (nhánh `feature/n04-account-lobby-challenge`) – nếu file đã đổi, tìm theo tên hàm.

## 0. Quy tắc bắt buộc trước khi sửa

1. **Nhánh**: tạo nhánh mới từ `feature/n04-account-lobby-challenge`, ví dụ `fix/n04-review`.
   Mỗi mục dưới đây = **một commit riêng** (tiêu đề dạng `fix(n04): ...`). **Không push** trừ khi được yêu cầu.
2. **Hợp đồng chung** (`docs/HOP_DONG_CHUNG.md`) – KHÔNG được vi phạm:
   - Không đổi/thêm mã lệnh trong `ObjectWrapper.java` (file dùng chung, cần cả 2 người review).
   - Không đổi **chữ ký public** đã có (được phép *thêm* phương thức/overload mới).
   - Khóa phía server: trạng thái phòng chỉ đồng bộ trên **một** khóa là đối tượng `GameRoom`.
   - `MatchMoveDAO.insertMoves(con, matchId, moves)` dùng chung Connection của `saveMatch`, không commit/rollback.
3. **Không sửa file của N11**: `ChessReferee.java`, `MoveTimeoutTask.java`, `MatchMoveDAO.java`,
   `ChessBoardView.java`, `client/view/ai/*`, `com/chess/**`, `strings_n11.xml`, `n11_*`.
4. Giữ phong cách hiện có: Java 17, comment tiếng Việt, chuỗi hiển thị tiếng Việt.
5. Sau **mỗi** mục: chạy `.\gradlew.bat build` (Windows) hoặc `./gradlew build` – phải `BUILD SUCCESSFUL`
   và `server/build/test-results/test/*.xml` không có `failures`/`errors`.
   Test hiện chạy **không cần MySQL** (log "Communications link failure" là bình thường) – giữ nguyên tính chất này.

Thứ tự đề xuất: 1 → 2 → 3 → 4 → 5 (bắt buộc), sau đó 6, 7, 9, 10. Mục 8 **phải hỏi chủ dự án trước**.

---

## 1. [Cao] Transaction lưu ván dùng chung Connection với mọi luồng

**File**: `server/src/main/java/server/dao/DAO.java`, `server/src/main/java/server/dao/MatchDAO.java`

**Vấn đề**: `DAO.con` là `static`, dùng chung cho mọi luồng `ServerProcessing`. `MatchDAO.saveMatch`
(dòng 24) có `synchronized` nhưng `GameRoom.endGame` (GameRoom.java:155) gọi `new MatchDAO().saveMatch(...)`
→ mỗi lần một instance khác → khóa vô tác dụng. Hai ván kết thúc cùng lúc sẽ:
- commit/rollback lẫn dữ liệu của nhau;
- luồng thứ hai đọc `oldAutoCommit = false` (do luồng 1 đang giữa transaction) rồi trả lại `false` ở `finally`
  → Connection dùng chung **kẹt ở autoCommit=false vĩnh viễn** → đăng ký/sửa thông tin/đổi mật khẩu sau đó không được lưu thật.
Ngoài ra mọi câu lệnh của luồng khác chạy trong lúc transaction mở sẽ bị cuốn vào transaction đó.

**Cách sửa**: `saveMatch` dùng **một Connection riêng** mở mới cho mỗi lần lưu, đóng sau khi xong.

1. `DAO.java` – tách phần đọc cấu hình + mở kết nối thành hàm trả về Connection mới:

```java
/** Mở MỘT kết nối mới, không dùng chung. Người gọi phải tự đóng. Trả null nếu lỗi. */
protected static Connection openNewConnection() {
    try {
        Properties props = loadDbProperties();   // tách vòng lặp possiblePaths hiện có ra hàm này
        String url = props.getProperty("url", "<giữ nguyên URL mặc định hiện tại>");
        String user = props.getProperty("user", "root");
        String pass = props.getProperty("password", "");
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, user, pass);
    } catch (Exception e) {
        System.err.println("DAO Database connection note: " + e.getMessage());
        return null;
    }
}

private static void connect() {
    con = openNewConnection();
}
```

   (Tùy chọn) Trong `getConnection()`, thay `con.isClosed()` bằng `!con.isValid(2)` để tự kết nối lại khi
   MySQL đã đóng kết nối cũ (wait_timeout).

2. `MatchDAO.saveMatch` – bỏ `synchronized`, bỏ biến `oldAutoCommit`, dùng try-with-resources:

```java
public boolean saveMatch(Match match) {
    if (match == null || match.getResults() == null || match.getResults().isEmpty()) {
        return false;
    }
    try (Connection connection = openNewConnection()) {   // null được try-with-resources bỏ qua
        if (connection == null) {
            return false;
        }
        connection.setAutoCommit(false);
        try {
            // ... GIỮ NGUYÊN bước 1 (tblMatch), 2 (tblMatchResult), 3 (insertMoves) hiện có,
            //     chỉ dùng biến `connection` này ...
            connection.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("MatchDAO.saveMatch error: " + e.getMessage());
            try { connection.rollback(); } catch (SQLException ignored) { }
            return false;
        }
    } catch (SQLException e) {
        System.err.println("MatchDAO.saveMatch close error: " + e.getMessage());
        return false;
    }
}
```

   Giữ nguyên `new MatchMoveDAO().insertMoves(connection, matchId, match.getMoves())` và khối try/catch quanh nó.
   Giữ nguyên nhánh `matchId == 0 → rollback, return false`.

**Kiểm tra**: build + test qua. Nếu có MySQL: chạy server, cho 2 cặp kết thúc ván gần như cùng lúc,
sau đó đăng ký tài khoản mới → khởi động lại server → tài khoản vẫn đăng nhập được.

---

## 2. [Cao] Thách đấu đồng thời tạo 2 phòng; người thách bị kẹt

**File**: `server/.../control/ServerCtr.java` (case `SEND_CHALLENGE`, dòng 298-310),
`server/.../control/GameRoom.java`, `app/.../view/ChallengeDialog.java`

**Vấn đề**:
- Kiểm tra "cả hai rảnh" và `createRoom` không nguyên tử. A và C cùng thách B (hoặc A↔B thách nhau cùng lúc)
  → cả hai luồng đều thấy rảnh → tạo 2 phòng, `B.room` bị ghi đè; phòng đầu mồ côi, người trong đó BUSY mãi.
- `ChallengeDialog` `setCancelable(false)` và không có nút → nếu đối thủ không trả lời, người thách kẹt vĩnh viễn.
- Gửi `SEND_CHALLENGE` khi chưa đăng nhập → NPE ở dòng 305 (`loggedInPlayer.getId()`). Có thể tự thách chính mình.

**Cách sửa**:

### 2a. Server – tạo phòng nguyên tử (`ServerCtr.java`)

```java
/** Khóa riêng cho ghép cặp: chỉ bao quanh kiểm tra trạng thái + createRoom. KHÔNG lấy khóa GameRoom bên trong. */
private final Object matchmakingLock = new Object();

/** Kiểm tra cả hai rảnh rồi tạo phòng trong cùng một khóa. Trả null nếu không ghép được. */
public GameRoom tryCreateRoom(ServerProcessing a, ServerProcessing b) {
    synchronized (matchmakingLock) {
        if (a == null || b == null || a == b) return null;
        if (a.getRoom() != null || b.getRoom() != null) return null;
        if (PlayerStat.BUSY.equals(a.getStatus()) || PlayerStat.BUSY.equals(b.getStatus())) return null;
        return createRoom(a, b);
    }
}
```

Thứ tự khóa sau khi sửa: `matchmakingLock → myRooms`. Không chỗ nào giữ `myRooms` hoặc khóa `GameRoom`
rồi mới lấy `matchmakingLock` → không deadlock. Giữ `createRoom` public (test đang dùng).

Trong `ServerProcessing`: đổi `status` và `room` thành `volatile` (đọc/ghi từ nhiều luồng).

Viết lại case `SEND_CHALLENGE`:

```java
case ObjectWrapper.SEND_CHALLENGE: {
    if (loggedInPlayer == null || !(w.getData() instanceof PlayerStat)) {
        break;
    }
    PlayerStat target = (PlayerStat) w.getData();
    ServerProcessing b = findProcess(target.getId());
    GameRoom newRoom = tryCreateRoom(this, b);          // KHÔNG đặt tên `room` (trùng field)
    if (newRoom == null) {
        sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_BUSY, target));
    } else {
        float pt = playerDAO.getTotalPoint(loggedInPlayer.getId());
        PlayerStat inviterStat = new PlayerStat(loggedInPlayer, pt, PlayerStat.BUSY);
        b.sendData(new ObjectWrapper(ObjectWrapper.SERVER_INFORM_CHALLENGE, inviterStat));
        broadcastOnlineList();   // để người khác thấy 2 người này đang bận
    }
    break;
}
```

### 2b. Cho phép người thách hủy lời mời (không thêm mã lệnh mới)

Tái sử dụng mã có sẵn: **người thách** gửi `REPLY_CHALLENGE_DECISION(false)` trong giai đoạn chờ = **hủy lời mời**.
Server xác nhận bằng `REPLY_CHALLENGE_REJECT` cho cả hai bên. Cách làm chi tiết ở mục 3 (`GameRoom.receiveChallengeDecision(sender, accept)`).

`ChallengeDialog.java`:
- Thêm field `private boolean cancelling;`.
- Trong `onCreateDialog`, thêm `.setNegativeButton(R.string.btn_cancel, null)` và gắn listener trong
  `setOnShowListener` (để dialog **không tự đóng**):
  ```java
  btnCancel.setOnClickListener(v -> {
      cancelling = true;
      btnCancel.setEnabled(false);
      dialog.setMessage("Đang hủy lời mời...");
      myControl.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_DECISION, false));
  });
  ```
- **Không dismiss ngay** – chờ server trả lời, vì đối thủ có thể đã bấm Đồng ý trước:
  - Nhận `REPLY_CHALLENGE_REJECT`: toast `cancelling ? "Đã hủy lời mời" : "Đối thủ đã từ chối"`, rồi dismiss.
  - Nhận `SERVER_START_MATCH`: vào ván như hiện tại (đối thủ đã đồng ý trước khi lệnh hủy tới).
- `InviteDialog` không cần sửa: nó đã xử lý `REPLY_CHALLENGE_REJECT` → "Người mời đã rời đi".

**Kiểm tra (thêm test vào `N04CoreTest`)**:
- `tryCreateRoom(spA, spA)` trả `null`.
- `tryCreateRoom(spA, spB)` khác null; gọi tiếp `tryCreateRoom(spC, spB)` (cần 3 socket) trả `null`, `getMyRooms().size() == 1`.

---

## 3. [Cao] Server tin trạng thái do client gửi (quyết định thách đấu / tiếp tục)

**File**: `server/.../control/GameRoom.java` (dòng 43, 168), `ServerCtr.java` (case `REPLY_CHALLENGE_DECISION`)

**Vấn đề**:
- `REPLY_CHALLENGE_DECISION` được nhận từ **bất kỳ** người nào trong phòng, ở **bất kỳ** lúc nào:
  `true` giữa ván → `startGame()` tạo ván mới, mất ván đang chơi; `false` giữa ván → `closeRoom()` không lưu kết quả,
  đối thủ kẹt trong `MatchPlayActivity`.
- `receiveContinueDecision` đếm **số lần gọi** (`continueCount++`), không đếm số người → một người gửi 2 lần là đủ.
  Không kiểm tra `playing` → gửi giữa ván cũng đổi màu/bỏ ván.
- Phòng đã `closeRoom()` vẫn có thể nhận lời gọi trễ.

**Cách sửa** (`GameRoom.java`):

```java
private final ServerCtr.ServerProcessing challenger;   // người thách (ván 1 cầm Đen)
private final ServerCtr.ServerProcessing challenged;   // người được thách (ván 1 cầm Trắng)
private final Set<ServerCtr.ServerProcessing> continueVotes = new HashSet<>();  // thay cho continueCount
private boolean closed;
```

- Constructor: gán thêm `challenger`, `challenged`.
- Xóa field `continueCount`; trong `beginNewMatch()` thay `continueCount = 0` bằng `continueVotes.clear()`.
- Đầu `closeRoom()`: `if (closed) return; closed = true;` (giữ phần còn lại).
- **Thêm overload** (giữ nguyên `receiveChallengeDecision(boolean)` cũ – test đang dùng – và cho nó gọi `receiveChallengeDecision(challenged, accept)`):

```java
/** Chỉ hợp lệ khi đang chờ trả lời (chưa có ván nào). */
public synchronized void receiveChallengeDecision(ServerCtr.ServerProcessing sender, boolean accept) {
    if (closed || currentMatch != null) return;          // đã vào ván / đã đóng -> bỏ qua
    if (sender == challenged) {
        if (accept) { startGame(); return; }
        // Từ chối: GIỮ NGUYÊN logic hiện tại (closeRoom + REPLY_CHALLENGE_REJECT cho challenger)
        ...
    } else if (sender == challenger && !accept) {
        // Người thách hủy lời mời: báo cả hai rồi đóng phòng
        challenged.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_REJECT, null));
        challenger.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_REJECT, null));
        closeRoom();
    }
    // các trường hợp khác: bỏ qua
}
```

  Lưu ý: logic từ chối hiện tại đọc `blackPlayer`/`whitePlayer` – trong giai đoạn chờ chúng trùng
  `challenger`/`challenged`, nên có thể đổi sang hai field mới cho rõ nghĩa.

- `receiveContinueDecision`:

```java
public synchronized void receiveContinueDecision(ServerCtr.ServerProcessing sp, boolean accept) {
    if (closed || playing || currentMatch == null) return;          // chỉ hợp lệ giữa 2 ván
    if (sp != whitePlayer && sp != blackPlayer) return;
    if (!accept) {
        // GIỮ NGUYÊN: gửi STOP / OPPONENT_STOP rồi closeRoom()
        ...
        return;
    }
    continueVotes.add(sp);
    if (continueVotes.size() == 2) {
        // GIỮ NGUYÊN: swapColor(); beginNewMatch(); gửi SERVER_RESTART_MATCH_SWAP_COLOR cho 2 bên
    }
}
```

- `ServerCtr.java`, case `REPLY_CHALLENGE_DECISION`: gọi `room.receiveChallengeDecision(this, (Boolean) w.getData())`.

**Kiểm tra (thêm test)**:
- Sau `endGame`, gọi `receiveContinueDecision(spA, true)` **hai lần** → `isPlaying()` vẫn `false`, màu chưa đổi.
- Đang chơi (`isPlaying()==true`), gọi `receiveContinueDecision(spA, false)` → phòng **không** đóng.
- Đang chơi, gọi `receiveChallengeDecision(spB, true)` → `getCurrentMatch()` vẫn là đối tượng cũ.
- `testGameRoomMatchFlowAndColorSwap` hiện có vẫn phải qua.

---

## 4. [Trung bình] Client báo "Mất kết nối" sai khi kết nối lại

**File**: `app/src/main/java/client/control/ClientCtr.java` (dòng 89-94 và lớp `ClientListening` dòng 199-229)

**Vấn đề**: Login → bấm Back về màn hình kết nối → bấm [Kết nối] lần nữa. `openConnection()` đặt
`closing = false` rồi đóng socket cũ → luồng `ClientListening` cũ ném IOException, thấy `!closing` →
toast "Mất kết nối server" và mở lại `ClientMainActivity` với `CLEAR_TASK`, đá người dùng khỏi màn Login vừa mở.
Ngoài ra luồng cũ đọc field `ois` dùng chung (có thể đã bị thay bằng stream mới). Khi mất mạng thật,
socket không được đóng nên `isConnected()` vẫn trả `true`.

**Cách sửa**:
- Đổi `mySocket`, `oos`, `ois` thành `volatile`.
- `ClientListening` giữ **socket và stream của riêng nó**:

```java
private class ClientListening extends Thread {
    private final Socket socket;
    private final ObjectInputStream in;

    ClientListening(Socket socket, ObjectInputStream in) {
        this.socket = socket;
        this.in = in;
    }

    @Override
    public void run() {
        try {
            while (!closing) {
                Object o = in.readObject();
                if (!(o instanceof ObjectWrapper)) continue;
                ObjectWrapper d = (ObjectWrapper) o;
                // ... GIỮ NGUYÊN vòng lặp phát tới myFunction (+ mục 6 nếu làm) ...
            }
        } catch (Exception e) {                       // gồm cả ClassCastException
            if (closing || socket != mySocket) {
                return;                               // đăng xuất, hoặc socket cũ đã được thay -> im lặng
            }
            try { socket.close(); } catch (IOException ignored) { }   // để isConnected() trả false
            mainHandler.post(() -> { /* GIỮ NGUYÊN: toast + mở ClientMainActivity */ });
        }
    }
}
```

- Trong `openConnection()`: `myListening = new ClientListening(mySocket, ois);`.

**Kiểm tra thủ công**: Kết nối → Login → Back → Kết nối lại: vào được Login, **không** có toast "Mất kết nối".
Tắt server khi đang ở danh sách online → có toast và quay về màn hình kết nối.

---

## 5. [Trung bình] Crash khi ván kết thúc lúc app đang ở nền

**File**: `app/.../view/MatchPlayActivity.java` (dòng 120-127), `ChallengeDialog.java`, `InviteDialog.java`,
`OnlineListActivity.java` (dòng 130-141)

**Vấn đề**: `ClientCtr` chuyển gói tin tới màn hình bất kể trạng thái vòng đời. Khi Activity đã qua
`onSaveInstanceState` (người dùng bấm Home), `ContinueDialog.show()` ném
`IllegalStateException: Can not perform this action after onSaveInstanceState` → crash.
Các dialog cũng có thể nhận gói tin đã được post **sau** khi bị gỡ khỏi Activity → `requireContext()` ném lỗi.

**Cách sửa**:

`MatchPlayActivity.java`:

```java
private GameResult pendingResult;   // kết quả nhận được lúc Activity đang ở nền

private void showContinueDialog(GameResult r) {
    FragmentManager fm = getSupportFragmentManager();
    if (fm.isStateSaved()) {        // đang ở nền -> để dành, hiện khi quay lại
        pendingResult = r;
        return;
    }
    if (fm.findFragmentByTag(ContinueDialog.TAG) == null) {
        ContinueDialog.newInstance(r).show(fm, ContinueDialog.TAG);
    }
}

@Override
protected void onPostResume() {     // FragmentManager đã sẵn sàng nhận transaction ở đây
    super.onPostResume();
    if (pendingResult != null && !isFinishing()) {
        GameResult r = pendingResult;
        pendingResult = null;
        showContinueDialog(r);
    }
}
```

- Trong case `SERVER_END_GAME`: thay lời gọi `.show(...)` bằng `showContinueDialog((GameResult) data.getData())`.
- Trong `swapColor(...)` và case `SERVER_END_SESSION_MATCH`: đặt `pendingResult = null`.

`ChallengeDialog.receivedDataProcessing` và `InviteDialog.receivedDataProcessing`: thêm dòng đầu
`if (!isAdded()) return;`.

`OnlineListActivity` case `SERVER_INFORM_CHALLENGE`: chỉ `show` khi
`cur instanceof AppCompatActivity && !((AppCompatActivity) cur).getSupportFragmentManager().isStateSaved()`,
ngược lại gửi từ chối như nhánh `else` hiện có.

**Kiểm tra thủ công**: 2 thiết bị/emulator vào ván; máy A bấm Home; máy B xin thua → A không crash;
A mở lại app → thấy `ContinueDialog`.

---

## 6. [Thấp] Có thể mất nước đi đầu tiên khi vừa vào ván

**File**: `app/.../control/ClientCtr.java`, `app/.../view/MatchPlayActivity.java`

**Vấn đề**: `MatchPlayActivity` chỉ đăng ký mã 31/32 trong `onCreate`. Giữa lúc dialog nhận `SERVER_START_MATCH`
và lúc Activity mới được tạo, nếu bên Trắng đã đi, gói `SERVER_INFORM_MOVE` không có ai nhận và bị bỏ → bàn cờ lệch.

**Cách sửa** (không sửa file của N11):
- `ClientCtr`: thêm `private final List<ObjectWrapper> pendingBoardMessages = new ArrayList<>();`
- Trong `ClientListening`, sau vòng lặp phát:
  - nếu `d` là `SERVER_START_MATCH` → `synchronized (pendingBoardMessages) { pendingBoardMessages.clear(); }`
    (làm **trước** vòng lặp phát);
  - nếu không receiver nào khớp **và** mã là `SERVER_INFORM_MOVE` hoặc `REPLY_MOVE_ILLEGAL` → thêm vào `pendingBoardMessages`.
- Thêm phương thức public mới:

```java
/** MatchPlayActivity gọi cuối onCreate (main thread) để nhận các gói nước đi đến sớm. */
public void flushPendingBoardMessages(DataReceiver r) {
    List<ObjectWrapper> copy;
    synchronized (pendingBoardMessages) {
        copy = new ArrayList<>(pendingBoardMessages);
        pendingBoardMessages.clear();
    }
    for (ObjectWrapper d : copy) r.receivedDataProcessing(d);
}
```

- `closeConnection()`: xóa luôn `pendingBoardMessages`.
- `MatchPlayActivity.onCreate`: sau `boardView.reset(myColor);` gọi `myControl.flushPendingBoardMessages(this);`.

---

## 7. [Thấp] Một gói tin lỗi làm rớt kết nối client; đăng nhập trùng không nguyên tử

**File**: `server/.../control/ServerCtr.java` (`ServerProcessing.run`, `handleMessage`)

**Vấn đề**: `run()` chỉ bắt `IOException | ClassNotFoundException`; mọi `RuntimeException` trong `handleMessage`
(NPE, `ClassCastException` do ép kiểu không kiểm tra, `ArrayIndexOutOfBounds` ở `CHANGE_PASSWORD` dòng 271-272)
thoát vòng lặp → client bị ngắt. Hai thiết bị đăng nhập cùng tài khoản cùng lúc đều qua được `findProcess`.

**Cách sửa**:
- Vòng lặp trong `run()`:

```java
while (true) {
    Object o = ois.readObject();
    if (!(o instanceof ObjectWrapper)) continue;
    try {
        handleMessage((ObjectWrapper) o);
    } catch (RuntimeException e) {
        System.err.println("ServerProcessing.handleMessage error: " + e);
        if (view != null) view.showMessage("Lỗi xử lý gói tin: " + e);
    }
}
```

- Thêm kiểm tra kiểu trước mỗi ép kiểu: `LOGIN_USER`, `REGISTER_USER`, `EDIT_PLAYER` cần `w.getData() instanceof Player`;
  `CHANGE_PASSWORD` cần `arr.length == 2 && arr[0] != null && arr[1] != null`.
- `GET_ONLINE_LIST` **giữ nguyên không yêu cầu đăng nhập** (test `testServerClientConnectionAndOnlineList` dựa vào điều này).
- `LOGIN_USER`: nếu `loggedInPlayer != null` (đã đăng nhập trên kết nối này) → trả `"logged_in"`.
  Bọc phần kiểm tra trùng + gán trong `synchronized (myProcess)` (khóa reentrant, `findProcess` cũng dùng khóa này);
  gọi `sendData`/`broadcastOnlineList()` **sau** khi ra khỏi khối synchronized:

```java
boolean duplicate;
synchronized (myProcess) {
    duplicate = findProcess(p.getId()) != null;
    if (!duplicate) {
        loggedInPlayer = p;
        status = PlayerStat.IDLE;
    }
}
```

---

## 8. [Thấp – CẦN HỎI CHỦ DỰ ÁN TRƯỚC] Kẹt "logged_in" sau khi điện thoại mất mạng đột ngột

**File**: `ServerCtr.java` (case `LOGIN_USER`, dòng 227)

**Vấn đề**: Điện thoại mất mạng mà không đóng socket → server có thể nhiều giờ mới phát hiện (TCP keepalive mặc định
~2 giờ). Trong thời gian đó đăng nhập lại bị báo "Tài khoản đang đăng nhập ở thiết bị khác".

**Phương án** (thay đổi hành vi so với báo cáo thiết kế → **không tự làm**, hỏi trước):
- A (đề xuất): đăng nhập mới thắng – khi phát hiện trùng: `removeProcess(old)`, đóng `old.getMySocket()`
  (luồng cũ tự chạy `onDisconnect` → `room.playerLeft` xử lý ván đang chơi), rồi cho đăng nhập mới.
- B: giữ nguyên hành vi, chỉ ghi chú hạn chế trong báo cáo.

---

## 9. [Thấp] Rò rỉ luồng ở màn hình kết nối

**File**: `app/.../view/ClientMainActivity.java` (dòng 66)

**Vấn đề**: mỗi lần bấm [Kết nối] tạo `Executors.newSingleThreadExecutor()` mới, không bao giờ `shutdown()`
→ mỗi lần bấm để lại một luồng sống mãi.

**Cách sửa**:
- Field `private final ExecutorService connectExecutor = Executors.newSingleThreadExecutor();`, dùng thay cho dòng 66.
- `onDestroy()`: `connectExecutor.shutdownNow();` rồi `super.onDestroy()`.
- Trong `runOnUiThread(...)`: thêm `if (isFinishing() || isDestroyed()) return;` ở đầu.

---

## 10. [Thấp] Test xếp hạng không kiểm tra code thật; test dùng `sleep`

**File**: `server/.../dao/PlayerDAO.java` (dòng 343-380), `server/src/test/java/server/N04CoreTest.java`

**Vấn đề**: `testRankingSortingCriteria` (dòng 288-321) **chép lại** comparator thay vì dùng comparator của
`PlayerDAO` → sửa sai `PlayerDAO` test vẫn qua. Các test dùng `Thread.sleep(300)` → có thể lúc qua lúc không trên máy chậm.

**Cách sửa**:
- `PlayerDAO`: đưa comparator ra hằng số public, **giữ nguyên logic**:
  ```java
  /** Thứ tự BXH: totalPoint DESC -> avgOpponentPoint DESC -> avgMovesWin ASC (-1 cuối) -> avgMovesLose DESC (-1 cuối) -> id ASC. */
  public static final Comparator<PlayerRanking> RANKING_ORDER = (p1, p2) -> { /* chuyển nguyên thân compare hiện tại vào đây */ };
  ```
  `getRanking()` dùng `rankingList.sort(RANKING_ORDER);`.
- Test: xóa comparator chép lại, dùng `list.sort(PlayerDAO.RANKING_ORDER);`.
- Thay `Thread.sleep(...)` + assert bằng helper chờ có điều kiện:
  ```java
  private static void waitUntil(java.util.function.BooleanSupplier cond, long timeoutMs) throws InterruptedException {
      long end = System.currentTimeMillis() + timeoutMs;
      while (!cond.getAsBoolean()) {
          if (System.currentTimeMillis() > end) Assert.fail("Hết thời gian chờ điều kiện");
          Thread.sleep(20);
      }
  }
  // ví dụ: waitUntil(() -> server.getMyProcess().size() == 2, 3000);
  ```
- Xóa 2 dòng thừa ở `testGameRoomLifecycleAndChallengeDecision` (dòng 110-113: tạo `pA` và gọi `spA.getLoggedInPlayer()` không dùng tới).

---

## 11. Kiểm tra cuối cùng

1. `.\gradlew.bat build` → `BUILD SUCCESSFUL`.
2. `server/build/test-results/test/TEST-server.N04CoreTest.xml`: `failures="0" errors="0"`, có các test mới ở mục 2, 3.
3. Lint không phát sinh **error** mới (`app/build/reports/lint-results-debug.html`).
4. `git log --oneline` – mỗi mục một commit; `git status` sạch; **chưa push**.
5. Báo lại: danh sách commit, mục nào đã làm, mục nào bỏ qua (mục 8 nếu chưa được đồng ý), kết quả test.
