package model;

import java.io.Serializable;

/** Payload SERVER_END_GAME (gửi riêng cho từng người chơi). Chủ: N11, dùng chung với N04. */
public class GameResult implements Serializable {
    private static final long serialVersionUID = 1L;
    // Lý do kết thúc ván
    public static final String CHECKMATE = "CHECKMATE";   // N11
    public static final String STALEMATE = "STALEMATE";   // N11
    public static final String TIMEOUT = "TIMEOUT";       // N11
    public static final String RESIGN = "RESIGN";         // N04 (QUIT_GAME)
    public static final String DISCONNECT = "DISCONNECT"; // N04

    private String result;         // MatchResult.WIN | DRAW | LOSE
    private String reason;
    private String color;          // màu quân của người nhận
    private float point;           // 1 | 0.5 | 0

    public GameResult(String result, String reason, String color, float point) {
        this.result = result;
        this.reason = reason;
        this.color = color;
        this.point = point;
    }

    public String getResult() { return result; }
    public String getReason() { return reason; }
    public String getColor() { return color; }
    public float getPoint() { return point; }
}
