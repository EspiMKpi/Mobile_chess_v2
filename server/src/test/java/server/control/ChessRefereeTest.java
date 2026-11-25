package server.control;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.chess.engine.board.BoardUtils;
import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.pieces.PieceUtils;
import model.GameResult;
import model.MatchMove;
import model.MoveInfo;
import model.MoveRequest;
import model.ObjectWrapper;

/**
 * Kiểm tra ChessReferee không cần mạng/CSDL: GameRoom và ServerProcessing được thay bằng bản giả
 * ghi lại gói tin gửi đi và lời gọi endGame.
 */
public class ChessRefereeTest {

    /** GameRoom giả: đang chơi cho tới khi endGame được gọi. */
    static class FakeRoom extends GameRoom {
        boolean playing = true;
        ServerCtr.ServerProcessing winner;
        String reason;
        int endGameCalls;

        FakeRoom(ServerCtr ctr, ServerCtr.ServerProcessing challenger, ServerCtr.ServerProcessing challenged) {
            super(ctr, challenger, challenged);   // ván 1: challenger cầm Đen, challenged cầm Trắng
        }

        @Override
        public synchronized void endGame(ServerCtr.ServerProcessing winner, String reason) {
            if (!playing) {
                return;
            }
            playing = false;
            endGameCalls++;
            this.winner = winner;
            this.reason = reason;
        }

        @Override
        public synchronized boolean isPlaying() {
            return playing;
        }
    }

    private final ServerCtr ctr = new ServerCtr(null, 0, 0);
    private final List<ObjectWrapper> whiteBox = new ArrayList<>();
    private final List<ObjectWrapper> blackBox = new ArrayList<>();
    private ServerCtr.ServerProcessing white;
    private ServerCtr.ServerProcessing black;
    private FakeRoom room;
    private ChessReferee ref;

    private ServerCtr.ServerProcessing fakeClient(List<ObjectWrapper> inbox) {
        return ctr.new ServerProcessing(null) {
            @Override
            public synchronized void sendData(Object obj) {
                inbox.add((ObjectWrapper) obj);
            }
        };
    }

    @Before
    public void setUp() {
        white = fakeClient(whiteBox);
        black = fakeClient(blackBox);
        room = new FakeRoom(ctr, black, white);
        ref = new ChessReferee(room);
        ref.reset();
    }

    @After
    public void tearDown() {
        ref.stopTimer();
    }

    private static int sq(String s) {
        return BoardUtils.INSTANCE.getCoordinateAtPosition(s);
    }

    private static MoveRequest mv(String from, String to) {
        return new MoveRequest(sq(from), sq(to), null);
    }

    private static Object lastData(List<ObjectWrapper> inbox, int performative) {
        for (int i = inbox.size() - 1; i >= 0; i--) {
            if (inbox.get(i).getPerformative() == performative) {
                return inbox.get(i).getData();
            }
        }
        return null;
    }

    private void setField(String name, Object value) throws Exception {
        Field f = ChessReferee.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(ref, value);
    }

    @Test
    public void wrongTurnOnlyRepliesToSender() {
        ref.handleMove(black, mv("e7", "e5"));
        assertEquals("Chưa tới lượt của bạn", lastData(blackBox, ObjectWrapper.REPLY_MOVE_ILLEGAL));
        assertTrue(whiteBox.isEmpty());
    }

    @Test
    public void illegalMoveIsRejected() {
        ref.handleMove(white, mv("e2", "e5"));
        assertEquals("Nước đi không hợp lệ", lastData(whiteBox, ObjectWrapper.REPLY_MOVE_ILLEGAL));
        assertTrue(ref.getMoves().isEmpty());
    }

    @Test
    public void legalMoveIsBroadcastToBoth() {
        ref.handleMove(white, mv("f2", "f3"));
        MoveInfo info = (MoveInfo) lastData(blackBox, ObjectWrapper.SERVER_INFORM_MOVE);
        assertNotNull(info);
        assertEquals("f3", info.getNotation());
        assertEquals("f3", ((MoveInfo) lastData(whiteBox, ObjectWrapper.SERVER_INFORM_MOVE)).getNotation());
        assertEquals("BLACK", info.getSideToMove());
    }

    @Test
    public void foolsMateEndsGameAndRecordsMoves() {
        ref.handleMove(white, mv("f2", "f3"));
        ref.handleMove(black, mv("e7", "e5"));
        ref.handleMove(white, mv("g2", "g4"));
        ref.handleMove(black, mv("d8", "h4"));
        MoveInfo mate = (MoveInfo) lastData(whiteBox, ObjectWrapper.SERVER_INFORM_MOVE);
        assertEquals("Qh4", mate.getNotation());           // nước chiếu được gửi trước kết quả
        assertTrue(mate.isInCheck());
        assertEquals(GameResult.CHECKMATE, room.reason);
        assertSame(black, room.winner);
        List<MatchMove> moves = ref.getMoves();
        assertEquals(4, moves.size());
        assertEquals(4, moves.get(3).getMoveIndex());
        assertEquals("Qh4", moves.get(3).getNotation());

        int before = whiteBox.size();
        ref.handleMove(white, mv("a2", "a3"));              // ván đã kết thúc -> bỏ qua
        assertEquals(before, whiteBox.size());
    }

    @Test
    public void timeoutIgnoresStaleTimerThenEndsGameOnce() throws Exception {
        ref.onMoveTimeout();                                 // chưa đủ 30s -> hẹn giờ cũ, bỏ qua
        assertTrue(room.isPlaying());
        setField("turnStartedAt", System.nanoTime() - (ChessReferee.MOVE_TIME_LIMIT_MS + 1) * 1_000_000);
        ref.onMoveTimeout();
        assertEquals(GameResult.TIMEOUT, room.reason);
        assertSame(black, room.winner);                      // Trắng tới lượt mà hết giờ -> Đen thắng
        ref.onMoveTimeout();
        assertEquals(1, room.endGameCalls);
    }

    @Test
    public void promotionRequiresPieceChoice() throws Exception {
        // Thế cờ: Tốt Trắng ở e7, Vua Trắng ở e1, Vua Đen ở h8; tới lượt Trắng
        Board promoBoard = new Board.Builder()
                .setPiece(PieceUtils.INSTANCE.getPawn(Alliance.WHITE, sq("e7"), true))
                .setPiece(PieceUtils.INSTANCE.getKing(Alliance.WHITE, sq("e1"), false, true))
                .setPiece(PieceUtils.INSTANCE.getKing(Alliance.BLACK, sq("h8"), false, true))
                .setMoveMaker(Alliance.WHITE)
                .build();
        setField("board", promoBoard);
        ref.handleMove(white, mv("e7", "e8"));
        assertEquals("Nước đi không hợp lệ", lastData(whiteBox, ObjectWrapper.REPLY_MOVE_ILLEGAL));
        ref.handleMove(white, new MoveRequest(sq("e7"), sq("e8"), "N"));
        MoveInfo promo = (MoveInfo) lastData(whiteBox, ObjectWrapper.SERVER_INFORM_MOVE);
        assertEquals("e8=N", promo.getNotation());
        assertEquals("N", ref.getMoves().get(0).getPromotion());
    }
}
