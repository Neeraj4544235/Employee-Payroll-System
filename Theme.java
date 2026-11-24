package payroll;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Locale;

/** Colours, fonts and small helpers so every screen looks the same. */
public final class Theme {

    private Theme() { }

    public static final Color SIDEBAR = new Color(0x14, 0x2B, 0x45);
    public static final Color PRIMARY = new Color(0x1F, 0x4E, 0x79);
    public static final Color ACCENT = new Color(0x2E, 0x86, 0xC1);
    public static final Color SUCCESS = new Color(0x1E, 0x8E, 0x5A);
    public static final Color DANGER = new Color(0xC0, 0x39, 0x2B);
    public static final Color NEUTRAL = new Color(0x6B, 0x77, 0x85);
    public static final Color BG = new Color(0xF4, 0xF6, 0xF9);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(0x22, 0x2B, 0x36);
    public static final Color MUTED = new Color(0x6B, 0x77, 0x85);
    public static final Color BORDER = new Color(0xD9, 0xDF, 0xE7);
    public static final Color STRIPE = new Color(0xF0, 0xF5, 0xFA);

    public static final Font BASE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font BOLD = BASE.deriveFont(Font.BOLD);
    public static final Font TITLE = BASE.deriveFont(Font.BOLD, 24f);
    public static final Font BIG = BASE.deriveFont(Font.BOLD, 26f);
    public static final Font SMALL = BASE.deriveFont(12f);
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 14);

    /** Call once before creating any window. */
    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) { }
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object k = keys.nextElement();
            if (UIManager.get(k) instanceof FontUIResource) {
                UIManager.put(k, new FontUIResource(BASE));
            }
        }
        UIManager.put("OptionPane.messageForeground", TEXT);
    }

    // ------------------------------------------------------------ buttons
    public static class Btn extends JButton {
        private final Color base;
        private boolean hover;

        public Btn(String text, Color base) {
            super(text);
            this.base = base;
            setFont(BOLD);
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(!isEnabled() ? new Color(0xB0, 0xB8, 0xC2) : hover ? base.darker() : base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JButton primary(String t) { return new Btn(t, PRIMARY); }
    public static JButton success(String t) { return new Btn(t, SUCCESS); }
    public static JButton danger(String t) { return new Btn(t, DANGER); }
    public static JButton neutral(String t) { return new Btn(t, NEUTRAL); }

    // ------------------------------------------------------------ inputs
    /** Text box that shows a light grey hint while it is empty. */
    public static class HintField extends JTextField {
        private final String hint;

        HintField(int cols, String hint) {
            super(cols);
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (hint == null || !getText().isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(0x9A, 0xA3, 0xAF));
            g2.setFont(getFont().deriveFont(Font.ITALIC));
            Insets in = getInsets();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    public static JTextField field(int cols) {
        return field(cols, null);
    }

    public static JTextField field(int cols, String hint) {
        JTextField f = new HintField(cols, hint);
        f.setFont(BASE);
        f.setForeground(TEXT);
        f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(6, 8, 6, 8)));
        return f;
    }

    public static JComboBox<String> combo(String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(BASE);
        c.setBackground(Color.WHITE);
        return c;
    }

    // ------------------------------------------------------------ labels & panels
    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BASE);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(SMALL);
        l.setForeground(MUTED);
        return l;
    }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BASE.deriveFont(Font.BOLD, 15f));
        l.setForeground(PRIMARY);
        return l;
    }

    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(16, 16, 16, 16)));
        return p;
    }

    public static JPanel pageHeader(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(TITLE);
        t.setForeground(PRIMARY);
        JLabel s = muted(subtitle);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(2));
        p.add(s);
        return p;
    }

    /** Adds "label + input" as one row of a GridBagLayout form. */
    public static void formRow(JPanel p, int y, String label, JComponent input) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = y; g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(6, 0, 2, 0);
        g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        JLabel l = muted(label);
        p.add(l, g);
        g.gridy = y + 1;
        g.insets = new Insets(0, 0, 4, 0);
        p.add(input, g);
    }

    // ------------------------------------------------------------ tables
    public static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    public static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(30);
        t.setShowVerticalLines(false);
        t.setGridColor(BORDER);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(new Color(0xCF, 0xE4, 0xF7));
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 36));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                l.setOpaque(true);
                l.setBackground(PRIMARY);
                l.setForeground(Color.WHITE);
                l.setFont(BOLD);
                l.setBorder(new EmptyBorder(0, 8, 0, 8));
                return l;
            }
        });

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                String text = v instanceof Double ? money((Double) v) : (v == null ? "" : v.toString());
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, text, sel, false, r, c);
                l.setHorizontalAlignment(v instanceof Double ? SwingConstants.RIGHT : SwingConstants.LEFT);
                l.setBorder(new EmptyBorder(0, 8, 0, 8));
                l.setForeground(TEXT);
                if (!sel) l.setBackground(r % 2 == 0 ? Color.WHITE : STRIPE);
                return l;
            }
        });
        return t;
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(new LineBorder(BORDER, 1, true));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    // ------------------------------------------------------------ formatting
    public static String money(double v) {
        return String.format(Locale.US, "%,.2f", v);
    }

    public static boolean isMonth(String s) {
        return s != null && s.matches("\\d{4}-(0[1-9]|1[0-2])");
    }

    // ------------------------------------------------------------ dialogs
    public static void info(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Done", JOptionPane.INFORMATION_MESSAGE); }
    public static void warn(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Please check", JOptionPane.WARNING_MESSAGE); }
    public static void error(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Error", JOptionPane.ERROR_MESSAGE); }

    public static boolean confirm(Component p, String m) {
        return JOptionPane.showConfirmDialog(p, m, "Please confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    // ------------------------------------------------------------ CSV export
    public static void exportTable(Component parent, DefaultTableModel m, String defaultName) {
        if (m.getRowCount() == 0) { warn(parent, "Nothing to export yet."); return; }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(defaultName));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter pw = new PrintWriter(fc.getSelectedFile(), "UTF-8")) {
            for (int c = 0; c < m.getColumnCount(); c++) pw.print((c > 0 ? "," : "") + csv(m.getColumnName(c)));
            pw.println();
            for (int r = 0; r < m.getRowCount(); r++) {
                for (int c = 0; c < m.getColumnCount(); c++) {
                    Object v = m.getValueAt(r, c);
                    pw.print((c > 0 ? "," : "") + csv(v == null ? "" : v.toString()));
                }
                pw.println();
            }
            info(parent, "Saved: " + fc.getSelectedFile().getName());
        } catch (Exception ex) {
            error(parent, ex.getMessage());
        }
    }

    private static String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
