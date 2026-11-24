package server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Player;
import model.PlayerRanking;

/** Chủ: N04. Truy vấn tblPlayer, điểm tổng hợp và bảng xếp hạng. */
public class PlayerDAO extends DAO {

    /**
     * Thứ tự BXH theo báo cáo mục 1.3:
     * 1. totalPoint giảm dần
     * 2. avgOpponentPoint giảm dần
     * 3. avgMovesWin tăng dần (-1 / chưa có trận thắng xếp sau)
     * 4. avgMovesLose giảm dần (-1 / chưa có trận thua xếp sau)
     * 5. username tăng dần
     */
    public static final Comparator<PlayerRanking> RANKING_ORDER = (p1, p2) -> {
        int cmp = Float.compare(p2.getTotalPoint(), p1.getTotalPoint());
        if (cmp != 0) return cmp;

        cmp = Float.compare(p2.getAvgOpponentPoint(), p1.getAvgOpponentPoint());
        if (cmp != 0) return cmp;

        float w1 = p1.getAvgMovesWin();
        float w2 = p2.getAvgMovesWin();
        if (w1 >= 0 && w2 >= 0) {
            cmp = Float.compare(w1, w2);
            if (cmp != 0) return cmp;
        } else if (w1 >= 0) {
            return -1;
        } else if (w2 >= 0) {
            return 1;
        }

        float l1 = p1.getAvgMovesLose();
        float l2 = p2.getAvgMovesLose();
        if (l1 >= 0 && l2 >= 0) {
            cmp = Float.compare(l2, l1);
            if (cmp != 0) return cmp;
        } else if (l1 >= 0) {
            return -1;
        } else if (l2 >= 0) {
            return 1;
        }

        String u1 = p1.getUsername() != null ? p1.getUsername() : "";
        String u2 = p2.getUsername() != null ? p2.getUsername() : "";
        return u1.compareToIgnoreCase(u2);
    };

    public boolean checkLogin(Player p) {
        if (p == null || p.getUsername() == null || p.getPassword() == null) {
            return false;
        }
        try (Connection con = openNewConnection()) {
            if (con == null) {
                return false;
            }
            String sql = "SELECT id, name, email, tel FROM tblPlayer WHERE username = ? AND password = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, p.getUsername().trim());
                ps.setString(2, hashPassword(p.getPassword()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        p.setId(rs.getInt("id"));
                        p.setName(rs.getString("name"));
                        p.setEmail(rs.getString("email"));
                        p.setTel(rs.getString("tel"));
                        p.setPassword(null);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("PlayerDAO.checkLogin error: " + e.getMessage());
        }
        return false;
    }

    public boolean registerPlayer(Player p) {
        if (p == null || p.getUsername() == null || p.getPassword() == null) {
            return false;
        }
        try (Connection con = openNewConnection()) {
            if (con == null) {
                return false;
            }
            String checkSql = "SELECT id FROM tblPlayer WHERE username = ?";
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setString(1, p.getUsername().trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return false;
                    }
                }
            }

            String insertSql = "INSERT INTO tblPlayer(username, password, name, email, tel) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setString(1, p.getUsername().trim());
                ps.setString(2, hashPassword(p.getPassword()));
                ps.setString(3, p.getName() != null ? p.getName().trim() : "");
                ps.setString(4, p.getEmail() != null ? p.getEmail().trim() : null);
                ps.setString(5, p.getTel() != null ? p.getTel().trim() : null);
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.println("PlayerDAO.registerPlayer error: " + e.getMessage());
            return false;
        }
    }

    public boolean editPlayer(int id, Player p) {
        if (p == null || p.getName() == null || p.getName().trim().isEmpty()) {
            return false;
        }
        try (Connection con = openNewConnection()) {
            if (con == null) {
                return false;
            }
            String sql = "UPDATE tblPlayer SET name = ?, email = ?, tel = ? WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, p.getName().trim());
                ps.setString(2, p.getEmail() != null ? p.getEmail().trim() : null);
                ps.setString(3, p.getTel() != null ? p.getTel().trim() : null);
                ps.setInt(4, id);
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.println("PlayerDAO.editPlayer error: " + e.getMessage());
            return false;
        }
    }

    public boolean changePassword(int id, String oldPass, String newPass) {
        if (oldPass == null || newPass == null || newPass.isEmpty()) {
            return false;
        }
        try (Connection con = openNewConnection()) {
            if (con == null) {
                return false;
            }
            String sql = "UPDATE tblPlayer SET password = ? WHERE id = ? AND password = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, hashPassword(newPass));
                ps.setInt(2, id);
                ps.setString(3, hashPassword(oldPass));
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.println("PlayerDAO.changePassword error: " + e.getMessage());
            return false;
        }
    }

    public float getTotalPoint(int playerId) {
        try (Connection con = openNewConnection()) {
            if (con == null) {
                return 0f;
            }
            String sql = "SELECT COALESCE(SUM(CASE result WHEN 'WIN' THEN 1.0 WHEN 'DRAW' THEN 0.5 ELSE 0 END), 0) "
                    + "FROM tblMatchResult WHERE playerId = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getFloat(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("PlayerDAO.getTotalPoint error: " + e.getMessage());
        }
        return 0f;
    }

    public ArrayList<PlayerRanking> getRanking() {
        ArrayList<PlayerRanking> rankingList = new ArrayList<>();
        try (Connection con = openNewConnection()) {
            if (con == null) {
                return rankingList;
            }

            Map<Integer, PlayerRanking> playerMap = new HashMap<>();
            Map<Integer, List<Integer>> matchOpponentsMap = new HashMap<>();
            Map<Integer, List<Integer>> winMovesMap = new HashMap<>();
            Map<Integer, List<Integer>> loseMovesMap = new HashMap<>();

            String playerSql =
                    "SELECT p.id, p.username, p.name, " +
                    "COALESCE(SUM(CASE r.result WHEN 'WIN' THEN 1.0 WHEN 'DRAW' THEN 0.5 ELSE 0 END), 0) AS totalPoint " +
                    "FROM tblPlayer p " +
                    "LEFT JOIN tblMatchResult r ON r.playerId = p.id " +
                    "GROUP BY p.id, p.username, p.name";
            try (PreparedStatement ps = con.prepareStatement(playerSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    PlayerRanking pr = new PlayerRanking();
                    pr.setId(id);
                    pr.setUsername(rs.getString("username"));
                    pr.setName(rs.getString("name"));
                    pr.setTotalPoint(rs.getFloat("totalPoint"));
                    playerMap.put(id, pr);
                    matchOpponentsMap.put(id, new ArrayList<>());
                    winMovesMap.put(id, new ArrayList<>());
                    loseMovesMap.put(id, new ArrayList<>());
                }
            }

            String matchSql =
                    "SELECT r1.playerId AS myId, r1.result, r2.playerId AS opponentId, " +
                    "COALESCE(SUM(CASE WHEN (r1.color = 'WHITE' AND mm.moveIndex % 2 = 1) " +
                    "OR (r1.color = 'BLACK' AND mm.moveIndex % 2 = 0) THEN 1 ELSE 0 END), 0) AS ownMoves " +
                    "FROM tblMatchResult r1 " +
                    "JOIN tblMatchResult r2 ON r1.matchId = r2.matchId AND r1.playerId <> r2.playerId " +
                    "LEFT JOIN tblMatchMove mm ON mm.matchId = r1.matchId " +
                    "GROUP BY r1.id, r1.playerId, r1.result, r1.color, r2.playerId";
            try (PreparedStatement ps = con.prepareStatement(matchSql);
                 ResultSet rs = ps.executeQuery()) {
                collectMatchRows(rs, true, matchOpponentsMap, winMovesMap, loseMovesMap);
            } catch (SQLException e) {
                // Nếu N11 chưa tạo bảng tblMatchMove, fallback với số nước = 0
                String fallbackSql =
                        "SELECT r1.playerId AS myId, r1.result, r2.playerId AS opponentId " +
                        "FROM tblMatchResult r1 " +
                        "JOIN tblMatchResult r2 ON r1.matchId = r2.matchId AND r1.playerId <> r2.playerId";
                try (PreparedStatement ps = con.prepareStatement(fallbackSql);
                     ResultSet rs = ps.executeQuery()) {
                    collectMatchRows(rs, false, matchOpponentsMap, winMovesMap, loseMovesMap);
                }
            }

            for (PlayerRanking pr : playerMap.values()) {
                int pid = pr.getId();
                List<Integer> opps = matchOpponentsMap.get(pid);
                if (opps != null && !opps.isEmpty()) {
                    float sumOpp = 0f;
                    for (int oppId : opps) {
                        PlayerRanking opp = playerMap.get(oppId);
                        if (opp != null) {
                            sumOpp += opp.getTotalPoint();
                        }
                    }
                    pr.setAvgOpponentPoint(sumOpp / opps.size());
                } else {
                    pr.setAvgOpponentPoint(0f);
                }

                pr.setAvgMovesWin(averageOrMinusOne(winMovesMap.get(pid)));
                pr.setAvgMovesLose(averageOrMinusOne(loseMovesMap.get(pid)));
                rankingList.add(pr);
            }

            rankingList.sort(RANKING_ORDER);
        } catch (SQLException e) {
            System.err.println("PlayerDAO.getRanking error: " + e.getMessage());
        }
        return rankingList;
    }

    private static void collectMatchRows(ResultSet rs, boolean hasOwnMoves,
                                         Map<Integer, List<Integer>> matchOpponentsMap,
                                         Map<Integer, List<Integer>> winMovesMap,
                                         Map<Integer, List<Integer>> loseMovesMap) throws SQLException {
        while (rs.next()) {
            int myId = rs.getInt("myId");
            int oppId = rs.getInt("opponentId");
            String result = rs.getString("result");
            int ownMoves = hasOwnMoves ? rs.getInt("ownMoves") : 0;

            List<Integer> opps = matchOpponentsMap.get(myId);
            if (opps != null) {
                opps.add(oppId);
            }
            if ("WIN".equalsIgnoreCase(result)) {
                List<Integer> wins = winMovesMap.get(myId);
                if (wins != null) wins.add(ownMoves);
            } else if ("LOSE".equalsIgnoreCase(result)) {
                List<Integer> loses = loseMovesMap.get(myId);
                if (loses != null) loses.add(ownMoves);
            }
        }
    }

    private static float averageOrMinusOne(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return -1f;
        }
        float sum = 0f;
        for (int v : values) {
            sum += v;
        }
        return sum / values.size();
    }
}
