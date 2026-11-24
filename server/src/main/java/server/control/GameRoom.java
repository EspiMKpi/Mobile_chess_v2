package server.control;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import model.GameResult;
import model.Match;
import model.MatchResult;
import model.MatchStartInfo;
import model.ObjectWrapper;
import server.dao.MatchDAO;

/**
 * [PHẦN N11 THAY THẾ ĐỂ TỰ KIỂM THỬ TRÊN NHÁNH FULL]
 *
 * Khi N11 hoàn thiện module đấu đối kháng, N11 sẽ thay thế phần xử lý bên trong bằng
 * ChessReferee (kiểm tra nước đi hợp lệ, đồng hồ 30 giây, chiếu bí/hết nước) và lưu tblMatchMove.
 * Hiện tại lớp này giả lập đủ: vào trận, xin thua (QUIT_GAME), mất kết nối, lưu MatchDAO,
 * hỏi chơi tiếp đổi màu (REQUEST_CONTINUE_MATCH) và trả người chơi về Lobby (onMatchSessionEnded).
 */
public class GameRoom implements MatchSession {

    private final ServerCtr myServer;
    private ServerCtr.ServerProcessing whitePlayer;
    private ServerCtr.ServerProcessing blackPlayer;
    private Match currentMatch;
    private boolean playing;
    private boolean closed;
    private final Set<ServerCtr.ServerProcessing> continueVotes = new HashSet<>();

    public GameRoom(ServerCtr myServer,
                    ServerCtr.ServerProcessing challenger,
                    ServerCtr.ServerProcessing challenged) {
        this.myServer = myServer;
        this.blackPlayer = challenger;   // Ván 1: người mời cầm Đen (đi sau)
        this.whitePlayer = challenged;   // Ván 1: người được mời cầm Trắng (đi trước)
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
        if (closed) {
            return;
        }
        int code = packet.getPerformative();
        if (code == ObjectWrapper.QUIT_GAME) {
            ServerCtr.ServerProcessing winner = (sender == whitePlayer) ? blackPlayer : whitePlayer;
            endGame(winner, GameResult.RESIGN);
        } else if (code == ObjectWrapper.REQUEST_CONTINUE_MATCH && packet.getData() instanceof Boolean) {
            handleContinueDecision(sender, (Boolean) packet.getData());
        }
    }

    @Override
    public synchronized void onDisconnect(ServerCtr.ServerProcessing player) {
        if (closed) {
            return;
        }
        ServerCtr.ServerProcessing opponent = (player == whitePlayer) ? blackPlayer : whitePlayer;
        if (playing) {
            endGame(opponent, GameResult.DISCONNECT);
        }
        if (opponent != null) {
            opponent.sendData(new ObjectWrapper(ObjectWrapper.SERVER_END_SESSION_MATCH, "OPPONENT_STOP"));
        }
        closeSession();
    }

    private void beginNewMatch() {
        currentMatch = new Match(new Date());
        continueVotes.clear();
        playing = true;
    }

    private void endGame(ServerCtr.ServerProcessing winner, String reason) {
        if (!playing || currentMatch == null) {
            return;
        }
        playing = false;
        currentMatch.setEndTime(new Date());

        String whiteRes = winner == null ? MatchResult.DRAW : (winner == whitePlayer ? MatchResult.WIN : MatchResult.LOSE);
        String blackRes = winner == null ? MatchResult.DRAW : (winner == blackPlayer ? MatchResult.WIN : MatchResult.LOSE);

        currentMatch.addResult(new MatchResult(whitePlayer.getLoggedInPlayer(), whiteRes, MatchResult.WHITE));
        currentMatch.addResult(new MatchResult(blackPlayer.getLoggedInPlayer(), blackRes, MatchResult.BLACK));
        new MatchDAO().saveMatch(currentMatch);

        whitePlayer.sendData(new ObjectWrapper(
                ObjectWrapper.SERVER_END_GAME, new GameResult(whiteRes, reason, MatchResult.WHITE, toPoint(whiteRes))));
        blackPlayer.sendData(new ObjectWrapper(
                ObjectWrapper.SERVER_END_GAME, new GameResult(blackRes, reason, MatchResult.BLACK, toPoint(blackRes))));
    }

    private void handleContinueDecision(ServerCtr.ServerProcessing sender, boolean accept) {
        if (playing || closed) {
            return;
        }
        if (!accept) {
            ServerCtr.ServerProcessing other = (sender == whitePlayer) ? blackPlayer : whitePlayer;
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

    private void closeSession() {
        if (closed) {
            return;
        }
        closed = true;
        playing = false;
        myServer.onMatchSessionEnded(this, whitePlayer, blackPlayer);
    }

    private static float toPoint(String result) {
        if (MatchResult.WIN.equals(result)) return 1f;
        if (MatchResult.DRAW.equals(result)) return 0.5f;
        return 0f;
    }
}
