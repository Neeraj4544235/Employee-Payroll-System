package payroll;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/** Main window: dark sidebar on the left, the selected screen on the right. */
public class MainFrame extends JFrame {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Refreshable> pages = new LinkedHashMap<>();
    private final Map<String, NavButton> navs = new LinkedHashMap<>();

    public MainFrame() {
        super("Employee Payroll System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        Runnable refreshAll = this::refreshAll;
        DashboardPanel dashboard = new DashboardPanel();
        EmployeePanel employees = new EmployeePanel(refreshAll);
        PayrollPanel payroll = new PayrollPanel(refreshAll);
        ReportPanel reports = new ReportPanel(refreshAll);

        addPage("Dashboard", dashboard, dashboard);
        addPage("Employees", employees, employees);
        addPage("Payroll", payroll, payroll);
        addPage("Reports", reports, reports);

        content.setBackground(Theme.BG);
        add(buildSidebar(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        setSize(1280, 780);
        setMinimumSize(new Dimension(1100, 650));
        setLocationRelativeTo(null);
        show("Dashboard");
    }

    private void addPage(String name, JPanel panel, Refreshable page) {
        content.add(panel, name);
        pages.put(name, page);
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Theme.SIDEBAR);
        side.setPreferredSize(new Dimension(210, 0));
        side.setBorder(new EmptyBorder(24, 14, 18, 14));

        JLabel logo = new JLabel("PAYROLL");
        logo.setFont(Theme.BASE.deriveFont(Font.BOLD, 22f));
        logo.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Employee Payroll System");
        sub.setFont(Theme.SMALL);
        sub.setForeground(new Color(0x9F, 0xB3, 0xC8));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(logo);
        side.add(sub);
        side.add(Box.createVerticalStrut(28));

        for (String name : pages.keySet()) {
            NavButton b = new NavButton(name);
            b.addActionListener(e -> show(name));
            navs.put(name, b);
            side.add(b);
            side.add(Box.createVerticalStrut(6));
        }
        side.add(Box.createVerticalGlue());
        return side;
    }

    private void show(String name) {
        for (Map.Entry<String, NavButton> e : navs.entrySet()) e.getValue().setSelectedNav(e.getKey().equals(name));
        cards.show(content, name);          // switch first, so the tab always opens
        safeRefresh(pages.get(name));
    }

    private void refreshAll() {
        for (Refreshable r : pages.values()) safeRefresh(r);
    }

    /** One screen failing to reload must never block the others. */
    private void safeRefresh(Refreshable r) {
        try {
            r.refresh();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
    }

    /** Sidebar button that highlights when selected. */
    private static class NavButton extends JButton {
        private boolean selected, hover;

        NavButton(String text) {
            super(text);
            setFont(Theme.BASE.deriveFont(Font.BOLD, 14f));
            setForeground(Color.WHITE);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(11, 16, 11, 16));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setSelectedNav(boolean s) { selected = s; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selected) {
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            } else if (hover) {
                g2.setColor(new Color(255, 255, 255, 28));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
