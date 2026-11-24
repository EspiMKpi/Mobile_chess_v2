package server.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import model.Player;

/** Chủ: N04. Truy vấn tblPlayer và tổng điểm (hình 4.2b, 4.10b). */
public class PlayerDAO extends DAO {

    public PlayerDAO() {
        super();
    }

    /**
     * Đăng nhập (hình 4.4, bước 9-13): băm mật khẩu, SELECT theo username + mật khẩu băm,
     * đúng thì điền id, name, email, tel vào p rồi trả true.
     */
    public boolean checkLogin(Player p) {
        if (con == null) {
            return false;
        }
        String sql = "SELECT id, name, email, tel FROM tblPlayer WHERE username = ? AND password = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getUsername());
            ps.setString(2, hashPassword(p.getPassword()));           // bước 9
            try (ResultSet rs = ps.executeQuery()) {                   // bước 10
                if (rs.next()) {
                    p.setId(rs.getInt("id"));                          // bước 11
                    p.setName(rs.getString("name"));
                    p.setEmail(rs.getString("email"));
                    p.setTel(rs.getString("tel"));
                    return true;                                       // bước 13
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Đăng ký (hình 4.3, bước 12-15): username đã có -> false;
     * chưa có -> băm mật khẩu, INSERT bản ghi mới, trả true.
     */
    public boolean registerPlayer(Player p) {
        if (con == null) {
            return false;
        }
        String checkSql = "SELECT id FROM tblPlayer WHERE username = ?";
        try (PreparedStatement ps = con.prepareStatement(checkSql)) {  // bước 12
            ps.setString(1, p.getUsername());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        String insertSql = "INSERT INTO tblPlayer(username, password, name, email, tel) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(insertSql)) {
            ps.setString(1, p.getUsername());
            ps.setString(2, hashPassword(p.getPassword()));           // bước 13
            ps.setString(3, p.getName());
            ps.setString(4, p.getEmail());
            ps.setString(5, p.getTel());
            return ps.executeUpdate() == 1;                            // bước 14-15
        } catch (SQLException e) {
            // Hai người đăng ký cùng username cùng lúc -> ràng buộc UNIQUE chặn lại
            e.printStackTrace();
            return false;
        }
    }

    /** Tổng điểm (hình 4.10b): WIN = 1, DRAW = 0.5, LOSE = 0. Chưa chơi ván nào -> 0. */
    public float getTotalPoint(int playerId) {
        if (con == null) {
            return 0f;
        }
        String sql = "SELECT COALESCE(SUM(CASE result WHEN 'WIN' THEN 1 WHEN 'DRAW' THEN 0.5 ELSE 0 END), 0) "
                + "FROM tblMatchResult WHERE playerId = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getFloat(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0f;
    }

    /**
     * TODO(N04 - Hình 4.5): Cập nhật họ tên, email, số điện thoại cho người chơi có mã `id`.
     * SQL: UPDATE tblPlayer SET name = ?, email = ?, tel = ? WHERE id = ?
     * Trả true nếu executeUpdate() == 1.
     */
    public boolean editPlayer(int id, Player p) {
        // TODO(N04)
        return false;
    }

    /**
     * TODO(N04 - Hình 4.6): Đổi mật khẩu khi mật khẩu hiện tại đúng.
     * Băm cả oldPass và newPass bằng hashPassword(...).
     * SQL: UPDATE tblPlayer SET password = ? WHERE id = ? AND password = ?
     * Trả true nếu executeUpdate() == 1 (sai mật khẩu hiện tại -> trả false).
     */
    public boolean changePassword(int id, String oldPass, String newPass) {
        // TODO(N04)
        return false;
    }

    /**
     * TODO(N04 - Hình 4.8 & Mục 1.3): Tính bảng xếp hạng toàn bộ người chơi trong tblPlayer.
     * Cần lấy 4 chỉ số cho mỗi người:
     *  1. totalPoint: tổng điểm (WIN = 1.0, DRAW = 0.5, LOSE = 0; chưa chơi -> 0)
     *  2. avgOpponentPoint: trung bình tổng điểm hiện tại của đối thủ theo TỪNG VÁN đã đấu (chưa chơi -> 0)
     *  3. avgMovesWin: trung bình số nước DO CHÍNH NGƯỜI ĐÓ ĐI trong các trận THẮNG
     *     (từ tblMatchMove: Trắng là moveIndex lẻ, Đen là moveIndex chẵn; chưa thắng trận nào -> -1f để app hiện "-")
     *  4. avgMovesLose: tương tự cho các trận THUA (chưa thua trận nào -> -1f để app hiện "-")
     *
     * Thứ tự sắp xếp:
     *  totalPoint DESC -> avgOpponentPoint DESC -> avgMovesWin ASC (-1 xếp sau)
     *  -> avgMovesLose DESC (-1 xếp sau) -> username ASC.
     */
    public java.util.ArrayList<model.PlayerRanking> getRanking() {
        // TODO(N04)
        return new java.util.ArrayList<>();
    }
}
