package model;

import java.io.Serializable;

/** Payload SEND_MOVE. Chủ: N11. */
public class MoveRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private int fromSquare;
    private int toSquare;
    private String promotion;      // "Q" | "R" | "B" | "N" | null nếu không phong cấp

    public MoveRequest(int fromSquare, int toSquare, String promotion) {
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.promotion = promotion;
    }

    public int getFromSquare() { return fromSquare; }
    public int getToSquare() { return toSquare; }
    public String getPromotion() { return promotion; }
}
