package model;

import java.io.Serializable;

/**
 * Payload SERVER_INFORM_MOVE (Server gửi cho CẢ 2 người chơi sau mỗi nước hợp lệ). Chủ: N11.
 * Không gửi thế cờ (FEN): mỗi máy giữ một bàn cờ riêng và tự đi lại đúng nước này bằng engine
 * (fromSquare, toSquare, promotion) nên 2 bàn luôn giống bàn của Server.
 * Ô cờ: 0 = a8 ... 63 = h1.
 */
public class MoveInfo implements Serializable {
    private static final long serialVersionUID = 2L;
    private String notation;       // ký hiệu đại số, ví dụ "e4", "Nf3", "O-O", "e8=Q"
    private int fromSquare;
    private int toSquare;
    private String promotion;      // "Q" | "R" | "B" | "N" | null
    private String sideToMove;     // bên đi tiếp theo: "WHITE" | "BLACK"
    private boolean inCheck;       // bên đi tiếp theo đang bị chiếu

    public MoveInfo(String notation, int fromSquare, int toSquare, String promotion,
                    String sideToMove, boolean inCheck) {
        this.notation = notation;
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.promotion = promotion;
        this.sideToMove = sideToMove;
        this.inCheck = inCheck;
    }

    public String getNotation() { return notation; }
    public int getFromSquare() { return fromSquare; }
    public int getToSquare() { return toSquare; }
    public String getPromotion() { return promotion; }
    public String getSideToMove() { return sideToMove; }
    public boolean isInCheck() { return inCheck; }
}
