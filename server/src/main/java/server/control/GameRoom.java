package server.control;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import model.GameResult;
import model.Match;
import model.MatchResult;
import model.MatchStartInfo;
import model.MoveRequest;
import model.ObjectWrapper;
import model.PlayerStat;
import server.dao.MatchDAO;
import server.dao.PlayerDAO;

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
    private final ServerCtr.ServerProcessing challenger;   // người thách (ván 1 cầm Đen)
    private final ServerCtr.ServerProcessing challenged;   // người được thách (ván 1 cầm Trắng)
    private ServerCtr.ServerProcessing whitePlayer;
    private ServerCtr.ServerProcessing blackPlayer;
    private Match currentMatch;
    private final Set<ServerCtr.ServerProcessing> continueVotes = new HashSet<>();
    private boolean playing;               // true từ startGame/ván mới tới endGame
    private boolean closed;
    private final ChessReferee referee;

    /** Tạo khi A thách đấu B đang rảnh. Ván 1: người thách đấu (A) cầm Đen, người được thách (B) cầm Trắng. */
    public GameRoom(ServerCtr myServer, ServerCtr.ServerProcessing challenger,
                    ServerCtr.ServerProcessing challenged) {
        this.myServer = myServer;
        this.challenger = challenger;
        this.challenged = challenged;
        this.blackPlayer = challenger;
        this.whitePlayer = challenged;
        this.referee = new ChessReferee(this);
    }

    /** B trả lời lời mời (REPLY_CHALLENGE_DECISION). true -> startGame(); false -> closeRoom() + REPLY_CHALLENGE_REJECT cho A. */
    public synchronized void receiveChallengeDecision(boolean accept) {
        receiveChallengeDecision(challenged, accept);
    }

    /** Chỉ hợp lệ khi đang chờ trả lời (chưa có ván nào). */
    public synchronized void receiveChallengeDecision(ServerCtr.ServerProcessing sender, boolean accept) {
        if (closed || currentMatch != null) return;          // đã vào ván / đã đóng -> bỏ qua
        if (sender == challenged) {
            if (accept) {
                startGame();
                return;
            }
            // Từ chối: closeRoom + REPLY_CHALLENGE_REJECT cho challenger
            ServerCtr.ServerProcessing ch = challenger;
            PlayerStat challengedStat = (challenged != null && challenged.getLoggedInPlayer() != null)
                    ? new PlayerStat(challenged.getLoggedInPlayer(), 0f, PlayerStat.IDLE)
                    : null;
            closeRoom();
            if (ch != null) {
                ch.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_REJECT, challengedStat));
            }
        } else if (sender == challenger && !accept) {
            // Người thách hủy lời mời: báo cả hai rồi đóng phòng
            if (challenged != null) {
                challenged.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_REJECT, null));
            }
            if (challenger != null) {
                challenger.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_REJECT, null));
            }
            closeRoom();
        }
    }

    /** Ván 1: new Match, referee.reset(), playing = true, broadcastOnlineList, gửi SERVER_START_MATCH cho 2 bên. */
    public synchronized void startGame() {
        beginNewMatch();
        if (myServer != null) {
            myServer.broadcastOnlineList();
        }

        PlayerDAO playerDAO = new PlayerDAO();
        float whitePt = (whitePlayer != null && whitePlayer.getLoggedInPlayer() != null)
                ? playerDAO.getTotalPoint(whitePlayer.getLoggedInPlayer().getId()) : 0f;
        float blackPt = (blackPlayer != null && blackPlayer.getLoggedInPlayer() != null)
                ? playerDAO.getTotalPoint(blackPlayer.getLoggedInPlayer().getId()) : 0f;

        PlayerStat whiteStat = (whitePlayer != null && whitePlayer.getLoggedInPlayer() != null)
                ? new PlayerStat(whitePlayer.getLoggedInPlayer(), whitePt, PlayerStat.BUSY) : null;
        PlayerStat blackStat = (blackPlayer != null && blackPlayer.getLoggedInPlayer() != null)
                ? new PlayerStat(blackPlayer.getLoggedInPlayer(), blackPt, PlayerStat.BUSY) : null;

        if (blackPlayer != null) {
            blackPlayer.sendData(new ObjectWrapper(ObjectWrapper.SERVER_START_MATCH,
                    new MatchStartInfo(MatchResult.BLACK, whiteStat)));
        }
        if (whitePlayer != null) {
            whitePlayer.sendData(new ObjectWrapper(ObjectWrapper.SERVER_START_MATCH,
                    new MatchStartInfo(MatchResult.WHITE, blackStat)));
        }
    }

    private void beginNewMatch() {
        currentMatch = new Match(new Date());
        continueVotes.clear();
        playing = true;
        if (referee != null) {
            referee.reset();
        }
    }

    /** Nhận SEND_MOVE từ ServerProcessing -> chuyển cho trọng tài (N11). */
    public void handleMove(ServerCtr.ServerProcessing sender, MoveRequest req) {
        if (referee != null) {
            referee.handleMove(sender, req);
        }
    }

    /**
     * Kết thúc ván. Gọi bởi N04 (RESIGN, DISCONNECT) và N11 (CHECKMATE, STALEMATE, TIMEOUT).
     * Idempotent: nếu !playing thì return ngay (tránh hết giờ và xin thua cùng lúc bị lưu 2 lần).
     * @param winner người thắng, null nếu hòa
     * @param reason một hằng số trong GameResult
     */
    public synchronized void endGame(ServerCtr.ServerProcessing winner, String reason) {
        if (!playing) {
            return;
        }
        playing = false;
        if (referee != null) {
            referee.stopTimer();
        }

        if (currentMatch == null) {
            currentMatch = new Match(new Date());
        }
        currentMatch.setEndTime(new Date());

        String whiteResult;
        float whitePoint;
        String blackResult;
        float blackPoint;

        if (winner == null) {
            whiteResult = MatchResult.DRAW;
            whitePoint = 0.5f;
            blackResult = MatchResult.DRAW;
            blackPoint = 0.5f;
        } else if (winner == whitePlayer) {
            whiteResult = MatchResult.WIN;
            whitePoint = 1.0f;
            blackResult = MatchResult.LOSE;
            blackPoint = 0.0f;
        } else {
            whiteResult = MatchResult.LOSE;
            whitePoint = 0.0f;
            blackResult = MatchResult.WIN;
            blackPoint = 1.0f;
        }

        if (whitePlayer != null && whitePlayer.getLoggedInPlayer() != null) {
            currentMatch.addResult(new MatchResult(whitePlayer.getLoggedInPlayer(), whiteResult, MatchResult.WHITE));
        }
        if (blackPlayer != null && blackPlayer.getLoggedInPlayer() != null) {
            currentMatch.addResult(new MatchResult(blackPlayer.getLoggedInPlayer(), blackResult, MatchResult.BLACK));
        }

        if (referee != null) {
            currentMatch.getMoves().addAll(referee.getMoves());
        }

        new MatchDAO().saveMatch(currentMatch);

        if (whitePlayer != null) {
            whitePlayer.sendData(new ObjectWrapper(ObjectWrapper.SERVER_END_GAME,
                    new GameResult(whiteResult, reason, MatchResult.WHITE, whitePoint)));
        }
        if (blackPlayer != null) {
            blackPlayer.sendData(new ObjectWrapper(ObjectWrapper.SERVER_END_GAME,
                    new GameResult(blackResult, reason, MatchResult.BLACK, blackPoint)));
        }
    }

    /** REQUEST_CONTINUE_MATCH. Đủ 2 người chơi khác nhau bỏ phiếu "Tiếp tục" -> swapColor + ván mới; có 1 "Dừng" -> closeRoom + END_SESSION. */
    public synchronized void receiveContinueDecision(ServerCtr.ServerProcessing sp, boolean accept) {
        if (closed || playing || currentMatch == null) return;          // chỉ hợp lệ giữa 2 ván
        if (sp != whitePlayer && sp != blackPlayer) return;
        if (!accept) {
            if (sp != null) {
                sp.sendData(new ObjectWrapper(ObjectWrapper.SERVER_END_SESSION_MATCH, "STOP"));
            }
            ServerCtr.ServerProcessing opponent = (sp == whitePlayer) ? blackPlayer : whitePlayer;
            if (opponent != null) {
                opponent.sendData(new ObjectWrapper(ObjectWrapper.SERVER_END_SESSION_MATCH, "OPPONENT_STOP"));
            }
            closeRoom();
            return;
        }

        continueVotes.add(sp);
        if (continueVotes.size() == 2) {
            swapColor();
            beginNewMatch();
            if (whitePlayer != null) {
                whitePlayer.sendData(new ObjectWrapper(ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR, MatchResult.WHITE));
            }
            if (blackPlayer != null) {
                blackPlayer.sendData(new ObjectWrapper(ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR, MatchResult.BLACK));
            }
        }
    }

    public synchronized void swapColor() {
        ServerCtr.ServerProcessing tmp = whitePlayer;
        whitePlayer = blackPlayer;
        blackPlayer = tmp;
    }

    /** referee.stopTimer(); 2 bên về IDLE, setRoom(null); myServer.removeRoom(this). */
    public synchronized void closeRoom() {
        if (closed) return;
        closed = true;
        playing = false;
        if (referee != null) {
            referee.stopTimer();
        }
        if (whitePlayer != null) {
            whitePlayer.setRoom(null);
            whitePlayer.setStatus(PlayerStat.IDLE);
        }
        if (blackPlayer != null) {
            blackPlayer.setRoom(null);
            blackPlayer.setStatus(PlayerStat.IDLE);
        }
        if (myServer != null) {
            myServer.removeRoom(this);
            myServer.broadcastOnlineList();
        }
    }

    /**
     * Xử lý khi một người chơi ngắt kết nối trong phòng.
     * Đang chờ trả lời: gửi từ chối cho người còn lại. Đã vào ván: endGame do DISCONNECT.
     */
    public synchronized void playerLeft(ServerCtr.ServerProcessing sp) {
        ServerCtr.ServerProcessing opponent = (sp == whitePlayer) ? blackPlayer : whitePlayer;
        if (currentMatch == null) {
            if (opponent != null) {
                opponent.sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_REJECT,
                        (sp != null) ? sp.getLoggedInPlayer() : null));
            }
        } else {
            if (playing) {
                endGame(opponent, GameResult.DISCONNECT);
            }
            if (opponent != null) {
                opponent.sendData(new ObjectWrapper(ObjectWrapper.SERVER_END_SESSION_MATCH, "OPPONENT_STOP"));
            }
        }
        closeRoom();
    }

    /** ChessReferee kiểm tra trước khi xử lý nước đi / hết giờ. */
    public synchronized boolean isPlaying() { return playing; }

    public ServerCtr.ServerProcessing getWhitePlayer() { return whitePlayer; }
    public ServerCtr.ServerProcessing getBlackPlayer() { return blackPlayer; }
    public Match getCurrentMatch() { return currentMatch; }
}
