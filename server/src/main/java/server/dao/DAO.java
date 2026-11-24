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

/** Chủ: N04. Lớp cha quản lý kết nối MySQL (mỗi thao tác mở kết nối riêng an toàn đa luồng) và băm SHA-256. */
public abstract class DAO {

    protected DAO() {
    }

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

    /** Mở một kết nối MySQL mới; người gọi đóng bằng try-with-resources. Trả null nếu lỗi. */
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
            System.err.println("DAO: không kết nối được CSDL - " + e.getMessage());
            return null;
        }
    }

    /** SHA-256, trả về 64 ký tự hex (khớp cột tblPlayer.password VARCHAR(64)). */
    public static String hashPassword(String raw) {
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
