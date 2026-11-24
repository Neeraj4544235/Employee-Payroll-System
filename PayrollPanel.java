package payroll;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Payroll screen: calculate one salary, preview the slip, save / export / print, or run payroll for everyone. */
public class PayrollPanel extends JPanel implements Refreshable {

    private final EmployeeDAO empDao = new EmployeeDAO();
    private final SalaryDAO salDao = new SalaryDAO();
    private final Runnable onChange;

    private final JComboBox<Employee> cbEmp = new JComboBox<>();
    private final JTextField fMonth = Theme.field(8, "2026-10"), fAllow = Theme.field(8, "e.g. 2000"),
            fOther = Theme.field(8, "e.g. 500");
    private static final String DEFAULT_SLIP_TEXT = "Pick an employee and press Calculate to see the salary slip here.";
    private static final String NO_EMPLOYEES_TEXT = "No employees yet.\n\nGo to the Employees tab, add an employee, then come back here.";
    private final JTextArea slipArea = new JTextArea();
    private SalaryRecord preview;

    public PayrollPanel(Runnable onChange) {
        this.onChange = onChange;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        add(Theme.pageHeader("Payroll", "Calculate a salary, check the slip, then save it"), BorderLayout.NORTH);

        fMonth.setText(new SimpleDateFormat("yyyy-MM").format(new Date()));
        fAllow.setText("0");
        fOther.setText("0");
        cbEmp.setFont(Theme.BASE);
        cbEmp.setBackground(Color.WHITE);
        cbEmp.setPreferredSize(new Dimension(240, 34));

        // ---- top controls
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.setOpaque(false);
        row1.add(Theme.muted("Employee")); row1.add(cbEmp);
        row1.add(Theme.muted("Month (year-month)")); row1.add(fMonth);
        JPanel rowMoney = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rowMoney.setOpaque(false);
        rowMoney.add(Theme.muted("Allowances (extra pay)")); rowMoney.add(fAllow);
        rowMoney.add(Theme.muted("Other deductions (loan, leave)")); rowMoney.add(fOther);

        JButton bCalc = Theme.primary("Calculate"), bSave = Theme.success("Save to Database"),
                bExport = Theme.neutral("Export Slip (.txt)"), bPrint = Theme.neutral("Print Slip"),
                bAll = Theme.danger("Run Payroll for ALL Employees");
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setOpaque(false);
        row2.add(bCalc); row2.add(bSave); row2.add(bExport); row2.add(bPrint); row2.add(bAll);

        JPanel top = Theme.card();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(row1);
        top.add(Box.createVerticalStrut(10));
        top.add(rowMoney);
        top.add(Box.createVerticalStrut(12));
        top.add(row2);

        // ---- slip preview
        slipArea.setEditable(false);
        slipArea.setFont(Theme.MONO);
        slipArea.setForeground(Theme.TEXT);
        slipArea.setBorder(new EmptyBorder(14, 18, 14, 18));
        slipArea.setText(DEFAULT_SLIP_TEXT);

        JPanel slipCard = Theme.card();
        slipCard.setLayout(new BorderLayout(0, 10));
        slipCard.add(Theme.heading("Salary Slip Preview"), BorderLayout.NORTH);
        slipCard.add(Theme.scroll(slipArea), BorderLayout.CENTER);

        // controls on top, slip preview below
        JPanel centre = new JPanel(new BorderLayout(0, 16));
        centre.setOpaque(false);
        centre.add(top, BorderLayout.NORTH);
        centre.add(slipCard, BorderLayout.CENTER);
        add(centre, BorderLayout.CENTER);

        bCalc.addActionListener(e -> calculate());
        bSave.addActionListener(e -> {
            if (preview == null) { Theme.warn(this, "Press Calculate first."); return; }
            try {
                salDao.save(preview);
                onChange.run();
                Theme.info(this, "Salary saved for " + preview.empName + " (" + preview.monthYear + ").");
            } catch (Exception ex) { Theme.error(this, ex.getMessage()); }
        });
        bExport.addActionListener(e -> exportSlip());
        bPrint.addActionListener(e -> {
            if (preview == null) { Theme.warn(this, "Press Calculate first."); return; }
            try { slipArea.print(); } catch (Exception ex) { Theme.error(this, ex.getMessage()); }
        });
        bAll.addActionListener(e -> runForAll());
    }

    @Override
    public void refresh() {
        try {
            Employee sel = (Employee) cbEmp.getSelectedItem();
            cbEmp.removeAllItems();
            for (Employee e : empDao.all()) {
                cbEmp.addItem(e);
                if (sel != null && sel.id == e.id) cbEmp.setSelectedItem(e);
            }
            if (cbEmp.getItemCount() == 0) {
                preview = null;
                slipArea.setText(NO_EMPLOYEES_TEXT);
            } else if (slipArea.getText().equals(NO_EMPLOYEES_TEXT)) {
                slipArea.setText(DEFAULT_SLIP_TEXT);
            }
        } catch (Exception ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    /** Reads month, allowances and deductions; returns null (after showing a message) if something is wrong. */
    private Object[] readInputs() {
        String month = fMonth.getText().trim();
        if (!Theme.isMonth(month)) { Theme.warn(this, "Month must look like 2026-10."); return null; }
        try {
            double a = Double.parseDouble(fAllow.getText().trim());
            double o = Double.parseDouble(fOther.getText().trim());
            if (a < 0 || o < 0) throw new NumberFormatException();
            return new Object[]{month, a, o};
        } catch (NumberFormatException ex) {
            Theme.warn(this, "Allowances and deductions must be numbers (0 or more).");
            return null;
        }
    }

    private void calculate() {
        Employee e = (Employee) cbEmp.getSelectedItem();
        if (e == null) { Theme.warn(this, "There are no employees yet. Add one in the Employees tab first."); return; }
        Object[] in = readInputs();
        if (in == null) return;
        preview = PayrollCalculator.calculate(e, (String) in[0], (Double) in[1], (Double) in[2]);
        slipArea.setText(slip(preview));
        slipArea.setCaretPosition(0);
    }

    private void runForAll() {
        Object[] in = readInputs();
        if (in == null) return;
        try {
            List<Employee> list = empDao.all();
            if (list.isEmpty()) { Theme.warn(this, "There are no employees yet."); return; }
            if (!Theme.confirm(this, "Create salary slips for " + list.size() + " employee(s) for " + in[0]
                    + "?\nThe same allowances and deductions are used for everyone.\n"
                    + "Slips already saved for this month will be replaced.")) return;
            double net = 0;
            for (Employee e : list) {
                SalaryRecord r = PayrollCalculator.calculate(e, (String) in[0], (Double) in[1], (Double) in[2]);
                salDao.save(r);
                net += r.net;
            }
            onChange.run();
            Theme.info(this, list.size() + " slip(s) saved for " + in[0] + ".\nTotal net payout: " + Theme.money(net));
        } catch (Exception ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    private void exportSlip() {
        if (preview == null) { Theme.warn(this, "Press Calculate first."); return; }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("salary_slip_" + preview.empId + "_" + preview.monthYear + ".txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter pw = new PrintWriter(fc.getSelectedFile(), "UTF-8")) {
            pw.print(slip(preview));
            Theme.info(this, "Slip saved: " + fc.getSelectedFile().getName());
        } catch (Exception ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    static String slip(SalaryRecord r) {
        String line = "=".repeat(46) + "\n";
        String thin = "-".repeat(46) + "\n";
        StringBuilder sb = new StringBuilder();
        sb.append(line).append("          SALARY SLIP  -  ").append(r.monthYear).append("\n").append(line);
        sb.append(String.format("Employee ID    : %d%n", r.empId));
        sb.append(String.format("Employee Name  : %s%n", r.empName));
        sb.append(thin).append("EARNINGS\n");
        sb.append(String.format("  Basic Salary       : %14s%n", Theme.money(r.basic)));
        sb.append(String.format("  HRA (20%%)          : %14s%n", Theme.money(r.hra)));
        sb.append(String.format("  DA (10%%)           : %14s%n", Theme.money(r.da)));
        sb.append(String.format("  Allowances         : %14s%n", Theme.money(r.allowances)));
        sb.append(String.format("  GROSS SALARY       : %14s%n", Theme.money(r.gross)));
        sb.append(thin).append("DEDUCTIONS\n");
        sb.append(String.format("  PF (12%%)           : %14s%n", Theme.money(r.pf)));
        sb.append(String.format("  Income Tax         : %14s%n", Theme.money(r.tax)));
        sb.append(String.format("  Other Deductions   : %14s%n", Theme.money(r.otherDeductions)));
        sb.append(String.format("  TOTAL DEDUCTIONS   : %14s%n", Theme.money(r.totalDeductions)));
        sb.append(line);
        sb.append(String.format("  NET PAY            : %14s%n", Theme.money(r.net)));
        sb.append(line);
        sb.append("  Computer generated slip. No signature needed.\n");
        return sb.toString();
    }
}
