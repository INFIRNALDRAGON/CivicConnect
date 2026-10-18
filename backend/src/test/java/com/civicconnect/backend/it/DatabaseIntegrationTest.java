package com.civicconnect.backend.it;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.auth.secret=integration-test-secret-integration-test-secret")
@ActiveProfiles("dev")
@Import(FaultInjection.class)
abstract class DatabaseIntegrationTest {

    @Value("${local.server.port}")
    int port;

    @Value("${spring.flyway.url}")
    String jdbcUrl;

    @Value("${spring.flyway.user}")
    String ownerUser;

    @Value("${spring.flyway.password}")
    String ownerPassword;

    @Value("${spring.datasource.username}")
    String appUser;

    @Value("${spring.datasource.password}")
    String appPassword;

    @BeforeAll
    static void requireDatabase() {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        int dbPort = Integer.parseInt(System.getenv().getOrDefault("DB_PORT", "5433"));
        boolean reachable;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, dbPort), 1500);
            reachable = true;
        } catch (java.io.IOException e) {
            reachable = false;
        }
        if (!reachable && System.getenv("CI") != null) {
            fail("PostgreSQL is not reachable at " + host + ":" + dbPort + " but CI requires the integration tests to run");
        }
        assumeTrue(reachable, "PostgreSQL is not reachable at " + host + ":" + dbPort
                + " - start it with: docker compose up -d db (integration tests skipped)");
    }

    Api api() {
        return new Api(port);
    }

    Connection owner() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, ownerUser, ownerPassword);
    }

    Connection app() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, appUser, appPassword);
    }

    String categoryId(String name) throws SQLException {
        return queryOne("SELECT id::text FROM categories WHERE name = ?", name);
    }

    String queryOne(String sql, Object... args) throws SQLException {
        List<String> rows = queryColumn(sql, args);
        if (rows.isEmpty()) {
            throw new AssertionError("No row for: " + sql);
        }
        return rows.get(0);
    }

    List<String> queryColumn(String sql, Object... args) throws SQLException {
        try (Connection c = owner(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
                return out;
            }
        }
    }

    long count(String sql, Object... args) throws SQLException {
        return Long.parseLong(queryOne(sql, args));
    }

    void execute(String sql, Object... args) throws SQLException {
        try (Connection c = owner(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
        }
    }

    static String unique(String prefix) {
        return prefix + " " + System.nanoTime();
    }
}
