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

            String jdbcUrl = envOrProperty(props, "db.url", "DB_URL", "MEPHAR_DB_URL");
            String user = envOrProperty(props, "db.user", "DB_USER", "MEPHAR_DB_USER");
            String password = envOrProperty(props, "db.password", "DB_PASSWORD", "MEPHAR_DB_PASSWORD");
            String driver = envOrProperty(props, "db.driver", "DB_DRIVER", "MEPHAR_DB_DRIVER");

            if (jdbcUrl == null || jdbcUrl.isBlank()) {
                throw new IllegalStateException("Database URL is missing. Set DB_URL (or MEPHAR_DB_URL) or db.url.");
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            if (user != null) {
                config.setUsername(user);
            }
            if (password != null) {
                config.setPassword(password);
            }
            if (driver != null && !driver.isBlank()) {
                config.setDriverClassName(driver);
            }

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
            System.out.println("[DatabaseHelper] Connection Pool initialized. URL="
                    + maskJdbcUrl(jdbcUrl) + ", USER=" + maskUser(user));
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

    private static String firstNonBlank(String envValue, String propertyValue) {
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        return null;
    }

    private static String envOrProperty(Properties props, String propertyKey, String... envKeys) {
        for (String envKey : envKeys) {
            String value = System.getenv(envKey);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return firstNonBlank(null, props.getProperty(propertyKey));
    }

    private static String maskUser(String user) {
        if (user == null || user.isBlank()) {
            return "<empty>";
        }
        return user;
    }

    private static String maskJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "<empty>";
        }
        return jdbcUrl.replaceAll("(?i)(password=)[^;]*", "$1***");
    }
}
