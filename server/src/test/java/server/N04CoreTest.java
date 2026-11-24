package server;

import org.junit.Assert;
import org.junit.Test;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import model.MatchResult;
import model.MatchStartInfo;
import model.ObjectWrapper;
import model.Player;
import model.PlayerRanking;
import model.PlayerStat;
import server.control.PendingChallenge;
import server.control.ServerCtr;
import server.dao.PlayerDAO;

/**
 * Kiểm thử đơn vị phần N04 (chạy không cần MySQL).
 */
public class N04CoreTest {

    private static void waitUntil(BooleanSupplier cond, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!cond.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                Assert.fail("Hết thời gian chờ điều kiện");
            }
            Thread.sleep(20);
        }
    }

    private static ObjectWrapper readPerformative(ObjectInputStream ois, int expected) throws Exception {
        for (int i = 0; i < 10; i++) {
            ObjectWrapper w = (ObjectWrapper) ois.readObject();
            if (w.getPerformative() == expected) {
                return w;
            }
        }
        Assert.fail("Không nhận được mã lệnh " + expected);
        return null;
    }

    @Test
    public void testUdpDiscoveryAndTcpOnlineList() throws Exception {
        ServerCtr server = new ServerCtr(null, 0, 0);
        server.openServer();
        try {
            waitUntil(() -> server.getTcpPort() > 0 && server.getDiscoveryPort() > 0, 3000);

            try (DatagramSocket udp = new DatagramSocket()) {
                udp.setSoTimeout(3000);
                byte[] req = "DISCOVER_SERVER".getBytes(StandardCharsets.UTF_8);
                udp.send(new DatagramPacket(req, req.length, InetAddress.getLoopbackAddress(), server.getDiscoveryPort()));
                byte[] buf = new byte[128];
                DatagramPacket reply = new DatagramPacket(buf, buf.length);
                udp.receive(reply);
                int discoveredPort = Integer.parseInt(
                        new String(reply.getData(), 0, reply.getLength(), StandardCharsets.UTF_8).trim());
                Assert.assertEquals(server.getTcpPort(), discoveredPort);
            }

            try (Socket s = new Socket("127.0.0.1", server.getTcpPort())) {
                ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(s.getInputStream());

                oos.writeObject(new ObjectWrapper(ObjectWrapper.GET_ONLINE_LIST, null));
                oos.flush();
                ObjectWrapper reply = readPerformative(ois, ObjectWrapper.REPLY_ONLINE_LIST);
                Assert.assertTrue(reply.getData() instanceof ArrayList<?>);
            }
        } finally {
            server.stopServer();
        }
    }

    @Test
    public void testChallengeAtomicCancelRejectAndStartMatch() throws Exception {
        ServerCtr server = new ServerCtr(null, 0, 0);
        server.openServer();
        try (Socket sA = new Socket("127.0.0.1", server.getTcpPort());
             Socket sB = new Socket("127.0.0.1", server.getTcpPort());
             Socket sC = new Socket("127.0.0.1", server.getTcpPort())) {

            ObjectOutputStream oosA = new ObjectOutputStream(sA.getOutputStream()); oosA.flush();
            ObjectInputStream oisA = new ObjectInputStream(sA.getInputStream());
            ObjectOutputStream oosB = new ObjectOutputStream(sB.getOutputStream()); oosB.flush();
            ObjectInputStream oisB = new ObjectInputStream(sB.getInputStream());
            ObjectOutputStream oosC = new ObjectOutputStream(sC.getOutputStream()); oosC.flush();
            ObjectInputStream oisC = new ObjectInputStream(sC.getInputStream());

            waitUntil(() -> server.getMyProcess().size() == 3, 3000);
            List<ServerCtr.ServerProcessing> procs = server.getMyProcess();
            ServerCtr.ServerProcessing spA = procs.get(0);
            ServerCtr.ServerProcessing spB = procs.get(1);
            ServerCtr.ServerProcessing spC = procs.get(2);

            Player pA = new Player("userA", ""); pA.setId(1);
            Player pB = new Player("userB", ""); pB.setId(2);
            Player pC = new Player("userC", ""); pC.setId(3);
            spA.setLoggedInPlayer(pA);
            spB.setLoggedInPlayer(pB);
            spC.setLoggedInPlayer(pC);

            // Tự thách đấu chính mình -> null
            Assert.assertNull(server.tryCreateChallenge(spA, spA));

            // A thách đấu B -> thành công; C thách đấu B lúc B đang bận -> bị chặn
            PendingChallenge ch1 = server.tryCreateChallenge(spA, spB);
            Assert.assertNotNull(ch1);
            Assert.assertEquals(PlayerStat.BUSY, spA.getStatus());
            Assert.assertEquals(PlayerStat.BUSY, spB.getStatus());
            Assert.assertNull(server.tryCreateChallenge(spC, spB));

            // A hủy lời mời -> cả 2 về IDLE và nhận thông báo hủy
            oosA.writeObject(new ObjectWrapper(ObjectWrapper.CANCEL_CHALLENGE, null));
            oosA.flush();
            ObjectWrapper rejectA = readPerformative(oisA, ObjectWrapper.REPLY_CHALLENGE_REJECT);
            Assert.assertEquals("CANCEL", rejectA.getData());
            ObjectWrapper cancelB = readPerformative(oisB, ObjectWrapper.SERVER_CANCEL_CHALLENGE);
            Assert.assertEquals("CANCEL", cancelB.getData());
            Assert.assertEquals(PlayerStat.IDLE, spA.getStatus());
            Assert.assertEquals(PlayerStat.IDLE, spB.getStatus());

            // A thách đấu lại B, B từ chối -> cả 2 về IDLE
            PendingChallenge ch2 = server.tryCreateChallenge(spA, spB);
            Assert.assertNotNull(ch2);
            oosB.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_DECISION, false));
            oosB.flush();
            ObjectWrapper rejectA2 = readPerformative(oisA, ObjectWrapper.REPLY_CHALLENGE_REJECT);
            Assert.assertEquals("REJECT", rejectA2.getData());
            Assert.assertEquals(PlayerStat.IDLE, spA.getStatus());
            Assert.assertEquals(PlayerStat.IDLE, spB.getStatus());

            // A thách đấu B, B đồng ý -> bàn giao sang MatchSession (GameRoom), A cầm ĐEN, B cầm TRẮNG
            PendingChallenge ch3 = server.tryCreateChallenge(spA, spB);
            Assert.assertNotNull(ch3);
            oosB.writeObject(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_DECISION, true));
            oosB.flush();

            ObjectWrapper startA = readPerformative(oisA, ObjectWrapper.SERVER_START_MATCH);
            ObjectWrapper startB = readPerformative(oisB, ObjectWrapper.SERVER_START_MATCH);
            Assert.assertEquals(MatchResult.BLACK, ((MatchStartInfo) startA.getData()).getMyColor());
            Assert.assertEquals(MatchResult.WHITE, ((MatchStartInfo) startB.getData()).getMyColor());
            Assert.assertEquals(1, server.getMySessions().size());

            // N11 kết thúc phiên -> trả 2 người về IDLE
            server.onMatchSessionEnded(spA.getMatchSession(), spA, spB);
            Assert.assertEquals(0, server.getMySessions().size());
            Assert.assertEquals(PlayerStat.IDLE, spA.getStatus());
            Assert.assertEquals(PlayerStat.IDLE, spB.getStatus());

            // Kiểm tra không bị unused warning
            Assert.assertNotNull(oosC);
            Assert.assertNotNull(oisC);
        } finally {
            server.stopServer();
        }
    }

    @Test
    public void testRankingSortingCriteria() {
        PlayerRanking r1 = makeRank("an", 10f, 8f, 30f, 40f);
        PlayerRanking r2 = makeRank("binh", 10f, 9f, 50f, 20f);       // hơn TB đối thủ
        PlayerRanking r3 = makeRank("cuong", 10f, 8f, 25f, 10f);      // ít nước thắng hơn r1
        PlayerRanking r4 = makeRank("dung", 10f, 8f, -1f, 35f);       // chưa thắng (-1) xếp sau
        PlayerRanking r5 = makeRank("em", 10f, 8f, 30f, 50f);         // hơn nước thua so với r1
        PlayerRanking r6 = makeRank("alpha", 10f, 8f, 30f, 40f);      // bằng cả 4 chỉ số với r1 -> alpha < an

        List<PlayerRanking> list = new ArrayList<>();
        list.add(r4);
        list.add(r1);
        list.add(r5);
        list.add(r2);
        list.add(r6);
        list.add(r3);
        list.sort(PlayerDAO.RANKING_ORDER);

        Assert.assertEquals("binh", list.get(0).getUsername());
        Assert.assertEquals("cuong", list.get(1).getUsername());
        Assert.assertEquals("em", list.get(2).getUsername());
        Assert.assertEquals("alpha", list.get(3).getUsername());
        Assert.assertEquals("an", list.get(4).getUsername());
        Assert.assertEquals("dung", list.get(5).getUsername());
    }

    private static PlayerRanking makeRank(String username, float total, float opp, float winMoves, float loseMoves) {
        PlayerRanking r = new PlayerRanking();
        r.setUsername(username);
        r.setTotalPoint(total);
        r.setAvgOpponentPoint(opp);
        r.setAvgMovesWin(winMoves);
        r.setAvgMovesLose(loseMoves);
        return r;
    }
}
