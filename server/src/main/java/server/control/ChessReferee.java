package server.control;

import java.util.ArrayList;
import java.util.Timer;

import com.chess.engine.board.Board;
import model.MatchMove;
import model.MoveRequest;

/**
 * Chủ: N11. Trọng tài một ván: giữ bàn cờ chuẩn, kiểm tra lượt + luật, phát nước đi,
 * kiểm tra chiếu bí / hết nước, đồng hồ 30 giây mỗi nước.
 * Khi ván kết thúc theo luật/hết giờ: gọi room.endGame(winner, GameResult.CHECKMATE | STALEMATE | TIMEOUT).
 *
 * QUY TẮC KHÓA: đồng bộ bằng synchronized (room), KHÔNG dùng synchronized method của lớp này
 * (xem docs/HUONG_DAN_N11.md mục "Luồng & khóa").
 */
public class ChessReferee {

    public static final long MOVE_TIME_LIMIT_MS = 30_000;

    private final GameRoom room;
    private Board board;
    private Timer moveTimer;
    private final ArrayList<MatchMove> moves = new ArrayList<>();

    public ChessReferee(GameRoom room) {
        this.room = room;
    }

    /** Gọi bởi GameRoom (đang giữ khóa room) mỗi khi bắt đầu ván mới: bàn chuẩn, xóa biên bản, bật đồng hồ cho Trắng. */
    public void reset() {
        synchronized (room) {
            // TODO(N11): board = Board.createStandardBoard(); moves.clear(); startMoveTimer();
        }
    }

    /**
     * Gọi bởi GameRoom.handleMove() trên luồng ServerProcessing của người gửi.
     * Bỏ qua nếu !room.isPlaying(). Sai lượt/sai luật -> REPLY_MOVE_ILLEGAL cho sender.
     * Hợp lệ -> cập nhật board, thêm MatchMove, đặt lại đồng hồ, SERVER_INFORM_MOVE cho cả 2, checkGameOver().
     * Phong cấp: dùng MoveFactory.createPromotionMove khi req.getPromotion() != null.
     */
    public void handleMove(ServerCtr.ServerProcessing sender, MoveRequest req) {
        synchronized (room) {
            // TODO(N11)
        }
    }

    /** Gọi bởi GameRoom.endGame()/closeRoom() (đang giữ khóa room). */
    public void stopTimer() {
        synchronized (room) {
            if (moveTimer != null) {
                moveTimer.cancel();
                moveTimer = null;
            }
        }
    }

    /** Biên bản nước đi của ván hiện tại (GameRoom.endGame chép vào Match trước khi lưu). */
    public ArrayList<MatchMove> getMoves() {
        synchronized (room) {
            return new ArrayList<>(moves);
        }
    }

    // TODO(N11): checkGameOver(), startMoveTimer(), onMoveTimeout() - cũng dùng synchronized (room)
}
