# BLUEPRINT PHẦN N04 (Nguyễn Đại Dũng) – CẤU TRÚC MỚI

> Cập nhật theo báo cáo `B23DCVT103 - Nguyễn Đại Dũng - Game Cờ vua.docx` và ranh giới phân chia mới:
> **N04 làm toàn bộ hạ tầng mạng + các chức năng trước khi vào trận (Đăng ký, Đăng nhập, Sửa thông tin, Đổi mật khẩu, Lịch sử đấu, Bảng xếp hạng, Danh sách online, Mời/Nhận/Hủy thách đấu).**
> Từ lúc vào trận đấu đối kháng trở đi (`GameRoom`, `MatchPlayActivity`, `ContinueDialog`, luật cờ, AI) do thành viên N11 đảm nhiệm thông qua hợp đồng `MatchSession` và `MatchStartInfo`.

## 1. Danh sách file thuộc phần N04

- **`:common` (`common/src/main/java/model/`):**
  - `ObjectWrapper.java`, `IPAddress.java`, `Player.java`, `PlayerStat.java`, `PlayerRanking.java`, `Match.java`, `MatchResult.java`, `MatchStartInfo.java`
- **`:server`:**
  - `ServerRun.java`, `view/ServerMainFrm.java`
  - `control/ServerCtr.java` (+ `ServerDiscovery` UDP, `ServerListening`, `ServerProcessing`), `control/PendingChallenge.java`, `control/MatchSession.java`
  - `dao/DAO.java`, `dao/PlayerDAO.java`, `dao/MatchDAO.java`
- **`:app`:**
  - `AndroidManifest.xml` (khởi chạy từ `LoginActivity`, không còn `ClientMainActivity`)
  - `client/control/ClientCtr.java` (+ `discoverServer()` UDP, `ClientListening`, `pendingMatchMessages`)
  - `client/view/DataReceiver.java`, `BaseActivity.java`
  - `LoginActivity.java`, `RegisterActivity.java`, `OnlineListActivity.java`, `EditPlayerActivity.java`, `ChangePasswordActivity.java`, `PlayerHistoryActivity.java`, `RankingActivity.java`
  - `ChallengeDialog.java`, `InviteDialog.java`
  - `adapter/OnlinePlayerAdapter.java`, `adapter/MatchHistoryAdapter.java`, `adapter/RankingAdapter.java`
- **`:db`:** `db/01_core.sql`

## 2. Thứ tự hoàn thiện trên nhánh `n04/skeleton`

1. **Bước 2 – `ServerCtr.java` (`ServerDiscovery` UDP):** lắng nghe UDP `"DISCOVER_SERVER"` ở cổng `9999` và trả về cổng TCP.
2. **Bước 3 – `ClientCtr.java`:** thêm `isConnected()`, `discoverServer()` (gửi broadcast LAN + `10.0.2.2` cho AVD), `resetSocketOnly()` (không xóa `myFunction` khi kết nối lỗi), cập nhật `openConnection()` và chuyển hướng mất kết nối sang `LoginActivity`.
3. **Bước 4 – `LoginActivity.java` & `RegisterActivity.java`:** gọi `openConnection()` trên luồng nền trước khi gửi `LOGIN_USER` / `REGISTER_USER`; dọn file `ClientMainActivity` cũ (Bước 5).
4. **Bước 6 – Hồ sơ & Thống kê:** hoàn thiện `editPlayer`, `changePassword`, `getRanking` (`PlayerDAO`), `getMatchesByPlayer` (`MatchDAO`), xử lý trong `ServerProcessing` và các màn hình `EditPlayerActivity`, `ChangePasswordActivity`, `PlayerHistoryActivity`, `RankingActivity`.
5. **Bước 7 – Danh sách online & Thách đấu:** hoàn thiện `OnlineListActivity`, `OnlinePlayerAdapter`, `PendingChallenge`, `tryCreateChallenge`, `ChallengeDialog`, `InviteDialog`, xử lý `SERVER_INFORM_CHALLENGE` toàn cục ở `ClientCtr` và bàn giao sang `GameRoom` / `MatchPlayActivity` của N11.
