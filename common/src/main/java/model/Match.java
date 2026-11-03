package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/** Bảng tblMatch. Chủ: N04 (danh sách moves do N11 điền). */
public class Match implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private Date startTime;
    private Date endTime;
    private ArrayList<MatchResult> results = new ArrayList<>();
    private ArrayList<MatchMove> moves = new ArrayList<>();

    public Match() {
    }

    public Match(Date startTime) {
        this.startTime = startTime;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public ArrayList<MatchResult> getResults() { return results; }
    public void addResult(MatchResult r) { results.add(r); }
    public ArrayList<MatchMove> getMoves() { return moves; }
    public void addMove(MatchMove m) { moves.add(m); }
}
