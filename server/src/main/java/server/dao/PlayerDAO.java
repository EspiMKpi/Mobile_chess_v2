package server.dao;

import java.util.ArrayList;
import java.util.Comparator;

import model.Player;
import model.PlayerRanking;

/** Chủ: N04. Truy vấn tblPlayer, tổng điểm, bảng xếp hạng. */
public class PlayerDAO extends DAO {

    /**
     * Thứ tự BXH (báo cáo mục 1.3):
     * 1. totalPoint giảm dần
     * 2. avgOpponentPoint giảm dần
     * 3. avgMovesWin tăng dần (chưa thắng trận nào = -1 -> xếp sau)
     * 4. avgMovesLose giảm dần (chưa thua trận nào = -1 -> xếp sau)
     * 5. Bằng nhau cả 4 tiêu chí -> theo tên đăng nhập (username tăng dần)
     */
    public static final Comparator<PlayerRanking> RANKING_ORDER = (p1, p2) -> {
        // TODO
        return 0;
    };

    /**
     * Đăng nhập (hình 4.4, bước 9-13): băm mật khẩu SHA-256, SELECT tblPlayer theo username + password băm.
     * Đúng -> gọi setter điền id, name, email, tel vào p, xóa password, trả true.
     */
    public boolean checkLogin(Player p) {
        // TODO
        return false;
    }

    /**
     * Đăng ký (hình 4.3, bước 12-15): kiểm tra username chưa có trong tblPlayer,
     * băm mật khẩu SHA-256, INSERT bản ghi mới. Username đã tồn tại -> false.
     */
    public boolean registerPlayer(Player p) {
        // TODO
        return false;
    }

    /** Sửa thông tin (hình 4.5, bước 14): UPDATE name, email, tel theo id. */
    public boolean editPlayer(Player p) {
        // TODO
        return false;
    }

    /**
     * Đổi mật khẩu (hình 4.6, bước 9-10): băm mật khẩu cũ và mới,
     * UPDATE password WHERE id = ? AND password = hash(oldPass). Trả true khi đúng 1 dòng bị sửa.
     */
    public boolean changePassword(int playerId, String oldPass, String newPass) {
        // TODO
        return false;
    }

    /** Tổng điểm từ tblMatchResult: WIN = 1, DRAW = 0.5, LOSE = 0. Chưa chơi ván nào -> 0. */
    public float getTotalPoint(int playerId) {
        // TODO
        return 0f;
    }

    /**
     * Bảng xếp hạng (hình 4.8, bước 7-11): toàn bộ người chơi, kể cả người chưa chơi.
     * - totalPoint: tổng điểm.
     * - avgOpponentPoint: mỗi ván đã lưu đều tính; điểm đối thủ = tổng điểm HIỆN TẠI của đối thủ,
     *   lấy trung bình theo từng ván.
     * - avgMovesWin / avgMovesLose: số nước do CHÍNH người đó đi (tblMatchMove, phần N11),
     *   trung bình trên các trận thắng / thua; chưa có trận -> -1 (client hiện "-").
     * Đóng gói từng dòng vào PlayerRanking rồi sort(RANKING_ORDER).
     */
    public ArrayList<PlayerRanking> getRanking() {
        // TODO
        return new ArrayList<>();
    }
}
