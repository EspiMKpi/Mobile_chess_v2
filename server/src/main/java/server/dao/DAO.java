package server.dao;

import java.sql.Connection;
import java.util.Properties;

/** Chủ: N04. Lớp cha giữ kết nối CSDL + hàm băm mật khẩu dùng chung. */
public class DAO {

    protected static Connection con;

    public DAO() {
        // TODO: nếu con == null thì connect()
    }

    /** Trả kết nối dùng chung; nếu con null / đã đóng / !isValid(2) thì connect() lại. */
    public static synchronized Connection getConnection() {
        // TODO
        return null;
    }

    public static synchronized void setConnection(Connection connection) {
        con = connection;
    }

    /** Đọc db.properties: thử lần lượt config/db.properties, server/config/db.properties, rồi bản .example. */
    protected static Properties loadDbProperties() {
        // TODO
        return new Properties();
    }

    /**
     * Mở MỘT kết nối mới (không dùng chung), người gọi tự đóng. Lỗi -> null.
     * url/user/password lấy từ loadDbProperties(); driver com.mysql.cj.jdbc.Driver.
     */
    public static Connection openNewConnection() {
        // TODO
        return null;
    }

    private static void connect() {
        // TODO: con = openNewConnection();
    }

    /** SHA-256 -> chuỗi 64 ký tự hexa (khớp cột tblPlayer.password VARCHAR(64)). */
    public static String hashPassword(String plain) {
        // TODO: MessageDigest.getInstance("SHA-256"), UTF-8, format "%02x"
        return null;
    }
}
