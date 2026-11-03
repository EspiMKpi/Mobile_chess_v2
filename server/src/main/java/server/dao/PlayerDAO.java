package server.dao;

import java.util.ArrayList;

import model.Player;
import model.PlayerRanking;

/** Chủ: N04. Truy vấn tblPlayer (+ điểm, BXH). Xem BLUEPRINT_N04 mục 3.5, 3.6. */
public class PlayerDAO extends DAO {

    /** Đúng username + password (hash) -> điền id, name, email, tel vào p, trả true. */
    public boolean checkLogin(Player p) {
        // TODO(N04, M2)
        return false;
    }

    /** Username đã tồn tại -> false; ngược lại INSERT (password hash). */
    public boolean registerPlayer(Player p) {
        // TODO(N04, M2)
        return false;
    }

    /** UPDATE name, email, tel theo id. */
    public boolean editPlayer(Player p) {
        // TODO(N04, M2)
        return false;
    }

    /** UPDATE password WHERE id=? AND password=hash(oldPass) -> executeUpdate() == 1. */
    public boolean changePassword(int playerId, String oldPass, String newPass) {
        // TODO(N04, M2)
        return false;
    }

    /** WIN = 1, DRAW = 0.5, LOSE = 0. */
    public float getTotalPoint(int playerId) {
        // TODO(N04, M3)
        return 0;
    }

    /** Toàn bộ người chơi, đã sắp theo 4 tiêu chí (xem PlayerRanking). */
    public ArrayList<PlayerRanking> getRanking() {
        // TODO(N04, M5)
        return new ArrayList<>();
    }
}
