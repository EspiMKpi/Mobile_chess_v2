package server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import model.Match;
import model.MatchResult;
import model.Player;

/** Chủ: N04. */
public class MatchDAO extends DAO {

    /**
     * Lưu 1 ván trong MỘT transaction: tblMatch -> 2 tblMatchResult ->
     * new MatchMoveDAO().insertMoves(con, matchId, match.getMoves()) (phần của N11) -> commit.
     */
    public synchronized boolean saveMatch(Match match) {
        if (match == null || match.getResults() == null || match.getResults().isEmpty()) {
            return false;
        }
        Connection connection = getConnection();
        if (connection == null) {
            return false;
        }

        boolean oldAutoCommit = true;
        try {
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            // 1. Lưu tblMatch
            String matchSql = "INSERT INTO tblMatch(startTime, endTime) VALUES (?, ?)";
            int matchId = 0;
            try (PreparedStatement psMatch = connection.prepareStatement(matchSql, Statement.RETURN_GENERATED_KEYS)) {
                long startMs = (match.getStartTime() != null) ? match.getStartTime().getTime() : System.currentTimeMillis();
                long endMs = (match.getEndTime() != null) ? match.getEndTime().getTime() : startMs;

                psMatch.setTimestamp(1, new Timestamp(startMs));
                psMatch.setTimestamp(2, new Timestamp(endMs));
                psMatch.executeUpdate();

                try (ResultSet rs = psMatch.getGeneratedKeys()) {
                    if (rs.next()) {
                        matchId = rs.getInt(1);
                        match.setId(matchId);
                    }
                }
            }

            if (matchId == 0) {
                connection.rollback();
                return false;
            }

            // 2. Lưu tblMatchResult (thường 2 dòng)
            String resultSql = "INSERT INTO tblMatchResult(matchId, playerId, result, color) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psRes = connection.prepareStatement(resultSql)) {
                for (MatchResult mr : match.getResults()) {
                    if (mr != null && mr.getPlayer() != null) {
                        psRes.setInt(1, matchId);
                        psRes.setInt(2, mr.getPlayer().getId());
                        psRes.setString(3, mr.getResult() != null ? mr.getResult() : "DRAW");
                        psRes.setString(4, mr.getColor() != null ? mr.getColor() : "WHITE");
                        psRes.addBatch();
                    }
                }
                psRes.executeBatch();
            }

            // 3. Lưu tblMatchMove qua MatchMoveDAO của N11
            if (match.getMoves() != null && !match.getMoves().isEmpty()) {
                try {
                    new MatchMoveDAO().insertMoves(connection, matchId, match.getMoves());
                } catch (Exception e) {
                    System.err.println("MatchDAO.saveMatch insertMoves note: " + e.getMessage());
                }
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("MatchDAO.saveMatch error: " + e.getMessage());
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(oldAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Lấy danh sách lịch sử trận đấu của 1 người chơi.
     * Mỗi Match chứa 2 MatchResult (của người chơi và của đối thủ).
     */
    public ArrayList<Match> getMatchesByPlayer(int playerId) {
        ArrayList<Match> matchList = new ArrayList<>();
        if (playerId <= 0) {
            return matchList;
        }
        Connection connection = getConnection();
        if (connection == null) {
            return matchList;
        }

        String sql =
                "SELECT m.id AS match_id, m.startTime, m.endTime, " +
                "       r.id AS res_id, r.result, r.color, " +
                "       p.id AS p_id, p.username, p.name " +
                "FROM tblMatch m " +
                "JOIN tblMatchResult r ON r.matchId = m.id " +
                "JOIN tblPlayer p ON p.id = r.playerId " +
                "WHERE m.id IN (SELECT matchId FROM tblMatchResult WHERE playerId = ?) " +
                "ORDER BY m.startTime DESC, m.id DESC";

        Map<Integer, Match> matchMap = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int matchId = rs.getInt("match_id");
                    Match match = matchMap.get(matchId);
                    if (match == null) {
                        match = new Match();
                        match.setId(matchId);
                        Timestamp startTs = rs.getTimestamp("startTime");
                        if (startTs != null) {
                            match.setStartTime(new java.util.Date(startTs.getTime()));
                        }
                        Timestamp endTs = rs.getTimestamp("endTime");
                        if (endTs != null) {
                            match.setEndTime(new java.util.Date(endTs.getTime()));
                        }
                        matchMap.put(matchId, match);
                    }

                    Player player = new Player();
                    player.setId(rs.getInt("p_id"));
                    player.setUsername(rs.getString("username"));
                    player.setName(rs.getString("name"));

                    MatchResult mr = new MatchResult(player, rs.getString("result"), rs.getString("color"));
                    mr.setId(rs.getInt("res_id"));
                    match.addResult(mr);
                }
            }
        } catch (SQLException e) {
            System.err.println("MatchDAO.getMatchesByPlayer error: " + e.getMessage());
        }

        matchList.addAll(matchMap.values());
        return matchList;
    }
}
