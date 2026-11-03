package model;

import java.io.Serializable;

/**
 * Gói tin trao đổi Client <-> Server. HỢP ĐỒNG CHUNG (Phase 0) - không tự ý đổi số.
 * Dải mã: N04 = 1..29, N11 = 30..39.
 */
public class ObjectWrapper implements Serializable {
    private static final long serialVersionUID = 1L;

    // Số 1..24 khớp BAO_CAO_THIET_KE_GAME_CO_VUA.md (sơ đồ lớp CN1, CN2, CN3 của N04).

    // ===== N04 - Chức năng 1: Tài khoản & thông tin người chơi =====
    public static final int LOGIN_USER = 1;
    public static final int REPLY_LOGIN_USER = 2;
    public static final int REGISTER_USER = 3;
    public static final int REPLY_REGISTER_USER = 4;
    public static final int EDIT_PLAYER = 5;
    public static final int REPLY_EDIT_PLAYER = 6;
    public static final int CHANGE_PASSWORD = 7;
    public static final int REPLY_CHANGE_PASSWORD = 8;
    public static final int GET_PLAYER_HISTORY = 9;
    public static final int REPLY_PLAYER_HISTORY = 10;
    // ===== N04 - Chức năng 2: Danh sách online =====
    public static final int GET_ONLINE_LIST = 11;
    public static final int REPLY_ONLINE_LIST = 12;
    public static final int SERVER_INFORM_ONLINE_LIST = 13;
    // ===== N04 - Chức năng 3: Thách đấu & ghép cặp (theo hình 4.5) =====
    public static final int SEND_CHALLENGE = 14;
    public static final int REPLY_CHALLENGE_BUSY = 15;
    public static final int SERVER_INFORM_CHALLENGE = 16;
    public static final int REPLY_CHALLENGE_DECISION = 17;        // data: Boolean (true = OK, false = Reject)
    public static final int REPLY_CHALLENGE_REJECT = 18;
    public static final int SERVER_START_MATCH = 19;              // data: MatchStartInfo {màu quân, đối thủ}
    public static final int QUIT_GAME = 20;                       // data: null
    public static final int SERVER_END_GAME = 21;                 // data: GameResult (dùng chung N04 + N11)
                                                                  //   (báo cáo N04 ghi String "WIN"/"LOSE" -> đã thống nhất đổi sang GameResult)
    public static final int REQUEST_CONTINUE_MATCH = 22;          // data: Boolean (true = Tiếp tục, false = Dừng)
    public static final int SERVER_RESTART_MATCH_SWAP_COLOR = 23; // data: String màu mới "WHITE" | "BLACK"
    public static final int SERVER_END_SESSION_MATCH = 24;
    // ===== N04 - Bảng xếp hạng (bổ sung) =====
    public static final int GET_RANKING = 25;
    public static final int REPLY_RANKING = 26;                   // data: ArrayList<PlayerRanking>

    // ===== N11 - Nước đi & đồng bộ bàn cờ =====
    public static final int SEND_MOVE = 30;                       // data: MoveRequest
    public static final int REPLY_MOVE_ILLEGAL = 31;              // data: String (lý do)
    public static final int SERVER_INFORM_MOVE = 32;              // data: MoveInfo

    private int performative;
    private Object data;

    public ObjectWrapper() {
    }

    public ObjectWrapper(int performative, Object data) {
        this.performative = performative;
        this.data = data;
    }

    public int getPerformative() { return performative; }
    public void setPerformative(int performative) { this.performative = performative; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
