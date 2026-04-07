package common;

import infrastructure.database.DatabaseHelper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Simple DB connectivity smoke runner for Maven exec target.
 */
public class ConnectDBTest {

    public static void main(String[] args) {
        System.out.println("[ConnectDBTest] Bat dau kiem tra ket noi CSDL...");

        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DB_NAME() AS db_name")) {

            System.out.println("[ConnectDBTest] Ket noi thanh cong.");
            if (rs.next()) {
                System.out.println("[ConnectDBTest] Database hien tai: " + rs.getString("db_name"));
            } else {
                System.out.println("[ConnectDBTest] Khong lay duoc ten database.");
            }
        } catch (Throwable ex) {
            Throwable root = ex.getCause() != null ? ex.getCause() : ex;
            System.err.println("[ConnectDBTest] Ket noi that bai: " + root.getMessage());
            System.exit(1);
            return;
        }

        DatabaseHelper.close();
        System.out.println("[ConnectDBTest] Hoan tat kiem tra.");
    }
}
