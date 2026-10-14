package com.civicconnect.backend.controller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEV ONLY diagnostic: proves the backend really reaches the database and shows
 * WHICH database role it connected as (should be civicconnect_app).
 * Disabled outside the "dev" profile so it never leaks server details in staging/production.
 */
@Profile("dev")
@RestController
@RequestMapping("/api/health")
public class DatabaseHealthController {

    private final DataSource dataSource;

    public DatabaseHealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/db")
    public Map<String, Object> db() throws SQLException {
        Map<String, Object> info = new LinkedHashMap<>();
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery(
                    "select current_database(), current_user, version()")) {
                rs.next();
                info.put("status", "UP");
                info.put("database", rs.getString(1));
                info.put("connectedAs", rs.getString(2));
                info.put("serverVersion", rs.getString(3));
            }
            try (ResultSet rs = st.executeQuery("select count(*) from categories")) {
                rs.next();
                info.put("categoryCount", rs.getInt(1));
            }
        }
        return info;
    }
}
