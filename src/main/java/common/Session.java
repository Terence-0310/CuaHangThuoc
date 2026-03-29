package common;

import domain.entity.User;
import infrastructure.database.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Session: Lưu thông tin user đang đăng nhập (SRP)
 */
public class Session {

    private static User currentUser;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        goOffline();
        currentUser = null;
    }

    /**
     * ★ Đánh dấu user đang online trong DB (giống Facebook)
     * Gọi sau khi login thành công.
     */
    public static void goOnline() {
        if (currentUser == null) return;
        updateOnlineStatus(currentUser.getMaND(), true);
    }

    /**
     * ★ Đánh dấu user offline trong DB
     * Gọi khi logout hoặc app đóng.
     */
    public static void goOffline() {
        if (currentUser == null) return;
        updateOnlineStatus(currentUser.getMaND(), false);
    }

    private static volatile boolean columnEnsured = false;

    private static void updateOnlineStatus(int maND, boolean online) {
        if (!columnEnsured) {
            ensureColumnExists();
        }
        String sql = "UPDATE NguoiDung SET DangOnline = ? WHERE MaND = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, online);
            ps.setInt(2, maND);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Silent fail — don't break app flow
        }
    }

    /**
     * Auto-migrate: tạo cột DangOnline nếu chưa tồn tại.
     * Chỉ chạy 1 lần duy nhất khi app khởi động.
     */
    private static synchronized void ensureColumnExists() {
        if (columnEnsured) return;
        String checkAndAdd =
            "IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('NguoiDung') AND name = 'DangOnline') " +
            "ALTER TABLE NguoiDung ADD DangOnline BIT NOT NULL DEFAULT 0";
        try (Connection conn = DatabaseHelper.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(checkAndAdd);
        } catch (SQLException e) {
            // Silent — column might already exist or DB doesn't support this syntax
        }
        columnEnsured = true;
    }

    /**
     * ★ Kiểm tra NV hiện tại đã chấm công chưa (có HR_Attendances ClockOut IS NULL hôm nay?)
     * Admin → luôn trả true (bypass).
     * NV không có trong HR_Employees → trả false.
     */
    public static boolean isClockedIn() {
        if (currentUser == null) return false;
        if (currentUser.isAdmin()) return true; // Admin bypass

        String sql =
            "SELECT 1 FROM HR_Attendances a " +
            "JOIN HR_Employees e ON a.EmpID = e.EmpID " +
            "WHERE e.MaND = ? AND a.ClockIn IS NOT NULL AND a.ClockOut IS NULL";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUser.getMaND());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false; // Fail-safe: chặn nếu lỗi
        }
    }
}
