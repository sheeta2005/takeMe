import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

// 只创建固定前缀的独立测试库；不从业务表复制任何账号或业务记录。
public class LoadTestFixture {
    static final String SCHEMA = "takeme_loadtest_20261004";
    public static void main(String[] args) throws Exception {
        String password = System.getenv("TAKEME_TEST_DB_PASSWORD");
        String username = System.getenv("TAKEME_TEST_DB_USER");
        String secret = System.getenv("TAKEME_TEST_JWT_SECRET");
        Path output = Path.of(args[0]).toAbsolutePath();
        Files.createDirectories(output);
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true",
                username, password); Statement s = c.createStatement()) {
            // 已存在时拒绝重建，避免覆盖之前的测量或误删测试状态。
            s.execute("CREATE DATABASE `" + SCHEMA + "` CHARACTER SET utf8mb4");
            s.execute("USE `" + SCHEMA + "`");
            for (String table : List.of("order", "order_item", "user", "volunteer", "admin", "message",
                    "address", "cart", "cart_item", "service_package", "payment_transaction",
                    "volunteer_points_record", "review", "approval", "volunteer_leave", "mq_outbox")) {
                s.execute("CREATE TABLE `" + table + "` LIKE takeme.`" + table + "`");
            }
            c.setAutoCommit(false);
            String loginPassword = UUID.randomUUID().toString();
            String hash = new BCryptPasswordEncoder(12).encode(loginPassword);
            for (String table : List.of("user", "volunteer", "admin")) {
                int count = table.equals("user") ? 2500 : table.equals("volunteer") ? 250 : 25;
                boolean admin = table.equals("admin");
                try (PreparedStatement p = c.prepareStatement("INSERT INTO `" + table
                        + "` (id, username, real_name, password" + (admin ? "" : ", phone, status") + ") VALUES (?,?,?,?"
                        + (admin ? "" : ",?,1") + ")")) {
                    for (int i = 1; i <= count; i++) {
                        p.setInt(1, i); p.setString(2, "load_" + table + i);
                        p.setString(3, "压测" + i); p.setString(4, hash);
                        if (!admin) p.setString(5, table.substring(0, 1) + i);
                        p.addBatch();
                    }
                    p.executeBatch();
                }
            }
            s.executeUpdate("UPDATE volunteer SET points=1000, service_days='0,1,2,3,4,5,6'");
            for (int i = 0; i < 5; i++) {
                s.executeUpdate("INSERT INTO service_package (id, name, type, price, description, status)"
                        + " VALUES (" + (i + 1) + ", '压测套餐" + i + "', " + i + ",100,'仅用于合成测试',1)");
            }
            c.commit();
            LocalDateTime appointment = LocalDateTime.now().plusHours(3);
            String date = appointment.toLocalDate().toString();
            String time = appointment.format(DateTimeFormatter.ofPattern("HH:mm"));
            try (PreparedStatement orders = c.prepareStatement("INSERT INTO `order` (id, order_no, user_id, total_price,"
                    + " service_date, service_time, address, status, create_time, complete_time)"
                    + " VALUES (?,?,?,200,?,?,'压测地址',?,?,?)");
                 PreparedStatement items = c.prepareStatement("INSERT INTO order_item (id, order_id, service_id,"
                    + " service_name, service_price, quantity, item_price, service_type, service_date, service_time,"
                    + " address, item_status, create_time) VALUES (?,?,3,'压测助餐',100,1,100,2,?,?,'压测地址',?,?)")) {
                for (int i = 1; i <= 101000; i++) {
                    boolean active = i > 100000;
                    LocalDateTime created = LocalDateTime.now().minusDays(active ? 0 : i % 365 + 1);
                    orders.setInt(1, i); orders.setString(2, "LOAD_" + i);
                    orders.setInt(3, (i - 1) % 2500 + 1);
                    orders.setString(4, date); orders.setString(5, time); orders.setInt(6, active ? 0 : 4);
                    orders.setObject(7, created); orders.setObject(8, active ? null : created.plusHours(2));
                    orders.addBatch();
                    for (int n = 0; n < 2; n++) {
                        items.setInt(1, 2 * i - 1 + n); items.setInt(2, i);
                        items.setString(3, date); items.setString(4, time);
                        items.setInt(5, active ? 0 : 4); items.setObject(6, created);
                        items.addBatch();
                    }
                    if (i % 1000 == 0) {
                        orders.executeBatch(); items.executeBatch(); c.commit();
                    }
                }
            }
            c.commit();
            // 预签名只测已登录业务流量；登录密码计算另用登录场景测量。
            try (var writer = Files.newBufferedWriter(output.resolve("accounts.csv"), StandardCharsets.UTF_8)) {
                writer.write("userId,userToken,volunteerId,volunteerToken,adminToken,existingOrderId\n");
                for (int i = 1; i <= 2500; i++) {
                    int v = (i - 1) % 250 + 1, a = (i - 1) % 25 + 1;
                    writer.write(i + "," + token(i, 2, secret) + "," + v + ","
                            + token(v, 1, secret) + "," + token(a, 0, secret) + "," + i + "\n");
                }
            }
            Files.writeString(output.resolve("login-password.txt"), loginPassword, StandardCharsets.UTF_8);
            System.out.println("合成数据：2500 老人、250 志愿者、25 管理员、101000 订单、202000 服务项");
        }
    }
    static String token(int id, int role, String secret) {
        return Jwts.builder().setClaims(Map.of("userId", (long)id, "role", role))
                .setIssuedAt(new java.util.Date()).setExpiration(new java.util.Date(System.currentTimeMillis() + 86400000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256).compact();
    }
}
