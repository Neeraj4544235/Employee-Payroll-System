package payroll;

/**
 * Salary calculation rules (simplified, easy to explain in viva):
 *   HRA   = 20% of basic
 *   DA    = 10% of basic
 *   Gross = Basic + HRA + DA + Allowances
 *   PF    = 12% of basic
 *   Tax   = monthly income tax from annual slabs on (Gross x 12)
 *   Net   = Gross - (PF + Tax + Other Deductions)
 */
public class PayrollCalculator {

    public static final double HRA_RATE = 0.20;
    public static final double DA_RATE = 0.10;
    public static final double PF_RATE = 0.12;

    public static SalaryRecord calculate(Employee e, String monthYear,
                                         double allowances, double otherDeductions) {
        SalaryRecord r = new SalaryRecord();
        r.empId = e.id;
        r.empName = e.name;
        r.monthYear = monthYear;
        r.basic = round(e.basicSalary);
        r.hra = round(e.basicSalary * HRA_RATE);
        r.da = round(e.basicSalary * DA_RATE);
        r.allowances = round(allowances);
        r.gross = round(r.basic + r.hra + r.da + r.allowances);
        r.pf = round(e.basicSalary * PF_RATE);
        r.tax = round(annualTax(r.gross * 12) / 12.0);
        r.otherDeductions = round(otherDeductions);
        r.totalDeductions = round(r.pf + r.tax + r.otherDeductions);
        r.net = round(r.gross - r.totalDeductions);
        return r;
    }

    /** Simplified slab-based annual tax. */
    static double annualTax(double annual) {
        double[] limits = {300000, 700000, 1000000, 1200000, 1500000};
        double[] rates = {0.00, 0.05, 0.10, 0.15, 0.20, 0.30};
        double tax = 0, prev = 0;
        for (int i = 0; i < limits.length; i++) {
            if (annual > limits[i]) {
                tax += (limits[i] - prev) * rates[i];
                prev = limits[i];
            } else {
                tax += (annual - prev) * rates[i];
                return tax;
            }
        }
        return tax + (annual - prev) * rates[rates.length - 1];
    }

    static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
