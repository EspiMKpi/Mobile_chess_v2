package model;

import java.io.Serializable;

/** Payload SERVER_INFORM_MOVE. Chủ: N11. */
public class MoveInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String notation;
    private String fen;
    private String sideToMove;     // "WHITE" | "BLACK"
    private boolean inCheck;

    public MoveInfo(String notation, String fen, String sideToMove, boolean inCheck) {
        this.notation = notation;
        this.fen = fen;
        this.sideToMove = sideToMove;
        this.inCheck = inCheck;
    }

    public String getNotation() { return notation; }
    public String getFen() { return fen; }
    public String getSideToMove() { return sideToMove; }
    public boolean isInCheck() { return inCheck; }
}
