package model;

import java.io.Serializable;

/**
 * Payload SERVER_START_MATCH (bàn giao từ N04 sang N11 khi vào trận).
 * Chứa màu quân của người nhận, thông tin bản thân và thông tin đối thủ.
 */
public class MatchStartInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String myColor;        // MatchResult.WHITE | MatchResult.BLACK
    private PlayerStat me;
    private PlayerStat opponent;

    public MatchStartInfo(String myColor, PlayerStat me, PlayerStat opponent) {
        this.myColor = myColor;
        this.me = me;
        this.opponent = opponent;
    }

    public MatchStartInfo(String myColor, PlayerStat opponent) {
        this(myColor, null, opponent);
    }

    public String getMyColor() { return myColor; }
    public PlayerStat getMe() { return me; }
    public PlayerStat getOpponent() { return opponent; }
}
