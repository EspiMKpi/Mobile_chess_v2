package server.dao;

import java.util.ArrayList;

import model.Match;

/** Chủ: N04. Lưu ván đấu và truy vấn lịch sử đấu. */
public class MatchDAO extends DAO {

    /**
     * Lưu ván (hình 4.13, bước 8-10) trong MỘT transaction, dùng openNewConnection() riêng:
     * 1. INSERT tblMatch(startTime, endTime), lấy id sinh ra
     * 2. INSERT 2 dòng tblMatchResult(matchId, playerId, result, color)
     * 3. new MatchMoveDAO().insertMoves(connection, matchId, match.getMoves())  (phần N11)
     * 4. commit; lỗi -> rollback, trả false
     */
    public boolean saveMatch(Match match) {
        // TODO
        return false;
    }

    /**
     * Lịch sử đấu (hình 4.7, bước 7-12): JOIN tblMatch + tblMatchResult + tblPlayer theo mã người chơi,
     * mới nhất ở trên. Mỗi Match chứa đủ 2 MatchResult (của mình và của đối thủ).
     */
    public ArrayList<Match> getMatchesByPlayer(int playerId) {
        // TODO
        return new ArrayList<>();
    }
}
