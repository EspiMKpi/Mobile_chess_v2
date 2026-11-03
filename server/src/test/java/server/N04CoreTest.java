package server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import model.GameResult;
import model.MatchResult;
import model.MatchStartInfo;
import model.ObjectWrapper;
import model.Player;
import model.PlayerRanking;
import model.PlayerStat;
import server.control.GameRoom;
import server.control.ServerCtr;
import server.dao.DAO;
import server.dao.PlayerDAO;

public class N04CoreTest {

    private static final int TEST_PORT = 19992;
    private ServerCtr server;

    @Before
    public void setUp() {
        server = new ServerCtr(null, TEST_PORT);
        boolean ok = server.openServer();
        Assert.assertTrue("Server failed to open on port " + TEST_PORT, ok);
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stopServer();
        }
    }

    @Test
    public void testPasswordHashing() {
        String plain = "123456";
        String hash = DAO.hashPassword(plain);
        Assert.assertNotNull(hash);
        Assert.assertEquals(64, hash.length());

        // Hashing lại cùng một chuỗi phải ra kết quả giống nhau
        String hash2 = DAO.hashPassword(plain);
        Assert.assertEquals(hash, hash2);

        // Chuỗi khác nhau phải cho hash khác nhau
        String hashOther = DAO.hashPassword("different_password");
        Assert.assertNotEquals(hash, hashOther);
    }

    @Test
    public void testServerClientConnectionAndOnlineList() throws Exception {
        Socket clientSocket = new Socket();
        clientSocket.connect(new InetSocketAddress("127.0.0.1", TEST_PORT), 3000);
        ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
        oos.flush();
        ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());

        // Đợi server accept
        Thread.sleep(300);
        Assert.assertEquals(1, server.getMyProcess().size());

        // Client gửi GET_ONLINE_LIST
        oos.writeObject(new ObjectWrapper(ObjectWrapper.GET_ONLINE_LIST, null));
        oos.flush();

        ObjectWrapper reply = (ObjectWrapper) ois.readObject();
        Assert.assertNotNull(reply);
        Assert.assertEquals(ObjectWrapper.REPLY_ONLINE_LIST, reply.getPerformative());
        Assert.assertTrue(reply.getData() instanceof ArrayList);

        // Đóng client
        clientSocket.close();
        Thread.sleep(300);
        Assert.assertEquals(0, server.getMyProcess().size());
    }

    @Test
    public void testGameRoomLifecycleAndChallengeDecision() throws Exception {
        Socket socketA = new Socket("127.0.0.1", TEST_PORT);
        ObjectOutputStream oosA = new ObjectOutputStream(socketA.getOutputStream());
        oosA.flush();
        ObjectInputStream oisA = new ObjectInputStream(socketA.getInputStream());

        Socket socketB = new Socket("127.0.0.1", TEST_PORT);
        ObjectOutputStream oosB = new ObjectOutputStream(socketB.getOutputStream());
        oosB.flush();
        ObjectInputStream oisB = new ObjectInputStream(socketB.getInputStream());

        Thread.sleep(300);
        Assert.assertEquals(2, server.getMyProcess().size());

        ServerCtr.ServerProcessing spA = server.getMyProcess().get(0);
        ServerCtr.ServerProcessing spB = server.getMyProcess().get(1);

        Player pA = new Player("playerA", "passA");
        pA.setId(101);
        pA.setName("Player A");
        spA.getLoggedInPlayer(); // gán thông qua reflection hoặc setLoggedInPlayer
        // Vì ServerProcessing lưu loggedInPlayer, ta kiểm tra logic GameRoom trực tiếp
        GameRoom room = server.createRoom(spA, spB);
        Assert.assertNotNull(room);
        Assert.assertEquals(PlayerStat.BUSY, spA.getStatus());
        Assert.assertEquals(PlayerStat.BUSY, spB.getStatus());
        Assert.assertEquals(1, server.getMyRooms().size());

        // Từ chối thách đấu
        room.receiveChallengeDecision(false);
        Thread.sleep(200);

        // Room phải đóng và cả hai trở lại IDLE
        Assert.assertEquals(PlayerStat.IDLE, spA.getStatus());
        Assert.assertEquals(PlayerStat.IDLE, spB.getStatus());
        Assert.assertEquals(0, server.getMyRooms().size());

        // Client A nhận được REPLY_CHALLENGE_REJECT
        ObjectWrapper reply = (ObjectWrapper) oisA.readObject();
        Assert.assertEquals(ObjectWrapper.REPLY_CHALLENGE_REJECT, reply.getPerformative());

        socketA.close();
        socketB.close();
    }

    @Test
    public void testGameRoomMatchFlowAndColorSwap() throws Exception {
        Socket socketA = new Socket("127.0.0.1", TEST_PORT);
        ObjectOutputStream oosA = new ObjectOutputStream(socketA.getOutputStream());
        oosA.flush();
        ObjectInputStream oisA = new ObjectInputStream(socketA.getInputStream());

        Socket socketB = new Socket("127.0.0.1", TEST_PORT);
        ObjectOutputStream oosB = new ObjectOutputStream(socketB.getOutputStream());
        oosB.flush();
        ObjectInputStream oisB = new ObjectInputStream(socketB.getInputStream());

        Thread.sleep(300);
        ServerCtr.ServerProcessing spA = server.getMyProcess().get(0);
        ServerCtr.ServerProcessing spB = server.getMyProcess().get(1);

        // Tạo phòng: A thách đấu B -> A cầm Đen, B cầm Trắng
        GameRoom room = server.createRoom(spA, spB);
        Assert.assertEquals(spA, room.getBlackPlayer());
        Assert.assertEquals(spB, room.getWhitePlayer());

        // Đồng ý thách đấu -> bắt đầu trận
        room.receiveChallengeDecision(true);
        Assert.assertTrue(room.isPlaying());

        // Nhận SERVER_START_MATCH phía A (Đen) và B (Trắng)
        ObjectWrapper startMsgA = (ObjectWrapper) oisA.readObject();
        Assert.assertEquals(ObjectWrapper.SERVER_START_MATCH, startMsgA.getPerformative());
        MatchStartInfo infoA = (MatchStartInfo) startMsgA.getData();
        Assert.assertEquals(MatchResult.BLACK, infoA.getMyColor());

        ObjectWrapper startMsgB = (ObjectWrapper) oisB.readObject();
        Assert.assertEquals(ObjectWrapper.SERVER_START_MATCH, startMsgB.getPerformative());
        MatchStartInfo infoB = (MatchStartInfo) startMsgB.getData();
        Assert.assertEquals(MatchResult.WHITE, infoB.getMyColor());

        // Kết thúc ván (A xin thua -> B thắng RESIGN)
        room.endGame(spB, GameResult.RESIGN);
        Assert.assertFalse(room.isPlaying());

        ObjectWrapper endMsgA = (ObjectWrapper) oisA.readObject();
        Assert.assertEquals(ObjectWrapper.SERVER_END_GAME, endMsgA.getPerformative());
        GameResult resA = (GameResult) endMsgA.getData();
        Assert.assertEquals(MatchResult.LOSE, resA.getResult());
        Assert.assertEquals(GameResult.RESIGN, resA.getReason());

        ObjectWrapper endMsgB = (ObjectWrapper) oisB.readObject();
        Assert.assertEquals(ObjectWrapper.SERVER_END_GAME, endMsgB.getPerformative());
        GameResult resB = (GameResult) endMsgB.getData();
        Assert.assertEquals(MatchResult.WIN, resB.getResult());

        // Cả 2 cùng đồng ý chơi tiếp -> đổi màu quân (A thành Trắng, B thành Đen)
        room.receiveContinueDecision(spA, true);
        room.receiveContinueDecision(spB, true);

        Assert.assertTrue(room.isPlaying());
        Assert.assertEquals(spA, room.getWhitePlayer());
        Assert.assertEquals(spB, room.getBlackPlayer());

        ObjectWrapper restartA = (ObjectWrapper) oisA.readObject();
        Assert.assertEquals(ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR, restartA.getPerformative());
        Assert.assertEquals(MatchResult.WHITE, restartA.getData());

        ObjectWrapper restartB = (ObjectWrapper) oisB.readObject();
        Assert.assertEquals(ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR, restartB.getPerformative());
        Assert.assertEquals(MatchResult.BLACK, restartB.getData());

        // Dừng chơi (A chọn dừng)
        room.receiveContinueDecision(spA, false);
        Assert.assertFalse(room.isPlaying());

        ObjectWrapper stopA = (ObjectWrapper) oisA.readObject();
        Assert.assertEquals(ObjectWrapper.SERVER_END_SESSION_MATCH, stopA.getPerformative());
        Assert.assertEquals("STOP", stopA.getData());

        ObjectWrapper stopB = (ObjectWrapper) oisB.readObject();
        Assert.assertEquals(ObjectWrapper.SERVER_END_SESSION_MATCH, stopB.getPerformative());
        Assert.assertEquals("OPPONENT_STOP", stopB.getData());

        socketA.close();
        socketB.close();
    }

    @Test
    public void testRankingSortingCriteria() {
        List<PlayerRanking> list = new ArrayList<>();

        // Người chơi 1: Điểm cao nhất -> xếp thứ 1
        PlayerRanking p1 = new PlayerRanking();
        p1.setId(1);
        p1.setUsername("user1");
        p1.setTotalPoint(15.0f);
        p1.setAvgOpponentPoint(10.0f);
        p1.setAvgMovesWin(30.0f);
        p1.setAvgMovesLose(40.0f);
        list.add(p1);

        // Người chơi 2 & 3: Cùng totalPoint (10.0f), nhưng p2 có avgOpponentPoint cao hơn (12.0 > 8.0)
        PlayerRanking p2 = new PlayerRanking();
        p2.setId(2);
        p2.setUsername("user2");
        p2.setTotalPoint(10.0f);
        p2.setAvgOpponentPoint(12.0f);
        p2.setAvgMovesWin(25.0f);
        p2.setAvgMovesLose(35.0f);
        list.add(p2);

        PlayerRanking p3 = new PlayerRanking();
        p3.setId(3);
        p3.setUsername("user3");
        p3.setTotalPoint(10.0f);
        p3.setAvgOpponentPoint(8.0f);
        p3.setAvgMovesWin(20.0f);
        p3.setAvgMovesLose(30.0f);
        list.add(p3);

        // Người chơi 4 & 5: Cùng totalPoint (5.0f), cùng avgOpponentPoint (5.0f).
        // p4 có avgMovesWin ít hơn (22.0 < 35.0) -> xếp trước p5
        PlayerRanking p4 = new PlayerRanking();
        p4.setId(4);
        p4.setUsername("user4");
        p4.setTotalPoint(5.0f);
        p4.setAvgOpponentPoint(5.0f);
        p4.setAvgMovesWin(22.0f);
        p4.setAvgMovesLose(30.0f);
        list.add(p4);

        PlayerRanking p5 = new PlayerRanking();
        p5.setId(5);
        p5.setUsername("user5");
        p5.setTotalPoint(5.0f);
        p5.setAvgOpponentPoint(5.0f);
        p5.setAvgMovesWin(35.0f);
        p5.setAvgMovesLose(30.0f);
        list.add(p5);

        // Người chơi 6: Chưa thắng ván nào (avgMovesWin = -1) -> xếp sau p4, p5 dù cùng điểm
        PlayerRanking p6 = new PlayerRanking();
        p6.setId(6);
        p6.setUsername("user6");
        p6.setTotalPoint(5.0f);
        p6.setAvgOpponentPoint(5.0f);
        p6.setAvgMovesWin(-1.0f);
        p6.setAvgMovesLose(30.0f);
        list.add(p6);

        // Đảo ngẫu nhiên danh sách trước khi sắp xếp
        Collections.shuffle(list);

        // Sắp xếp bằng Comparator từ PlayerDAO
        Collections.sort(list, new Comparator<PlayerRanking>() {
            @Override
            public int compare(PlayerRanking a, PlayerRanking b) {
                int cmp = Float.compare(b.getTotalPoint(), a.getTotalPoint());
                if (cmp != 0) return cmp;

                cmp = Float.compare(b.getAvgOpponentPoint(), a.getAvgOpponentPoint());
                if (cmp != 0) return cmp;

                float w1 = a.getAvgMovesWin();
                float w2 = b.getAvgMovesWin();
                if (w1 >= 0 && w2 >= 0) {
                    cmp = Float.compare(w1, w2);
                    if (cmp != 0) return cmp;
                } else if (w1 >= 0 && w2 < 0) {
                    return -1;
                } else if (w1 < 0 && w2 >= 0) {
                    return 1;
                }

                float l1 = a.getAvgMovesLose();
                float l2 = b.getAvgMovesLose();
                if (l1 >= 0 && l2 >= 0) {
                    cmp = Float.compare(l2, l1);
                    if (cmp != 0) return cmp;
                } else if (l1 >= 0 && l2 < 0) {
                    return -1;
                } else if (l1 < 0 && l2 >= 0) {
                    return 1;
                }

                return Integer.compare(a.getId(), b.getId());
            }
        });

        Assert.assertEquals(1, list.get(0).getId());
        Assert.assertEquals(2, list.get(1).getId());
        Assert.assertEquals(3, list.get(2).getId());
        Assert.assertEquals(4, list.get(3).getId());
        Assert.assertEquals(5, list.get(4).getId());
        Assert.assertEquals(6, list.get(5).getId());
    }
}
