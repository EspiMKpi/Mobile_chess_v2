package server.control;

import model.MatchResult;
import model.MatchStartInfo;
import model.ObjectWrapper;

/**
 * [PHẦN N11] Phòng đấu đối kháng thực hiện hợp đồng {@link MatchSession}.
 *
 * N04 CHỈ tạo đối tượng GameRoom(myServer, challenger, challenged) và gọi start() khi B đồng ý thách đấu.
 * Toàn bộ phần còn lại (luật cờ, đồng hồ 30 giây, xử lý nước đi, xin thua, lưu DB khi kết thúc ván,
 * hỏi chơi tiếp / đổi màu) do thành viên N11 phụ trách.
 */
public class GameRoom implements MatchSession {

    private final ServerCtr myServer;
    private final ServerCtr.ServerProcessing challenger; // Ván 1 cầm ĐEN (đi sau)
    private final ServerCtr.ServerProcessing challenged; // Ván 1 cầm TRẮNG (đi trước)

    public GameRoom(ServerCtr myServer,
                    ServerCtr.ServerProcessing challenger,
                    ServerCtr.ServerProcessing challenged) {
        this.myServer = myServer;
        this.challenger = challenger;
        this.challenged = challenged;
    }

    @Override
    public synchronized void start() {
        // Khung bàn giao tối thiểu để test chuyển sang màn hình trận đấu của N11.
        // N11 sẽ thay thế / mở rộng bằng ChessReferee, Match, đồng hồ 30s...
        MatchStartInfo infoA = new MatchStartInfo(
                MatchResult.BLACK,
                myServer.getPlayerStat(challenger),
                myServer.getPlayerStat(challenged));
        MatchStartInfo infoB = new MatchStartInfo(
                MatchResult.WHITE,
                myServer.getPlayerStat(challenged),
                myServer.getPlayerStat(challenger));
        challenger.sendData(new ObjectWrapper(ObjectWrapper.SERVER_START_MATCH, infoA));
        challenged.sendData(new ObjectWrapper(ObjectWrapper.SERVER_START_MATCH, infoB));
    }

    @Override
    public synchronized void onPacket(ServerCtr.ServerProcessing sender, ObjectWrapper packet) {
        // TODO(N11): xử lý SEND_MOVE, QUIT_GAME, REQUEST_CONTINUE_MATCH
        // Khi kết thúc hẳn phòng đấu, N11 gọi: myServer.onMatchSessionEnded(this, challenger, challenged);
    }

    @Override
    public synchronized void onDisconnect(ServerCtr.ServerProcessing player) {
        // TODO(N11): xử thua người mất kết nối, lưu ván nếu đang chơi, báo đối thủ và đóng phòng
        myServer.onMatchSessionEnded(this, challenger, challenged);
    }
}
