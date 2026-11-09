package server.control;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

import model.Match;
import model.MoveRequest;

/**
 * Chủ: N04. Vòng đời một cặp đấu: chờ trả lời thách đấu (30s), bắt đầu ván, kết thúc ván,
 * chơi tiếp / đổi màu (30s), đóng phòng. Luật cờ / nước đi / đồng hồ mỗi nước nằm ở ChessReferee (N11).
 *
 * QUY TẮC KHÓA (hợp đồng chung): mọi thay đổi trạng thái phòng đồng bộ trên CHÍNH đối tượng GameRoom
 * (synchronized method ở đây, synchronized (room) bên ChessReferee). Không dùng khóa thứ hai.
 */
public class GameRoom {

    public static final long CHALLENGE_TIMEOUT_MS = 30_000;
    public static final long CONTINUE_TIMEOUT_MS = 30_000;

    private final ServerCtr myServer;
    private final ServerCtr.ServerProcessing challenger;   // người thách (A) - ván 1 cầm Đen, đi sau
    private final ServerCtr.ServerProcessing challenged;   // người được thách (B) - ván 1 cầm Trắng, đi trước
    private ServerCtr.ServerProcessing whitePlayer;
    private ServerCtr.ServerProcessing blackPlayer;
    private Match currentMatch;
    private final Set<ServerCtr.ServerProcessing> continueVotes = new HashSet<>();
    private boolean playing;               // true từ lúc bắt đầu ván tới endGame
    private boolean closed;
    private Timer challengeTimer;          // 30s chờ B trả lời lời mời
    private Timer continueTimer;           // 30s chờ hai bên chọn Tiếp tục / Dừng
    private final ChessReferee referee;

    /** Ván 1: challenger (A) cầm Đen, challenged (B) cầm Trắng. */
    public GameRoom(ServerCtr myServer, ServerCtr.ServerProcessing challenger,
                    ServerCtr.ServerProcessing challenged) {
        this.myServer = myServer;
        this.challenger = challenger;
        this.challenged = challenged;
        this.referee = new ChessReferee(this);
        // TODO: gán blackPlayer, whitePlayer
    }

    // ===================== Lời mời thách đấu =====================

    /** Bật hẹn giờ 30 giây chờ B trả lời (ServerCtr.createRoom gọi). Hết giờ -> onChallengeTimeout(). */
    public synchronized void startChallengeTimer() {
        // TODO
    }

    /** Hủy hẹn giờ chờ trả lời. */
    private void cancelChallengeTimer() {
        // TODO
    }

    /** Hết 30 giây mà B chưa trả lời: lời mời kết thúc, báo lý do cho cả hai, closeRoom(). */
    private synchronized void onChallengeTimeout() {
        // TODO
    }

    /** Rút gọn: coi như B (challenged) trả lời. Test N04CoreTest dùng bản này. */
    public synchronized void receiveChallengeDecision(boolean accept) {
        // TODO: receiveChallengeDecision(challenged, accept);
    }

    /**
     * REPLY_CHALLENGE_DECISION (hình 4.12, bước 4-5). Chỉ hợp lệ khi chưa vào ván.
     * - B đồng ý: cancelChallengeTimer(), startGame().
     * - B từ chối: cancelChallengeTimer(), closeRoom(), REPLY_CHALLENGE_REJECT cho A.
     * - A hủy (sender == challenger, accept == false): báo cả hai, closeRoom().
     */
    public synchronized void receiveChallengeDecision(ServerCtr.ServerProcessing sender, boolean accept) {
        // TODO
    }

    // ===================== Ván đấu =====================

    /**
     * Bắt đầu ván 1 (hình 4.12, bước 5-22): beginNewMatch(), lấy myServer.getPlayerStat() của hai bên,
     * gửi SERVER_START_MATCH (MatchStartInfo: màu quân + đối thủ) cho A rồi B, broadcastOnlineList().
     */
    public synchronized void startGame() {
        // TODO
    }

    /** new Match(new Date()), xóa continueVotes, playing = true, referee.reset(). */
    private void beginNewMatch() {
        // TODO
    }

    /** SEND_MOVE từ ServerProcessing -> referee.handleMove(sender, req) (N11). */
    public void handleMove(ServerCtr.ServerProcessing sender, MoveRequest req) {
        // TODO
    }

    /**
     * Kết thúc ván (hình 4.13, bước 4-22). N04 gọi (RESIGN, DISCONNECT), N11 gọi (CHECKMATE, STALEMATE, TIMEOUT).
     * Idempotent: !playing -> return ngay. playing = false; referee.stopTimer(); referee.getMoves();
     * currentMatch.setEndTime + addResult (2 MatchResult); new MatchDAO().saveMatch(currentMatch);
     * gửi SERVER_END_GAME (GameResult riêng: thắng 1, hòa 0.5, thua 0) cho từng người; startContinueTimer().
     * @param winner người thắng, null nếu hòa
     * @param reason một hằng số trong GameResult
     */
    public synchronized void endGame(ServerCtr.ServerProcessing winner, String reason) {
        // TODO
    }

    // ===================== Chơi tiếp =====================

    /** Bật hẹn giờ 30 giây chờ hai bên chọn Tiếp tục / Dừng (gọi cuối endGame). Hết giờ -> onContinueTimeout(). */
    private void startContinueTimer() {
        // TODO
    }

    private void cancelContinueTimer() {
        // TODO
    }

    /** Hết 30 giây chưa đủ hai người chọn Tiếp tục: SERVER_END_SESSION_MATCH cho cả hai, closeRoom(). */
    private synchronized void onContinueTimeout() {
        // TODO
    }

    /**
     * REQUEST_CONTINUE_MATCH (hình 4.14, bước 4-22). Chỉ hợp lệ giữa hai ván.
     * - Dừng: SERVER_END_SESSION_MATCH "STOP" cho người bấm, "OPPONENT_STOP" cho người kia, closeRoom().
     * - Tiếp tục: thêm vào continueVotes; đủ 2 người -> cancelContinueTimer(), swapColor(), beginNewMatch(),
     *   gửi SERVER_RESTART_MATCH_SWAP_COLOR ("WHITE" / "BLACK") cho từng người.
     */
    public synchronized void receiveContinueDecision(ServerCtr.ServerProcessing sp, boolean accept) {
        // TODO
    }

    /** Hoán đổi whitePlayer và blackPlayer. */
    public synchronized void swapColor() {
        // TODO
    }

    // ===================== Đóng phòng =====================

    /** Hủy mọi hẹn giờ, referee.stopTimer(); hai bên setRoom(null) + IDLE; myServer.removeRoom(this) + broadcastOnlineList(). */
    public synchronized void closeRoom() {
        // TODO
    }

    /**
     * Một người mất kết nối:
     * - Đang chờ trả lời lời mời: lời mời bị hủy, báo người còn lại.
     * - Đang chơi: endGame(đối thủ, GameResult.DISCONNECT), báo người còn lại về danh sách online.
     * Sau đó closeRoom().
     */
    public synchronized void playerLeft(ServerCtr.ServerProcessing sp) {
        // TODO
    }

    /** ChessReferee kiểm tra trước khi xử lý nước đi / hết giờ. */
    public synchronized boolean isPlaying() { return playing; }

    public ServerCtr.ServerProcessing getWhitePlayer() { return whitePlayer; }
    public ServerCtr.ServerProcessing getBlackPlayer() { return blackPlayer; }
    public Match getCurrentMatch() { return currentMatch; }
}
