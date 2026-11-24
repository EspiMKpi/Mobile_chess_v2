package client.control;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import client.view.DataReceiver;
import client.view.InviteDialog;
import client.view.LoginActivity;
import model.IPAddress;
import model.ObjectWrapper;
import model.Player;
import model.PlayerStat;

/**
 * Chủ: N04. Bộ điều khiển phía Client Android (singleton sống suốt vòng đời ứng dụng).
 * Tự tìm server qua UDP broadcast, quản lý Socket TCP, hàng đợi gửi tuần tự và điều phối gói tin.
 */
public class ClientCtr {

    public static final int DISCOVERY_PORT = 9999;

    private static ClientCtr instance;

    private IPAddress serverAddress;
    private volatile Socket mySocket;
    private volatile ObjectOutputStream oos;
    private volatile ObjectInputStream ois;
    private ClientListening myListening;
    private volatile boolean closing;

    private final List<ObjectWrapper> myFunction = new CopyOnWriteArrayList<>();
    private final List<ObjectWrapper> pendingMatchMessages = new ArrayList<>();
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Player myPlayer;
    private Activity currentActivity;
    private Context appContext;

    private ClientCtr() {
    }

    public static synchronized ClientCtr getInstance() {
        if (instance == null) {
            instance = new ClientCtr();
        }
        return instance;
    }

    public void setServerAddress(IPAddress a) {
        this.serverAddress = a;
    }

    public synchronized boolean isConnected() {
        return mySocket != null && !mySocket.isClosed() && mySocket.isConnected();
    }

    /** Tìm server trong mạng Wi-Fi (UDP broadcast) và máy ảo AVD (10.0.2.2). */
    private IPAddress discoverServer() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(2500);
            byte[] sendData = "DISCOVER_SERVER".getBytes(StandardCharsets.UTF_8);

            try {
                socket.send(new DatagramPacket(
                        sendData, sendData.length, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT));
            } catch (IOException ignored) {
            }
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    if (ni.isLoopback() || !ni.isUp()) {
                        continue;
                    }
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        InetAddress broadcast = ia.getBroadcast();
                        if (broadcast != null) {
                            try {
                                socket.send(new DatagramPacket(sendData, sendData.length, broadcast, DISCOVERY_PORT));
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            try {
                socket.send(new DatagramPacket(
                        sendData, sendData.length, InetAddress.getByName("10.0.2.2"), DISCOVERY_PORT));
            } catch (IOException ignored) {
            }

            byte[] recvBuf = new byte[256];
            DatagramPacket reply = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(reply);
            String portStr = new String(reply.getData(), 0, reply.getLength(), StandardCharsets.UTF_8).trim();
            int tcpPort = Integer.parseInt(portStr);
            return new IPAddress(reply.getAddress().getHostAddress(), tcpPort);
        } catch (Exception e) {
            return null;
        }
    }

    /** Kết nối tới server (tự tìm qua UDP nếu chưa kết nối). PHẢI gọi trên luồng nền. */
    public synchronized boolean openConnection() {
        if (isConnected()) {
            return true;
        }
        IPAddress found = discoverServer();
        if (found == null) {
            return false;
        }
        this.serverAddress = found;
        closing = false;
        Socket s = null;
        try {
            s = new Socket();
            s.connect(new InetSocketAddress(serverAddress.getHost(), serverAddress.getPort()), 5000);
            s.setKeepAlive(true);
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            oos = out;
            ois = in;
            mySocket = s;
            myListening = new ClientListening(s, in);
            myListening.start();
            return true;
        } catch (IOException e) {
            resetSocketOnly(s);
            return false;
        }
    }

    /** Đóng socket khi lỗi hoặc khi đang ở LoginActivity (KHÔNG xóa myFunction). */
    private void resetSocketOnly(Socket failedSocket) {
        Socket s = mySocket != null ? mySocket : failedSocket;
        mySocket = null;
        oos = null;
        ois = null;
        myListening = null;
        myPlayer = null;
        synchronized (pendingMatchMessages) {
            pendingMatchMessages.clear();
        }
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }

    /** Ngắt kết nối chủ động (đăng xuất). */
    public synchronized boolean closeConnection() {
        closing = true;
        Socket s = mySocket;
        mySocket = null;
        oos = null;
        ois = null;
        myListening = null;
        myPlayer = null;
        myFunction.clear();
        synchronized (pendingMatchMessages) {
            pendingMatchMessages.clear();
        }
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    /** Gửi gói tin lên server trên 1 luồng gửi tuần tự (gọi được từ main thread). */
    public boolean sendData(Object o) {
        final ObjectOutputStream out = oos;
        if (!isConnected() || out == null) {
            return false;
        }
        sendExecutor.execute(() -> {
            synchronized (out) {
                try {
                    out.writeObject(o);
                    out.flush();
                    out.reset();
                } catch (IOException ignored) {
                }
            }
        });
        return true;
    }

    public void register(int code, DataReceiver r) {
        for (ObjectWrapper w : myFunction) {
            if (w.getPerformative() == code && w.getData() == r) {
                return;
            }
        }
        myFunction.add(new ObjectWrapper(code, r));
    }

    public void unregister(DataReceiver r) {
        for (ObjectWrapper w : myFunction) {
            if (w.getData() == r) {
                myFunction.remove(w);
            }
        }
    }

    public List<ObjectWrapper> getActiveFunction() {
        return myFunction;
    }

    public Player getMyPlayer() {
        return myPlayer;
    }

    public void setMyPlayer(Player p) {
        this.myPlayer = p;
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(Activity a) {
        this.currentActivity = a;
        if (a != null && appContext == null) {
            appContext = a.getApplicationContext();
        }
    }

    /** N11 gọi cuối MatchPlayActivity.onCreate để nhận lại gói tin trận đấu đến sớm. */
    public void flushPendingMatchMessages(DataReceiver r) {
        if (r == null) {
            return;
        }
        List<ObjectWrapper> copy;
        synchronized (pendingMatchMessages) {
            copy = new ArrayList<>(pendingMatchMessages);
            pendingMatchMessages.clear();
        }
        for (ObjectWrapper d : copy) {
            r.receivedDataProcessing(d);
        }
    }

    private static boolean isMatchMessage(int code) {
        return code == ObjectWrapper.SERVER_END_GAME
                || code == ObjectWrapper.SERVER_RESTART_MATCH_SWAP_COLOR
                || code == ObjectWrapper.SERVER_END_SESSION_MATCH
                || code == ObjectWrapper.REPLY_MOVE_ILLEGAL
                || code == ObjectWrapper.SERVER_INFORM_MOVE;
    }

    private void handleGlobalInvite(PlayerStat inviter) {
        mainHandler.post(() -> {
            Activity a = currentActivity;
            if (a instanceof AppCompatActivity && !a.isFinishing() && !a.isDestroyed()) {
                FragmentManager fm = ((AppCompatActivity) a).getSupportFragmentManager();
                if (!fm.isStateSaved() && fm.findFragmentByTag(InviteDialog.TAG) == null) {
                    InviteDialog.newInstance(inviter).show(fm, InviteDialog.TAG);
                    return;
                }
            }
            sendData(new ObjectWrapper(ObjectWrapper.REPLY_CHALLENGE_DECISION, false));
        });
    }

    private class ClientListening extends Thread {
        private final Socket socket;
        private final ObjectInputStream in;

        ClientListening(Socket socket, ObjectInputStream in) {
            super("ClientListening");
            this.socket = socket;
            this.in = in;
        }

        @Override
        public void run() {
            try {
                while (!closing) {
                    Object o = in.readObject();
                    if (!(o instanceof ObjectWrapper)) {
                        continue;
                    }
                    ObjectWrapper data = (ObjectWrapper) o;
                    int code = data.getPerformative();

                    if (code == ObjectWrapper.SERVER_START_MATCH) {
                        synchronized (pendingMatchMessages) {
                            pendingMatchMessages.clear();
                        }
                    }

                    if (code == ObjectWrapper.SERVER_INFORM_CHALLENGE && data.getData() instanceof PlayerStat) {
                        handleGlobalInvite((PlayerStat) data.getData());
                        continue;
                    }

                    boolean dispatched = false;
                    for (ObjectWrapper fto : myFunction) {
                        if (fto.getPerformative() == code) {
                            DataReceiver r = (DataReceiver) fto.getData();
                            mainHandler.post(() -> r.receivedDataProcessing(data));
                            dispatched = true;
                        }
                    }

                    if (!dispatched && isMatchMessage(code)) {
                        synchronized (pendingMatchMessages) {
                            pendingMatchMessages.add(data);
                        }
                    }
                }
            } catch (Exception e) {
                if (closing || socket != mySocket) {
                    return;
                }
                Activity a = currentActivity;
                if (a instanceof LoginActivity) {
                    resetSocketOnly(socket);
                    mainHandler.post(() -> Toast.makeText(a, "Mất kết nối server", Toast.LENGTH_LONG).show());
                } else {
                    closeConnection();
                    Context ctx = a != null ? a : appContext;
                    if (ctx != null) {
                        mainHandler.post(() -> {
                            Toast.makeText(ctx, "Mất kết nối server", Toast.LENGTH_LONG).show();
                            Intent i = new Intent(ctx, LoginActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            ctx.startActivity(i);
                        });
                    }
                }
            }
        }
    }
}
