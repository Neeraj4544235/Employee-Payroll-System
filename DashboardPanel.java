package payroll;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Home screen: quick numbers, department table and a small bar chart. */
public class DashboardPanel extends JPanel implements Refreshable {

    private final EmployeeDAO empDao = new EmployeeDAO();
    private final SalaryDAO salDao = new SalaryDAO();

    private final JLabel vEmployees = bigValue(Theme.PRIMARY), vDepts = bigValue(Theme.ACCENT),
            vAvg = bigValue(Theme.SUCCESS), vPayout = bigValue(new Color(0xB9, 0x77, 0x0E));
    private final JLabel subPayout = Theme.muted(" ");
    private final DefaultTableModel deptModel = Theme.model("Department", "Employees", "Total Basic", "Average Basic");
    private final BarChart chart = new BarChart();

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        add(Theme.pageHeader("Dashboard", "A quick look at your team and this month's payroll"), BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.add(statCard("Total Employees", vEmployees, Theme.muted(" ")));
        cards.add(statCard("Departments", vDepts, Theme.muted(" ")));
        cards.add(statCard("Average Basic Salary", vAvg, Theme.muted("per employee")));
        cards.add(statCard("This Month Net Payout", vPayout, subPayout));

        JPanel left = Theme.card();
        left.setLayout(new BorderLayout(0, 10));
        left.add(Theme.heading("Department Summary"), BorderLayout.NORTH);
        left.add(Theme.scroll(Theme.table(deptModel)), BorderLayout.CENTER);

        JPanel right = Theme.card();
        right.setLayout(new BorderLayout(0, 10));
        right.add(Theme.heading("Total Basic Salary by Department"), BorderLayout.NORTH);
        right.add(chart, BorderLayout.CENTER);

        JPanel lower = new JPanel(new GridLayout(1, 2, 16, 0));
        lower.setOpaque(false);
        lower.add(left);
        lower.add(right);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(cards, BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private static JLabel bigValue(Color c) {
        JLabel l = new JLabel("0");
        l.setFont(Theme.BIG);
        l.setForeground(c);
        return l;
    }

    private static JPanel statCard(String caption, JLabel value, JLabel sub) {
        JPanel p = Theme.card();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel c = Theme.muted(caption);
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(c);
        p.add(Box.createVerticalStrut(6));
        p.add(value);
        p.add(Box.createVerticalStrut(4));
        p.add(sub);
        return p;
    }

    @Override
    public void refresh() {
        try {
            String month = new SimpleDateFormat("yyyy-MM").format(new Date());
            List<Object[]> rows = empDao.deptSummary();
            deptModel.setRowCount(0);
            int total = 0;
            double sum = 0;
            for (Object[] r : rows) {
                deptModel.addRow(r);
                total += (Integer) r[1];
                sum += (Double) r[2];
            }
            vEmployees.setText(String.valueOf(total));
            vDepts.setText(String.valueOf(rows.size()));
            vAvg.setText(total == 0 ? "0.00" : Theme.money(sum / total));

            List<SalaryRecord> slips = salDao.list(month, "", "");
            double net = 0;
            for (SalaryRecord s : slips) net += s.net;
            vPayout.setText(Theme.money(net));
            subPayout.setText(slips.size() + " salary slip(s) for " + month);
            chart.setData(rows);
        } catch (Exception ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    /** Very small horizontal bar chart drawn by hand. */
    private static class BarChart extends JPanel {
        private List<Object[]> data;

        BarChart() { setOpaque(false); }

        void setData(List<Object[]> d) { data = d; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(Theme.BASE);
            if (data == null || data.isEmpty()) {
                g2.setColor(Theme.MUTED);
                g2.drawString("No data yet. Add employees to see the chart.", 10, 30);
                g2.dispose();
                return;
            }
            double max = 0;
            for (Object[] r : data) max = Math.max(max, (Double) r[2]);
            if (max <= 0) max = 1;
            int labelW = 110, valueW = 90, rowH = 34;
            int barMax = Math.max(40, getWidth() - labelW - valueW - 10);
            Color[] palette = {Theme.PRIMARY, Theme.ACCENT, Theme.SUCCESS, new Color(0xB9, 0x77, 0x0E), new Color(0x7D, 0x3C, 0x98)};
            FontMetrics fm = g2.getFontMetrics();
            int y = 8;
            for (int i = 0; i < data.size() && y + rowH <= getHeight() + rowH / 2; i++) {
                Object[] r = data.get(i);
                String name = (String) r[0];
                while (fm.stringWidth(name) > labelW - 8 && name.length() > 3) name = name.substring(0, name.length() - 1);
                double v = (Double) r[2];
                int w = (int) Math.max(4, v / max * barMax);
                g2.setColor(Theme.TEXT);
                g2.drawString(name, 0, y + 17);
                g2.setColor(palette[i % palette.length]);
                g2.fillRoundRect(labelW, y + 4, w, 18, 8, 8);
                g2.setColor(Theme.MUTED);
                g2.drawString(Theme.money(v), labelW + w + 8, y + 18);
                y += rowH;
            }
            g2.dispose();
        }
    }
}
