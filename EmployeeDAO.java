package payroll;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** All database work for the employees table. */
public class EmployeeDAO {

    public void add(Employee e) throws SQLException {
        String sql = "INSERT INTO employees(name,email,phone,department,designation,join_date,basic_salary) VALUES(?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, e);
            ps.executeUpdate();
        }
    }

    public void update(Employee e) throws SQLException {
        String sql = "UPDATE employees SET name=?,email=?,phone=?,department=?,designation=?,join_date=?,basic_salary=? WHERE emp_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, e);
            ps.setInt(8, e.id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM employees WHERE emp_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Employee> all() throws SQLException {
        return filter("", "", null, null, "ID");
    }

    /**
     * Filter + sort in one query.
     * @param text  matches id, name, email or designation (blank = ignore)
     * @param dept  exact department (blank = all)
     * @param min   minimum basic salary (null = ignore)
     * @param max   maximum basic salary (null = ignore)
     * @param sort  one of the labels shown in the Sort dropdown
     */
    public List<Employee> filter(String text, String dept, Double min, Double max, String sort) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM employees WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (text != null && !text.trim().isEmpty()) {
            sql.append(" AND (CAST(emp_id AS TEXT) LIKE ? OR name LIKE ? OR email LIKE ? OR designation LIKE ?)");
            String q = "%" + text.trim() + "%";
            for (int i = 0; i < 4; i++) params.add(q);
        }
        if (dept != null && !dept.trim().isEmpty()) {
            sql.append(" AND department = ?");
            params.add(dept.trim());
        }
        if (min != null) { sql.append(" AND basic_salary >= ?"); params.add(min); }
        if (max != null) { sql.append(" AND basic_salary <= ?"); params.add(max); }
        sql.append(" ORDER BY ").append(orderBy(sort));

        List<Employee> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Employee e = new Employee();
                    e.id = rs.getInt("emp_id");
                    e.name = rs.getString("name");
                    e.email = rs.getString("email");
                    e.phone = rs.getString("phone");
                    e.department = rs.getString("department");
                    e.designation = rs.getString("designation");
                    String jd = rs.getString("join_date");
                    e.joinDate = (jd == null || jd.isEmpty()) ? null : Date.valueOf(jd);
                    e.basicSalary = rs.getDouble("basic_salary");
                    list.add(e);
                }
            }
        }
        return list;
    }

    private static String orderBy(String sort) {
        if (sort == null) return "emp_id";
        switch (sort) {
            case "Name (A-Z)": return "name COLLATE NOCASE";
            case "Basic Salary (High-Low)": return "basic_salary DESC";
            case "Basic Salary (Low-High)": return "basic_salary ASC";
            case "Join Date (Newest)": return "join_date DESC";
            default: return "emp_id";
        }
    }

    /** Distinct, non-empty department names (for the filter dropdowns). */
    public List<String> departments() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT department FROM employees WHERE department IS NOT NULL AND TRIM(department) <> '' ORDER BY department";
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString(1));
        }
        return list;
    }

    /** One row per department: {name, head-count, total basic, average basic}. */
    public List<Object[]> deptSummary() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT COALESCE(NULLIF(TRIM(department),''),'Unassigned') AS d, COUNT(*), SUM(basic_salary), AVG(basic_salary) "
                + "FROM employees GROUP BY d ORDER BY COUNT(*) DESC, d";
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new Object[]{rs.getString(1), rs.getInt(2), rs.getDouble(3), rs.getDouble(4)});
            }
        }
        return rows;
    }

    private void fill(PreparedStatement ps, Employee e) throws SQLException {
        ps.setString(1, e.name);
        ps.setString(2, e.email);
        ps.setString(3, e.phone);
        ps.setString(4, e.department);
        ps.setString(5, e.designation);
        ps.setString(6, e.joinDate == null ? null : e.joinDate.toString());
        ps.setDouble(7, e.basicSalary);
    }
}
