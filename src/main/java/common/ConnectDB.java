package common;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Class quản lý kết nối database với HikariCP Connection Pooling
 * 
 * @author Generated
 * @version 2.0
 */
public class ConnectDB {
	// Thông tin kết nối database
	private static final String SERVER_NAME = "DESKTOP-H716U5Q";
	private static final String PORT = "1433";
	private static final String DATABASE_NAME = "CuaHangThuoc_Batch";
	private static final String USER = "sa";
	private static final String PASSWORD = "123456";
	
	// HikariCP DataSource (Connection Pool)
	private static HikariDataSource dataSource;

	// Khởi tạo connection pool khi class được load
	static {
		initializeConnectionPool();
	}

	/**
	 * Khởi tạo HikariCP Connection Pool với cấu hình tối ưu
	 */
	private static void initializeConnectionPool() {
		try {
			HikariConfig config = new HikariConfig();
			
			// Connection string với format chuẩn (databaseName thay vì databasename)
			String jdbcUrl = String.format(
				"jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
				SERVER_NAME, PORT, DATABASE_NAME
			);
			
			config.setJdbcUrl(jdbcUrl);
			config.setUsername(USER);
			config.setPassword(PASSWORD);
			
			// Cấu hình connection pool
			config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			config.setMinimumIdle(5);              // Số connection tối thiểu trong pool
			config.setMaximumPoolSize(20);          // Số connection tối đa trong pool
			config.setConnectionTimeout(30000);     // Timeout khi lấy connection (30 giây)
			config.setIdleTimeout(600000);          // Timeout khi connection idle (10 phút)
			config.setMaxLifetime(1800000);         // Thời gian sống tối đa của connection (30 phút)
			config.setLeakDetectionThreshold(60000); // Phát hiện connection leak (60 giây)
			
			// Cấu hình connection properties
			config.addDataSourceProperty("cachePrepStmts", "true");
			config.addDataSourceProperty("prepStmtCacheSize", "250");
			config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
			
			// Tạo DataSource
			dataSource = new HikariDataSource(config);
			
			System.out.println("✓ Connection Pool đã được khởi tạo thành công!");
			
		} catch (Exception e) {
			System.err.println("✗ Lỗi khởi tạo Connection Pool: " + e.getMessage());
			e.printStackTrace();
			dataSource = null;
		}
	}

	/**
	 * Lấy connection từ pool
	 * Connection sẽ tự động được trả về pool khi đóng (try-with-resources)
	 * 
	 * @return Connection từ pool, null nếu có lỗi
	 */
	public static Connection getCon() {
		try {
			if (dataSource == null || dataSource.isClosed()) {
				System.err.println("✗ Connection Pool chưa được khởi tạo hoặc đã đóng!");
				return null;
			}
			
			Connection connection = dataSource.getConnection();
			
			// Kiểm tra connection có hợp lệ không
			if (connection != null && !connection.isClosed()) {
				return connection;
			}
			
		} catch (SQLException e) {
			System.err.println("✗ Lỗi khi lấy connection từ pool: " + e.getMessage());
			e.printStackTrace();
		}
		
		return null;
	}

	/**
	 * Đóng connection pool (gọi khi ứng dụng shutdown)
	 */
	public static void closeConnectionPool() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
			System.out.println("✓ Connection Pool đã được đóng!");
		}
	}

	/**
	 * Kiểm tra trạng thái connection pool
	 * 
	 * @return true nếu pool đang hoạt động
	 */
	public static boolean isConnectionPoolActive() {
		return dataSource != null && !dataSource.isClosed();
	}

	/**
	 * Lấy thông tin connection pool (để debug/monitoring)
	 */
	public static String getPoolInfo() {
		if (dataSource == null || dataSource.isClosed()) {
			return "Connection Pool chưa được khởi tạo";
		}
		
		return String.format(
			"Pool Status - Active: %d, Idle: %d, Total: %d, Waiting: %d",
			dataSource.getHikariPoolMXBean().getActiveConnections(),
			dataSource.getHikariPoolMXBean().getIdleConnections(),
			dataSource.getHikariPoolMXBean().getTotalConnections(),
			dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
		);
	}
}







