package server.control;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import model.GameResult;
import model.Match;
import model.MatchResult;
import model.MatchStartInfo;
import model.MoveRequest;
import model.ObjectWrapper;
import server.dao.MatchDAO;

/**
 * Chủ: N11. Phòng đấu của một cặp người chơi (cài đặt MatchSession của N04).
 * - Luật cờ, lượt đi, đồng hồ 30 giây mỗi nước, chiếu bí / hết nước: ChessReferee.
 * - Kết thúc ván (chiếu bí, hết nước, hết giờ, xin thua, mất kết nối): endGame() lưu DB (MatchDAO.saveMatch
 *   ghi cả tblMatchMove) rồi gửi SERVER_END_GAME riêng cho từng người.
 * - Sau mỗi ván: chờ 2 người chọn Tiếp tục / Dừng trong 30 giây; cả 2 Tiếp tục thì đổi màu, ván mới.
 *
 * QUY TẮC KHÓA: mọi trạng thái phòng + trọng tài đồng bộ trên CHÍNH đối tượng GameRoom này
 * (method synchronized ở đây, synchronized (room) bên ChessReferee) -> luồng 2 người chơi và
 * luồng Timer không thể làm endGame chạy 2 lần cho cùng một ván.
 */
public class GameRoom implements MatchSession {

    public static final long CONTINUE_TIMEOUT_MS = 30_000;

    private final ServerCtr myServer;
    private ServerCtr.ServerProcessing whitePlayer;
    private ServerCtr.ServerProcessing blackPlayer;
    private Match currentMatch;
    private boolean playing;
    private boolean closed;
    private final Set<ServerCtr.ServerProcessing> continueVotes = new HashSet<>();
    private final ChessReferee referee;
    private Timer continueTimer;

    public GameRoom(ServerCtr myServer,
                    ServerCtr.ServerProcessing challenger,
                    ServerCtr.ServerProcessing challenged) {
        this.myServer = myServer;
        this.blackPlayer = challenger;   // Ván 1: người mời cầm Đen (đi sau)
        this.whitePlayer = challenged;   // Ván 1: người được mời cầm Trắng (đi trước)
        this.referee = new ChessReferee(this);
    }

    @Override
    public synchronized void start() {
        beginNewMatch();
        MatchStartInfo infoWhite = new MatchStartInfo(
                MatchResult.WHITE, myServer.getPlayerStat(whitePlayer), myServer.getPlayerStat(blackPlayer));
        MatchStartInfo infoBlack = new MatchStartInfo(
                MatchResult.BLACK, myServer.getPlayerStat(blackPlayer), myServer.getPlayerStat(whitePlayer));
        whitePlayer.sendData(new ObjectWrapper(ObjectWrapper.SERVER_START_MATCH, infoWhite));
        blackPlayer.sendData(new ObjectWrapper(ObjectWrapper.SERVER_START_MATCH, infoBlack));
    }

    @Override
    public synchronized void onPacket(ServerCtr.ServerProcessing sender, ObjectWrapper packet) {
        if (closed || packet == null) {
            return;
        }
        switch (packet.getPerformative()) {
            case ObjectWrapper.SEND_MOVE:
                if (packet.getData() instanceof MoveRequest) {
                    referee.handleMove(sender, (MoveRequest) packet.getData());
                }
                break;
            case ObjectWrapper.QUIT_GAME:
                if (playing) {
                    endGame(opponentOf(sender), GameResult.RESIGN);
                }
                break;
            case ObjectWrapper.REQUEST_CONTINUE_MATCH:
                if (packet.getData() instanceof Boolean) {
                    handleContinueDecision(sender, (Boolean) packet.getData());
                }
                break;
            default:
                break;
        }
    }

    @Override
    public synchronized void onDisconnect(ServerCtr.ServerProcessing player) {
        if (closed) {
            return;
        }
        ServerCtr.ServerProcessing opponent = opponentOf(player);
        if (playing) {
            endGame(opponent, GameResult.DISCONNECT);
        }
        if (opponent != null) {
            opponent.sendData(new ObjectWrapper(ObjectWrapper.SERVER_END_SESSION_MATCH, "OPPONENT_STOP"));
        }
        closeSession();
    }

    /**
     * Kết thúc ván. Gọi bởi phòng (RESIGN, DISCONNECT) và ChessReferee (CHECKMATE, STALEMATE, TIMEOUT).
     * Idempotent: ván đã kết thúc thì bỏ qua (tránh hết giờ và xin thua cùng lúc bị lưu 2 lần).
     * @param winner người thắng, null nếu hòa
     * @param reason một hằng số trong GameResult
     */
    public synchronized void endGame(ServerCtr.ServerProcessing winner, String reason) {
        if (!playing || currentMatch == null) {
            return;
        }
        playing = false;
        referee.stopTimer();
        currentMatch.setEndTime(new Date());

        String whiteRes = winner == null ? MatchResult.DRAW : (winner == whitePlayer ? MatchResult.WIN : MatchResult.LOSE);
        String blackRes = winner == null ? MatchResult.DRAW : (winner == blackPlayer ? MatchResult.WIN : MatchResult.LOSE);

        currentMatch.addResult(new MatchResult(whitePlayer.getLoggedInPlayer(), whiteRes, MatchResult.WHITE));
        currentMatch.addResult(new MatchResult(blackPlayer.getLoggedInPlayer(), blackRes, MatchResult.BLACK));
        currentMatch.getMoves().addAll(referee.getMoves());     // moveIndex lẻ = Trắng, chẵn = Đen
        new MatchDAO().saveMatch(currentMatch);

        whitePlayer.sendData(new ObjectWrapper(
                ObjectWrapper.SERVER_END_GAME, new GameResult(whiteRes, reason, MatchResult.WHITE, toPoint(whiteRes))));
        blackPlayer.sendData(new ObjectWrapper(
                ObjectWrapper.SERVER_END_GAME, new GameResult(blackRes, reason, MatchResult.BLACK, toPoint(blackRes))));
        startContinueTimer();
    }

    /** ChessReferee kiểm tra trước khi xử lý nước đi / hết giờ. */
    public synchronized boolean isPlaying() {
        return playing;
    }

    public ServerCtr.ServerProcessing getWhitePlayer() { return whitePlayer; }
    public ServerCtr.ServerProcessing getBlackPlayer() { return blackPlayer; }

    // ============================== nội bộ ==============================

    private void beginNewMatch() {
        currentMatch = new Match(new Date());
        continueVotes.clear();
        cancelContinueTimer();
        playing = true;
        referee.reset();                 // bàn chuẩn, xóa biên bản, bật đồng hồ 30s cho Trắng
    }

    private void handleContinueDecision(ServerCtr.ServerProcessing sender, boolean accept) {
        if (playing || closed) {
            return;
        }
        if (!accept) {
            ServerCtr.ServerProcessing other = opponentOf(sender);
            sender.sendData(new ObjectWrapper(ObjectWrapper.SERVER_END_SESSION_MATCH, "STOP"));
            other.sendData(new ObjectWrapper(ObjectWrapper.SERVER_END_SESSION_MATCH, "OPPONENT_STOP"));
            closeSession();
            return;
        }
        continueVotes.add(sender);
        if (continueVotes.size() == 2) {
            ServerCtr.ServerProcessing tmp = whitePlayer;
            whitePlayer = blackPlayer;
            blackPlayer = tmp;
            beginNewMatch();
            whitePlayer.sendData(new ObjectWrapper(ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR, MatchResult.WHITE));
            blackPlayer.sendData(new ObjectWrapper(ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR, MatchResult.BLACK));
        }
    }

    /** Hết 30 giây chờ chọn: người chưa chọn coi như Dừng; người đã chọn Tiếp tục nhận "đối thủ đã dừng". */
    private synchronized void onContinueTimeout() {
        if (closed || playing) {
            return;
        }
        for (ServerCtr.ServerProcessing sp : new ServerCtr.ServerProcessing[]{whitePlayer, blackPlayer}) {
            sp.sendData(new ObjectWrapper(ObjectWrapper.SERVER_END_SESSION_MATCH,
                    continueVotes.contains(sp) ? "OPPONENT_STOP" : "STOP"));
        }
        closeSession();
    }

    private void startContinueTimer() {
        cancelContinueTimer();
        continueTimer = new Timer("continue-timer", true);
        continueTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                onContinueTimeout();
            }
        }, CONTINUE_TIMEOUT_MS);
    }

    private void cancelContinueTimer() {
        if (continueTimer != null) {
            continueTimer.cancel();
            continueTimer = null;
        }
    }

    private void closeSession() {
        if (closed) {
            return;
        }
        closed = true;
        playing = false;
        referee.stopTimer();
        cancelContinueTimer();
        myServer.onMatchSessionEnded(this, whitePlayer, blackPlayer);
    }

    private ServerCtr.ServerProcessing opponentOf(ServerCtr.ServerProcessing sp) {
        return sp == whitePlayer ? blackPlayer : whitePlayer;
    }

    private static float toPoint(String result) {
        if (MatchResult.WIN.equals(result)) return 1f;
        if (MatchResult.DRAW.equals(result)) return 0.5f;
        return 0f;
    }
}
