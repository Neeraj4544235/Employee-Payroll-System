package payroll;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Handles the SQLite connection. The database is a single file (payroll.db) created automatically. */
public class DBConnection {

    private static final String URL = "jdbc:sqlite:payroll.db";

    public static Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");   // needed for ON DELETE CASCADE
        }
        return c;
    }

    /** Creates tables if they do not exist. Call once at startup. */
    public static void init() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS employees ("
                    + "emp_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL,"
                    + "email TEXT,"
                    + "phone TEXT,"
                    + "department TEXT,"
                    + "designation TEXT,"
                    + "join_date TEXT,"
                    + "basic_salary REAL NOT NULL DEFAULT 0"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS salary ("
                    + "sal_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "emp_id INTEGER NOT NULL,"
                    + "month_year TEXT NOT NULL,"
                    + "basic REAL, hra REAL, da REAL, allowances REAL,"
                    + "pf REAL, tax REAL, other_deductions REAL,"
                    + "gross REAL, total_deductions REAL, net REAL,"
                    + "generated_on TEXT DEFAULT CURRENT_TIMESTAMP,"
                    + "UNIQUE (emp_id, month_year),"
                    + "FOREIGN KEY (emp_id) REFERENCES employees(emp_id) ON DELETE CASCADE"
                    + ")");
        }
    }
}
