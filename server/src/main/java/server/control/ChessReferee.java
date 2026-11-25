package server.control;

import java.util.ArrayList;
import java.util.Timer;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.Move.MoveFactory;
import com.chess.engine.board.Move.PawnPromotion;
import com.chess.engine.board.MoveTransition;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.Player;
import model.GameResult;
import model.MatchMove;
import model.MoveInfo;
import model.MoveRequest;
import model.ObjectWrapper;

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

    private static final String ILLEGAL_TURN = "Chưa tới lượt của bạn";
    private static final String ILLEGAL_MOVE = "Nước đi không hợp lệ";
    private static final String ILLEGAL_CHECK = "Nước đi khiến Vua bị chiếu";

    private final GameRoom room;
    private Board board;
    private Timer moveTimer;
    private long turnStartedAt;            // System.nanoTime() lúc bắt đầu lượt hiện tại
    private final ArrayList<MatchMove> moves = new ArrayList<>();

    public ChessReferee(GameRoom room) {
        this.room = room;
    }

    /** Gọi bởi GameRoom (đang giữ khóa room) mỗi khi bắt đầu ván mới: bàn chuẩn, xóa biên bản, bật đồng hồ cho Trắng. */
    public void reset() {
        synchronized (room) {
            board = Board.createStandardBoard();
            moves.clear();
            startMoveTimer();
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
            if (!room.isPlaying() || board == null || req == null) {
                return;
            }
            // MoveFactory.createMove duyệt nước của CẢ HAI bên -> bắt buộc kiểm tra lượt trước
            if (!colorOf(sender).equals(board.currentPlayer().getAlliance().toString())) {
                reject(sender, ILLEGAL_TURN);
                return;
            }
            final int from = req.getFromSquare();
            final int to = req.getToSquare();
            if (!isSquare(from) || !isSquare(to)) {
                reject(sender, ILLEGAL_MOVE);
                return;
            }

            final Move move;
            if (req.getPromotion() == null) {
                move = MoveFactory.createMove(board, from, to);
                // Tốt tới hàng cuối mà không chọn quân phong cấp -> client phải hỏi PromotionDialog
                if (move instanceof PawnPromotion) {
                    reject(sender, ILLEGAL_MOVE);
                    return;
                }
            } else {
                final Piece.PieceType type = toPieceType(req.getPromotion());
                if (type == null) {
                    reject(sender, ILLEGAL_MOVE);
                    return;
                }
                move = MoveFactory.createPromotionMove(board, from, to, type);
            }

            final MoveTransition transition = board.currentPlayer().makeMove(move);
            switch (transition.getMoveStatus()) {
                case ILLEGAL_MOVE:
                    reject(sender, ILLEGAL_MOVE);
                    return;
                case LEAVES_PLAYER_IN_CHECK:
                    reject(sender, ILLEGAL_CHECK);
                    return;
                case DONE:
                default:
                    break;
            }

            // Ký hiệu phải lấy TRƯỚC khi thay bàn (dựa vào bàn cũ để phân biệt quân cùng loại)
            final String notation = move.toString();
            board = transition.getToBoard();
            moves.add(new MatchMove(moves.size() + 1, from, to, notation, req.getPromotion()));
            startMoveTimer();

            final Player toMove = board.currentPlayer();
            final MoveInfo info = new MoveInfo(notation, from, to, req.getPromotion(),
                    toMove.getAlliance().toString(), toMove.isInCheck());
            // Gửi nước đi TRƯỚC để 2 client thấy nước chiếu bí rồi mới nhận kết quả
            room.getWhitePlayer().sendData(new ObjectWrapper(ObjectWrapper.SERVER_INFORM_MOVE, info));
            room.getBlackPlayer().sendData(new ObjectWrapper(ObjectWrapper.SERVER_INFORM_MOVE, info));
            checkGameOver(sender);
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

    /** Bên vừa tới lượt bị chiếu bí -> người vừa đi thắng; hết nước đi -> hòa. */
    private void checkGameOver(ServerCtr.ServerProcessing justMoved) {
        synchronized (room) {
            final Player toMove = board.currentPlayer();
            if (toMove.isInCheckMate()) {
                room.endGame(justMoved, GameResult.CHECKMATE);
            } else if (toMove.isInStaleMate()) {
                room.endGame(null, GameResult.STALEMATE);
            }
        }
    }

    /** Hủy hẹn giờ cũ, hẹn 30 giây cho bên tới lượt. */
    private void startMoveTimer() {
        synchronized (room) {
            stopTimer();
            turnStartedAt = System.nanoTime();
            moveTimer = new Timer("move-timer", true);
            moveTimer.schedule(new MoveTimeoutTask(this), MOVE_TIME_LIMIT_MS);
        }
    }

    /** Gọi bởi MoveTimeoutTask trên luồng Timer: bên tới lượt thua vì hết giờ. */
    void onMoveTimeout() {
        synchronized (room) {
            if (!room.isPlaying() || board == null) {
                return;
            }
            // Hẹn giờ cũ có thể đã chạy và đang chờ khóa đúng lúc một nước hợp lệ tới -> bỏ qua
            final long elapsedMs = (System.nanoTime() - turnStartedAt) / 1_000_000;
            if (elapsedMs < MOVE_TIME_LIMIT_MS) {
                return;
            }
            final boolean whiteToMove = board.currentPlayer().getAlliance().isWhite();
            final ServerCtr.ServerProcessing winner = whiteToMove ? room.getBlackPlayer() : room.getWhitePlayer();
            room.endGame(winner, GameResult.TIMEOUT);
        }
    }

    private String colorOf(ServerCtr.ServerProcessing sp) {
        return sp == room.getWhitePlayer() ? "WHITE" : "BLACK";
    }

    private static void reject(ServerCtr.ServerProcessing sender, String reason) {
        sender.sendData(new ObjectWrapper(ObjectWrapper.REPLY_MOVE_ILLEGAL, reason));
    }

    private static boolean isSquare(int square) {
        return square >= 0 && square < 64;
    }

    /** "Q" | "R" | "B" | "N" -> PieceType; giá trị khác -> null. */
    static Piece.PieceType toPieceType(String promotion) {
        switch (promotion) {
            case "Q": return Piece.PieceType.QUEEN;
            case "R": return Piece.PieceType.ROOK;
            case "B": return Piece.PieceType.BISHOP;
            case "N": return Piece.PieceType.KNIGHT;
            default:  return null;
        }
    }
}
