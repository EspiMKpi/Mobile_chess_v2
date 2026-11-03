package server.dao;

import model.Match;

/** Chủ: N04. */
public class MatchDAO extends DAO {

    /**
     * Lưu 1 ván trong MỘT transaction: tblMatch -> 2 tblMatchResult ->
     * new MatchMoveDAO().insertMoves(con, matchId, match.getMoves()) (phần của N11) -> commit.
     */
    public boolean saveMatch(Match match) {
        // TODO(N04)
        return false;
    }

    // TODO(N04): getMatchesByPlayer(Player) cho Lịch sử đấu
}
