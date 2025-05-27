import gui.MainWindow;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow window = new MainWindow();
                window.setSize(1200, 800);  // Set explicit window size
                window.setLocationRelativeTo(null);  // Center on screen
                window.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error creating window: " + e.getMessage());
            }
        });
    }
}
