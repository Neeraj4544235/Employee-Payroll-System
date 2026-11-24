package payroll;

import java.sql.Date;

public class Employee {
    public int id;
    public String name, email, phone, department, designation;
    public Date joinDate;
    public double basicSalary;

    public Employee() {}

    @Override
    public String toString() {
        return id + " - " + name;
    }
}
