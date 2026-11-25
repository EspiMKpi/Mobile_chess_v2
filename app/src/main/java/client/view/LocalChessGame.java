package client.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.Move.MoveFactory;
import com.chess.engine.board.Move.PawnPromotion;
import com.chess.engine.board.MoveTransition;
import com.chess.engine.pieces.Piece;

import ptit.e23cnpm01.nguyentiendung.covua.R;

/**
 * Chủ: N11. Điều khiển một ván cờ 2 người chơi chung máy (chưa nối Server):
 * chạm chọn quân -> chấm ô đi được -> chạm ô đích để đi; phong cấp; biên bản;
 * đồng hồ 15 phút mỗi bên; kết thúc khi chiếu bí, hết nước, hết giờ hoặc đầu hàng.
 * Mọi hàm chạy trên main thread.
 */
public class LocalChessGame {

    public static final long GAME_TIME_MS = 15 * 60 * 1000L;   // 15 phút mỗi bên
    private static final long TICK_MS = 200;
    private static final long LOW_TIME_MS = 30 * 1000L;         // dưới 30 giây thì đồng hồ đỏ
    private static final int COLOR_LOW_TIME = 0xFFD32F2F;

    private final Context context;
    private final BoardCanvasView boardView;
    private final TextView whiteClock;
    private final TextView blackClock;
    private final TextView moveLog;
    private final int clockColor;

    private Board board;
    private int selected = -1;
    private boolean gameOver = true;
    private int plyCount;
    private final StringBuilder log = new StringBuilder();

    private long whiteMs;
    private long blackMs;
    private long lastTick;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            if (!gameOver) {
                handler.postDelayed(this, TICK_MS);
            }
        }
    };

    public LocalChessGame(Context context, BoardCanvasView boardView,
                          TextView whiteClock, TextView blackClock, TextView moveLog) {
        this.context = context;
        this.boardView = boardView;
        this.whiteClock = whiteClock;
        this.blackClock = blackClock;
        this.moveLog = moveLog;
        this.clockColor = whiteClock.getCurrentTextColor();
        moveLog.setMovementMethod(new ScrollingMovementMethod());   // biên bản cuộn được
        boardView.setOnSquareTapListener(this::onSquareTapped);
    }

    /** Bắt đầu ván mới: bàn chuẩn, Trắng đi trước, đồng hồ 15' mỗi bên, Trắng bắt đầu chạy giờ. */
    public void start() {
        board = Board.createStandardBoard();
        selected = -1;
        plyCount = 0;
        log.setLength(0);
        moveLog.setText("");
        whiteMs = GAME_TIME_MS;
        blackMs = GAME_TIME_MS;
        gameOver = false;
        boardView.setBoard(board);
        boardView.setLastMove(-1, -1);
        boardView.setHighlights(-1, Collections.<Integer>emptyList(), -1);
        refreshClocks();
        lastTick = SystemClock.elapsedRealtime();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, TICK_MS);
    }

    /** Dừng đồng hồ (gọi trong onDestroy của Activity). */
    public void stop() {
        handler.removeCallbacks(tick);
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /** Nút Đầu hàng: bên đang tới lượt đầu hàng, sau khi xác nhận. */
    public void confirmResign() {
        if (gameOver) {
            return;
        }
        final Alliance loser = board.currentPlayer().getAlliance();
        new AlertDialog.Builder(context)
                .setTitle(R.string.n11_resign)
                .setMessage(context.getString(R.string.n11_resign_confirm, sideName(loser)))
                .setPositiveButton(R.string.n11_yes, (d, w) -> {
                    if (!gameOver) {
                        Alliance winner = opposite(loser);
                        finish(context.getString(R.string.n11_win_resign, sideName(winner), sideName(loser)));
                    }
                })
                .setNegativeButton(R.string.n11_no, null)
                .show();
    }

    /** Nút Thoát: đang chơi thì hỏi lại trước khi thoát; ván đã xong thì thoát luôn. */
    public void confirmExit(Runnable onExit) {
        if (gameOver) {
            onExit.run();
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle(R.string.n11_exit_title)
                .setMessage(R.string.n11_exit_confirm)
                .setPositiveButton(R.string.n11_yes, (d, w) -> {
                    stop();
                    onExit.run();
                })
                .setNegativeButton(R.string.n11_no, null)
                .show();
    }

    // ============================== Chạm & đi quân ==============================

    private void onSquareTapped(int square) {
        if (gameOver) {
            return;
        }
        Alliance side = board.currentPlayer().getAlliance();
        Piece piece = board.getPiece(square);
        boolean ownPiece = piece != null && piece.getPieceAllegiance() == side;

        if (selected == -1 || (ownPiece && square != selected)) {
            select(ownPiece ? square : -1);          // chọn quân của bên đang tới lượt
            return;
        }
        if (square == selected) {
            select(-1);                               // chạm lại -> bỏ chọn
            return;
        }
        List<Move> candidates = legalMovesBetween(board, selected, square);
        if (candidates.isEmpty()) {
            select(-1);                               // ô không đi được -> bỏ chọn
            return;
        }
        if (candidates.get(0) instanceof PawnPromotion) {
            askPromotion(selected, square);
        } else {
            applyMove(candidates.get(0));
        }
    }

    private void select(int square) {
        selected = square;
        List<Integer> targets = square == -1
                ? Collections.<Integer>emptyList()
                : legalDestinations(board, square);
        boardView.setHighlights(square, targets, BoardCanvasView.findCheckedKing(board));
    }

    private void askPromotion(int from, int to) {
        final Piece.PieceType[] types = {Piece.PieceType.QUEEN, Piece.PieceType.ROOK,
                Piece.PieceType.BISHOP, Piece.PieceType.KNIGHT};
        new AlertDialog.Builder(context)
                .setTitle(R.string.n11_promotion_title)
                .setItems(R.array.n11_promotion_labels, (d, which) -> {
                    if (!gameOver) {
                        applyMove(MoveFactory.createPromotionMove(board, from, to, types[which]));
                    }
                })
                .setCancelable(false)
                .show();
    }

    /** Đi một nước đã biết là hợp lệ: trừ giờ bên vừa đi, thay bàn, ghi biên bản, kiểm tra kết thúc. */
    private void applyMove(Move move) {
        String notation = move.toString();                       // lấy TRƯỚC khi thay bàn
        int from = move.getCurrentCoordinate();
        int to = move.getDestinationCoordinate();
        Alliance mover = board.currentPlayer().getAlliance();
        MoveTransition transition = board.currentPlayer().makeMove(move);
        if (!transition.getMoveStatus().isDone()) {
            select(-1);
            return;
        }
        updateClock();                                            // tính giờ cho bên vừa đi tới lúc này
        if (gameOver) {
            return;                                               // vừa hết giờ đúng lúc đi
        }
        board = transition.getToBoard();
        selected = -1;

        boolean mate = board.currentPlayer().isInCheckMate();
        boolean check = board.currentPlayer().isInCheck();
        appendLog(notation + (mate ? "#" : check ? "+" : ""), mover == Alliance.WHITE);

        boardView.setBoard(board);
        boardView.setLastMove(from, to);
        boardView.setHighlights(-1, Collections.<Integer>emptyList(), BoardCanvasView.findCheckedKing(board));
        refreshClocks();

        if (mate) {
            finish(context.getString(R.string.n11_win_checkmate, sideName(mover)));
        } else if (board.currentPlayer().isInStaleMate()) {
            finish(context.getString(R.string.n11_draw_stalemate));
        }
    }

    // ============================== Đồng hồ ==============================

    /** Trừ thời gian đã trôi cho bên đang tới lượt; về 0 thì bên đó thua. */
    private void updateClock() {
        if (gameOver || board == null) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        long elapsed = now - lastTick;
        lastTick = now;
        Alliance side = board.currentPlayer().getAlliance();
        if (side == Alliance.WHITE) {
            whiteMs = Math.max(0, whiteMs - elapsed);
        } else {
            blackMs = Math.max(0, blackMs - elapsed);
        }
        refreshClocks();
        if ((side == Alliance.WHITE ? whiteMs : blackMs) == 0) {
            finish(context.getString(R.string.n11_win_timeout, sideName(opposite(side)), sideName(side)));
        }
    }

    /** Hiện mm:ss; bên đang tới lượt in đậm, dưới 30 giây chuyển đỏ. */
    private void refreshClocks() {
        Alliance side = board == null ? Alliance.WHITE : board.currentPlayer().getAlliance();
        styleClock(whiteClock, whiteMs, !gameOver && side == Alliance.WHITE);
        styleClock(blackClock, blackMs, !gameOver && side == Alliance.BLACK);
    }

    private void styleClock(TextView view, long ms, boolean running) {
        long totalSeconds = (ms + 999) / 1000;                    // làm tròn lên: 0,4s vẫn hiện 0:01
        view.setText(String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60));
        view.setTypeface(Typeface.MONOSPACE, running ? Typeface.BOLD : Typeface.NORMAL);
        view.setTextColor(ms <= LOW_TIME_MS ? COLOR_LOW_TIME : clockColor);
        view.setAlpha(running || gameOver ? 1f : 0.6f);
    }

    // ============================== Kết thúc & biên bản ==============================

    private void finish(String message) {
        if (gameOver) {
            return;
        }
        gameOver = true;
        handler.removeCallbacks(tick);
        selected = -1;
        boardView.clearSelection();
        refreshClocks();
        new AlertDialog.Builder(context)
                .setTitle(R.string.n11_game_over_title)
                .setMessage(message)
                .setPositiveButton(R.string.n11_ok, null)
                .show();
    }

    /** Mỗi lượt một dòng: "1.  e4      e5". */
    private void appendLog(String notation, boolean whiteMove) {
        plyCount++;
        if (whiteMove) {
            if (log.length() > 0) {
                log.append('\n');
            }
            log.append(String.format(Locale.US, "%-4s%-8s", (plyCount + 1) / 2 + ".", notation));
        } else {
            log.append(notation);
        }
        moveLog.setText(log);
        moveLog.post(() -> {
            if (moveLog.getLayout() != null) {
                int scroll = moveLog.getLayout().getLineTop(moveLog.getLineCount()) - moveLog.getHeight()
                        + moveLog.getTotalPaddingTop() + moveLog.getTotalPaddingBottom();
                moveLog.scrollTo(0, Math.max(0, scroll));          // luôn thấy nước mới nhất
            }
        });
    }

    private String sideName(Alliance side) {
        return context.getString(side == Alliance.WHITE ? R.string.n11_white : R.string.n11_black);
    }

    private static Alliance opposite(Alliance side) {
        return side == Alliance.WHITE ? Alliance.BLACK : Alliance.WHITE;
    }

    // ============================== Luật: nước thật sự hợp lệ ==============================

    /** Các nước từ from tới to không để Vua mình bị chiếu (phong cấp có tới 4 nước). */
    static List<Move> legalMovesBetween(Board board, int from, int to) {
        List<Move> result = new ArrayList<>();
        for (Move m : board.currentPlayer().getLegalMoves()) {
            if (m.getCurrentCoordinate() == from && m.getDestinationCoordinate() == to
                    && board.currentPlayer().makeMove(m).getMoveStatus().isDone()) {
                result.add(m);
            }
        }
        return result;
    }

    /** Ô đích hợp lệ của quân ở from, để chấm gợi ý. */
    static List<Integer> legalDestinations(Board board, int from) {
        List<Integer> result = new ArrayList<>();
        for (Move m : board.currentPlayer().getLegalMoves()) {
            int to = m.getDestinationCoordinate();
            if (m.getCurrentCoordinate() == from && !result.contains(to)
                    && board.currentPlayer().makeMove(m).getMoveStatus().isDone()) {
                result.add(to);
            }
        }
        return result;
    }
}
