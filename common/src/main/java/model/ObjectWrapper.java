package model;

import java.io.Serializable;

/**
 * Gói tin trao đổi Client <-> Server (Hợp đồng chung N04 <-> N11).
 * Dải mã:
 *  - N04 (trước khi vào trận + tài khoản + BXH + thách đấu): 1..19, 25..28
 *  - N11 (trong trận đấu đối kháng): 20..24, 30..32
 */
public class ObjectWrapper implements Serializable {
    private static final long serialVersionUID = 1L;

    // ===== N04 - Chức năng 1: Tài khoản & thông tin người chơi =====
    public static final int LOGIN_USER = 1;                       // data: Player(username, password)
    public static final int REPLY_LOGIN_USER = 2;                 // data: Player (password = null) | "false" | "logged_in"
    public static final int REGISTER_USER = 3;                    // data: Player
    public static final int REPLY_REGISTER_USER = 4;              // data: "ok" | "false"
    public static final int EDIT_PLAYER = 5;                      // data: Player(name, email, tel)
    public static final int REPLY_EDIT_PLAYER = 6;                // data: "ok" | "false"
    public static final int CHANGE_PASSWORD = 7;                  // data: String[]{oldPass, newPass}
    public static final int REPLY_CHANGE_PASSWORD = 8;            // data: "ok" | "false"
    public static final int GET_PLAYER_HISTORY = 9;               // data: null
    public static final int REPLY_PLAYER_HISTORY = 10;            // data: ArrayList<Match>

    // ===== N04 - Chức năng 2: Danh sách online =====
    public static final int GET_ONLINE_LIST = 11;                 // data: null
    public static final int REPLY_ONLINE_LIST = 12;               // data: ArrayList<PlayerStat>
    public static final int SERVER_INFORM_ONLINE_LIST = 13;       // data: ArrayList<PlayerStat>

    // ===== N04 - Chức năng 3: Thách đấu & ghép cặp (trước khi vào trận) =====
    public static final int SEND_CHALLENGE = 14;                  // data: PlayerStat (đối thủ B)
    public static final int REPLY_CHALLENGE_BUSY = 15;            // data: PlayerStat (đối thủ B)
    public static final int SERVER_INFORM_CHALLENGE = 16;         // data: PlayerStat (người mời A)
    public static final int REPLY_CHALLENGE_DECISION = 17;        // data: Boolean (true = Đồng ý, false = Từ chối)
    public static final int REPLY_CHALLENGE_REJECT = 18;          // data: String lý do ("REJECT" | "CANCEL" | "TIMEOUT" | "DISCONNECT") hoặc PlayerStat
    public static final int SERVER_START_MATCH = 19;              // data: MatchStartInfo

    // ===== N11 - Trong trận đấu đối kháng (Server chuyển tiếp sang MatchSession) =====
    public static final int QUIT_GAME = 20;                       // data: null
    public static final int SERVER_END_GAME = 21;                 // data: GameResult
    public static final int REQUEST_CONTINUE_MATCH = 22;          // data: Boolean (true = Tiếp tục, false = Dừng)
    public static final int SERVER_RESTART_MATCH_SWAP_COLOR = 23; // data: String ("WHITE" | "BLACK")
    public static final int SERVER_END_SESSION_MATCH = 24;        // data: String ("STOP" | "OPPONENT_STOP")

    // ===== N04 - Bảng xếp hạng =====
    public static final int GET_RANKING = 25;                     // data: null
    public static final int REPLY_RANKING = 26;                   // data: ArrayList<PlayerRanking>

    // ===== N04 - Hủy lời mời thách đấu =====
    public static final int CANCEL_CHALLENGE = 27;                // data: null (người mời A bấm Hủy lời mời)
    public static final int SERVER_CANCEL_CHALLENGE = 28;         // data: String lý do ("CANCEL" | "TIMEOUT" | "DISCONNECT")

    // ===== N11 - Nước đi & đồng bộ bàn cờ =====
    public static final int SEND_MOVE = 30;                       // data: MoveRequest
    public static final int REPLY_MOVE_ILLEGAL = 31;              // data: String lý do
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
