package client.view;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

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

import model.GameResult;
import model.MatchResult;
import model.MatchStartInfo;
import model.MoveInfo;
import model.MoveRequest;
import model.ObjectWrapper;
import model.PlayerStat;
import ptit.e23cnpm01.nguyentiendung.covua.R;

/**
 * Chủ: N11. Phòng đấu online. N04 mở màn hình này (ChallengeDialog / InviteDialog) với
 * MatchStartInfo trong extra {@link #EXTRA_START_INFO}.
 *
 * Bàn cờ trên máy chỉ để hiển thị và gợi ý; Server (ChessReferee) quyết định:
 * - chạm ô đích -> gửi SEND_MOVE, chờ Server trả SERVER_INFORM_MOVE (cả 2 máy) hoặc REPLY_MOVE_ILLEGAL;
 * - SERVER_INFORM_MOVE mang ô đi / ô đến / quân phong cấp -> máy tự đi lại đúng nước đó bằng engine;
 * - đồng hồ 30 giây mỗi nước chỉ để hiển thị, hết giờ do Server phán (SERVER_END_GAME, lý do TIMEOUT).
 */
public class MatchPlayActivity extends BaseActivity implements ContinueDialog.Listener {

    public static final String EXTRA_START_INFO = "start_info";

    private static final long MOVE_TIME_MS = 30_000;
    private static final long TICK_MS = 200;
    private static final long LOW_TIME_MS = 10_000;
    private static final int COLOR_LOW_TIME = 0xFFD32F2F;

    private BoardCanvasView boardView;
    private TextView txtNameWhite, txtNameBlack, txtClockWhite, txtClockBlack, txtMoveLog;
    private int clockColor;

    private PlayerStat me;
    private PlayerStat opponent;
    private String myColor = MatchResult.WHITE;

    private Board board;
    private int selected = -1;
    private boolean playing;          // ván đang diễn ra
    private boolean waitingServer;    // đã gửi nước đi, chờ Server trả lời
    private boolean leaving;          // người chơi bấm Thoát: xin thua rồi rời phòng
    private int plyCount;
    private final StringBuilder log = new StringBuilder();
    private GameResult pendingResult; // kết quả chờ hiện ContinueDialog khi Activity quay lại

    private long moveDeadline;        // SystemClock.elapsedRealtime() lúc hết giờ của lượt hiện tại
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refreshClocks();
            if (playing) {
                handler.postDelayed(this, TICK_MS);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.n11_activity_match_play);
        setTitle(R.string.n11_title_match);

        boardView = findViewById(R.id.n11_boardCanvas);
        txtNameWhite = findViewById(R.id.n11_txtNameWhite);
        txtNameBlack = findViewById(R.id.n11_txtNameBlack);
        txtClockWhite = findViewById(R.id.n11_txtClockWhite);
        txtClockBlack = findViewById(R.id.n11_txtClockBlack);
        txtMoveLog = findViewById(R.id.n11_moveLog);
        txtMoveLog.setMovementMethod(new ScrollingMovementMethod());
        clockColor = txtClockWhite.getCurrentTextColor();
        boardView.setOnSquareTapListener(this::onSquareTapped);

        Button btnResign = findViewById(R.id.n11_btnResign);
        Button btnExit = findViewById(R.id.n11_btnExit);
        btnResign.setOnClickListener(v -> confirmResign());
        btnExit.setOnClickListener(v -> confirmExit());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmExit();
            }
        });

        MatchStartInfo info = (MatchStartInfo) getIntent().getSerializableExtra(EXTRA_START_INFO);
        if (info != null) {
            me = info.getMe();
            opponent = info.getOpponent();
            if (info.getMyColor() != null) {
                myColor = info.getMyColor();
            }
        }
        startNewGame(myColor);

        // Đăng ký nhận gói tin trong trận, rồi lấy lại các gói đã tới trước khi màn hình kịp mở
        listen(ObjectWrapper.SERVER_END_GAME,
                ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR,
                ObjectWrapper.SERVER_END_SESSION_MATCH,
                ObjectWrapper.REPLY_MOVE_ILLEGAL,
                ObjectWrapper.SERVER_INFORM_MOVE);
        myControl.flushPendingMatchMessages(this);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (pendingResult != null) {              // kết quả tới lúc app đang ở nền
            GameResult r = pendingResult;
            pendingResult = null;
            showContinueDialog(r);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }

    // ============================== Gói tin từ Server ==============================

    @Override
    public void receivedDataProcessing(ObjectWrapper d) {
        switch (d.getPerformative()) {
            case ObjectWrapper.SERVER_INFORM_MOVE:
                if (d.getData() instanceof MoveInfo) {
                    applyServerMove((MoveInfo) d.getData());
                }
                break;
            case ObjectWrapper.REPLY_MOVE_ILLEGAL:
                waitingServer = false;
                select(-1);
                new AlertDialog.Builder(this)
                        .setTitle(R.string.n11_illegal_title)
                        .setMessage(String.valueOf(d.getData()))
                        .setPositiveButton(R.string.n11_ok, null)
                        .show();
                break;
            case ObjectWrapper.SERVER_END_GAME:
                if (d.getData() instanceof GameResult) {
                    onGameEnded((GameResult) d.getData());
                }
                break;
            case ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR:
                dismissContinueDialog();
                if (d.getData() instanceof String) {
                    startNewGame((String) d.getData());
                }
                break;
            case ObjectWrapper.SERVER_END_SESSION_MATCH:
                dismissContinueDialog();
                if ("OPPONENT_STOP".equals(d.getData()) && !leaving) {
                    toast(getString(R.string.n11_opponent_stopped));
                }
                finish();                          // quay về OnlineListActivity
                break;
            default:
                break;
        }
    }

    /** Server xác nhận một nước (của mình hoặc đối thủ): đi lại đúng nước đó trên bàn của máy. */
    private void applyServerMove(MoveInfo info) {
        Move move = info.getPromotion() == null
                ? MoveFactory.createMove(board, info.getFromSquare(), info.getToSquare())
                : MoveFactory.createPromotionMove(board, info.getFromSquare(), info.getToSquare(),
                        toPieceType(info.getPromotion()));
        boolean whiteMoved = board.currentPlayer().getAlliance() == Alliance.WHITE;
        MoveTransition t = board.currentPlayer().makeMove(move);
        if (t.getMoveStatus().isDone()) {
            board = t.getToBoard();
        } else {
            toast(getString(R.string.n11_sync_error));
        }
        waitingServer = false;
        selected = -1;

        boolean mate = board.currentPlayer().isInCheckMate();
        boolean check = info.isInCheck() || board.currentPlayer().isInCheck();
        appendLog(info.getNotation() + (mate ? "#" : check ? "+" : ""), whiteMoved);

        boardView.setBoard(board);
        boardView.setLastMove(info.getFromSquare(), info.getToSquare());
        boardView.setHighlights(-1, Collections.<Integer>emptyList(), BoardCanvasView.findCheckedKing(board));
        restartMoveClock();
    }

    private void onGameEnded(GameResult result) {
        playing = false;
        waitingServer = false;
        handler.removeCallbacks(tick);
        selected = -1;
        boardView.clearSelection();
        refreshClocks();
        if (leaving) {
            // Người chơi bấm Thoát: đã xin thua, giờ báo Dừng chơi -> Server gửi SERVER_END_SESSION_MATCH
            myControl.sendData(new ObjectWrapper(ObjectWrapper.REQUEST_CONTINUE_MATCH, false));
            return;
        }
        showContinueDialog(result);
    }

    // ============================== Thao tác người chơi ==============================

    private void onSquareTapped(int square) {
        if (!playing || waitingServer || !isMyTurn()) {
            return;
        }
        Piece piece = board.getPiece(square);
        boolean ownPiece = piece != null && piece.getPieceAllegiance().toString().equals(myColor);

        if (selected == -1 || (ownPiece && square != selected)) {
            select(ownPiece ? square : -1);
            return;
        }
        if (square == selected) {
            select(-1);
            return;
        }
        List<Move> candidates = LocalChessGame.legalMovesBetween(board, selected, square);
        if (candidates.isEmpty()) {
            select(-1);
            return;
        }
        final int from = selected;
        if (candidates.get(0) instanceof PawnPromotion) {
            PromotionDialog.show(this, code -> sendMove(from, square, code));
        } else {
            sendMove(from, square, null);
        }
    }

    private void sendMove(int from, int to, String promotion) {
        if (!playing || waitingServer) {
            return;
        }
        boolean sent = myControl.sendData(new ObjectWrapper(ObjectWrapper.SEND_MOVE,
                new MoveRequest(from, to, promotion)));
        if (sent) {
            waitingServer = true;                 // khóa bàn tới khi Server trả lời
            select(-1);
        } else {
            toast(getString(R.string.n11_disconnected));
        }
    }

    private void select(int square) {
        selected = square;
        List<Integer> targets = square == -1
                ? Collections.<Integer>emptyList()
                : LocalChessGame.legalDestinations(board, square);
        boardView.setHighlights(square, targets, BoardCanvasView.findCheckedKing(board));
    }

    private void confirmResign() {
        if (!playing) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.n11_resign)
                .setMessage(R.string.n11_resign_confirm_online)
                .setPositiveButton(R.string.n11_yes, (dlg, w) -> {
                    if (playing) {
                        myControl.sendData(new ObjectWrapper(ObjectWrapper.QUIT_GAME, null));
                    }
                })
                .setNegativeButton(R.string.n11_no, null)
                .show();
    }

    /** Đang chơi: hỏi lại, rồi xin thua + Dừng chơi. Ván đã xong: báo Dừng chơi luôn. */
    private void confirmExit() {
        if (leaving) {
            return;
        }
        if (!playing) {
            leave();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.n11_exit_title)
                .setMessage(R.string.n11_exit_confirm_online)
                .setPositiveButton(R.string.n11_yes, (dlg, w) -> leave())
                .setNegativeButton(R.string.n11_no, null)
                .show();
    }

    private void leave() {
        leaving = true;
        boolean sent = playing
                ? myControl.sendData(new ObjectWrapper(ObjectWrapper.QUIT_GAME, null))
                : myControl.sendData(new ObjectWrapper(ObjectWrapper.REQUEST_CONTINUE_MATCH, false));
        if (!sent) {
            finish();                             // mất kết nối: rời màn hình luôn
        }
    }

    @Override
    public void onContinueDecision(boolean accept) {
        if (!accept) {
            leaving = true;
        }
        myControl.sendData(new ObjectWrapper(ObjectWrapper.REQUEST_CONTINUE_MATCH, accept));
    }

    // ============================== Ván mới & hiển thị ==============================

    /** Ván 1 (SERVER_START_MATCH) và mỗi lần cả 2 chọn Tiếp tục (đã đổi màu). */
    private void startNewGame(String color) {
        myColor = color;
        board = Board.createStandardBoard();
        selected = -1;
        playing = true;
        waitingServer = false;
        plyCount = 0;
        log.setLength(0);
        txtMoveLog.setText("");
        txtMoveLog.scrollTo(0, 0);
        boardView.setFlipped(MatchResult.BLACK.equals(myColor));   // cầm Đen thì quân Đen ở dưới
        boardView.setBoard(board);
        boardView.setLastMove(-1, -1);
        boardView.setHighlights(-1, Collections.<Integer>emptyList(), -1);
        renderNames();
        restartMoveClock();
    }

    /** Trái = Trắng, phải = Đen; tên của mình có thêm "(Bạn)". */
    private void renderNames() {
        String myName = me != null ? getString(R.string.n11_you, me.getUsername()) : getString(R.string.n11_you, "");
        String oppName = opponent != null ? opponent.getUsername() : "";
        boolean iAmWhite = MatchResult.WHITE.equals(myColor);
        txtNameWhite.setText(iAmWhite ? myName : oppName);
        txtNameBlack.setText(iAmWhite ? oppName : myName);
    }

    private void restartMoveClock() {
        moveDeadline = SystemClock.elapsedRealtime() + MOVE_TIME_MS;
        refreshClocks();
        handler.removeCallbacks(tick);
        if (playing) {
            handler.postDelayed(tick, TICK_MS);
        }
    }

    /** Bên đang tới lượt: đếm ngược 30 giây, in đậm; bên kia: 0:30, mờ. */
    private void refreshClocks() {
        boolean whiteToMove = board.currentPlayer().getAlliance() == Alliance.WHITE;
        long left = Math.max(0, moveDeadline - SystemClock.elapsedRealtime());
        styleClock(txtClockWhite, playing && whiteToMove ? left : MOVE_TIME_MS, playing && whiteToMove);
        styleClock(txtClockBlack, playing && !whiteToMove ? left : MOVE_TIME_MS, playing && !whiteToMove);
    }

    private void styleClock(TextView view, long ms, boolean running) {
        long seconds = (ms + 999) / 1000;
        view.setText(String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60));
        view.setTypeface(Typeface.MONOSPACE, running ? Typeface.BOLD : Typeface.NORMAL);
        view.setTextColor(running && ms <= LOW_TIME_MS ? COLOR_LOW_TIME : clockColor);
        view.setAlpha(running ? 1f : 0.5f);
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
            if (log.length() == 0) {
                log.append(String.format(Locale.US, "%-4s%-8s", "1.", "..."));
            }
            log.append(notation);
        }
        txtMoveLog.setText(log);
        txtMoveLog.post(() -> {
            if (txtMoveLog.getLayout() != null) {
                int scroll = txtMoveLog.getLayout().getLineTop(txtMoveLog.getLineCount()) - txtMoveLog.getHeight()
                        + txtMoveLog.getTotalPaddingTop() + txtMoveLog.getTotalPaddingBottom();
                txtMoveLog.scrollTo(0, Math.max(0, scroll));
            }
        });
    }

    private void showContinueDialog(GameResult result) {
        if (getSupportFragmentManager().isStateSaved()) {
            pendingResult = result;               // app ở nền: hiện khi quay lại (onPostResume)
            return;
        }
        dismissContinueDialog();
        ContinueDialog.newInstance(result).show(getSupportFragmentManager(), ContinueDialog.TAG);
    }

    private void dismissContinueDialog() {
        pendingResult = null;
        Fragment f = getSupportFragmentManager().findFragmentByTag(ContinueDialog.TAG);
        if (f instanceof ContinueDialog) {
            ((ContinueDialog) f).dismissAllowingStateLoss();
        }
    }

    private boolean isMyTurn() {
        return board.currentPlayer().getAlliance().toString().equals(myColor);
    }

    private static Piece.PieceType toPieceType(String code) {
        switch (code) {
            case "R": return Piece.PieceType.ROOK;
            case "B": return Piece.PieceType.BISHOP;
            case "N": return Piece.PieceType.KNIGHT;
            default:  return Piece.PieceType.QUEEN;
        }
    }
}
