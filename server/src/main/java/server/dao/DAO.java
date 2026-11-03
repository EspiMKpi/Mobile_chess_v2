package server.dao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;

/** Chủ: N04. Lớp cha giữ kết nối CSDL + hàm băm mật khẩu dùng chung. */
public class DAO {

    protected static Connection con;

    public DAO() {
        // TODO(N04): nếu con == null thì DriverManager.getConnection(jdbc:mysql://..., user, pass)
    }

    /** SHA-256, trả về 64 ký tự hexa (khớp cột tblPlayer.password VARCHAR(64)). */
    protected static String hashPassword(String plain) {
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
