package client.control;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import client.view.ClientMainActivity;
import client.view.DataReceiver;
import model.IPAddress;
import model.ObjectWrapper;
import model.Player;

/**
 * Chủ: N04. Bộ điều khiển phía Client Android - SINGLETON, sống suốt vòng đời ứng dụng
 * (Activity bị tạo/hủy liên tục nhưng kết nối Socket phải giữ nguyên).
 *  - Mọi I/O Socket chạy trên luồng nền (Android cấm mạng trên main thread).
 *  - ClientListening đọc gói tin rồi chuyển sang main thread qua mainHandler
 *    trước khi gọi receivedDataProcessing() của màn hình đã đăng ký.
 * Hợp đồng với N11: getInstance(), sendData(Object), getActiveFunction() - không đổi chữ ký.
 * Chi tiết: docs/BLUEPRINT_N04.md mục 4.1.
 */
public class ClientCtr {

    private static ClientCtr instance;

    private final CopyOnWriteArrayList<ObjectWrapper> myFunction = new CopyOnWriteArrayList<>();
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Context appContext;
    private IPAddress serverAddress;
    private Socket mySocket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private ClientListening myListening;
    private volatile boolean closing;

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

    /** Gọi một lần ở ClientMainActivity.onCreate - dùng để mở lại màn hình kết nối khi mất mạng. */
    public void init(Context context) {
        if (context != null) {
            this.appContext = context.getApplicationContext();
        }
    }

    public void setServerAddress(IPAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    public IPAddress getServerAddress() {
        return serverAddress;
    }

    /**
     * PHẢI gọi trên luồng nền. new Socket() + connect(timeout 5s) + setKeepAlive(true),
     * tạo oos TRƯỚC ois, khởi động myListening. Trả true nếu kết nối được.
     */
    public boolean openConnection() {
        if (serverAddress == null) {
            return false;
        }
        try {
            closing = false;
            if (mySocket != null && !mySocket.isClosed()) {
                try {
                    mySocket.close();
                } catch (IOException ignored) {
                }
            }

            mySocket = new Socket();
            mySocket.connect(new InetSocketAddress(serverAddress.getHost(), serverAddress.getPort()), 5000);
            mySocket.setKeepAlive(true);

            // Tạo OOS trước OIS
            oos = new ObjectOutputStream(mySocket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(mySocket.getInputStream());

            myListening = new ClientListening();
            myListening.start();
            return true;
        } catch (Exception e) {
            closing = true;
            try {
                if (mySocket != null) {
                    mySocket.close();
                }
            } catch (IOException ignored) {
            }
            return false;
        }
    }

    /** Đăng xuất: closing = true, đóng socket (ClientListening tự thoát), xóa myFunction. */
    public boolean closeConnection() {
        closing = true;
        myPlayer = null;
        try {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException ignored) {
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException ignored) {
                }
            }
            if (mySocket != null && !mySocket.isClosed()) {
                mySocket.close();
            }
        } catch (IOException ignored) {
        }
        myListening = null;
        myFunction.clear();
        return true;
    }

    public boolean isConnected() {
        return mySocket != null && mySocket.isConnected() && !mySocket.isClosed() && !closing;
    }

    /**
     * Gửi gói tin lên Server. Gọi được từ main thread: việc ghi Socket được đẩy sang sendExecutor
     * (1 luồng duy nhất -> giữ đúng thứ tự gói tin). Trả về false nếu chưa kết nối.
     */
    public boolean sendData(Object obj) {
        if (!isConnected() || oos == null) {
            return false;
        }
        sendExecutor.execute(() -> {
            try {
                synchronized (this) {
                    if (oos != null) {
                        oos.writeObject(obj);
                        oos.flush();
                        oos.reset();
                    }
                }
            } catch (IOException e) {
                // Lỗi ghi socket
            }
        });
        return true;
    }

    public List<ObjectWrapper> getActiveFunction() {
        return myFunction;
    }

    /** Tiện ích: đăng ký màn hình r nhận mã code. */
    public void register(int code, DataReceiver r) {
        if (r == null) return;
        myFunction.add(new ObjectWrapper(code, r));
    }

    /** Tiện ích: gỡ mọi đăng ký của màn hình r (gọi trong onDestroy). */
    public void unregister(DataReceiver r) {
        if (r == null) return;
        myFunction.removeIf(f -> f.getData() == r);
    }

    public Player getMyPlayer() { return myPlayer; }
    public void setMyPlayer(Player myPlayer) { this.myPlayer = myPlayer; }

    /** Activity đang hiển thị (BaseActivity cập nhật) - dùng để mở InviteDialog ở bất kỳ màn hình nào. */
    public Activity getCurrentActivity() { return currentActivity; }
    public void setCurrentActivity(Activity currentActivity) { this.currentActivity = currentActivity; }

    /** Luồng nền đọc gói tin từ Server. */
    private class ClientListening extends Thread {
        @Override
        public void run() {
            try {
                while (!closing && ois != null) {
                    ObjectWrapper d = (ObjectWrapper) ois.readObject();
                    if (d == null) continue;

                    for (ObjectWrapper fto : myFunction) {
                        if (fto.getPerformative() == d.getPerformative()) {
                            Object receiver = fto.getData();
                            if (receiver instanceof DataReceiver) {
                                mainHandler.post(() -> ((DataReceiver) receiver).receivedDataProcessing(d));
                            }
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                if (!closing) {
                    mainHandler.post(() -> {
                        if (appContext != null) {
                            Toast.makeText(appContext, "Mất kết nối server", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(appContext, ClientMainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            appContext.startActivity(intent);
                        }
                    });
                }
            }
        }
    }
}
