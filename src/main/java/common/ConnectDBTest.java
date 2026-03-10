package common;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Class test kết nối database
 * 
 * @author Generated
 * @version 1.0
 */
public class ConnectDBTest {

	public static void main(String[] args) {
		System.out.println("========================================");
		System.out.println("  TEST KẾT NỐI DATABASE");
		System.out.println("========================================\n");

		// Test 1: Kiểm tra Connection Pool
		testConnectionPool();

		// Test 2: Kiểm tra lấy Connection
		testGetConnection();

		// Test 3: Kiểm tra thực hiện query
		testQuery();

		// Test 4: Hiển thị thông tin database
		testDatabaseInfo();

		// Test 5: Hiển thị thông tin pool
		displayPoolInfo();

		System.out.println("\n========================================");
		System.out.println("  TEST HOÀN TẤT");
		System.out.println("========================================");
	}

	/**
	 * Test 1: Kiểm tra Connection Pool có khởi tạo thành công không
	 */
	private static void testConnectionPool() {
		System.out.println("[Test 1] Kiểm tra Connection Pool...");
		try {
			if (ConnectDB.isConnectionPoolActive()) {
				System.out.println("✓ Connection Pool đang hoạt động");
			} else {
				System.out.println("✗ Connection Pool chưa được khởi tạo hoặc đã đóng");
			}
		} catch (Exception e) {
			System.err.println("✗ Lỗi kiểm tra Connection Pool: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println();
	}

	/**
	 * Test 2: Kiểm tra lấy Connection từ pool
	 */
	private static void testGetConnection() {
		System.out.println("[Test 2] Kiểm tra lấy Connection...");
		try (Connection con = ConnectDB.getCon()) {
			if (con != null && !con.isClosed()) {
				System.out.println("✓ Lấy Connection thành công");
				System.out.println("  - Connection class: " + con.getClass().getName());
				System.out.println("  - Auto commit: " + con.getAutoCommit());
				System.out.println("  - Read only: " + con.isReadOnly());
			} else {
				System.out.println("✗ Connection null hoặc đã đóng");
			}
		} catch (Exception e) {
			System.err.println("✗ Lỗi khi lấy Connection: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println();
	}

	/**
	 * Test 3: Kiểm tra thực hiện query đơn giản
	 */
	private static void testQuery() {
		System.out.println("[Test 3] Kiểm tra thực hiện Query...");
		try (
			Connection con = ConnectDB.getCon();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT @@VERSION AS Version, DB_NAME() AS DatabaseName, GETDATE() AS CurrentTime");
		) {
			if (rs.next()) {
				System.out.println("✓ Query thực hiện thành công");
				System.out.println("  - Database: " + rs.getString("DatabaseName"));
				String version = rs.getString("Version");
				System.out.println("  - SQL Server Version: " + version.substring(0, Math.min(50, version.length())) + "...");
				System.out.println("  - Current Time: " + rs.getTimestamp("CurrentTime"));
			} else {
				System.out.println("✗ Query không trả về kết quả");
			}
		} catch (Exception e) {
			System.err.println("✗ Lỗi khi thực hiện Query: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println();
	}

	/**
	 * Test 4: Hiển thị thông tin database
	 */
	private static void testDatabaseInfo() {
		System.out.println("[Test 4] Thông tin Database...");
		try (
			Connection con = ConnectDB.getCon();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(
				"SELECT " +
				"  DB_NAME() AS DatabaseName, " +
				"  USER_NAME() AS CurrentUser, " +
				"  @@SERVERNAME AS ServerName, " +
				"  @@VERSION AS SQLVersion"
			);
		) {
			if (rs.next()) {
				System.out.println("✓ Thông tin Database:");
				System.out.println("  - Database Name: " + rs.getString("DatabaseName"));
				System.out.println("  - Current User: " + rs.getString("CurrentUser"));
				System.out.println("  - Server Name: " + rs.getString("ServerName"));
			}
		} catch (Exception e) {
			System.err.println("✗ Lỗi khi lấy thông tin Database: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println();
	}

	/**
	 * Test 5: Hiển thị thông tin Connection Pool
	 */
	private static void displayPoolInfo() {
		System.out.println("[Test 5] Thông tin Connection Pool...");
		try {
			String poolInfo = ConnectDB.getPoolInfo();
			System.out.println("✓ " + poolInfo);
		} catch (Exception e) {
			System.err.println("✗ Lỗi khi lấy thông tin Pool: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println();
	}
}
