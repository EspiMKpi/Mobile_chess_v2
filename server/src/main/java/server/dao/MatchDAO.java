package server.dao;

import java.util.ArrayList;

import model.Match;

/**
 * Chủ: N04. Truy vấn lịch sử đấu (và lưu ván đấu nếu cần dùng chung).
 */
public class MatchDAO extends DAO {

    public MatchDAO() {
        super();
    }

    /**
     * TODO(N04 - Hình 4.7, bước 6-12): Lấy danh sách ván đấu mà playerId đã tham gia,
     * sắp xếp mới nhất ở trên (ORDER BY m.startTime DESC, m.id DESC).
     * Mỗi Match cần gom đủ 2 đối tượng MatchResult (mỗi kết quả gắn với Player chứa id, username, name).
     *
     * Gợi ý SQL:
     *   SELECT m.id AS match_id, m.startTime, m.endTime,
     *          r.id AS res_id, r.result, r.color,
     *          p.id AS p_id, p.username, p.name
     *   FROM tblMatch m
     *   JOIN tblMatchResult r ON r.matchId = m.id
     *   JOIN tblPlayer p ON p.id = r.playerId
     *   WHERE m.id IN (SELECT matchId FROM tblMatchResult WHERE playerId = ?)
     *   ORDER BY m.startTime DESC, m.id DESC, r.id ASC
     */
    public ArrayList<Match> getMatchesByPlayer(int playerId) {
        // TODO(N04)
        return new ArrayList<>();
    }

    /**
     * Lưu một ván đấu vào tblMatch và 2 dòng tblMatchResult trong cùng một transaction.
     * (Ở cấu trúc phân chia mới, phía N11 gọi hàm này hoặc tự cài đặt khi kết thúc ván đấu.)
     */
    public boolean saveMatch(Match match) {
        // TODO(N04 / N11)
        return false;
    }
}
