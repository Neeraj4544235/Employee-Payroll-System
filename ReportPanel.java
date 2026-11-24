package payroll;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Reports screen: filter saved salary slips by month, department and name, see totals, export CSV. */
public class ReportPanel extends JPanel implements Refreshable {

    private final EmployeeDAO empDao = new EmployeeDAO();
    private final SalaryDAO salDao = new SalaryDAO();
    private final Runnable onChange;

    private final JTextField fMonth = Theme.field(8, "2026-10"), fName = Theme.field(14, "part of the name");
    private final CardLayout areaCards = new CardLayout();
    private final JPanel area = new JPanel(areaCards);
    private final JComboBox<String> cbDept = Theme.combo("All departments");
    private final DefaultTableModel model = Theme.model("Slip ID", "Emp ID", "Name", "Department", "Month", "Basic",
            "HRA", "DA", "Allowances", "Gross", "PF", "Tax", "Other Ded.", "Net Pay");
    private final JTable table = Theme.table(model);

    private final JLabel tRecords = total(), tGross = total(), tDed = total(), tNet = total();
    private boolean adjusting = false;

    public ReportPanel(Runnable onChange) {
        this.onChange = onChange;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        add(Theme.pageHeader("Reports", "Look up saved salary slips and see the totals"), BorderLayout.NORTH);

        fMonth.setText(new SimpleDateFormat("yyyy-MM").format(new Date()));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(Theme.muted("Month (blank = all)")); filters.add(fMonth);
        filters.add(Theme.muted("Department")); filters.add(cbDept);
        filters.add(Theme.muted("Employee name")); filters.add(fName);
        JButton bLoad = Theme.primary("Load Report"), bReset = Theme.neutral("Reset"),
                bCsv = Theme.success("Export CSV"), bDel = Theme.danger("Delete Selected Slip");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(bLoad); buttons.add(bReset); buttons.add(bCsv); buttons.add(bDel);
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(filters);
        top.add(Box.createVerticalStrut(8));
        top.add(buttons);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {60, 60, 150, 120, 80, 95, 90, 90, 95, 105, 90, 90, 95, 110};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JPanel totals = new JPanel(new GridLayout(1, 4, 12, 0));
        totals.setOpaque(false);
        totals.add(totalBox("Slips", tRecords));
        totals.add(totalBox("Total Gross", tGross));
        totals.add(totalBox("Total Deductions", tDed));
        totals.add(totalBox("Total Net Payout", tNet));

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(top, BorderLayout.NORTH);
        JLabel none = new JLabel("<html><div style='text-align:center'><b style='font-size:15px'>No salary slips found</b><br><br>"
                + "Save a salary in the Payroll tab, or change the filters above.</div></html>", SwingConstants.CENTER);
        none.setForeground(Theme.MUTED);
        none.setOpaque(true);
        none.setBackground(Color.WHITE);
        none.setBorder(new javax.swing.border.LineBorder(Theme.BORDER, 1, true));
        area.add(Theme.scroll(table), "table");
        area.add(none, "empty");
        card.add(area, BorderLayout.CENTER);
        card.add(totals, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        bLoad.addActionListener(e -> load());
        fMonth.addActionListener(e -> load());
        fName.addActionListener(e -> load());
        cbDept.addActionListener(e -> { if (!adjusting) load(); });
        bReset.addActionListener(e -> {
            fMonth.setText(""); fName.setText("");
            adjusting = true; cbDept.setSelectedIndex(0); adjusting = false;
            load();
        });
        bCsv.addActionListener(e -> Theme.exportTable(this, model, "payroll_report.csv"));
        bDel.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r < 0) { Theme.warn(this, "Click a slip in the table first."); return; }
            if (!Theme.confirm(this, "Delete this salary slip?")) return;
            try { salDao.delete((Integer) model.getValueAt(r, 0)); onChange.run(); }
            catch (Exception ex) { Theme.error(this, ex.getMessage()); }
        });
    }

    private static JLabel total() {
        JLabel l = new JLabel("0");
        l.setFont(Theme.BASE.deriveFont(Font.BOLD, 18f));
        l.setForeground(Theme.PRIMARY);
        return l;
    }

    private static JPanel totalBox(String caption, JLabel value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Theme.STRIPE);
        p.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel c = Theme.muted(caption);
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(c);
        p.add(value);
        return p;
    }

    @Override
    public void refresh() {
        try {
            Object keep = cbDept.getSelectedItem();
            adjusting = true;
            cbDept.removeAllItems();
            cbDept.addItem("All departments");
            for (String d : empDao.departments()) cbDept.addItem(d);
            if (keep != null) cbDept.setSelectedItem(keep);
            if (cbDept.getSelectedIndex() < 0) cbDept.setSelectedIndex(0);
        } catch (Exception ex) {
            Theme.error(this, ex.getMessage());
        } finally {
            adjusting = false;
        }
        load();
    }

    private void load() {
        String month = fMonth.getText().trim();
        if (!month.isEmpty() && !Theme.isMonth(month)) {
            Theme.warn(this, "Month must look like 2026-10 (or leave it blank for all months).");
            return;
        }
        String dept = cbDept.getSelectedIndex() <= 0 ? "" : (String) cbDept.getSelectedItem();
        try {
            List<SalaryRecord> list = salDao.list(month, dept, fName.getText());
            model.setRowCount(0);
            double g = 0, d = 0, n = 0;
            for (SalaryRecord r : list) {
                model.addRow(new Object[]{r.salId, r.empId, r.empName, r.department, r.monthYear, r.basic, r.hra,
                        r.da, r.allowances, r.gross, r.pf, r.tax, r.otherDeductions, r.net});
                g += r.gross; d += r.totalDeductions; n += r.net;
            }
            areaCards.show(area, list.isEmpty() ? "empty" : "table");
            tRecords.setText(String.valueOf(list.size()));
            tGross.setText(Theme.money(g));
            tDed.setText(Theme.money(d));
            tNet.setText(Theme.money(n));
        } catch (Exception ex) {
            Theme.error(this, ex.getMessage());
        }
    }
}
