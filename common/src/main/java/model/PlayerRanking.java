package model;

/**
 * Dòng bảng xếp hạng (không ánh xạ bảng). Chủ: N04.
 * Thứ tự sắp: totalPoint DESC, avgOpponentPoint DESC, avgMovesWin ASC, avgMovesLose DESC.
 */
public class PlayerRanking extends Player {
    private static final long serialVersionUID = 1L;
    private float totalPoint;
    private float avgOpponentPoint;
    private float avgMovesWin;
    private float avgMovesLose;

    public float getTotalPoint() { return totalPoint; }
    public void setTotalPoint(float totalPoint) { this.totalPoint = totalPoint; }
    public float getAvgOpponentPoint() { return avgOpponentPoint; }
    public void setAvgOpponentPoint(float avgOpponentPoint) { this.avgOpponentPoint = avgOpponentPoint; }
    public float getAvgMovesWin() { return avgMovesWin; }
    public void setAvgMovesWin(float avgMovesWin) { this.avgMovesWin = avgMovesWin; }
    public float getAvgMovesLose() { return avgMovesLose; }
    public void setAvgMovesLose(float avgMovesLose) { this.avgMovesLose = avgMovesLose; }
}
