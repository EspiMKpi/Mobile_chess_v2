package client.control;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        this.appContext = context.getApplicationContext();
    }

    public void setServerAddress(IPAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    /**
     * PHẢI gọi trên luồng nền. new Socket() + connect(timeout 5s) + setKeepAlive(true),
     * tạo oos TRƯỚC ois, khởi động myListening. Trả true nếu kết nối được.
     */
    public boolean openConnection() {
        // TODO(N04, M1)
        return false;
    }

    /** Đăng xuất: closing = true, đóng socket (ClientListening tự thoát), xóa myFunction. */
    public boolean closeConnection() {
        // TODO(N04, M1)
        return false;
    }

    public boolean isConnected() {
        return mySocket != null && mySocket.isConnected() && !mySocket.isClosed();
    }

    /**
     * Gửi gói tin lên Server. Gọi được từ main thread: việc ghi Socket được đẩy sang sendExecutor
     * (1 luồng duy nhất -> giữ đúng thứ tự gói tin). Trả về false nếu chưa kết nối.
     */
    public boolean sendData(Object obj) {
        // TODO(N04, M1): if (oos == null) return false;
        //   sendExecutor.execute(() -> { oos.writeObject(obj); oos.flush(); oos.reset(); });
        return false;
    }

    public List<ObjectWrapper> getActiveFunction() {
        return myFunction;
    }

    /** Tiện ích: đăng ký màn hình r nhận mã code. */
    public void register(int code, DataReceiver r) {
        myFunction.add(new ObjectWrapper(code, r));
    }

    /** Tiện ích: gỡ mọi đăng ký của màn hình r (gọi trong onDestroy). */
    public void unregister(DataReceiver r) {
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
            // TODO(N04, M1): while (true) { ObjectWrapper d = (ObjectWrapper) ois.readObject();
            //   for (ObjectWrapper fto : myFunction) if (fto.getPerformative() == d.getPerformative())
            //       mainHandler.post(() -> ((DataReceiver) fto.getData()).receivedDataProcessing(d)); }
            // IOException && !closing -> mainHandler.post: Toast "Mất kết nối server",
            //   mở ClientMainActivity với FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK (appContext)
        }
    }
}
