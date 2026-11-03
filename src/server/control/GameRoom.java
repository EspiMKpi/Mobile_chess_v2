package server.control;

import model.Match;
import model.MoveRequest;

/**
 * Chủ: N04. Vòng đời một cặp đấu: chờ trả lời thách đấu, phân màu, bắt đầu ván, kết thúc ván,
 * tiếp tục/đổi màu, đóng phòng. Phần luật cờ / nước đi / đồng hồ 30s nằm ở {@link ChessReferee} (N11).
 *
 * QUY TẮC KHÓA (hợp đồng chung): mọi thay đổi trạng thái phòng + trọng tài đều đồng bộ trên
 * CHÍNH ĐỐI TƯỢNG GameRoom này (synchronized method ở đây, synchronized (room) bên ChessReferee).
 * Không dùng khóa thứ hai -> không thể deadlock giữa luồng client và luồng Timer.
 */
public class GameRoom {

    private final ServerCtr myServer;
    private ServerCtr.ServerProcessing whitePlayer;
    private ServerCtr.ServerProcessing blackPlayer;
    private Match currentMatch;
    private int continueCount;
    private boolean playing;               // true từ startGame/ván mới tới endGame
    private final ChessReferee referee;

    /** Tạo khi A thách đấu B đang rảnh. Ván 1: người thách đấu (A) cầm Đen, người được thách (B) cầm Trắng. */
    public GameRoom(ServerCtr myServer, ServerCtr.ServerProcessing challenger,
                    ServerCtr.ServerProcessing challenged) {
        this.myServer = myServer;
        this.blackPlayer = challenger;
        this.whitePlayer = challenged;
        this.referee = new ChessReferee(this);
    }

    /** B trả lời lời mời (REPLY_CHALLENGE_DECISION). true -> startGame(); false -> closeRoom() + REPLY_CHALLENGE_REJECT cho A. */
    public synchronized void receiveChallengeDecision(boolean accept) {
        // TODO(N04)
    }

    /** Ván 1: new Match, referee.reset(), playing = true, broadcastOnlineList, gửi SERVER_START_MATCH cho 2 bên. */
    public synchronized void startGame() {
        // TODO(N04)
    }

    /** Nhận SEND_MOVE từ ServerProcessing -> chuyển cho trọng tài (N11). */
    public void handleMove(ServerCtr.ServerProcessing sender, MoveRequest req) {
        referee.handleMove(sender, req);
    }

    /**
     * Kết thúc ván. Gọi bởi N04 (RESIGN, DISCONNECT) và N11 (CHECKMATE, STALEMATE, TIMEOUT).
     * Idempotent: nếu !playing thì return ngay (tránh hết giờ và xin thua cùng lúc bị lưu 2 lần).
     * @param winner người thắng, null nếu hòa
     * @param reason một hằng số trong GameResult
     */
    public synchronized void endGame(ServerCtr.ServerProcessing winner, String reason) {
        // TODO(N04): if (!playing) return; playing = false; referee.stopTimer(); ... (xem blueprint)
    }

    /** REQUEST_CONTINUE_MATCH. Đủ 2 "Tiếp tục" -> swapColor + ván mới; có 1 "Dừng" -> closeRoom + END_SESSION. */
    public synchronized void receiveContinueDecision(ServerCtr.ServerProcessing sp, boolean accept) {
        // TODO(N04)
    }

    public synchronized void swapColor() {
        ServerCtr.ServerProcessing tmp = whitePlayer;
        whitePlayer = blackPlayer;
        blackPlayer = tmp;
    }

    /** referee.stopTimer(); 2 bên về IDLE, setRoom(null); myServer.removeRoom(this). */
    public synchronized void closeRoom() {
        // TODO(N04)
    }

    /** ChessReferee kiểm tra trước khi xử lý nước đi / hết giờ. */
    public synchronized boolean isPlaying() { return playing; }

    public ServerCtr.ServerProcessing getWhitePlayer() { return whitePlayer; }
    public ServerCtr.ServerProcessing getBlackPlayer() { return blackPlayer; }
    public Match getCurrentMatch() { return currentMatch; }
}
