package payroll;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** All database work for the salary table. */
public class SalaryDAO {

    /** Saves a salary record; generating the same employee + month again overwrites it. */
    public void save(SalaryRecord r) throws SQLException {
        String sql = "INSERT INTO salary(emp_id,month_year,basic,hra,da,allowances,pf,tax,other_deductions,gross,total_deductions,net) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(emp_id, month_year) DO UPDATE SET "
                + "basic=excluded.basic,hra=excluded.hra,da=excluded.da,allowances=excluded.allowances,pf=excluded.pf,"
                + "tax=excluded.tax,other_deductions=excluded.other_deductions,gross=excluded.gross,"
                + "total_deductions=excluded.total_deductions,net=excluded.net,generated_on=CURRENT_TIMESTAMP";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.empId);
            ps.setString(2, r.monthYear);
            ps.setDouble(3, r.basic);
            ps.setDouble(4, r.hra);
            ps.setDouble(5, r.da);
            ps.setDouble(6, r.allowances);
            ps.setDouble(7, r.pf);
            ps.setDouble(8, r.tax);
            ps.setDouble(9, r.otherDeductions);
            ps.setDouble(10, r.gross);
            ps.setDouble(11, r.totalDeductions);
            ps.setDouble(12, r.net);
            ps.executeUpdate();
        }
    }

    public void delete(int salId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM salary WHERE sal_id=?")) {
            ps.setInt(1, salId);
            ps.executeUpdate();
        }
    }

    /**
     * @param monthYear like "2026-10" (blank = all months)
     * @param dept      exact department (blank = all)
     * @param nameText  part of the employee name (blank = all)
     */
    public List<SalaryRecord> list(String monthYear, String dept, String nameText) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT s.*, e.name, COALESCE(NULLIF(TRIM(e.department),''),'Unassigned') AS dept "
                        + "FROM salary s JOIN employees e ON e.emp_id=s.emp_id WHERE 1=1");
        List<String> params = new ArrayList<>();
        if (monthYear != null && !monthYear.trim().isEmpty()) { sql.append(" AND s.month_year=?"); params.add(monthYear.trim()); }
        if (dept != null && !dept.trim().isEmpty()) { sql.append(" AND e.department=?"); params.add(dept.trim()); }
        if (nameText != null && !nameText.trim().isEmpty()) { sql.append(" AND e.name LIKE ?"); params.add("%" + nameText.trim() + "%"); }
        sql.append(" ORDER BY s.month_year DESC, s.emp_id");

        List<SalaryRecord> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setString(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SalaryRecord r = new SalaryRecord();
                    r.salId = rs.getInt("sal_id");
                    r.empId = rs.getInt("emp_id");
                    r.empName = rs.getString("name");
                    r.department = rs.getString("dept");
                    r.monthYear = rs.getString("month_year");
                    r.basic = rs.getDouble("basic");
                    r.hra = rs.getDouble("hra");
                    r.da = rs.getDouble("da");
                    r.allowances = rs.getDouble("allowances");
                    r.pf = rs.getDouble("pf");
                    r.tax = rs.getDouble("tax");
                    r.otherDeductions = rs.getDouble("other_deductions");
                    r.gross = rs.getDouble("gross");
                    r.totalDeductions = rs.getDouble("total_deductions");
                    r.net = rs.getDouble("net");
                    list.add(r);
                }
            }
        }
        return list;
    }
}
