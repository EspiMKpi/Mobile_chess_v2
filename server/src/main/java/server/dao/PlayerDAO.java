package server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Player;
import model.PlayerRanking;

/** Chủ: N04. Truy vấn tblPlayer (+ điểm, BXH). Xem BLUEPRINT_N04 mục 3.5, 3.6. */
public class PlayerDAO extends DAO {

    /** Đúng username + password (hash) -> điền id, name, email, tel vào p, trả true. */
    public boolean checkLogin(Player p) {
        if (p == null || p.getUsername() == null || p.getPassword() == null) {
            return false;
        }
        Connection connection = getConnection();
        if (connection == null) {
            return false;
        }

        String sql = "SELECT id, name, email, tel FROM tblPlayer WHERE username = ? AND password = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
        } catch (SQLException e) {
            System.err.println("PlayerDAO.checkLogin error: " + e.getMessage());
        }
        return false;
    }

    /** Username đã tồn tại -> false; ngược lại INSERT (password hash). */
    public boolean registerPlayer(Player p) {
        if (p == null || p.getUsername() == null || p.getPassword() == null) {
            return false;
        }
        Connection connection = getConnection();
        if (connection == null) {
            return false;
        }

        String checkSql = "SELECT id FROM tblPlayer WHERE username = ?";
        try (PreparedStatement psCheck = connection.prepareStatement(checkSql)) {
            psCheck.setString(1, p.getUsername().trim());
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    return false; // Đã tồn tại username
                }
            }
        } catch (SQLException e) {
            System.err.println("PlayerDAO.registerPlayer check error: " + e.getMessage());
            return false;
        }

        String insertSql = "INSERT INTO tblPlayer(username, password, name, email, tel) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement psInsert = connection.prepareStatement(insertSql)) {
            psInsert.setString(1, p.getUsername().trim());
            psInsert.setString(2, hashPassword(p.getPassword()));
            psInsert.setString(3, p.getName() != null ? p.getName().trim() : "");
            psInsert.setString(4, p.getEmail() != null ? p.getEmail().trim() : null);
            psInsert.setString(5, p.getTel() != null ? p.getTel().trim() : null);
            return psInsert.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("PlayerDAO.registerPlayer insert error: " + e.getMessage());
            return false;
        }
    }

    /** UPDATE name, email, tel theo id. */
    public boolean editPlayer(Player p) {
        if (p == null || p.getId() <= 0) {
            return false;
        }
        Connection connection = getConnection();
        if (connection == null) {
            return false;
        }

        String sql = "UPDATE tblPlayer SET name = ?, email = ?, tel = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getName() != null ? p.getName().trim() : "");
            ps.setString(2, p.getEmail() != null ? p.getEmail().trim() : null);
            ps.setString(3, p.getTel() != null ? p.getTel().trim() : null);
            ps.setInt(4, p.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("PlayerDAO.editPlayer error: " + e.getMessage());
            return false;
        }
    }

    /** UPDATE password WHERE id=? AND password=hash(oldPass) -> executeUpdate() == 1. */
    public boolean changePassword(int playerId, String oldPass, String newPass) {
        if (playerId <= 0 || oldPass == null || newPass == null) {
            return false;
        }
        Connection connection = getConnection();
        if (connection == null) {
            return false;
        }

        String sql = "UPDATE tblPlayer SET password = ? WHERE id = ? AND password = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hashPassword(newPass));
            ps.setInt(2, playerId);
            ps.setString(3, hashPassword(oldPass));
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("PlayerDAO.changePassword error: " + e.getMessage());
            return false;
        }
    }

    /** WIN = 1, DRAW = 0.5, LOSE = 0. */
    public float getTotalPoint(int playerId) {
        if (playerId <= 0) {
            return 0f;
        }
        Connection connection = getConnection();
        if (connection == null) {
            return 0f;
        }

        String sql = "SELECT COALESCE(SUM(CASE result WHEN 'WIN' THEN 1.0 WHEN 'DRAW' THEN 0.5 ELSE 0 END), 0) AS totalPoint " +
                "FROM tblMatchResult WHERE playerId = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getFloat("totalPoint");
                }
            }
        } catch (SQLException e) {
            System.err.println("PlayerDAO.getTotalPoint error: " + e.getMessage());
        }
        return 0f;
    }

    /**
     * Toàn bộ người chơi, đã sắp theo 4 tiêu chí:
     * totalPoint DESC -> avgOpponentPoint DESC -> avgMovesWin ASC (NULL xếp cuối) -> avgMovesLose DESC (NULL xếp cuối)
     */
    public ArrayList<PlayerRanking> getRanking() {
        ArrayList<PlayerRanking> rankingList = new ArrayList<>();
        Connection connection = getConnection();
        if (connection == null) {
            return rankingList;
        }

        Map<Integer, PlayerRanking> playerMap = new HashMap<>();

        // 1. Lấy tất cả người chơi
        String allPlayersSql = "SELECT id, username, name, email, tel FROM tblPlayer";
        try (PreparedStatement ps = connection.prepareStatement(allPlayersSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PlayerRanking pr = new PlayerRanking();
                pr.setId(rs.getInt("id"));
                pr.setUsername(rs.getString("username"));
                pr.setName(rs.getString("name"));
                pr.setEmail(rs.getString("email"));
                pr.setTel(rs.getString("tel"));
                pr.setTotalPoint(0f);
                pr.setAvgOpponentPoint(0f);
                pr.setAvgMovesWin(-1f);
                pr.setAvgMovesLose(-1f);
                playerMap.put(pr.getId(), pr);
            }
        } catch (SQLException e) {
            System.err.println("PlayerDAO.getRanking load players error: " + e.getMessage());
            return rankingList;
        }

        if (playerMap.isEmpty()) {
            return rankingList;
        }

        // 2. Tính totalPoint cho từng người chơi
        String pointsSql = "SELECT playerId, COALESCE(SUM(CASE result WHEN 'WIN' THEN 1.0 WHEN 'DRAW' THEN 0.5 ELSE 0 END), 0) AS pt " +
                "FROM tblMatchResult GROUP BY playerId";
        try (PreparedStatement ps = connection.prepareStatement(pointsSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int pid = rs.getInt("playerId");
                PlayerRanking pr = playerMap.get(pid);
                if (pr != null) {
                    pr.setTotalPoint(rs.getFloat("pt"));
                }
            }
        } catch (SQLException e) {
            System.err.println("PlayerDAO.getRanking points error: " + e.getMessage());
        }

        // 3. Lấy thông tin các trận đấu: đối thủ và số nước đi (nếu có tblMatchMove)
        Map<Integer, Set<Integer>> distinctOpponentsMap = new HashMap<>();
        Map<Integer, List<Integer>> winMovesMap = new HashMap<>();
        Map<Integer, List<Integer>> loseMovesMap = new HashMap<>();

        for (Integer pid : playerMap.keySet()) {
            distinctOpponentsMap.put(pid, new HashSet<>());
            winMovesMap.put(pid, new ArrayList<>());
            loseMovesMap.put(pid, new ArrayList<>());
        }

        String matchDetailsSql =
                "SELECT r1.playerId AS myId, r1.matchId, r1.result, r2.playerId AS opponentId, " +
                "       COALESCE(m.moveCount, 0) AS moveCount " +
                "FROM tblMatchResult r1 " +
                "JOIN tblMatchResult r2 ON r1.matchId = r2.matchId AND r1.playerId <> r2.playerId " +
                "LEFT JOIN ( " +
                "    SELECT matchId, COUNT(*) AS moveCount " +
                "    FROM tblMatchMove " +
                "    GROUP BY matchId " +
                ") m ON m.matchId = r1.matchId";

        try (PreparedStatement ps = connection.prepareStatement(matchDetailsSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int myId = rs.getInt("myId");
                int oppId = rs.getInt("opponentId");
                String result = rs.getString("result");
                int moveCount = rs.getInt("moveCount");

                Set<Integer> opponents = distinctOpponentsMap.get(myId);
                if (opponents != null) {
                    opponents.add(oppId);
                }

                if ("WIN".equalsIgnoreCase(result)) {
                    List<Integer> wins = winMovesMap.get(myId);
                    if (wins != null) {
                        wins.add(moveCount);
                    }
                } else if ("LOSE".equalsIgnoreCase(result)) {
                    List<Integer> loses = loseMovesMap.get(myId);
                    if (loses != null) {
                        loses.add(moveCount);
                    }
                }
            }
        } catch (SQLException e) {
            // Trường hợp tblMatchMove chưa được tạo, thử lại truy vấn không có tblMatchMove
            String fallbackSql =
                    "SELECT r1.playerId AS myId, r1.matchId, r1.result, r2.playerId AS opponentId " +
                    "FROM tblMatchResult r1 " +
                    "JOIN tblMatchResult r2 ON r1.matchId = r2.matchId AND r1.playerId <> r2.playerId";
            try (PreparedStatement ps = connection.prepareStatement(fallbackSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int myId = rs.getInt("myId");
                    int oppId = rs.getInt("opponentId");
                    String result = rs.getString("result");

                    Set<Integer> opponents = distinctOpponentsMap.get(myId);
                    if (opponents != null) {
                        opponents.add(oppId);
                    }

                    if ("WIN".equalsIgnoreCase(result)) {
                        List<Integer> wins = winMovesMap.get(myId);
                        if (wins != null) {
                            wins.add(0);
                        }
                    } else if ("LOSE".equalsIgnoreCase(result)) {
                        List<Integer> loses = loseMovesMap.get(myId);
                        if (loses != null) {
                            loses.add(0);
                        }
                    }
                }
            } catch (SQLException ex) {
                System.err.println("PlayerDAO.getRanking match details fallback error: " + ex.getMessage());
            }
        }

        // 4. Tính toán avgOpponentPoint, avgMovesWin, avgMovesLose
        for (PlayerRanking pr : playerMap.values()) {
            int pid = pr.getId();

            // avgOpponentPoint: trung bình totalPoint của các đối thủ khác nhau đã gặp
            Set<Integer> opponents = distinctOpponentsMap.get(pid);
            if (opponents != null && !opponents.isEmpty()) {
                float sumOppPoint = 0f;
                for (int oppId : opponents) {
                    PlayerRanking opp = playerMap.get(oppId);
                    if (opp != null) {
                        sumOppPoint += opp.getTotalPoint();
                    }
                }
                pr.setAvgOpponentPoint(sumOppPoint / opponents.size());
            } else {
                pr.setAvgOpponentPoint(0f);
            }

            // avgMovesWin: trung bình số nước của các ván thắng, chưa thắng -> -1
            List<Integer> wins = winMovesMap.get(pid);
            if (wins != null && !wins.isEmpty()) {
                float sumMoves = 0f;
                for (int m : wins) {
                    sumMoves += m;
                }
                pr.setAvgMovesWin(sumMoves / wins.size());
            } else {
                pr.setAvgMovesWin(-1f);
            }

            // avgMovesLose: trung bình số nước của các ván thua, chưa thua -> -1
            List<Integer> loses = loseMovesMap.get(pid);
            if (loses != null && !loses.isEmpty()) {
                float sumMoves = 0f;
                for (int m : loses) {
                    sumMoves += m;
                }
                pr.setAvgMovesLose(sumMoves / loses.size());
            } else {
                pr.setAvgMovesLose(-1f);
            }

            rankingList.add(pr);
        }

        // 5. Sắp xếp theo 4 tiêu chí
        Collections.sort(rankingList, new Comparator<PlayerRanking>() {
            @Override
            public int compare(PlayerRanking p1, PlayerRanking p2) {
                // 1. totalPoint DESC
                int cmp = Float.compare(p2.getTotalPoint(), p1.getTotalPoint());
                if (cmp != 0) return cmp;

                // 2. avgOpponentPoint DESC
                cmp = Float.compare(p2.getAvgOpponentPoint(), p1.getAvgOpponentPoint());
                if (cmp != 0) return cmp;

                // 3. avgMovesWin ASC (NULL / -1 xếp cuối)
                float w1 = p1.getAvgMovesWin();
                float w2 = p2.getAvgMovesWin();
                if (w1 >= 0 && w2 >= 0) {
                    cmp = Float.compare(w1, w2);
                    if (cmp != 0) return cmp;
                } else if (w1 >= 0 && w2 < 0) {
                    return -1;
                } else if (w1 < 0 && w2 >= 0) {
                    return 1;
                }

                // 4. avgMovesLose DESC (NULL / -1 xếp cuối)
                float l1 = p1.getAvgMovesLose();
                float l2 = p2.getAvgMovesLose();
                if (l1 >= 0 && l2 >= 0) {
                    cmp = Float.compare(l2, l1);
                    if (cmp != 0) return cmp;
                } else if (l1 >= 0 && l2 < 0) {
                    return -1;
                } else if (l1 < 0 && l2 >= 0) {
                    return 1;
                }

                return Integer.compare(p1.getId(), p2.getId());
            }
        });

        return rankingList;
    }
}
