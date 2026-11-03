package model;

import java.io.Serializable;

/** Bảng tblMatchMove. Ô: 0 = a8 ... 63 = h1 (giống BoardUtils). Chủ: N11. */
public class MatchMove implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private int moveIndex;
    private int fromSquare;
    private int toSquare;
    private String notation;
    private String promotion;      // "Q" | "R" | "B" | "N" | null

    public MatchMove() {
    }

    public MatchMove(int moveIndex, int fromSquare, int toSquare, String notation, String promotion) {
        this.moveIndex = moveIndex;
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.notation = notation;
        this.promotion = promotion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMoveIndex() { return moveIndex; }
    public int getFromSquare() { return fromSquare; }
    public int getToSquare() { return toSquare; }
    public String getNotation() { return notation; }
    public String getPromotion() { return promotion; }
}
