package model;

import java.io.Serializable;

/** Bảng tblMatchResult. Chủ: N04. */
public class MatchResult implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String WIN = "WIN";
    public static final String DRAW = "DRAW";
    public static final String LOSE = "LOSE";
    public static final String WHITE = "WHITE";
    public static final String BLACK = "BLACK";

    private int id;
    private Player player;
    private String result;
    private String color;

    public MatchResult() {
    }

    public MatchResult(Player player, String result, String color) {
        this.player = player;
        this.result = result;
        this.color = color;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
