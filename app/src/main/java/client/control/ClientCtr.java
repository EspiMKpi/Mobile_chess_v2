package client.control;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

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
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// [HƯỚNG DẪN BƯỚC 3] Bỏ import ClientMainActivity và đổi thành LoginActivity:
// import client.view.LoginActivity;
import client.view.ClientMainActivity;
import client.view.DataReceiver;
import model.IPAddress;
import model.ObjectWrapper;
import model.Player;

/** Chủ: N04. Bộ điều khiển phía Client - dùng chung cho mọi Activity qua getInstance() (hình 4.2a). */
public class ClientCtr {

    private static ClientCtr instance;

    // [HƯỚNG DẪN BƯỚC 3] Cổng UDP khớp với ServerCtr.DISCOVERY_PORT:
    public static final int DISCOVERY_PORT = 9999;

    private IPAddress serverAddress;
    private Socket mySocket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private ClientListening myListening;
    private List<ObjectWrapper> myFunction = new CopyOnWriteArrayList<>();
    private Player myPlayer;
    private Activity currentActivity;

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

    public synchronized boolean isConnected(){
        return mySocket != null && !mySocket.isClosed() && mySocket.isConnected();
    }

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

    // [HƯỚNG DẪN BƯỚC 3 - Kiểm tra kết nối & Tự tìm server (Hình 4.2a, 4.3, 4.4)]
    // 1) Thêm hàm isConnected() để tránh mở lại socket khi đã kết nối sẵn (vd: từ RegisterActivity quay về LoginActivity):
    //
    // public synchronized boolean isConnected() {
    //     return mySocket != null && !mySocket.isClosed() && mySocket.isConnected();
    // }
    //
    // 2) Thêm hàm discoverServer() (Hình 4.2a) gửi gói UDP "DISCOVER_SERVER" để nhận cổng TCP từ ServerDiscovery:
    //    - Gửi broadcast LAN (255.255.255.255 + broadcast của từng NetworkInterface) cho điện thoại thật cùng Wi-Fi.
    //    - Gửi thêm tới 10.0.2.2 để máy ảo Android (AVD) cũng tìm được server chạy trên máy thật.
    //
    // private IPAddress discoverServer() {
    //     try (DatagramSocket socket = new DatagramSocket()) {
    //         socket.setBroadcast(true);
    //         socket.setSoTimeout(2500);
    //         byte[] sendData = "DISCOVER_SERVER".getBytes(StandardCharsets.UTF_8);
    //
    //         try {
    //             socket.send(new DatagramPacket(
    //                     sendData, sendData.length, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT));
    //         } catch (IOException ignored) {
    //         }
    //         try {
    //             Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    //             while (interfaces.hasMoreElements()) {
    //                 NetworkInterface ni = interfaces.nextElement();
    //                 if (ni.isLoopback() || !ni.isUp()) {
    //                     continue;
    //                 }
    //                 for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
    //                     InetAddress broadcast = ia.getBroadcast();
    //                     if (broadcast != null) {
    //                         try {
    //                             socket.send(new DatagramPacket(sendData, sendData.length, broadcast, DISCOVERY_PORT));
    //                         } catch (IOException ignored) {
    //                         }
    //                     }
    //                 }
    //             }
    //         } catch (Exception ignored) {
    //         }
    //         try {
    //             socket.send(new DatagramPacket(
    //                     sendData, sendData.length, InetAddress.getByName("10.0.2.2"), DISCOVERY_PORT));
    //         } catch (IOException ignored) {
    //         }
    //
    //         byte[] recvBuf = new byte[256];
    //         DatagramPacket reply = new DatagramPacket(recvBuf, recvBuf.length);
    //         socket.receive(reply);
    //         String portStr = new String(reply.getData(), 0, reply.getLength(), StandardCharsets.UTF_8).trim();
    //         int tcpPort = Integer.parseInt(portStr);
    //         return new IPAddress(reply.getAddress().getHostAddress(), tcpPort);
    //     } catch (Exception e) {
    //         return null;
    //     }
    // }
    //
    // 3) Sửa openConnection() thành synchronized: gọi isConnected() trước, sau đó gọi discoverServer(),
    //    và LƯU Ý khi kết nối lỗi KHÔNG gọi closeConnection() vì closeConnection() xóa mất myFunction.clear()
    //    (sẽ làm LoginActivity/RegisterActivity mất đăng ký nhận REPLY_LOGIN_USER / REPLY_REGISTER_USER!):
    //
    // public synchronized boolean openConnection() {
    //     if (isConnected()) {
    //         return true;
    //     }
    //     IPAddress found = discoverServer();
    //     if (found == null) {
    //         return false;
    //     }
    //     this.serverAddress = found;
    //     Socket s = null;
    //     try {
    //         s = new Socket();
    //         s.connect(new InetSocketAddress(serverAddress.getHost(), serverAddress.getPort()), 5000);
    //         oos = new ObjectOutputStream(s.getOutputStream());
    //         oos.flush();
    //         ois = new ObjectInputStream(s.getInputStream());
    //         mySocket = s;
    //         myListening = new ClientListening();
    //         myListening.start();
    //         return true;
    //     } catch (IOException e) {
    //         resetSocketOnly(s);
    //         return false;
    //     }
    // }
    //
    // private void resetSocketOnly(Socket failedSocket) {
    //     Socket s = mySocket != null ? mySocket : failedSocket;
    //     mySocket = null;
    //     oos = null;
    //     ois = null;
    //     myListening = null;
    //     myPlayer = null;
    //     if (s != null) {
    //         try {
    //             s.close();
    //         } catch (IOException ignored) {
    //         }
    //     }
    // }

    /** Kết nối tới server. Có truy cập mạng nên PHẢI gọi trên luồng nền. */
    public boolean openConnection() {
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(serverAddress.getHost(), serverAddress.getPort()), 5000);
            oos = new ObjectOutputStream(s.getOutputStream());    // tạo oos TRƯỚC ois
            oos.flush();
            ois = new ObjectInputStream(s.getInputStream());
            mySocket = s;
            myListening = new ClientListening();
            myListening.start();
            return true;
        } catch (IOException e) {
            closeConnection();
            return false;
        }
    }

    /** Ngắt kết nối chủ động (đăng xuất / kết nối lại). */
    public boolean closeConnection() {
        Socket s = mySocket;
        mySocket = null;          // đánh dấu: ClientListening biết đây là ngắt chủ động
        oos = null;
        ois = null;
        myListening = null;
        myPlayer = null;
        myFunction.clear();
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    /** Gửi gói tin lên server. Việc ghi socket chạy ở luồng nền vì Android cấm mạng trên luồng giao diện. */
    public boolean sendData(Object o) {
        final ObjectOutputStream out = oos;
        if (mySocket == null || out == null) {
            return false;
        }
        new Thread(() -> {
            synchronized (out) {
                try {
                    out.writeObject(o);
                    out.flush();
                    out.reset();
                } catch (IOException ignored) {
                    // mất kết nối: ClientListening sẽ xử lý
                }
            }
        }).start();
        return true;
    }

    /** Đăng ký màn hình r nhận gói tin có mã code. */
    public void register(int code, DataReceiver r) {
        myFunction.add(new ObjectWrapper(code, r));
    }

    /** Gỡ mọi đăng ký của màn hình r. */
    public void unregister(DataReceiver r) {
        for (ObjectWrapper w : myFunction) {
            if (w.getData() == r) {
                myFunction.remove(w);
            }
        }
    }

    public void setMyPlayer(Player p) { this.myPlayer = p; }
    public void setCurrentActivity(Activity a) { this.currentActivity = a; }

    // [HƯỚNG DẪN CÁC CHỨC NĂNG SAU]
    // 1) Mở thêm các getter cho EditPlayerActivity, OnlineListActivity, N11:
    // public Player getMyPlayer() { return myPlayer; }
    // public Activity getCurrentActivity() { return currentActivity; }
    // public List<ObjectWrapper> getActiveFunction() { return myFunction; }
    //
    // 2) Bộ đệm gói tin trận đấu đến sớm (giữa lúc nhận SERVER_START_MATCH và khi MatchPlayActivity chạy onCreate):
    // private final List<ObjectWrapper> pendingMatchMessages = new java.util.ArrayList<>();
    // public void flushPendingMatchMessages(DataReceiver r) {
    //     List<ObjectWrapper> copy;
    //     synchronized (pendingMatchMessages) {
    //         copy = new java.util.ArrayList<>(pendingMatchMessages);
    //         pendingMatchMessages.clear();
    //     }
    //     for (ObjectWrapper d : copy) r.receivedDataProcessing(d);
    // }

    /** Luồng nền đọc gói tin từ server, chuyển sang luồng giao diện rồi gọi màn hình đã đăng ký. */
    private class ClientListening extends Thread {

        public ClientListening() {
            super("ClientListening");
        }

        @Override
        public void run() {
            Socket s = mySocket;
            ObjectInputStream in = ois;
            Handler ui = new Handler(Looper.getMainLooper());
            try {
                while (true) {
                    Object o = in.readObject();
                    if (!(o instanceof ObjectWrapper)) {
                        continue;
                    }
                    ObjectWrapper data = (ObjectWrapper) o;
                    // [HƯỚNG DẪN BƯỚC THÁCH ĐẤU TOÀN CỤC - Hình 4.11, bước 25]
                    // Khi nhận SERVER_INFORM_CHALLENGE ở bất kỳ màn hình nào (kể cả lúc đang chơi với máy):
                    //  - Lấy Activity đang hiển thị: Activity a = currentActivity;
                    //  - Nếu a là AppCompatActivity && !a.getSupportFragmentManager().isStateSaved()
                    //    và chưa có InviteDialog đang mở -> ui.post(() -> InviteDialog.newInstance((PlayerStat) data.getData()).show(...));
                    //  - Ngược lại (app đang ở nền) -> tự gửi sendData(new ObjectWrapper(REPLY_CHALLENGE_DECISION, false));
                    //
                    // [HƯỚNG DẪN ĐỆM GÓI TRẬN ĐẤU]
                    //  - Nếu mã == SERVER_START_MATCH -> xóa pendingMatchMessages trước khi phát cho receiver.
                    //  - Nếu không có receiver nào nhận mã 21..24, 31..32 -> lưu vào pendingMatchMessages.
                    for (ObjectWrapper fto : myFunction) {
                        if (fto.getPerformative() == data.getPerformative()) {
                            DataReceiver r = (DataReceiver) fto.getData();
                            ui.post(() -> r.receivedDataProcessing(data));
                        }
                    }
                }
            } catch (Exception e) {
                if (mySocket == s) {
                    // [HƯỚNG DẪN BƯỚC 3] Đổi ClientMainActivity.class -> LoginActivity.class (báo cáo mục 1.3).
                    // Nếu người dùng đang ở sẵn LoginActivity thì chỉ dọn socket (giữ myFunction) và Toast thông báo:
                    //
                    // Activity a = currentActivity;
                    // if (a instanceof LoginActivity) {
                    //     resetSocketOnly(s);
                    //     ui.post(() -> Toast.makeText(a, "Mất kết nối server", Toast.LENGTH_LONG).show());
                    //     return;
                    // }
                    // closeConnection();
                    // if (a != null) {
                    //     ui.post(() -> {
                    //         Toast.makeText(a, "Mất kết nối server", Toast.LENGTH_LONG).show();
                    //         Intent i = new Intent(a, LoginActivity.class);
                    //         i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    //         a.startActivity(i);
                    //     });
                    // }

                    // Mất kết nối ngoài ý muốn (server tắt / tài khoản đăng nhập ở máy khác) -> về màn hình kết nối
                    closeConnection();
                    Activity a = currentActivity;
                    if (a != null) {
                        ui.post(() -> {
                            Toast.makeText(a, "Mất kết nối server", Toast.LENGTH_LONG).show();
                            Intent i = new Intent(a, ClientMainActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            a.startActivity(i);
                        });
                    }
                }
            }
        }
    }
}
