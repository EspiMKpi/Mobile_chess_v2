package client.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.pieces.Piece;

public class BoardCanvasView extends View {

    public interface OnSquareTapListener {
        void onSquareTapped(int square);
    }

    private static final int COLOR_LIGHT = 0xFFEFD9B4;
    private static final int COLOR_DARK = 0xFFB48963;
    private static final int COLOR_SELECTED = 0xFF29A3D8;
    private static final int COLOR_TARGET = 0xB32E7D32;      // xanh lá, hơi trong suốt
    private static final int COLOR_CHECK = 0xCCE53935;       // đỏ, hơi trong suốt
    private static final int COLOR_LAST_MOVE = 0x66F6E05E;   // vàng nhạt

    // Ký tự Unicode theo thứ tự P N B R Q K. Bộ "đặc" dùng làm thân quân, bộ "rỗng" làm viền.
    // ︎ ép hiện dạng chữ: không có nó, ♟ trên một số máy Android hiện thành emoji màu.
    private static final String[] SOLID = {"♟", "♞", "♝", "♜", "♛", "♚"};
    private static final String[] OUTLINE = {"♙", "♘", "♗", "♖", "♕", "♔"};
    private static final String TEXT_STYLE = "︎";

    private Board board;
    private boolean flipped;
    private int selected = -1;
    private final List<Integer> targets = new ArrayList<>();
    private int checkSquare = -1;
    private int lastFrom = -1;
    private int lastTo = -1;
    private boolean showCoordinates = true;
    private OnSquareTapListener listener;

    private final Paint lightPaint = new Paint();
    private final Paint darkPaint = new Paint();
    private final Paint checkPaint = new Paint();
    private final Paint lastMovePaint = new Paint();
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coordPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    public BoardCanvasView(Context context) {
        this(context, null);
    }

    public BoardCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        lightPaint.setColor(COLOR_LIGHT);
        darkPaint.setColor(COLOR_DARK);
        checkPaint.setColor(COLOR_CHECK);
        lastMovePaint.setColor(COLOR_LAST_MOVE);

        selectedPaint.setColor(COLOR_SELECTED);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(3 * density);

        targetPaint.setColor(COLOR_TARGET);
        targetRingPaint.setColor(COLOR_TARGET);
        targetRingPaint.setStyle(Paint.Style.STROKE);
        targetRingPaint.setStrokeWidth(3 * density);

        pieceFillPaint.setTextAlign(Paint.Align.CENTER);
        pieceLinePaint.setTextAlign(Paint.Align.CENTER);
        pieceLinePaint.setColor(Color.BLACK);

        coordPaint.setTypeface(Typeface.DEFAULT_BOLD);
        coordPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10,
                getResources().getDisplayMetrics()));

        setClickable(true);
        setContentDescription("Bàn cờ");
        if (isInEditMode()) {
            board = Board.createStandardBoard();     // xem trước được trong cửa sổ Design của Android Studio
        }
    }


    public void setBoard(Board board) {
        this.board = board;
        invalidate();
    }

    public Board getBoard() {
        return board;
    }

    /** true = nhìn từ phía Đen (dùng khi người chơi cầm Đen). */
    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        invalidate();
    }

    public boolean isFlipped() {
        return flipped;
    }

    /**
     * @param selected    ô đang chọn (viền xanh), -1 nếu không chọn
     * @param targets     các ô đích hợp lệ (chấm xanh; ô có quân thì vẽ vòng tròn)
     * @param checkSquare ô Vua đang bị chiếu (tô đỏ), -1 nếu không
     */
    public void setHighlights(int selected, List<Integer> targets, int checkSquare) {
        this.selected = selected;
        this.targets.clear();
        if (targets != null) {
            this.targets.addAll(targets);
        }
        this.checkSquare = checkSquare;
        invalidate();
    }

    /** Bỏ viền chọn và chấm ô đích (giữ nguyên ô bị chiếu và nước vừa đi). */
    public void clearSelection() {
        selected = -1;
        targets.clear();
        invalidate();
    }

    /** Tô vàng nhạt ô đi và ô đến của nước vừa đi; truyền -1, -1 để bỏ. */
    public void setLastMove(int from, int to) {
        lastFrom = from;
        lastTo = to;
        invalidate();
    }

    public void setShowCoordinates(boolean show) {
        showCoordinates = show;
        invalidate();
    }

    public void setOnSquareTapListener(OnSquareTapListener listener) {
        this.listener = listener;
    }

    /** Ô của Vua bên tới lượt nếu đang bị chiếu, ngược lại -1 (tiện dùng cho setHighlights). */
    public static int findCheckedKing(Board board) {
        if (board == null || !board.currentPlayer().isInCheck()) {
            return -1;
        }
        return board.currentPlayer().getPlayerKing().getPiecePosition();
    }

    // ============================== Đo, vẽ, chạm ==============================

    /** Bàn luôn vuông: cạnh = min(chiều rộng, chiều cao được phép). */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int size = heightMode == MeasureSpec.UNSPECIFIED || height == 0 ? width : Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cell = getWidth() / 8f;
        if (cell <= 0) {
            return;
        }
        pieceFillPaint.setTextSize(cell * 0.82f);
        pieceLinePaint.setTextSize(cell * 0.82f);
        Paint.FontMetrics fm = pieceFillPaint.getFontMetrics();
        float pieceOffset = -(fm.ascent + fm.descent) / 2f;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int sq = toSquare(row, col);
                float left = col * cell;
                float top = row * cell;
                float cx = left + cell / 2f;
                float cy = top + cell / 2f;
                boolean light = ((sq / 8) + (sq % 8)) % 2 == 0;

                // 1. nền ô + các lớp tô
                canvas.drawRect(left, top, left + cell, top + cell, light ? lightPaint : darkPaint);
                if (sq == lastFrom || sq == lastTo) {
                    canvas.drawRect(left, top, left + cell, top + cell, lastMovePaint);
                }
                if (sq == checkSquare) {
                    canvas.drawRect(left, top, left + cell, top + cell, checkPaint);
                }
                if (sq == selected) {
                    float inset = selectedPaint.getStrokeWidth() / 2f;
                    canvas.drawRect(left + inset, top + inset, left + cell - inset, top + cell - inset, selectedPaint);
                }

                // 2. quân cờ
                Piece piece = board == null ? null : board.getPiece(sq);
                if (piece != null) {
                    drawPiece(canvas, piece, cx, cy + pieceOffset);
                }

                // 3. gợi ý ô đích: ô trống = chấm, ô có quân (ăn được) = vòng tròn
                if (targets.contains(sq)) {
                    if (piece == null) {
                        canvas.drawCircle(cx, cy, cell * 0.16f, targetPaint);
                    } else {
                        canvas.drawCircle(cx, cy, cell / 2f - targetRingPaint.getStrokeWidth(), targetRingPaint);
                    }
                }

                // 4. tọa độ a-h ở hàng dưới cùng, 1-8 ở cột trái (theo hướng đang nhìn)
                if (showCoordinates) {
                    coordPaint.setColor(light ? COLOR_DARK : COLOR_LIGHT);
                    float pad = 2.5f * density;
                    if (row == 7) {
                        coordPaint.setTextAlign(Paint.Align.RIGHT);
                        canvas.drawText(String.valueOf((char) ('a' + sq % 8)), left + cell - pad, top + cell - pad, coordPaint);
                    }
                    if (col == 0) {
                        coordPaint.setTextAlign(Paint.Align.LEFT);
                        canvas.drawText(String.valueOf(8 - sq / 8), left + pad, top + pad - coordPaint.ascent(), coordPaint);
                    }
                }
            }
        }
    }

    /** Quân Trắng: thân trắng + viền đen để nổi trên mọi màu ô; quân Đen: thân đen. */
    private void drawPiece(Canvas canvas, Piece piece, float x, float baseline) {
        int idx = "PNBRQK".indexOf(piece.getPieceType().toString());
        if (idx < 0) {
            return;
        }
        if (piece.getPieceAllegiance() == Alliance.WHITE) {
            pieceFillPaint.setColor(Color.WHITE);
            canvas.drawText(SOLID[idx] + TEXT_STYLE, x, baseline, pieceFillPaint);
            canvas.drawText(OUTLINE[idx] + TEXT_STYLE, x, baseline, pieceLinePaint);
        } else {
            pieceFillPaint.setColor(Color.BLACK);
            canvas.drawText(SOLID[idx] + TEXT_STYLE, x, baseline, pieceFillPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return true;                                  // phải nhận DOWN thì mới nhận được UP
            case MotionEvent.ACTION_UP: {
                float cell = getWidth() / 8f;
                if (cell <= 0) {
                    return true;
                }
                int col = clamp((int) (event.getX() / cell));
                int row = clamp((int) (event.getY() / cell));
                if (listener != null) {
                    listener.onSquareTapped(toSquare(row, col));
                }
                performClick();
                return true;
            }
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int toSquare(int row, int col) {
        int sq = row * 8 + col;
        return flipped ? 63 - sq : sq;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(7, v));
    }
}
