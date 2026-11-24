package server.control;

import model.ObjectWrapper;

/**
 * Hợp đồng phía Server giữa N04 (trước khi vào trận) và N11 (trong trận đối kháng).
 *
 * Khi lời mời được đối thủ đồng ý, ServerCtr tạo phòng đấu (GameRoom cài đặt MatchSession),
 * gắn vào cả hai ServerProcessing rồi gọi start().
 * Từ thời điểm đó, mọi gói tin trong trận (mã 20..24, 30..39) hoặc sự kiện mất kết nối
 * sẽ được ServerProcessing chuyển tiếp sang MatchSession này.
 */
public interface MatchSession {

    /** Bắt đầu trận đấu (tạo Match, reset trọng tài/đồng hồ, gửi SERVER_START_MATCH cho 2 bên). */
    void start();

    /** Nhận gói tin trong trận (QUIT_GAME, REQUEST_CONTINUE_MATCH, SEND_MOVE, ...). */
    void onPacket(ServerCtr.ServerProcessing sender, ObjectWrapper packet);

    /** Một trong hai người chơi bị ngắt kết nối (tắt app / rớt mạng) khi đang ở trong phòng. */
    void onDisconnect(ServerCtr.ServerProcessing player);
}
