package server.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/** Chủ: N04. Lớp cha trừu tượng: giữ kết nối CSDL + hàm băm mật khẩu (hình 4.2b). */
public abstract class DAO {

    protected Connection con;

    /** Đọc server/config/db.properties rồi mở kết nối MySQL. Lỗi -> con = null (server vẫn chạy). */
    protected DAO() {
        // [HƯỚNG DẪN BỔ SUNG - An toàn đa luồng]
        // Hiện tại mỗi DAO mở một Connection `con` trong constructor và giữ nguyên.
        // Khi làm các thao tác có transaction (như MatchDAO.saveMatch) hoặc muốn tránh nghẽn nhiều luồng,
        // nên tách phần đọc db.properties ra hàm openNewConnection() và dùng try-with-resources cho từng truy vấn.
        try {
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
                    }
                }
            }
            String url = props.getProperty("url",
                    "jdbc:mysql://localhost:3306/chess?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh");
            String user = props.getProperty("user", "root");
            String pass = props.getProperty("password", "");

            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, user, pass);
        } catch (Exception e) {
            System.err.println("DAO: không kết nối được CSDL - " + e.getMessage());
        }
    }

    /** SHA-256, trả về 64 ký tự hex (khớp cột tblPlayer.password VARCHAR(64)). */
    protected String hashPassword(String raw) {
        if (raw == null) {
            return "";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
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
