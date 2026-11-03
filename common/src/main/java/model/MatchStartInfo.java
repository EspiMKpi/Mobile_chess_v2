package model;

import java.io.Serializable;

/** Payload SERVER_START_MATCH (ván đầu tiên). Chủ: N04. */
public class MatchStartInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String myColor;        // MatchResult.WHITE | MatchResult.BLACK
    private PlayerStat opponent;

    public MatchStartInfo(String myColor, PlayerStat opponent) {
        this.myColor = myColor;
        this.opponent = opponent;
    }

    public String getMyColor() { return myColor; }
    public PlayerStat getOpponent() { return opponent; }
}
