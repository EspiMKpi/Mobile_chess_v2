package server.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import model.MatchMove;

/** Chủ: N11. Ghi biên bản nước đi vào tblMatchMove. */
public class MatchMoveDAO {

    /**
     * Dùng chung Connection (đang trong transaction của MatchDAO.saveMatch) - KHÔNG commit/rollback ở đây.
     */
    public void insertMoves(Connection con, int matchId, List<MatchMove> moves) throws SQLException {
        // TODO(N11): INSERT INTO tblMatchMove(matchId, moveIndex, fromSquare, toSquare, notation, promotion)
        //            dùng PreparedStatement + addBatch/executeBatch
    }
}
