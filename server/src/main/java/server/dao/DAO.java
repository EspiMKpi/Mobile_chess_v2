package server.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Chủ: N04. Lớp cha giữ kết nối CSDL + hàm băm mật khẩu dùng chung. */
public class DAO {

    protected static Connection con;

    public DAO() {
        if (con == null) {
            connect();
        }
    }

    public static synchronized Connection getConnection() {
        try {
            if (con == null || con.isClosed() || !con.isValid(2)) {
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return con;
    }

    public static synchronized void setConnection(Connection connection) {
        con = connection;
    }

    /** Tách đọc cấu hình db.properties. */
    protected static Properties loadDbProperties() {
        Properties props = new Properties();
        String[] possiblePaths = {
                "config/db.properties",
                "server/config/db.properties",
                "config/db.properties.example",
                "server/config/db.properties.example"
        };

        for (String path : possiblePaths) {
            File f = new File(path);
            if (f.exists()) {
                try (InputStream in = new FileInputStream(f)) {
                    props.load(in);
                    break;
                } catch (Exception ignored) {
                }
            }
        }
        return props;
    }

    /** Mở MỘT kết nối mới, không dùng chung. Người gọi phải tự đóng. Trả null nếu lỗi. */
    public static Connection openNewConnection() {
        try {
            Properties props = loadDbProperties();
            String url = props.getProperty("url",
                    "jdbc:mysql://localhost:3306/chess?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh");
            String user = props.getProperty("user", "root");
            String pass = props.getProperty("password", "");

            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(url, user, pass);
        } catch (Exception e) {
            System.err.println("DAO Database connection note: " + e.getMessage());
            return null;
        }
    }

    private static void connect() {
        con = openNewConnection();
    }

    /** SHA-256, trả về 64 ký tự hexa (khớp cột tblPlayer.password VARCHAR(64)). */
    public static String hashPassword(String plain) {
        if (plain == null) {
            return "";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
