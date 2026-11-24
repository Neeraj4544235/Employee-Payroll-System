package payroll;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        Theme.install();

        try {
            DBConnection.init();   // creates payroll.db + tables if missing
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Could not open the SQLite database:\n" + e.getMessage()
                            + "\n\nCheck that lib/sqlite-jdbc-*.jar exists (run.sh / run.bat download it).",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
