package payroll;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.util.List;

/** Employees screen: add / edit / delete, plus search, department, salary-range filters and sorting. */
public class EmployeePanel extends JPanel implements Refreshable {

    private final EmployeeDAO dao = new EmployeeDAO();
    private final Runnable onChange;

    // form fields (the grey text inside each box is the example shown to the user)
    private final JTextField fName = Theme.field(18, "e.g. Ravi Kumar"),
            fEmail = Theme.field(18, "e.g. ravi@gmail.com"),
            fPhone = Theme.field(18, "e.g. 9876543210"),
            fDept = Theme.field(18, "e.g. IT, HR, Finance"),
            fDesig = Theme.field(18, "e.g. Software Developer"),
            fJoin = Theme.field(18, "yyyy-mm-dd   e.g. 2024-06-01"),
            fBasic = Theme.field(18, "e.g. 30000   (per month)");

    // form heading + buttons (they change between "add" mode and "edit" mode)
    private final JLabel lblMode = Theme.heading("Add New Employee");
    private final JLabel lblModeHint = Theme.muted("Fill the details below and press Add.");
    private final JButton bAdd = Theme.primary("Add Employee"), bUpd = Theme.success("Update"),
            bDel = Theme.danger("Delete"), bClr = Theme.neutral("Clear");

    // filter controls
    private final JTextField fSearch = Theme.field(16, "ID, name, email, designation"),
            fMin = Theme.field(7, "Min"), fMax = Theme.field(7, "Max");
    private final JComboBox<String> cbDept = Theme.combo("All departments");
    private final JComboBox<String> cbSort = Theme.combo("ID", "Name (A-Z)", "Basic Salary (High-Low)",
            "Basic Salary (Low-High)", "Join Date (Newest)");

    private final DefaultTableModel model = Theme.model("ID", "Name", "Email", "Phone", "Department",
            "Designation", "Join Date", "Basic Salary");
    private final JTable table = Theme.table(model);
    private final JLabel lblCount = Theme.muted(" ");

    // table <-> empty message
    private final CardLayout listCards = new CardLayout();
    private final JPanel listArea = new JPanel(listCards);
    private final JLabel emptyTitle = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel emptyText = new JLabel(" ", SwingConstants.CENTER);

    private int selectedId = -1;
    private boolean adjusting = false;

    public EmployeePanel(Runnable onChange) {
        this.onChange = onChange;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        add(Theme.pageHeader("Employees", "Add, edit, search and filter your team"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setOpaque(false);
        body.add(buildForm(), BorderLayout.WEST);
        body.add(buildList(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        wireTable();
        setMode(false);
    }

    // ------------------------------------------------------------ layout
    private JPanel buildForm() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.setPreferredSize(new Dimension(330, 0));

        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setOpaque(false);
        lblMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblModeHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(lblMode);
        head.add(Box.createVerticalStrut(2));
        head.add(lblModeHint);
        card.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        Theme.formRow(form, 0, "Full name *  (required)", fName);
        Theme.formRow(form, 2, "Email", fEmail);
        Theme.formRow(form, 4, "Phone", fPhone);
        Theme.formRow(form, 6, "Department", fDept);
        Theme.formRow(form, 8, "Designation (job title)", fDesig);
        Theme.formRow(form, 10, "Join date", fJoin);
        Theme.formRow(form, 12, "Basic salary per month *  (required)", fBasic);
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = 14; filler.weighty = 1;
        form.add(Box.createGlue(), filler);
        card.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new GridLayout(2, 2, 8, 8));
        btns.setOpaque(false);
        btns.add(bAdd); btns.add(bUpd); btns.add(bDel); btns.add(bClr);
        card.add(btns, BorderLayout.SOUTH);

        bAdd.addActionListener(e -> {
            Employee emp = readForm(false);
            if (emp == null) return;
            try {
                dao.add(emp);
                clearForm();
                onChange.run();
                Theme.info(this, emp.name + " was added.");
            } catch (Exception ex) { Theme.error(this, ex.getMessage()); }
        });
        bUpd.addActionListener(e -> {
            if (selectedId < 0) { Theme.warn(this, "Click an employee in the table first."); return; }
            Employee emp = readForm(true);
            if (emp == null) return;
            try {
                dao.update(emp);
                clearForm();
                onChange.run();
                Theme.info(this, emp.name + " was updated.");
            } catch (Exception ex) { Theme.error(this, ex.getMessage()); }
        });
        bDel.addActionListener(e -> {
            if (selectedId < 0) { Theme.warn(this, "Click an employee in the table first."); return; }
            if (!Theme.confirm(this, "Delete " + fName.getText().trim() + " and all their salary slips?")) return;
            try {
                dao.delete(selectedId);
                clearForm();
                onChange.run();
            } catch (Exception ex) { Theme.error(this, ex.getMessage()); }
        });
        bClr.addActionListener(e -> clearForm());
        return card;
    }

    private JPanel buildList() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.setOpaque(false);
        row1.add(Theme.muted("Search"));
        row1.add(fSearch);
        row1.add(Theme.muted("Department"));
        row1.add(cbDept);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setOpaque(false);
        row2.add(Theme.muted("Sort by"));
        row2.add(cbSort);
        row2.add(Theme.muted("Basic salary from"));
        row2.add(fMin);
        row2.add(Theme.muted("to"));
        row2.add(fMax);

        JButton bApply = Theme.primary("Apply Filters"), bReset = Theme.neutral("Reset"),
                bCsv = Theme.success("Export CSV");
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row3.setOpaque(false);
        row3.add(bApply); row3.add(bReset); row3.add(bCsv);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(row1);
        top.add(Box.createVerticalStrut(8));
        top.add(row2);
        top.add(Box.createVerticalStrut(8));
        top.add(row3);

        // table page + empty-message page
        JPanel empty = new JPanel();
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setBackground(Color.WHITE);
        empty.setBorder(new LineBorder(Theme.BORDER, 1, true));
        emptyTitle.setFont(Theme.BASE.deriveFont(Font.BOLD, 18f));
        emptyTitle.setForeground(Theme.PRIMARY);
        emptyText.setFont(Theme.BASE.deriveFont(14f));
        emptyText.setForeground(Theme.MUTED);
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyText.setAlignmentX(Component.CENTER_ALIGNMENT);
        empty.add(Box.createVerticalGlue());
        empty.add(emptyTitle);
        empty.add(Box.createVerticalStrut(8));
        empty.add(emptyText);
        empty.add(Box.createVerticalGlue());
        listArea.add(Theme.scroll(table), "table");
        listArea.add(empty, "empty");

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        foot.setOpaque(false);
        foot.add(lblCount);

        card.add(top, BorderLayout.NORTH);
        card.add(listArea, BorderLayout.CENTER);
        card.add(foot, BorderLayout.SOUTH);

        bApply.addActionListener(e -> applyFilter());
        fSearch.addActionListener(e -> applyFilter());
        fMin.addActionListener(e -> applyFilter());
        fMax.addActionListener(e -> applyFilter());
        cbDept.addActionListener(e -> { if (!adjusting) applyFilter(); });
        cbSort.addActionListener(e -> { if (!adjusting) applyFilter(); });
        bReset.addActionListener(e -> {
            fSearch.setText(""); fMin.setText(""); fMax.setText("");
            adjusting = true; cbDept.setSelectedIndex(0); cbSort.setSelectedIndex(0); adjusting = false;
            applyFilter();
        });
        bCsv.addActionListener(e -> Theme.exportTable(this, model, "employees.csv"));
        return card;
    }

    private void wireTable() {
        int[] widths = {45, 140, 175, 110, 105, 130, 95, 110};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int r = table.getSelectedRow();
            if (r < 0) return;
            selectedId = (Integer) model.getValueAt(r, 0);
            fName.setText(s(model.getValueAt(r, 1)));
            fEmail.setText(s(model.getValueAt(r, 2)));
            fPhone.setText(s(model.getValueAt(r, 3)));
            fDept.setText(s(model.getValueAt(r, 4)));
            fDesig.setText(s(model.getValueAt(r, 5)));
            fJoin.setText(s(model.getValueAt(r, 6)));
            Object b = model.getValueAt(r, 7);
            if (b instanceof Double) {
                double d = (Double) b;
                fBasic.setText(d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d));
            }
            setMode(true);
        });
    }

    /** Switches the form between "add a new employee" and "edit the selected employee". */
    private void setMode(boolean editing) {
        if (editing) {
            lblMode.setText("Edit Employee  (ID " + selectedId + ")");
            lblModeHint.setText("Change what you need, then press Update.");
        } else {
            lblMode.setText("Add New Employee");
            lblModeHint.setText("Fill the details below and press Add Employee.");
        }
        bAdd.setEnabled(!editing);
        bUpd.setEnabled(editing);
        bDel.setEnabled(editing);
        bClr.setText(editing ? "Cancel Edit" : "Clear");
    }

    // ------------------------------------------------------------ data
    @Override
    public void refresh() {
        loadDepartments();
        applyFilter();
    }

    private void loadDepartments() {
        try {
            Object keep = cbDept.getSelectedItem();
            adjusting = true;
            cbDept.removeAllItems();
            cbDept.addItem("All departments");
            for (String d : dao.departments()) cbDept.addItem(d);
            if (keep != null) cbDept.setSelectedItem(keep);
            if (cbDept.getSelectedIndex() < 0) cbDept.setSelectedIndex(0);
        } catch (Exception ex) {
            Theme.error(this, ex.getMessage());
        } finally {
            adjusting = false;
        }
    }

    private void applyFilter() {
        Double min, max;
        try {
            min = parseOpt(fMin.getText());
            max = parseOpt(fMax.getText());
        } catch (NumberFormatException ex) {
            Theme.warn(this, "Salary range must be numbers, for example Min 20000 and Max 60000.");
            return;
        }
        if (min != null && max != null && min > max) {
            Theme.warn(this, "Salary 'from' is bigger than 'to'. Please swap them.");
            return;
        }
        String dept = cbDept.getSelectedIndex() <= 0 ? "" : (String) cbDept.getSelectedItem();
        try {
            List<Employee> list = dao.filter(fSearch.getText(), dept, min, max, (String) cbSort.getSelectedItem());
            model.setRowCount(0);
            double total = 0;
            for (Employee e : list) {
                model.addRow(new Object[]{e.id, e.name, e.email, e.phone, e.department, e.designation,
                        e.joinDate == null ? "" : e.joinDate.toString(), e.basicSalary});
                total += e.basicSalary;
            }
            if (list.isEmpty()) {
                if (dao.all().isEmpty()) {
                    emptyTitle.setText("No employees yet");
                    emptyText.setText("Fill the form on the left and press \"Add Employee\" to create the first one.");
                    lblCount.setText("0 employees");
                } else {
                    emptyTitle.setText("No employee matches these filters");
                    emptyText.setText("Change the search text or press Reset to see everyone.");
                    lblCount.setText("0 employees match");
                }
                listCards.show(listArea, "empty");
            } else {
                listCards.show(listArea, "table");
                lblCount.setText(list.size() + " employee(s) shown   |   Total basic salary: " + Theme.money(total)
                        + "   |   Click a row to edit it");
            }
        } catch (Exception ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    /** Blank = null (no limit). A wrong number throws NumberFormatException. */
    private static Double parseOpt(String t) {
        if (t == null || t.trim().isEmpty()) return null;
        return Double.valueOf(t.trim());
    }

    private Employee readForm(boolean withId) {
        String name = fName.getText().trim();
        if (name.isEmpty()) { Theme.warn(this, "Please enter the employee's full name."); return null; }

        String email = fEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            Theme.warn(this, "That email does not look right. Example: ravi@gmail.com"); return null;
        }
        String phone = fPhone.getText().trim();
        if (!phone.isEmpty() && !phone.matches("^[0-9+\\- ]{7,15}$")) {
            Theme.warn(this, "Phone should have 7 to 15 digits. Example: 9876543210"); return null;
        }
        double basic;
        try {
            basic = Double.parseDouble(fBasic.getText().trim());
            if (basic < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            Theme.warn(this, "Basic salary must be a number, for example 30000."); return null;
        }
        Date join = null;
        if (!fJoin.getText().trim().isEmpty()) {
            try { join = Date.valueOf(fJoin.getText().trim()); }
            catch (IllegalArgumentException ex) { Theme.warn(this, "Join date must look like 2024-06-01 (year-month-day)."); return null; }
        }
        Employee e = new Employee();
        if (withId) e.id = selectedId;
        e.name = name;
        e.email = email;
        e.phone = phone;
        e.department = fDept.getText().trim();
        e.designation = fDesig.getText().trim();
        e.joinDate = join;
        e.basicSalary = basic;
        return e;
    }

    private void clearForm() {
        selectedId = -1;
        for (JTextField f : new JTextField[]{fName, fEmail, fPhone, fDept, fDesig, fJoin, fBasic}) f.setText("");
        table.clearSelection();
        setMode(false);
    }

    private static String s(Object o) { return o == null ? "" : o.toString(); }
}
