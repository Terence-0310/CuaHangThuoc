package infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Infrastructure: HikariCP Connection Pool
 * Đọc config từ application.properties
 */
public class DatabaseHelper {

    private static HikariDataSource dataSource;

    static {
        try {
            Properties props = new Properties();
            InputStream is = DatabaseHelper.class.getClassLoader()
                    .getResourceAsStream("application.properties");
            if (is != null) {
                props.load(is);
                is.close();
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url"));
            String username = props.getProperty("db.user", "").trim();
            String password = props.getProperty("db.password", "");
            if (!username.isEmpty()) {
                config.setUsername(username);
                config.setPassword(password);
            }
            config.setDriverClassName(props.getProperty("db.driver"));

            config.setMinimumIdle(Integer.parseInt(
                    props.getProperty("db.pool.minimumIdle", "2")));
            config.setMaximumPoolSize(Integer.parseInt(
                    props.getProperty("db.pool.maximumPoolSize", "10")));
            config.setConnectionTimeout(Long.parseLong(
                    props.getProperty("db.pool.connectionTimeout", "5000")));
            config.setIdleTimeout(Long.parseLong(
                    props.getProperty("db.pool.idleTimeout", "300000")));
            config.setMaxLifetime(Long.parseLong(
                    props.getProperty("db.pool.maxLifetime", "600000")));

            dataSource = new HikariDataSource(config);
            System.out.println("[DatabaseHelper] Connection Pool initialized successfully.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database config", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
