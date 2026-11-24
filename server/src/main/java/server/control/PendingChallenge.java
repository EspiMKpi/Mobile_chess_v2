package server.control;

import java.util.Timer;
import java.util.TimerTask;

import model.ObjectWrapper;
import model.PlayerStat;

/**
 * Chủ: N04. Quản lý vòng đời MỘT lời mời thách đấu đang chờ phản hồi (tối đa 30 giây).
 * Khi hết 30s / bị từ chối / bị hủy / một bên mất mạng -> giải phóng 2 người về IDLE.
 * Khi đối thủ Đồng ý -> bàn giao sang MatchSession (GameRoom của N11).
 */
public class PendingChallenge {

    public static final long INVITE_TIMEOUT_MS = 30_000L;

    private final ServerCtr myServer;
    private final ServerCtr.ServerProcessing challenger;  // A: người mời (sẽ cầm ĐEN)
    private final ServerCtr.ServerProcessing challenged;  // B: người được mời (sẽ cầm TRẮNG)
    private Timer timeoutTimer;
    private boolean resolved;

    public PendingChallenge(ServerCtr myServer,
                            ServerCtr.ServerProcessing challenger,
                            ServerCtr.ServerProcessing challenged) {
        this.myServer = myServer;
        this.challenger = challenger;
        this.challenged = challenged;
    }

    /**
     * TODO(N04 - Hình 4.11, bước 20): Bắt đầu đếm ngược 30 giây chờ B trả lời.
     * Gợi ý:
     *   timeoutTimer = new Timer(true);
     *   timeoutTimer.schedule(new TimerTask() {
     *       @Override
     *       public void run() { onTimeout(); }
     *   }, INVITE_TIMEOUT_MS);
     */
    public synchronized void startTimeout() {
        // TODO(N04): cài đặt bộ đếm 30 giây ở trên
    }

    /**
     * TODO(N04 - Hình 4.12, bước 4-5): B phản hồi REPLY_CHALLENGE_DECISION (true = Đồng ý, false = Từ chối).
     *  - Nếu resolved hoặc sender != challenged -> return.
     *  - Đánh dấu resolved = true; hủy timeoutTimer.
     *  - Nếu accept == false:
     *      + Gỡ pendingChallenge của A và B về null, trả status về PlayerStat.IDLE, gọi myServer.broadcastOnlineList().
     *      + Gửi ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_REJECT, "REJECT") cho challenger.
     *  - Nếu accept == true:
     *      + Gỡ pendingChallenge của A và B về null (status vẫn BUSY).
     *      + Tạo MatchSession session = new GameRoom(myServer, challenger, challenged);
     *      + Gắn session vào challenger.setMatchSession(session) và challenged.setMatchSession(session).
     *      + Gọi session.start();
     */
    public synchronized void onDecision(ServerCtr.ServerProcessing sender, boolean accept) {
        // TODO(N04): xử lý Đồng ý / Từ chối
    }

    /**
     * TODO(N04 - Mục 1.3): Người mời A bấm Hủy lời mời (gửi CANCEL_CHALLENGE = 27).
     *  - Nếu resolved hoặc sender != challenger -> return.
     *  - Đánh dấu resolved = true; hủy timeoutTimer.
     *  - Giải phóng A và B về IDLE + pendingChallenge = null + broadcastOnlineList().
     *  - Gửi SERVER_CANCEL_CHALLENGE ("CANCEL") cho challenged và REPLY_CHALLENGE_REJECT ("CANCEL") cho challenger.
     */
    public synchronized void onCancel(ServerCtr.ServerProcessing sender) {
        // TODO(N04): xử lý hủy lời mời
    }

    /**
     * TODO(N04 - Mục 1.3): Hết 30 giây mà B chưa trả lời.
     *  - Nếu resolved -> return.
     *  - Giải phóng A và B về IDLE + pendingChallenge = null + broadcastOnlineList().
     *  - Gửi REPLY_CHALLENGE_REJECT ("TIMEOUT") cho challenger và SERVER_CANCEL_CHALLENGE ("TIMEOUT") cho challenged.
     */
    public synchronized void onTimeout() {
        // TODO(N04): xử lý hết giờ 30 giây
    }

    /**
     * TODO(N04 - Mục 1.3): A hoặc B mất kết nối trong lúc lời mời đang chờ.
     *  - Giải phóng người còn lại về IDLE + pendingChallenge = null + broadcastOnlineList().
     *  - Nếu A mất kết nối -> gửi SERVER_CANCEL_CHALLENGE ("DISCONNECT") cho B.
     *  - Nếu B mất kết nối -> gửi REPLY_CHALLENGE_REJECT ("DISCONNECT") cho A.
     */
    public synchronized void onDisconnect(ServerCtr.ServerProcessing disconnected) {
        // TODO(N04): xử lý mất kết nối khi đang chờ lời mời
    }
}
