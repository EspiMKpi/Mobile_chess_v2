package model;

/** Dòng trong danh sách online (không ánh xạ bảng). Chủ: N04. */
public class PlayerStat extends Player {
    private static final long serialVersionUID = 1L;
    public static final String IDLE = "IDLE";
    public static final String BUSY = "BUSY";

    private float totalPoint;
    private String status;

    public PlayerStat() {
    }

    /** Theo hình 3.2: sao chép id, username, name, email, tel từ p (KHÔNG sao chép password). */
    public PlayerStat(Player p, float totalPoint, String status) {
        setId(p.getId());
        setUsername(p.getUsername());
        setName(p.getName());
        setEmail(p.getEmail());
        setTel(p.getTel());
        this.totalPoint = totalPoint;
        this.status = status;
    }

    public float getTotalPoint() { return totalPoint; }
    public void setTotalPoint(float totalPoint) { this.totalPoint = totalPoint; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
