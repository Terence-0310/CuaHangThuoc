package common;

import infrastructure.database.DatabaseHelper;

import java.sql.*;

/**
 * SystemLogger — Ghi nhận vết hệ thống (Audit Log)
 *
 * Tự động tạo bảng SystemLogs nếu chưa tồn tại.
 * Gọi SystemLogger.log(...) từ bất kỳ đâu trong app.
 *
 * Format: [TenUser] đã [HanhDong] cho [DoiTuong] vào lúc [ThoiGian]
 */
public class SystemLogger {

    private static volatile boolean tableEnsured = false;

    /**
     * Ghi log vào DB (dùng connection riêng, không ảnh hưởng transaction)
     *
     * @param hanhDong   "TRA_HANG", "HUY_HANG", "THEM_USER", "SUA_SAN_PHAM", ...
     * @param doiTuong   "Lô LOT-IBU-001 (MaLo=12)", "User admin02 (MaND=3)", ...
     * @param chiTiet    Chi tiết thêm (nullable)
     */
    public static void log(String hanhDong, String doiTuong, String chiTiet) {
        if (!tableEnsured) ensureTable();

        int maND = -1;
        String tenUser = "SYSTEM";
        if (Session.getCurrentUser() != null) {
            maND = Session.getCurrentUser().getMaND();
            tenUser = Session.getCurrentUser().getHoTen();
        }

        String sql = "INSERT INTO SystemLogs (MaND, TenUser, HanhDong, DoiTuong, ChiTiet) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (maND > 0) ps.setInt(1, maND); else ps.setNull(1, Types.INTEGER);
            ps.setNString(2, tenUser);
            ps.setNString(3, hanhDong);
            ps.setNString(4, doiTuong);
            ps.setNString(5, chiTiet != null ? chiTiet : "");
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silent — logging should never break the main flow
            System.err.println("[SystemLogger] Error: " + e.getMessage());
        }
    }

    /**
     * Ghi log TRONG CÙNG transaction (dùng connection đã có)
     */
    public static void logInTransaction(Connection conn, String hanhDong,
                                        String doiTuong, String chiTiet) throws SQLException {
        if (!tableEnsured) ensureTableWithConn(conn);

        int maND = -1;
        String tenUser = "SYSTEM";
        if (Session.getCurrentUser() != null) {
            maND = Session.getCurrentUser().getMaND();
            tenUser = Session.getCurrentUser().getHoTen();
        }

        String sql = "INSERT INTO SystemLogs (MaND, TenUser, HanhDong, DoiTuong, ChiTiet) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (maND > 0) ps.setInt(1, maND); else ps.setNull(1, Types.INTEGER);
            ps.setNString(2, tenUser);
            ps.setNString(3, hanhDong);
            ps.setNString(4, doiTuong);
            ps.setNString(5, chiTiet != null ? chiTiet : "");
            ps.executeUpdate();
        }
    }

    // ================================================================

    private static synchronized void ensureTable() {
        if (tableEnsured) return;
        try (Connection conn = DatabaseHelper.getConnection()) {
            ensureTableWithConn(conn);
        } catch (SQLException e) {
            System.err.println("[SystemLogger] Cannot ensure table: " + e.getMessage());
        }
    }

    private static void ensureTableWithConn(Connection conn) {
        if (tableEnsured) return;
        String ddl =
            "IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'SystemLogs') " +
            "CREATE TABLE SystemLogs (" +
            "  MaLog     INT IDENTITY(1,1) PRIMARY KEY, " +
            "  MaND      INT NULL, " +
            "  TenUser   NVARCHAR(100) NOT NULL, " +
            "  HanhDong  NVARCHAR(50)  NOT NULL, " +
            "  DoiTuong  NVARCHAR(200) NOT NULL, " +
            "  ChiTiet   NVARCHAR(500) NULL, " +
            "  ThoiGian  DATETIME NOT NULL DEFAULT GETDATE()" +
            ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
            tableEnsured = true;
        } catch (SQLException e) {
            // Silent
        }
    }
}
