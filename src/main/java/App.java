import com.formdev.flatlaf.FlatLightLaf;
import presentation.LoginFrame;

import javax.swing.*;
import java.awt.*;

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

        // Launch Login
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
