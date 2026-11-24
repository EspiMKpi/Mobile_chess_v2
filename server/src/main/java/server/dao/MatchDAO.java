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
import model.MatchMove;
import model.MatchResult;
import model.Player;

/** Chủ: N04. Truy vấn lịch sử đấu và hỗ trợ lưu ván đấu theo transaction. */
public class MatchDAO extends DAO {

    public ArrayList<Match> getMatchesByPlayer(int playerId) {
        ArrayList<Match> matchList = new ArrayList<>();
        try (Connection con = openNewConnection()) {
            if (con == null) {
                return matchList;
            }
            String sql =
                    "SELECT m.id AS match_id, m.startTime, m.endTime, " +
                    "r.id AS res_id, r.result, r.color, " +
                    "p.id AS p_id, p.username, p.name " +
                    "FROM tblMatch m " +
                    "JOIN tblMatchResult r ON r.matchId = m.id " +
                    "JOIN tblPlayer p ON p.id = r.playerId " +
                    "WHERE m.id IN (SELECT matchId FROM tblMatchResult WHERE playerId = ?) " +
                    "ORDER BY m.startTime DESC, m.id DESC, r.id ASC";

            Map<Integer, Match> matchMap = new LinkedHashMap<>();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
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
            }
            matchList.addAll(matchMap.values());
        } catch (SQLException e) {
            System.err.println("MatchDAO.getMatchesByPlayer error: " + e.getMessage());
        }
        return matchList;
    }

    /**
     * Lưu một ván đấu trong 1 transaction bằng Connection riêng:
     * tblMatch -> 2 dòng tblMatchResult -> danh sách tblMatchMove (nếu có).
     */
    public boolean saveMatch(Match match) {
        if (match == null || match.getResults() == null || match.getResults().isEmpty()) {
            return false;
        }
        try (Connection con = openNewConnection()) {
            if (con == null) {
                return false;
            }
            con.setAutoCommit(false);
            try {
                String matchSql = "INSERT INTO tblMatch(startTime, endTime) VALUES (?, ?)";
                int matchId = 0;
                try (PreparedStatement psMatch = con.prepareStatement(matchSql, Statement.RETURN_GENERATED_KEYS)) {
                    long startMs = match.getStartTime() != null ? match.getStartTime().getTime() : System.currentTimeMillis();
                    long endMs = match.getEndTime() != null ? match.getEndTime().getTime() : startMs;
                    psMatch.setTimestamp(1, new Timestamp(startMs));
                    psMatch.setTimestamp(2, new Timestamp(endMs));
                    psMatch.executeUpdate();
                    try (ResultSet keys = psMatch.getGeneratedKeys()) {
                        if (keys.next()) {
                            matchId = keys.getInt(1);
                            match.setId(matchId);
                        }
                    }
                }
                if (matchId == 0) {
                    con.rollback();
                    return false;
                }

                String resSql = "INSERT INTO tblMatchResult(matchId, playerId, result, color) VALUES (?, ?, ?, ?)";
                try (PreparedStatement psRes = con.prepareStatement(resSql)) {
                    for (MatchResult mr : match.getResults()) {
                        psRes.setInt(1, matchId);
                        psRes.setInt(2, mr.getPlayer().getId());
                        psRes.setString(3, mr.getResult());
                        psRes.setString(4, mr.getColor());
                        psRes.addBatch();
                    }
                    psRes.executeBatch();
                }

                if (match.getMoves() != null && !match.getMoves().isEmpty()) {
                    String moveSql = "INSERT INTO tblMatchMove(matchId, moveIndex, fromSquare, toSquare, notation, promotion) "
                            + "VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement psMove = con.prepareStatement(moveSql)) {
                        for (MatchMove mm : match.getMoves()) {
                            psMove.setInt(1, matchId);
                            psMove.setInt(2, mm.getMoveIndex());
                            psMove.setInt(3, mm.getFromSquare());
                            psMove.setInt(4, mm.getToSquare());
                            psMove.setString(5, mm.getNotation());
                            psMove.setString(6, mm.getPromotion());
                            psMove.addBatch();
                        }
                        psMove.executeBatch();
                    }
                }

                con.commit();
                return true;
            } catch (SQLException e) {
                System.err.println("MatchDAO.saveMatch error: " + e.getMessage());
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
                return false;
            }
        } catch (SQLException e) {
            System.err.println("MatchDAO.saveMatch close error: " + e.getMessage());
            return false;
        }
    }
}
