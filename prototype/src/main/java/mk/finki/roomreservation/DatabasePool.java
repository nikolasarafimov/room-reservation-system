package mk.finki.roomreservation;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class DatabasePool {

    private static HikariDataSource dataSource;

    private DatabasePool() {
    }

    public static void initialize(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1800000);
        config.setPoolName("RoomReservationPool");

        dataSource = new HikariDataSource(config);

        System.out.println("Database connection pool initialized successfully.");
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("Database connection pool is not initialized.");
        }

        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            System.out.println("Database connection pool closed.");
        }
    }
}