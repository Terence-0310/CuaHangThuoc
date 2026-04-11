import com.formdev.flatlaf.FlatLightLaf;
import infrastructure.database.DatabaseHelper;
import presentation.LoginFrame;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

/**
 * Entry Point: FlatLaf Light + Apothecary Pro theme
 */
public class App {
    public static void main(String[] args) {
        // Set FlatLaf Light (phù hợp Apothecary Pro palette)
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());

            // Custom UI defaults cho Apothecary Pro
            UIManager.put("Button.arc", 6);
            UIManager.put("Component.arc", 6);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));

        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf: " + e.getMessage());
        }

        // Kiem tra CSDL truoc khi mo dang nhap (tranh loi user rong / SQL khong chay)
        SwingUtilities.invokeLater(() -> {
            try (Connection ignored = DatabaseHelper.getConnection()) {
                // ket noi OK
            } catch (Throwable ex) {
                Throwable root = ex.getCause() != null ? ex.getCause() : ex;
                JOptionPane.showMessageDialog(null,
                        "Không kết nối được SQL Server trước khi đăng nhập.\n\n"
                                + root.getMessage()
                                + "\n\nKiểm tra:\n"
                                + "• SQL Server đang chạy, TCP/IP (ví dụ cổng 1433)\n"
                                + "• Database QuanLyCuaHangThuoc đã tạo (chạy database/run_all_migrations.ps1)\n"
                                + "• db.user / db.password trong application.properties\n"
                                + "  hoặc biến môi trường DB_USER, DB_PASSWORD, DB_URL",
                        "MerPhar — Lỗi CSDL",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }

            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
